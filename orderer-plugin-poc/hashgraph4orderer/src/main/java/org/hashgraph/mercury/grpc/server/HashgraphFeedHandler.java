package org.hashgraph.mercury.grpc.server;

import org.hyperledger.fabric.protos.orderer.Hashgraph;

public interface HashgraphFeedHandler {

    boolean handle(Hashgraph.Transaction message);

}
