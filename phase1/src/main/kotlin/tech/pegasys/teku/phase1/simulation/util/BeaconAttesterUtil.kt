package tech.pegasys.teku.phase1.simulation.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tech.pegasys.teku.phase1.integration.datastructures.AttestationData
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.Checkpoint
import tech.pegasys.teku.phase1.integration.datastructures.FullAttestation
import tech.pegasys.teku.phase1.integration.datastructures.FullAttestationData
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.integration.ssz.SSZBitlistImpl
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.phase1.CommitteeIndex
import tech.pegasys.teku.phase1.onotole.phase1.MAX_VALIDATORS_PER_COMMITTEE
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.ssz.SSZBitlist

suspend fun computeAggregateAttestationByCommittee(
  committeeIndex: CommitteeIndex,
  committee: List<ValidatorIndex>,
  headRoot: Root,
  headState: BeaconState,
  shardHeadRoot: Root,
  shardBlocksToCrosslink: List<SignedShardBlock>,
  secretKeys: SecretKeyRegistry,
  spec: Phase1Spec
): FullAttestation {
  val attestationData =
    computeAttestationData(
      committeeIndex,
      headRoot,
      headState,
      shardHeadRoot,
      shardBlocksToCrosslink,
      spec
    )

  val res: Pair<SSZBitlist, List<BLSSignature>>
  coroutineScope {
    res = committee.map {
      async { it to spec.get_attestation_signature(headState, AttestationData(attestationData), secretKeys[it]) }
    }.awaitAll()
      .fold<Pair<ValidatorIndex, BLSSignature>, Pair<SSZBitlist, MutableList<BLSSignature>>>(
        SSZBitlistImpl(MAX_VALIDATORS_PER_COMMITTEE) to mutableListOf<BLSSignature>()
      ) { acc, pair ->
        val indexWithinCommittee = committee.indexOf(pair.first).toULong()
        val updatedBitList = acc.first.set(indexWithinCommittee)
        acc.second.add(pair.second)
        updatedBitList to acc.second
      }
  }

  val (aggregationBits, signatureAggregate) = res.first to spec.bls.Aggregate(res.second)
  return FullAttestation(aggregationBits, attestationData, signatureAggregate)
}

fun computeAttestationData(
  committeeIndex: CommitteeIndex,
  headRoot: Root,
  headState: BeaconState,
  shardHeadRoot: Root,
  shardBlocksToCrosslink: List<SignedShardBlock>,
  spec: Phase1Spec
): FullAttestationData {
  val startSlot = spec.compute_start_slot_at_epoch(spec.get_current_epoch(headState))
  val shard = spec.compute_shard_from_committee_index(headState, committeeIndex, headState.slot)
  val epochBoundaryBlockRoot =
    if (startSlot == headState.slot) headRoot else spec.get_block_root(
      headState,
      spec.get_current_epoch(headState)
    )
  val shardTransition = spec.get_shard_transition(headState, shard, shardBlocksToCrosslink)
  return FullAttestationData(
    headState.slot,
    committeeIndex,
    headRoot,
    headState.current_justified_checkpoint,
    Checkpoint(epoch = spec.get_current_epoch(headState), root = epochBoundaryBlockRoot),
    shard,
    shardHeadRoot,
    shardTransition
  )
}
