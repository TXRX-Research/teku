package tech.pegasys.teku.phase1

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tech.pegasys.teku.phase1.simulation.ExecutableBeaconSimulation
import tech.pegasys.teku.phase1.util.Color
import tech.pegasys.teku.phase1.util.log
import java.security.Security

fun main(args: Array<String>) = runBlocking<Unit> {
  Security.addProvider(BouncyCastleProvider())

  val scope = CoroutineScope(coroutineContext + Dispatchers.Default)
  val proposerEth1Engine = "http://127.0.0.1:8545"
  val processorEth1Engine = "http://127.0.0.1:8545"
  val debug = true
  val bls = ExecutableBeaconSimulation.BLSConfig.BLS12381
  val noCache = false

  val config = ExecutableBeaconSimulation.Config(
    epochsToRun = 128uL,
    registrySize = 2048uL,
    activeShards = 2uL,
    debug = debug,
    bls = bls,
    cache = if (noCache) ExecutableBeaconSimulation.CacheConfig.NoOp else ExecutableBeaconSimulation.CacheConfig.LRU,
    proposerEth1Engine = proposerEth1Engine,
    processorEth1Engine = processorEth1Engine
  )
  log("Launching with ${config.toStringPretty()}\n", Color.GREEN)
  ExecutableBeaconSimulation(scope, config).start()
}
