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

import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.spec.datastructures.type.SszSignature;
import tech.pegasys.teku.spec.datastructures.type.SszSignatureSchema;
import tech.pegasys.teku.ssz.containers.Container2;
import tech.pegasys.teku.ssz.containers.ContainerSchema2;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class SignedShardBlobHeader
    extends Container2<SignedShardBlobHeader, ShardBlobHeader, SszSignature> {

  public static class SignedShardBlobHeaderSchema
      extends ContainerSchema2<SignedShardBlobHeader, ShardBlobHeader, SszSignature> {

    public SignedShardBlobHeaderSchema() {
      super(
          "SignedBeaconBlockHeader",
          namedSchema("message", ShardBlobHeader.SSZ_SCHEMA),
          namedSchema("signature", SszSignatureSchema.INSTANCE));
    }

    @Override
    public SignedShardBlobHeader createFromBackingNode(TreeNode node) {
      return new SignedShardBlobHeader(this, node);
    }
  }

  public static final SignedShardBlobHeaderSchema SSZ_SCHEMA = new SignedShardBlobHeaderSchema();

  private SignedShardBlobHeader(SignedShardBlobHeaderSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public SignedShardBlobHeader(final ShardBlobHeader message, final BLSSignature signature) {
    super(SSZ_SCHEMA, message, new SszSignature(signature));
  }

  public ShardBlobHeader getMessage() {
    return getField0();
  }

  public BLSSignature getSignature() {
    return getField1().getSignature();
  }
}
