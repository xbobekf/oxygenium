[View code on GitHub](https://github.com/oxygenium/oxygenium/app/src/it/scala/org/oxygenium/app/ShutdownTest.scala)

The `ShutdownTest` code is a test suite for the Oxygenium project that verifies the proper functioning of the shutdown mechanism in two different scenarios. The first scenario tests whether the node shuts down correctly when the TCP port is in use. The second scenario tests whether the clique shuts down correctly when one of the nodes in the clique is down.

The `ShutdownTest` class extends the `OxygeniumActorSpec` class, which is a test kit for testing actors in the Oxygenium project. The `ShutdownTest` class contains two test cases that verify the shutdown mechanism in different scenarios.

The first test case verifies whether the node shuts down correctly when the TCP port is in use. The test case creates a new `TestProbe` object and binds it to the TCP port. Then, it boots a new node with the default master port and broker ID 0. Finally, it verifies that the `flowSystem` of the server is terminated.

The second test case verifies whether the clique shuts down correctly when one of the nodes in the clique is down. The test case boots a new clique with two nodes and starts it. Then, it stops the first node in the clique and verifies that the `flowSystem` of the second node is terminated.

Overall, the `ShutdownTest` code is a test suite that verifies the proper functioning of the shutdown mechanism in two different scenarios. It is an essential part of the Oxygenium project as it ensures that the project's shutdown mechanism works correctly and prevents any data loss or corruption.
## Questions: 
 1. What is the purpose of the `ShutdownTest` class?
- The `ShutdownTest` class is a test suite for testing the shutdown functionality of the Oxygenium node in different scenarios.

2. What is the `CliqueFixture` class used for?
- The `CliqueFixture` class is used to set up a test environment for testing the Oxygenium node in a clique network.

3. What is the purpose of the two test cases in the `ShutdownTest` class?
- The first test case tests the shutdown functionality of the Oxygenium node when the TCP port is used, while the second test case tests the shutdown functionality of the Oxygenium node when one node of the clique network is down.