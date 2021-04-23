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

import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKey;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKeySchema;
import tech.pegasys.teku.ssz.containers.Container2;
import tech.pegasys.teku.ssz.containers.ContainerSchema2;
import tech.pegasys.teku.ssz.primitive.SszUInt64;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class DataCommitment extends Container2<DataCommitment, SszPublicKey, SszUInt64> {

  public static class DataCommitmentSchema
      extends ContainerSchema2<DataCommitment, SszPublicKey, SszUInt64> {

    public DataCommitmentSchema() {
      super(
          "DataCommitment",
          namedSchema("point", SszPublicKeySchema.INSTANCE),
          namedSchema("lenght", SszPrimitiveSchemas.UINT64_SCHEMA));
    }

    @Override
    public DataCommitment createFromBackingNode(TreeNode node) {
      return new DataCommitment(this, node);
    }
  }

  public static final DataCommitmentSchema SSZ_SCHEMA = new DataCommitmentSchema();

  private DataCommitment(DataCommitmentSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public DataCommitment() {
    super(SSZ_SCHEMA);
  }

  public DataCommitment(final BLSPublicKey point, final UInt64 length) {
    super(SSZ_SCHEMA, new SszPublicKey(point), SszUInt64.of(length));
  }

  public BLSPublicKey getPoint() {
    return getField0().getBLSPublicKey();
  }

  public UInt64 getLength() {
    return getField1().get();
  }
}
