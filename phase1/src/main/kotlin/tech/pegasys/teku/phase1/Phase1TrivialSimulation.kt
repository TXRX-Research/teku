package tech.pegasys.teku.phase1

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.bls.BLSSecretKey
import tech.pegasys.teku.datastructures.util.MockStartBeaconStateGenerator
import tech.pegasys.teku.datastructures.util.MockStartDepositGenerator
import tech.pegasys.teku.datastructures.util.MockStartValidatorKeyPairFactory
import tech.pegasys.teku.phase1.integration.datastructures.Attestation
import tech.pegasys.teku.phase1.integration.datastructures.AttestationData
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlockBody
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.Checkpoint
import tech.pegasys.teku.phase1.integration.datastructures.ShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.ShardTransition
import tech.pegasys.teku.phase1.integration.datastructures.SignedBeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.integration.spec.get_shard_block_signature
import tech.pegasys.teku.phase1.integration.ssz.SSZBitlistImpl
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.onotole.deps.BLS12381
import tech.pegasys.teku.phase1.onotole.phase1.CommitteeIndex
import tech.pegasys.teku.phase1.onotole.phase1.GENESIS_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.MAX_VALIDATORS_PER_COMMITTEE
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.SECONDS_PER_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.SLOTS_PER_EPOCH
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.pylib.pyint
import tech.pegasys.teku.phase1.onotole.ssz.Sequence
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import java.math.BigInteger
import java.util.*
import tech.pegasys.teku.datastructures.blocks.BeaconBlock as Phase0Block

private val SLOTS = 128uL * SLOTS_PER_EPOCH
private val blsKeyPairs = MockStartValidatorKeyPairFactory().generateKeyPairs(0, 16)
private val rnd = Random(1)
private val spec = Phase1Spec(BLS12381)

fun main() {
  var state = getGenesisState()
  val genesis = Phase0Block(state.hashTreeRoot())
  var parentRoot = genesis.hash_tree_root()
  val store = spec.get_forkchoice_store(state)
  for (slot in 1uL..SLOTS) {
    // compute attestations as if they were computed in the previous slot
    val (attestations, shardTransitions, shardBlocks) = computeAttestations(parentRoot, state)

    // feed the fork choice with shard blocks starting from after GENESIS_SLOT
    if (state.slot > GENESIS_SLOT) {
      shardBlocks.forEach {
        val shard = it.message.shard
        spec.on_shard_block(store, it)

        val pendingShardBlocks = spec.get_pending_shard_blocks(store, shard)
        assert(shardBlocksDict[shard]!!.message.hashTreeRoot() == spec.get_shard_head(store, shard))
        assert(pendingShardBlocks.size == 1)
        assert(pendingShardBlocks[0] == shardBlocksDict[shard]!!)
      }
    }

    // feed the fork choice with attestations a slot after
    spec.on_tick(store, state.genesis_time + slot * SECONDS_PER_SLOT)
    attestations.forEach { spec.on_attestation(store, it) }

    val signedBlock = produceBlock(state.copy(), slot, parentRoot, attestations, shardTransitions)
    parentRoot = signedBlock.message.hashTreeRoot()
    state = spec.state_transition(state, signedBlock)
    state = state.applyChanges()

    spec.on_block(store, signedBlock)

    assert(signedBlock.message.hashTreeRoot() == spec.get_head(store))

    println("Slot $slot: block = $signedBlock, state = $state")
    if (slot % SLOTS_PER_EPOCH == 0uL) {
      println("Validator balances: ${state.balances.mapIndexed { index, balance -> "$index: $balance" }
        .joinToString { it }}")
    }
  }
}

fun computeAttestations(
  headBlockRoot: Root,
  state: BeaconState
): Triple<List<Attestation>, List<ShardTransition>, List<SignedShardBlock>> {
  val attestationsWithTransitionAndBlock = (0 until state.validators.size)
    .mapNotNull {
      val assignment =
        spec.get_committee_assignment(state, spec.get_current_epoch(state), it.toULong())
      if (assignment != null && state.slot == assignment.third) Pair(
        assignment,
        ValidatorIndex(it.toULong())
      ) else null
    }
    .map { attest(it.second, it.first.first, it.first.second, headBlockRoot, state) }

  val attestations = listOf(attestationsWithTransitionAndBlock
    .map { it.first }
    .reduce { acc, att ->
      Attestation(
        acc.aggregation_bits or att.aggregation_bits,
        acc.data,
        spec.get_aggregate_signature(listOf(acc, att))
      )
    })
  val shardTransitions = attestationsWithTransitionAndBlock.map { it.second }.distinct()
  val shardBlocks = attestationsWithTransitionAndBlock.map { it.third }.distinct()

  return Triple(attestations, shardTransitions, shardBlocks)
}

