package tech.pegasys.teku.phase1.simulation.actors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import tech.pegasys.teku.phase1.integration.datastructures.FullAttestation
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.onotole.phase1.DOMAIN_BEACON_ATTESTER
import tech.pegasys.teku.phase1.onotole.phase1.GENESIS_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.SLOTS_PER_EPOCH
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.simulation.BeaconHead
import tech.pegasys.teku.phase1.simulation.Eth2Actor
import tech.pegasys.teku.phase1.simulation.Eth2Event
import tech.pegasys.teku.phase1.simulation.NewAttestations
import tech.pegasys.teku.phase1.simulation.NewHead
import tech.pegasys.teku.phase1.simulation.NewShardHeads
import tech.pegasys.teku.phase1.simulation.NewSlot
import tech.pegasys.teku.phase1.simulation.NotCrosslinkedBlocksPublished
import tech.pegasys.teku.phase1.simulation.SlotTerminal
import tech.pegasys.teku.phase1.simulation.util.SecretKeyRegistry
import tech.pegasys.teku.phase1.simulation.util.computeAggregateAttestationByCommittee
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.logDebug

class BeaconAttester(
  eventBus: SendChannel<Eth2Event>,
  private val secretKeys: SecretKeyRegistry,
  private val spec: Phase1Spec
) : Eth2Actor(eventBus) {

  private var recentSlot = GENESIS_SLOT
  private var recentHead: BeaconHead? = null
  private var recentShardHeadRoots: List<Root>? = null
  private var recentShardBlocksToCrosslink: List<SignedShardBlock>? = null

  override suspend fun dispatchImpl(event: Eth2Event, scope: CoroutineScope) {
    when (event) {
      is NewSlot -> onNewSlot(event.slot)
      is NewHead -> onNewHead(event.head)
      is NewShardHeads -> onNewShardHeads(event.shardHeads)
      is NotCrosslinkedBlocksPublished -> onNotCrosslinkedBlocksPublished(event.blocks)
      is SlotTerminal -> onSlotTerminal()
    }
  }

  private fun onNewSlot(slot: Slot) {
    this.recentSlot = slot
  }

  private suspend fun onNewHead(head: BeaconHead) {
    this.recentHead = head
    attestIfReady()
  }

  private suspend fun onNewShardHeads(shardHeads: List<Pair<Root, SignedShardBlock>>) {
    this.recentShardHeadRoots = shardHeads.map { it.first }
    attestIfReady()
  }

  private suspend fun onNotCrosslinkedBlocksPublished(blocks: List<SignedShardBlock>) {
    this.recentShardBlocksToCrosslink = blocks
    attestIfReady()
  }

  private suspend fun attestIfReady() {
    if (readyToAttest()) {
      val attestations = computeAttestations()
      publish(NewAttestations(attestations))

      logDebug {
        "BeaconAttester: New attestations produced:" +
            "\n${attestations.joinToString("\n") { it.toStringFull() }}\n"
      }

      log(
        "BeaconAttester: New attestations produced:" +
            "\n${attestations.joinToString("\n") { it.toString() }}\n"
      )
    }
  }

  private fun readyToAttest(): Boolean =
    recentHead != null && recentShardHeadRoots != null && recentShardBlocksToCrosslink != null

  /**
   * Reset slot data
   */
  private fun onSlotTerminal() {
    this.recentHead = null
    this.recentShardHeadRoots = null
    this.recentShardBlocksToCrosslink = null
  }

  private suspend fun computeAttestations(): List<FullAttestation> = coroutineScope {
    val (headRoot, state) = recentHead!!
    val slot = recentSlot

    val epoch = spec.compute_epoch_at_slot(slot)
    val committeesPerSlot = spec.get_committee_count_per_slot(state, epoch)
    val activeValidatorIndices = spec.get_active_validator_indices(state, epoch)
    val seed = spec.get_seed(state, epoch, DOMAIN_BEACON_ATTESTER)
    val shardBlocks = recentShardBlocksToCrosslink!!.groupBy { it.message.shard }
      .map { e -> e.key to e.value.sortedBy { it.message.slot } }.toMap()

    (0uL until committeesPerSlot).map {
      async {
        val index = it
        val shard = spec.compute_shard_from_committee_index(state, index, slot)
        val committee = spec.compute_committee(
          indices = activeValidatorIndices,
          seed = seed,
          index = (((slot % SLOTS_PER_EPOCH) * committeesPerSlot) + index),
          count = (committeesPerSlot * SLOTS_PER_EPOCH)
        )

        computeAggregateAttestationByCommittee(
          index,
          committee,
          headRoot,
          state,
          recentShardHeadRoots!![shard.toInt()],
          shardBlocks[shard] ?: listOf(),
          secretKeys,
          spec
        )
      }
    }.awaitAll()
  }
}
