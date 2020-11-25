package tech.pegasys.teku.phase1.integration.spec

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.datastructures.util.CommitteeUtil
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.PendingAttestation
import tech.pegasys.teku.phase1.onotole.deps.BLS
import tech.pegasys.teku.phase1.onotole.phase1.BASE_REWARDS_PER_EPOCH
import tech.pegasys.teku.phase1.onotole.phase1.BASE_REWARD_FACTOR
import tech.pegasys.teku.phase1.onotole.phase1.CommitteeIndex
import tech.pegasys.teku.phase1.onotole.phase1.Epoch
import tech.pegasys.teku.phase1.onotole.phase1.Gwei
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.pylib.get
import tech.pegasys.teku.phase1.onotole.pylib.len
import tech.pegasys.teku.phase1.onotole.pylib.min
import tech.pegasys.teku.phase1.onotole.pylib.range
import tech.pegasys.teku.phase1.onotole.pylib.set
import tech.pegasys.teku.phase1.onotole.pylib.toPyList
import tech.pegasys.teku.phase1.onotole.ssz.Sequence
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.Caches

open class OptimizedPhase1Spec(private val caches: Caches, bls: BLS) : Phase1Spec(bls) {

  override fun get_active_validator_indices(
    state: BeaconState,
    epoch: Epoch
  ): Sequence<ValidatorIndex> =
    caches.activeValidators.get(epoch) { super.get_active_validator_indices(state, epoch) }

  override fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex =
    caches.beaconProposerIndex.get(state.slot) { super.get_beacon_proposer_index(state) }

  override fun get_beacon_committee(
    state: BeaconState,
    slot: Slot,
    index: CommitteeIndex
  ): Sequence<ValidatorIndex> = caches.beaconCommittee.get(slot to index) {
    super.get_beacon_committee(state, slot, index)
  }

  override fun get_base_reward(state: BeaconState, index: ValidatorIndex): Gwei {
    val total_balance_squareroot = get_total_active_balance_with_root(state).second
    val effective_balance = state.validators[index].effective_balance
    return Gwei((((effective_balance * BASE_REWARD_FACTOR) / total_balance_squareroot) / BASE_REWARDS_PER_EPOCH))
  }

  override fun get_total_active_balance(state: BeaconState): Gwei {
    return get_total_active_balance_with_root(state).first
  }

  override fun compute_committee(
    indices: Sequence<ValidatorIndex>,
    seed: Bytes32,
    index: uint64,
    count: uint64
  ): Sequence<ValidatorIndex> {
    val start = ((len(indices) * index) / count)
    val end = ((len(indices) * (index + 1uL)) / count)
    return compute_committee_shuffle(indices, seed, start, end)
  }

  override fun get_inclusion_delay_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
    val rewards = range(
      len(
        state.validators
      )
    ).map { _ -> Gwei(0uL) }.toPyList()
    val matching_source_attestations =
      get_matching_source_attestations(state, get_previous_epoch(state))
    val attestations_by_validator_index =
      group_attestations_by_validator_index(state, matching_source_attestations)
    for (index in get_unslashed_attesting_indices(state, matching_source_attestations)) {
      val attestation =
        min(
          attestations_by_validator_index[index]!!,
          key = { a -> a.inclusion_delay })
      rewards[attestation.proposer_index] += get_proposer_reward(state, index)
      val max_attester_reward = (get_base_reward(state, index) - get_proposer_reward(state, index))
      rewards[index] += Gwei((max_attester_reward / attestation.inclusion_delay))
    }
    val penalties = range(
      len(state.validators)
    ).map { _ -> Gwei(0uL) }.toPyList()
    return Pair(rewards, penalties)
  }

  private fun get_total_active_balance_with_root(
    state: BeaconState
  ): Pair<Gwei, uint64> = caches.totalActiveBalance.get(get_current_epoch(state)) {
    val total_balance = get_total_balance(
      state,
      set(
        get_active_validator_indices(
          state,
          it
        )
      )
    )
    val squareroot = integer_squareroot(total_balance)
    total_balance to squareroot
  }

  private fun compute_committee_shuffle(
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
      CommitteeUtil.shuffle_list(
        arrayOfIndices,
        seed
      )
      arrayOfIndices.map {
        ValidatorIndex(
          it
        )
      }
    }.subList(fromIndex.toInt(), toIndex.toInt())
  }

  private fun group_attestations_by_validator_index(
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
}