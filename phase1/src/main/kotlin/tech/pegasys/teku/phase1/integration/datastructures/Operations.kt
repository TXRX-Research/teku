package tech.pegasys.teku.phase1.integration.datastructures

import org.apache.tuweni.bytes.Bytes48
import tech.pegasys.teku.phase1.integration.Bytes48Type
import tech.pegasys.teku.phase1.integration.Bytes96Type
import tech.pegasys.teku.phase1.integration.getBasicValue
import tech.pegasys.teku.phase1.integration.ssz.SSZAbstractCollection
import tech.pegasys.teku.phase1.integration.ssz.SSZBitlistImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZByteListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZByteVectorImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZVectorImpl
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.integration.wrapBasicValue
import tech.pegasys.teku.phase1.integration.wrapValues
import tech.pegasys.teku.phase1.onotole.phase1.BLSPubkey
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.phase1.BYTES_PER_CUSTODY_CHUNK
import tech.pegasys.teku.phase1.onotole.phase1.CUSTODY_RESPONSE_DEPTH
import tech.pegasys.teku.phase1.onotole.phase1.CommitteeIndex
import tech.pegasys.teku.phase1.onotole.phase1.DEPOSIT_CONTRACT_TREE_DEPTH
import tech.pegasys.teku.phase1.onotole.phase1.Epoch
import tech.pegasys.teku.phase1.onotole.phase1.Gwei
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARD_BLOCK_SIZE
import tech.pegasys.teku.phase1.onotole.phase1.MAX_VALIDATORS_PER_COMMITTEE
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.Bytes96
import tech.pegasys.teku.phase1.onotole.ssz.SSZBitlist
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteList
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteVector
import tech.pegasys.teku.phase1.onotole.ssz.SSZList
import tech.pegasys.teku.phase1.onotole.ssz.SSZVector
import tech.pegasys.teku.phase1.onotole.ssz.boolean
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.printRoot
import tech.pegasys.teku.ssz.backing.ListViewRead
import tech.pegasys.teku.ssz.backing.VectorViewRead
import tech.pegasys.teku.ssz.backing.tree.TreeNode
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes
import tech.pegasys.teku.ssz.backing.type.ContainerViewType
import tech.pegasys.teku.ssz.backing.type.ListViewType
import tech.pegasys.teku.ssz.backing.type.VectorViewType
import tech.pegasys.teku.ssz.backing.view.AbstractImmutableContainer
import tech.pegasys.teku.ssz.backing.view.BasicViews.BitView
import tech.pegasys.teku.ssz.backing.view.BasicViews.ByteView
import tech.pegasys.teku.ssz.backing.view.BasicViews.Bytes32View
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt64View
import tech.pegasys.teku.ssz.backing.view.ViewUtils

class AttestationData : AbstractImmutableContainer {
  val slot: Slot
    get() = getBasicValue(get(0))
  val index: CommitteeIndex
    get() = getBasicValue(get(1))
  val beacon_block_root: Root
    get() = getBasicValue(get(2))
  val source: Checkpoint
    get() = getAny(3)
  val target: Checkpoint
    get() = getAny(4)
  val shard: Shard
    get() = getBasicValue(get(5))
  val shard_head_root: Root
    get() = getBasicValue(get(6))
  val shard_transition_root: Root
    get() = getBasicValue(get(7))

  constructor() : super(TYPE)

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(
    slot: Slot,
    index: CommitteeIndex,
    beacon_block_root: Root,
    source: Checkpoint,
    target: Checkpoint,
    shard: Shard,
    shard_head_root: Root,
    shard_transition_root: Root
  ) : super(
    TYPE,
    *wrapValues(
      slot,
      index,
      beacon_block_root,
      source,
      target,
      shard,
      shard_head_root,
      shard_transition_root
    )
  )

  constructor(fullAttestationData: FullAttestationData) : this(
    fullAttestationData.slot,
    fullAttestationData.index,
    fullAttestationData.beacon_block_root,
    fullAttestationData.source,
    fullAttestationData.target,
    fullAttestationData.shard,
    fullAttestationData.shard_head_root,
    fullAttestationData.shard_transition.hashTreeRoot()
  )

