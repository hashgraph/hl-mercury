
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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

import com.swirlds.platform.Browser;
import com.swirlds.platform.Console;
import com.swirlds.platform.Platform;
import com.swirlds.platform.SwirldMain;
import com.swirlds.platform.SwirldState;

/**
 * This demonstrates a cryptocurrency and a stock market. There are 10 stocks, and each member repeatedly
 * generates an ask or a bid on a stock, offering to sell or buy, respectively, a single share at a random
 * price between 1 and 127 cents (inclusive).
 */
public class CryptocurrencyDemoMain implements SwirldMain {
	/** time to delay between screen updates, in milliseconds (250 for 4 times a second) */
	private final long screenUpdateDelay = 250;
	/** the app is run by this */
	private Platform platform;
	/** ID number for this member */
	private long selfId;
	/** a console window for text output */
	private Console console;
	/** used to randomly choose ask/bid and prices */
	private Random rand = new Random();
	/** so user can use arrows and spacebar */
	private GuiKeyListener keyListener = new GuiKeyListener();
	/** if not -1, then need to create a transaction to sync fast or slow */
	private byte speedCmd = -1;
	/** is the simulation running fast now? */
	private boolean isFast = false;

	/** Listen for input from the keyboard, and remember the last key typed. */
	private class GuiKeyListener implements KeyListener {
		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyChar() == 'F' || e.getKeyChar() == 'f') {
				isFast = true;
				speedCmd = (byte) CryptocurrencyDemoState.TransType.fast
						.ordinal();
			} else if (e.getKeyChar() == 'S' || e.getKeyChar() == 's') {
				isFast = false;
				speedCmd = (byte) CryptocurrencyDemoState.TransType.slow
						.ordinal();
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

	/**
	 * This is just for debugging: it allows the app to run in Eclipse. If the config.txt exists and lists a
	 * particular SwirldMain class as the one to run, then it can run in Eclipse (with the green triangle
	 * icon).
	 * 
	 * @param args
	 *            these are not used
	 */
	public static void main(String[] args) {
		Browser.main(args);
	}

	// ///////////////////////////////////////////////////////////////////

	@Override
	public void preEvent() {
		int numStocks = CryptocurrencyDemoState.NUM_STOCKS;
		byte[] price = new byte[numStocks];
		CryptocurrencyDemoState state = (CryptocurrencyDemoState) platform
				.getState();
		state.getPriceCopy(price);
		// create one transaction for each stock, to try to buy or sell it
		for (int i = 0; i < numStocks; i++) {
			if (Math.random() < 0.5) {
				byte askBid = (byte) (rand.nextInt(2) == 0 // randomly choose either ask or bid
						? CryptocurrencyDemoState.TransType.ask.ordinal()
						: CryptocurrencyDemoState.TransType.bid.ordinal());
				byte cents = (byte) Math.max(0, // ask or bid a price close to the current price
						Math.min(127, price[i] + rand.nextInt(5) - 2));
				platform.createTransaction(
						new byte[] { askBid, (byte) i, cents });
			}
		}
		if (speedCmd != -1) {
			platform.createTransaction(new byte[] { speedCmd });
			speedCmd = -1;
		}
	}

	@Override
	public void init(Platform platform, long id) {
		this.platform = platform;
		this.selfId = id;
		this.console = platform.createConsole(true); // create the window, make it visible
		platform.setAbout("Cryptocurrency and stock market demo v. 1.0\n"); // set the browser's "about" box
		this.console.addKeyListener(keyListener);
	}

	@Override
	public void run() {
		long seq = 0;
		// print the latest trades to the console, 4 times a second, forever
		while (true) {
			CryptocurrencyDemoState state = (CryptocurrencyDemoState) platform
					.getState();
			console.setHeading(" Cryptocurrency and Stock Market Demo\n"
					+ " Press F for fast sync, S for slow, (currently "
					+ (isFast ? "fast" : "slow") + ")\n"
					+ String.format(" %d",
							(int) platform.getStats().getStat("trans/sec"))
					+ " transactions per second for member " + selfId + "\n\n"
					+ " count  ticker  price change  change%  seller->buyer");
			long lastSeq = state.getNumTrades();
			for (; seq < lastSeq; seq++) {
				String s = state.getTrade(seq);
				if (!s.equals("")) {
					console.out.println(s);
				}
			}
			try {
				Thread.sleep(screenUpdateDelay);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public SwirldState newState() {
		return new CryptocurrencyDemoState();
	}
}