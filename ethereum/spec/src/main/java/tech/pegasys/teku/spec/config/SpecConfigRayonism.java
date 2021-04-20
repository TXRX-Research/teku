package tech.pegasys.teku.spec.config;

import com.google.common.base.Objects;
import java.util.Optional;
import java.util.function.Function;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.type.Bytes4;

public class SpecConfigRayonism extends DelegatingSpecConfig {

  // Fork
  private final Bytes4 mergeForkVersion;
  private final UInt64 mergeForkSlot;

  // Transition
  private final long transitionTotalDifficulty;

  // Sharding
  private final int maxShardProposerSlashings;
  private final int maxShards;
  private final int maxShardHeadersPerShard;

  public SpecConfigRayonism(SpecConfig specConfig,
      Bytes4 mergeForkVersion, UInt64 mergeForkSlot, long transitionTotalDifficulty,
      int maxShardProposerSlashings, int maxShards,
      int maxShardHeadersPerShard) {
    super(specConfig);
    this.mergeForkVersion = mergeForkVersion;
    this.mergeForkSlot = mergeForkSlot;
    this.transitionTotalDifficulty = transitionTotalDifficulty;
    this.maxShardProposerSlashings = maxShardProposerSlashings;
    this.maxShards = maxShards;
    this.maxShardHeadersPerShard = maxShardHeadersPerShard;
  }

  public static SpecConfigRayonism required(final SpecConfig specConfig) {
    return specConfig
        .toVersionRayonism()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Expected merge spec config but got: "
                        + specConfig.getClass().getSimpleName()));
  }

  public static <T> T required(
      final SpecConfig specConfig, final Function<SpecConfigRayonism, T> ctr) {
    return ctr.apply(
        specConfig
            .toVersionRayonism()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Expected merge spec config but got: "
                            + specConfig.getClass().getSimpleName())));
  }

  public Bytes4 getMergeForkVersion() {
    return mergeForkVersion;
  }

  public UInt64 getMergeForkSlot() {
    return mergeForkSlot;
  }

  public long getTransitionTotalDifficulty() {
    return transitionTotalDifficulty;
  }

  public int getMaxShardProposerSlashings() {
    return maxShardProposerSlashings;
  }

  public int getMaxShards() {
    return maxShards;
  }

  public int getMaxShardHeadersPerShard() {
    return maxShardHeadersPerShard;
  }

  @Override
  public Optional<SpecConfigRayonism> toVersionRayonism() {
    return Optional.of(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SpecConfigRayonism)) {
      return false;
    }
    SpecConfigRayonism that = (SpecConfigRayonism) o;
    return getTransitionTotalDifficulty() == that.getTransitionTotalDifficulty() &&
        Objects.equal(getMergeForkVersion(), that.getMergeForkVersion()) &&
        Objects.equal(getMergeForkSlot(), that.getMergeForkSlot()) &&
        Objects
            .equal(getMaxShardProposerSlashings(), that.getMaxShardProposerSlashings()) &&
        Objects.equal(getMaxShards(), that.getMaxShards()) &&
        Objects
            .equal(getMaxShardHeadersPerShard(), that.getMaxShardHeadersPerShard());
  }

  @Override
  public int hashCode() {
    return Objects
        .hashCode(getMergeForkVersion(), getMergeForkSlot(), getTransitionTotalDifficulty(),
            getMaxShardProposerSlashings(), getMaxShards(), getMaxShardHeadersPerShard());
  }
}
