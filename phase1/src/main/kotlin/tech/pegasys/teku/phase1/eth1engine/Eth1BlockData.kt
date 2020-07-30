package tech.pegasys.teku.phase1.eth1engine

import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.printRoot

data class Eth1BlockData(
  val blockHash: Bytes32,
  val parentHash: Bytes32,
  val stateRoot: Bytes32,
  val receiptsRoot: Bytes32,
  val number: uint64,
  val blockRLP: Bytes
) {

  constructor(pseudoSosBytes: Bytes) : this(
    Bytes32.wrap(pseudoSosBytes.slice(0 * Bytes32.SIZE, Bytes32.SIZE)),
    Bytes32.wrap(pseudoSosBytes.slice(1 * Bytes32.SIZE, Bytes32.SIZE)),
    Bytes32.wrap(pseudoSosBytes.slice(2 * Bytes32.SIZE, Bytes32.SIZE)),
    Bytes32.wrap(pseudoSosBytes.slice(3 * Bytes32.SIZE, Bytes32.SIZE)),
    pseudoSosBytes.slice(4 * Bytes32.SIZE, 8).toLong().toULong(),
    pseudoSosBytes.slice(4 * Bytes32.SIZE + 8, pseudoSosBytes.size() - (4 * Bytes32.SIZE + 8))
  )

  fun encodeWithPseudoSOS(): Bytes {
    return Bytes.concatenate(
      blockHash,
      parentHash,
      stateRoot,
      receiptsRoot,
      Bytes.ofUnsignedLong(number.toLong()),
      blockRLP
    )
  }

  override fun toString(): String {
    return "Eth1BlockData(" +
        "number=$number, " +
        "blockHash=${printRoot(blockHash)}, " +
        "parentHash=${printRoot(parentHash)}, " +
        "stateRoot=${printRoot(stateRoot)}, " +
        "receiptsRoot=${printRoot(receiptsRoot)}, " +
        "blockRLP=[(${blockRLP.size()} bytes): ${blockRLP.slice(0, 8).toHexString()}...])"
  }
}
