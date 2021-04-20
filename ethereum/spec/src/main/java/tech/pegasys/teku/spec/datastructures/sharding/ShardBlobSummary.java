/*
 * Copyright 2020 ConsenSys AG.
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

package tech.pegasys.teku.spec.datastructures.sharding;

import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKey;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKeySchema;
import tech.pegasys.teku.ssz.containers.Container4;
import tech.pegasys.teku.ssz.containers.ContainerSchema4;
import tech.pegasys.teku.ssz.primitive.SszBytes32;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class ShardBlobSummary
    extends Container4<ShardBlobSummary, DataCommitment, SszPublicKey, SszBytes32, SszBytes32> {

  public static class ShardBlobSummarySchema
      extends ContainerSchema4<
          ShardBlobSummary, DataCommitment, SszPublicKey, SszBytes32, SszBytes32> {

    public ShardBlobSummarySchema() {
      super(
          "ShardBlobSummary",
          namedSchema("commitment", DataCommitment.SSZ_SCHEMA),
          namedSchema("degree_proof", SszPublicKeySchema.INSTANCE),
          namedSchema("data_root", SszPrimitiveSchemas.BYTES32_SCHEMA),
          namedSchema("beacon_block_root", SszPrimitiveSchemas.BYTES32_SCHEMA));
    }

    @Override
    public ShardBlobSummary createFromBackingNode(TreeNode node) {
      return new ShardBlobSummary(this, node);
    }
  }

  public static final ShardBlobSummarySchema SSZ_SCHEMA = new ShardBlobSummarySchema();

  private ShardBlobSummary(ShardBlobSummarySchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public ShardBlobSummary(
      DataCommitment commitment,
      BLSPublicKey degree_proof,
      Bytes32 data_root,
      Bytes32 beacon_block_root) {
    super(
        SSZ_SCHEMA,
        commitment,
        new SszPublicKey(degree_proof),
        SszBytes32.of(data_root),
        SszBytes32.of(beacon_block_root));
  }

  public DataCommitment getCommitment() {
    return getField0();
  }

  public BLSPublicKey getDegreeProof() {
    return getField1().getBLSPublicKey();
  }

  public Bytes32 getDataRoot() {
    return getField2().get();
  }

  public Bytes32 getBeaconBlockRoot() {
    return getField3().get();
  }
}
