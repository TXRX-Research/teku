package tech.pegasys.teku.phase1.integration.datastructures

import tech.pegasys.teku.phase1.integration.Bytes96Type
import tech.pegasys.teku.phase1.integration.getBasicValue
import tech.pegasys.teku.phase1.integration.ssz.SSZAbstractCollection
import tech.pegasys.teku.phase1.integration.ssz.SSZBitvectorImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZVectorImpl
import tech.pegasys.teku.phase1.integration.ssz.getListView
import tech.pegasys.teku.phase1.integration.ssz.getVectorView
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.integration.wrapValues
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.phase1.Domain
import tech.pegasys.teku.phase1.onotole.phase1.INITIAL_ACTIVE_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.LIGHT_CLIENT_COMMITTEE_SIZE
import tech.pegasys.teku.phase1.onotole.phase1.MAX_ATTESTATIONS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_ATTESTER_SLASHINGS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_CUSTODY_CHUNK_CHALLENGES
import tech.pegasys.teku.phase1.onotole.phase1.MAX_CUSTODY_CHUNK_CHALLENGE_RESPONSES
import tech.pegasys.teku.phase1.onotole.phase1.MAX_CUSTODY_KEY_REVEALS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_CUSTODY_SLASHINGS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_DEPOSITS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_EARLY_DERIVED_SECRET_REVEALS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_PROPOSER_SLASHINGS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_VOLUNTARY_EXITS
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.Bytes96
import tech.pegasys.teku.phase1.onotole.ssz.SSZBitvector
import tech.pegasys.teku.phase1.onotole.ssz.SSZList
import tech.pegasys.teku.phase1.onotole.ssz.SSZVector
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.util.printRoot
import tech.pegasys.teku.phase1.util.printSignature
import tech.pegasys.teku.ssz.backing.VectorViewRead
import tech.pegasys.teku.ssz.backing.tree.TreeNode
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes
import tech.pegasys.teku.ssz.backing.type.ContainerViewType
import tech.pegasys.teku.ssz.backing.type.ListViewType
import tech.pegasys.teku.ssz.backing.type.VectorViewType
import tech.pegasys.teku.ssz.backing.view.AbstractImmutableContainer
import tech.pegasys.teku.ssz.backing.view.BasicViews.BitView
import tech.pegasys.teku.ssz.backing.view.BasicViews.Bytes32View
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt64View
import tech.pegasys.teku.ssz.backing.view.ViewUtils

class Eth1Data : AbstractImmutableContainer {
  val deposit_root: Root
    get() = getBasicValue(get(0))
  val deposit_count: uint64
    get() = getBasicValue(get(1))
  val block_hash: Bytes32
    get() = getBasicValue(get(2))

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(deposit_root: Root = Root(), deposit_count: uint64, block_hash: Bytes32) : super(
    TYPE,
    *wrapValues(
      deposit_root,
      deposit_count,
      block_hash
    )
  )

  constructor() : super(TYPE)

  fun copy(
    deposit_root: Root = this.deposit_root,
    deposit_count: uint64 = this.deposit_count,
    block_hash: Bytes32 = this.block_hash
  ): Eth1Data = Eth1Data(deposit_root, deposit_count, block_hash)

  companion object {
    val TYPE = ContainerViewType(
      listOf(BasicViewTypes.BYTES32_TYPE, BasicViewTypes.UINT64_TYPE, BasicViewTypes.BYTES32_TYPE),
      ::Eth1Data
    )
  }

  override fun toString(): String {
    return "(deposit_root='${printRoot(deposit_root)}', deposit_count='$deposit_count', block_hash='${printRoot(
      block_hash
    )}')"
  }
}

class BeaconBlockHeader : AbstractImmutableContainer {
  val slot: Slot
    get() = getBasicValue(get(0))
  val proposer_index: ValidatorIndex
    get() = getBasicValue(get(1))
  val parent_root: Root
    get() = getBasicValue(get(2))
  val state_root: Root
    get() = getBasicValue(get(3))
  val body_root: Root
    get() = getBasicValue(get(4))

  constructor(
    slot: Slot = Slot(),
    proposer_index: ValidatorIndex = ValidatorIndex(),
    parent_root: Root = Root(),
    state_root: Root = Root(),
    body_root: Root
  ) : super(
    TYPE, *wrapValues(
      slot,
      proposer_index,
      parent_root,
      state_root,
      body_root
    )
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)


  constructor() : super(TYPE)

