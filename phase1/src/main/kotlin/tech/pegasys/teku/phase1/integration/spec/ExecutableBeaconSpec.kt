package tech.pegasys.teku.phase1.integration.spec

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.ExecutableDataDTO
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.ExecutableData
import tech.pegasys.teku.phase1.onotole.deps.BLS
import tech.pegasys.teku.phase1.util.Caches
import tech.pegasys.teku.phase1.util.NoOpCaches

const val RECENT_BLOCK_ROOTS_SIZE = 256

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
    val parent_hash = state.latest_block_header.eth1_parent_hash
    val slot = state.slot
    val epoch = get_current_epoch(state)
    val timestamp = compute_time_at_slot(state, slot)
    val randao_mix = get_randao_mix(state, epoch)

    val insert_block_response = eth1_engine.eth2_insertBlock(
      parent_hash,
      randao_mix,
      slot,
      timestamp,
      Array(RECENT_BLOCK_ROOTS_SIZE) { Bytes32.ZERO },
      ExecutableDataDTO(data)
    )

    assert(insert_block_response.result == true) {
      "Failed to eth2_insertBlock($data), slot=${state.slot} reason: ${insert_block_response.reason}"
    }
  }
}
