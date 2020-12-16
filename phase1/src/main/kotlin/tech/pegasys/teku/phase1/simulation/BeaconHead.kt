package tech.pegasys.teku.phase1.simulation

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.onotole.phase1.Root

data class BeaconHead(
  val root: Root,
  val state: BeaconState,
  val eth1_block_hash: Bytes32
)
