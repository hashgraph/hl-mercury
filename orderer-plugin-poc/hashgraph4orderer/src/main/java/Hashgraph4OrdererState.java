import com.swirlds.platform.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hashgraph.mercury.grpc.client.OrdererClient;
import org.hashgraph.mercury.grpc.server.ConsensusHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Hashgraph4OrdererState implements SwirldState {

    private static final Logger LOG = LogManager.getLogger("Hashgraph4Orderer");

    private AtomicLong lastTransactionId = new AtomicLong(-1L);

    private List<String> strings = new ArrayList<String>();

    private AddressBook addressBook;

    private List<ConsensusHandler> consensusHandlers = new ArrayList<>();

    public synchronized List<String> getStrings() {
        return strings;
    }

    public synchronized void addConsensusHandler(ConsensusHandler handler) {
        this.consensusHandlers.add(handler);
    }

    @Override
    public synchronized void init(Platform platform, AddressBook addressBook) {
        this.addressBook = addressBook;

        int consensusPort = platform.getAddress().getPortExternalIpv4();
        int ordererPort = consensusPort + 2000;
        String ordererHost = "orderer.example.com"; // TODO extract to config
        OrdererClient ordererClient = new OrdererClient(ordererHost, ordererPort);
        addConsensusHandler(ordererClient);
    }

    @Override
    public synchronized AddressBook getAddressBookCopy() {
        return addressBook.copy();
    }

    @Override
    public synchronized void copyFrom(SwirldState old) {
        Hashgraph4OrdererState oldHSState = (Hashgraph4OrdererState) old;
        strings = new ArrayList<>(oldHSState.strings);
        lastTransactionId = new AtomicLong(((Hashgraph4OrdererState) old).lastTransactionId.get());
        addressBook = oldHSState.addressBook.copy();
        consensusHandlers = new ArrayList<>(oldHSState.consensusHandlers);
    }

    @Override
    public synchronized void handleTransaction(long id, boolean consensus,
                                               Instant timestamp, byte[] transaction, Address address) {

        if (consensus) {
            long txId = nextTransactionId();

            String message = new String(transaction, StandardCharsets.UTF_8);
            strings.add(message);
            consensusHandlers.forEach(handler -> {
                handler.handle(id, consensus, timestamp, transaction, address, txId);
            });
        }
    }

    private synchronized long nextTransactionId() {
        return lastTransactionId.incrementAndGet();
    }

    @Override
    public void noMoreTransactions() {

    }

    @Override
    public synchronized FastCopyable copy() {
        Hashgraph4OrdererState copy = new Hashgraph4OrdererState();
        copy.copyFrom(this);
        return copy;
    }

    @Override
    public synchronized void copyTo(FCDataOutputStream outStream) {
        try {
            Utilities.writeStringArray(outStream,
                    strings.toArray(new String[0]));
        } catch (IOException e) {
            LOG.warn(e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public synchronized void copyFrom(FCDataInputStream inStream) throws IOException {
        strings = Arrays.asList(Utilities.readStringArray(inStream));
    }
}
