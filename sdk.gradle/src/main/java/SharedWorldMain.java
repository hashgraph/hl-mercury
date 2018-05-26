
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
import java.nio.charset.StandardCharsets;

import com.swirlds.platform.Browser;
import com.swirlds.platform.Console;
import com.swirlds.platform.Platform;
import com.swirlds.platform.SwirldMain;
import com.swirlds.platform.SwirldState;

/**
 * This HelloSwirld creates a single transaction, consisting of the string "Hello Swirld", and then goes
 * into a busy loop (checking once a second) to see when the state gets the transaction. When it does, it
 * prints it, too.
 */
public class SharedWorldMain implements SwirldMain {
	/** the platform running this app */
	public Platform platform;
	/** ID number for this member */
	public long selfId;
	/** a console window for text output */
	public Console console;
	/** sleep this many milliseconds after each sync */
	public final int sleepPeriod = 100;




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
	}

	@Override
	public void init(Platform platform, long id) {
		this.platform = platform;
		this.selfId = id;
		this.console = platform.createConsole(true); // create the window, make it visible
		platform.setAbout("Hello Swirld v. 1.0\n"); // set the browser's "about" box
		platform.setSleepAfterSync(sleepPeriod);
	}

	@Override
	public void run() {
		String myName = platform.getState().getAddressBookCopy().getAddress(selfId).getSelfName();

		console.out.println("Hello Swirld from " + myName);

		// create a transaction. For this example app,
		// we will define each transactions to simply
		// be a string in UTF-8 encoding.
		byte[] transaction = myName.getBytes(StandardCharsets.UTF_8);

		// Send the transaction to the Platform, which will then
		// forward it to the State object.
		// The Platform will also send the transaction to
		// all the other members of the community during syncs with them.
		// The community as a whole will decide the order of the transactions
		platform.createTransaction(transaction);
		String lastAllReceived = "";




		/////
		//stdin
		/////
		this.console.addKeyListener(new KeyListener() {
			private String _buffer="";

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {
				this._buffer+=e.getKeyChar();

				if(e.getKeyCode()==KeyEvent.VK_ENTER){
					this._buffer=this._buffer.trim();
					console.out.println("Writing: "+this._buffer);
					this.eoh_write();

					//clear the buffer:
					this._buffer="";
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

			}

			private void eoh_write(){
				//TODO - protobuf encoding?
				byte[] transaction = this._buffer.getBytes(StandardCharsets.UTF_8);
				platform.createTransaction(transaction);
			}

		});



		/////
		//network in
		/////
		//TODO - receive input from the network (i.e. not just stdin)




		/////
		//Listen for consensus updates
		/////
		while (true) {
			SharedWorldState state = (SharedWorldState) platform.getState();
			String allReceived = state.getAllReceived();
			String received = state.getReceived();

			if (!lastAllReceived.equals(allReceived)) {
				lastAllReceived = allReceived;
				console.out.println("Received: " + allReceived); // print all received transactions
			}
			try {
				Thread.sleep(sleepPeriod);
			} catch (Exception e) {

			}
		}
	}

	@Override
	public SwirldState newState() {
		return new SharedWorldState();
	}











}
