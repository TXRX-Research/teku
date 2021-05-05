/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.teku.statetransition.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.core.ChainBuilder;
import tech.pegasys.teku.core.ChainBuilder.BlockOptions;
import tech.pegasys.teku.infrastructure.async.eventthread.InlineEventThread;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlockHeader;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBlockAndState;
import tech.pegasys.teku.spec.datastructures.sharding.DataCommitment;
import tech.pegasys.teku.spec.datastructures.sharding.PendingShardHeader;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobSummary;
import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.block.AbstractBlockProcessor;
import tech.pegasys.teku.spec.logic.common.statetransition.results.BlockImportResult;
import tech.pegasys.teku.spec.logic.common.statetransition.results.BlockImportResult.FailureReason;
import tech.pegasys.teku.spec.logic.common.util.BeaconStateUtil;
import tech.pegasys.teku.spec.logic.versions.rayonism.util.CommitteeUtilRayonism;
import tech.pegasys.teku.ssz.SszCollection;
import tech.pegasys.teku.ssz.SszList;
import tech.pegasys.teku.statetransition.block.BlockImporter;
import tech.pegasys.teku.statetransition.forkchoice.ForkChoice;
import tech.pegasys.teku.storage.client.ChainUpdater;
import tech.pegasys.teku.storage.storageSystem.InMemoryStorageSystemBuilder;
import tech.pegasys.teku.storage.storageSystem.StorageSystem;
import tech.pegasys.teku.weaksubjectivity.WeakSubjectivityValidator;

public class ShardingBlockImporterTest {
  private final Spec spec = TestSpecFactory.createMinimalRayonism();
  SpecConfigRayonism specConfigRayonism = spec.getSpecConfig(UInt64.ZERO).toVersionRayonism()
      .orElseThrow();
  private final int slotsPerEpoch = specConfigRayonism.getSlotsPerEpoch();
  private final int shardHeadersPerEpochCount =
      slotsPerEpoch * specConfigRayonism.getInitialActiveShards();
  private final int shardHeadersPerEpochMaxCount =
      slotsPerEpoch * specConfigRayonism.getMaxShards();

  private final BeaconStateUtil beaconStateUtil = spec.atSlot(UInt64.ZERO).getBeaconStateUtil();
  private final CommitteeUtilRayonism committeeUtilRayonism = spec
      .atSlot(UInt64.ZERO).getCommitteeUtil().toVersionRayonism().orElseThrow();

  final StorageSystem storageSystem = InMemoryStorageSystemBuilder.create()
      .specProvider(spec)
      .numberOfValidators(256)
      .build();
  private final ChainBuilder chainBuilder = storageSystem.chainBuilder();
  private final ChainUpdater chainUpdater = storageSystem.chainUpdater();
  final SignedBlockAndState genesis = storageSystem.chainUpdater().initializeGenesis();
  final ForkChoice forkChoice =
      ForkChoice.create(spec, new InlineEventThread(), storageSystem.recentChainData());
  private final WeakSubjectivityValidator weakSubjectivityValidator =
      mock(WeakSubjectivityValidator.class);
  final BlockImporter blockImporter =
      new BlockImporter(
          storageSystem.recentChainData(),
          forkChoice,
          weakSubjectivityValidator,
          storageSystem.eventBus());

  @BeforeAll
  public static void init() {
    AbstractBlockProcessor.BLS_VERIFY_DEPOSIT = false;
  }

  @AfterAll
  public static void dispose() {
    AbstractBlockProcessor.BLS_VERIFY_DEPOSIT = true;
  }

  @BeforeEach
  public void setup() {
    when(weakSubjectivityValidator.isBlockValid(any(), any())).thenReturn(true);
    // workaround: process 2 epochs to fill the
    // current_epoch_pending_shard_headers and previous_epoch_pending_shard_headers
    // with correct values
    advanceChain(slotsPerEpoch * 2);
  }

  @Test
  public void importBlock_emptyBlockSuccess() throws Exception {
    SignedBlockAndState block1 = chainBuilder.generateBlockAtSlot(17);
    chainUpdater.setCurrentSlot(UInt64.valueOf(17));
    final BlockImportResult result = blockImporter.importBlock(block1.getBlock()).get();
    assertSuccessfulResult(result);
  }

  @Test
  public void importBlock_blockWithShardHeaderNoAttestations() throws Exception {

    BeaconStateRayonism state1 = advanceChain(1);

    assertThat(state1.getCurrent_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);
    assertThat(state1.getPrevious_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);

    SignedShardBlobHeader signedShardBlobHeader = createDummyShardHeader(state1,
        chainUpdater.getHeadSlot(), UInt64.ZERO);
    BlockOptions blockOptions = BlockOptions.create().addShardBlobHeader(signedShardBlobHeader);
    BeaconStateRayonism state2 = advanceChain(2, blockOptions);

    assertThat(state2.getCurrent_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount + 1);
    assertThat(state2.getPrevious_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);

    BeaconStateRayonism state10 = advanceChain(slotsPerEpoch);

    assertThat(state10.getCurrent_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);
    assertThat(state10.getPrevious_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount + 1);

    BeaconStateRayonism state20 = advanceChain(slotsPerEpoch);

    assertThat(state20.getPrevious_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);
    assertThat(state20.getCurrent_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);
    assertThat(state20.getGrandparent_epoch_confirmed_commitments().stream().flatMap(
        SszCollection::stream).filter(dc -> !dc.equals(new DataCommitment()))).isEmpty();
  }

