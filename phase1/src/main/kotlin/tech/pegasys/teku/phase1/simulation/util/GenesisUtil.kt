package tech.pegasys.teku.phase1.simulation.util

import tech.pegasys.teku.bls.BLSKeyPair
import tech.pegasys.teku.datastructures.util.BeaconStateUtil
import tech.pegasys.teku.datastructures.util.MockStartBeaconStateGenerator
import tech.pegasys.teku.datastructures.util.MockStartDepositGenerator
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.toUnsignedLong
import tech.pegasys.teku.phase1.onotole.phase1.INITIAL_ACTIVE_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.MIN_GENESIS_TIME
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec

fun getGenesisState(blsKeyPairs: List<BLSKeyPair>, spec: Phase1Spec): BeaconState {
  BeaconStateUtil.BLS_VERIFY_DEPOSIT = false
  val deposits = MockStartDepositGenerator().createDeposits(blsKeyPairs)
  val state = spec.upgrade_to_phase1(
    MockStartBeaconStateGenerator().createInitialBeaconState(
      MIN_GENESIS_TIME.toUnsignedLong(),
      deposits
    )
  )
  return state.applyChanges()
}

fun getGenesisStore(state: BeaconState, spec: Phase1Spec) = spec.get_forkchoice_store(state)

fun getShardGenesisStores(state: BeaconState, spec: Phase1Spec) =
  (0uL until INITIAL_ACTIVE_SHARDS).map { it to spec.get_forkchoice_shard_store(state, it) }.toMap()
