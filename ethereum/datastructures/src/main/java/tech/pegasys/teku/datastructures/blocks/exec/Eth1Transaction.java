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

package tech.pegasys.teku.datastructures.blocks.exec;

import static tech.pegasys.teku.util.config.Constants.MAX_BYTES_PER_TRANSACTION_PAYLOAD;

import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import tech.pegasys.teku.datastructures.util.SpecDependent;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.Bytes20;
import tech.pegasys.teku.ssz.backing.ListViewRead;
import tech.pegasys.teku.ssz.backing.VectorViewRead;
import tech.pegasys.teku.ssz.backing.containers.Container9;
import tech.pegasys.teku.ssz.backing.containers.ContainerType9;
import tech.pegasys.teku.ssz.backing.tree.TreeNode;
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes;
import tech.pegasys.teku.ssz.backing.type.ListViewType;
import tech.pegasys.teku.ssz.backing.type.VectorViewType;
import tech.pegasys.teku.ssz.backing.view.BasicViews.ByteView;
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt256View;
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt64View;
import tech.pegasys.teku.ssz.backing.view.ViewUtils;

public class Eth1Transaction
    extends Container9<
        Eth1Transaction,
        UInt64View,
        UInt256View,
        UInt64View,
        VectorViewRead<ByteView>,
        UInt256View,
        ListViewRead<ByteView>,
        UInt256View,
        UInt256View,
        UInt256View> {

  public static class Eth1TransactionType
      extends ContainerType9<
          Eth1Transaction,
          UInt64View,
          UInt256View,
          UInt64View,
          VectorViewRead<ByteView>,
          UInt256View,
          ListViewRead<ByteView>,
          UInt256View,
          UInt256View,
          UInt256View> {

    public Eth1TransactionType() {
      super(
          "Eth1TransactionType",
          namedType("nonce", BasicViewTypes.UINT64_TYPE),
          namedType("gas_price", BasicViewTypes.UINT256_TYPE),
          namedType("gas_limit", BasicViewTypes.UINT64_TYPE),
          namedType("recipient", new VectorViewType<>(BasicViewTypes.BYTE_TYPE, Bytes20.SIZE)),
          namedType("value", BasicViewTypes.UINT256_TYPE),
          namedType(
              "input",
              new ListViewType<>(BasicViewTypes.BYTE_TYPE, MAX_BYTES_PER_TRANSACTION_PAYLOAD)),
          namedType("v", BasicViewTypes.UINT256_TYPE),
          namedType("r", BasicViewTypes.UINT256_TYPE),
          namedType("s", BasicViewTypes.UINT256_TYPE));
    }

    public ListViewType<ByteView> getInputType() {
      return (ListViewType<ByteView>) getFieldType5();
    }

    @Override
    public Eth1Transaction createFromBackingNode(TreeNode node) {
      return new Eth1Transaction(this, node);
    }
  }

  public Eth1Transaction(Eth1TransactionType type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public static final SpecDependent<Eth1TransactionType> TYPE =
      SpecDependent.of(Eth1TransactionType::new);

  public Eth1Transaction() {
    super(TYPE.get());
  }

  public Eth1Transaction(
      UInt64 nonce,
      UInt256 gas_price,
      UInt64 gas_limit,
      Bytes20 recipient,
      UInt256 value,
      Bytes input,
      UInt256 v,
      UInt256 r,
      UInt256 s) {
    this(TYPE.get(), nonce, gas_price, gas_limit, recipient, value, input, v, r, s);
  }

  public Eth1Transaction(
      Eth1TransactionType type,
      UInt64 nonce,
      UInt256 gas_price,
      UInt64 gas_limit,
      Bytes20 recipient,
      UInt256 value,
      Bytes input,
      UInt256 v,
      UInt256 r,
      UInt256 s) {
    super(
        type,
        new UInt64View(nonce),
        new UInt256View(gas_price),
        new UInt64View(gas_limit),
        ViewUtils.createVectorFromBytes(recipient.getWrappedBytes()),
        new UInt256View(value),
        ViewUtils.createListFromBytes(type.getInputType(), input),
        new UInt256View(v),
        new UInt256View(r),
        new UInt256View(s));
  }

  public UInt64 getNonce() {
    return getField0().get();
  }

  public UInt256 getGas_price() {
    return getField1().get();
  }

  public UInt64 getGas_limit() {
    return getField2().get();
  }

  public Bytes20 getRecipient() {
    return new Bytes20(ViewUtils.getAllBytes(getField3()));
  }

  public UInt256 getValue() {
    return getField4().get();
  }

  public Bytes getInput() {
    return ViewUtils.getListBytes(getField5());
  }

  public UInt256 getV() {
    return getField6().get();
  }

  public UInt256 getR() {
    return getField7().get();
  }

  public UInt256 getS() {
    return getField8().get();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nonce", getNonce())
        .add("gas_price", getGas_price())
        .add("gas", getGas_limit())
        .add("to", getRecipient())
        .add("value", getValue())
        .add("input", getInput())
        .add("v", getV())
        .add("r", getR())
        .add("s", getS())
        .toString();
  }
}
