package tech.pegasys.teku.phase1.util

import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.onotole.phase1.CommitteeIndex
import tech.pegasys.teku.phase1.onotole.phase1.Epoch
import tech.pegasys.teku.phase1.onotole.phase1.Gwei
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.ValidatorIndex
import tech.pegasys.teku.phase1.onotole.ssz.Sequence
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.util.cache.Cache
import tech.pegasys.teku.util.cache.LRUCache
import tech.pegasys.teku.util.cache.NoOpCache

class Caches internal constructor(
    val activeValidators: Cache<Epoch, Sequence<ValidatorIndex>>,
    val beaconProposerIndex: Cache<Slot, ValidatorIndex>,
    val beaconCommittee: Cache<Pair<Slot, CommitteeIndex>, Sequence<ValidatorIndex>>,
    val totalActiveBalance: Cache<Epoch, Pair<Gwei, uint64>>,
    val committeeShuffle: Cache<Bytes32, List<ValidatorIndex>>
)

@Suppress("FunctionName")
fun LRUCaches(): Caches = Caches(
    LRUCache(MAX_ACTIVE_VALIDATORS_CACHE),
    LRUCache(MAX_BEACON_PROPOSER_INDEX_CACHE),
    LRUCache(MAX_BEACON_COMMITTEE_CACHE),
    LRUCache(MAX_TOTAL_ACTIVE_BALANCE_CACHE),
    LRUCache(MAX_COMMITTEE_SHUFFLE_CACHE)
)

@Suppress("FunctionName")
fun NoOpCaches(): Caches = Caches(
    NoOpCache.getNoOpCache(),
    NoOpCache.getNoOpCache(),
    NoOpCache.getNoOpCache(),
    NoOpCache.getNoOpCache(),
    NoOpCache.getNoOpCache()
)

private const val MAX_ACTIVE_VALIDATORS_CACHE = 8
private const val MAX_BEACON_PROPOSER_INDEX_CACHE = 1
private const val MAX_BEACON_COMMITTEE_CACHE = 64 * 64
private const val MAX_TOTAL_ACTIVE_BALANCE_CACHE = 1
private const val MAX_COMMITTEE_SHUFFLE_CACHE = 2
