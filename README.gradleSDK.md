## Gradle swirlds

Stripped down version of the Swirlds SDK. Featuring:

- Gradle build (instead of Maven)
- simplified project structure
- copy and paste generated .jar into official Swirls SDK
- first stab at processing contracts (using Docker for now)
- first protocol (dumb) for processing smart contracts
- experiments with Google protobuf for state storage


### Build instructions

Gradle version of official Swirlds `sdk` in "sdk.gradle"

`cd sdk.gradle`

//build gradle project

`gradle build`

//generate jar:

`gradle jar`

//.jar is in "build/libs/"

//copy the generated jar file to the "sdk" folder

`cp ./build/libs/SharedWorld*.jar ../sdk/data/apps/SharedWorld.jar`



### Run instructions

Ensure Docker is running

`cd sdk`

`java -jar swirlds.jar`

Note: swirlds.jar should be downloaded from https://swirlds.com

Initiate a Docker instance by typing (i.e. stdin):

`mercury`

into any of the nodes

TODO: net connection - WebSockets, RPC, RabbitMQ, APMQ?


### protobuf

Google protobuf is used in preference to POJOs

State is an array of protobuf bytes[]?

//TODO
