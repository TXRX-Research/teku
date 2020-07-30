package tech.pegasys.teku.phase1

import org.bouncycastle.jce.provider.BouncyCastleProvider
import tech.pegasys.teku.phase1.cli.Eth2ClientCommand
import java.security.Security
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  Security.addProvider(BouncyCastleProvider())
  val result = Eth2ClientCommand().run(args)
  if (result != 0) {
    exitProcess(result)
  }
}