  companion object {
    val TYPE = ContainerViewType<AttestationData>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        Checkpoint.TYPE,
        Checkpoint.TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE
      ), ::AttestationData
    )
  }

  override fun toString(): String {
    return "(" +
        "slot=$slot, " +
        "shard=$shard, " +
        "beacon_block_root=${printRoot(beacon_block_root)}, " +
        "shard_head_root=${printRoot(shard_head_root)}, " +
        "shard_transition_root=${printRoot(shard_transition_root)}, " +
        "source=$source, " +
        "target=$target)"
  }

  fun toStringShort(): String {
    return "(slot=$slot, shard=$shard)"
  }
}

class FullAttestationData : AbstractImmutableContainer {
  val slot: Slot
    get() = getBasicValue(get(0))
  val index: CommitteeIndex
    get() = getBasicValue(get(1))
  val beacon_block_root: Root
    get() = getBasicValue(get(2))
  val source: Checkpoint
    get() = getAny(3)
  val target: Checkpoint
    get() = getAny(4)
  val shard: Shard
    get() = getBasicValue(get(5))
  val shard_head_root: Root
    get() = getBasicValue(get(6))
  val shard_transition: ShardTransition
    get() = getAny(7)

  constructor() : super(TYPE)

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(
    slot: Slot,
    index: CommitteeIndex,
    beacon_block_root: Root,
    source: Checkpoint,
    target: Checkpoint,
    shard: Shard,
    shard_head_root: Root,
    shard_transition: ShardTransition
  ) : super(
    TYPE,
    *wrapValues(
      slot,
      index,
      beacon_block_root,
      source,
      target,
      shard,
      shard_head_root,
      shard_transition
    )
  )

  companion object {
    val TYPE = ContainerViewType<FullAttestationData>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        Checkpoint.TYPE,
        Checkpoint.TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        ShardTransition.TYPE
      ), ::FullAttestationData
    )
  }

  override fun toString(): String {
    return "(" +
        "slot=$slot, " +
        "shard=$shard, " +
        "beacon_block_root=${printRoot(beacon_block_root)}, " +
        "shard_head_root=${printRoot(shard_head_root)}, " +
        "shard_transition_root=${printRoot(shard_transition.hashTreeRoot())}, " +
        "source=$source, " +
        "target=$target)"
  }

  fun toStringFull(): String {
    return "(" +
        "slot=$slot, " +
        "shard=$shard, " +
        "beacon_block_root=${printRoot(beacon_block_root)}, " +
        "shard_head_root=${printRoot(shard_head_root)}, " +
        "shard_transition=${shard_transition}, " +
        "source=$source, " +
        "target=$target)"
  }

  fun toStringShort(): String {
    return "(slot=$slot, shard=$shard, beacon_block_root=${printRoot(beacon_block_root)}"
  }
}

class DepositMessage : AbstractImmutableContainer {
  val pubkey: BLSPubkey
    get() = Bytes48.wrap(ViewUtils.getAllBytes(getAny(0)))
  val withdrawal_credentials: Bytes32
    get() = (get(1) as Bytes32View).get()
  val amount: Gwei
    get() = (get(2) as UInt64View).get().toUInt64()

  constructor(pubkey: BLSPubkey, withdrawal_credentials: Bytes32, amount: Gwei) : super(
    TYPE,
    ViewUtils.createVectorFromBytes(pubkey),
    Bytes32View(withdrawal_credentials),
    UInt64View(amount.toUInt64())
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<DepositMessage>(
      listOf(Bytes48Type, BasicViewTypes.BYTES32_TYPE, BasicViewTypes.UINT64_TYPE),
      ::DepositMessage
    )
  }
}

