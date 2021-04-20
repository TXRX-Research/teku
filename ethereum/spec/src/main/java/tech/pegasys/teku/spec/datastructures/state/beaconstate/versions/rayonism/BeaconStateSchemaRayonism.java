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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader;
import tech.pegasys.teku.spec.datastructures.sharding.DataCommitment;
import tech.pegasys.teku.spec.datastructures.sharding.PendingShardHeader;
import tech.pegasys.teku.spec.datastructures.state.PendingAttestation;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconStateSchema;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.common.AbstractBeaconStateSchema;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.common.BeaconStateFields;
import tech.pegasys.teku.ssz.schema.SszListSchema;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.schema.SszVectorSchema;
import tech.pegasys.teku.ssz.sos.SszField;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class BeaconStateSchemaRayonism
    extends AbstractBeaconStateSchema<BeaconStateRayonism, MutableBeaconStateRayonism> {

  private static final int PREVIOUS_EPOCH_ATTESTATIONS_FIELD_INDEX = 15;
  private static final int CURRENT_EPOCH_ATTESTATIONS_FIELD_INDEX = 16;
  private static final int LATEST_EXECUTION_PAYLOAD_HEADER_FIELD_INDEX = 21;
  private static final int PREVIOUS_EPOCH_PENDING_SHARD_HEADERS_FIELD_INDEX = 22;
  private static final int CURRENT_EPOCH_PENDING_SHARD_HEADERS_FIELD_INDEX = 23;
  private static final int GRANDPARENT_EPOCH_CONFIRMED_COMMITMENTS_FIELD_INDEX = 24;
  private static final int SHARD_GASPRICE_FIELD_INDEX = 25;
  private static final int CURRENT_EPOCH_START_SHARD_FIELD_INDEX = 26;

  @VisibleForTesting
  BeaconStateSchemaRayonism(final SpecConfig specConfig) {
    super("BeaconStateRayonism", getUniqueFields(specConfig), specConfig);
  }

  public static BeaconStateSchemaRayonism create(final SpecConfig specConfig) {
    return new BeaconStateSchemaRayonism(specConfig);
  }

  private static List<SszField> getUniqueFields(final SpecConfig specConfig) {
    final SszField previousEpochAttestationsField =
        new SszField(
            PREVIOUS_EPOCH_ATTESTATIONS_FIELD_INDEX,
            BeaconStateFields.PREVIOUS_EPOCH_ATTESTATIONS.name(),
            () ->
                SszListSchema.create(
                    PendingAttestation.SSZ_SCHEMA,
                    (long) specConfig.getMaxAttestations() * specConfig.getSlotsPerEpoch()));
    final SszField currentEpochAttestationsField =
        new SszField(
            CURRENT_EPOCH_ATTESTATIONS_FIELD_INDEX,
            BeaconStateFields.CURRENT_EPOCH_ATTESTATIONS.name(),
            () ->
                SszListSchema.create(
                    PendingAttestation.SSZ_SCHEMA,
                    (long) specConfig.getMaxAttestations() * specConfig.getSlotsPerEpoch()));
    final SszField latestExecutionPayloadHeaderField =
        new SszField(
            LATEST_EXECUTION_PAYLOAD_HEADER_FIELD_INDEX,
            BeaconStateFields.LATEST_EXECUTION_PAYLOAD_HEADER.name(),
            () -> ExecutionPayloadHeader.SSZ_SCHEMA);

    SpecConfigRayonism specConfigRayonism = specConfig.toVersionRayonism().orElseThrow();
    int maxPendingShardHeaders = specConfigRayonism.getMaxShards() *
        specConfigRayonism.getMaxShardHeadersPerShard() * specConfig.getSlotsPerEpoch();

    final SszField previousEpochPendingShardHeadersField =
        new SszField(
            PREVIOUS_EPOCH_PENDING_SHARD_HEADERS_FIELD_INDEX,
            BeaconStateFields.PREVIOUS_EPOCH_PENDING_SHARD_HEADERS.name(),
            SszListSchema.create(PendingShardHeader.SSZ_SCHEMA, maxPendingShardHeaders));
    final SszField currentEpochPendingShardHeadersField =
        new SszField(
            CURRENT_EPOCH_PENDING_SHARD_HEADERS_FIELD_INDEX,
            BeaconStateFields.CURRENT_EPOCH_PENDING_SHARD_HEADERS.name(),
            SszListSchema.create(PendingShardHeader.SSZ_SCHEMA, maxPendingShardHeaders));
    final SszField grandparentEpochConfirmedCommitmentsField =
        new SszField(
            GRANDPARENT_EPOCH_CONFIRMED_COMMITMENTS_FIELD_INDEX,
            BeaconStateFields.GRANDPARENT_EPOCH_CONFIRMED_COMMITMENTS.name(),
            SszVectorSchema.create(
                SszVectorSchema.create(DataCommitment.SSZ_SCHEMA, specConfig.getSlotsPerEpoch()),
                specConfigRayonism.getMaxShards()));
    final SszField shardGaspriceField =
        new SszField(
            SHARD_GASPRICE_FIELD_INDEX,
            BeaconStateFields.SHARD_GASPRICE.name(),
            SszPrimitiveSchemas.UINT64_SCHEMA);
    final SszField currentEpochStartShardField =
        new SszField(
            CURRENT_EPOCH_START_SHARD_FIELD_INDEX,
            BeaconStateFields.CURRENT_EPOCH_START_SHARD.name(),
            SszPrimitiveSchemas.UINT64_SCHEMA);

    return List.of(
        previousEpochAttestationsField,
        currentEpochAttestationsField,
        latestExecutionPayloadHeaderField,
        previousEpochPendingShardHeadersField,
        currentEpochPendingShardHeadersField,
        grandparentEpochConfirmedCommitmentsField,
        shardGaspriceField,
        currentEpochStartShardField);
  }

  public static BeaconStateSchemaRayonism required(final BeaconStateSchema<?, ?> schema) {
    checkArgument(
        schema instanceof BeaconStateSchemaRayonism,
        "Expected a BeaconStateSchemaMerge but was %s",
        schema.getClass());
    return (BeaconStateSchemaRayonism) schema;
  }

  @SuppressWarnings("unchecked")
  public SszListSchema<PendingAttestation, ?> getPreviousEpochAttestationsSchema() {
    return (SszListSchema<PendingAttestation, ?>)
        getChildSchema(getFieldIndex(BeaconStateFields.PREVIOUS_EPOCH_ATTESTATIONS.name()));
  }

  @SuppressWarnings("unchecked")
  public SszListSchema<PendingAttestation, ?> getCurrentEpochAttestationsSchema() {
    return (SszListSchema<PendingAttestation, ?>)
        getChildSchema(getFieldIndex(BeaconStateFields.CURRENT_EPOCH_ATTESTATIONS.name()));
  }

  @Override
  public BeaconStateRayonism createFromBackingNode(TreeNode node) {
    return new BeaconStateRayonismImpl(this, node);
  }

  @Override
  public MutableBeaconStateRayonism createBuilder() {
    return new MutableBeaconStateRayonismImpl(createEmptyBeaconStateImpl(), true);
  }

  @Override
  public BeaconStateRayonism createEmpty() {
    return createEmptyBeaconStateImpl();
  }

  private BeaconStateRayonismImpl createEmptyBeaconStateImpl() {
    return new BeaconStateRayonismImpl(this);
  }
}
