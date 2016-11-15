package com.re4ct.fileflatten;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class FileFlattener implements ActionListener {
	final static Logger	log				= Logger.getLogger(FileFlattener.class);
	// List<File> dstFiles = new LinkedList<>();
	// List<String> dstFiles = new LinkedList<>();
	Set<String>			writtenFiles	= new HashSet<>();
	JFrame				frame;
	JTextArea			outputArea;
	File				dstRootFolder;
	File				srcRootFolder;
	boolean				aborting;
	int					copies;
	int					visited;
	int					renames;
	private JLabel		iterLbl;
	JTextField			dstWFld;
	JTextField			dstHFld;
	public int			maxImgW;
	public int			maxImgH;
	private long		hash1;
	private long		hash2;
	static byte[]		buf1;
	static byte[]		buf2;
	static ByteBuffer	bufa			= ByteBuffer.allocateDirect(64 * 1024);
	static ByteBuffer	bufb			= ByteBuffer.allocateDirect(64 * 1024);
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

	class Copyer implements Runnable {

		@Override
		public void run() {
			log.info("Start");
			writtenFiles.clear();
			for (File child : dstRootFolder.listFiles()) {
				assertTrue("Dest folder should not contain sub-folders", child.isFile());
				assertFalse("Dest folder should not contain sub-folders", child.isDirectory());
				String dstPath = child.getName();
				log.info(dstPath);
				outputArea.append(dstPath);
				outputArea.append("\n");
				// dstFiles.add(dstPath);
			}
			long start = System.currentTimeMillis();
			maxImgW = Integer.parseInt(dstWFld.getText());
			maxImgH = Integer.parseInt(dstHFld.getText());
			recurseDownFolderAndCopyToDest(srcRootFolder);
			long finish = System.currentTimeMillis();
			long time = finish - start;
			log.info("Time taken " + time + "ms");
			log.info("Copied " + copies + " files");
			log.info("Renamed " + renames + " files");
		}
	}

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
		dstRootFolder = srcRootFolder = null;
		copies = renames = visited = 0;

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
		outputArea.setSize(400, 200);
		outputArea.setAutoscrolls(true);

		boardPanel.add(dstWFld);
		boardPanel.add(dstHFld);
		JButton moveBtn = new JButton("Copy");
		moveBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String src = srcData.getPath();
				String dst = dstData.getPath();
				outputArea.append(src);
				outputArea.append(" to ");
				outputArea.append(dst);
				outputArea.append("\n");

				dstRootFolder = new File(dst);
				assertTrue("Dst must be folder", dstRootFolder.isDirectory());

				srcRootFolder = new File(src);
				assertTrue("Src must be folder", srcRootFolder.isDirectory());

				Thread thread = new Thread(new Copyer());
				thread.start();
			}
		});

		boardPanel.add(moveBtn);

		iterLbl = new JLabel();
		boardPanel.add(iterLbl);
		boardPanel.add(outputArea);

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
	}

	protected void recurseDownFolderAndCopyToDest(File srcPath) {
		if (aborting)
			return;
		log.info("<<" + srcPath.getName() + ">>");
		for (File child : srcPath.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String extn = FilenameUtils.getExtension(pathname.getName());
				return pathname.isDirectory() || extn.equalsIgnoreCase("jpg") || extn.equalsIgnoreCase("png");
				// return pathname.isDirectory() || pathname.getName().equalsIgnoreCase("DSCF0369.JPG");
			}
		})) {
			visited++;
			try {
				if (child.isDirectory()) {
					recurseDownFolderAndCopyToDest(child);
				} else {
					if (child.length() > 32768) {
						String srcName = child.getName();
						String key = srcName.toLowerCase();
						File dest = null;

						// What we want to do now is determine the hash of the output image that will be generated
						// after scaling and rotation,
						// then see if the hash already exists. If it does we need to discover what file(s)
						// that relates to so that we can drag those out and do a pixel compare of those.
						if (writtenFiles.contains(key)) {
							assertTrue(child.isFile());
							File existingDstFile = new File(dstRootFolder + "/" + child.getName());
							if (sameContents1(child, existingDstFile)) {
								log.info("Same contents so we can ignore " + child + ", " + existingDstFile);
								continue;
							}
							String newName;
							String base = FilenameUtils.getBaseName(srcName);

							log.info("Different contents so we have to rename");
							for (int suf = 0;; suf++) {
								newName = base + "-" + suf + "." + "jpg";
								if (!writtenFiles.contains(newName.toLowerCase())) {
									break;
								}
							}

							log.info("Renaming from " + srcName + " to " + newName + ", while copying " + child + " to " + existingDstFile.getPath());
							dest = new File(dstRootFolder.getAbsolutePath() + "/" + newName);
							renames++;
						} else {
							log.info("Copy file " + child.getCanonicalFile() + " to " + dstRootFolder.getAbsolutePath() + "/" + child.getName());
							String base = FilenameUtils.getBaseName(srcName);

							dest = new File(dstRootFolder.getAbsolutePath() + "/" + base + ".jpg");
						}
						assertNotNull(dest);

						// TODO the file name might be .png, we need to change to .jpg
						BufferedImage scaledImage = getScaledBufferedImage(child);
						if (scaledImage != null) {
							ImageIO.write(scaledImage, "jpg", dest);
							writtenFiles.add(dest.getName().toLowerCase());
							copies++;

							log.warn("copied");
							// fail("OK");
						}
					}
				}
				if (visited % 10 == 0)
					iterLbl.setText("Total visited " + visited + ", copied " + (renames + copies));
				// if (renames > 3)
				// break;
			} catch (Exception e) {
				log.error("Exc", e);
			}
		}
	}

	private BufferedImage getScaledBufferedImage(File child) {
		BufferedImage img1;
		try {
			img1 = ImageIO.read(child);

			if (img1 == null) {
				log.error("Bad file " + child.getCanonicalFile());
				return null;
			}
			int rawW = img1.getWidth();
			int rawH = img1.getHeight();

			double underWidth = 1.0 * maxImgW / rawW;
			double underHeight = 1.0 * maxImgH / rawH;
			// if it's overwidth by more than overheight then scale according to overwidth
			// if it's under both, same
			double scale = (underWidth < underHeight) ? underWidth : underHeight;

			int newW = (int) (rawW * scale);
			int newH = (int) (rawH * scale);

			BufferedImage scaledBufferedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaledBufferedImage.createGraphics();
			g.drawImage(img1, 0, 0, newW, newH, null);
			return scaledBufferedImage;
		} catch (Exception e) {
			log.error("Failed to open image " + child.getName(), e);
			return null;
		}
	}

	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

	// private static void copyFileUsingStream(File source, File dest) throws IOException {
	// InputStream is = null;
	// OutputStream os = null;
	// try {
	// is = new FileInputStream(source);
	// os = new FileOutputStream(dest);
	// int length;
	// while ((length = is.read(buffer)) > 0) {
	// os.write(buffer, 0, length);
	// }
	// } finally {
	// is.close();
	// os.close();
	// }
	// }

	public static boolean isEqual(File f1, File f2) throws IOException {
		try (InputStream is1 = new FileInputStream(f1); InputStream is2 = new FileInputStream(f2)) {
			return isEqual(is1, is2);
		}
	}

	private static boolean isEqual(InputStream i1, InputStream i2) throws IOException {
		try (ReadableByteChannel ch1 = Channels.newChannel(i1); ReadableByteChannel ch2 = Channels.newChannel(i2);) {
			while (true) {

				int n1 = ch1.read(bufa);
				int n2 = ch2.read(bufb);

				if (n1 == -1 || n2 == -1)
					return n1 == n2;

				bufa.flip();
				bufb.flip();

				for (int i = 0; i < Math.min(n1, n2); i++)
					if (bufa.get() != bufb.get())
						return false;

				bufa.compact();
				bufb.compact();
			}

		} finally {
			if (i1 != null)
				i1.close();
			if (i2 != null)
				i2.close();
		}
	}

	// private static boolean isEqual2(InputStream i1, InputStream i2) throws IOException {
	// try (DataInputStream d1 = new DataInputStream(i1); DataInputStream d2 = new DataInputStream(i2)) {
	// int len;
	// int ofs = 0;
	// while ((len = d1.read(buf1)) > 0) {
	// d2.readFully(buf2, 0, len);
	// for (int i = 0; i < len; i++)
	// if (buf1[i] != buf2[i]) {
	// ofs += i;
	// log.info("Ofs for mismatch is " + ofs + " with " + buf1[i] + "-" + buf2[i]);
	// return false;
	// }
	// ofs += len;
	// }
	// return d2.read() < 0; // is the end of the second file also.
	// }
	// }

	// private static boolean sameContents(File srcFile, File existingDstfile) throws IOException {
	// if (srcFile.length() != existingDstfile.length())
	// return false;
	// long start = System.currentTimeMillis();
	// // boolean res = FileUtils.contentEquals(srcFile, existingDstfile);
	// boolean res = isEqual(srcFile, existingDstfile);
	// long finish = System.currentTimeMillis();
	// // log.info("FileUtils.contentEquals " + (finish - start) + "ms");
	// return res;
	// }

	boolean sameContents1(File f1, File f2) throws IOException {
		BufferedImage srcFileImage = ImageIO.read(f1);
		BufferedImage dstFileImage = ImageIO.read(f2);
		return bufferedImagesEqual2(srcFileImage, dstFileImage);
	}

	boolean bufferedImagesEqual2(BufferedImage img1, BufferedImage img2) {
		DataBuffer img1DB = img1.getData().getDataBuffer();
		DataBuffer img2DB = img2.getData().getDataBuffer();

		int numBanks = img1DB.getNumBanks();
		// log.info("Banks " + numBanks);

		if (numBanks != img2DB.getNumBanks())
			return false;

		int numEls = img1DB.getSize();
		// log.info("numEls " + numEls);
		if (numEls != img2DB.getSize())
			return false;
		hash1 = 0;
		hash2 = 0;
		for (int bank = 0; bank < numBanks; bank++) {
			for (int el = 0; el < numEls; el++) {
				int el1 = img1DB.getElem(bank, el);
				int el2 = img2DB.getElem(bank, el);
				hash1 += el1;
				hash1 = hash1 << 3 | hash1 >> (64 - 3);
				hash1 ^= -1;
				hash2 += el2;
				hash2 = hash2 << 3 | hash2 >> (64 - 3);
				hash2 ^= -1;

				if (el1 != el2)
					return false;
			}
		}
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

	}

}
