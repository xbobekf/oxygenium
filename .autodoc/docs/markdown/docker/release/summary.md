[View code on GitHub](https://github.com/oxygenium/oxygenium/.autodoc/docs/json/docker/release)

The `.autodoc/docs/json/docker/release` folder contains essential files for building and running the Oxygenium node software using Docker. The Dockerfiles provided in this folder create a Docker image that includes the Oxygenium binary, sets up the necessary directories and configuration files, and exposes the necessary ports for the node to communicate with other nodes on the network.

The `Dockerfile.release` and `Dockerfile.release.adoptjdk` files are used to build Docker images for the Oxygenium project. Both files follow similar steps, but they use different base images for the Java runtime environment. The `Dockerfile.release` uses `eclipse-temurin:17-jre`, while `Dockerfile.release.adoptjdk` uses `adoptopenjdk:11-jre`. These Dockerfiles create a container that runs the Oxygenium node software as a non-root user and stores the Oxygenium node data and wallet data in volumes.

The `entrypoint.sh` script is a shell script that starts the Oxygenium project with the desired Java options. This script is used as the entrypoint for the container, allowing users to easily customize the Java environment for the Oxygenium project without having to manually specify the options each time they start the project.

The `user-mainnet-release.conf` file contains configuration settings for the Oxygenium node, specifically setting the network and mining API interfaces to listen on all available network interfaces. This allows for communication between different nodes in the network and enables mining operations.

Here's an example of how to build and run the Oxygenium node using the provided Dockerfiles:

```bash
# Build the Docker image
docker build -t oxygenium-node -f Dockerfile.release .

# Run the Oxygenium node in a Docker container
docker run -d -p 12973:12973 -p 11973:11973 -p 10973:10973 -p 9973:9973 -v /path/to/data:/oxygenium-home/.oxygenium -v /path/to/wallets:/oxygenium-home/.oxygenium-wallets oxygenium-node
```

In summary, the `.autodoc/docs/json/docker/release` folder provides the necessary files for building a Docker image of the Oxygenium node software and running it in a container. The Dockerfiles set up the environment, directories, and configuration files, while the `entrypoint.sh` script allows for easy customization of the Java environment. The `user-mainnet-release.conf` file configures the network and mining API interfaces, enabling communication and mining operations within the Oxygenium network.
