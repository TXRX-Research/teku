package tech.pegasys.teku.phase1.integration.datastructures

import org.apache.tuweni.units.bigints.UInt256
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
val MAX_BYTES_PER_TRANSACTION_PAYLOAD = 1uL shl 32

class ExecutableData : AbstractImmutableContainer {

  val coinbase: Bytes20
    get() = getBasicValue(get(0))
  val state_root: Bytes32
    get() = getBasicValue(get(1))
  val gas_limit: uint64
    get() = getBasicValue(get(2))
  val gas_used: uint64
    get() = getBasicValue(get(3))
  val transactions: SSZList<Eth1Transaction>
    get() = SSZListImpl<Eth1Transaction, Eth1Transaction>(getAny(4)) { it }
  val receipt_root: Bytes32
    get() = getBasicValue(get(5))
  val logs_bloom: SSZByteVector
    get() = SSZByteVectorImpl(getAny<VectorViewRead<BasicViews.ByteView>>(6))
  val block_hash: Bytes32
    get() = getBasicValue(get(7))
  val difficulty: uint64
    get() = getBasicValue(get(8))

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>,
    backingNode: TreeNode
  ) : super(type, backingNode)

  constructor(
    coinbase: Bytes20,
    state_root: Bytes32,
    gas_limit: uint64,
    gas_used: uint64,
    transactions: List<Eth1Transaction>,
    receipt_root: Bytes32,
    logs_bloom: Bytes,
    block_hash: Bytes32,
    difficulty: uint64
  ) : super(
    TYPE,
    *wrapValues(
      coinbase,
      state_root,
      gas_limit,
      gas_used,
      getListView(Eth1Transaction.TYPE, MAX_TRANSACTIONS, transactions) { it },
      receipt_root,
      logs_bloom,
      block_hash,
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
        ListViewType<Eth1Transaction>(Eth1Transaction.TYPE, MAX_TRANSACTIONS.toLong()),
        BasicViewTypes.BYTES32_TYPE,
        VectorViewType<BasicViews.ByteView>(BasicViewTypes.BYTE_TYPE, LOGS_BLOOM_SIZE.toLong()),
        BasicViewTypes.BYTES32_TYPE,
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
        "receipt_root=${printRoot(receipt_root)}, " +
        "logs_bloom=${
          ViewUtils.getAllBytes((logs_bloom as SSZByteVectorImpl).view).slice(0, 10)
        }, " +
        "block_hash=${printRoot(block_hash)}, " +
        "difficulty=$difficulty)"
  }
}


class Eth1Transaction : AbstractImmutableContainer {

  val nonce: uint64
    get() = getBasicValue(get(0))
  val gas_price: UInt256
    get() = getBasicValue(get(1))
  val gas_limit: uint64
    get() = getBasicValue(get(2))
  val recipient: Bytes20
    get() = getBasicValue(get(3))
  val value: UInt256
    get() = getBasicValue(get(4))
  val input: SSZByteList
    get() = SSZByteListImpl(getAny(5))
  val v: UInt256
    get() = getBasicValue(get(6))
  val r: UInt256
    get() = getBasicValue(get(7))
  val s: UInt256
    get() = getBasicValue(get(8))

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>,
    backingNode: TreeNode
  ) : super(type, backingNode)

  constructor(
    nonce: uint64,
    gas_price: UInt256,
    gas_limit: uint64,
    recipient: Bytes20,
    value: UInt256,
    input: Bytes,
    v: UInt256,
    r: UInt256,
    s: UInt256
  ) : super(
    TYPE,
    *wrapValues(
      nonce,
      gas_price,
      gas_limit,
      recipient,
      value,
      getListView(
        BasicViewTypes.BYTE_TYPE,
        MAX_BYTES_PER_TRANSACTION_PAYLOAD,
        input.toArray().toList()
      ) { BasicViews.ByteView(it) },
      v,
      r,
      s
    )
  )

  constructor() : super(TYPE)

  companion object {

    val TYPE = ContainerViewType(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        ListViewType<BasicViews.ByteView>(
          BasicViewTypes.BYTE_TYPE,
          MAX_BYTES_PER_TRANSACTION_PAYLOAD.toLong()
        ),
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE
      ),
      ::Eth1Transaction
    )
  }
}