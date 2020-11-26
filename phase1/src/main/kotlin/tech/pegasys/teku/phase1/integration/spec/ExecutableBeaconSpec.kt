package tech.pegasys.teku.phase1.integration.spec

import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.encodeEth1BlockWithRLP
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.ExecutableData
import tech.pegasys.teku.phase1.onotole.deps.BLS
import tech.pegasys.teku.phase1.simulation.util.executableDataToEth1Block
import tech.pegasys.teku.phase1.util.Caches
import tech.pegasys.teku.phase1.util.NoOpCaches
import tech.pegasys.teku.phase1.util.printRoot

class ExecutableBeaconSpec(
  private val eth1_engine: Eth1EngineClient,
  bls: BLS,
  caches: Caches = NoOpCaches()
) : OptimizedPhase1Spec(caches, bls) {

  override fun process_block(state: BeaconState, block: BeaconBlock) {
    process_block_header(state, block)
    process_randao(state, block.body)
    process_eth1_data(state, block.body)
    process_light_client_aggregate(state, block.body)
    process_operations(state, block.body)
    process_executable_data(state, block.body.executable_data)
  }

  fun process_executable_data(state: BeaconState, data: ExecutableData) {
    val eth1_block = executableDataToEth1Block(data)
    val block_rlp = encodeEth1BlockWithRLP(eth1_block)

    val insert_block_response = eth1_engine.eth2_insertBlock(block_rlp)
    assert(insert_block_response.result == true) {
      "Failed to eth2_insertBlock($eth1_block), slot=${state.slot} reason: ${insert_block_response.reason}"
    }

    // a hack to make old styled eth1-engine work
    val set_header_response = eth1_engine.eth2_setHead(eth1_block.hash)
    assert(insert_block_response.result == true) {
      "Failed to eth2_setHead(hash=${printRoot(eth1_block.hash)}), reason ${set_header_response.reason}"
    }
  }
}
