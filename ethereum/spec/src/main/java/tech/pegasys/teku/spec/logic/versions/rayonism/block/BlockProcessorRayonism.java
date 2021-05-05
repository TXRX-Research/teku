/*
 * Copyright 2021 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.spec.logic.versions.rayonism.block;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.toIntExact;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSignatureVerifier;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.cache.IndexedAttestationCache;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.SyncAggregate;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.rayonism.BeaconBlockBodyRayonism;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader;
import tech.pegasys.teku.spec.datastructures.operations.Attestation;
import tech.pegasys.teku.spec.datastructures.operations.AttestationData;
import tech.pegasys.teku.spec.datastructures.sharding.PendingShardHeader;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobSummary;
import tech.pegasys.teku.spec.datastructures.sharding.ShardProposerSlashing;
import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.state.PendingAttestation;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateRayonism;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.MutableBeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.block.AbstractBlockProcessor;
import tech.pegasys.teku.spec.logic.common.helpers.BeaconStateMutators;
import tech.pegasys.teku.spec.logic.common.helpers.Predicates;
import tech.pegasys.teku.spec.logic.common.operations.validation.AttestationDataStateTransitionValidator;
import tech.pegasys.teku.spec.logic.common.statetransition.exceptions.BlockProcessingException;
import tech.pegasys.teku.spec.logic.common.util.AttestationUtil;
import tech.pegasys.teku.spec.logic.common.util.BeaconStateUtil;
import tech.pegasys.teku.spec.logic.common.util.ExecutionPayloadUtil;
import tech.pegasys.teku.spec.logic.common.util.ValidatorsUtil;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.BeaconStateAccessorsRayonism;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.MiscHelpersRayonism;
import tech.pegasys.teku.spec.logic.versions.rayonism.util.CommitteeUtilRayonism;
import tech.pegasys.teku.ssz.SszList;
import tech.pegasys.teku.ssz.SszMutableList;
import tech.pegasys.teku.ssz.collections.SszBitlist;

public class BlockProcessorRayonism extends AbstractBlockProcessor {

  private static final Logger LOG = LogManager.getLogger();

  private final SpecConfigRayonism specConfigRayonism;
  private final MiscHelpersRayonism miscHelpersRayonism;
  private final BeaconStateAccessorsRayonism beaconStateAccessorsRayonism;
  private final ExecutionPayloadUtil executionPayloadUtil;
  private final CommitteeUtilRayonism committeeUtilRayonism;

  public BlockProcessorRayonism(
      final SpecConfigRayonism specConfig,
      final Predicates predicates,
      final MiscHelpersRayonism miscHelpers,
      final BeaconStateAccessorsRayonism beaconStateAccessors,
      final BeaconStateMutators beaconStateMutators,
      final BeaconStateUtil beaconStateUtil,
      final AttestationUtil attestationUtil,
      final ValidatorsUtil validatorsUtil,
      final AttestationDataStateTransitionValidator attestationValidator,
      final ExecutionPayloadUtil executionPayloadUtil,
      final CommitteeUtilRayonism committeeUtilRayonism) {
    super(
        specConfig,
        predicates,
        miscHelpers,
        beaconStateAccessors,
        beaconStateMutators,
        beaconStateUtil,
        attestationUtil,
        validatorsUtil,
        attestationValidator);

    this.miscHelpersRayonism = miscHelpers;
    this.executionPayloadUtil = executionPayloadUtil;
    this.beaconStateAccessorsRayonism = beaconStateAccessors;
    this.committeeUtilRayonism = committeeUtilRayonism;
    this.specConfigRayonism =
        specConfig
            .toVersionRayonism()
            .orElseThrow(
                () -> new IllegalArgumentException("Expected Rayonism version of specConfig"));
  }

  @Override
  public void processSyncCommittee(MutableBeaconState state, SyncAggregate syncAggregate)
      throws BlockProcessingException {
    throw new UnsupportedOperationException("No SyncCommittee in merge");
  }

  @Override
  public void processExecutionPayload(MutableBeaconState genericState, BeaconBlockBody genericBody)
      throws BlockProcessingException {
    try {
      final MutableBeaconStateRayonism state = MutableBeaconStateRayonism.required(genericState);
      final BeaconBlockBodyRayonism blockBody = BeaconBlockBodyRayonism.required(genericBody);
      final ExecutionPayload executionPayload = blockBody.getExecution_payload();

      // Pre-merge, skip processing
      if (!miscHelpersRayonism.isTransitionCompleted(state)
          && !miscHelpersRayonism.isTransitionBlock(state, blockBody)) {
        return;
      }

      if (miscHelpersRayonism.isTransitionCompleted(state)) {
        checkArgument(
            executionPayload
                .getParent_hash()
                .equals(state.getLatest_execution_payload_header().getBlock_hash()),
            "process_execution_payload: Verify that the parent matches");
        checkArgument(
            executionPayload
                .getNumber()
                .equals(state.getLatest_execution_payload_header().getNumber().increment()),
            "process_execution_payload: Verify that the number is consequent");
      }

      checkArgument(
          executionPayload
              .getTimestamp()
              .equals(miscHelpersRayonism.computeTimeAtSlot(state, state.getSlot())),
          "process_execution_payload: Verify that the timestamp is correct");

      boolean isExecutionPayloadValid =
          executionPayloadUtil.verifyExecutionStateTransition(executionPayload);

      checkArgument(
          isExecutionPayloadValid,
          "process_execution_payload: Verify that the payload is valid with respect to execution state transition");

      state.setLatestExecutionPayloadHeader(
          new ExecutionPayloadHeader(
              executionPayload.getBlock_hash(),
              executionPayload.getParent_hash(),
              executionPayload.getCoinbase(),
              executionPayload.getState_root(),
              executionPayload.getNumber(),
              executionPayload.getGas_limit(),
              executionPayload.getGas_used(),
              executionPayload.getTimestamp(),
              executionPayload.getReceipt_root(),
              executionPayload.getLogs_bloom(),
              executionPayload.getTransactions().hashTreeRoot()));

    } catch (IllegalArgumentException e) {
      LOG.warn(e.getMessage());
      throw new BlockProcessingException(e);
    }
  }

  @Override
  protected void processAttestation(
      MutableBeaconState genericState,
      Attestation attestation,
      IndexedAttestationProvider indexedAttestationProvider) {
    final MutableBeaconStateRayonism state = MutableBeaconStateRayonism.required(genericState);
    final AttestationData data = attestation.getData();

    PendingAttestation pendingAttestation =
        new PendingAttestation(
            attestation.getAggregation_bits(),
            data,
            state.getSlot().minus(data.getSlot()),
            UInt64.valueOf(beaconStateAccessors.getBeaconProposerIndex(state)));

    if (data.getTarget().getEpoch().equals(beaconStateAccessors.getCurrentEpoch(state))) {
      state.getCurrent_epoch_attestations().append(pendingAttestation);
    } else {
      state.getPrevious_epoch_attestations().append(pendingAttestation);
    }
    updatePendingVotes(state, attestation);
  }

  //  def update_pending_votes(state: BeaconState, attestation: Attestation) -> None:
  private void updatePendingVotes(MutableBeaconStateRayonism state, Attestation attestation) {
    //      # Find and update the PendingShardHeader object, invalid block if pending header not in
    // state
    //  if compute_epoch_at_slot(attestation.data.slot) == get_current_epoch(state):
    //    pending_headers = state.current_epoch_pending_shard_headers
    //  else:
    //    pending_headers = state.previous_epoch_pending_shard_headers
    final SszMutableList<PendingShardHeader> pendingHeaders;
    if (miscHelpers
        .computeEpochAtSlot(attestation.getData().getSlot())
        .equals(beaconStateAccessors.getCurrentEpoch(state))) {
      pendingHeaders = state.getCurrent_epoch_pending_shard_headers();
    } else {
      pendingHeaders = state.getPrevious_epoch_pending_shard_headers();
    }
    //  pending_header = None
    //  for header in pending_headers:
    //      if header.root == attestation.data.shard_header_root:
    //        pending_header = header
    int pendingHeaderIndex = 0;
    Optional<PendingShardHeader> maybePendingHeader = Optional.empty();
    for (int i = 0; i < pendingHeaders.size(); i++) {
      PendingShardHeader pendingHeader = pendingHeaders.get(i);
      if (pendingHeader.getRoot().equals(attestation.getData().getShard_header_root())) {
        maybePendingHeader = Optional.of(pendingHeader);
        pendingHeaderIndex = i;
        break;
      }
    }
    //  assert pending_header is not None
    PendingShardHeader pendingHeader =
        maybePendingHeader.orElseThrow(
            () ->
                new IllegalArgumentException(
                    "updatePendingVotes: attested shard header should be in the state pending list"));
    //  assert pending_header.slot == attestation.data.slot
    checkArgument(
        pendingHeader.getSlot().equals(attestation.getData().getSlot()),
        "updatePendingVotes: attestation slot is equal to header slot");
    //  assert pending_header.shard == compute_shard_from_committee_index(
    //      state,
    //      attestation.data.slot,
    //      attestation.data.index,
    //      )
    UInt64 shard =
        committeeUtilRayonism.computeShardFromCommitteeIndex(
            state, attestation.getData().getSlot(), attestation.getData().getIndex());
    checkArgument(
        pendingHeader.getShard().equals(shard),
        "updatePendingVotes: attestation index should match header shard");
    //  for i in range(len(pending_header.votes)):
    //    pending_header.votes[i] = pending_header.votes[i] or attestation.aggregation_bits[i]
    SszBitlist updatedVotes = pendingHeader.getVotes().or(attestation.getAggregation_bits());

    // TODO question: looks like an obsolete check
    // From discord:
    // Shouldn't the ShardHeader be unique with respect to (slot, shard)? There is just one shard
    // proposer for (slot, shard) who may publish just one ShardHeader (else he is subject to be
    // slashed).
    // Why we are looking for other pending_headers with the same (slot, shard) then?
    //
    //  # Check if the PendingShardHeader is eligible for expedited confirmation
    //  # Requirement 1: nothing else confirmed
    //  all_candidates = [
    //      c for c in pending_headers if
    //      (c.slot, c.shard) == (pending_header.slot, pending_header.shard)
    //  ]
    //  if True in [c.confirmed for c in all_candidates]:
    //      return

    //  # Requirement 2: >= 2/3 of balance attesting
    //  participants = get_attesting_indices(state, attestation.data, pending_header.votes)
    List<Integer> participants =
        attestationUtil.getAttestingIndices(state, attestation.getData(), updatedVotes);
    //  participants_balance = get_total_balance(state, participants)
    UInt64 participantsBalance = beaconStateAccessors.getTotalBalance(state, participants);
    //  full_committee = get_beacon_committee(state, attestation.data.slot, attestation.data.index)
    List<Integer> fullCommittee =
        beaconStateUtil.getBeaconCommittee(
            state, attestation.getData().getSlot(), attestation.getData().getIndex());
    //  full_committee_balance = get_total_balance(state, full_committee)
    UInt64 fullCommitteeBalance = beaconStateAccessors.getTotalBalance(state, fullCommittee);
    //  if participants_balance * 3 >= full_committee_balance * 2:
    //    pending_header.confirmed = True
    boolean pendingHeaderConfirmed =
        participantsBalance.times(3).isGreaterThanOrEqualTo(fullCommitteeBalance.times(2));

    PendingShardHeader updatedPendingHeader =
        new PendingShardHeader(pendingHeader, updatedVotes, pendingHeaderConfirmed);
    pendingHeaders.set(pendingHeaderIndex, updatedPendingHeader);
  }

  /**
   * Processes all block body operations
   *
   * @param state
   * @param body
   * @throws BlockProcessingException
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#operations</a>
   */
  @Override
  public void processOperationsNoValidation(
      MutableBeaconState state,
      BeaconBlockBody body,
      IndexedAttestationCache indexedAttestationCache)
      throws BlockProcessingException {
    try {
      BeaconBlockBodyRayonism bodyRayonism =
          body.toVersionRayonism()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("Expected Rayonism version of BeaconBlockBody"));

      checkArgument(
          body.getDeposits().size()
              == Math.min(
                  specConfig.getMaxDeposits(),
                  toIntExact(
                      state
                          .getEth1_data()
                          .getDeposit_count()
                          .minus(state.getEth1_deposit_index())
                          .longValue())),
          "process_operations: Verify that outstanding deposits are processed up to the maximum number of deposits");

      checkArgument(
          bodyRayonism.getShard_headers().size()
              <= specConfigRayonism.getMaxShardHeadersPerShard()
                  * beaconStateAccessorsRayonism.getActiveShardCount(
                      state, beaconStateAccessors.getCurrentEpoch(state)),
          "process_operations: Verify that number of shard headers doesn't exceeds maximum threshold");

      processProposerSlashingsNoValidation(state, body.getProposer_slashings());
      processAttesterSlashings(state, body.getAttester_slashings());
      processShardProposerSlashing(state, bodyRayonism.getShard_proposer_slashings());
      processShardHeaders(state, bodyRayonism.getShard_headers());
      processAttestations(state, body.getAttestations(), indexedAttestationCache, false);
      processDeposits(state, body.getDeposits());
      processVoluntaryExitsNoValidation(state, body.getVoluntary_exits());
      // @process_shard_receipt_proofs
    } catch (IllegalArgumentException e) {
      LOG.warn(e.getMessage());
      throw new BlockProcessingException(e);
    }
  }

  @SuppressWarnings("UnusedVariable")
  private void processShardProposerSlashing(
      MutableBeaconState state, SszList<ShardProposerSlashing> shard_proposer_slashings) {
    // TODO to implement
  }

  protected void processShardHeaders(
      MutableBeaconState state, SszList<SignedShardBlobHeader> shard_headers)
      throws BlockProcessingException {
    try {
      MutableBeaconStateRayonism stateRayonism = MutableBeaconStateRayonism.required(state);
      for (SignedShardBlobHeader shardHeader : shard_headers) {
        processShardHeader(stateRayonism, shardHeader);
      }
    } catch (IllegalArgumentException e) {
      LOG.warn(e.getMessage());
      throw new BlockProcessingException(e);
    }
  }

  protected void processShardHeader(
      MutableBeaconStateRayonism state, SignedShardBlobHeader shardHeader) {
    ShardBlobHeader header = shardHeader.getMessage();
    UInt64 headerEpoch = miscHelpers.computeEpochAtSlot(header.getSlot());
    ShardBlobSummary bodySummary = header.getBodySummary();

    //    # Verify the header is not 0, and not from the future.
    //    assert Slot(0) < header.slot <= state.slot
    checkArgument(
        header.getSlot().isGreaterThan(0) && header.getSlot().isLessThanOrEqualTo(state.getSlot()),
        "process_shard_header: Verify the header is not 0, and not from the future");
    //    # Verify that the header is within the processing time window
    //    assert header_epoch in [get_previous_epoch(state), get_current_epoch(state)]
    checkArgument(
        headerEpoch.equals(beaconStateAccessors.getPreviousEpoch(state))
            || headerEpoch.equals(beaconStateAccessors.getCurrentEpoch(state)),
        "process_shard_header: Verify that the header is within the processing time window");
    //    # Verify that the shard is active
    //    assert header.shard < get_active_shard_count(state, header_epoch)
    checkArgument(
        header
            .getShard()
            .isLessThan(beaconStateAccessorsRayonism.getActiveShardCount(state, headerEpoch)),
        "process_shard_header: Verify that the shard is active");
    //    # Verify that the block root matches,
    //    # to ensure the header will only be included in this specific Beacon Chain sub-tree.
    //    assert header.body_summary.beacon_block_root == get_block_root_at_slot(state, header.slot - 1)
    checkArgument(
        bodySummary
            .getBeaconBlockRoot()
            .equals(beaconStateUtil.getBlockRootAtSlot(state, header.getSlot().minus(1))),
        "process_shard_header: Verify that the block root matches");
    //    # Verify proposer
    //    assert header.proposer_index == get_shard_proposer_index(state, header.slot, header.shard)
    checkArgument(
        header.getProposerIndex().intValue()
            == committeeUtilRayonism.getShardProposerIndex(
                state, header.getSlot(), header.getShard()));

    // Signature verification is performed by the BlockValidator separately
    // see BlockProcessorRayonism.verifyShardHeadersSignatures()

    // TODO
    //    # Verify the length by verifying the degree.
    //        body_summary = header.body_summary
    //    if body_summary.commitment.length == 0:
    //    assert body_summary.degree_proof == G1_SETUP[0]
    //    assert (
    //        bls.Pairing(body_summary.degree_proof, G2_SETUP[0])
    //            == bls.Pairing(body_summary.commitment.point,
    //              G2_SETUP[-body_summary.commitment.length])
    //    )

    //    # Get the correct pending header list
    //    if header_epoch == get_current_epoch(state):
    //      pending_headers = state.current_epoch_pending_shard_headers
    //    else:
    //      pending_headers = state.previous_epoch_pending_shard_headers
    SszMutableList<PendingShardHeader> pendingHeaders =
        headerEpoch.equals(beaconStateAccessors.getCurrentEpoch(state))
            ? state.getCurrent_epoch_pending_shard_headers()
            : state.getPrevious_epoch_pending_shard_headers();
    //    header_root = hash_tree_root(header)
    Bytes32 headerRoot = header.hashTreeRoot();
    //    # Check that this header is not yet in the pending list
    //    assert header_root not in [pending_header.root for pending_header in pending_headers]
    checkArgument(
        pendingHeaders.stream()
            .map(PendingShardHeader::getRoot)
            .noneMatch(r -> r.equals(headerRoot)),
        "process_shard_header: Check that this header is not yet in the pending list");
    //    # Include it in the pending list
    //    index = compute_committee_index_from_shard(state, header.slot, header.shard)
    UInt64 index =
        committeeUtilRayonism.computeCommitteeIndexFromShard(
            state, header.getSlot(), header.getShard());
    //    committee_length = len(get_beacon_committee(state, header.slot, index))
    int committeeLength = beaconStateUtil.getBeaconCommittee(state, header.getSlot(), index).size();
    //    pending_headers.append(PendingShardHeader(
    //        slot=header.slot,
    //        shard=header.shard,
    //        commitment=body_summary.commitment,
    //        root=header_root,
    //        votes=Bitlist[MAX_VALIDATORS_PER_COMMITTEE]([0] * committee_length),
    //    confirmed=False,
    //    ))
    PendingShardHeader newPendingHeader =
        new PendingShardHeader(
            header.getSlot(),
            header.getShard(),
            bodySummary.getCommitment(),
            headerRoot,
            PendingShardHeader.SSZ_SCHEMA.getVotesSchema().ofBits(committeeLength),
            false);
    pendingHeaders.append(newPendingHeader);
  }

  @Override
  public boolean verifyShardHeadersSignatures(BeaconStateRayonism state,
      BeaconBlockBodyRayonism blockBodyRayonism, BLSSignatureVerifier signatureVerifier) {
    return blockBodyRayonism.getShard_headers().stream()
        .map(header -> verifyShardHeadersSignature(state, header, signatureVerifier))
        .reduce((a, b) -> a & b).orElse(true);
  }

  private boolean verifyShardHeadersSignature(BeaconStateRayonism state,
      SignedShardBlobHeader header, BLSSignatureVerifier signatureVerifier) {
    //    # Verify signature
    //    signing_root = compute_signing_root(header, get_domain(state, DOMAIN_SHARD_PROPOSER))
    //    assert bls.Verify(state.validators[header.proposer_index].pubkey, signing_root,
    // signed_header.signature)

    final Bytes32 domain = beaconStateUtil.computeDomain(specConfigRayonism.getDomainShardProposer());
    Bytes root = beaconStateUtil.computeSigningRoot(header.getMessage(), domain);
    BLSPublicKey proposerPubKey = beaconStateAccessors
        .getValidatorPubKey(state, header.getMessage().getProposerIndex()).orElseThrow();
    return signatureVerifier.verify(proposerPubKey, root, header.getSignature());
  }
}
