package tech.pegasys.teku.phase1

import org.bouncycastle.jce.provider.BouncyCastleProvider
import tech.pegasys.teku.phase1.cli.Phase1SimulationCommand
import java.security.Security
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  Security.addProvider(BouncyCastleProvider())
  val result = Phase1SimulationCommand().run(args)
  if (result != 0) {
    exitProcess(result)
  }
}
