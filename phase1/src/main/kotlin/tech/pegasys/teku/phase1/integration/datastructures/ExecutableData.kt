package tech.pegasys.teku.phase1.integration.datastructures

import tech.pegasys.teku.phase1.integration.getBasicValue
import tech.pegasys.teku.phase1.integration.ssz.SSZByteListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZByteVectorImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZListImpl
import tech.pegasys.teku.phase1.integration.ssz.getListView
import tech.pegasys.teku.phase1.integration.wrapValues
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes20
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteList
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteVector
import tech.pegasys.teku.phase1.onotole.ssz.SSZList
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.printRoot
import tech.pegasys.teku.ssz.backing.ListViewRead
import tech.pegasys.teku.ssz.backing.VectorViewRead
import tech.pegasys.teku.ssz.backing.tree.TreeNode
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes
import tech.pegasys.teku.ssz.backing.type.ContainerViewType
import tech.pegasys.teku.ssz.backing.type.ListViewType
import tech.pegasys.teku.ssz.backing.type.VectorViewType
import tech.pegasys.teku.ssz.backing.view.AbstractImmutableContainer
import tech.pegasys.teku.ssz.backing.view.BasicViews
import tech.pegasys.teku.ssz.backing.view.ViewUtils

val MAX_TRANSACTIONS = 1uL shl 32
val MAX_TRANSACTION_SIZE = 1uL shl 32
val LOGS_BLOOM_SIZE = 256uL

class ExecutableData : AbstractImmutableContainer {

  val coinbase: Bytes20
    get() = getBasicValue(get(0))
  val state_root: Bytes32
    get() = getBasicValue(get(1))
  val gas_limit: uint64
    get() = getBasicValue(get(2))
  val gas_used: uint64
    get() = getBasicValue(get(3))
  val transactions: SSZList<SSZByteList>
    get() = SSZListImpl<SSZByteList, ListViewRead<BasicViews.ByteView>>(getAny(4)) {
      SSZByteListImpl(it)
    }
  val receipts_root: Bytes32
    get() = getBasicValue(get(5))
  val logs_bloom: SSZByteVector
    get() = SSZByteVectorImpl(getAny<VectorViewRead<BasicViews.ByteView>>(6))
  val tx_root: Bytes32
    get() = getBasicValue(get(7))
  val parent_hash: Bytes32
    get() = getBasicValue(get(8))
  val timestamp: uint64
    get() = getBasicValue(get(9))
  val number: uint64
    get() = getBasicValue(get(10))
  val difficulty: uint64
    get() = getBasicValue(get(11))

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>,
    backingNode: TreeNode
  ) : super(type, backingNode)

  constructor(
    coinbase: Bytes20,
    state_root: Bytes32,
    gas_limit: uint64,
    gas_used: uint64,
    transactions: List<Bytes>,
    receipts_root: Bytes32,
    logs_bloom: Bytes,
    tx_root: Bytes32,
    parent_hash: Bytes32,
    timestamp: uint64,
    number: uint64,
    difficulty: uint64
  ) : super(
    TYPE,
    *wrapValues(
      coinbase,
      state_root,
      gas_limit,
      gas_used,
      getListView(
        ListViewType<BasicViews.ByteView>(BasicViewTypes.BYTE_TYPE, MAX_TRANSACTIONS.toLong()),
        MAX_TRANSACTIONS,
        transactions
      ) { SSZByteListImpl(MAX_TRANSACTION_SIZE, it).view },
      receipts_root,
      logs_bloom,
      tx_root,
      parent_hash,
      timestamp,
      number,
      difficulty
    )
  )

  constructor() : super(TYPE)

  companion object {

    val TYPE = ContainerViewType(
      listOf(
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        ListViewType<ListViewRead<BasicViews.ByteView>>(
          ListViewType<BasicViews.ByteView>(
            BasicViewTypes.BYTE_TYPE,
            MAX_TRANSACTION_SIZE.toLong()
          ),
          MAX_TRANSACTIONS.toLong()
        ),
        BasicViewTypes.BYTES32_TYPE,
        VectorViewType<BasicViews.ByteView>(BasicViewTypes.BYTE_TYPE, LOGS_BLOOM_SIZE.toLong()),
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE
      ),
      ::ExecutableData
    )
  }

  override fun toString(): String {
    return "(" +
        "coinbase=${coinbase}, " +
        "state_root=${printRoot(state_root)}, " +
        "gas_limit=${gas_limit}, " +
        "gas_used=${gas_used}, " +
        "transactions=${transactions.size}, " +
        "receipts_root=${printRoot(receipts_root)}, " +
        "logs_bloom=${
          ViewUtils.getAllBytes((logs_bloom as SSZByteVectorImpl).view).slice(0, 10)
        }, " +
        "tx_root=${printRoot(tx_root)}, " +
        "number=$number, " +
        "parent_hash=${printRoot(parent_hash)}, " +
        "timestamp=$timestamp)"
  }
}
