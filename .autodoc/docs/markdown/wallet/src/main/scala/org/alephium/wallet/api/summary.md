[View code on GitHub](https://github.com/oxygenium/oxygenium/.autodoc/docs/json/wallet/src/main/scala/org/oxygenium/wallet/api)

The code in the `org.oxygenium.wallet.api` folder is responsible for defining the wallet API endpoints and providing examples for various wallet-related operations in the Oxygenium project. The wallet API allows users to interact with their wallets, manage addresses, and perform transactions.

`WalletEndpoints.scala` defines the wallet API endpoints using the Tapir library, which provides a type-safe and composable way to define HTTP endpoints in Scala. The `WalletEndpoints` trait includes endpoints for creating, restoring, listing, and managing wallets, as well as transferring ALPH tokens, signing data, and managing miner addresses. Each endpoint is defined as a `BaseEndpoint` object, specifying the input and output types using case classes that represent the JSON objects sent and received by the endpoint. The endpoints are organized into two main groups: `wallets` and `minerWallet`, with the former containing endpoints for all wallets and the latter for wallets created with the `isMiner = true` flag.

`WalletExamples.scala` provides examples of various API requests and responses for the Oxygenium wallet. The `WalletExamples` trait defines several implicit `Example` objects, which generate example requests and responses for the wallet API. These examples include creating, restoring, unlocking, and deleting wallets, transferring funds, revealing mnemonics, and changing active addresses. Developers can use these examples to generate sample code and test their integrations with the Oxygenium wallet.

The `model` subfolder contains case classes and objects representing different wallet-related operations and data structures, such as managing wallets, addresses, balances, and transactions. For example, the `Transfer` model represents a transfer of funds between addresses, while the `WalletCreation` and `WalletCreationResult` case classes are used to create new wallets and return the resulting wallet information.

Here's an example of how the `Transfer` class might be used:

```scala
import org.oxygenium.wallet.api.model.Transfer
import org.oxygenium.api.model.Destination
import org.oxygenium.protocol.vm.GasBox
import org.oxygenium.protocol.vm.GasPrice
import org.oxygenium.util.AVector

// Create a transfer request with two destinations and gas and gas price specified
val destinations = AVector(Destination("address1", 100), Destination("address2", 200))
val gasBox = GasBox(1000, 10000)
val gasPrice = GasPrice(100)
val transfer = Transfer(destinations, Some(gasBox), Some(gasPrice))

// Send the transfer request to the Oxygenium network for processing
val transferResult = oxygeniumApi.sendTransfer(transfer)

// Retrieve the transaction ID and group indices from the transfer result
val txId = transferResult.txId
val fromGroup = transferResult.fromGroup
val toGroup = transferResult.toGroup
```

In this example, a new `Transfer` object is created with two destination addresses, a gas amount, and a gas price. The object is then sent to the Oxygenium network for processing using the `sendTransfer` method of the `oxygeniumApi` object. The resulting `transferResult` object contains the transaction ID and group indices for the transfer.

Overall, the code in this folder plays a crucial role in managing wallets, addresses, balances, and transactions in the Oxygenium project, providing a convenient and type-safe way for users to interact with their wallets.
