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

package tech.pegasys.teku.statetransition.sharding;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.sharding.PendingShardHeader;
import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.BeaconStateAccessorsRayonism;
import tech.pegasys.teku.spec.logic.versions.rayonism.util.CommitteeUtilRayonism;
import tech.pegasys.teku.ssz.SszData;
import tech.pegasys.teku.ssz.schema.SszListSchema;
import tech.pegasys.teku.statetransition.OperationPool;
import tech.pegasys.teku.statetransition.validation.OperationValidator;

public class ShardHeaderPool extends OperationPool<SignedShardBlobHeader> {

  private final Spec spec;

  public ShardHeaderPool(
      Function<UInt64, SszListSchema<SignedShardBlobHeader, ?>> slotToSszListSchemaSupplier,
      OperationValidator<SignedShardBlobHeader> operationValidator,
      Spec spec) {
    super(slotToSszListSchemaSupplier, operationValidator);
    this.spec = spec;
  }

  @Override
  protected int itemsLimit(BeaconState stateAtBlockSlot) {
    UInt64 stateEpoch = spec.getCurrentEpoch(stateAtBlockSlot);
    Optional<SpecConfigRayonism> maybeSpecConfigRayonism = spec.getSpecConfig(stateEpoch)
        .toVersionRayonism();
    Optional<BeaconStateAccessorsRayonism> maybeBeaconStateAccessorsRayonism = spec
        .atSlot(stateAtBlockSlot.getSlot()).beaconStateAccessors().toVersionRayonism();
    if (maybeSpecConfigRayonism.isEmpty() || maybeBeaconStateAccessorsRayonism.isEmpty()) {
      return 0;
    }
    SpecConfigRayonism specConfig = maybeSpecConfigRayonism.get();
    BeaconStateAccessorsRayonism stateAccessors = maybeBeaconStateAccessorsRayonism.get();
    return specConfig.getMaxShardHeadersPerShard() * stateAccessors.getActiveShardCount(
        stateAtBlockSlot, stateEpoch);
  }

  public Optional<Bytes32> getAttestedHeaderRoot(BeaconState state, UInt64 slot,
      UInt64 committeeIndex) {
    Optional<CommitteeUtilRayonism> maybeCommitteeUtilRayonism = spec.atSlot(slot)
        .getCommitteeUtil()
        .toVersionRayonism();
    if (maybeCommitteeUtilRayonism.isEmpty()) {
      return Optional.empty();
    }
    CommitteeUtilRayonism committeeUtil = maybeCommitteeUtilRayonism.get();
    UInt64 shard = committeeUtil
        .computeShardFromCommitteeIndex(state.toVersionRayonism().orElseThrow(), slot,
            committeeIndex);

    return state.toVersionRayonism().flatMap(rState -> {
      Stream<Bytes32> stateRoots = Stream.concat(
              rState.getCurrent_epoch_pending_shard_headers().stream(),
              rState.getPrevious_epoch_pending_shard_headers().stream()
          ).filter(h -> h.getSlot().equals(slot) && h.getShard().equals(shard))
              .map(PendingShardHeader::getRoot);
      Stream<Bytes32> poolRoots = getAll().stream()
          .map(SignedShardBlobHeader::getMessage)
          .filter(h -> h.getSlot().equals(slot) && h.getShard().equals(shard))
          .map(SszData::hashTreeRoot);
      // making 'empty' Bytes32.ZERO root the least suitable candidate
      // TODO add actual attestation logic
      return Stream.concat(stateRoots, poolRoots).max(Comparator.naturalOrder());
    });
  }
}
