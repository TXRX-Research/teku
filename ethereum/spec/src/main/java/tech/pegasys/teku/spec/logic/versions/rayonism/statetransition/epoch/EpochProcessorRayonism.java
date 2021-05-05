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

package tech.pegasys.teku.spec.logic.versions.rayonism.statetransition.epoch;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.sharding.DataCommitment;
import tech.pegasys.teku.spec.datastructures.sharding.PendingShardHeader;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateSchemaRayonism;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.MutableBeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.helpers.BeaconStateMutators;
import tech.pegasys.teku.spec.logic.common.helpers.MiscHelpers;
import tech.pegasys.teku.spec.logic.common.statetransition.epoch.AbstractEpochProcessor;
import tech.pegasys.teku.spec.logic.common.statetransition.epoch.RewardAndPenaltyDeltas;
import tech.pegasys.teku.spec.logic.common.statetransition.epoch.status.ValidatorStatusFactory;
import tech.pegasys.teku.spec.logic.common.statetransition.epoch.status.ValidatorStatuses;
import tech.pegasys.teku.spec.logic.common.statetransition.exceptions.EpochProcessingException;
import tech.pegasys.teku.spec.logic.common.util.BeaconStateUtil;
import tech.pegasys.teku.spec.logic.common.util.ValidatorsUtil;
import tech.pegasys.teku.spec.logic.versions.phase0.statetransition.epoch.RewardsAndPenaltiesCalculatorPhase0;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.BeaconStateAccessorsRayonism;
import tech.pegasys.teku.spec.logic.versions.rayonism.util.CommitteeUtilRayonism;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsRayonism;
import tech.pegasys.teku.ssz.SszList;
import tech.pegasys.teku.ssz.SszMutableList;
import tech.pegasys.teku.ssz.SszVector;

public class EpochProcessorRayonism extends AbstractEpochProcessor {

  private final CommitteeUtilRayonism committeeUtil;
  private final BeaconStateAccessorsRayonism beaconStateAccessorsRayonism;
  private final SpecConfigRayonism specConfigRayonism;
  private final SchemaDefinitionsRayonism schemaDefinitions;

  public EpochProcessorRayonism(
      final SpecConfig specConfig,
      final MiscHelpers miscHelpers,
      final BeaconStateAccessorsRayonism beaconStateAccessors,
      final BeaconStateMutators beaconStateMutators,
      final ValidatorsUtil validatorsUtil,
      final BeaconStateUtil beaconStateUtil,
      final ValidatorStatusFactory validatorStatusFactory,
      CommitteeUtilRayonism committeeUtil,
      SchemaDefinitionsRayonism schemaDefinitions) {
    super(
        specConfig,
        miscHelpers,
        beaconStateAccessors,
        beaconStateMutators,
        validatorsUtil,
        beaconStateUtil,
        validatorStatusFactory);
    this.committeeUtil = committeeUtil;
    this.beaconStateAccessorsRayonism = beaconStateAccessors;
    specConfigRayonism = SpecConfigRayonism.required(specConfig);
    this.schemaDefinitions = schemaDefinitions;
  }

  @Override
  protected void processEpoch(final BeaconState preState, final MutableBeaconState state)
      throws EpochProcessingException {
    final ValidatorStatuses validatorStatuses =
        validatorStatusFactory.createValidatorStatuses(preState);
    processJustificationAndFinalization(state, validatorStatuses.getTotalBalances());
    processInactivityUpdates(state, validatorStatuses);
    processRewardsAndPenalties(state, validatorStatuses);
    processRegistryUpdates(state, validatorStatuses.getStatuses());
    processSlashings(state, validatorStatuses.getTotalBalances().getCurrentEpochActiveValidators());

    // Sharding
    MutableBeaconStateRayonism beaconStateRayonism = MutableBeaconStateRayonism.required(state);
    processPendingHeaders(beaconStateRayonism);
    processConfirmedHeaderFees(beaconStateRayonism);
    resetPendingHeaders(beaconStateRayonism);

    // Final updates
    processEth1DataReset(state);
    processEffectiveBalanceUpdates(state);
    processSlashingsReset(state);
    processRandaoMixesReset(state);
    processHistoricalRootsUpdate(state);
    processParticipationUpdates(state);
    processSyncCommitteeUpdates(state);

    processShardEpochIncrement(beaconStateRayonism);
  }

