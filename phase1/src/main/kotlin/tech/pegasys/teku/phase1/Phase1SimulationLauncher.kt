package tech.pegasys.teku.phase1

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tech.pegasys.teku.phase1.eth1client.stub.Eth1EngineClientStub
import tech.pegasys.teku.phase1.simulation.Phase1Simulation
import tech.pegasys.teku.phase1.simulation.util.SimulationRandomness
import java.security.Security

fun main() = runBlocking<Unit> {
  Security.addProvider(BouncyCastleProvider())
  val scope = CoroutineScope(coroutineContext + Dispatchers.Default)
  val simulation = Phase1Simulation(scope) {
    it.epochsToRun = 128uL
    it.validatorRegistrySize = 16
    it.debug = false
    it.bls = Phase1Simulation.BLSConfig.Pseudo
    it.activeShards = 2uL
    it.eth1ShardNumber = 0uL
    it.proposerEth1Engine = Eth1EngineClientStub(SimulationRandomness)
    it.processorEth1Engine = Eth1EngineClientStub(SimulationRandomness)
  }
  simulation.start()
}
