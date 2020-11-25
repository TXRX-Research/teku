package tech.pegasys.teku.phase1.simulation

import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.decodeEth1BlockRLP
import tech.pegasys.teku.phase1.integration.datastructures.ExecutableData
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.simulation.util.eth1BlockToExecutableData
import tech.pegasys.teku.phase1.util.printRoot

interface ExecutableDataProducer {

  fun produce(currentSlot: Slot): ExecutableData = ExecutableData.TYPE.default
}

val EmptyDataProducer = object : ExecutableDataProducer {}

internal class ExecutableDataProducerImpl(private val eth1Engine: Eth1EngineClient) :
  ExecutableDataProducer {

  override fun produce(currentSlot: Slot): ExecutableData {

    // request parent hash from eth1-engine, temporal workaround
    val parentBlockHashResponse = eth1Engine.eth_getHeadBlockHash()
    if (parentBlockHashResponse.result == null) {
      throw IllegalStateException("Failed to eth_getHeadBlockHash(), slot=$currentSlot")
    }
    val parentHash = parentBlockHashResponse.result

    val produceResponse = eth1Engine.eth2_produceBlock(parentHash)
    if (produceResponse.result == null) {
      throw IllegalStateException(
        "Failed to eth2_produceBlock(parent_hash=${printRoot(parentHash)}) for slot=$currentSlot, " +
            "reason ${produceResponse.reason}"
      )
    }
    val blockRLP = produceResponse.result
    val eth1Block = decodeEth1BlockRLP(blockRLP)

    return eth1BlockToExecutableData(eth1Block)
  }
}

fun ExecutableDataProducer(eth1Engine: Eth1EngineClient): ExecutableDataProducer {
  return ExecutableDataProducerImpl(eth1Engine)
}