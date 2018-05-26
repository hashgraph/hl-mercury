## Gradle swirlds

Stripped down version of the Swirlds SDK. Featuring:

- Gradle build (instead of Maven)
- simplified project structure - focus on state and application
- copy and paste generated .jar into official Swirls SDK
- first stab at processing contracts (using Docker for now)
- first protocol (dumb) for processing smart contracts
- experiments with Google protobuf for state storage


### Prerequisites

- Java 8 (Oracle)
- Gradle
- *Optional: IntelliJ*

Java 8 (Oracle) and Gradle can be conveniently installed using [sdkman](http://sdkman.io/).

`sdk list java`

`sdk install java 8.0.171-oracle`

`sdk list gradle`

`sdk install gradle 4.7`

### Build instructions

A stripped down gradle version of official Swirlds SDK is in "sdk.gradle"

There you can focus on designing your Main class and State class. Built with Gradle/IntelliJ IDE in mind.

Simply provide a reference to the official SDK.

`cd sdk.gradle`

`./run.bash /path/to/official/swirlds/sdk`

### Run instructions

Ensure Docker is running on the host machine

`java -jar /path/to/official/swirlds/sdk/swirlds.jar`

### Write a transaction that invokes a smart contract execution

Initiate a Docker instance by typing (i.e. stdin):

"mercury"

into any of the nodes

For now, the pseudocode protocol is:

```
when(transaction string == "mercury"){
  - spin up a Docker instance
  - perform a computation
  - shut down (walltime=10 seconds)
}
```

TODO: net connection - WebSockets, RPC, RabbitMQ, APMQ?


### protobuf

[Google protobuf](https://developers.google.com/protocol-buffers/) is used in preference to POJOs.

State is an array of protobuf bytes[]?

//TODO
