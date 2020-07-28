package tech.pegasys.teku.phase1.simulation.util

import tech.pegasys.teku.phase1.integration.datastructures.ShardStore
import tech.pegasys.teku.phase1.integration.datastructures.Store

fun revampStoreFromFinalizedCheckpoint(store: Store): Store {
  val finalizedEpoch = store.finalized_checkpoint.epoch
  val finalizedSlot = store.blocks[store.finalized_checkpoint.root]!!.slot
  return store.copy(
    blocks = store.blocks.filter { it.value.slot >= finalizedSlot }.toMutableMap(),
    block_states = store.block_states.filter { it.value.slot >= finalizedSlot }.toMutableMap(),
    checkpoint_states = store.checkpoint_states.filter { it.key.epoch >= finalizedEpoch }
      .toMutableMap()
  )
}

fun revampShardStoreFromFinalizedCheckpoint(store: Store, shardStore: ShardStore): ShardStore {
  val finalizedSlot = store.blocks[store.finalized_checkpoint.root]!!.slot
  return shardStore.copy(
    signed_blocks = shardStore.signed_blocks.filter { it.value.message.slot >= finalizedSlot }
      .toMutableMap(),
    block_states = shardStore.block_states.filter { it.value.slot >= finalizedSlot }.toMutableMap()
  )
}
