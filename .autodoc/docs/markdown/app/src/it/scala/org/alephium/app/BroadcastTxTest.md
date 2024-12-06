[View code on GitHub](https://github.com/oxygenium/oxygenium/app/src/it/scala/org/oxygenium/app/BroadcastTxTest.scala)

The `BroadcastTxTest` class is a test suite for testing the broadcasting of transactions in the Oxygenium network. The class contains two test cases that test the broadcasting of transactions within a clique and between inter-clique nodes.

The first test case tests the broadcasting of transactions within a clique. It creates a clique with two nodes and starts mining. It then creates an intra-group transaction and verifies that the transaction is added to the mempool of the node that received the transaction. It then confirms the transaction by starting mining and checking that the transaction is included in the block. It then creates a cross-group transaction and verifies that the transaction is added to the mempool of both nodes. It then confirms the transaction by starting mining and checking that the transaction is included in the block.

The second test case tests the broadcasting of transactions between inter-clique nodes. It creates multiple cliques and connects them together. It then creates multiple transactions and verifies that the transactions are added to the mempool of all nodes. It then confirms the transactions by starting mining and checking that the transactions are included in the block.

The `BroadcastTxTest` class uses the `OxygeniumActorSpec` class, which is a base class for testing actors in the Oxygenium network. It also uses several utility classes and methods from the `org.oxygenium` package, such as `Address`, `BrokerInfo`, `GroupIndex`, and `transfer`.

Overall, the `BroadcastTxTest` class is an important part of the Oxygenium project as it ensures that transactions are broadcasted correctly within and between cliques, which is essential for the proper functioning of the network.
## Questions: 
 1. What is the purpose of the `BroadcastTxTest` class?
- The `BroadcastTxTest` class is a test suite for broadcasting transactions between nodes in a clique network.

2. What is the significance of the `cross-group transaction` test case?
- The `cross-group transaction` test case tests the ability of the network to broadcast transactions between different groups within the clique network.

3. What is the purpose of the `numCliques` and `numTxs` variables in the `broadcast sequential txs between inter clique node` test case?
- The `numCliques` and `numTxs` variables are used to control the number of cliques and transactions used in stress testing the network's ability to broadcast transactions between inter-clique nodes.