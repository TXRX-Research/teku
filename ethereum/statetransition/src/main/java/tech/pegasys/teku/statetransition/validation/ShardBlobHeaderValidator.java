package tech.pegasys.teku.statetransition.validation;

import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;

public class ShardBlobHeaderValidator implements OperationValidator<SignedShardBlobHeader> {

  @Override
  public InternalValidationResult validateFully(SignedShardBlobHeader operation) {
    return InternalValidationResult.ACCEPT;
  }

  @Override
  public boolean validateForStateTransition(BeaconState state, SignedShardBlobHeader operation) {
    return true;
  }
}