  // def process_pending_headers(state: BeaconState) -> None:
  private void processPendingHeaders(MutableBeaconStateRayonism state) {
    // # Pending header processing applies to the previous epoch.
    // # Skip if `GENESIS_EPOCH` because no prior epoch to process.
    // if get_current_epoch(state) == GENESIS_EPOCH:
    //     return
    if (beaconStateAccessorsRayonism.getCurrentEpoch(state).equals(SpecConfig.GENESIS_EPOCH)) {
      return;
    }
    // previous_epoch_start_slot = compute_start_slot_at_epoch(get_previous_epoch(state))
    UInt64 previousEpoch = beaconStateAccessorsRayonism.getPreviousEpoch(state);
    UInt64 previousEpochStartSlot = miscHelpers.computeStartSlotAtEpoch(previousEpoch);
    // for slot in range(previous_epoch_start_slot, previous_epoch_start_slot + SLOTS_PER_EPOCH):
    UInt64 currentEpochStartSlot = previousEpochStartSlot.plus(specConfig.getSlotsPerEpoch());
    int activeShardCount = beaconStateAccessorsRayonism.getActiveShardCount(state, previousEpoch);
    SszMutableList<PendingShardHeader> previousEpochPendingShardHeaders =
        state.getPrevious_epoch_pending_shard_headers();
    for (UInt64 slot_ = previousEpochStartSlot;
        slot_.isLessThan(currentEpochStartSlot);
        slot_ = slot_.increment()) {
      final UInt64 slot = slot_;
      //     for shard in range(get_active_shard_count(state, previous_epoch)):
      for (UInt64 shard_ = UInt64.ZERO;
          shard_.isLessThan(activeShardCount);
          shard_ = shard_.increment()) {
        final UInt64 shard = shard_;
        //         # Pending headers for this (slot, shard) combo
        //         candidates = [
        //             c for c in state.previous_epoch_pending_shard_headers
        //             if (c.slot, c.shard) == (slot, shard)
        //         ]
        List<Pair<Integer, PendingShardHeader>> candidates =
            IntStream.range(0, previousEpochPendingShardHeaders.size())
                .mapToObj(i -> Pair.of(i, previousEpochPendingShardHeaders.get(i)))
                .filter(
                    p ->
                        p.getValue().getSlot().equals(slot)
                            && p.getValue().getShard().equals(shard))
                .collect(Collectors.toList());
        //         # If any candidates already confirmed, skip
        //         if True in [c.confirmed for c in candidates]:
        //             continue
        if (candidates.stream().anyMatch(p -> p.getValue().isConfirmed())) {
          continue;
        }
        //         # The entire committee (and its balance)
        //        index = compute_committee_index_from_shard(state, slot, shard)
        //        full_committee = get_beacon_committee(state, slot, index)
        UInt64 committeeIndex = committeeUtil.computeCommitteeIndexFromShard(state, slot, shard);
        List<Integer> fullCommittee = beaconStateUtil.getBeaconCommittee(state, slot, committeeIndex);
        //         # The set of voters who voted for each header (and their total balances)
        //         voting_sets = [
        //             [v for i, v in enumerate(full_committee) if c.votes[i]]
        //             for c in candidates
        //         ]
        //         voting_balances = [
        //             get_total_balance(state, voters)
        //             for voters in voting_sets
        //         ]
        List<UInt64> votingBalances =
            candidates.stream()
                .map(Pair::getValue)
                .map(shardHeader ->
                    IntStream.range(0, fullCommittee.size())
                        .filter(i -> shardHeader.getVotes().getBit(i))
                        .mapToObj(fullCommittee::get)
                        .collect(Collectors.toList()))
                .map(votingSet -> beaconStateAccessorsRayonism.getTotalBalance(state, votingSet))
                .collect(Collectors.toList());
        //         # Get the index with the most total balance voting for them.
        //         # NOTE: if two choices get exactly the same voting balance,
        //         # the candidate earlier in the list wins
        //         if max(voting_balances) > 0:
        //             winning_index = voting_balances.index(max(voting_balances))
        //         else:
        // TODO question: seems like the empty candidate always goes first in the list, so probably
        // no need for 'else'
        //             # If no votes, zero wins
        //             winning_index = [c.root for c in candidates].index(Root())
        // TODO: temp workaround for the first epoch with no empty pending headers
        Optional<UInt64> maybeMax = votingBalances.stream().max(Comparator.naturalOrder());
        if (maybeMax.isEmpty()) {
          continue;
        }
        UInt64 max = maybeMax.get();
        int winningIndex;
        if (max.isGreaterThan(0)) {
          winningIndex = votingBalances.indexOf(max);
        } else {
          winningIndex =
              candidates.stream()
                  .filter(p -> p.getValue().getRoot().equals(Bytes32.ZERO))
                  .findFirst()
                  .orElseThrow()
                  .getKey();
        }
        //         candidates[winning_index].confirmed = True
        PendingShardHeader headerToModify = candidates.get(winningIndex).getValue();
        int headerIndexToModify = candidates.get(winningIndex).getKey();
        PendingShardHeader updatedHeader = new PendingShardHeader(headerToModify, true);
        previousEpochPendingShardHeaders.set(headerIndexToModify, updatedHeader);
      }
    }
    // for slot_index in range(SLOTS_PER_EPOCH):
    //     for shard in range(MAX_SHARDS):
    //         state.grandparent_epoch_confirmed_commitments[shard][slot_index] = DataCommitment()
    // confirmed_headers = [candidate for candidate in state.previous_epoch_pending_shard_headers if
    // candidate.confirmed]
    // for header in confirmed_headers:
    //     state.grandparent_epoch_confirmed_commitments[c.shard][c.slot % SLOTS_PER_EPOCH] =
    // c.commitment
    BeaconStateSchemaRayonism beaconStateSchemaRayonism =
        BeaconStateSchemaRayonism.required(schemaDefinitions.getBeaconStateSchema());
    Map<Pair<Integer, Integer>, DataCommitment> confirmedHeaders =
        previousEpochPendingShardHeaders.stream()
            .filter(PendingShardHeader::isConfirmed)
            .collect(
                Collectors.toMap(
                    h ->
                        Pair.of(
                            h.getSlot().mod(specConfig.getSlotsPerEpoch()).intValue(),
                            h.getShard().intValue()),
                    PendingShardHeader::getCommitment));

    SszVector<SszVector<DataCommitment>> confirmedCommitments =
        IntStream.range(0, specConfigRayonism.getMaxShards())
            .mapToObj(
                 shard ->
                    IntStream.range(0, specConfig.getSlotsPerEpoch())
                        .mapToObj(
                            slot_index ->
                                confirmedHeaders.getOrDefault(
                                    Pair.of(slot_index, shard), new DataCommitment()))
                        .collect(
                            beaconStateSchemaRayonism
                                .getGrandparentEpochConfirmedCommitmentsElementSchema()
                                .collector()))
            .collect(
                beaconStateSchemaRayonism
                    .getGrandparentEpochConfirmedCommitmentsSchema()
                    .collector());
    state.setGrandparent_epoch_confirmed_commitments(confirmedCommitments);
  }

