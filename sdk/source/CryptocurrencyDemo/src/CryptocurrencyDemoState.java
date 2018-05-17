
/*
 * This file is public domain.
 *
 * SWIRLDS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF 
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SWIRLDS SHALL NOT BE LIABLE FOR 
 * ANY DAMAGES SUFFERED AS A RESULT OF USING, MODIFYING OR 
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

import com.swirlds.platform.Address;
import com.swirlds.platform.AddressBook;
import com.swirlds.platform.FCDataInputStream;
import com.swirlds.platform.FCDataOutputStream;
import com.swirlds.platform.FastCopyable;
import com.swirlds.platform.Platform;
import com.swirlds.platform.SwirldState;
import com.swirlds.platform.Utilities;

/**
 * This holds the current state of a swirld representing both a cryptocurrency and a stock market.
 * 
 * This is just a simulated stock market, with fictitious stocks and ticker symbols. But the cryptocurrency
 * is actually real. At least, it is real in the sense that if enough people participate for long enough
 * (and if Swirlds has encryption turned on), then it could actually be a reliable cryptocurrency. An
 * entirely new cryptocurrency is created every time all the computers start the program over again, so
 * these cryptocurrencies won't have any actual value.
 */
public class CryptocurrencyDemoState implements SwirldState {
	/**
	 * the first byte of a transaction is the ordinal of one of these four: do not delete any of these or
	 * change the order (and add new ones only to the end)
	 */
	public static enum TransType {
		slow, fast, bid, ask // run slow/fast or broadcast a bid/ask
	};

	/** in slow mode, number of milliseconds to sleep after each outgoing sync */
	private final static int delaySlowSync = 1000;
	/** in fast mode, number of milliseconds to sleep after each outgoing sync */
	private final static int delayFastSync = 0;
	/** number of different stocks that can be bought and sold */
	public final static int NUM_STOCKS = 10;
	/** remember the last MAX_TRADES trades that occurred. */
	private final static int MAX_TRADES = 200;
	/** the platform running this app */
	private Platform platform = null;

	////////////////////////////////////////////////////
	// the following are the shared state:

	/** names and addresses of all members */
	private AddressBook addressBook;
	/** the number of members participating in this swirld */
	private int numMembers;
	/** ticker symbols for each of the stocks */
	private String[] tickerSymbol;
	/** number of cents owned by each member */
	private long[] wallet;
	/** shares[m][s] is the number of shares that member m owns of stock s */
	private long[][] shares;
	/** a record of the last NUM_TRADES trades */
	private String[] trades;
	/** number of trades currently stored in trades[] (from 0 to MAX_TRADES, inclusive) */
	private int numTradesStored = 0;
	/** the latest trade was stored in trades[lastTradeIndex] */
	private int lastTradeIndex = 0;
	/** how many trades have happened in all history */
	private long numTrades = 0;
	/** the most recent price (in cents) that a seller has offered for each stock */
	private byte[] ask;
	/** the most recent price (in cents) that a buyer has offered for each stock */
	private byte[] bid;
	/** the ID number of the member whose offer is stored in ask[] (or -1 if none) */
	private long[] askId;
	/** the ID number of the member whose offer is stored in bid[] (or -1 if none) */
	private long[] bidId;
	/** price of the most recent trade for each stock */
	private byte[] price;

	////////////////////////////////////////////////////

	/**
	 * get the string representing the trade with the given sequence number. The first trade in all of
	 * history is sequence 1, the next is 2, etc.
	 * 
	 * @param seq
	 *            the sequence number of the trade
	 * @return the trade, or "" if it hasn't happened yet or happened so long ago that it is no longer
	 *         stored
	 */
	public synchronized String getTrade(long seq) {
		if (seq > numTrades || seq <= numTrades - numTradesStored) {
			return "";
		}
		return trades[(int) ((lastTradeIndex + seq - numTrades + MAX_TRADES)
				% MAX_TRADES)];
	}

	/**
	 * get the current price of each stock, copying it into the given array
	 * 
	 * @param price
	 *            the array of NUM_STOCKS elements that will be filled with the prices
	 */
	public synchronized void getPriceCopy(byte[] price) {
		for (int i = 0; i < NUM_STOCKS; i++) {
			price[i] = this.price[i];
		}
	}

	/**
	 * return how many trades have occurred. So getTrade(getNumTrades()) will return a non-empty string (if
	 * any trades have ever occurred), but getTrade(getNumTrades()+1) will return "" (unless one happens
	 * between the two method calls).
	 * 
	 * @return number of trades
	 */
	public synchronized long getNumTrades() {
		return numTrades;
	}

	@Override
	public synchronized AddressBook getAddressBookCopy() {
		return addressBook.copy();
	}

	@Override
	public synchronized FastCopyable copy() {
		CryptocurrencyDemoState copy = new CryptocurrencyDemoState();
		copy.copyFrom(this);
		return copy;
	}