class DepositData : AbstractImmutableContainer {
  val pubkey: BLSPubkey
    get() = Bytes48.wrap(ViewUtils.getAllBytes(getAny(0)))
  val withdrawal_credentials: Bytes32
    get() = (get(1) as Bytes32View).get()
  val amount: Gwei
    get() = (get(2) as UInt64View).get().toUInt64()
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(3)))

  constructor(
    pubkey: BLSPubkey,
    withdrawal_credentials: Bytes32,
    amount: Gwei,
    signature: BLSSignature
  ) : super(
    TYPE,
    ViewUtils.createVectorFromBytes(pubkey),
    Bytes32View(withdrawal_credentials),
    UInt64View(amount.toUInt64()),
    ViewUtils.createVectorFromBytes(signature.wrappedBytes)
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<DepositData>(
      listOf(
        Bytes48Type, BasicViewTypes.BYTES32_TYPE, BasicViewTypes.UINT64_TYPE,
        Bytes96Type
      ),
      ::DepositData
    )
  }
}

class Attestation : AbstractImmutableContainer {
  val aggregation_bits: SSZBitlist
    get() = SSZBitlistImpl(getAny(0))
  val data: AttestationData
    get() = getAny(1)
  val custody_bits_blocks: SSZList<SSZBitlist>
    get() = SSZListImpl<SSZBitlist, ListViewRead<BitView>>(getAny(2)) { v ->
      SSZBitlistImpl(v)
    }
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(3)))

  constructor(
    aggregation_bits: SSZBitlist,
    data: AttestationData,
    custody_bits_blocks: SSZList<SSZBitlist>,
    signature: BLSSignature
  ) : super(
    TYPE,
    *wrapValues(
      aggregation_bits,
      data,
      custody_bits_blocks,
      signature
    )
  )

  constructor(
    aggregation_bits: SSZBitlist,
    data: AttestationData,
    signature: BLSSignature
  ) : super(
    TYPE,
    *wrapValues(
      aggregation_bits,
      data,
      ListViewType<ListViewRead<BitView>>(
        ListViewType<BitView>(
          BasicViewTypes.BIT_TYPE,
          MAX_VALIDATORS_PER_COMMITTEE.toLong()
        ), MAX_VALIDATORS_PER_COMMITTEE.toLong()
      ).default,
      signature
    )
  )

  constructor(fullAttestation: FullAttestation) : this(
    fullAttestation.aggregation_bits,
    AttestationData(fullAttestation.data),
    fullAttestation.custody_bits_blocks,
    fullAttestation.signature
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<Attestation>(
      listOf(
        ListViewType<BitView>(BasicViewTypes.BIT_TYPE, MAX_VALIDATORS_PER_COMMITTEE.toLong()),
        AttestationData.TYPE,
        ListViewType<ListViewRead<BitView>>(
          ListViewType<BitView>(
            BasicViewTypes.BIT_TYPE,
            MAX_VALIDATORS_PER_COMMITTEE.toLong()
          ), MAX_VALIDATORS_PER_COMMITTEE.toLong()
        ),
        Bytes96Type
      ),
      ::Attestation
    )
  }

  override fun toString(): String {
    return "Attestation(signed_vals=${aggregation_bits.bitsSet().size}, data=$data)"
  }

  fun toStringShort(): String {
    return "(signed_vals=${aggregation_bits.bitsSet().size}, data=${data.toStringShort()})"
  }
}