  // def charge_confirmed_header_fees(state: BeaconState) -> None:
  private void processConfirmedHeaderFees(MutableBeaconStateRayonism state) {
    //    new_gasprice = state.shard_gasprice
    UInt64 newGasPrice = state.getShard_gasprice();
    //    adjustment_quotient = (
    //        get_active_shard_count(state, get_current_epoch(state))
    //        * SLOTS_PER_EPOCH * GASPRICE_ADJUSTMENT_COEFFICIENT
    //    )
    int activeShardCountCur =
        beaconStateAccessorsRayonism.getActiveShardCount(
            state, beaconStateAccessorsRayonism.getCurrentEpoch(state));
    int adjustmentQuotient =
        activeShardCountCur
            * specConfig.getSlotsPerEpoch()
            * specConfigRayonism.getGaspriceAdjustmentCoefficient();
    //    previous_epoch = get_previous_epoch(state)
    //    previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch)
    UInt64 previousEpoch = beaconStateAccessorsRayonism.getPreviousEpoch(state);
    UInt64 previousEpochStartSlot = miscHelpers.computeStartSlotAtEpoch(previousEpoch);
    UInt64 currentEpochStartSlot = previousEpochStartSlot.plus(specConfig.getSlotsPerEpoch());
    int activeShardCount = beaconStateAccessorsRayonism.getActiveShardCount(state, previousEpoch);
    SszList<PendingShardHeader> previousEpochPendingShardHeaders =
        state.getPrevious_epoch_pending_shard_headers();
    //    for slot in range(previous_epoch_start_slot, previous_epoch_start_slot + SLOTS_PER_EPOCH):
    for (UInt64 slot_ = previousEpochStartSlot;
        slot_.isLessThan(currentEpochStartSlot);
        slot_ = slot_.increment()) {
      final UInt64 slot = slot_;
      // for shard in range(get_active_shard_count(state, previous_epoch)):
      for (UInt64 shard_ = UInt64.ZERO;
          shard_.isLessThan(activeShardCount);
          shard_ = shard_.increment()) {
        final UInt64 shard = shard_;
        // confirmed_candidates = [
        //     c for c in state.previous_epoch_pending_shard_headers
        //     if (c.slot, c.shard, c.confirmed) == (slot, shard, True)
        // ]
        Optional<PendingShardHeader> maybeCandidate =
            previousEpochPendingShardHeaders.stream()
                .filter(
                    h -> h.getSlot().equals(slot) && h.getShard().equals(shard) && h.isConfirmed())
                .findFirst();
        // if not any(confirmed_candidates):
        //     continue
        if (maybeCandidate.isEmpty()) {
          continue;
        }
        // candidate = confirmed_candidates[0]
        PendingShardHeader candidate = maybeCandidate.get();

        // # Charge EIP 1559 fee
        // proposer = get_shard_proposer_index(state, slot, shard)
        int proposer = committeeUtil.getShardProposerIndex(state, slot, shard);
        // fee = (
        //     (state.shard_gasprice * candidate.commitment.length)
        //     // TARGET_SAMPLES_PER_BLOCK
        // )
        UInt64 fee =
            state
                .getShard_gasprice()
                .times(candidate.getCommitment().getLength())
                .dividedBy(specConfigRayonism.getTargetSamplesPerBlock());
        // decrease_balance(state, proposer, fee)
        beaconStateMutators.decreaseBalance(state, proposer, fee);

        // # Track updated gas price
        // new_gasprice = compute_updated_gasprice(
        //     new_gasprice,
        //     candidate.commitment.length,
        //     adjustment_quotient,
        // )
        newGasPrice =
            computeUpdatedGasprice(
                newGasPrice, candidate.getCommitment().getLength(), adjustmentQuotient);
      }
    }
    //    state.shard_gasprice = new_gasprice
    state.setShard_gasprice(newGasPrice);
  }

