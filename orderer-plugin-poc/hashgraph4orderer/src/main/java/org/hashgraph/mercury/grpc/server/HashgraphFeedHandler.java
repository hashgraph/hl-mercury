package org.hashgraph.mercury.grpc.server;

public interface HashgraphFeedHandler {

    boolean handle(byte[] message);

}