class FullAttestation : AbstractImmutableContainer {
  val aggregation_bits: SSZBitlist
    get() = SSZBitlistImpl(getAny(0))
  val data: FullAttestationData
    get() = getAny(1)
  val custody_bits_blocks: SSZList<SSZBitlist>
    get() = SSZListImpl<SSZBitlist, ListViewRead<BitView>>(getAny(2)) { v ->
      SSZBitlistImpl(v)
    }
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(3)))

  constructor(
    aggregation_bits: SSZBitlist,
    data: FullAttestationData,
    custody_bits_blocks: SSZList<SSZBitlist>,
    signature: BLSSignature
  ) : super(
    TYPE,
    *wrapValues(
      aggregation_bits,
      data,
      custody_bits_blocks,
      signature
    )
  )

  constructor(
    aggregation_bits: SSZBitlist,
    data: FullAttestationData,
    signature: BLSSignature
  ) : super(
    TYPE,
    *wrapValues(
      aggregation_bits,
      data,
      ListViewType<ListViewRead<BitView>>(
        ListViewType<BitView>(
          BasicViewTypes.BIT_TYPE,
          MAX_VALIDATORS_PER_COMMITTEE.toLong()
        ), MAX_VALIDATORS_PER_COMMITTEE.toLong()
      ).default,
      signature
    )
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<FullAttestation>(
      listOf(
        ListViewType<BitView>(BasicViewTypes.BIT_TYPE, MAX_VALIDATORS_PER_COMMITTEE.toLong()),
        FullAttestationData.TYPE,
        ListViewType<ListViewRead<BitView>>(
          ListViewType<BitView>(
            BasicViewTypes.BIT_TYPE,
            MAX_VALIDATORS_PER_COMMITTEE.toLong()
          ), MAX_VALIDATORS_PER_COMMITTEE.toLong()
        ),
        Bytes96Type
      ),
      ::FullAttestation
    )
  }

  override fun toString(): String {
    return "FullAttestation(signed_vals=${aggregation_bits.bitsSet().size}, data=$data)"
  }

  fun toStringFull(): String {
    return "FullAttestation(signed_vals=${aggregation_bits.bitsSet().size}, data=${data.toStringFull()})"
  }

  fun toStringShort(): String {
    return "(signed_vals=${aggregation_bits.bitsSet().size}, data=${data.toStringShort()})"
  }
}

class IndexedAttestation : AbstractImmutableContainer {
  val attesting_indices: SSZList<ValidatorIndex>
    get() = SSZListImpl<ValidatorIndex, UInt64View>(getAny(0)) { v -> v.get().toUInt64() }
  val data: AttestationData
    get() = getAny(1)
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(2)))

  constructor(
    attesting_indices: SSZList<ValidatorIndex>,
    data: AttestationData,
    signature: BLSSignature
  ) : super(
    TYPE,
    *wrapValues(
      attesting_indices,
      data,
      signature
    )
  )

  constructor(
    attesting_indices: List<ValidatorIndex>,
    data: AttestationData,
    signature: BLSSignature
  ) : this(
    SSZListImpl(
      BasicViewTypes.UINT64_TYPE,
      MAX_VALIDATORS_PER_COMMITTEE,
      attesting_indices,
      { v -> getBasicValue<ValidatorIndex>(v) },
      { u -> wrapBasicValue<UInt64View>(u) }
    ),
    data,
    signature
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<IndexedAttestation>(
      listOf(
        ListViewType<UInt64View>(BasicViewTypes.UINT64_TYPE, MAX_VALIDATORS_PER_COMMITTEE.toLong()),
        AttestationData.TYPE, Bytes96Type
      ),
      ::IndexedAttestation
    )
  }
}

class AttesterSlashing : AbstractImmutableContainer {
  val attestation_1: IndexedAttestation
    get() = getAny(0)
  val attestation_2: IndexedAttestation
    get() = getAny(1)

  constructor(attestation_1: IndexedAttestation, attestation_2: IndexedAttestation) : super(
    TYPE,
    attestation_1,
    attestation_2
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<AttesterSlashing>(
      listOf(IndexedAttestation.TYPE, IndexedAttestation.TYPE),
      ::AttesterSlashing
    )
  }
}

class Deposit : AbstractImmutableContainer {
  val proof: SSZVector<Bytes32>
    get() = SSZVectorImpl(getAny(0), Bytes32View::get)
  val data: DepositData
    get() = getAny(1)

  constructor(proof: SSZVector<Bytes32>, data: DepositData) : super(
    TYPE,
    (proof as SSZAbstractCollection<*, *>).view,
    data
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<Deposit>(
      listOf(
        VectorViewType<Bytes32View>(
          BasicViewTypes.BYTES32_TYPE,
          (DEPOSIT_CONTRACT_TREE_DEPTH + 1uL).toLong()
        ), DepositData.TYPE
      ), ::Deposit
    )
  }
}

class VoluntaryExit : AbstractImmutableContainer {
  val epoch: Epoch
    get() = (get(0) as UInt64View).get().toUInt64()
  val validator_index: ValidatorIndex
    get() = (get(1) as UInt64View).get().toUInt64()

