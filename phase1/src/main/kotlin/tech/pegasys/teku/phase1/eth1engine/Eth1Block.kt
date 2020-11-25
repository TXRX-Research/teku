package tech.pegasys.teku.phase1.eth1engine

import org.apache.tuweni.crypto.Hash
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes20
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.uint64

data class Eth1BlockHeader(
  val parentHash: Bytes32,
  val coinbase: Bytes20,
  val stateRoot: Bytes32,
  val logsBloom: Bytes,
  val number: uint64,
  val timestamp: uint64,
  val gasLimit: uint64,
  val gasUsed: uint64,
  val receiptsRoot: Bytes32 = EMPTY_TRIE_HASH,
  val txRoot: Bytes32 = EMPTY_TRIE_HASH,
  val difficulty: uint64 = 0uL,
  val mixHash: Bytes32 = Bytes32.ZERO,
  val extraData: Bytes32 = Bytes32.ZERO,
  val nonce: Bytes = Bytes.wrap(ByteArray(8)),
  val unclesHash: Bytes32 = EMPTY_LIST_HASH
) {

  val hash: Bytes32
    get() = Hash.keccak256(encodeEth1BlockHeaderWithRLP(this))
}

data class Eth1Block(
  val header: Eth1BlockHeader,
  val transactions: List<Bytes> = listOf(),
  val uncles: List<Bytes> = listOf()
) {

  val hash: Bytes32
    get() = header.hash
}
