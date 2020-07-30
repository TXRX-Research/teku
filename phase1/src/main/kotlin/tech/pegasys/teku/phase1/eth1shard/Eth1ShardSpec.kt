package tech.pegasys.teku.phase1.eth1shard

import tech.pegasys.teku.phase1.eth1engine.Eth1BlockData
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.integration.datastructures.ShardStore
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.Store
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Shard

var ETH1_SHARD_NUMBER = Shard(0uL)

class Eth1ShardSpec(private val spec: Phase1Spec) {
  fun on_eth1_shard_block(store: Store, shard_store: ShardStore, signed_shard_block: SignedShardBlock, eth1_engine: Eth1EngineClient): Unit {
    val eth1BlockData = Eth1BlockData(signed_shard_block.message.body.toBytes())
    val ret = eth1_engine.eth2_insertBlock(eth1BlockData.blockRLP)
    if (ret.result != true) {
      throw IllegalStateException("Failed to eth2_insertBlock($eth1BlockData), reason: ${ret.reason}")
    }
    spec.on_shard_block(store, shard_store, signed_shard_block)
  }
}
