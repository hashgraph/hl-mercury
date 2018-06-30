package org.hashgraph.mercury.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcServer {

    private Server server;

    private HashgraphFeedImpl service;

    private int port;

    public GrpcServer(int port) {
        this.port = port;
        this.service = new HashgraphFeedImpl();
    }

    public HashgraphFeedImpl getService() {
        return service;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(service)
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down. port: " + port);
            GrpcServer.this.stop();
            System.err.println("*** server shut down. port: " + port);
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

}
