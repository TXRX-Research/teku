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

import static tech.pegasys.teku.util.config.Constants.BYTES_PER_LOGS_BLOOM;
import static tech.pegasys.teku.util.config.Constants.MAX_ETH1_TRANSACTIONS;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.function.Function;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.datastructures.util.SpecDependent;
import tech.pegasys.teku.infrastructure.logging.LogFormatter;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.Bytes20;
import tech.pegasys.teku.ssz.SSZTypes.SSZBackingList;
import tech.pegasys.teku.ssz.SSZTypes.SSZList;
import tech.pegasys.teku.ssz.backing.ListViewRead;
import tech.pegasys.teku.ssz.backing.VectorViewRead;
import tech.pegasys.teku.ssz.backing.containers.Container10;
import tech.pegasys.teku.ssz.backing.containers.ContainerType10;
import tech.pegasys.teku.ssz.backing.tree.TreeNode;
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes;
import tech.pegasys.teku.ssz.backing.type.ListViewType;
import tech.pegasys.teku.ssz.backing.type.VectorViewType;
import tech.pegasys.teku.ssz.backing.view.BasicViews.ByteView;
import tech.pegasys.teku.ssz.backing.view.BasicViews.Bytes32View;
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt64View;
import tech.pegasys.teku.ssz.backing.view.ViewUtils;

public class ExecutableData
    extends Container10<
        ExecutableData,
        Bytes32View,
        Bytes32View,
        VectorViewRead<ByteView>,
        Bytes32View,
        UInt64View,
        UInt64View,
        Bytes32View,
        VectorViewRead<ByteView>,
        UInt64View,
        ListViewRead<Eth1Transaction>> {

  public static class ExecutableDataType
      extends ContainerType10<
          ExecutableData,
          Bytes32View,
          Bytes32View,
          VectorViewRead<ByteView>,
          Bytes32View,
          UInt64View,
          UInt64View,
          Bytes32View,
          VectorViewRead<ByteView>,
          UInt64View,
          ListViewRead<Eth1Transaction>> {

    public ExecutableDataType() {
      super(
          "ExecutableData",
          namedType("parent_hash", BasicViewTypes.BYTES32_TYPE),
          namedType("block_hash", BasicViewTypes.BYTES32_TYPE),
          namedType("coinbase", new VectorViewType<>(BasicViewTypes.BYTE_TYPE, Bytes20.SIZE)),
          namedType("state_root", BasicViewTypes.BYTES32_TYPE),
          namedType("gas_limit", BasicViewTypes.UINT64_TYPE),
          namedType("gas_used", BasicViewTypes.UINT64_TYPE),
          namedType("receipt_root", BasicViewTypes.BYTES32_TYPE),
          namedType(
              "logs_bloom", new VectorViewType<>(BasicViewTypes.BYTE_TYPE, BYTES_PER_LOGS_BLOOM)),
          namedType("difficulty", BasicViewTypes.UINT64_TYPE),
          namedType(
              "transactions",
              new ListViewType<>(Eth1Transaction.TYPE.get(), MAX_ETH1_TRANSACTIONS)));
    }

    public ListViewType<Eth1Transaction> getTransactionsType() {
      return (ListViewType<Eth1Transaction>) getFieldType9();
    }

    @Override
    public ExecutableData createFromBackingNode(TreeNode node) {
      return new ExecutableData(this, node);
    }
  }

  private ExecutableData(ExecutableDataType type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public static final SpecDependent<ExecutableDataType> TYPE =
      SpecDependent.of(ExecutableDataType::new);

  public ExecutableData() {
    super(TYPE.get());
  }

  public ExecutableData(
      ExecutableDataType type,
      Bytes32 parent_hash,
      Bytes32 block_hash,
      Bytes20 coinbase,
      Bytes32 state_root,
      UInt64 gas_limit,
      UInt64 gas_used,
      Bytes32 receipt_root,
      Bytes logs_bloom,
      UInt64 difficulty,
      Iterable<Eth1Transaction> transactions) {
    super(
        type,
        new Bytes32View(parent_hash),
        new Bytes32View(block_hash),
        ViewUtils.createVectorFromBytes(coinbase.getWrappedBytes()),
        new Bytes32View(state_root),
        new UInt64View(gas_limit),
        new UInt64View(gas_used),
        new Bytes32View(receipt_root),
        ViewUtils.createVectorFromBytes(logs_bloom),
        new UInt64View(difficulty),
        ViewUtils.toListView(type.getTransactionsType(), transactions));
  }

  public ExecutableData(
      Bytes32 parent_hash,
      Bytes32 block_hash,
      Bytes20 coinbase,
      Bytes32 state_root,
      UInt64 gas_limit,
      UInt64 gas_used,
      Bytes32 receipt_root,
      Bytes logs_bloom,
      UInt64 difficulty,
      List<Eth1Transaction> transactions) {
    this(
        TYPE.get(),
        parent_hash,
        block_hash,
        coinbase,
        state_root,
        gas_limit,
        gas_used,
        receipt_root,
        logs_bloom,
        difficulty,
        transactions);
  }

  public Bytes32 getParent_hash() {
    return ((Bytes32View) get(0)).get();
  }

  public Bytes32 getBlock_hash() {
    return ((Bytes32View) get(1)).get();
  }

  public Bytes20 getCoinbase() {
    return new Bytes20(ViewUtils.getAllBytes(getAny(2)));
  }

  public Bytes32 getState_root() {
    return ((Bytes32View) get(3)).get();
  }

  public UInt64 getGas_limit() {
    return ((UInt64View) get(4)).get();
  }

  public UInt64 getGas_used() {
    return ((UInt64View) get(5)).get();
  }

  public Bytes32 getReceipt_root() {
    return ((Bytes32View) get(6)).get();
  }

  public Bytes getLogs_bloom() {
    return ViewUtils.getAllBytes(getAny(7));
  }

  public UInt64 getDifficulty() {
    return ((UInt64View) get(8)).get();
  }

  public SSZList<Eth1Transaction> getTransactions() {
    return new SSZBackingList<>(
        Eth1Transaction.class, getAny(9), Function.identity(), Function.identity());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("parent_hash", LogFormatter.formatHashRoot(getParent_hash()))
        .add("block_hash", LogFormatter.formatHashRoot(getBlock_hash()))
        .add("coinbase", getCoinbase().getWrappedBytes().slice(0, 8))
        .add("state_root", LogFormatter.formatHashRoot(getState_root()))
        .add("gas_limit", getGas_limit())
        .add("gas_used", getGas_used())
        .add("receipt_root", LogFormatter.formatHashRoot(getReceipt_root()))
        .add("logs_bloom", getLogs_bloom().slice(0, 8))
        .add("difficulty", getDifficulty())
        .toString();
  }
}
