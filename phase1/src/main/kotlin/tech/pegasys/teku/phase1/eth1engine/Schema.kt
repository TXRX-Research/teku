package tech.pegasys.teku.phase1.eth1engine

import org.apache.tuweni.units.bigints.UInt256
import tech.pegasys.teku.phase1.integration.datastructures.Eth1Transaction
import tech.pegasys.teku.phase1.integration.datastructures.ExecutableData
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes20
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32

data class ExecutableDataDTO(
  var coinbase: String? = null,
  var state_root: String? = null,
  var gas_limit: Long? = null,
  var gas_used: Long? = null,
  var transactions: List<Eth1TransactionDTO>? = null,
  var receipt_root: String? = null,
  var logs_bloom: String? = null,
  var block_hash: String? = null,
  var difficulty: Long? = null
)

fun ExecutableDataDTO(data: ExecutableData) = ExecutableDataDTO(
  data.coinbase.wrappedBytes.toHexString(),
  data.state_root.toHexString(),
  data.gas_limit.toLong(),
  data.gas_used.toLong(),
  data.transactions.map { Eth1TransactionDTO(it) }.toList(),
  data.receipt_root.toHexString(),
  data.logs_bloom.toBytes().toBase64String(),
  data.block_hash.toHexString(),
  data.difficulty.toLong()
)

fun ExecutableData(dto: ExecutableDataDTO) = ExecutableData(
  Bytes20(Bytes.fromHexString(dto.coinbase!!)),
  Bytes32.fromHexString(dto.state_root!!),
  dto.gas_limit!!.toULong(),
  dto.gas_used!!.toULong(),
  dto.transactions?.map { Eth1Transaction(it) }.orEmpty(),
  Bytes32.fromHexString(dto.receipt_root!!),
  Bytes.fromBase64String(dto.logs_bloom!!),
  Bytes32.fromHexString(dto.block_hash!!),
  dto.difficulty!!.toULong()
)

data class Eth1TransactionDTO(
  var nonce: String? = null,
  var gasPrice: String? = null,
  var gas: String? = null,
  var to: String? = null,
  var value: String? = null,
  var input: String? = null,
  var v: String? = null,
  var r: String? = null,
  var s: String? = null
)

fun Eth1TransactionDTO(eth1Transaction: Eth1Transaction) = Eth1TransactionDTO(
  UInt256.valueOf(eth1Transaction.nonce.toLong()).toBytes().toQuantityHexString(),
  eth1Transaction.gas_price.toBytes().toQuantityHexString(),
  UInt256.valueOf(eth1Transaction.gas_limit.toLong()).toBytes().toQuantityHexString(),
  if (eth1Transaction.recipient != Bytes20.ZERO) eth1Transaction.recipient.wrappedBytes.toHexString() else null,
  eth1Transaction.value.toBytes().toQuantityHexString(),
  eth1Transaction.input.toBytes().toHexString(),
  eth1Transaction.v.toBytes().toQuantityHexString(),
  eth1Transaction.r.toBytes().toQuantityHexString(),
  eth1Transaction.s.toBytes().toQuantityHexString()
)

fun Eth1Transaction(dto: Eth1TransactionDTO) = Eth1Transaction(
  UInt256.fromHexString(dto.nonce!!).toLong().toULong(),
  UInt256.fromHexString(dto.gasPrice!!),
  UInt256.fromHexString(dto.gas!!).toLong().toULong(),
  if (!dto.to.isNullOrBlank()) Bytes20(Bytes.fromHexString(dto.to!!)) else Bytes20.ZERO,
  UInt256.fromHexString(dto.value!!),
  if (!dto.input.isNullOrBlank()) Bytes.fromHexString(dto.input!!) else Bytes.EMPTY,
  UInt256.fromHexString(dto.v!!),
  UInt256.fromHexString(dto.r!!),
  UInt256.fromHexString(dto.s!!)
)