  @Test
  public void importBlock_blockWithShardHeaderAndAttestations() throws Exception {

    BeaconStateRayonism state1 = advanceChain(1);

    assertThat(state1.getCurrent_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);
    assertThat(state1.getPrevious_epoch_pending_shard_headers()).hasSize(shardHeadersPerEpochCount);

    SignedShardBlobHeader signedShardBlobHeader = createDummyShardHeader(state1,
        chainUpdater.getHeadSlot().increment(), UInt64.ZERO);
    BlockOptions blockOptions2 = BlockOptions.create().addShardBlobHeader(signedShardBlobHeader);
    BeaconStateRayonism state2 = advanceChain(1, blockOptions2);

    BeaconStateRayonism state3 = advanceChain(1);

    BlockOptions blockOptions4 = BlockOptions.create();
    chainBuilder.streamValidAttestationsForBlockAtSlot(state3.getSlot())
        .peek(System.out::println)
        .filter(att -> att.getData().getSlot().equals(signedShardBlobHeader.getMessage().getSlot()))
        .forEachOrdered(blockOptions4::addAttestation);
    BeaconStateRayonism state4 = advanceChain(1, blockOptions4);

    BeaconStateRayonism state20 = advanceChain(slotsPerEpoch * 2);

    Stream<DataCommitment> nonEmptyCommitments = state20
        .getGrandparent_epoch_confirmed_commitments().stream()
        .flatMap(SszCollection::stream)
        .filter(commitment -> commitment.getLength().isGreaterThan(0));
    assertThat(nonEmptyCommitments)
        .containsOnlyOnce(signedShardBlobHeader.getMessage().getBodySummary().getCommitment());
  }

  private BeaconStateRayonism getLastState() {
    return storageSystem
        .recentChainData().getBestState().orElseThrow()
        .toVersionRayonism().orElseThrow();
  }

  private BeaconStateRayonism advanceChain(long slotIncrement) {
    return advanceChain(slotIncrement, BlockOptions.create());
  }

  private BeaconStateRayonism advanceChain(long slotIncrement, BlockOptions blockOptions) {
    try {
      UInt64 newSlot = chainUpdater.getHeadSlot().plus(slotIncrement);
      chainUpdater.setCurrentSlot(newSlot);
      SignedBlockAndState block = chainBuilder.generateBlockAtSlot(newSlot, blockOptions);
      final BlockImportResult result = blockImporter.importBlock(block.getBlock()).get();
      assertSuccessfulResult(result);
      return getLastState();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private SignedShardBlobHeader createDummyShardHeader(BeaconStateRayonism state, UInt64 shardBlobSlot,
      UInt64 shardBlobShard) {
    int shardBlobProposedIndex = committeeUtilRayonism
        .getShardProposerIndex(state, shardBlobSlot, shardBlobShard);

    DataCommitment dataCommitment = new DataCommitment(BLSPublicKey.empty(),
        UInt64.valueOf(shardBlobSlot.longValue() << 16 | shardBlobShard.longValue()));

    Bytes32 blockRoot = spec.getBeaconStateUtil(shardBlobSlot)
        .getBlockRootAtSlotEx(state, shardBlobSlot.decrement());
    ShardBlobSummary shardBlobSummary = new ShardBlobSummary(dataCommitment, BLSPublicKey.empty(),
        Bytes32.ZERO, blockRoot);
    ShardBlobHeader shardBlobHeader = new ShardBlobHeader(shardBlobSlot, shardBlobShard,
        shardBlobSummary, UInt64.valueOf(shardBlobProposedIndex));
    BLSSecretKey blobProposerKey = chainBuilder.getValidatorKeys().get(shardBlobProposedIndex)
        .getSecretKey();
    final Bytes32 domain = beaconStateUtil.computeDomain(specConfigRayonism.getDomainShardProposer());
    Bytes signingRoot = beaconStateUtil.computeSigningRoot(shardBlobHeader, domain);
    BLSSignature blobSignature = BLS.sign(blobProposerKey, signingRoot);
    return new SignedShardBlobHeader(shardBlobHeader, blobSignature);
  }

  private void assertSuccessfulResult(final BlockImportResult result) {
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.getFailureReason()).isNull();
    assertThat(result.getFailureCause()).isEmpty();
  }

  private void assertImportFailed(
      final BlockImportResult result, final FailureReason expectedReason) {
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getFailureReason()).isEqualTo(expectedReason);
  }
}
