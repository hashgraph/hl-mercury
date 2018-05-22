
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

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


	private final long walltime = 10000L;

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


				if(shouldRunSmartContract(received)==true){
				  runSmartContract();
				}

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










	/*
	 * not every state update causes a smart contract execution
	 *
	 */
	private boolean shouldRunSmartContract(String received){
		//TODO - for now, the protocol is a very dumb keyword protocol
		//System.out.println(received.trim());
		if(received.trim().endsWith("mercury")==true){
			return true;
		}
		return false;
	}

	/*
	 * `docker run -it --rm alpine /bin/ash`
	 * max walltime of ~10 seconds
	 */
	private void runSmartContract(){
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {

				//this could be gvisor or some other more secure execution environment
				String _cmd="docker run -t -d alpine";   //run Alpine Linux in background. Returns an ID once successfully called

				try{
					Runtime rt = Runtime.getRuntime();
					Process p = rt.exec(_cmd);
					p.waitFor();
					System.out.println("Process exited with code = "+p.exitValue());

					//Get process's output:
					InputStream is = p.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String s;
					String localContainerID="";
					while((s=br.readLine()) != null){
						localContainerID=s;
						//System.out.println(s);
					}
					is.close();
					System.out.println("Started local container with ID: "+localContainerID);





					//checks:
					if(localContainerID == null){
						System.out.println("ERROR: Problem starting the container! Is Docker running?");
						return;
					}
					if(localContainerID.compareTo("null") == 0){
						System.out.println("ERROR: Problem starting the container! Is Docker running?");
						return;
					}
					if(localContainerID.length() < 10){
						System.out.println("ERROR: Problem starting the container! Is Docker running?");
						return;
					}








					//passed checks
					//shutdown after 10 seconds...
					new Timer().schedule(
							new TimerTaskEOH(localContainerID) {
								@Override
								public void run() {
									System.out.println("Stopping localContainerID: "+this.localContainerID);

									//fire and forget:
									try {
										String _cmd = "docker rm -f " + this.localContainerID;
										Runtime rt = Runtime.getRuntime();
										Process p = rt.exec(_cmd);
										p.waitFor();
										System.out.println("`docker rm -f ...` request exited with code = " + p.exitValue());

									}catch(IOException e){
										e.printStackTrace();
									}catch(InterruptedException e){
										e.printStackTrace();
									}
								}
							},
							walltime
					);


				}catch(IOException e){
					e.printStackTrace();
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		});
		t.run();
	}



	private class TimerTaskEOH extends TimerTask{
		public final String localContainerID;

		TimerTaskEOH(String _localContainerID){
			this.localContainerID=_localContainerID;
		}
		@Override
		public void run() {

		}
	}
}
