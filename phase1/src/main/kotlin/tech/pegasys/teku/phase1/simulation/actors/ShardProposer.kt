package tech.pegasys.teku.phase1.simulation.actors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import tech.pegasys.teku.phase1.eth1engine.Eth1BlockData
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1shard.ETH1_SHARD_NUMBER
import tech.pegasys.teku.phase1.integration.datastructures.ShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.onotole.phase1.GENESIS_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.INITIAL_ACTIVE_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.simulation.BeaconHead
import tech.pegasys.teku.phase1.simulation.Eth2Actor
import tech.pegasys.teku.phase1.simulation.Eth2Event
import tech.pegasys.teku.phase1.simulation.HeadAfterNewBeaconBlock
import tech.pegasys.teku.phase1.simulation.NewShardBlocks
import tech.pegasys.teku.phase1.simulation.NewShardHeads
import tech.pegasys.teku.phase1.simulation.NewSlot
import tech.pegasys.teku.phase1.simulation.ShardBlockProducer
import tech.pegasys.teku.phase1.simulation.util.SecretKeyRegistry
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.printRoot

class ShardProposer(
  eventBus: SendChannel<Eth2Event>,
  private val secretKeys: SecretKeyRegistry,
  private val eth1Engine: Eth1EngineClient,
  private val spec: Phase1Spec
) : Eth2Actor(eventBus) {

  private var recentSlot = GENESIS_SLOT
  private var recentShardHeads =
    List(INITIAL_ACTIVE_SHARDS.toInt()) { Root() to SignedShardBlock() }

  override suspend fun dispatchImpl(event: Eth2Event, scope: CoroutineScope) {
    when (event) {
      is NewSlot -> onNewSlot(event.slot)
      is NewShardHeads -> onNewShardHeads(event.shardHeads)
      is HeadAfterNewBeaconBlock -> onHeadAfterNewBeaconBlock(event.head)
    }
  }

  private fun onNewSlot(slot: Slot) {
    this.recentSlot = slot
  }

  private fun onNewShardHeads(heads: List<Pair<Root, SignedShardBlock>>) {
    this.recentShardHeads = heads

    // Update eth1-engine with new Eth1 heads
    if (ETH1_SHARD_NUMBER < heads.size.toULong()) {
      updateEth1ShardHead(heads[ETH1_SHARD_NUMBER.toInt()].second.message)
    }

    log("ShardProposer: shard heads updated to [${heads.joinToString { printRoot(it.first) }}]")
  }

  private fun updateEth1ShardHead(head: ShardBlock) {
    // Skip PHASE_1_GENESIS case
    if (head.body.size > 0) {
      val eth1BlockData = Eth1BlockData(head.body.toBytes())
      val response = eth1Engine.eth2_setHead(eth1BlockData.blockHash)
      if (response.result != true) {
        throw IllegalStateException(
          "Failed to eth2_setHead(hash=${printRoot(eth1BlockData.blockHash)}), " +
              "reason ${response.reason}"
        )
      }
    }
  }

  private suspend fun onHeadAfterNewBeaconBlock(head: BeaconHead) = coroutineScope {
    val newShardBlocks = (0uL until INITIAL_ACTIVE_SHARDS)
      .map { it to ShardBlockProducer(it, secretKeys, eth1Engine, spec) }
      .map {
        val (shardHeadRoot, shardHead) = recentShardHeads[it.first.toInt()]
        async { it.second.produce(recentSlot, head, shardHeadRoot, shardHead.message) }
      }.awaitAll()

    publish(NewShardBlocks(newShardBlocks))

    log("ShardProposer: New shard blocks proposed:\n${newShardBlocks.map { it.message }
      .joinToString("\n") { it.toString() }}\n")
  }
}