  fun copy(
    slot: Slot = this.slot,
    proposer_index: ValidatorIndex = this.proposer_index,
    parent_root: Root = this.parent_root,
    state_root: Root = this.state_root,
    body_root: Root = this.body_root
  ): BeaconBlockHeader = BeaconBlockHeader(slot, proposer_index, parent_root, state_root, body_root)

  companion object {
    val TYPE = ContainerViewType(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE
      ), ::BeaconBlockHeader
    )
  }
}

class SignedBeaconBlockHeader : AbstractImmutableContainer {
  val message: BeaconBlockHeader
    get() = getAny(0)
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(1)))

  constructor(message: BeaconBlockHeader, signature: BLSSignature) : super(
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
    val TYPE = ContainerViewType<SignedBeaconBlockHeader>(
      listOf(
        BeaconBlockHeader.TYPE,
        Bytes96Type
      ), ::SignedBeaconBlockHeader
    )
  }
}

class BeaconBlockBody : AbstractImmutableContainer {
  val randao_reveal: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(0)))
  val eth1_data: Eth1Data
    get() = getAny(1)
  val graffiti: Bytes32
    get() = (get(2) as Bytes32View).get()
  val proposer_slashings: SSZList<ProposerSlashing>
    get() = SSZListImpl<ProposerSlashing, ProposerSlashing>(getAny(3)) { it }
  val attester_slashings: SSZList<AttesterSlashing>
    get() = SSZListImpl<AttesterSlashing, AttesterSlashing>(getAny(4)) { it }
  val attestations: SSZList<Attestation>
    get() = SSZListImpl<Attestation, Attestation>(getAny(5)) { it }
  val deposits: SSZList<Deposit>
    get() = SSZListImpl<Deposit, Deposit>(getAny(6)) { it }
  val voluntary_exits: SSZList<SignedVoluntaryExit>
    get() = SSZListImpl<SignedVoluntaryExit, SignedVoluntaryExit>(getAny(7)) { it }
  val chunk_challenges: SSZList<CustodyChunkChallenge>
    get() = SSZListImpl<CustodyChunkChallenge, CustodyChunkChallenge>(getAny(8)) { it }
  val chunk_challenge_responses: SSZList<CustodyChunkResponse>
    get() = SSZListImpl<CustodyChunkResponse, CustodyChunkResponse>(getAny(9)) { it }
  val custody_key_reveals: SSZList<CustodyKeyReveal>
    get() = SSZListImpl<CustodyKeyReveal, CustodyKeyReveal>(getAny(10)) { it }
  val early_derived_secret_reveals: SSZList<EarlyDerivedSecretReveal>
    get() = SSZListImpl<EarlyDerivedSecretReveal, EarlyDerivedSecretReveal>(getAny(11)) { it }
  val custody_slashings: SSZList<SignedCustodySlashing>
    get() = SSZListImpl<SignedCustodySlashing, SignedCustodySlashing>(getAny(12)) { it }
  val shard_transitions: SSZVector<ShardTransition>
    get() = SSZVectorImpl<ShardTransition, ShardTransition>(getAny(13)) { it }
  val light_client_bits: SSZBitvector
    get() = SSZBitvectorImpl(getAny<VectorViewRead<BitView>>(14))
  val light_client_signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(15)))

  constructor(
    randao_reveal: BLSSignature,
    eth1_data: Eth1Data,
    graffiti: Bytes32,
    proposer_slashings: SSZList<ProposerSlashing>,
    attester_slashings: SSZList<AttesterSlashing>,
    attestations: SSZList<Attestation>,
    deposits: SSZList<Deposit>,
    voluntary_exits: SSZList<SignedVoluntaryExit>,
    custody_slashings: SSZList<SignedCustodySlashing>,
    custody_key_reveals: SSZList<CustodyKeyReveal>,
    early_derived_secret_reveals: SSZList<EarlyDerivedSecretReveal>,
    shard_transitions: SSZVector<ShardTransition>,
    light_client_signature_bitfield: SSZBitvector,
    light_client_signature: BLSSignature
  ) : super(
    TYPE,
    ViewUtils.createVectorFromBytes(randao_reveal.wrappedBytes),
    eth1_data,
    Bytes32View(graffiti),
    (proposer_slashings as SSZAbstractCollection<*, *>).view,
    (attester_slashings as SSZAbstractCollection<*, *>).view,
    (attestations as SSZAbstractCollection<*, *>).view,
    (deposits as SSZAbstractCollection<*, *>).view,
    (voluntary_exits as SSZAbstractCollection<*, *>).view,
    (custody_slashings as SSZAbstractCollection<*, *>).view,
    (custody_key_reveals as SSZAbstractCollection<*, *>).view,
    (early_derived_secret_reveals as SSZAbstractCollection<*, *>).view,
    (shard_transitions as SSZAbstractCollection<*, *>).view,
    (light_client_signature_bitfield as SSZAbstractCollection<*, *>).view,
    ViewUtils.createVectorFromBytes(light_client_signature.wrappedBytes)
  )

  constructor(
    randao_reveal: BLSSignature,
    eth1_data: Eth1Data,
    graffiti: Bytes32,
    attestations: List<Attestation>,
    shard_transitions: List<ShardTransition>
  ) : super(
    TYPE,
    ViewUtils.createVectorFromBytes(randao_reveal.wrappedBytes),
    eth1_data,
    Bytes32View(graffiti),
    ListViewType<ProposerSlashing>(ProposerSlashing.TYPE, MAX_PROPOSER_SLASHINGS.toLong()).default,
    ListViewType<AttesterSlashing>(AttesterSlashing.TYPE, MAX_ATTESTER_SLASHINGS.toLong()).default,
    getListView(Attestation.TYPE, MAX_ATTESTATIONS, attestations) { it },
    ListViewType<Deposit>(Deposit.TYPE, MAX_DEPOSITS.toLong()).default,
    ListViewType<SignedVoluntaryExit>(
      SignedVoluntaryExit.TYPE,
      MAX_VOLUNTARY_EXITS.toLong()
    ).default,
    ListViewType<CustodyChunkChallenge>(
      CustodyChunkChallenge.TYPE,
      MAX_CUSTODY_CHUNK_CHALLENGES.toLong()
    ).default,
    ListViewType<CustodyChunkResponse>(
      CustodyChunkResponse.TYPE,
      MAX_CUSTODY_CHUNK_CHALLENGE_RESPONSES.toLong()
    ).default,
    ListViewType<CustodyKeyReveal>(CustodyKeyReveal.TYPE, MAX_CUSTODY_KEY_REVEALS.toLong()).default,
    ListViewType<EarlyDerivedSecretReveal>(
      EarlyDerivedSecretReveal.TYPE,
      MAX_EARLY_DERIVED_SECRET_REVEALS.toLong()
    ).default,
    ListViewType<SignedCustodySlashing>(
      SignedCustodySlashing.TYPE,
      MAX_CUSTODY_SLASHINGS.toLong()
    ).default,
    getVectorView(ShardTransition.TYPE, MAX_SHARDS, shard_transitions) { it },
    VectorViewType<BitView>(BasicViewTypes.BIT_TYPE, LIGHT_CLIENT_COMMITTEE_SIZE.toLong()).default,
    Bytes96Type.default
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<BeaconBlockBody>(
      listOf(
        Bytes96Type,
        Eth1Data.TYPE,
        BasicViewTypes.BYTES32_TYPE,
        ListViewType<ProposerSlashing>(ProposerSlashing.TYPE, MAX_PROPOSER_SLASHINGS.toLong()),
        ListViewType<AttesterSlashing>(AttesterSlashing.TYPE, MAX_ATTESTER_SLASHINGS.toLong()),
        ListViewType<Attestation>(Attestation.TYPE, MAX_ATTESTATIONS.toLong()),
        ListViewType<Deposit>(Deposit.TYPE, MAX_DEPOSITS.toLong()),
        ListViewType<SignedVoluntaryExit>(
          SignedVoluntaryExit.TYPE,
          MAX_VOLUNTARY_EXITS.toLong()
        ),
        ListViewType<CustodyChunkChallenge>(
          CustodyChunkChallenge.TYPE,
          MAX_CUSTODY_CHUNK_CHALLENGES.toLong()
        ),
        ListViewType<CustodyChunkResponse>(
          CustodyChunkResponse.TYPE,
          MAX_CUSTODY_CHUNK_CHALLENGE_RESPONSES.toLong()
        ),
        ListViewType<CustodyKeyReveal>(CustodyKeyReveal.TYPE, MAX_CUSTODY_KEY_REVEALS.toLong()),
        ListViewType<EarlyDerivedSecretReveal>(
          EarlyDerivedSecretReveal.TYPE,
          MAX_EARLY_DERIVED_SECRET_REVEALS.toLong()
        ),
        ListViewType<SignedCustodySlashing>(
          SignedCustodySlashing.TYPE,
          MAX_CUSTODY_SLASHINGS.toLong()
        ),
        VectorViewType<ShardTransition>(ShardTransition.TYPE, MAX_SHARDS.toLong()),
        VectorViewType<BitView>(BasicViewTypes.BIT_TYPE, LIGHT_CLIENT_COMMITTEE_SIZE.toLong()),
        Bytes96Type
      ), ::BeaconBlockBody
    )
  }

  override fun toString(): String {
    return "(eth1_data=$eth1_data, attestations=[${attestations.joinToString { it.toStringShort() }}])"
  }

  fun toStringFull(): String {
    return "(\n" +
        "  eth1_data=$eth1_data\n" +
        "  attestations:\n    ${attestations.joinToString("\n    ") { it.toString() }}\n" +
        "  shard_transitions:\n    ${(0uL until INITIAL_ACTIVE_SHARDS).map { shard_transitions[it] }
          .joinToString("\n    ") { it.toString() }}\n)"
  }
}

