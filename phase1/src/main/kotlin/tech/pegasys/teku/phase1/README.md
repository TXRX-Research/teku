# Phase 1

An implementation of Ethereum 2.0 [Phase 1 spec](https://github.com/ethereum/eth2.0-specs/tree/dev/specs/phase1) written in Kotlin. 

It is changing rapidly and have a WIP status in most of its parts. But there is already something that you could try.

## Eth1 Shard Simulation

[Eth2Client](https://github.com/txrx-research/teku/blob/phase1/phase1/src/main/kotlin/tech/pegasys/teku/phase1/Eth2Client.kt) 
has been implemented as a part of Eth1-Eth2 Merger project on top of 
[Phase1Simulation](https://github.com/txrx-research/teku/blob/phase1/phase1/src/main/kotlin/tech/pegasys/teku/phase1/simulation/Phase1Simulation.kt) 
which has an option for Eth1 Shard.

### Build eth2-client

#### Prerequisites

- Java 11

#### Build and Dist

To create a ready to run distribution:

```shell script
git clone -b phase1 https://github.com/PegaSysEng/teku.git
cd teku && ./gradlew :phase1:distTar :phase1:installDist
```

This produces:
- Fully packaged distribution in `phase1/build/distributions` 
- Expanded distribution, ready to run in `phase1/build/install/eth2-client`

### Run eth2-client
```shell script
cd phase1/build/install/eth2-client/bin
./eth2-client --registry-size=16
```

### Add eth1-engine
Previous command runs eth2-client with stubbed eth1-engine. But for those who want to try, there is [Catalyst](https://github.com/gballet/go-ethereum/tree/eth1-eth2-proto1), 
an eth1-engine built on top of Go-ethereum by [@gballet](https://github.com/gballet/).

#### Build Catalyst

##### Prerequisites
- https://github.com/ethereum/go-ethereum/wiki/Building-Ethereum

To built Catalyst one should first clone the repository:
```shell script
git clone -b eth1-eth2-proto1 git@github.com:gballet/go-ethereum.git
```

And run a built command:
```shell script
cd go-ethereum
make geth
```

#### Run eth2-client with Catalyst
Add custom genesis file:
```shell script
cd build/bin
echo \{ \
  \"config\": \{ \
    \"chainId\": 220720, \
    \"homesteadBlock\": 0, \
    \"eip150Block\": 0, \
    \"eip155Block\": 0, \
    \"eip158Block\": 0, \
    \"byzantiumBlock\": 0, \
    \"constantinopleBlock\": 0, \
    \"petersburgBlock\": 0, \
    \"istanbulBlock\": 0 \
  \}, \
  \"alloc\": \{\}, \
  \"coinbase\": \"0x0000000000000000000000000000000000000000\", \
  \"difficulty\": \"0x20000\", \
  \"extraData\": \"\", \
  \"gasLimit\": \"0x2fefd8\", \
  \"nonce\": \"0x0000000000220720\", \
  \"mixhash\": \"0x0000000000000000000000000000000000000000000000000000000000000000\", \
  \"parentHash\": \"0x0000000000000000000000000000000000000000000000000000000000000000\", \
  \"timestamp\": \"0x00\" \
\} > genesis.json
```

Init Catalyst with the genesis and run it in a custom data directory:
```shell script
./geth --datadir ./chaindata init genesis.json
./geth --rpc --rpcapi eth,eth2 --nodiscover --etherbase 0x1000000000000000000000000000000000000000 --datadir ./chaindata
```

Run eth2-client connected to Catalyst:
```shell script
./eth2-client --registry-size 16 --eth1-engine http://127.0.0.1:8545
```

Use separate Catalyst instances for block producing and processing:

```shell script
./geth --datadir ./producer_chaindata init genesis.json
./geth --rpc --rpcapi eth,eth2 --nodiscover --etherbase 0x1000000000000000000000000000000000000000 --datadir ./producer_chaindata

./geth --datadir ./processor_chaindata init genesis.json
./geth --rpc --rpcapi eth,eth2 --nodiscover --datadir ./processor_chaindata --port 30304 --rpcport 8546

./eth2-client --registry-size 16 --eth1-engine http://127.0.0.1:8545 --processor-eth1-engine http://127.0.0.1:8546
```

### eth2-client CLI options
Use `--help` to print a list of options:
```
./eth2-client --help

eth2-client [OPTIONS]

Description:
Run eth2-client

Options:
      --epochs-to-run=<NUMBER>
                     Number of epochs to run a simulation for.
                       Default: 128
      --registry-size=<NUMBER>
                     Size of validator registry.
                       Default: 2048
      --active-shards=<NUMBER>
                     Number of active shards during simulation.
                       Default: 2
      --eth1-engine=<URL | "stub">
                     Eth1-engine endpoint.
                       Default: stub
      --processor-eth1-engine=<URL>
                     Use this option if you want Eth1 blocks
                     executed by separate eth1-engine instance.
      --eth1-shard=<NUMBER>
                     Identifier of Eth1 Shard,
                     use -1 to disable Eth1 Shard.
                       Default: 0
      --bls=<MODE>   BLS mode: "BLS12381", "Pseudo", "NoOp".
                       Default: BLS12381
  -d, --debug        Debug mode with additional output.
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.

Eth2 Client is licensed under the Apache License 2.0
```

### References
- Phase 1 spec https://github.com/ethereum/eth2.0-specs/tree/dev/specs/phase1
- Eth1+eth2 client relationship https://ethresear.ch/t/eth1-eth2-client-relationship/7248
- The scope of Eth1-Eth2 merger https://ethresear.ch/t/the-scope-of-eth1-eth2-merger/7362
- Architecture of a geth-based eth1 engine https://ethresear.ch/t/architecture-of-a-geth-based-eth1-engine/7574
