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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.containers.Container4;
import tech.pegasys.teku.ssz.containers.ContainerSchema4;
import tech.pegasys.teku.ssz.primitive.SszUInt64;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class ShardBlobHeader
    extends Container4<ShardBlobHeader, SszUInt64, SszUInt64, ShardBlobSummary, SszUInt64> {

  public static class ShardBlobHeaderSchema
      extends ContainerSchema4<ShardBlobHeader, SszUInt64, SszUInt64, ShardBlobSummary, SszUInt64> {

    public ShardBlobHeaderSchema() {
      super(
          "ShardBlobHeader",
          namedSchema("slot", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("shard", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("body_summary", ShardBlobSummary.SSZ_SCHEMA),
          namedSchema("proposer_index", SszPrimitiveSchemas.UINT64_SCHEMA));
    }

    @Override
    public ShardBlobHeader createFromBackingNode(TreeNode node) {
      return new ShardBlobHeader(this, node);
    }
  }

  public static final ShardBlobHeaderSchema SSZ_SCHEMA = new ShardBlobHeaderSchema();

  private ShardBlobHeader(ShardBlobHeaderSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public ShardBlobHeader(
      UInt64 slot, UInt64 shard, ShardBlobSummary body_summary, UInt64 proposer_index) {
    super(
        SSZ_SCHEMA,
        SszUInt64.of(slot),
        SszUInt64.of(shard),
        body_summary,
        SszUInt64.of(proposer_index));
  }

  public UInt64 getSlot() {
    return getField0().get();
  }

  public UInt64 getShard() {
    return getField1().get();
  }

  public ShardBlobSummary getShardBlobSummary() {
    return getField2();
  }

  public UInt64 getProposerIndex() {
    return getField3().get();
  }
}
