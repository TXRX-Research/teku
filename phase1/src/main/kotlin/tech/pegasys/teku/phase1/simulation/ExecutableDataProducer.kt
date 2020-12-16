package tech.pegasys.teku.phase1.simulation

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.ExecutableData
import tech.pegasys.teku.phase1.integration.datastructures.ExecutableData
import tech.pegasys.teku.phase1.integration.spec.RECENT_BLOCK_ROOTS_SIZE
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.printRoot

interface ExecutableDataProducer {

  fun produce(
    parentHash: Bytes32,
    slot: uint64,
    timestamp: uint64,
    randaoMix: Bytes32
  ): ExecutableData =
    ExecutableData.TYPE.default
}

val EmptyDataProducer = object : ExecutableDataProducer {}

internal class ExecutableDataProducerImpl(
  private val eth1Engine: Eth1EngineClient
) : ExecutableDataProducer {

  override fun produce(
    parentHash: Bytes32,
    slot: uint64,
    timestamp: uint64,
    randaoMix: Bytes32
  ): ExecutableData {
    val produceResponse = eth1Engine.eth2_produceBlock(
      parentHash,
      randaoMix,
      slot,
      timestamp,
      Array(RECENT_BLOCK_ROOTS_SIZE) { Bytes32.ZERO }
    )
    if (produceResponse.result == null) {
      throw IllegalStateException(
        "Failed to eth2_produceBlock(parent_hash=${printRoot(parentHash)}) for slot=${slot}, " +
            "reason ${produceResponse.reason}"
      )
    }

    return ExecutableData(produceResponse.result)
  }
}

fun ExecutableDataProducer(eth1Engine: Eth1EngineClient): ExecutableDataProducer {
  return ExecutableDataProducerImpl(eth1Engine)
}