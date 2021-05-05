/*
 * Copyright 2021 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.spec.logic.versions.rayonism.util;

import java.util.List;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.helpers.MathHelpers;
import tech.pegasys.teku.spec.logic.common.util.CommitteeUtil;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.BeaconStateAccessorsRayonism;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.MiscHelpersRayonism;

public class CommitteeUtilRayonism extends CommitteeUtil {

  private final SpecConfigRayonism specConfig;
  private final BeaconStateAccessorsRayonism stateAccessorsRayonism;
  private final MiscHelpersRayonism miscHelpers;

  public CommitteeUtilRayonism(
      SpecConfigRayonism specConfig,
      BeaconStateAccessorsRayonism beaconStateAccessors,
      MiscHelpersRayonism miscHelpersRayonism) {
    super(specConfig, beaconStateAccessors);
    this.specConfig = specConfig;
    this.stateAccessorsRayonism = beaconStateAccessors;
    this.miscHelpers = miscHelpersRayonism;
  }

  public UInt64 getCommitteeCountPerSlot(BeaconState state, UInt64 epoch) {
    // return max(uint64(1), min(
    //     get_active_shard_count(state, epoch),
    //     uint64(len(get_active_validator_indices(state, epoch))) // SLOTS_PER_EPOCH // TARGET_COMMITTEE_SIZE,
    // ))
    List<Integer> active_validator_indices = stateAccessors.getActiveValidatorIndices(state, epoch);
    return UInt64.valueOf(
        Math.max(
            1,
            Math.min(
                stateAccessorsRayonism.getActiveShardCount(state, epoch),
                Math.floorDiv(
                    Math.floorDiv(active_validator_indices.size(), specConfig.getSlotsPerEpoch()),
                    specConfig.getTargetCommitteeSize()))));
  }

  //  def compute_committee_source_epoch(epoch: Epoch, period: uint64) -> Epoch:
  //    """
  //    Return the source epoch for computing the committee.
  //    """
  public UInt64 computeCommitteeSourceEpoch(UInt64 epoch, UInt64 period) {
    //    source_epoch = Epoch(epoch - epoch % period)
    //    if source_epoch >= period:
    //    source_epoch -= period  # `period` epochs lookahead
    //    return source_epoch
    UInt64 sourceEpoch = epoch.minus(epoch.mod(period));
    return sourceEpoch.isGreaterThanOrEqualTo(period) ? sourceEpoch.minus(period) : sourceEpoch;
  }

  //  def get_shard_committee(beacon_state: BeaconState, epoch: Epoch, shard: Shard) ->
  // Sequence[ValidatorIndex]:
  //      """
  //    Return the shard committee of the given ``epoch`` of the given ``shard``.
  //    """
  public List<Integer> getShardCommittee(BeaconState state, UInt64 epoch, UInt64 shard) {
    //  source_epoch = compute_committee_source_epoch(epoch, SHARD_COMMITTEE_PERIOD)
    UInt64 sourceEpoch = computeCommitteeSourceEpoch(epoch, specConfig.getShardCommitteePeriod());
    //  active_validator_indices = get_active_validator_indices(beacon_state, source_epoch)
    List<Integer> activeValidatorIndices =
        stateAccessorsRayonism.getActiveValidatorIndices(state, sourceEpoch);
    //  seed = get_seed(beacon_state, source_epoch, DOMAIN_SHARD_COMMITTEE)
    Bytes32 seed =
        stateAccessorsRayonism.getSeed(state, sourceEpoch, specConfig.getDomainShardCommittee());
    //    return compute_committee(
    //      indices=active_validator_indices,
    //      seed=seed,
    //      index=shard,
    //      count=get_active_shard_count(beacon_state, epoch),
    //    )
    int activeShardCount = stateAccessorsRayonism.getActiveShardCount(state, epoch);
    return computeCommittee(
        state, activeValidatorIndices, seed, shard.intValue(), activeShardCount);
  }

  //  def get_shard_proposer_index(beacon_state: BeaconState, slot: Slot, shard: Shard) ->
  // ValidatorIndex:
  //      """
  //    Return the proposer's index of shard block at ``slot``.
  //    """
  public int getShardProposerIndex(BeaconStateRayonism state, UInt64 slot, UInt64 shard) {
    //  epoch = compute_epoch_at_slot(slot)
    UInt64 epoch = miscHelpers.computeEpochAtSlot(slot);
    //  committee = get_shard_committee(beacon_state, epoch, shard)
    List<Integer> committee = getShardCommittee(state, epoch, shard);
    // uint_to_bytes(beacon_state.slot))
    Bytes32 epochSeed =
        stateAccessorsRayonism.getSeed(state, epoch, specConfig.getDomainShardProposer());
    Bytes32 seed =
        Hash.sha2_256(Bytes.concatenate(epochSeed, MathHelpers.uintToBytes(slot)));
    //      # Proposer must have sufficient balance to pay for worst case fee burn
    // TODO question: https://github.com/ethereum/eth2.0-specs/pull/2386
    //  EFFECTIVE_BALANCE_MAX_DOWNWARD_DEVIATION = (
    //      (EFFECTIVE_BALANCE_INCREMENT - EFFECTIVE_BALANCE_INCREMENT)
    //      * HYSTERESIS_DOWNWARD_MULTIPLIER // HYSTERESIS_QUOTIENT
    //    )
    UInt64 effectiveBalanceMaxDownwardDeviation =
        specConfig
            .getEffectiveBalanceIncrement()
            .minus(
                specConfig
                    .getEffectiveBalanceIncrement()
                    .times(specConfig.getHysteresisDownwardMultiplier())
                    .dividedBy(specConfig.getHysteresisQuotient()));
    //  min_effective_balance = (
    //  beacon_state.shard_gasprice * MAX_SAMPLES_PER_BLOCK // TARGET_SAMPLES_PER_BLOCK
    //        + EFFECTIVE_BALANCE_MAX_DOWNWARD_DEVIATION
    //    )
    UInt64 minEffectiveBalance =
        state
            .getShard_gasprice()
            .times(specConfig.getMaxSamplesPerBlock())
            .dividedBy(specConfig.getTargetSamplesPerBlock())
            .plus(effectiveBalanceMaxDownwardDeviation);
    //        return compute_proposer_index(beacon_state, committee, seed, min_effective_balance)
    return miscHelpers.computeProposerIndex(state, committee, seed, minEffectiveBalance);
  }

  //  def compute_shard_from_committee_index(state: BeaconState, slot: Slot, index: CommitteeIndex)
  // -> Shard:
  public UInt64 computeShardFromCommitteeIndex(
      BeaconStateRayonism state, UInt64 slot, UInt64 index) {
    //  active_shards = get_active_shard_count(state, compute_epoch_at_slot(slot))
    int activeShards =
        stateAccessorsRayonism.getActiveShardCount(state, miscHelpers.computeEpochAtSlot(slot));
    //  return Shard((index + get_start_shard(state, slot)) % active_shards)
    return index.plus(getStartShard(state, slot)).mod(activeShards);
  }

  //  def compute_committee_index_from_shard(state: BeaconState, slot: Slot, shard: Shard) ->
  // CommitteeIndex:
  public UInt64 computeCommitteeIndexFromShard(
      BeaconStateRayonism state, UInt64 slot, UInt64 shard) {
    //  active_shards = get_active_shard_count(state, compute_epoch_at_slot(slot))
    int activeShards =
        stateAccessorsRayonism.getActiveShardCount(state, miscHelpers.computeEpochAtSlot(slot));
    //  return CommitteeIndex((active_shards + shard - get_start_shard(state, slot)) %
    // active_shards)
    return UInt64.valueOf(activeShards)
        .plus(shard)
        .minus(getStartShard(state, slot))
        .mod(activeShards);
  }

  //  def get_start_shard(state: BeaconState, slot: Slot) -> Shard:
  //      """
  //    Return the start shard at ``slot``.
  //    """
  public UInt64 getStartShard(final BeaconStateRayonism state, final UInt64 slot) {
    UInt64 currentEpochStartSlot =
        miscHelpers.computeStartSlotAtEpoch(stateAccessorsRayonism.getCurrentEpoch(state));
    //  shard = state.current_epoch_start_shard
    UInt64 shard = state.getCurrent_epoch_start_shard();
    //  if slot > current_epoch_start_slot:
    if (slot.isGreaterThan(currentEpochStartSlot)) {
      //  # Current epoch or the next epoch lookahead
      //  for _slot in range(current_epoch_start_slot, slot):
      for (UInt64 iSlot = currentEpochStartSlot;
          iSlot.isLessThan(slot);
          iSlot = iSlot.increment()) {
        UInt64 epoch = miscHelpers.computeEpochAtSlot(iSlot);
        // committee_count = get_committee_count_per_slot(state, compute_epoch_at_slot(Slot(_slot)))
        UInt64 committeeCount = getCommitteeCountPerSlot(state, epoch);
        // active_shard_count = get_active_shard_count(state, compute_epoch_at_slot(Slot(_slot)))
        int activeShardCount = stateAccessorsRayonism.getActiveShardCount(state, epoch);
        // shard = (shard + committee_count) % active_shard_count
        shard = shard.plus(committeeCount).mod(activeShardCount);
      }
      //  elif slot < current_epoch_start_slot:
    } else if (slot.isLessThan(currentEpochStartSlot)) {
      // # Previous epoch
      // for _slot in list(range(slot, current_epoch_start_slot))[::-1]:
      for (UInt64 iSlot = slot;
          iSlot.isLessThan(currentEpochStartSlot);
          iSlot = iSlot.increment()) {
        UInt64 epoch = miscHelpers.computeEpochAtSlot(iSlot);
        //   committee_count = get_committee_count_per_slot(state,
        // compute_epoch_at_slot(Slot(_slot)))
        UInt64 committeeCount = getCommitteeCountPerSlot(state, epoch);
        //   active_shard_count = get_active_shard_count(state, compute_epoch_at_slot(Slot(_slot)))
        int activeShardCount = stateAccessorsRayonism.getActiveShardCount(state, epoch);
        //   # Ensure positive
        //   shard = (shard + active_shard_count - committee_count) % active_shard_count
        shard = shard.plus(activeShardCount).minus(committeeCount).mod(activeShardCount);
      }
    }
    //  return Shard(shard)
    return shard;
  }

  @Override
  public Optional<CommitteeUtilRayonism> toVersionRayonism() {
    return Optional.of(this);
  }
}
