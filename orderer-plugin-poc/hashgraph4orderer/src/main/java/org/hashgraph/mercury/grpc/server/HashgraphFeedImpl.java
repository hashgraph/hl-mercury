package org.hashgraph.mercury.grpc.server;

import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.protos.orderer.Hashgraph;
import org.hyperledger.fabric.protos.orderer.HashgraphServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class HashgraphFeedImpl extends HashgraphServiceGrpc.HashgraphServiceImplBase {

    private final List<HashgraphFeedHandler> handlers = new ArrayList<>();

    public void addMessageHandler(HashgraphFeedHandler handler) {
        handlers.add(handler);
    }

    @Override
    public void create(Hashgraph.Transaction request, StreamObserver<Hashgraph.CreateResponse> responseObserver) {
        handlers.forEach(handler -> {
            boolean accepted = handler.handle(request);
            Hashgraph.CreateResponse response = Hashgraph.CreateResponse.newBuilder()
                    .setAccepted(accepted)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        });
    }

}
