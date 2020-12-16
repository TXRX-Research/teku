package tech.pegasys.teku.phase1.eth1engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.JsonRpc2_0Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import kotlin.coroutines.CoroutineContext

class Web3jEth1EngineClient(private val rpcUrl: String, private val context: CoroutineContext) :
  Eth1EngineClient {

  private val web3j: CustomJsonRpc2_0Web3j = CustomJsonRpc2_0Web3j(HttpService(rpcUrl))

  @kotlinx.coroutines.FlowPreview
  override fun eth_getHeadBlockHash() = runBlocking(context) {
    val response = web3j.ethBlockNumber().flowable().asFlow()
      .map {
        web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(it.blockNumber), false).flowable()
          .asFlow()
      }
      .flattenConcat().first()

    getResultOrThrowError(response, { Bytes32.fromHexString(it!!.hash) })
  }

  override fun eth2_produceBlock(parentHash: Bytes32) = runBlocking(context) {
    val response = web3j.eth2ProduceBlock(parentHash.toHexString()).flowable().asFlow().first()
    getResultOrThrowError(response, { Bytes.fromBase64String(it!!) })
  }

  override fun eth2_produceBlock(
    parentHash: Bytes32,
    randaoMix: Bytes32,
    slot: uint64,
    timestamp: uint64,
    recentBeaconBlockRoots: Array<Bytes32>
  ): Eth1EngineClient.Response<ExecutableDataDTO> = runBlocking(context) {
    val response =
      web3j.eth2ProduceBlock(parentHash, randaoMix, slot, timestamp, recentBeaconBlockRoots)
        .flowable().asFlow().first()
    getResultOrThrowError(response, { it!! })
  }

  override fun eth2_validateBlock(blockRLP: Bytes) = runBlocking {
    val response = web3j.eth2ValidateBlock(blockRLP.toBase64String()).flowable().asFlow().first()
    if (response.hasError() && response.error.message != BLOCK_EXISTS_MESSAGE) {
      throw EthereumJsonRPCError("${response.error.code}: ${response.error.message}")
    }
    Eth1EngineClient.Response(true)
  }

  override fun eth2_insertBlock(blockRLP: Bytes) = runBlocking {
    val response = web3j.eth2InsertBlock(blockRLP.toBase64String()).flowable().asFlow().first()
    getResponseOnActionResultOrThrowError(response)
  }

  override fun eth2_insertBlock(
    parentHash: Bytes32,
    randaoMix: Bytes32,
    slot: uint64,
    timestamp: uint64,
    recentBeaconBlockRoots: Array<Bytes32>,
    executableData: ExecutableDataDTO
  ): Eth1EngineClient.Response<Boolean> = runBlocking(context) {
    val response = web3j.eth2InsertBlock(
      parentHash,
      randaoMix,
      slot,
      timestamp,
      recentBeaconBlockRoots,
      executableData
    ).flowable().asFlow().first()
    getResponseOnActionResultOrThrowError(response)
  }

  override fun eth2_setHead(blockHash: Bytes32) = runBlocking {
    val response = web3j.eth2SetHead(blockHash.toHexString()).flowable().asFlow().first()
    getResponseOnActionResultOrThrowError(response)
  }

  override fun toString(): String {
    return "Web3jEth1EngineClient(url=${rpcUrl})"
  }
}

private class CustomJsonRpc2_0Web3j(web3jService: Web3jService) : JsonRpc2_0Web3j(web3jService) {

  fun eth2ProduceBlock(parentHash: String): Request<String, ProducedBlock> {
    return Request(
      "eth2_produceBlock",
      listOf(parentHash),
      web3jService,
      ProducedBlock::class.java
    )
  }

  fun eth2ProduceBlock(
    parentHash: Bytes32,
    randaoMix: Bytes32,
    slot: uint64,
    timestamp: uint64,
    recentBeaconBlockRoots: Array<Bytes32>
  ): Request<Any, ProducedExecutableData> {
    val params = mapOf(
      "parent_hash" to parentHash.toHexString(),
      "randao_mix" to randaoMix.toHexString(),
      "slot" to slot.toLong(),
      "timestamp" to timestamp.toLong(),
      "recentBeaconBlockRoots" to recentBeaconBlockRoots.map { it.toHexString() }
    )
    return Request(
      "eth2_produceBlock",
      listOf(params),
      web3jService,
      ProducedExecutableData::class.java
    )
  }

  fun eth2InsertBlock(blockRLP: String): Request<String, ResponseOnAction> {
    return Request(
      "eth2_insertBlock",
      listOf(blockRLP),
      web3jService,
      ResponseOnAction::class.java
    )
  }

  fun eth2InsertBlock(
    parentHash: Bytes32,
    randaoMix: Bytes32,
    slot: uint64,
    timestamp: uint64,
    recentBeaconBlockRoots: Array<Bytes32>,
    executableData: ExecutableDataDTO
  ): Request<Any, ResponseOnAction> {
    val params = mapOf(
      "parent_hash" to parentHash.toHexString(),
      "randao_mix" to randaoMix.toHexString(),
      "slot" to slot.toLong(),
      "timestamp" to timestamp.toLong(),
      "recentBeaconBlockRoots" to recentBeaconBlockRoots.map { it.toHexString() },
      "executable_data" to executableData
    )
    return Request(
      "eth2_insertBlock",
      listOf(params),
      web3jService,
      ResponseOnAction::class.java
    )
  }

  fun eth2SetHead(blockHash: String): Request<String, ResponseOnAction> {
    return Request(
      "eth2_setHead",
      listOf(blockHash),
      web3jService,
      ResponseOnAction::class.java
    )
  }

  fun eth2ValidateBlock(blockRLP: String): Request<String, ResponseOnAction> {
    return Request(
      "eth2_validateBlock",
      listOf(blockRLP),
      web3jService,
      ResponseOnAction::class.java
    )
  }
}

class EthereumJsonRPCError(message: String?) : RuntimeException(message)

private fun <U, V> getResultOrThrowError(
  response: Response<U>,
  result: (U?) -> V,
  reason: (U?) -> String? = { null }
): Eth1EngineClient.Response<V> {
  return if (response.hasError()) {
    throw EthereumJsonRPCError("${response.error.code}: ${response.error.message}")
  } else {
    Eth1EngineClient.Response(result(response.result), reason(response.result))
  }
}

private fun getResponseOnActionResultOrThrowError(
  response: Response<String>
): Eth1EngineClient.Response<Boolean> {
  return getResultOrThrowError(response, { true })
}


private class ProducedBlock : Response<String>()
private class ResponseOnAction : Response<String>()

private class ProducedExecutableData : Response<ExecutableDataDTO>()

private const val BLOCK_EXISTS_MESSAGE = "block already known"