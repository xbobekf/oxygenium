[View code on GitHub](https://github.com/oxygenium/oxygenium/.autodoc/docs/json/conf/src/main/scala/org/oxygenium/conf)

The folder `.autodoc/docs/json/conf/src/main/scala/org/oxygenium/conf` contains the configuration files for the Oxygenium project. These files are responsible for setting up various parameters and configurations that are used throughout the project. The following is a summary of the files in this folder:

1. `OxygeniumConfig.scala`: This file contains the main configuration class `OxygeniumConfig`, which is responsible for loading and managing all the configurations for the Oxygenium project. It includes configurations for network settings, consensus settings, mining settings, and more. The `OxygeniumConfig` class is used throughout the project to access these configurations. For example, to get the network settings, you can use `OxygeniumConfig.network`.

   ```scala
   val config = OxygeniumConfig.load()
   val networkSettings = config.network
   ```

2. `ConsensusConfig.scala`: This file contains the `ConsensusConfig` class, which is responsible for managing the consensus-related configurations, such as block time, block target, and difficulty adjustment. These configurations are used in the consensus algorithm to ensure the proper functioning of the blockchain.

   ```scala
   val config = OxygeniumConfig.load()
   val consensusSettings = config.consensus
   val blockTime = consensusSettings.blockTime
   ```

3. `DiscoveryConfig.scala`: This file contains the `DiscoveryConfig` class, which is responsible for managing the configurations related to the peer discovery process. It includes settings for the discovery interval, the maximum number of peers, and the timeout for peer discovery. These configurations are used in the peer discovery process to maintain a healthy network of nodes.

   ```scala
   val config = OxygeniumConfig.load()
   val discoverySettings = config.discovery
   val discoveryInterval = discoverySettings.interval
   ```

4. `MiningConfig.scala`: This file contains the `MiningConfig` class, which is responsible for managing the mining-related configurations, such as the mining algorithm, the mining reward, and the mining difficulty. These configurations are used in the mining process to ensure the proper functioning of the blockchain.

   ```scala
   val config = OxygeniumConfig.load()
   val miningSettings = config.mining
   val miningReward = miningSettings.reward
   ```

5. `NetworkConfig.scala`: This file contains the `NetworkConfig` class, which is responsible for managing the network-related configurations, such as the network type, the network port, and the network address. These configurations are used in the network layer to ensure proper communication between nodes.

   ```scala
   val config = OxygeniumConfig.load()
   val networkSettings = config.network
   val networkPort = networkSettings.port
   ```

In summary, the code in this folder is responsible for managing the various configurations used throughout the Oxygenium project. These configurations are essential for the proper functioning of the blockchain, consensus algorithm, mining process, and network communication.
