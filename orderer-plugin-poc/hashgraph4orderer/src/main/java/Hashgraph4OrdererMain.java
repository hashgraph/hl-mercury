import com.swirlds.platform.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hashgraph.mercury.grpc.server.ConsensusHandler;
import org.hashgraph.mercury.grpc.server.GrpcServer;
import org.hyperledger.fabric.protos.orderer.Hashgraph;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

public class Hashgraph4OrdererMain implements SwirldMain, ConsensusHandler {

    private static final Logger LOG = LogManager.getLogger("Hashgraph4Orderer");

    private Platform platform;

    private Console console;

    private GrpcServer server;


    @Override
    public void init(Platform platform, long l) {
        this.platform = platform;
        this.console = platform.createConsole(true);

        int consensusPort = platform.getAddress().getPortExternalIpv4();
        int grpcPort = consensusPort + 1000;
        server = new GrpcServer(grpcPort);

        getConsole().println("Initialized " + platform.getAddress().getSelfName());
    }

    @Override
    public void run() {
        try {
            server.start();
            server.getService().addMessageHandler(this::sendAsTransaction);
            Hashgraph4OrdererState state = (Hashgraph4OrdererState) platform.getState();
            state.addConsensusHandler(this);
            server.blockUntilShutdown();
        } catch (IOException | InterruptedException e) {
            LOG.warn(e);
            throw new RuntimeException("Could not start GRPC Server", e);
        }
    }

    private boolean sendAsTransaction(Hashgraph.Transaction message) {
        getConsole().println("Got one transaction from Hyperledger Orderer." +
                "\nPayload Bytes:   " + message.getPayload().size() +
                "\nChainID:         " + message.getChainID() +
                "\nConfig:          " + message.getConfigMessage());
        return this.platform.createTransaction(message.toByteArray());
    }

    @Override
    public void preEvent() {

    }

    @Override
    public SwirldState newState() {
        Hashgraph4OrdererState state = new Hashgraph4OrdererState();
        state.addConsensusHandler(this);
        return state;
    }

    public static void main(String[] args) {
        Browser.main(args);
    }

    @Override
    public void handle(long id, boolean consensus, Instant timestamp, byte[] transaction, Address address, long txId) {
        getConsole().println("Received CONSENSUS transaction. TxID: " + txId);
    }

    public PrintStream getConsole() {
        PrintStream printStream;

        //TODO find a safer check for headless environment
        if (console != null) {
            printStream = console.out;
        } else {
            printStream = System.out;
        }
        return printStream;
    }
}
