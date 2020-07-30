package tech.pegasys.teku.phase1.eth1engine

import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32

interface Eth1EngineClient {
  fun eth_getHeadBlockHash(): Response<Bytes32>
  fun eth2_produceBlock(parentHash: Bytes32): Response<Eth1BlockData>
  fun eth2_validateBlock(blockRLP: Bytes): Response<Boolean>
  fun eth2_insertBlock(blockRLP: Bytes): Response<Boolean>
  fun eth2_setHead(blockHash: Bytes32): Response<Boolean>

  data class Response<T>(val result: T?, val reason: String? = null)
}
