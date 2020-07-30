package tech.pegasys.teku.phase1.eth1engine

import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.rlp.RLP
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import java.math.BigInteger

val EMPTY_TRIE_HASH = Hash.keccak256(RLP.encodeValue(Bytes.EMPTY))!!

fun computeEth1BlockHash(rlp: Bytes): Bytes32 = Hash.keccak256(rlp)

fun parseEth1BlockDataFromRLP(rlp: Bytes): Eth1BlockData {
  val blockHeaderElements = RLP.decodeList(rlp) { block ->
    block.readListContents { it.readValue() }
  }
  val encodedBlockHeader = RLP.encodeList(blockHeaderElements) { writer, element ->
    writer.writeValue(element)
  }
  val blockHash = computeEth1BlockHash(encodedBlockHeader)
  val parentHash = Bytes32.wrap(blockHeaderElements[0])              // parentHash       = header[0]
  val stateRoot = Bytes32.wrap(blockHeaderElements[3])               // stateRoot        = header[3]
  val receiptsRootValue = blockHeaderElements[5]                     // receiptTrieRoot  = header[5]
  val number = BigInteger(1, blockHeaderElements[8].toArrayUnsafe()) // number           = header[8]
    .toLong().toULong()

  val receiptsRoot = if (receiptsRootValue.isEmpty) {
    EMPTY_TRIE_HASH
  } else {
    Bytes32.wrap(receiptsRootValue)
  }

  return Eth1BlockData(blockHash, parentHash, stateRoot, receiptsRoot, number, rlp)
}