  constructor(epoch: Epoch, validator_index: ValidatorIndex) : super(
    TYPE, UInt64View(epoch.toUInt64()), UInt64View(validator_index.toUInt64())
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<VoluntaryExit>(
      listOf(BasicViewTypes.UINT64_TYPE, BasicViewTypes.UINT64_TYPE), ::VoluntaryExit
    )
  }
}

class SignedVoluntaryExit : AbstractImmutableContainer {
  val message: VoluntaryExit
    get() = getAny(0)
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(1)))

  constructor(message: VoluntaryExit, signature: BLSSignature) : super(
    TYPE,
    message,
    ViewUtils.createVectorFromBytes(signature.wrappedBytes)
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<SignedVoluntaryExit>(
      listOf(
        VoluntaryExit.TYPE,
        Bytes96Type
      ), ::SignedVoluntaryExit
    )
  }
}

class ProposerSlashing : AbstractImmutableContainer {
  val signed_header_1: SignedBeaconBlockHeader
    get() = getAny(0)
  val signed_header_2: SignedBeaconBlockHeader
    get() = getAny(1)

  constructor(signed_header_1: SignedBeaconBlockHeader, signed_header_2: SignedBeaconBlockHeader)
      : super(TYPE, signed_header_1, signed_header_2)

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<ProposerSlashing>(
      listOf(SignedBeaconBlockHeader.TYPE, SignedBeaconBlockHeader.TYPE), ::ProposerSlashing
    )
  }
}

class CustodyKeyReveal : AbstractImmutableContainer {
  val revealer_index: ValidatorIndex
    get() = (get(0) as UInt64View).get().toUInt64()
  val reveal: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(1)))

  constructor(revealer_index: ValidatorIndex, reveal: BLSSignature)
      : super(
    TYPE,
    UInt64View(revealer_index.toUInt64()),
    ViewUtils.createVectorFromBytes(reveal.wrappedBytes)
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<CustodyKeyReveal>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        Bytes96Type
      ), ::CustodyKeyReveal
    )
  }
}

class EarlyDerivedSecretReveal : AbstractImmutableContainer {
  val revealed_index: ValidatorIndex
    get() = (get(0) as UInt64View).get().toUInt64()
  val epoch: Epoch
    get() = (get(1) as UInt64View).get().toUInt64()
  val reveal: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(2)))
  val masker_index: ValidatorIndex
    get() = (get(3) as UInt64View).get().toUInt64()
  val mask: Bytes32
    get() = (get(4) as Bytes32View).get()

  constructor(
    revealed_index: ValidatorIndex,
    epoch: Epoch,
    reveal: BLSSignature,
    masker_index: ValidatorIndex,
    mask: Bytes32
  ) : super(
    TYPE,
    UInt64View(revealed_index.toUInt64()),
    UInt64View(epoch.toUInt64()),
    ViewUtils.createVectorFromBytes(reveal.wrappedBytes),
    UInt64View(masker_index.toUInt64()),
    Bytes32View(mask)
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<EarlyDerivedSecretReveal>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        Bytes96Type,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE
      ), ::EarlyDerivedSecretReveal
    )
  }
}

class CustodySlashing : AbstractImmutableContainer {
  val data_index: uint64
    get() = (get(0) as UInt64View).get().toUInt64()
  val malefactor_index: ValidatorIndex
    get() = (get(1) as UInt64View).get().toUInt64()
  val malefactor_secret: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(2)))
  val whistleblower_index: ValidatorIndex
    get() = (get(3) as UInt64View).get().toUInt64()
  val shard_transition: ShardTransition
    get() = getAny(4)
  val attestation: Attestation
    get() = getAny(5)
  val data: SSZByteList
    get() = SSZByteListImpl(getAny(6))

  constructor(
    data_index: uint64,
    malefactor_index: ValidatorIndex,
    malefactor_secret: BLSSignature,
    whistleblower_index: ValidatorIndex,
    shard_transition: ShardTransition,
    attestation: Attestation,
    data: SSZByteList
  ) : super(
    TYPE,
    UInt64View(data_index.toUInt64()),
    UInt64View(malefactor_index.toUInt64()),
    ViewUtils.createVectorFromBytes(malefactor_secret.wrappedBytes),
    UInt64View(whistleblower_index.toUInt64()),
    shard_transition,
    attestation,
    (data as SSZAbstractCollection<*, *>).view
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<CustodySlashing>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        Bytes96Type,
        BasicViewTypes.UINT64_TYPE,
        ShardTransition.TYPE,
        Attestation.TYPE,
        ListViewType<ByteView>(BasicViewTypes.BYTE_TYPE, MAX_SHARD_BLOCK_SIZE.toLong())
      ), ::CustodySlashing
    )
  }
}

