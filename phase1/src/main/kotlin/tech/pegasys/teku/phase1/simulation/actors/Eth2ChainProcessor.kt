package tech.pegasys.teku.phase1.simulation.actors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tech.pegasys.teku.phase1.eth1client.Eth1EngineClient
import tech.pegasys.teku.phase1.integration.datastructures.Attestation
import tech.pegasys.teku.phase1.integration.datastructures.FullAttestation
import tech.pegasys.teku.phase1.integration.datastructures.ShardStore
import tech.pegasys.teku.phase1.integration.datastructures.SignedBeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.Store
import tech.pegasys.teku.phase1.onotole.phase1.GENESIS_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.INITIAL_ACTIVE_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.SECONDS_PER_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.get_current_slot
import tech.pegasys.teku.phase1.onotole.phase1.get_head
import tech.pegasys.teku.phase1.onotole.phase1.get_pending_shard_blocks
import tech.pegasys.teku.phase1.onotole.phase1.get_shard_head
import tech.pegasys.teku.phase1.onotole.phase1.on_attestation
import tech.pegasys.teku.phase1.onotole.phase1.on_block
import tech.pegasys.teku.phase1.onotole.phase1.on_tick
import tech.pegasys.teku.phase1.simulation.BeaconHead
import tech.pegasys.teku.phase1.simulation.Eth2Actor
import tech.pegasys.teku.phase1.simulation.Eth2Event
import tech.pegasys.teku.phase1.simulation.HeadAfterAttestationsApplied
import tech.pegasys.teku.phase1.simulation.HeadAfterNewBeaconBlock
import tech.pegasys.teku.phase1.simulation.NewBeaconBlock
import tech.pegasys.teku.phase1.simulation.NewShardBlocks
import tech.pegasys.teku.phase1.simulation.NewShardHeads
import tech.pegasys.teku.phase1.simulation.NewSlot
import tech.pegasys.teku.phase1.simulation.NotCrosslinkedBlocksPublished
import tech.pegasys.teku.phase1.simulation.PrevSlotAttestationsPublished
import tech.pegasys.teku.phase1.simulation.ShardBlockProcessor
import tech.pegasys.teku.phase1.util.Color
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.printRoot

class Eth2ChainProcessor(
  eventBus: SendChannel<Eth2Event>,
  private val store: Store,
  private val shardStores: Map<Shard, ShardStore>,
  private val eth1Engine: Eth1EngineClient
) : Eth2Actor(eventBus) {

  override suspend fun dispatchImpl(event: Eth2Event, scope: CoroutineScope) {
    when (event) {
      is NewSlot -> onNewSlot(event.slot)
      is PrevSlotAttestationsPublished -> onPrevSlotAttestationsPublished(event.attestations)
      is NewBeaconBlock -> onNewBeaconBlock(event.block)
      is NewShardBlocks -> onNewShardBlocks(event.blocks)
    }
  }

  private suspend fun onNewBeaconBlock(block: SignedBeaconBlock) {
    on_block(store, block)
    publishBeaconHead(::HeadAfterNewBeaconBlock)

    log("Eth2ChainProcessor: beacon block processed (root=${printRoot(block.message.hashTreeRoot())})")
  }

  private fun onNewSlot(slot: Slot) {
    on_tick(store, store.genesis_time + slot * SECONDS_PER_SLOT)
    log("Eth2ChainProcessor: New slot ($slot)", Color.BLUE)
  }

  private suspend fun onPrevSlotAttestationsPublished(attestations: List<FullAttestation>) {
    attestations.map { Attestation(it) }.forEach { on_attestation(store, it) }
    publishBeaconHead(::HeadAfterAttestationsApplied)

    if (get_current_slot(store) == GENESIS_SLOT) {
      collectAndPublishShardHeads()
      collectAndPublishNotCrosslinkedBlocks()
    }

    log("Eth2ChainProcessor: attestations processed [${attestations.joinToString { it.toStringShort() }}]")
  }

  private suspend fun publishBeaconHead(eventCtor: (BeaconHead) -> Eth2Event) {
    val headRoot = get_head(store)
    val headState = store.block_states[headRoot]!!
    publish(
      eventCtor(
        BeaconHead(
          headRoot,
          headState
        )
      )
    )
  }

  private suspend fun onNewShardBlocks(blocks: List<SignedShardBlock>) {
    processNewShardBlocks(blocks)
    collectAndPublishShardHeads()
    collectAndPublishNotCrosslinkedBlocks()

    log("Eth2ChainProcessor: shard blocks processed: [${blocks.map { it.message.hashTreeRoot() }
      .joinToString { "(root=${printRoot(it)})" }}]")
  }

  /**
   * Groups blocks by shard and processes each group in a separate coroutine
   */
  private suspend fun processNewShardBlocks(blocks: List<SignedShardBlock>) = coroutineScope {
    blocks.groupBy { it.message.shard }.entries.forEach {
      launch {
        val processor = ShardBlockProcessor(it.key, store, shardStores[it.key]!!, eth1Engine)
        it.value.sortedBy { it.message.slot }.forEach { processor.process(it) }
      }
    }
  }

  /**
   * Computes shards' heads concurrently and publishes them
   */
  private suspend fun collectAndPublishShardHeads() = coroutineScope {
    val res = (0uL until INITIAL_ACTIVE_SHARDS).map {
      async {
        val root = get_shard_head(store, shardStores[it]!!)
        root to shardStores[it]!!.signed_blocks[root]!!
      }
    }.awaitAll()

    publish(NewShardHeads(res))
  }

  /**
   * Uses flow to collect blocks for different shards concurrently
   */
  private suspend fun collectAndPublishNotCrosslinkedBlocks() = coroutineScope {
    val res = (0uL until INITIAL_ACTIVE_SHARDS).map {
      async { get_pending_shard_blocks(store, shardStores[it]!!) }
    }.awaitAll().flatten()

    publish(NotCrosslinkedBlocksPublished(res))
  }
}
