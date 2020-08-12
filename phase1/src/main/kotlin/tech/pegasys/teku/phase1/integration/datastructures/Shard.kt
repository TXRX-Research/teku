package tech.pegasys.teku.phase1.integration.datastructures

import tech.pegasys.teku.phase1.integration.Bytes96Type
import tech.pegasys.teku.phase1.integration.getBasicValue
import tech.pegasys.teku.phase1.integration.ssz.SSZByteListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZListImpl
import tech.pegasys.teku.phase1.integration.ssz.getListView
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.integration.wrapValues
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.phase1.Gwei
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARD_BLOCKS_PER_ATTESTATION
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARD_BLOCK_SIZE
import tech.pegasys.teku.phase1.onotole.phase1.NO_SIGNATURE
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.Bytes96
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteList
import tech.pegasys.teku.phase1.onotole.ssz.SSZList
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.printRoot
import tech.pegasys.teku.ssz.backing.tree.TreeNode
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes
import tech.pegasys.teku.ssz.backing.type.ContainerViewType
import tech.pegasys.teku.ssz.backing.type.ListViewType
import tech.pegasys.teku.ssz.backing.view.AbstractImmutableContainer
import tech.pegasys.teku.ssz.backing.view.BasicViews.ByteView
import tech.pegasys.teku.ssz.backing.view.BasicViews.Bytes32View
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt64View
import tech.pegasys.teku.ssz.backing.view.ViewUtils

class ShardTransition : AbstractImmutableContainer {
  val start_slot: Slot
    get() = (get(0) as UInt64View).get().toUInt64()
  val shard_block_lengths: SSZList<uint64>
    get() = SSZListImpl<uint64, UInt64View>(getAny(1)) { it.get().toUInt64() }
  val shard_data_roots: SSZList<Root>
    get() = SSZListImpl(getAny(2), Bytes32View::get)
  val shard_states: SSZList<ShardState>
    get() = SSZListImpl<ShardState, ShardState>(getAny(3)) { it }
  val proposer_signature_aggregate: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(4)))

  constructor(
    start_slot: Slot,
    shard_block_lengths: SSZList<uint64>,
    shard_data_roots: SSZList<Bytes32>,
    shard_states: SSZList<ShardState>,
    proposer_signature_aggregate: BLSSignature
  ) : super(
    TYPE,
    *wrapValues(
      start_slot,
      shard_block_lengths,
      shard_data_roots,
      shard_states,
      proposer_signature_aggregate
    )
  )

  constructor(
    start_slot: Slot,
    shard_block_lengths: List<uint64> = listOf(),
    shard_data_roots: List<Bytes32> = listOf(),
    shard_states: List<ShardState> = listOf(),
    proposer_signature_aggregate: BLSSignature = NO_SIGNATURE
  ) : super(
    TYPE,
    *wrapValues(
      start_slot,
      getListView(
        BasicViewTypes.UINT64_TYPE,
        MAX_SHARD_BLOCKS_PER_ATTESTATION,
        shard_block_lengths
      ) { UInt64View(it.toUInt64()) },
      getListView(
        BasicViewTypes.BYTES32_TYPE,
        MAX_SHARD_BLOCKS_PER_ATTESTATION,
        shard_data_roots
      ) { Bytes32View(it) },
      getListView(
        ShardState.TYPE,
        MAX_SHARD_BLOCKS_PER_ATTESTATION,
        shard_states
      ) { it },
      proposer_signature_aggregate
    )
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<ShardTransition>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        ListViewType<UInt64View>(
          BasicViewTypes.UINT64_TYPE,
          MAX_SHARD_BLOCKS_PER_ATTESTATION.toLong()
        ),
        ListViewType<Bytes32View>(
          BasicViewTypes.BYTES32_TYPE,
          MAX_SHARD_BLOCKS_PER_ATTESTATION.toLong()
        ),
        ListViewType<ShardState>(ShardState.TYPE, MAX_SHARD_BLOCKS_PER_ATTESTATION.toLong()),
        Bytes96Type
      ),
      ::ShardTransition
    )
  }

  override fun toString(): String {
    return "ShardTransition(" +
        "start_slot=$start_slot, " +
        "shard_block_lengths=[${shard_block_lengths.joinToString { it.toString() }}], " +
        "shard_data_roots=[${shard_data_roots.joinToString { printRoot(it) }}], " +
        "shard_states=[${shard_states.joinToString { it.toString() }}])"
  }
}

data class MutableShardState(
  var slot: Slot,
  var gasprice: Gwei,
  var latest_block_root: Root
)

class ShardState : AbstractImmutableContainer {
  val slot: Slot
    get() = getBasicValue(get(0))
  val gasprice: Gwei
    get() = getBasicValue(get(1))
  val latest_block_root: Root
    get() = getBasicValue(get(2))

