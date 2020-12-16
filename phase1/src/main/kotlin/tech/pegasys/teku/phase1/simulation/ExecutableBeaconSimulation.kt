package tech.pegasys.teku.phase1.simulation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tech.pegasys.teku.datastructures.util.MockStartValidatorKeyPairFactory
import tech.pegasys.teku.phase1.eth1engine.Eth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.Web3jEth1EngineClient
import tech.pegasys.teku.phase1.eth1engine.stub.Eth1EngineClientStub
import tech.pegasys.teku.phase1.eth1engine.withLogger
import tech.pegasys.teku.phase1.integration.spec.ExecutableBeaconSpec
import tech.pegasys.teku.phase1.integration.spec.OptimizedPhase1Spec
import tech.pegasys.teku.phase1.onotole.deps.BLS12381
import tech.pegasys.teku.phase1.onotole.deps.NoOpBLS
import tech.pegasys.teku.phase1.onotole.phase1.SLOTS_PER_EPOCH
import tech.pegasys.teku.phase1.simulation.actors.BeaconAttester
import tech.pegasys.teku.phase1.simulation.actors.BeaconProposer
import tech.pegasys.teku.phase1.simulation.actors.DelayedAttestationsPark
import tech.pegasys.teku.phase1.simulation.actors.Eth2ChainProcessor
import tech.pegasys.teku.phase1.simulation.actors.ShardProposer
import tech.pegasys.teku.phase1.simulation.actors.SlotTicker
import tech.pegasys.teku.phase1.simulation.util.ConstantsConfig
import tech.pegasys.teku.phase1.simulation.util.SecretKeyRegistry
import tech.pegasys.teku.phase1.simulation.util.SimulationRandomness
import tech.pegasys.teku.phase1.simulation.util.getGenesisState
import tech.pegasys.teku.phase1.simulation.util.getGenesisStore
import tech.pegasys.teku.phase1.simulation.util.runsOutOfSlots
import tech.pegasys.teku.phase1.simulation.util.setConstants
import tech.pegasys.teku.phase1.util.LRUCaches
import tech.pegasys.teku.phase1.util.NoOpCaches
import tech.pegasys.teku.phase1.util.log
import tech.pegasys.teku.phase1.util.logDebug
import tech.pegasys.teku.phase1.util.logSetDebugMode
import kotlin.coroutines.CoroutineContext

class ExecutableBeaconSimulation(
  private val scope: CoroutineScope,
  private val config: Config
) {

  private val eventBus: Channel<Eth2Event> = Channel(Channel.UNLIMITED)
  private val terminator = object : Eth2Actor(eventBus) {
    override suspend fun dispatchImpl(event: Eth2Event, scope: CoroutineScope) {
      // stop simulation when the last slot has been processed
      if (event is SlotTerminal && runsOutOfSlots(event.slot, config.slotsToRun)) {
        stop()
      }
    }
  }

  private val actors: List<Eth2Actor>

  init {
    setConstants(
      "minimal",
      ConstantsConfig(config.activeShards, (-1).toULong(), config.registrySize)
    )
    logSetDebugMode(config.debug)

    val bls = when (config.bls) {
      BLSConfig.BLS12381 -> BLS12381
      BLSConfig.NoOp -> NoOpBLS
    }

    val cache = when (config.cache) {
      CacheConfig.LRU -> LRUCaches()
      CacheConfig.NoOp -> NoOpCaches()
    }

    val spec =
      OptimizedPhase1Spec(cache, bls)

    log("Initializing ${config.registrySize} BLS Key Pairs...")
    val blsKeyPairs =
      MockStartValidatorKeyPairFactory().generateKeyPairs(0, config.registrySize.toInt())

    val proposerEth1Engine = instantiateEth1Engine(
      config.proposerEth1Engine,
      scope.coroutineContext
    ).withLogger("ProposerEth1Engine")

    val processorEth1Engine = instantiateEth1Engine(
      config.processorEth1Engine,
      scope.coroutineContext
    ).withLogger("ProcessorEth1Engine")

    val eth1GenesisBlockHash = proposerEth1Engine.eth_getHeadBlockHash().result!!

    log("Initializing genesis state and store...")
    val preGenesisState = getGenesisState(blsKeyPairs, spec)
    preGenesisState.genesis_time = (System.currentTimeMillis() / 1000).toULong()
    val genesisState = preGenesisState.applyChanges()
    val store = getGenesisStore(genesisState, spec, eth1GenesisBlockHash)
    val secretKeys = SecretKeyRegistry(blsKeyPairs)

    val proposerSpec = ExecutableBeaconSpec(proposerEth1Engine, bls, cache)
    val processorSpec = ExecutableBeaconSpec(processorEth1Engine, bls, cache)
    val executableDataProducer = ExecutableDataProducer(proposerEth1Engine)

    actors = listOf(
      SlotTicker(eventBus, config.slotsToRun),
      Eth2ChainProcessor(eventBus, store, processorEth1Engine, processorSpec),
      BeaconProposer(eventBus, secretKeys, proposerSpec, executableDataProducer),
      BeaconAttester(eventBus, secretKeys, spec),
      ShardProposer(eventBus, secretKeys, Eth1EngineClientStub(SimulationRandomness), spec),
      DelayedAttestationsPark(eventBus),
      terminator
    )
  }

  suspend fun start() {
    log("Starting simulation...")
    eventLoop(actors, scope)
    eventBus.send(GenesisSlotEvent)
  }

  fun stop() {
    eventBus.close()
    log("Simulation stopped")
  }

  /**
   * A concurrent event loop
   */
  private fun eventLoop(actors: List<Eth2Actor>, scope: CoroutineScope) = scope.launch {
    for (event in eventBus) {
      logDebug { "Dispatch $event" }
      coroutineScope {
        actors.forEach {
          launch { it.dispatchImpl(event, scope) }
        }
      }
    }
  }

  data class Config(
    var epochsToRun: ULong = 2uL,
    var registrySize: ULong = 16uL,
    var proposerEth1Engine: String = "stub",
    var processorEth1Engine: String = "stub",
    var debug: Boolean = false,
    var bls: BLSConfig = BLSConfig.BLS12381,
    var cache: CacheConfig = CacheConfig.NoOp,
    var activeShards: ULong = 2uL
  ) {

    val slotsToRun: ULong
      get() = epochsToRun * SLOTS_PER_EPOCH

    fun toStringPretty(): String {
      return "Config(\n" +
          "    epochsToRun=$epochsToRun\n" +
          "    registrySize=$registrySize\n" +
          "    proposerEth1Engine=$proposerEth1Engine\n" +
          "    processorEth1Engine=$processorEth1Engine\n" +
          "    debug=$debug\n" +
          "    bls=$bls\n" +
          "    activeShards=$activeShards\n" +
          ")"
    }
  }

  enum class BLSConfig {
    BLS12381,
    NoOp
  }

  enum class CacheConfig {
    LRU,
    NoOp
  }
}

private fun instantiateEth1Engine(engine: String, ctx: CoroutineContext): Eth1EngineClient {
  return if (engine == "stub") {
    Eth1EngineClientStub(SimulationRandomness)
  } else {
    Web3jEth1EngineClient(engine, ctx)
  }
}

@Suppress("FunctionName")
fun ExecutableBeaconSimulator(
  scope: CoroutineScope,
  userConfig: (ExecutableBeaconSimulation.Config) -> Unit
): ExecutableBeaconSimulation {
  val config = ExecutableBeaconSimulation.Config()
  userConfig(config)
  return ExecutableBeaconSimulation(scope, config)
}