class BeaconBlock : AbstractImmutableContainer {
  val slot: Slot
    get() = (get(0) as UInt64View).get().toUInt64()
  val proposer_index: ValidatorIndex
    get() = (get(1) as UInt64View).get().toUInt64()
  val parent_root: Root
    get() = (get(2) as Bytes32View).get()
  val state_root: Root
    get() = (get(3) as Bytes32View).get()
  val body: BeaconBlockBody
    get() = getAny(4)

  constructor(
    slot: Slot,
    proposer_index: ValidatorIndex,
    parent_root: Root,
    state_root: Root,
    body: BeaconBlockBody
  ) : super(
    TYPE,
    UInt64View(slot.toUInt64()),
    UInt64View(proposer_index.toUInt64()),
    Bytes32View(parent_root),
    Bytes32View(state_root),
    body
  )

  constructor(state_root: Root) : this(
    Slot(),
    ValidatorIndex(),
    Root(),
    state_root,
    BeaconBlockBody()
  )

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<BeaconBlock>(
      listOf(
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.UINT64_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BasicViewTypes.BYTES32_TYPE,
        BeaconBlockBody.TYPE
      ), ::BeaconBlock
    )
  }

  fun copy(
    slot: Slot = this.slot,
    proposer_index: ValidatorIndex = this.proposer_index,
    parent_root: Root = this.parent_root,
    state_root: Root = this.state_root,
    body: BeaconBlockBody = this.body
  ): BeaconBlock = BeaconBlock(slot, proposer_index, parent_root, state_root, body)

  override fun toString(): String {
    return "BeaconBlock(" +
        "slot=$slot, " +
        "root=${printRoot(hashTreeRoot())}, " +
        "parent_root=${printRoot(parent_root)}, " +
        "state_root=${printRoot(state_root)}, " +
        "body=$body)"
  }

  fun toStringFull(): String {
    return "BeaconBlock(" +
        "slot=$slot, " +
        "root=${printRoot(hashTreeRoot())}, " +
        "parent_root=${printRoot(parent_root)}, " +
        "state_root=${printRoot(state_root)}, " +
        "proposer_index=$proposer_index, " +
        "body=${body.toStringFull()})"
  }
}

