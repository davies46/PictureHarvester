package com.re4ct.fileflatten;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

public class FileFlattener implements ActionListener, VisualCallbacks {
	final static Logger	log		= Logger.getLogger(FileFlattener.class);
	// List<File> dstFiles = new LinkedList<>();
	// List<String> dstFiles = new LinkedList<>();
	JFrame				frame;
	JTextArea			outputArea;
	private JLabel		iterLbl;
	JTextField			dstWFld;
	JTextField			dstHFld;
	static byte[]		buf1;
	static byte[]		buf2;
	static ByteBuffer	bufa	= ByteBuffer.allocateDirect(64 * 1024);
	static ByteBuffer	bufb	= ByteBuffer.allocateDirect(64 * 1024);
	static byte[]		buffer;
	// final static int MAX_IMAGE_WIDTH = 1920;
	// final static int MAX_IMAGE_HEIGHT = 19;
	static {
		buffer = new byte[64 * 1024];
		buf1 = new byte[64 * 1024];
		buf2 = new byte[64 * 1024];
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					FileFlattener window = new FileFlattener();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private JLabel		pic1;
	private JLabel		pic2;
	protected boolean	aborting;

	/**
	 * Create the application.
	 */
	public FileFlattener() {
		initialize();
	}

	/**
	 * Initialise the contents of the frame.
	 */
	private void initialize() {
		aborting = false;
		JPanel boardPanel = new JPanel();
		BoxLayout mainLyt = new BoxLayout(boardPanel, BoxLayout.PAGE_AXIS);
		boardPanel.setLayout(mainLyt);

		JPanel srcPnl = new JPanel();
		BoxLayout srcLyt = new BoxLayout(srcPnl, BoxLayout.LINE_AXIS);
		srcPnl.setLayout(srcLyt);

		JPanel dstPnl = new JPanel();
		BoxLayout dstLyt = new BoxLayout(dstPnl, BoxLayout.LINE_AXIS);
		dstPnl.setLayout(dstLyt);

		JLabel srcFolderLbl = new JLabel("No source folder chosen");
		srcFolderLbl.setEnabled(false);

		JLabel dstFolderLbl = new JLabel("No dest folder chosen");
		dstFolderLbl.setEnabled(false);

		ChooserData srcData = new ChooserData(srcFolderLbl);
		ChooserBtn chooseSrcFolderBtn = new ChooserBtn("Src Folder", "R:/Media/pix", srcData);

		ChooserData dstData = new ChooserData(dstFolderLbl);
		ChooserBtn chooseDstFolderBtn = new ChooserBtn("Dst Folder", "D:/Flat pics", dstData);

		dstWFld = new JTextField("1280");
		dstWFld.setToolTipText("Screen width");
		dstWFld.setMaximumSize(new Dimension(200, 24));

		dstHFld = new JTextField("1024");
		dstHFld.setToolTipText("Screen height");
		dstHFld.setSize(200, 24);
		dstHFld.setMaximumSize(new Dimension(200, 24));
		srcPnl.add(srcFolderLbl);
		srcPnl.add(chooseSrcFolderBtn);

		dstPnl.add(dstFolderLbl, 0);
		dstPnl.add(chooseDstFolderBtn, 1);

		boardPanel.add(srcPnl, 0);
		boardPanel.add(dstPnl, 1);

		outputArea = new JTextArea();
		outputArea.setMaximumSize(new Dimension(400, 200));
		outputArea.setAutoscrolls(true);

		boardPanel.add(dstWFld, 2);
		boardPanel.add(dstHFld, 3);
		JButton moveBtn = new JButton("Copy");
		moveBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int maxImgW = Integer.parseInt(dstWFld.getText());
				int maxImgH = Integer.parseInt(dstHFld.getText());
				String src = "R:/pix";
				String dst = "D:/Flat pics";
				outputArea.append(src);
				outputArea.append(" to ");
				outputArea.append(dst);
				outputArea.append("\n");
				CopyManager copyManager = new CopyManager(FileFlattener.this, maxImgW, maxImgH);
				copyManager.doCopy(src, dst, maxImgW, maxImgH);
			}
		});

		boardPanel.add(moveBtn, 4);

		iterLbl = new JLabel();
		boardPanel.add(iterLbl, 5);

		pic1 = new JLabel();
		pic2 = new JLabel();
		pic1.setMaximumSize(new Dimension(200, 200));
		pic1.setMinimumSize(new Dimension(200, 200));
		pic2.setMaximumSize(new Dimension(200, 200));
		pic2.setMinimumSize(new Dimension(200, 200));

		boardPanel.add(pic1, 6);
		boardPanel.add(pic2, 7);
		boardPanel.add(outputArea, 8);
		// boardPanel.add

		frame = new JFrame();
		frame.setBounds(100, 100, 450, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(boardPanel);

		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("Closed");
				aborting = true;
				e.getWindow().dispose();
			}
		});

		int maxImgW = Integer.parseInt(dstWFld.getText());
		int maxImgH = Integer.parseInt(dstHFld.getText());
		CopyManager copyManager = new CopyManager(this, maxImgW, maxImgH);
		String src = "R:/pix";
		String dst = "D:/Flat pics";
		outputArea.append(dst);
		outputArea.append("\n");
		outputArea.append(src);
		outputArea.append(" to ");
		outputArea.append(dst);
		outputArea.append("\n");

		copyManager.doCopy(src, dst, maxImgW, maxImgH);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

	}

	@Override
	public void visualImageCompare(String absolutePath, String dstFileName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIterLbl(String string) {
		// TODO Auto-generated method stub

	}

}
