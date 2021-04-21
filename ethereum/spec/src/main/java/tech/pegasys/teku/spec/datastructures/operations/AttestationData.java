/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.teku.spec.datastructures.operations;

import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.state.Checkpoint;
import tech.pegasys.teku.ssz.containers.Container6;
import tech.pegasys.teku.ssz.containers.ContainerSchema6;
import tech.pegasys.teku.ssz.primitive.SszBytes32;
import tech.pegasys.teku.ssz.primitive.SszUInt64;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class AttestationData
    extends
    Container6<AttestationData, SszUInt64, SszUInt64, SszBytes32, Checkpoint, Checkpoint, SszBytes32> {

  public static class AttestationDataSchema
      extends ContainerSchema6<
                AttestationData, SszUInt64, SszUInt64, SszBytes32, Checkpoint, Checkpoint, SszBytes32> {

    public AttestationDataSchema() {
      super(
          "AttestationData",
          namedSchema("slot", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("index", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("beacon_block_root", SszPrimitiveSchemas.BYTES32_SCHEMA),
          namedSchema("source", Checkpoint.SSZ_SCHEMA),
          namedSchema("target", Checkpoint.SSZ_SCHEMA),
          // TODO need versioned structure here
          namedSchema("shard_header_root", SszPrimitiveSchemas.BYTES32_SCHEMA));
    }

    @Override
    public AttestationData createFromBackingNode(TreeNode node) {
      return new AttestationData(this, node);
    }
  }

  public static final AttestationDataSchema SSZ_SCHEMA = new AttestationDataSchema();

  private AttestationData(AttestationDataSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public AttestationData(
      UInt64 slot, UInt64 index, Bytes32 beacon_block_root, Checkpoint source, Checkpoint target) {
    this(slot, index, beacon_block_root, source, target, Bytes32.ZERO);
  }

  public AttestationData(
      UInt64 slot, UInt64 index, Bytes32 beacon_block_root, Checkpoint source, Checkpoint target,
      Bytes32 shard_header_root) {

    super(
        SSZ_SCHEMA,
        SszUInt64.of(slot),
        SszUInt64.of(index),
        SszBytes32.of(beacon_block_root),
        source,
        target,
        SszBytes32.of(shard_header_root));
  }

  public AttestationData(UInt64 slot, AttestationData data) {
    this(slot, data.getIndex(), data.getBeacon_block_root(), data.getSource(), data.getTarget(),
        data.getShard_header_root());
  }

  public UInt64 getEarliestSlotForForkChoice() {
    // Attestations can't be processed by fork choice until their slot is in the past and until we
    // are in the same epoch as their target.
    return getSlot().plus(UInt64.ONE).max(getTarget().getEpochStartSlot());
  }

  public UInt64 getSlot() {
    return getField0().get();
  }

  public UInt64 getIndex() {
    return getField1().get();
  }

  public Bytes32 getBeacon_block_root() {
    return getField2().get();
  }

  public Checkpoint getSource() {
    return getField3();
  }

  public Checkpoint getTarget() {
    return getField4();
  }

  public Bytes32 getShard_header_root() {
    return getField5().get();
  }
}
