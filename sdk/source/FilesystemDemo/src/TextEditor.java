
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.function.*;

import javax.swing.*;

import com.swirlds.platform.*;
import com.swirlds.platform.fc.fs.*;

/**
 * A simple text editor that saves files on a Swirlds Fast Copyable Filesystem. Every save is transmitted as
 * a transaction to peer nodes. This code also demonstrates how to invoke the graphical FileExplorer to load
 * or save files in a filesystem.
 */
public class TextEditor extends JPanel {
	/** used to serialize */
	private static final long serialVersionUID = 1L;

	/** The Swirlds Platform object for this app and node */
	private Platform platform;

	/** the window for the editor */
	private JFrame topFrame;
	/** the text of the file, which the user can edit */
	private JTextArea text;
	/** a button to save the user's changes */
	private JButton saveButton;
	/** the status bar in the window */
	private JTextField statusBar;
	/** the displayed file path in the fast copyable file system */
	private JTextField docPath;

	/**
	 * instantiate and remember the top frame (window) and platform. Use openOn() to open the GUI.
	 * 
	 * @param topFrame
	 *            the window that the user uses to edit
	 * @param platform
	 *            the platform running this app
	 */
	private TextEditor(JFrame topFrame, Platform platform) {
		this.topFrame = topFrame;
		this.platform = platform;
		build();
	}

	/**
	 * Open a GUI
	 * 
	 * @param platform
	 *            the platform running this app
	 * @return the new editor
	 * @throws InterruptedException
	 *             if the thread is interrupted
	 */
	public static TextEditor openOn(Platform platform)
			throws InterruptedException {
		class Holder {
			TextEditor val;
		}
		Holder h = new Holder();
		SwingUtilities.invokeLater(() -> {
			openOn1(platform, (wp) -> {
				synchronized (h) {
					h.val = wp;
					h.notifyAll();
				}
			});
		});
		synchronized (h) {
			while (h.val == null)
				h.wait();
			return h.val;
		}
	}

	/**
	 * called from run() to open it
	 * 
	 * @param platform
	 *            the platform running this app
	 * @param c
	 *            a lambda expression to run on the new TextEditor created
	 */
	private static void openOn1(Platform platform, Consumer<TextEditor> c) {
		JFrame frame = platform.createWindow(false);

		TextEditor wp = new TextEditor(frame, platform);
		frame.getContentPane().add(wp);

		frame.pack();
		frame.setVisible(true);
		c.accept(wp);
	}

	/**
	 * Update the text editor's status bar with some text
	 * 
	 * @param text
	 *            the text to display
	 */
	public void status(String text) {
		SwingUtilities.invokeLater(() -> {
			statusBar.setText(text);
		});
	}

	/** create and set up the window */
	private void build() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		text = new JTextArea(10, 25);
		add(text);

		docPath = new JTextField();
		setUnitHeight(docPath);
		docPath.setEditable(false);
		add(docPath);

		JPanel buttons = new JPanel();
		JButton loadButton = new JButton("Load");
		loadButton.addActionListener(this::load);
		buttons.add(loadButton);
		saveButton = new JButton("Save");
		saveButton.addActionListener(this::save);
		saveButton.setEnabled(false);
		buttons.add(saveButton);
		JButton saveAsButton = new JButton("Save As");
		saveAsButton.addActionListener(this::saveAs);
		buttons.add(saveAsButton);
		add(buttons);

		statusBar = new JTextField();
		statusBar.setEditable(false);
		setUnitHeight(statusBar);
		add(statusBar);
	}

	/**
	 * set the height of the text box to 1, and the width to the maximum
	 * 
	 * @param field
	 *            the JTextField whose height should be set to 1
	 */
	private void setUnitHeight(JTextField field) {
		field.setMaximumSize(
				new Dimension((int) field.getMaximumSize().getWidth(), 1));
	}

	/**
	 * This method is an ActionListener that will open a file explorer that allows loading a file into the
	 * edit pane
	 * 
	 * @param e
	 *            the action event, which is ignored, but must be a parameter in order for this to be an
	 *            action listener
	 */
	private void load(ActionEvent e) {
		try {
			FilesystemDemoState state = getState();
			FilesystemFC fs = state.getFS();
			String pathname = FileExplorer.choose(topFrame, fs, false);
			if (pathname != null && fs.isFile(pathname)) {
				text.setText(state.fileContents(pathname));
				setDocPath(pathname);
			}
		} catch (IOException e1) {
			showError(e1);
		}
	}

	/**
	 * get the current state from the platform
	 * 
	 * @return the state
	 */
	private FilesystemDemoState getState() {
		return ((FilesystemDemoState) platform.getState());
	}

	/**
	 * This method is an ActionListener that will open a file explorer that allows saving the current text
	 * 
	 * @param e
	 *            the action event, which is ignored, but must be a parameter in order for this to be an
	 *            action listener
	 */
	private void saveAs(ActionEvent e) {
		try {
			FilesystemDemoState state = getState();
			boolean done = false;
			while (!done) {
				String pathname = FileExplorer.choose(topFrame, state.getFS(),
						true);
				if (pathname == null)
					done = true;
				else if (state.getFS().isDir(pathname))
					showMessage(String.format("Sorry, \"%s\" is a directory.",
							pathname));
				else {
					done = true;
					publishTx(pathname, text.getText());
					setDocPath(pathname);
				}
			}
		} catch (IOException e1) {
			showError(e1);
		}
	}

	/**
	 * create the transaction to send the text out
	 * 
	 * @param pathname
	 *            the path to the file, including the filename, in the fast copyable filesystem
	 * @param text2
	 *            the text in the file
	 * @throws IOException
	 *             if there is a problem with the files and streams
	 */
	private void publishTx(String pathname, String text2) throws IOException {
		platform.createTransaction(
				new FileTransaction(pathname, text2).serialize());
	}

	/**
	 * This method is an ActionListener that will save the current version of the text
	 * 
	 * @param e
	 *            the action event, which is ignored, but must be a parameter in order for this to be an
	 *            action listener
	 */
	private void save(ActionEvent e) {
		try {
			publishTx(docPath.getText(), text.getText());
		} catch (IOException e1) {
			showError(e1);
		}
	}

	/**
	 * set the document path within the fast copyable filesystem
	 * 
	 * @param pathname
	 *            the path, including filename, in the fast copyable filesystem
	 */
	private void setDocPath(String pathname) {
		docPath.setText(pathname);
		saveButton.setEnabled(true);
	}

	/**
	 * display an exception error message
	 * 
	 * @param e1
	 *            the exception to show
	 */
	private void showError(IOException e1) {
		showMessage(e1.getMessage());
	}

	/**
	 * display a message in a popup window
	 * 
	 * @param message
	 *            the message to display
	 */
	private void showMessage(String message) {
		JOptionPane.showMessageDialog(topFrame, message);
	}
}
