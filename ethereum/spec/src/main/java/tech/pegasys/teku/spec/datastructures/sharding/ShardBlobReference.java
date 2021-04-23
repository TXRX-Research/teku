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
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.containers.Container4;
import tech.pegasys.teku.ssz.containers.ContainerSchema4;
import tech.pegasys.teku.ssz.primitive.SszBytes32;
import tech.pegasys.teku.ssz.primitive.SszUInt64;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class ShardBlobReference
    extends Container4<ShardBlobReference, SszUInt64, SszUInt64, SszBytes32, SszUInt64> {

  public static class ShardBlobReferenceSchema
      extends ContainerSchema4<ShardBlobReference, SszUInt64, SszUInt64, SszBytes32, SszUInt64> {

    public ShardBlobReferenceSchema() {
      super(
          "ShardBlobReference",
          namedSchema("slot", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("shard", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("body_root", SszPrimitiveSchemas.BYTES32_SCHEMA),
          namedSchema("proposer_index", SszPrimitiveSchemas.UINT64_SCHEMA));
    }

    @Override
    public ShardBlobReference createFromBackingNode(TreeNode node) {
      return new ShardBlobReference(this, node);
    }
  }

  public static final ShardBlobReferenceSchema SSZ_SCHEMA = new ShardBlobReferenceSchema();

  private ShardBlobReference(ShardBlobReferenceSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public ShardBlobReference(UInt64 slot, UInt64 shard, Bytes32 body_root, UInt64 proposer_index) {
    super(
        SSZ_SCHEMA,
        SszUInt64.of(slot),
        SszUInt64.of(shard),
        SszBytes32.of(body_root),
        SszUInt64.of(proposer_index));
  }

  public UInt64 getSlot() {
    return getField0().get();
  }

  public UInt64 getShard() {
    return getField1().get();
  }

  public Bytes32 getBodyRoot() {
    return getField2().get();
  }

  public UInt64 getProposerIndex() {
    return getField3().get();
  }
}
