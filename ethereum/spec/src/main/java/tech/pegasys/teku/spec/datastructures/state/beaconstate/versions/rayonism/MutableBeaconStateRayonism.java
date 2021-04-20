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

package tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism;

import java.util.Optional;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader;
import tech.pegasys.teku.spec.datastructures.sharding.DataCommitment;
import tech.pegasys.teku.spec.datastructures.sharding.PendingShardHeader;
import tech.pegasys.teku.spec.datastructures.state.PendingAttestation;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.common.BeaconStateFields;
import tech.pegasys.teku.ssz.SszMutableList;
import tech.pegasys.teku.ssz.SszMutableVector;
import tech.pegasys.teku.ssz.SszVector;
import tech.pegasys.teku.ssz.primitive.SszUInt64;

public interface MutableBeaconStateRayonism extends MutableBeaconState, BeaconStateRayonism {

  static MutableBeaconStateRayonism required(final MutableBeaconState state) {
    return state
        .toMutableVersionRayonism()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Expected a merge state but got: " + state.getClass().getSimpleName()));
  }

  // Attestations
  @Override
  default SszMutableList<PendingAttestation> getPrevious_epoch_attestations() {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.PREVIOUS_EPOCH_ATTESTATIONS.name());
    return getAnyByRef(fieldIndex);
  }

  @Override
  default SszMutableList<PendingAttestation> getCurrent_epoch_attestations() {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.CURRENT_EPOCH_ATTESTATIONS.name());
    return getAnyByRef(fieldIndex);
  }

  // Execution
  default void setLatestExecutionPayloadHeader(ExecutionPayloadHeader executionPayloadHeader) {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.LATEST_EXECUTION_PAYLOAD_HEADER.name());
    set(fieldIndex, executionPayloadHeader);
  }

  // Sharding
  @Override
  default SszMutableList<PendingShardHeader> getPrevious_epoch_pending_shard_headers() {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.PREVIOUS_EPOCH_PENDING_SHARD_HEADERS.name());
    return getAnyByRef(fieldIndex);
  }

  @Override
  default SszMutableList<PendingShardHeader> getCurrent_epoch_pending_shard_headers() {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.CURRENT_EPOCH_PENDING_SHARD_HEADERS.name());
    return getAnyByRef(fieldIndex);
  }

  @Override
  default SszMutableVector<SszVector<DataCommitment>> getGrandparent_epoch_confirmed_commitments() {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.GRANDPARENT_EPOCH_CONFIRMED_COMMITMENTS.name());
    return getAnyByRef(fieldIndex);
  }

  default void setShard_gasprice(UInt64 shardPrice) {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.SHARD_GASPRICE.name());
    set(fieldIndex, SszUInt64.of(shardPrice));
  }

  default void setCurrent_epoch_start_shard(UInt64 currentEpochStartShard) {
    final int fieldIndex =
        getSchema().getFieldIndex(BeaconStateFields.CURRENT_EPOCH_START_SHARD.name());
    set(fieldIndex, SszUInt64.of(currentEpochStartShard));
  }

  @Override
  BeaconStateRayonism commitChanges();

  @Override
  default Optional<MutableBeaconStateRayonism> toMutableVersionRayonism() {
    return Optional.of(this);
  }
}
