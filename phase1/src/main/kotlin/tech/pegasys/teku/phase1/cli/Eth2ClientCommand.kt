package tech.pegasys.teku.phase1.cli

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import tech.pegasys.teku.phase1.simulation.Phase1Simulation
import tech.pegasys.teku.phase1.util.Color
import tech.pegasys.teku.phase1.util.log
import java.io.PrintWriter
import java.util.concurrent.Callable
import kotlin.text.Charsets.UTF_8

@CommandLine.Command(
    name = "eth2-client",
    showDefaultValues = true,
    abbreviateSynopsis = true,
    description = ["Run eth2-client"],
    mixinStandardHelpOptions = true,
    version = ["0.1.0"],
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    footer = ["Eth2 Client is licensed under the Apache License 2.0"],
    sortOptions = false
)
class Eth2ClientCommand : Callable<Int> {

  @CommandLine.Option(
      names = ["--epochs-to-run"],
      paramLabel = "<NUMBER>",
      description = ["Number of epochs to run a simulation for."]
  )
  private var epochsToRun = 128uL

  @CommandLine.Option(
      names = ["--registry-size"],
      paramLabel = "<NUMBER>",
      description = ["Size of validator registry."]
  )
  private var registrySize = 2048uL

  @CommandLine.Option(
      names = ["--active-shards"],
      paramLabel = "<NUMBER>",
      description = ["Number of active shards during simulation."]
  )
  private var activeShards = 2uL

  @CommandLine.Option(
      names = ["--eth1-engine"],
      paramLabel = "<URL | \"stub\">",
      description = ["Eth1-engine endpoint."]
  )
  private var proposerEth1Engine = "stub"

  @CommandLine.Option(
      names = ["--processor-eth1-engine"],
      paramLabel = "<URL>",
      description = ["Use this option if you want Eth1 blocks",
        "executed by separate eth1-engine instance."]
  )
  private var processorEth1Engine: String? = null

  @CommandLine.Option(
      names = ["--eth1-shard"],
      paramLabel = "<NUMBER>",
      description = ["Identifier of Eth1 Shard,", "use -1 to disable Eth1 Shard."]
  )
  private var eth1ShardNumber = 0uL

  @CommandLine.Option(
      names = ["--bls"],
      paramLabel = "<MODE>",
      description = ["BLS mode: \"BLS12381\", \"NoOp\"."]
  )
  private var bls = Phase1Simulation.BLSConfig.BLS12381

  @CommandLine.Option(
      names = ["--no-cache"],
      description = ["Disable state transition caches."]
  )
  private var noCache = false

  @CommandLine.Option(
      names = ["-d", "--debug"],
      description = ["Debug mode with additional output."]
  )
  private var debug = false

  override fun call() = runBlocking<Int> {
    val scope = CoroutineScope(coroutineContext + Dispatchers.Default)
    checkEth1EngineOptions(proposerEth1Engine, processorEth1Engine)
    val config = Phase1Simulation.Config(
        epochsToRun = epochsToRun,
        registrySize = registrySize,
        activeShards = activeShards,
        eth1ShardNumber = eth1ShardNumber,
        debug = debug,
        bls = bls,
        cache = if (noCache) Phase1Simulation.CacheConfig.NoOp else Phase1Simulation.CacheConfig.LRU,
        proposerEth1Engine = proposerEth1Engine,
        processorEth1Engine =
        if (processorEth1Engine != null) {
          processorEth1Engine!!
        } else {
          proposerEth1Engine
        }
    )
    log("Launching with ${config.toStringPretty()}\n", Color.GREEN)
    Phase1Simulation(scope, config).start()

    return@runBlocking 0
  }

  fun run(args: Array<String>): Int {
    val commandLine = CommandLine(this).setCaseInsensitiveEnumValuesAllowed(true)
    commandLine.out = PrintWriter(System.out, true, UTF_8)
    commandLine.err = PrintWriter(System.err, true, UTF_8)
    return commandLine.execute(*args)
  }
}

private fun checkEth1EngineOptions(proposerOption: String, processorOption: String?) {
  if (proposerOption != "stub") {
    checkURL(proposerOption)
    require(processorOption != "stub") {
      "Can't stub processor eth1-engine while proposers are set to use a real instance"
    }
  }

  if (processorOption != null && processorOption != "stub") {
    checkURL(processorOption)
  }
}
