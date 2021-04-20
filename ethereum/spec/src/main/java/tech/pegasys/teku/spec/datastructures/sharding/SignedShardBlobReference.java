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

public class SignedShardBlobReference
    extends Container2<SignedShardBlobReference, ShardBlobReference, SszSignature> {

  public static class SignedShardBlobReferenceSchema
      extends ContainerSchema2<SignedShardBlobReference, ShardBlobReference, SszSignature> {

    public SignedShardBlobReferenceSchema() {
      super(
          "SignedShardBlobReference",
          namedSchema("message", ShardBlobReference.SSZ_SCHEMA),
          namedSchema("signature", SszSignatureSchema.INSTANCE));
    }

    @Override
    public SignedShardBlobReference createFromBackingNode(TreeNode node) {
      return new SignedShardBlobReference(this, node);
    }
  }

  public static final SignedShardBlobReferenceSchema SSZ_SCHEMA = new SignedShardBlobReferenceSchema();

  private SignedShardBlobReference(SignedShardBlobReferenceSchema type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public SignedShardBlobReference(final ShardBlobReference message, final BLSSignature signature) {
    super(SSZ_SCHEMA, message, new SszSignature(signature));
  }

  public ShardBlobReference getMessage() {
    return getField0();
  }

  public BLSSignature getSignature() {
    return getField1().getSignature();
  }
}
