package tech.pegasys.teku.phase1.eth1engine

import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPReader
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes20
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import java.math.BigInteger

val EMPTY_TRIE_HASH = Hash.keccak256(RLP.encodeValue(Bytes.EMPTY))!!
val EMPTY_LIST_HASH = Hash.keccak256(RLP.encodeList {})

fun parseEth1BlockDataFromRLP(rlp: Bytes): Eth1BlockData {
  val block = decodeEth1BlockRLP(rlp)
  return Eth1BlockData(
    block.hash,
    block.header.parentHash,
    block.header.stateRoot,
    block.header.receiptsRoot,
    block.header.number,
    rlp
  )
}

fun encodeEth1BlockHeaderWithRLP(header: Eth1BlockHeader): Bytes {
  return RLP.encodeList {
    it.writeValue(header.parentHash)
    it.writeValue(header.unclesHash)
    it.writeValue(header.coinbase.wrappedBytes)
    it.writeValue(header.stateRoot)
    it.writeValue(header.txRoot)
    it.writeValue(header.receiptsRoot)
    it.writeValue(header.logsBloom)
    it.writeBigInteger(BigInteger.valueOf(header.difficulty.toLong()))
    it.writeBigInteger(BigInteger.valueOf(header.number.toLong()))
    it.writeBigInteger(BigInteger.valueOf(header.gasLimit.toLong()))
    it.writeBigInteger(BigInteger.valueOf(header.gasUsed.toLong()))
    it.writeBigInteger(BigInteger.valueOf(header.timestamp.toLong()))
    if (header.extraData == Bytes32.ZERO) {
      it.writeValue(Bytes.EMPTY)
    } else {
      it.writeValue(header.extraData)
    }
    it.writeValue(header.mixHash)
    it.writeValue(header.nonce)
  }
}

fun decodeEth1BlockHeaderRLP(rlp: Bytes): Eth1BlockHeader {
  return RLP.decodeList(rlp) { decodeEth1BlockHeaderRLP(it) }
}

private fun decodeEth1BlockHeaderRLP(reader: RLPReader): Eth1BlockHeader {
  val parentHash = Bytes32.wrap(reader.readValue())
  val unclesHash = Bytes32.wrap(reader.readValue())
  val coinbase = Bytes20(reader.readValue())
  val stateRoot = Bytes32.wrap(reader.readValue())
  val txRoot = reader.readValue()
  val receiptsRoot = reader.readValue()
  val logsBloom = reader.readValue()
  val difficulty = reader.readBigInteger().toLong().toULong()
  val number = reader.readBigInteger().toLong().toULong()
  val gasLimit = reader.readBigInteger().toLong().toULong()
  val gasUsed = reader.readBigInteger().toLong().toULong()
  val timestamp = reader.readBigInteger().toLong().toULong()
  val extraData = reader.readValue()
  val mixHash = Bytes32.wrap(reader.readValue())
  val nonce = reader.readValue()

  return Eth1BlockHeader(
    parentHash = parentHash,
    unclesHash = unclesHash,
    coinbase = coinbase,
    stateRoot = stateRoot,
    txRoot = if (txRoot.isEmpty) EMPTY_TRIE_HASH else Bytes32.wrap(txRoot),
    receiptsRoot = if (receiptsRoot.isEmpty) EMPTY_TRIE_HASH else Bytes32.wrap(receiptsRoot),
    logsBloom = logsBloom,
    difficulty = difficulty,
    number = number,
    gasLimit = gasLimit,
    gasUsed = gasUsed,
    timestamp = timestamp,
    extraData = if (extraData.isEmpty) Bytes32.ZERO else Bytes32.wrap(extraData),
    mixHash = mixHash,
    nonce = nonce
  )
}

fun encodeEth1BlockWithRLP(block: Eth1Block): Bytes {
  return RLP.encodeList {
    it.writeRLP(encodeEth1BlockHeaderWithRLP(block.header))
    it.writeList { txList ->
      block.transactions.forEach { txRLP -> txList.writeRLP(txRLP) }
    }
    it.writeList { uncleList ->
      block.uncles.forEach { uncleRLP -> uncleList.writeRLP(uncleRLP) }
    }
  }
}

private fun decodeTransactions(reader: RLPReader): List<Bytes> {
  return reader.readListContents<Bytes> {
    val txElements = it.readListContents<Bytes> { txElement -> txElement.readValue() }
    RLP.encodeList(txElements) { writer, value -> writer.writeValue(value) }
  }
}

fun decodeEth1BlockRLP(rlp: Bytes): Eth1Block {
  return RLP.decodeList(rlp) {
    val header = it.readList<Eth1BlockHeader>(::decodeEth1BlockHeaderRLP)
    val transactions = decodeTransactions(it)
    Eth1Block(header, transactions)
  }
}
