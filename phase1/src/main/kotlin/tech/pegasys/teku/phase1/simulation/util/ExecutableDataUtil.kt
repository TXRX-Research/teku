package tech.pegasys.teku.phase1.simulation.util

import tech.pegasys.teku.phase1.eth1engine.Eth1Block
import tech.pegasys.teku.phase1.eth1engine.Eth1BlockHeader
import tech.pegasys.teku.phase1.integration.datastructures.ExecutableData

fun eth1BlockToExecutableData(block: Eth1Block): ExecutableData {
  return ExecutableData(
    block.header.coinbase,
    block.header.stateRoot,
    block.header.gasLimit,
    block.header.gasUsed,
    block.transactions,
    block.header.receiptsRoot,
    block.header.logsBloom,
    block.header.txRoot,
    block.header.parentHash,
    block.header.timestamp,
    block.header.number,
    block.header.difficulty
  )
}

fun executableDataToEth1Block(data: ExecutableData): Eth1Block {
  return Eth1Block(
    header = Eth1BlockHeader(
      parentHash = data.parent_hash,
      coinbase = data.coinbase,
      stateRoot = data.state_root,
      logsBloom = data.logs_bloom.toBytes(),
      number = data.number,
      timestamp = data.timestamp,
      gasLimit = data.gas_limit,
      gasUsed = data.gas_used,
      receiptsRoot = data.receipts_root,
      txRoot = data.tx_root,
      difficulty = data.difficulty
    ),
    transactions = data.transactions.map { it.toBytes() }
  )
}