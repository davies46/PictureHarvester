package com.re4ct.fileflatten;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

import com.galenframework.rainbow4j.ComparisonOptions;
import com.galenframework.rainbow4j.ImageCompareResult;
import com.galenframework.rainbow4j.Rainbow4J;
import com.galenframework.rainbow4j.filters.BlurFilter;

public class FileFlattener implements ActionListener {
	final static Logger				log					= Logger.getLogger(FileFlattener.class);
	// List<File> dstFiles = new LinkedList<>();
	// List<String> dstFiles = new LinkedList<>();
	Set<String>						writtenFilenames	= new HashSet<>();
	JFrame							frame;
	JTextArea						outputArea;
	File							dstRootFolder;
	File							srcRootFolder;
	boolean							aborting;
	int								copies;
	int								visited;
	int								renames;
	private JLabel					iterLbl;
	JTextField						dstWFld;
	JTextField						dstHFld;
	public int						maxImgW;
	public int						maxImgH;
	private long					hash1;
	private long					hash2;
	static byte[]					buf1;
	static byte[]					buf2;
	static ByteBuffer				bufa				= ByteBuffer.allocateDirect(64 * 1024);
	static ByteBuffer				bufb				= ByteBuffer.allocateDirect(64 * 1024);
	static byte[]					buffer;
	// final static int MAX_IMAGE_WIDTH = 1920;
	// final static int MAX_IMAGE_HEIGHT = 19;
	Map<Long, Set<ImageDetails>>	imageDetailsMap		= new HashMap<>();

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
			writtenFilenames.clear();
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
			log.info("max " + maxImgW);
			assertNotEquals(0, maxImgH);
			assertNotEquals(0, maxImgW);

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
		for (File srcFile : srcPath.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String extn = FilenameUtils.getExtension(pathname.getName());
				return pathname.isDirectory() || extn.equalsIgnoreCase("jpg") || extn.equalsIgnoreCase("png");
				// return pathname.isDirectory() || pathname.getName().equalsIgnoreCase("DSCF0369.JPG");
			}
		})) {
			visited++;
			try {
				if (srcFile.isDirectory()) {
					recurseDownFolderAndCopyToDest(srcFile);
				} else {
					if (srcFile.length() > 32768) {
						String srcName = srcFile.getName();
						String newFilename = srcName.toLowerCase();

						ImageDetails imageDetails = new ImageDetails(srcFile, maxImgW, maxImgH);
						// ImageDetails imageDetails2 = new ImageDetails(srcFile, maxImgW, maxImgH);
						// // if (imageDetails.getDigest() != imageDetails2.getDigest()) {
						// // log.error("Unequal digest on same image");
						// // }
						// if (similar(imageDetails.getScaledBufferedImage(), imageDetails2.getScaledBufferedImage())) {
						// log.info("yeah same is same");
						// } else {
						// log.error("Same is different");
						// }
						long digest = imageDetails.getDigest();
						if (imageDetailsMap.containsKey(digest)) {
							log.warn("Clash with digest " + digest);
							Set<ImageDetails> candidates = imageDetailsMap.get(digest);
							for (ImageDetails candidate : candidates) {
								log.info("File may already exist: " + srcName + " as " + candidate.getFile().getName());
								// if (sameContents(imageDetails.getScaledBufferedImage(), candidate.getScaledBufferedImage())) {
								if (similar(imageDetails.getScaledBufferedImage(), candidate.getScaledBufferedImage())) {
									log.warn("File already exists, ignore this");
									continue;
								}
							}
							log.info("OK, it's different");
						}

						File dest = null;

						// What we want to do now is determine the hash of the output image that will be generated
						// after scaling and rotation,
						// then see if the hash already exists. If it does we need to discover what file(s)
						// that relates to so that we can drag those out and do a pixel compare of those.

						if (writtenFilenames.contains(newFilename)) {

							assertTrue(srcFile.isFile());
							File existingDstFile = new File(dstRootFolder + "/" + srcFile.getName());

							String newName;
							String base = FilenameUtils.getBaseName(srcName);

							log.info("Different contents so we have to rename");
							for (int suf = 0;; suf++) {
								newName = base + "-" + suf + "." + "jpg";
								if (!writtenFilenames.contains(newName)) {
									break;
								}
							}

							log.info("Renaming from " + srcName + " to " + newName + ", while copying " + srcFile + " to " + existingDstFile.getPath());
							dest = new File(dstRootFolder.getAbsolutePath() + "/" + newName);
							renames++;
						} else {
							log.info("Copy file " + srcFile.getCanonicalFile() + " to " + dstRootFolder.getAbsolutePath() + "/" + srcFile.getName());
							String base = FilenameUtils.getBaseName(srcName);

							dest = new File(dstRootFolder.getAbsolutePath() + "/" + base + ".jpg");
						}
						assertNotNull(dest);

						// TODO the file name might be .png, we need to change to .jpg

						BufferedImage scaledImage = imageDetails.getScaledBufferedImage();
						if (scaledImage != null) {
							// log.info("Would write " + srcName + " to " + dest.getName());
							// ImageIO.write(scaledImage, "jpg", dest);
							writtenFilenames.add(dest.getName().toLowerCase());
							Set<ImageDetails> existing = imageDetailsMap.get(digest);
							if (existing == null) {
								existing = new HashSet<>();
								imageDetailsMap.put(digest, existing);
							}
							// potentially add another file with the same digest
							existing.add(imageDetails);
							copies++;

							// log.info("copied");
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

	static boolean similar(BufferedImage rba, BufferedImage rbb) {
		try {
			ComparisonOptions options = new ComparisonOptions();
			options.addFilterBoth(new BlurFilter(2));
			ImageCompareResult res = Rainbow4J.compare(rba, rbb, options);
			return !(res.getPercentage() > 2.0);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	boolean sameContents(File f1, File f2) throws IOException {
		BufferedImage srcFileImage = ImageIO.read(f1);
		BufferedImage dstFileImage = ImageIO.read(f2);
		return sameContents(srcFileImage, dstFileImage);
	}

	boolean sameContents(BufferedImage img1, BufferedImage img2) {
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
