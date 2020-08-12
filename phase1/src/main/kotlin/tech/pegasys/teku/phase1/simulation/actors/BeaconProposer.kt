package tech.pegasys.teku.phase1.simulation.actors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import tech.pegasys.teku.phase1.integration.datastructures.Attestation
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.FullAttestation
import tech.pegasys.teku.phase1.onotole.phase1.GENESIS_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.simulation.BeaconHead
import tech.pegasys.teku.phase1.simulation.Eth2Actor
import tech.pegasys.teku.phase1.simulation.Eth2Event
import tech.pegasys.teku.phase1.simulation.HeadAtTheBeginningOfNewSlot
import tech.pegasys.teku.phase1.simulation.NewBeaconBlock
import tech.pegasys.teku.phase1.simulation.NewSlot
import tech.pegasys.teku.phase1.simulation.PrevSlotAttestationsPublished
import tech.pegasys.teku.phase1.simulation.SlotTerminal
import tech.pegasys.teku.phase1.simulation.util.SecretKeyRegistry
import tech.pegasys.teku.phase1.simulation.util.produceBeaconBlock
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.logDebug
import tech.pegasys.teku.phase1.util.printRoot

class BeaconProposer(
  eventBus: SendChannel<Eth2Event>,
  private val secretKeys: SecretKeyRegistry,
  private val spec: Phase1Spec
) : Eth2Actor(eventBus) {

  private var recentSlot = GENESIS_SLOT
  private var recentHead: BeaconHead? = null
  private var recentAttestations: List<FullAttestation>? = null

  override suspend fun dispatchImpl(event: Eth2Event, scope: CoroutineScope) {
    when (event) {
      is NewSlot -> onNewSlot(event.slot)
      is PrevSlotAttestationsPublished -> onPrevSlotAttestationsPublished(event.attestations)
      is HeadAtTheBeginningOfNewSlot -> onHeadDuringThePreviousSlot(event.head)
      is SlotTerminal -> onSlotTerminal()
    }
  }

  private fun onNewSlot(slot: Slot) {
    this.recentSlot = slot
  }

  private suspend fun onPrevSlotAttestationsPublished(attestations: List<FullAttestation>) {
    this.recentAttestations = attestations
    proposeIfReady()
  }

  private suspend fun onHeadDuringThePreviousSlot(head: BeaconHead) {
    this.recentHead = head
    proposeIfReady()
  }

  private fun onSlotTerminal() {
    this.recentHead = null
    this.recentAttestations = null
  }

  private suspend fun proposeIfReady() {
    if (isReadyToPropose()) {
      logDebug { "Proposing a block atop of head(root=${printRoot(recentHead!!.root)})..." }
      proposeBlock(recentHead!!.root, recentHead!!.state, recentAttestations!!)
    }
  }

  private fun isReadyToPropose(): Boolean =
    recentSlot > GENESIS_SLOT && recentHead != null && recentAttestations != null

  private suspend fun proposeBlock(
    headRoot: Root,
    headState: BeaconState,
    recentAttestations: List<FullAttestation>
  ) {
    val attestations = recentAttestations.map { Attestation(it) }
    val shardTransitions = recentAttestations.map { it.data.shard_transition }
    val newBlock =
      produceBeaconBlock(
        headState,
        recentSlot,
        headRoot,
        attestations,
        shardTransitions,
        secretKeys,
        spec
      )
    logDebug { "Publishing ${NewBeaconBlock(newBlock)}..." }
    publish(NewBeaconBlock(newBlock))

    log("BeaconProposer: New block proposed\n${newBlock.message.toStringFull()}\n")
  }
}
