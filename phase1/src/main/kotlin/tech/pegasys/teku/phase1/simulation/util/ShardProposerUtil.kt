package tech.pegasys.teku.phase1.simulation.util

import org.apache.tuweni.bytes.Bytes
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.ShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.integration.spec.get_shard_block_signature

private const val SHARD_BLOCK_SIZE = 1024

fun produceShardBlock(
  slot: Slot,
  shard: Shard,
  shardParentRoot: Root,
  beaconHeadRoot: Root,
  beaconHeadState: BeaconState,
  body: Bytes,
  secretKeys: SecretKeyRegistry,
  spec: Phase1Spec
): SignedShardBlock {
  val proposerIndex = spec.get_shard_proposer_index(beaconHeadState, slot, shard)
  val shardBlock = ShardBlock(
    shardParentRoot,
    beaconHeadRoot,
    slot,
    shard,
    proposerIndex,
    body.toArrayUnsafe().toList()
  )

  return SignedShardBlock(
    shardBlock,
    spec.get_shard_block_signature(beaconHeadState, shardBlock, secretKeys[proposerIndex])
  )
}

fun getRandomShardBlockBody(): Bytes = Bytes.random(SHARD_BLOCK_SIZE, SimulationRandomness)
