
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

import java.io.*;

/** The pathname and contents of a file, to be transmitted over the network */
public class FileTransaction implements Serializable {
	/** needed for serializing */
	private static final long serialVersionUID = 1L;

	/** the full path name, including the file name */
	String pathname;
	/** the contents of this file */
	String text;

	/**
	 * record the pathname and text
	 * 
	 * @param pathname
	 *            the full path name, including the file name
	 * @param text
	 *            the contents of this file
	 */
	public FileTransaction(String pathname, String text) {
		this.pathname = pathname;
		this.text = text;
	}

	/**
	 * Serialize this file transaction to a sequence of bytes
	 * 
	 * @return the sequence as a byte array
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream o = new DataOutputStream(b);
		o.writeUTF(pathname);
		o.writeUTF(text);
		o.close();
		return b.toByteArray();
	}

	/**
	 * Deserialize this file transaction from a sequence of bytes
	 *
	 * @param b
	 *            the sequence of bytes
	 * @return the file transaction
	 * @throws IOException
	 *             if anything goes wrong
	 */
	public static FileTransaction deserialize(byte[] b) throws IOException {
		DataInputStream o = new DataInputStream(new ByteArrayInputStream(b));
		String pathname2 = o.readUTF();
		String text2 = o.readUTF();
		FileTransaction result = new FileTransaction(pathname2, text2);
		o.close();
		return result;
	}
}