	@Override
	public synchronized void copyTo(FCDataOutputStream outStream) {
		try {
			addressBook.copyTo(outStream);
			outStream.writeInt(numMembers);
			Utilities.writeStringArray(outStream, tickerSymbol);
			Utilities.writeLongArray(outStream, wallet);
			Utilities.writeLongArray2D(outStream, shares);
			Utilities.writeStringArray(outStream, trades);
			outStream.writeInt(numTradesStored);
			outStream.writeInt(lastTradeIndex);
			outStream.writeLong(numTrades);
			Utilities.writeByteArray(outStream, ask);
			Utilities.writeByteArray(outStream, bid);
			Utilities.writeLongArray(outStream, askId);
			Utilities.writeLongArray(outStream, bidId);
			Utilities.writeByteArray(outStream, price);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void copyFrom(FCDataInputStream inStream) {
		try {
			addressBook.copyFrom(inStream);
			numMembers = inStream.readInt();
			tickerSymbol = Utilities.readStringArray(inStream);
			wallet = Utilities.readLongArray(inStream);
			shares = Utilities.readLongArray2D(inStream);
			trades = Utilities.readStringArray(inStream);
			numTradesStored = inStream.readInt();
			lastTradeIndex = inStream.readInt();
			numTrades = inStream.readLong();
			ask = Utilities.readByteArray(inStream);
			bid = Utilities.readByteArray(inStream);
			askId = Utilities.readLongArray(inStream);
			bidId = Utilities.readLongArray(inStream);
			price = Utilities.readByteArray(inStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void copyFrom(SwirldState oldCryptocurrencyState) {
		CryptocurrencyDemoState old = (CryptocurrencyDemoState) oldCryptocurrencyState;

		platform = old.platform;
		addressBook = old.addressBook.copy();
		numMembers = old.numMembers;
		tickerSymbol = old.tickerSymbol.clone();
		wallet = old.wallet.clone();
		shares = Utilities.deepClone(old.shares);
		trades = old.trades.clone();
		numTradesStored = old.numTradesStored;
		lastTradeIndex = old.lastTradeIndex;
		numTrades = old.numTrades;
		ask = old.ask.clone();
		bid = old.bid.clone();
		askId = old.askId.clone();
		bidId = old.bidId.clone();
		price = old.price.clone();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The matching algorithm for any given stock is as follows. The first bid or ask for a stock is
	 * remembered. Then, if there is a higher bid or lower ask, it is remembered, replacing the earlier one.
	 * Eventually, there will be a bid that is equal to or greater than the ask. At that point, they are
	 * matched, and a trade occurs, selling one share at the average of the bid and ask. Then the stored bid
	 * and ask are erased, and it goes back to waiting for a bid or ask to remember.
	 * <p>
	 * If a member tries to sell a stock for which they own no shares, or if they try to buy a stock at a
	 * price higher than the amount of money they currently have, then their bid/ask for that stock will not
	 * be stored.
	 * <p>
	 * A transaction is 1 or 3 bytes:
	 * 
	 * <pre>
	 * {SLOW} = run slowly 
	 * {FAST} = run quickly 
	 * {BID,s,p} = bid to buy 1 share of stock s at p cents (where 0 &lt;= p &lt;= 127) 
	 * {ASK,s,p} = ask to sell 1 share of stock s at p cents (where 1 &lt;= p &lt;= 127)
	 * </pre>
	 */
	@Override
	public synchronized void handleTransaction(long id, boolean isConsensus,
			Instant timestamp, byte[] transaction, Address address) {
		if (transaction == null || transaction.length == 0) {
			return;
		}
		if (transaction[0] == TransType.slow.ordinal()) {
			platform.setSleepAfterSync(delaySlowSync);
			return;
		} else if (transaction[0] == TransType.fast.ordinal()) {
			platform.setSleepAfterSync(delayFastSync);
			return;
		} else if (!isConsensus || transaction.length < 3) {
			return;// ignore any bid/ask that doesn't have consensus yet
		}
		int selfId = (int) id;
		int askBid = transaction[0];
		int tradeStock = transaction[1];
		int tradePrice = transaction[2];

		if (tradePrice < 1 || tradePrice > 127) {
			return; // all asks and bids must be in the range 1 to 127
		}

		if (askBid == TransType.ask.ordinal()) { // it is an ask
			// if they're trying to sell something they don't have, then ignore it
			if (shares[selfId][tradeStock] == 0) {
				return;
			}
			// if previous member with bid no longer has enough money, then forget them
			if (bidId[tradeStock] != -1
					&& wallet[(int) bidId[tradeStock]] < bid[tradeStock]) {
				bidId[tradeStock] = -1;
			}
			// if this is the lowest ask for this stock since its last trade, then remember it
			if (askId[tradeStock] == -1 || tradePrice < ask[tradeStock]) {
				askId[tradeStock] = selfId;
				ask[tradeStock] = (byte) tradePrice;
			}
		} else { // it is a bid
			// if they're trying to buy but don't have enough money, then ignore it
			if (shares[selfId][tradeStock] == 0) {
				return;
			}
			// if previous member with ask no longer has the share, then forget them
			if (askId[tradeStock] != -1
					&& shares[(int) askId[tradeStock]][tradeStock] == 0) {
				askId[tradeStock] = -1;
			}
			// if this is the highest bid for this stock since its last trade, then remember it
			if (bidId[tradeStock] == -1 || tradePrice > bid[tradeStock]) {
				bidId[tradeStock] = selfId;
				bid[tradeStock] = (byte) tradePrice;
			}
		}
		// if there is not yet a match for this stock, then don't create a trade yet
		if (askId[tradeStock] == -1 || bidId[tradeStock] == -1
				|| ask[tradeStock] > bid[tradeStock]) {
			return;
		}

		// there is a match, so create the trade

		// the trade occurs at the mean of the ask and bid
		// if the mean is a non-integer, round to the nearest event integer
		tradePrice = ask[tradeStock] + bid[tradeStock];
		tradePrice = (tradePrice / 2) + ((tradePrice % 4) / 3);

		// perform the trade (exchanging money for a share)
		wallet[(int) askId[tradeStock]] += tradePrice; // seller gets money
		wallet[(int) bidId[tradeStock]] -= tradePrice; // buyer gives money
		shares[(int) askId[tradeStock]][tradeStock] -= 1; // seller gives 1 share
		shares[(int) bidId[tradeStock]][tradeStock] += 1; // buyer gets 1 share

		// save a description of the trade to show on the console
		String selfName = addressBook.getAddress(id).getSelfName();
		String sellerNickname = addressBook.getAddress(askId[tradeStock])
				.getNickname();
		String buyerNickname = addressBook.getAddress(bidId[tradeStock])
				.getNickname();
		int change = tradePrice - price[tradeStock];
		double changePerc = 100. * change / price[tradeStock];
		String dir = (change > 0) ? "^" : (change < 0) ? "v" : " ";
		numTrades++;
		numTradesStored = Math.min(MAX_TRADES, 1 + numTradesStored);
		lastTradeIndex = (lastTradeIndex + 1) % MAX_TRADES;
		String tradeDescription = String.format(
				"%6d %6s %7.2f %s %4.2f %7.2f%% %7s->%s      %5s has $%-8.2f and shares: %s",
				numTrades, tickerSymbol[tradeStock], tradePrice / 100., dir,
				Math.abs(change / 100.), Math.abs(changePerc), sellerNickname,
				buyerNickname, selfName, wallet[(int) id] / 100.,
				Arrays.toString(shares[(int) id]));

		// record the trade, and say there are now no pending asks or bids
		trades[lastTradeIndex] = tradeDescription;
		price[tradeStock] = (byte) tradePrice;
		askId[tradeStock] = -1;
		bidId[tradeStock] = -1;

		// start with fast syncing until first trade, then be slow until user hits "F"
		if (numTrades == 1) {
			platform.setSleepAfterSync(delaySlowSync);
		}
	}

	@Override
	public synchronized void noMoreTransactions() {
	}

	@Override
	public synchronized void init(Platform platform, AddressBook addressBook) {
		this.platform = platform;
		this.addressBook = addressBook;
		this.numMembers = addressBook.getSize();
		tickerSymbol = new String[NUM_STOCKS];
		wallet = new long[numMembers];
		shares = new long[numMembers][NUM_STOCKS];
		trades = new String[MAX_TRADES];
		numTradesStored = 0;
		lastTradeIndex = 0;
		numTrades = 0;
		ask = new byte[NUM_STOCKS];
		bid = new byte[NUM_STOCKS];
		askId = new long[NUM_STOCKS];
		bidId = new long[NUM_STOCKS];
		price = new byte[NUM_STOCKS];

		// seed 0 so everyone gets same ticker symbols on every run
		Random rand = new Random(0);
		for (int i = 0; i < NUM_STOCKS; i++) {
			tickerSymbol[i] = "" // each ticker symbol is 4 random capital letters (ASCII 65 is 'A')
					+ (char) (65 + rand.nextInt(26))
					+ (char) (65 + rand.nextInt(26))
					+ (char) (65 + rand.nextInt(26))
					+ (char) (65 + rand.nextInt(26));
			askId[i] = bidId[i] = -1; // no one has offered to buy or sell yet
			ask[i] = bid[i] = price[i] = 64; // start the trading around 64 cents
		}
		for (int i = 0; i < numMembers; i++) {
			wallet[i] = 20000; // each member starts with $200 dollars (20,000 cents)
			shares[i] = new long[NUM_STOCKS];
			for (int j = 0; j < NUM_STOCKS; j++) {
				shares[i][j] = 200; // each member starts with 200 shares of each stock
			}
		}
		// start with fast syncing, until the first trade
		this.platform.setSleepAfterSync(delayFastSync);
	}
}