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
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlockHeader;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBlockAndState;
import tech.pegasys.teku.spec.datastructures.sharding.DataCommitment;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.sharding.ShardBlobSummary;
import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.block.AbstractBlockProcessor;
import tech.pegasys.teku.spec.logic.common.statetransition.results.BlockImportResult;
import tech.pegasys.teku.spec.logic.common.statetransition.results.BlockImportResult.FailureReason;
import tech.pegasys.teku.spec.logic.versions.rayonism.util.CommitteeUtilRayonism;
import tech.pegasys.teku.statetransition.block.BlockImporter;
import tech.pegasys.teku.statetransition.forkchoice.ForkChoice;
import tech.pegasys.teku.storage.client.ChainUpdater;
import tech.pegasys.teku.storage.storageSystem.InMemoryStorageSystemBuilder;
import tech.pegasys.teku.storage.storageSystem.StorageSystem;
import tech.pegasys.teku.weaksubjectivity.WeakSubjectivityValidator;

public class ShardingBlockImporterTest {
  private final Spec spec = TestSpecFactory.createMinimalRayonism();

  final StorageSystem storageSystem = InMemoryStorageSystemBuilder.create()
      .specProvider(spec)
      .numberOfValidators(128)
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
  private final SpecConfig genesisConfig = spec.getGenesisSpecConfig();

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
  }

  @Test
  public void importBlock_emptyBlockSuccess() throws Exception {
    SignedBlockAndState block1 = chainBuilder.generateBlockAtSlot(1);
    chainUpdater.setCurrentSlot(UInt64.valueOf(1));
    final BlockImportResult result = blockImporter.importBlock(block1.getBlock()).get();
    assertSuccessfulResult(result);
  }

  @Test
  public void importBlock_blockWithShardHeaderSuccess() throws Exception {

    SignedBlockAndState block1 = chainBuilder.generateBlockAtSlot(1);
    chainUpdater.setCurrentSlot(UInt64.valueOf(1));
    final BlockImportResult result1 = blockImporter.importBlock(block1.getBlock()).get();
    assertSuccessfulResult(result1);

    BeaconStateRayonism state1 = storageSystem.recentChainData().getChainHead().orElseThrow()
        .getState().toVersionRayonism().orElseThrow();

    CommitteeUtilRayonism committeeUtilRayonism = (CommitteeUtilRayonism) spec
        .atSlot(state1.getSlot()).getCommitteeUtil();

    UInt64 shardBlobSlot = state1.getSlot().increment();
    UInt64 shardBlobShard = UInt64.ZERO;
    int shardBlobProposedIndex = committeeUtilRayonism
        .getShardProposerIndex(state1, shardBlobSlot, shardBlobShard);

    DataCommitment dataCommitment = new DataCommitment(BLSPublicKey.empty(), UInt64.ZERO);

    BeaconBlock beaconBlock1 = block1.getBlock().getMessage();
    Bytes32 blockRoot1 = beaconBlock1.hashTreeRoot();
    ShardBlobSummary shardBlobSummary = new ShardBlobSummary(dataCommitment, BLSPublicKey.empty(),
        Bytes32.ZERO, blockRoot1);
    ShardBlobHeader shardBlobHeader = new ShardBlobHeader(shardBlobSlot, UInt64.ZERO,
        shardBlobSummary, UInt64.valueOf(shardBlobProposedIndex));
    BLSSecretKey blobProposerKey = chainBuilder.getValidatorKeys().get(shardBlobProposedIndex)
        .getSecretKey();
    BLSSignature blobSignature = BLS.sign(blobProposerKey, shardBlobHeader.hashTreeRoot());
    SignedShardBlobHeader signedShardBlobHeader = new SignedShardBlobHeader(shardBlobHeader,
        blobSignature);

    BlockOptions blockOptions = BlockOptions.create().addShardBlobHeader(signedShardBlobHeader);
    SignedBlockAndState block2 = chainBuilder.generateBlockAtSlot(2, blockOptions);
    chainUpdater.setCurrentSlot(UInt64.valueOf(2));
    final BlockImportResult result2 = blockImporter.importBlock(block2.getBlock()).get();
    assertSuccessfulResult(result2);

    BeaconStateRayonism state2 = storageSystem
        .recentChainData().getChainHead().orElseThrow().getState()
        .toVersionRayonism().orElseThrow();
    assertThat(state2.getCurrent_epoch_pending_shard_headers()).hasSize(1);
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
