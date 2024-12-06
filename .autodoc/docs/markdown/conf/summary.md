[View code on GitHub](https://github.com/oxygenium/oxygenium/.autodoc/docs/json/conf)

The code in the `.autodoc/docs/json/conf/src` folder plays a crucial role in managing various configurations within the Oxygenium project. These configurations ensure the proper functioning of the blockchain, consensus algorithm, mining process, and network communication.

For instance, the `OxygeniumConfig.scala` file contains the main configuration class `OxygeniumConfig`, which loads and manages all configurations for the project. To access network settings, you can use `OxygeniumConfig.network`:

```scala
val config = OxygeniumConfig.load()
val networkSettings = config.network
```

The `ConsensusConfig.scala` file contains the `ConsensusConfig` class, responsible for managing consensus-related configurations like block time and difficulty adjustment. These configurations are used in the consensus algorithm to ensure the proper functioning of the blockchain:

```scala
val config = OxygeniumConfig.load()
val consensusSettings = config.consensus
val blockTime = consensusSettings.blockTime
```

The `DiscoveryConfig.scala` file contains the `DiscoveryConfig` class, managing configurations related to the peer discovery process. It includes settings for the discovery interval, maximum number of peers, and peer discovery timeout. These configurations are used in the peer discovery process to maintain a healthy network of nodes:

```scala
val config = OxygeniumConfig.load()
val discoverySettings = config.discovery
val discoveryInterval = discoverySettings.interval
```

The `MiningConfig.scala` file contains the `MiningConfig` class, managing mining-related configurations like the mining algorithm, reward, and difficulty. These configurations are used in the mining process to ensure the proper functioning of the blockchain:

```scala
val config = OxygeniumConfig.load()
val miningSettings = config.mining
val miningReward = miningSettings.reward
```

The `NetworkConfig.scala` file contains the `NetworkConfig` class, managing network-related configurations like the network type, port, and address. These configurations are used in the network layer to ensure proper communication between nodes:

```scala
val config = OxygeniumConfig.load()
val networkSettings = config.network
val networkPort = networkSettings.port
```

In summary, the code in this folder is responsible for managing various configurations used throughout the Oxygenium project. These configurations are essential for the proper functioning of the blockchain, consensus algorithm, mining process, and network communication.
