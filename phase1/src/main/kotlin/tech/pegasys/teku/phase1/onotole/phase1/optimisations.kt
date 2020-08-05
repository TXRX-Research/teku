package tech.pegasys.teku.phase1.onotole.phase1

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.datastructures.util.CommitteeUtil
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.PendingAttestation
import tech.pegasys.teku.phase1.onotole.pylib.set
import tech.pegasys.teku.phase1.onotole.ssz.Sequence
import tech.pegasys.teku.phase1.onotole.ssz.uint64

fun Phase1Spec.group_attestations_by_validator_index(
    state: BeaconState,
    attestations: Sequence<PendingAttestation>
): Map<ValidatorIndex, Sequence<PendingAttestation>> {
  return attestations.groupBy { it.data.slot to it.data.index }
      .map { get_beacon_committee(state, it.key.first, it.key.second) to it.value }
      .map {
        it.second.map { a ->
          a.aggregation_bits.bitsSet().map { indexWithinCommittee ->
            it.first[indexWithinCommittee.toInt()]
          } to a
        }
      }
      .flatten().map {
        it.first.map { validatorIndex ->
          validatorIndex to it.second
        }
      }
      .flatten().groupBy({ it.first }, { it.second })
}

fun Phase1Spec.get_total_active_balance_with_root(
    state: BeaconState
): Pair<Gwei, uint64> = caches.totalActiveBalance.get(get_current_epoch(state)) {
  val total_balance = get_total_balance(state, set(get_active_validator_indices(state, it)))
  val squareroot = integer_squareroot(total_balance)
  total_balance to squareroot
}

fun Phase1Spec.compute_committee_shuffle(
    indices: List<ValidatorIndex>,
    seed: Bytes32,
    fromIndex: ValidatorIndex,
    toIndex: ValidatorIndex
): List<ValidatorIndex> {
  if (fromIndex < toIndex) {
    val index_count = indices.size.toULong()
    require(fromIndex < index_count)
    require(toIndex <= index_count)
  }
  return caches.committeeShuffle.get(seed) {
    val arrayOfIndices = indices.map { it.toInt() }.toIntArray()
    CommitteeUtil.shuffle_list(arrayOfIndices, seed)
    arrayOfIndices.map { ValidatorIndex(it) }
  }.subList(fromIndex.toInt(), toIndex.toInt())
}
