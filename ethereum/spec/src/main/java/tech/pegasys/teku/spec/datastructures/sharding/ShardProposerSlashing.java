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

import tech.pegasys.teku.ssz.containers.Container2;
import tech.pegasys.teku.ssz.containers.ContainerSchema2;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class ShardProposerSlashing
    extends Container2<ShardProposerSlashing, SignedShardBlobReference, SignedShardBlobReference> {

  public static class ShardProposerSlashingSchema
      extends ContainerSchema2<
          ShardProposerSlashing, SignedShardBlobReference, SignedShardBlobReference> {

    public ShardProposerSlashingSchema() {
      super(
          "ShardProposerSlashing",
          namedSchema("signed_reference_1", SignedShardBlobReference.SSZ_SCHEMA),
          namedSchema("signed_reference_2", SignedShardBlobReference.SSZ_SCHEMA));
    }

    @Override
    public ShardProposerSlashing createFromBackingNode(TreeNode node) {
      return new ShardProposerSlashing(this, node);
    }
  }

  public static final ShardProposerSlashingSchema SSZ_SCHEMA = new ShardProposerSlashingSchema();

  private ShardProposerSlashing(ShardProposerSlashingSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public ShardProposerSlashing(
      final SignedShardBlobReference signed_reference_1,
      final SignedShardBlobReference signed_reference_2) {
    super(SSZ_SCHEMA, signed_reference_1, signed_reference_2);
  }

  public SignedShardBlobReference getSignedReference1() {
    return getField0();
  }

  public SignedShardBlobReference getSignedReference2() {
    return getField1();
  }
}