class SignedCustodySlashing : AbstractImmutableContainer {
  val message: CustodySlashing
    get() = getAny(0)
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(1)))

  constructor(message: CustodySlashing, signature: BLSSignature)
      : super(TYPE, message, ViewUtils.createVectorFromBytes(signature.wrappedBytes))

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<SignedCustodySlashing>(
      listOf(
        CustodySlashing.TYPE,
        Bytes96Type
      ),
      ::SignedCustodySlashing
    )
  }
}

class AttestationCustodyBit : AbstractImmutableContainer {
  val attestation_data_root: Root
    get() = (get(0) as Bytes32View).get()
  val block_index: uint64
    get() = (get(1) as UInt64View).get().toUInt64()
  val bit: boolean
    get() = (get(2) as BitView).get()


  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(attestation_data_root: Root, block_index: uint64, bit: boolean) : super(
    TYPE, Bytes32View(attestation_data_root), UInt64View(block_index.toUInt64()), BitView(bit)
  )

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<AttestationCustodyBit>(
      listOf(BasicViewTypes.BYTES32_TYPE, BasicViewTypes.UINT64_TYPE, BasicViewTypes.BIT_TYPE),
      ::AttestationCustodyBit
    )
  }
}

class CustodyChunkChallenge : AbstractImmutableContainer {
  val responder_index: ValidatorIndex
    get() = (get(0) as UInt64View).get().toUInt64()
  val shard_transition: ShardTransition
    get() = getAny(1)
  val attestation: Attestation
    get() = getAny(2)
  val data_index: uint64
    get() = (get(3) as UInt64View).get().toUInt64()
  val chunk_index: uint64
    get() = (get(4) as UInt64View).get().toUInt64()

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(
    responder_index: ValidatorIndex,
    shard_transition: ShardTransition,
    attestation: Attestation,
    data_index: uint64,
    chunk_index: uint64
  ) : super(
    TYPE,
    UInt64View(responder_index.toUInt64()),
    shard_transition,
    attestation,
    UInt64View(data_index.toUInt64()),
    UInt64View(chunk_index.toUInt64())
  )

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<CustodyChunkChallenge>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        ShardTransition.TYPE,
        Attestation.TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE
      ), ::CustodyChunkChallenge
    )
  }
}

class CustodyChunkResponse : AbstractImmutableContainer {
  val challenge_index: uint64
    get() = (get(0) as UInt64View).get().toUInt64()
  val chunk_index: uint64
    get() = (get(1) as UInt64View).get().toUInt64()
  val chunk: SSZByteVector
    get() = SSZByteVectorImpl(getAny<VectorViewRead<ByteView>>(2))
  val branch: SSZVector<Root>
    get() = SSZVectorImpl(getAny(3), Bytes32View::get)

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(
    challenge_index: uint64,
    chunk_index: uint64,
    chunk: SSZByteVector,
    branch: SSZVector<Root>
  ) : super(
    TYPE,
    UInt64View(challenge_index.toUInt64()),
    UInt64View(chunk_index.toUInt64()),
    (chunk as SSZAbstractCollection<*, *>).view,
    (branch as SSZAbstractCollection<*, *>).view
  )

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<CustodyChunkChallenge>(
      listOf(
        BasicViewTypes.UINT64_TYPE, BasicViewTypes.UINT64_TYPE,
        VectorViewType<ByteView>(BasicViewTypes.BYTE_TYPE, BYTES_PER_CUSTODY_CHUNK.toLong()),
        VectorViewType<ByteView>(BasicViewTypes.BYTE_TYPE, CUSTODY_RESPONSE_DEPTH.value.toLong())
      ), ::CustodyChunkChallenge
    )
  }
}
