package tech.pegasys.teku.phase1.cli

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import tech.pegasys.teku.phase1.simulation.Phase1Simulation
import java.io.PrintWriter
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Callable
import kotlin.text.Charsets.UTF_8

@CommandLine.Command(
  name = "simulator",
  showDefaultValues = true,
  abbreviateSynopsis = true,
  description = ["Run Eth1 Shard Simulator"],
  mixinStandardHelpOptions = true,
  version = ["0.1.0"],
  synopsisHeading = "%n",
  descriptionHeading = "%nDescription:%n%n",
  optionListHeading = "%nOptions:%n",
  footerHeading = "%n",
  footer = ["Eth1 Shard Simulator is licensed under the Apache License 2.0"],
  sortOptions = false
)
class Phase1SimulationCommand : Callable<Int> {

  @CommandLine.Option(
    names = ["-e", "--epochs-to-run"],
    paramLabel = "<NUMBER>",
    description = ["Number of epochs to run a simulation for."]
  )
  private var epochsToRun = 128uL

  @CommandLine.Option(
    names = ["-r", "--registry-size"],
    paramLabel = "<NUMBER>",
    description = ["Size of validator registry."]
  )
  private var registrySize = 2048uL

  @CommandLine.Option(
    names = ["-s", "--active-shards"],
    paramLabel = "<NUMBER>",
    description = ["Number of active shards during simulation."]
  )
  private var activeShards = 2uL

  @CommandLine.Option(
    names = ["--proposer-eth1-engine"],
    paramLabel = "<URL>",
    description = ["Eth1-engine endpoint used by proposers."]
  )
  private var proposerEth1Engine = "stub"

  @CommandLine.Option(
    names = ["--processor-eth1-engine"],
    paramLabel = "<URL>",
    description = ["Eth1-engine endpoint used by processors. ",
      "Do not set to re-use proposer's engine."]
  )
  private var processorEth1Engine = "stub"

  @CommandLine.Option(
    names = ["--eth1-shard"],
    paramLabel = "<NUMBER>",
    description = ["Identifier of Eth1 Shard."]
  )
  private var eth1ShardNumber = 0uL

  @CommandLine.Option(
    names = ["-d", "--debug"],
    description = ["Debug mode with additional output."]
  )
  private var debug = false

  @CommandLine.Option(
    names = ["--bls"],
    paramLabel = "<MODE>",
    description = ["BLS mode: \"BLS12381\", \"Pseudo\", \"NoOp\"."]
  )
  private var bls = Phase1Simulation.BLSConfig.BLS12381

  override fun call() = runBlocking<Int> {
    val scope = CoroutineScope(coroutineContext + Dispatchers.Default)
    val simulation = Phase1Simulation(scope) {
      it.epochsToRun = epochsToRun
      it.registrySize = registrySize
      it.activeShards = activeShards
      it.eth1ShardNumber = eth1ShardNumber
      it.debug = debug
      it.bls = bls
      val proposerEngineOption = checkEth1EngineOption(proposerEth1Engine)
      val processorEngineOption = checkEth1EngineOption(processorEth1Engine)

      it.proposerEth1Engine = proposerEngineOption
      it.processorEth1Engine =
        if (proposerEngineOption != "stub" && processorEngineOption == "stub") {
          proposerEth1Engine
        } else {
          processorEth1Engine
        }
    }
    simulation.start()

    return@runBlocking 0
  }

  fun run(args: Array<String>): Int {
    val commandLine = CommandLine(this).setCaseInsensitiveEnumValuesAllowed(true)
    commandLine.out = PrintWriter(System.out, true, UTF_8)
    commandLine.err = PrintWriter(System.err, true, UTF_8)
    return commandLine.execute(*args)
  }
}

private fun checkEth1EngineOption(option: String): String {
  if (option != "stub") {
    try {
      URL(option)
    } catch (e: MalformedURLException) {
      throw IllegalArgumentException("Invalid URL '$option': ${e.message}")
    }
  }
  return option
}