  constructor(
    slot: Slot,
    gasprice: Gwei,
    latest_block_root: Root
  ) : super(TYPE, *wrapValues(slot, gasprice, latest_block_root))

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  fun updated(mutator: (MutableShardState) -> Unit): ShardState {
    val mutableCopy = MutableShardState(slot, gasprice, latest_block_root)
    mutator(mutableCopy)
    return ShardState(mutableCopy.slot, mutableCopy.gasprice, mutableCopy.latest_block_root)
  }

  fun copy(
    slot: Slot = this.slot,
    gasprice: Gwei = this.gasprice,
    latest_block_root: Root = this.latest_block_root
  ): ShardState = ShardState(slot, gasprice, latest_block_root)

  override fun toString(): String {
    return "(slot=$slot, gasprice=$gasprice, latest_block_root=${printRoot(latest_block_root)})"
  }


  companion object {
    val TYPE = ContainerViewType<ShardState>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE
      ),
      ::ShardState
    )
  }
}

class ShardBlockHeader : AbstractImmutableContainer {
  val shard_parent_root: Root
    get() = getBasicValue(get(0))
  val beacon_parent_root: Root
    get() = getBasicValue(get(1))
  val slot: Slot
    get() = getBasicValue(get(2))
  val shard: Shard
    get() = getBasicValue(get(3))
  val proposer_index: ValidatorIndex
    get() = getBasicValue(get(4))
  val body_root: Root
    get() = getBasicValue(get(5))

  constructor(
    shard_parent_root: Root,
    beacon_parent_root: Root,
    slot: Slot,
    shard: Shard,
    proposer_index: ValidatorIndex,
    body_root: Root
  ) : super(
    TYPE,
    *wrapValues(
      shard_parent_root,
      beacon_parent_root,
      slot,
      shard,
      proposer_index,
      body_root
    )
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<ShardBlockHeader>(
      listOf(
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE
      ),
      ::ShardBlockHeader
    )
  }
}

class ShardBlock : AbstractImmutableContainer {
  val shard_parent_root: Root
    get() = getBasicValue(get(0))
  val beacon_parent_root: Root
    get() = getBasicValue(get(1))
  val slot: Slot
    get() = getBasicValue(get(2))
  val shard: Shard
    get() = getBasicValue(get(3))
  val proposer_index: ValidatorIndex
    get() = getBasicValue(get(4))
  val body: SSZByteList
    get() = SSZByteListImpl(getAny(5))

  constructor(
    shard_parent_root: Root = Root(),
    beacon_parent_root: Root = Root(),
    slot: Slot,
    shard: Shard,
    proposer_index: ValidatorIndex = ValidatorIndex(),
    body: SSZByteList = SSZByteListImpl(
      ListViewType<ByteView>(
        BasicViewTypes.BYTE_TYPE,
        MAX_SHARD_BLOCK_SIZE.toLong()
      ).default
    )
  ) : super(
    TYPE,
    *wrapValues(
      shard_parent_root,
      beacon_parent_root,
      slot,
      shard,
      proposer_index,
      body
    )
  )

  constructor(
    shard_parent_root: Root,
    beacon_parent_root: Root,
    slot: Slot,
    shard: Shard,
    proposer_index: ValidatorIndex,
    body: List<Byte>
  ) : this(
    shard_parent_root,
    beacon_parent_root,
    slot,
    shard,
    proposer_index,
    SSZByteListImpl(MAX_SHARD_BLOCK_SIZE, body)
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<ShardBlock>(
      listOf(
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        ListViewType<ByteView>(BasicViewTypes.BYTE_TYPE, MAX_SHARD_BLOCK_SIZE.toLong())
      ),
      ::ShardBlock
    )
  }

  override fun toString(): String {
    return "ShardBlock(" +
        "slot=$slot, " +
        "shard=$shard, " +
        "root=${printRoot(hashTreeRoot())}, " +
        "proposer_index=$proposer_index, " +
        "beacon_parent_root=${printRoot(beacon_parent_root)}, " +
        "shard_parent_root=${printRoot(shard_parent_root)}, " +
        "body_root=${printRoot(body.hashTreeRoot())})"
  }
}

class SignedShardBlock : AbstractImmutableContainer {
  val message: ShardBlock
    get() = getAny(0)
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(1)))

  constructor(message: ShardBlock, signature: BLSSignature = BLSSignature()) : super(
    TYPE, message, ViewUtils.createVectorFromBytes(signature.wrappedBytes)
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<SignedShardBlock>(
      listOf(
        ShardBlock.TYPE,
        Bytes96Type
      ),
      ::SignedShardBlock
    )
  }
}
