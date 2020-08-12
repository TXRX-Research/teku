package tech.pegasys.teku.phase1.simulation

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.eth1engine.Eth1BlockData
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1shard.ETH1_SHARD_NUMBER
import tech.pegasys.teku.phase1.integration.datastructures.ShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.simulation.util.SecretKeyRegistry
import tech.pegasys.teku.phase1.simulation.util.getRandomShardBlockBody
import tech.pegasys.teku.phase1.simulation.util.produceShardBlock
import tech.pegasys.teku.phase1.util.Color
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.printRoot

interface ShardBlockProducer {
  fun produce(
      slot: Slot,
      beaconHead: BeaconHead,
      parentRoot: Root,
      parent: ShardBlock
  ): SignedShardBlock
}

class RandomShardBlockProducer(
    private val shard: Shard,
    private val secretKeys: SecretKeyRegistry,
    private val spec: Phase1Spec
) : ShardBlockProducer {
  override fun produce(
      slot: Slot,
      beaconHead: BeaconHead,
      parentRoot: Root,
      parent: ShardBlock
  ): SignedShardBlock {
    val body = getRandomShardBlockBody()
    return produceShardBlock(
        slot,
        shard,
        parentRoot,
        beaconHead.root,
        beaconHead.state,
        body,
        secretKeys,
        spec
    )
  }
}

class Eth1ShardBlockProducer(
    private val shard: Shard,
    private val secretKeys: SecretKeyRegistry,
    private val eth1Engine: Eth1EngineClient,
    private val spec: Phase1Spec
) : ShardBlockProducer {

  override fun produce(
      slot: Slot,
      beaconHead: BeaconHead,
      parentRoot: Root,
      parent: ShardBlock
  ): SignedShardBlock {
    val eth1ParentHash = getEth1BlockHash(parent, eth1Engine)

    // Create Eth1 block and assert that creation is succeeded
    val produceResponse = eth1Engine.eth2_produceBlock(eth1ParentHash)
    if (produceResponse.result == null) {
      throw IllegalStateException(
          "Failed to eth2_produceBlock(parent_hash=${printRoot(eth1ParentHash)}) for slot=$slot, " +
              "reason ${produceResponse.reason}"
      )
    }
    val eth1BlockData = produceResponse.result

    // Check if created Eth1 block is imported successfully
    val importResponse = eth1Engine.eth2_insertBlock(eth1BlockData.blockRLP)
    if (importResponse.result != true) {
      throw IllegalStateException(
          "Failed to import $eth1BlockData, " +
              "reason ${produceResponse.reason}"
      )
    }

    log("Eth1ShardBlockProducer: New block created:\n$eth1BlockData\n", Color.YELLOW)

    val body = eth1BlockData.encodeWithPseudoSOS()
    return produceShardBlock(
        slot,
        shard,
        parentRoot,
        beaconHead.root,
        beaconHead.state,
        body,
        secretKeys,
        spec
    )
  }
}

/**
 * Falls back to eth1-engine if shard block body is empty (PHASE_1_GENESIS case)
 */
private fun getEth1BlockHash(block: ShardBlock, eth1Engine: Eth1EngineClient): Bytes32 {
  return if (block.body.size > 0) {
    val eth1ParentData = Eth1BlockData(block.body.toBytes())
    eth1ParentData.blockHash
  } else {
    eth1Engine.eth_getHeadBlockHash().result!!
  }
}

@Suppress("FunctionName")
fun ShardBlockProducer(
    shard: Shard,
    secretKeys: SecretKeyRegistry,
    eth1Engine: Eth1EngineClient,
    spec: Phase1Spec
): ShardBlockProducer {
  return if (shard == ETH1_SHARD_NUMBER) {
    Eth1ShardBlockProducer(shard, secretKeys, eth1Engine, spec)
  } else {
    RandomShardBlockProducer(shard, secretKeys, spec)
  }
}