fun produceBlock(
  state: BeaconState,
  slot: uint64,
  parentRoot: Root,
  attestations: List<Attestation>,
  shardTransitions: List<ShardTransition>
): SignedBeaconBlock {
  val stateWithAdvancedSlot = state.copy()
  if (stateWithAdvancedSlot.slot < slot) {
    spec.process_slots(stateWithAdvancedSlot, slot)
  }
  val proposerIndex = spec.get_beacon_proposer_index(stateWithAdvancedSlot)
  val proposerSecretKey = blsKeyPairs[proposerIndex.toInt()].secretKey.toPyint()
  val blockHeader = BeaconBlock(
    slot,
    proposerIndex,
    parentRoot,
    Bytes32.ZERO,
    BeaconBlockBody()
  )
  val randaoReveal = spec.get_epoch_signature(stateWithAdvancedSlot, blockHeader, proposerSecretKey)
  val (shards, winningRoots) = spec.get_shard_winning_roots(stateWithAdvancedSlot, attestations)
  val shardTransitionDict = shardTransitions.map { it.hashTreeRoot() to it }.toMap()
  val shardTransitionVector = List(MAX_SHARDS.toInt()) {
    val indexOfWinningRoot = shards.indexOf(it.toULong())
    if (indexOfWinningRoot >= 0) {
      val winningRoot = winningRoots[indexOfWinningRoot]
      shardTransitionDict[winningRoot] ?: ShardTransition()
    } else {
      ShardTransition()
    }
  }
  val block = BeaconBlock(
    slot,
    proposerIndex,
    parentRoot,
    Bytes32.ZERO,
    BeaconBlockBody(
      randaoReveal,
      state.eth1_data,
      Bytes32.rightPad(Bytes.ofUnsignedLong(proposerIndex.toLong())),
      attestations,
      shardTransitionVector
    )
  )
  val endState = spec.state_transition(state.copy(), SignedBeaconBlock(block), false)
  val blockWithStateRoot = block.copy(state_root = endState.applyChanges().hashTreeRoot())
  val signature = spec.get_block_signature(state, blockWithStateRoot, proposerSecretKey)

  return SignedBeaconBlock(blockWithStateRoot, signature)
}

private val shardBlocksDict = hashMapOf<Shard, SignedShardBlock>()
private const val SHARD_BLOCK_SIZE = 1024

fun produceShardBlock(
  shardParentRoot: Root,
  beaconHeadRoot: Root,
  shard: Shard,
  slot: Slot,
  beaconHeadState: BeaconState
): SignedShardBlock {
  val proposerIndex = spec.get_shard_proposer_index(beaconHeadState, slot, shard)
  val body = Bytes.random(SHARD_BLOCK_SIZE, rnd).toArrayUnsafe().toList()
  val shardBlock = ShardBlock(shardParentRoot, beaconHeadRoot, slot, shard, proposerIndex, body)

  println(shardBlock)

  return SignedShardBlock(
    shardBlock,
    spec.get_shard_block_signature(
      beaconHeadState,
      shardBlock,
      blsKeyPairs[proposerIndex.toInt()].secretKey.toPyint()
    )
  )
}

fun getOrProduceShardHead(
  shard: Shard,
  slot: Slot,
  beaconHeadRoot: Root,
  beaconHeadState: BeaconState
): SignedShardBlock {
  val existingSignedBlock =
    shardBlocksDict[shard] ?: SignedShardBlock(ShardBlock(slot = GENESIS_SLOT, shard = shard))
  val existingBlock = existingSignedBlock.message

  return if (existingBlock.slot < slot) {
    val newBlock = produceShardBlock(
      if (existingBlock.slot == GENESIS_SLOT) Root() else existingBlock.hashTreeRoot(),
      beaconHeadRoot,
      shard,
      slot,
      beaconHeadState
    )
    shardBlocksDict[shard] = newBlock
    newBlock
  } else {
    existingSignedBlock
  }
}

fun attest(
  index: ValidatorIndex,
  committee: Sequence<ValidatorIndex>,
  committeeIndex: CommitteeIndex,
  headBlockRoot: Root,
  headState: BeaconState
): Triple<Attestation, ShardTransition, SignedShardBlock> {
  val startSlot = spec.compute_start_slot_at_epoch(spec.get_current_epoch(headState))
  val shard = spec.compute_shard_from_committee_index(headState, committeeIndex, headState.slot)
  val epochBoundaryBlockRoot =
    if (startSlot == headState.slot) headBlockRoot else spec.get_block_root(
      headState,
      spec.get_current_epoch(headState)
    )
  val signedShardBlock = getOrProduceShardHead(shard, headState.slot, headBlockRoot, headState)
  val shardTransition = spec.get_shard_transition(headState, shard, listOf(signedShardBlock))
  val data = AttestationData(
    headState.slot,
    committeeIndex,
    headBlockRoot,
    headState.current_justified_checkpoint,
    Checkpoint(epoch = spec.get_current_epoch(headState), root = epochBoundaryBlockRoot),
    shard,
    if (signedShardBlock.message.slot == GENESIS_SLOT) Root() else signedShardBlock.message.hashTreeRoot(),
    shardTransition.hashTreeRoot()
  )
  val indexWithinCommittee = committee.indexOf(index).toULong()
  val attestation = Attestation(
    SSZBitlistImpl(MAX_VALIDATORS_PER_COMMITTEE).set(indexWithinCommittee),
    data,
    spec.get_attestation_signature(headState, data, blsKeyPairs[index.toInt()].secretKey.toPyint())
  )
  return Triple(attestation, shardTransition, signedShardBlock)
}

fun getGenesisState(): BeaconState {
  val deposits = MockStartDepositGenerator().createDeposits(blsKeyPairs)
  val state = spec.upgrade_to_phase1(
    MockStartBeaconStateGenerator().createInitialBeaconState(
      0uL.toUInt64(),
      deposits
    )
  )
  return state.applyChanges()
}

private fun BLSSecretKey.toPyint() = pyint(BigInteger(1, this.toBytes().toArray()))