class SignedBeaconBlock : AbstractImmutableContainer {
  val message: BeaconBlock
    get() = getAny(0)
  val signature: BLSSignature
    get() = Bytes96(ViewUtils.getAllBytes(getAny(1)))

  constructor(message: BeaconBlock, signature: BLSSignature = BLSSignature()) : super(
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
    val TYPE = ContainerViewType<SignedBeaconBlock>(
      listOf(
        BeaconBlock.TYPE,
        Bytes96Type
      ),
      ::SignedBeaconBlock
    )
  }

  override fun toString(): String {
    return "SignedBeaconBlock(message='$message', signature='${printSignature(signature)}')"
  }
}

class SigningData : AbstractImmutableContainer {
  val object_root: Root
    get() = (get(0) as Bytes32View).get()
  val domain: Domain
    get() = (get(0) as Bytes32View).get()

  constructor(
    type: ContainerViewType<out AbstractImmutableContainer>?,
    backingNode: TreeNode?
  ) : super(type, backingNode)

  constructor(
    object_root: Root,
    domain: Domain
  ) : super(TYPE, Bytes32View(object_root), Bytes32View(domain))

  constructor() : super(TYPE)

  companion object {
    val TYPE = ContainerViewType<SigningData>(
      listOf(BasicViewTypes.BYTES32_TYPE, BasicViewTypes.BYTES32_TYPE), ::SigningData
    )
  }
}
