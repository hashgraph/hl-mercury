### Hashgraph Consensus for Hyperledger Fabric Orderer Server

This is one of the pieces that enables running Hyperledger Fabric (HLF) chaincode on Hashgraph consensus. More exactly, it is a Hashgraph Application that has the sole purpose to accept Fabric transactions, run them through Hashgraph consensus and send the consented (and ordered) transactions back to Fabric.

To setup HLF Orderer, see the [Hashgraph Consensus README.md](https://github.com/dappcoder/fabric/tree/hashgraph-orderer-plugin/orderer/consensus/hashgraph)

The communication with the HLF Orderer Server takes place via gRPC and protobuf. This app exposes one gRPC endpoint through which transactions are sent by the Orderer. After the transactions reach hashgraph consensus, they are sent back to the Orderer through gRPC again. This time the Orderer plays the server role accepting transactions.


##### 1. Hashgraph SDK
   * Change `sdk.dir` property in the pom.xml to point to the location where Hashgraph SDK is installed (default `/usr/local/swirlds/sdk`).
   * To install Swirlds Hashgraph SDK, follow the steps on project's official web page https://dev.hashgraph.com/docs/installation/

##### 2. Configure the SDK

Change the config.txt file found in the SDK as follows
   * comment all `app` lines and add a new one for this application (Hashgraph4Orderer)
```
...
# app,		GameDemo.jar,		   9000, 9000
app,        Hashgraph4Orderer.jar
...
```
   * For now, switch off the TLS encryption for a faster startup. You can come back and revert this change later. Just uncomment the line:
```
TLS, off
```
   * Raise transaction limit to 1MB
```
transactionMaxBytes, 1048576
```

##### 3. Build it
```
mvn clean install
```
This will package the app jar and copy it to the 'data/apps' dir inside the Hashgraph sdk.

##### 4. Run it
From IntelliJ IDEA:
   * Run -> Edit Configurations...
   * Add new Application cofiguration
   * Main Class: Hashgraph4OrdererMain
   * Working Directory: /home/alex/Repositories/swirlds/sdk
   * Press "Run..." or "Debug..."

From command line:
Go to hashgraph sdk dir and run:
```
java -jar swirlds.jar
```

You should see four console windows and one main browser window.

NOTE: Every code change needs a 'mvn clean install' before you run the app again
