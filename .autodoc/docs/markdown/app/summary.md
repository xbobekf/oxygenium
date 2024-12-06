[View code on GitHub](https://github.com/oxygenium/oxygenium/.autodoc/docs/json/app)

The `.autodoc/docs/json/app` folder contains essential Scala files and integration tests for the Oxygenium project, a blockchain platform. These files handle various aspects of the project, such as API configurations, block exporting and importing, application booting, CPU solo mining, API documentation generation, and REST and WebSocket server management.

For example, `ApiConfig.scala` in the `main` folder defines the `ApiConfig` class and its companion object, responsible for loading and validating configuration parameters for the Oxygenium API. This makes it easy to pass around and use these parameters in other parts of the codebase:

```scala
val apiConfig: ApiConfig = ...
val apiPort: Int = apiConfig.port
```

`BlocksExporter.scala` and `BlocksImporter.scala` provide functionality for exporting and importing blocks from the Oxygenium blockchain to a file, useful for analysis, backup, or migration purposes:

```scala
val blockFlow: BlockFlow = ...
val outputPath: Path = ...
val blocksExporter = new BlocksExporter(blockFlow, outputPath)
val filename = "exported_blocks.txt"
val exportResult = blocksExporter.export(filename)
```

The `it` folder contains integration tests for the Oxygenium project, which are essential for ensuring the correct functioning of the system. For instance, the **OxygeniumFlowSpec.scala** file contains the OxygeniumFlowSpec class, which tests the flow of data and transactions within the Oxygenium network:

```scala
val flow = new OxygeniumFlowSpec
flow.test("propagate valid transactions") { ... }
flow.test("reject invalid transactions") { ... }
```

The subfolders in the `it` folder contain more specific integration tests for different aspects of the Oxygenium project. For example, the **api** subfolder contains tests for the Oxygenium API, which is used by clients to interact with the Oxygenium network:

- **WalletApiSpec.scala**: Tests the wallet-related API endpoints, such as creating and managing wallets, and sending transactions.
- **NodeApiSpec.scala**: Tests the node-related API endpoints, such as querying the blockchain and managing the node's configuration.

In summary, developers working on the Oxygenium project should be familiar with these files and tests, using them to validate their changes and ensure that they do not introduce any regressions or unexpected behavior.