  // def compute_updated_gasprice(prev_gasprice: Gwei, shard_block_length: uint64,
  // adjustment_quotient: uint64) -> Gwei:
  UInt64 computeUpdatedGasprice(
      UInt64 preevGasprice, UInt64 shardBlockLength, int adjustmentQuotient) {
    //  if shard_block_length > TARGET_SAMPLES_PER_BLOCK:
    if (shardBlockLength.isGreaterThan(specConfigRayonism.getTargetSamplesPerBlock())) {
      //      delta = max(1, prev_gasprice * (shard_block_length - TARGET_SAMPLES_PER_BLOCK)
      //                     // TARGET_SAMPLES_PER_BLOCK // adjustment_quotient)
      UInt64 delta =
          preevGasprice
              .times(shardBlockLength.minus(specConfigRayonism.getTargetSamplesPerBlock()))
              .dividedBy(specConfigRayonism.getTargetSamplesPerBlock())
              .dividedBy(adjustmentQuotient)
              .max(UInt64.ONE);
      //      return min(prev_gasprice + delta, MAX_GASPRICE)
      return preevGasprice.plus(delta).min(specConfigRayonism.getMaxGasprice());
      //  else:
    } else {
      //      delta = max(1, prev_gasprice * (TARGET_SAMPLES_PER_BLOCK - shard_block_length)
      //                     // TARGET_SAMPLES_PER_BLOCK // adjustment_quotient)
      UInt64 delta =
          preevGasprice
              .times(specConfigRayonism.getTargetSamplesPerBlock().minus(shardBlockLength))
              .dividedBy(specConfigRayonism.getTargetSamplesPerBlock())
              .dividedBy(adjustmentQuotient)
              .max(UInt64.ONE);
      //      return max(prev_gasprice, MIN_GASPRICE + delta) - delta
      return preevGasprice.max(specConfigRayonism.getMinGasprice().plus(delta)).minus(delta);
    }
  }

