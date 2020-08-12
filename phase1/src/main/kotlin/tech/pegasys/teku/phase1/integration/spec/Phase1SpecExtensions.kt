package tech.pegasys.teku.phase1.integration.spec

import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.ShardBlock
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.phase1.DOMAIN_SHARD_PROPOSAL
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.pylib.pyint

fun Phase1Spec.get_shard_block_signature(
  state: BeaconState,
  block: ShardBlock,
  privkey: pyint
): BLSSignature {
  val domain = get_domain(state,
    DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(block.slot))
  val signing_root = compute_signing_root(block, domain)
  return bls.Sign(privkey, signing_root)
}
