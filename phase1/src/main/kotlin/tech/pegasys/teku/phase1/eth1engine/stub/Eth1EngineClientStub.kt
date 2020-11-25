package tech.pegasys.teku.phase1.eth1engine.stub

import tech.pegasys.teku.phase1.eth1engine.Eth1Block
import tech.pegasys.teku.phase1.eth1engine.Eth1BlockHeader
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.decodeEth1BlockRLP
import tech.pegasys.teku.phase1.eth1engine.encodeEth1BlockWithRLP
import tech.pegasys.teku.phase1.integration.datastructures.LOGS_BLOOM_SIZE
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes20
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.util.printRoot
import java.util.*
import kotlin.collections.HashMap

const val GAS_LIMIT = 12_000_000uL

class Eth1EngineClientStub(private val rnd: Random) :
  Eth1EngineClient {

  private val blocks = HashMap<Bytes32, Bytes>()
  private var headBlockHash: Bytes32

  init {
    val genesis = Eth1Block(
      Eth1BlockHeader(
        parentHash = Bytes32.ZERO,
        coinbase = Bytes20.ZERO,
        stateRoot = Bytes32.ZERO,
        receiptsRoot = Bytes32.ZERO,
        logsBloom = Bytes.wrap(ByteArray(256)),
        number = 0uL,
        timestamp = 0uL,
        gasLimit = 0uL,
        gasUsed = 0uL
    )
    )
    val genesisRLP = encodeEth1BlockWithRLP(genesis)
    headBlockHash = genesis.hash
    blocks[headBlockHash] = genesisRLP
  }

  override fun eth_getHeadBlockHash(): Eth1EngineClient.Response<Bytes32> =
    Eth1EngineClient.Response(headBlockHash)

  override fun eth2_produceBlock(parentHash: Bytes32): Eth1EngineClient.Response<Bytes> {
    if (headBlockHash != parentHash) {
      return Eth1EngineClient.Response(
        null, "Expected parent hash ${printRoot(headBlockHash)}, got ${printRoot(parentHash)}"
      )
    }

    val receiptsRoot = Bytes32.random(rnd)
    val stateRoot = Bytes32.random(rnd)

    val parentBlockRLP = blocks[parentHash]!!
    val number = decodeEth1BlockRLP(parentBlockRLP).header.number + 1uL

    val block = Eth1Block(Eth1BlockHeader(
      parentHash = parentHash,
      coinbase = Bytes20.ZERO,
      stateRoot = stateRoot,
      receiptsRoot = receiptsRoot,
      logsBloom = Bytes.random(LOGS_BLOOM_SIZE.toInt()),
      number = number,
      timestamp = (System.currentTimeMillis() / 1000).toULong(),
      gasLimit = GAS_LIMIT,
      gasUsed = 0uL
    ))
    val blockRLP = encodeEth1BlockWithRLP(block)

    return Eth1EngineClient.Response(blockRLP)
  }

  override fun eth2_validateBlock(blockRLP: Bytes): Eth1EngineClient.Response<Boolean> {
    return Eth1EngineClient.Response(true)
  }

  override fun eth2_insertBlock(blockRLP: Bytes): Eth1EngineClient.Response<Boolean> {
    val block = decodeEth1BlockRLP(blockRLP)
    val parentHash = block.header.parentHash
    val number = block.header.number
    if (!exist(parentHash)) {
      throw IllegalStateException("Parent block for block(number=$number) does not exist, parentHash=${parentHash}")
    }
    val parentNumber = decodeEth1BlockRLP(blocks[parentHash]!!).header.number
    if (number != parentNumber + 1uL) {
      throw IllegalArgumentException("Block number != parentNumber + 1: [$number != ${parentNumber + 1uL}]")
    }

    val blockHash = block.hash
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
