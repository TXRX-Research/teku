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
  private final UInt64 maxSamplesPerBlock;
  private final UInt64 targetSamplesPerBlock;
  private final int initialActiveShards;
  private final int gaspriceAdjustmentCoefficient;
  private final UInt64 maxGasprice;
  private final UInt64 minGasprice;

  // Signature domains
  private final Bytes4 domainShardProposer;
  private final Bytes4 domainShardCommittee;

  public SpecConfigRayonism(SpecConfig specConfig,
      Bytes4 mergeForkVersion, UInt64 mergeForkSlot, long transitionTotalDifficulty,
      int maxShardProposerSlashings, int maxShards, int maxShardHeadersPerShard,
      UInt64 maxSamplesPerBlock,
      UInt64 targetSamplesPerBlock,
      int initialActiveShards,
      int gaspriceAdjustmentCoefficient,
      UInt64 maxGasprice, UInt64 minGasprice,
      Bytes4 domainShardProposer, Bytes4 domainShardCommittee) {
    super(specConfig);
    this.mergeForkVersion = mergeForkVersion;
    this.mergeForkSlot = mergeForkSlot;
    this.transitionTotalDifficulty = transitionTotalDifficulty;
    this.maxShardProposerSlashings = maxShardProposerSlashings;
    this.maxShards = maxShards;
    this.maxShardHeadersPerShard = maxShardHeadersPerShard;
    this.maxSamplesPerBlock = maxSamplesPerBlock;
    this.targetSamplesPerBlock = targetSamplesPerBlock;
    this.initialActiveShards = initialActiveShards;
    this.gaspriceAdjustmentCoefficient = gaspriceAdjustmentCoefficient;
    this.maxGasprice = maxGasprice;
    this.minGasprice = minGasprice;
    this.domainShardProposer = domainShardProposer;
    this.domainShardCommittee = domainShardCommittee;
  }

  public static SpecConfigRayonism required(final SpecConfig specConfig) {
    return specConfig
        .toVersionRayonism()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Expected Rayonism spec config but got: "
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
                        "Expected Rayonism spec config but got: "
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

  public UInt64 getMaxSamplesPerBlock() {
    return maxSamplesPerBlock;
  }

  public UInt64 getTargetSamplesPerBlock() {
    return targetSamplesPerBlock;
  }

  public int getInitialActiveShards() {
    return initialActiveShards;
  }

  public int getGaspriceAdjustmentCoefficient() {
    return gaspriceAdjustmentCoefficient;
  }

  public UInt64 getMaxGasprice() {
    return maxGasprice;
  }

  public UInt64 getMinGasprice() {
    return minGasprice;
  }

  public Bytes4 getDomainShardProposer() {
    return domainShardProposer;
  }

  public Bytes4 getDomainShardCommittee() {
    return domainShardCommittee;
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
        getMaxShardProposerSlashings() == that.getMaxShardProposerSlashings() &&
        getMaxShards() == that.getMaxShards() &&
        getMaxShardHeadersPerShard() == that.getMaxShardHeadersPerShard() &&
        initialActiveShards == that.initialActiveShards &&
        gaspriceAdjustmentCoefficient == that.gaspriceAdjustmentCoefficient &&
        getShardCommitteePeriod() == that.getShardCommitteePeriod() &&
        Objects.equal(getMergeForkVersion(), that.getMergeForkVersion()) &&
        Objects.equal(getMergeForkSlot(), that.getMergeForkSlot()) &&
        Objects.equal(maxGasprice, that.maxGasprice) &&
        Objects.equal(minGasprice, that.minGasprice) &&
        Objects.equal(domainShardProposer, that.domainShardProposer) &&
        Objects.equal(domainShardCommittee, that.domainShardCommittee);
  }

  @Override
  public int hashCode() {
    return Objects
        .hashCode(getMergeForkVersion(), getMergeForkSlot(), getTransitionTotalDifficulty(),
            getMaxShardProposerSlashings(), getMaxShards(), getMaxShardHeadersPerShard(),
            initialActiveShards, gaspriceAdjustmentCoefficient, maxGasprice, minGasprice,
            getShardCommitteePeriod(), domainShardProposer, domainShardCommittee);
  }
}
