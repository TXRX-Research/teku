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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.MutableBeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.helpers.BeaconStateAccessors;
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
import tech.pegasys.teku.spec.logic.versions.rayonism.util.CommitteeUtilRayonism;

public class EpochProcessorRayonism extends AbstractEpochProcessor {

  private final CommitteeUtilRayonism committeeUtil;

  public EpochProcessorRayonism(
      final SpecConfig specConfig,
      final MiscHelpers miscHelpers,
      final BeaconStateAccessors beaconStateAccessors,
      final BeaconStateMutators beaconStateMutators,
      final ValidatorsUtil validatorsUtil,
      final BeaconStateUtil beaconStateUtil,
      final ValidatorStatusFactory validatorStatusFactory,
      CommitteeUtilRayonism committeeUtil) {
    super(
        specConfig,
        miscHelpers,
        beaconStateAccessors,
        beaconStateMutators,
        validatorsUtil,
        beaconStateUtil,
        validatorStatusFactory);
    this.committeeUtil = committeeUtil;
  }

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

  private void processPendingHeaders(MutableBeaconStateRayonism state) {

  }

  private void processConfirmedHeaderFees(MutableBeaconStateRayonism state) {

  }

  private void resetPendingHeaders(MutableBeaconStateRayonism state) {

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
            specConfig, state, validatorStatuses, miscHelpers, beaconStateAccessors);

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
