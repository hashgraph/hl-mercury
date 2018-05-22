
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.swirlds.platform.*;
import com.swirlds.platform.fc.fs.*;

/**
 * The state is primarily a Fast Copyable Filesystem. A transaction is an update to the filesystem. For now
 * that takes the form of a filename and its contents.
 */
public class FilesystemDemoState implements SwirldState {
	/** the platform running this app */
	private Platform platform;
	/** the names and addresses of all members */
	private AddressBook addressBook;
	/** the fast copyable filesystem storing all the files */
	private FilesystemFC fs;

	/** @return the current filesystem */
	public synchronized FilesystemFC getFS() {
		return fs;
	}

	/**
	 * @return the contents of the file given by <code>pathname</code>
	 * @param pathname
	 *            the path name, including the file name, in the fast copyable filesystem
	 * @return the text inside the file
	 * @throws IOException
	 *             problems with the files and streams
	 */
	public synchronized String fileContents(String pathname)
			throws IOException {
		return new String(fs.slurp(pathname), StandardCharsets.UTF_8);
	}

	/** @return a copy of the current address book */
	public synchronized AddressBook getAddressBookCopy() {
		return addressBook.copy();
	}

	@Override
	public synchronized FastCopyable copy() {
		FilesystemDemoState copy = new FilesystemDemoState();
		copy.copyFrom(this);
		return copy;
	}

	@Override
	public synchronized void copyTo(FCDataOutputStream outStream)
			throws IOException {
		addressBook.copyTo(outStream);
		fs.copyTo(outStream);
	}

	@Override
	public synchronized void copyFrom(FCDataInputStream inStream)
			throws IOException {
		addressBook.copyFrom(inStream);
		fs.copyFrom(inStream);
	}

	@Override
	public synchronized void copyFrom(SwirldState old) {
		FilesystemDemoState old1 = (FilesystemDemoState) old;
		platform = old1.platform;
		addressBook = old1.addressBook.copy();
		fs = old1.fs.copy();
	}

	/**
	 * Create (or replace) a file whose pathname and contents are described in the transaction. Any
	 * intermediate directories that don't already exist locally are created first.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void handleTransaction(long id, boolean consensus,
			Instant timestamp, byte[] transaction, Address address) {
		try {
			FileTransaction tx = FileTransaction.deserialize(transaction);
			if (fs.resolvePath(tx.pathname).isEmpty())
				throw new IllegalArgumentException("empty pathname");
			fs.ensureDirectoriesExist(fs.parentDir(tx.pathname));
			fs.dump(tx.text.getBytes(StandardCharsets.UTF_8), tx.pathname);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void noMoreTransactions() {
	}

	@Override
	public synchronized void init(Platform platform, AddressBook addressBook) {
		this.platform = platform;
		this.addressBook = addressBook;
		fs = FilesystemFC.newInstance();
	}
}