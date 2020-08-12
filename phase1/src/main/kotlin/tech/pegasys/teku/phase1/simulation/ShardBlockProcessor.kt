package tech.pegasys.teku.phase1.simulation

import tech.pegasys.teku.phase1.eth1engine.Eth1BlockData
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1shard.ETH1_SHARD_NUMBER
import tech.pegasys.teku.phase1.eth1shard.Eth1ShardSpec
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.Store
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.util.Color
import tech.pegasys.teku.phase1.util.log

interface ShardBlockProcessor {
  fun process(block: SignedShardBlock)
}

class RegularShardProcessor(
  private val store: Store,
  private val spec: Phase1Spec
) : ShardBlockProcessor {

  override fun process(block: SignedShardBlock) {
    spec.on_shard_block(store, block)
  }
}

class Eth1ShardProcessor(
  private val store: Store,
  private val eth1Engine: Eth1EngineClient,
  private val eth1ShardSpec: Eth1ShardSpec
) : ShardBlockProcessor {

  override fun process(block: SignedShardBlock) {
    eth1ShardSpec.on_eth1_shard_block(store, block, eth1Engine)
    val eth1BlockData = Eth1BlockData(block.message.body.toBytes())
    log("Eth1ShardProcessor: eth1 shard block inserted:\n$eth1BlockData\n", Color.YELLOW)
  }
}

@Suppress("FunctionName")
fun ShardBlockProcessor(
  shard: Shard,
  store: Store,
  eth1Engine: Eth1EngineClient,
  spec: Phase1Spec
): ShardBlockProcessor {
  return if (shard == ETH1_SHARD_NUMBER) {
    Eth1ShardProcessor(store, eth1Engine, Eth1ShardSpec(spec))
  } else {
    RegularShardProcessor(store, spec)
  }
}
