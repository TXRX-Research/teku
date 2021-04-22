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
import tech.pegasys.teku.ssz.collections.SszBitlist;
import tech.pegasys.teku.ssz.containers.Container6;
import tech.pegasys.teku.ssz.containers.ContainerSchema6;
import tech.pegasys.teku.ssz.primitive.SszBit;
import tech.pegasys.teku.ssz.primitive.SszBytes32;
import tech.pegasys.teku.ssz.primitive.SszUInt64;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.schema.collections.SszBitlistSchema;
import tech.pegasys.teku.ssz.tree.TreeNode;
import tech.pegasys.teku.util.config.Constants;

public class PendingShardHeader
    extends Container6<
        PendingShardHeader, SszUInt64, SszUInt64, DataCommitment, SszBytes32, SszBitlist, SszBit> {

  public static class PendingShardHeaderSchema
      extends ContainerSchema6<
          PendingShardHeader,
          SszUInt64,
          SszUInt64,
          DataCommitment,
          SszBytes32,
          SszBitlist,
          SszBit> {

    public PendingShardHeaderSchema() {
      super(
          "PendingShardHeader",
          namedSchema("slot", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("shard", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("commitment", DataCommitment.SSZ_SCHEMA),
          namedSchema("root", SszPrimitiveSchemas.BYTES32_SCHEMA),
          namedSchema("votes", SszBitlistSchema.create(Constants.MAX_VALIDATORS_PER_COMMITTEE)),
          namedSchema("confirmed", SszPrimitiveSchemas.BIT_SCHEMA));
    }

    @Override
    public PendingShardHeader createFromBackingNode(TreeNode node) {
      return new PendingShardHeader(this, node);
    }

    public SszBitlistSchema<?> getVotesSchema() {
      return (SszBitlistSchema<?>) getFieldSchema4();
    }
  }

  public static final PendingShardHeaderSchema SSZ_SCHEMA = new PendingShardHeaderSchema();

  private PendingShardHeader(PendingShardHeaderSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public PendingShardHeader(
      UInt64 slot,
      UInt64 shard,
      DataCommitment commitment,
      Bytes32 root,
      SszBitlist votes,
      boolean confirmed) {
    super(
        SSZ_SCHEMA,
        SszUInt64.of(slot),
        SszUInt64.of(shard),
        commitment,
        SszBytes32.of(root),
        votes,
        SszBit.of(confirmed));
  }

  public PendingShardHeader(PendingShardHeader header, SszBitlist newVotes, boolean confirmed) {
    super(
        header.getSchema(),
        SszUInt64.of(header.getSlot()),
        SszUInt64.of(header.getShard()),
        header.getCommitment(),
        SszBytes32.of(header.getRoot()),
        newVotes,
        SszBit.of(confirmed));
  }

  public UInt64 getSlot() {
    return getField0().get();
  }

  public UInt64 getShard() {
    return getField1().get();
  }

  public DataCommitment getCommitment() {
    return getField2();
  }

  public Bytes32 getRoot() {
    return getField3().get();
  }

  public SszBitlist getVotes() {
    return getField4();
  }

  public boolean isConfirmed() {
    return getField5().get();
  }

  @Override
  public PendingShardHeaderSchema getSchema() {
    return (PendingShardHeaderSchema) super.getSchema();
  }
}
