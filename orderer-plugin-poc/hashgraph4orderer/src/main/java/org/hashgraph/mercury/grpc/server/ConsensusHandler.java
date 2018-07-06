package org.hashgraph.mercury.grpc.server;

import com.swirlds.platform.Address;

import java.time.Instant;

public interface ConsensusHandler {
    void handle(long id, boolean consensus, Instant timestamp, byte[] transaction, Address address, long txId);
}
