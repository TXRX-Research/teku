package tech.pegasys.teku.phase1.eth1engine.stub

import tech.pegasys.teku.phase1.eth1engine.Eth1BlockData
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.computeEth1BlockHash
import tech.pegasys.teku.phase1.onotole.deps.hash_tree_root
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.util.printRoot
import java.util.*
import kotlin.collections.HashMap

const val ETH1_BLOCK_BODY_SIZE = 1 shl 17

class Eth1EngineClientStub(private val rnd: Random) :
  Eth1EngineClient {

  private val blocks = HashMap<Bytes32, Bytes>()
  private var headBlockHash: Bytes32

  init {
    val genesisBody = Bytes.EMPTY
    val genesisRLP =
      encodeBlockDataWithRLP(
        0uL,
        Bytes32(),
        hash_tree_root(genesisBody),
        Bytes32(),
        genesisBody
      )
    headBlockHash = computeEth1BlockHash(genesisRLP)
    blocks[headBlockHash] = genesisRLP
  }

  override fun eth_getHeadBlockHash(): Eth1EngineClient.Response<Bytes32> =
    Eth1EngineClient.Response(headBlockHash)

  override fun eth2_produceBlock(parentHash: Bytes32): Eth1EngineClient.Response<Eth1BlockData> {
    if (headBlockHash != parentHash) {
      return Eth1EngineClient.Response(
        null, "Expected parent hash ${printRoot(headBlockHash)}, got ${printRoot(parentHash)}"
      )
    }

    val body = Bytes.random(ETH1_BLOCK_BODY_SIZE, rnd)
    val receiptsRoot = Bytes32.random(rnd)
    val stateRoot = hash_tree_root(body)

    val parentBlockRLP = blocks[parentHash]!!
    val number = parseBlockNumberFromRLP(
      parentBlockRLP
    ) + 1uL
    val blockRLP =
      encodeBlockDataWithRLP(
        number,
        parentHash,
        stateRoot,
        receiptsRoot,
        body
      )
    val blockData = Eth1BlockData(
      number = number,
      blockHash = computeEth1BlockHash(blockRLP),
      parentHash = parentHash,
      stateRoot = stateRoot,
      receiptsRoot = receiptsRoot,
      blockRLP = blockRLP
    )

    return Eth1EngineClient.Response(
      blockData
    )
  }

  override fun eth2_validateBlock(blockRLP: Bytes): Eth1EngineClient.Response<Boolean> {
    return Eth1EngineClient.Response(true)
  }

  override fun eth2_insertBlock(blockRLP: Bytes): Eth1EngineClient.Response<Boolean> {
    val parentHash =
      parseParentHashFromRLP(blockRLP)
    val number =
      parseBlockNumberFromRLP(blockRLP)
    if (!exist(parentHash)) {
      throw IllegalStateException("Parent block for block(number=$number) does not exist, parentHash=${parentHash}")
    }
    val parentNumber =
      parseBlockNumberFromRLP(blocks[parentHash]!!)
    if (number != parentNumber + 1uL) {
      throw IllegalArgumentException("Block number != parentNumber + 1: [$number != ${parentNumber + 1uL}]")
    }

    val blockHash = computeEth1BlockHash(blockRLP)
    blocks[blockHash] = blockRLP
    return Eth1EngineClient.Response(true)
  }

  override fun eth2_setHead(blockHash: Bytes32): Eth1EngineClient.Response<Boolean> {
    return if (exist(blockHash)) {
      headBlockHash = blockHash
      Eth1EngineClient.Response(true)
    } else {
      Eth1EngineClient.Response(
        false,
        "Block with given hash=${printRoot(blockHash)} does not exist"
      )
    }
  }

  private fun exist(blockHash: Bytes32): Boolean = blocks.containsKey(blockHash)

  override fun toString(): String {
    return "Eth1EngineClientStub()"
  }
}