  //  def reset_pending_headers(state: BeaconState) -> None:
  private void resetPendingHeaders(MutableBeaconStateRayonism state) {
    //  state.previous_epoch_pending_shard_headers = state.current_epoch_pending_shard_headers
    state.setPrevious_epoch_pending_shard_headers(state.getCurrent_epoch_pending_shard_headers());
    //  state.current_epoch_pending_shard_headers = []
    state.getCurrent_epoch_pending_shard_headers().clear();
    //  # Add dummy "empty" PendingShardHeader (default vote for if no shard header available)
    //  next_epoch = get_current_epoch(state) + 1
    UInt64 nextEpoch = beaconStateAccessorsRayonism.getCurrentEpoch(state).increment();
    //  next_epoch_start_slot = compute_start_slot_at_epoch(next_epoch)
    UInt64 nextEpochStartSlot = miscHelpers.computeStartSlotAtEpoch(nextEpoch);
    //  for slot in range(next_epoch_start_slot, next_epoch_start_slot + SLOTS_IN_EPOCH):
    for (UInt64 slot = nextEpochStartSlot;
        slot.isLessThan(nextEpochStartSlot.plus(specConfig.getSlotsPerEpoch()));
        slot = slot.increment()) {
      //      for index in range(get_committee_count_per_slot(next_epoch)):
      UInt64 committeeCountPerSlot = committeeUtil.getCommitteeCountPerSlot(state, nextEpoch);
      for (UInt64 index = UInt64.ZERO;
          index.isLessThan(committeeCountPerSlot);
          index = index.increment()) {
        // shard = compute_shard_from_committee_index(state, slot, index)
        UInt64 shard = committeeUtil.computeShardFromCommitteeIndex(state, slot, index);
        // committee_length = len(get_beacon_committee(state, slot, shard))
        int committeeLength = beaconStateUtil.getBeaconCommittee(state, slot, index).size();
        // state.current_epoch_pending_shard_headers.append(PendingShardHeader(
        //     slot=slot,
        //     shard=shard,
        //     commitment=DataCommitment(),
        //  TODO question: should be ShardBlobHeader(slot, shard).hashTreeRott() ?
        // https://github.com/ethereum/eth2.0-specs/pull/2368
        //     root=Root(),
        //
        //     votes=Bitlist[MAX_VALIDATORS_PER_COMMITTEE]([0] * committee_length),
        //     confirmed=False,
        // ))
        PendingShardHeader emptyPendingShardHeader =
            new PendingShardHeader(
                slot,
                shard,
                new DataCommitment(),
                Bytes32.ZERO,
                PendingShardHeader.SSZ_SCHEMA.getVotesSchema().ofBits(committeeLength),
                false);
        state.getCurrent_epoch_pending_shard_headers().append(emptyPendingShardHeader);
      }
    }
  }

  private void processShardEpochIncrement(MutableBeaconStateRayonism state) {
    //    # Update current_epoch_start_shard
    //    state.current_epoch_start_shard = get_start_shard(state, Slot(state.slot + 1))
    UInt64 newStartShard = committeeUtil.getStartShard(state, state.getSlot().increment());
    state.setCurrent_epoch_start_shard(newStartShard);
  }

  @Override
  public RewardAndPenaltyDeltas getRewardAndPenaltyDeltas(
      BeaconState state, ValidatorStatuses validatorStatuses) {
    final RewardsAndPenaltiesCalculatorPhase0 calculator =
        new RewardsAndPenaltiesCalculatorPhase0(
            specConfig, state, validatorStatuses, miscHelpers, beaconStateAccessorsRayonism);

    return calculator.getDeltas();
  }

  @Override
  public void processParticipationUpdates(MutableBeaconState genericState) {
    // Rotate current/previous epoch attestations
    final MutableBeaconStateRayonism state = MutableBeaconStateRayonism.required(genericState);
    state.getPrevious_epoch_attestations().setAll(state.getCurrent_epoch_attestations());
    state.getCurrent_epoch_attestations().clear();
  }

  @Override
  public void processSyncCommitteeUpdates(final MutableBeaconState state) {
    // Nothing to do
  }
}
