package tech.pegasys.teku.phase1.eth1engine

import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.util.Color
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.logDebug
import tech.pegasys.teku.phase1.util.printRoot

class LoggerAwareEth1EngineClient(
  private val name: String,
  private val delegate: Eth1EngineClient
) : Eth1EngineClient {
  override fun eth_getHeadBlockHash(): Eth1EngineClient.Response<Bytes32> {
    logDebug("$name: attempt to eth_getHeadBlockHash()")
    val response = delegate.eth_getHeadBlockHash()
    log("$name: eth_getHeadBlockHash() ~> ${printRoot(response.result!!)}")
    return response
  }

  override fun eth2_produceBlock(parentHash: Bytes32): Eth1EngineClient.Response<Eth1BlockData> {
    logDebug("$name: attempt to eth2_produceBlock(parent_hash=${printRoot(parentHash)})")
    val response = delegate.eth2_produceBlock(parentHash)
    log("$name: eth2_produceBlock(parent_hash=${printRoot(parentHash)}) ~> ${response.result}")
    return response
  }

  override fun eth2_validateBlock(blockRLP: Bytes): Eth1EngineClient.Response<Boolean> {
    logDebug("$name: attempt to eth2_validateBlock(rlp=${blockRLP.slice(0, 8).toHexString()}...)")
    val response = delegate.eth2_validateBlock(blockRLP)
    log(
      "$name: eth2_validateBlock(rlp=${blockRLP.slice(0, 8)
        .toHexString()}...) ~> ${response.result}"
    )
    return response
  }

  override fun eth2_insertBlock(blockRLP: Bytes): Eth1EngineClient.Response<Boolean> {
    logDebug("$name: attempt to eth2_insertBlock(rlp=${blockRLP.slice(0, 8).toHexString()}...)")
    val response = delegate.eth2_insertBlock(blockRLP)
    log(
      "$name: eth2_insertBlock(rlp=${blockRLP.slice(0, 8).toHexString()}...) ~> ${response.result}"
    )
    return response
  }

  override fun eth2_setHead(blockHash: Bytes32): Eth1EngineClient.Response<Boolean> {
    logDebug("$name: attempt to eth2_setHead(hash=${printRoot(blockHash)})")
    val response = delegate.eth2_setHead(blockHash)
    log("$name: eth2_setHead(hash=${printRoot(blockHash)}) ~> ${response.result}")
    return response
  }

  private fun log(msg: String) = log(msg, Color.CYAN)
}

fun Eth1EngineClient.withLogger(name: String): Eth1EngineClient =
  LoggerAwareEth1EngineClient(name, this)
