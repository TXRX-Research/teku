package tech.pegasys.teku.spec.logic.versions.rayonism.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateRayonism;
import tech.pegasys.teku.spec.logic.versions.rayonism.helpers.BeaconStateAccessorsRayonism;

public class CommitteeUtilRayonismTest {

  private static final Spec spec = TestSpecFactory.createMinimalRayonism();
  SpecConfigRayonism specConfigRayonism = spec.getSpecConfig(UInt64.ZERO).toVersionRayonism()
      .orElseThrow();

  BeaconStateAccessorsRayonism stateAccessorsRayonism = mock(BeaconStateAccessorsRayonism.class);
  CommitteeUtilRayonism committeeUtil = new CommitteeUtilRayonism(specConfigRayonism,
      stateAccessorsRayonism,
      spec.atSlot(UInt64.ZERO).miscHelpers().toVersionMerge().orElseThrow());
  BeaconStateRayonism state = mock(BeaconStateRayonism.class);


  {
    when(stateAccessorsRayonism.getActiveShardCount(any(), any())).thenReturn(8);
    when(stateAccessorsRayonism.getSeed(any(), any(), any())).thenReturn(Bytes32.ZERO);
  }

  @Test
  void test1() {
    when(stateAccessorsRayonism.getActiveValidatorIndices(any(), any()))
        .thenReturn(IntStream.range(0, 160).boxed().collect(Collectors.toList()));
    UInt64 committeeCountPerSlot = committeeUtil.getCommitteeCountPerSlot(state, UInt64.ZERO);

    for (int epoch = 0; epoch < 10; epoch++) {
      for (int shard = 0; shard < 8; shard++) {

        List<Integer> shardCommittee = committeeUtil
            .getShardCommittee(state, UInt64.valueOf(epoch), UInt64.valueOf(shard));
        System.out.println("Epoch: " + epoch + ", Shard: " + shard + ": " + shardCommittee);
      }
    }
    System.out.println(committeeCountPerSlot);
  }

  @Test
  void getStartShard_test() {
    for (int committeeCount = 3; committeeCount < 9; committeeCount++) {
//    int committeeCount = 8;
      int validatorsCount = committeeCount * 32;
      when(stateAccessorsRayonism.getActiveValidatorIndices(any(), any()))
          .thenReturn(IntStream.range(0, validatorsCount).boxed().collect(Collectors.toList()));

      when(state.getCurrent_epoch_start_shard()).thenReturn(UInt64.ZERO);
      when(stateAccessorsRayonism.getCurrentEpoch(any())).thenReturn(UInt64.valueOf(1));
      for (int slot = 0; slot < 64; slot++) {
        UInt64 startShard = committeeUtil.getStartShard(state, UInt64.valueOf(slot));
        System.out.println("CommitteeCount: " + committeeCount + ", slot: " + slot + ", StartShard: " + startShard);

      }
    }
  }
}
