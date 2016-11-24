package com.re4ct.fileflatten;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.galenframework.rainbow4j.ComparisonOptions;
import com.galenframework.rainbow4j.ImageCompareResult;
import com.galenframework.rainbow4j.Rainbow4J;
import com.galenframework.rainbow4j.filters.BlurFilter;

public class CopyManager {
	final static Logger	log					= Logger.getLogger(CopyManager.class);
	File				dstRootFolder;
	File				srcRootFolder;
	Set<String>			destFilenames		= new HashSet<>();
	public int			maxImgW;
	public int			maxImgH;
	ImageDetailsMap		imageDetailsMap		= new ImageDetailsMap();
	ImageDetailsMap		imageDetailsMap2	= new ImageDetailsMap();
	// ImageDetails firstImage = null;
	private long		hash1;
	private long		hash2;
	int					visited;
	int					copies;
	int					renames;
	boolean				aborting;
	VisualCallbacks		visualCallbacks;

	class CopyRequest {
		ImageDetails	imageDetails;
		File			dest;

		public CopyRequest(ImageDetails imageDetails, File dest) {
			super();
			this.imageDetails = imageDetails;
			this.dest = dest;
		}

	}

	final BlockingDeque<CopyRequest> copyRequests = new LinkedBlockingDeque<>();

	public void doCopy(String src, String dst, int maxImgW, int maxImgH) {

		dstRootFolder = new File(dst);
		assertTrue("Dst must be folder", dstRootFolder.isDirectory());

		srcRootFolder = new File(src);
		assertTrue("Src must be folder", srcRootFolder.isDirectory());

		Thread thread = new Thread(new Copyer());
		thread.start();
	}

	class Copyer implements Runnable {
		@Override
		public void run() {
			log.info("Start");
			destFilenames.clear();
			log.info("max " + maxImgW);
			assertNotEquals(0, maxImgH);
			assertNotEquals(0, maxImgW);
			long start = System.currentTimeMillis();

			loadDestImages();
			recurseDownFolderAndCopyToDest(srcRootFolder);

			long finish = System.currentTimeMillis();
			long time = finish - start;
			log.info("Time taken " + time + "ms");
			log.info("Copied " + copies + " files");
			log.info("Renamed " + renames + " files");
		}
	}

	public CopyManager(VisualCallbacks visualCallbacks, int maxW, int maxH) {
		copies = renames = visited = 0;
		this.visualCallbacks = visualCallbacks;
		aborting = false;
		maxImgW = maxW;
		maxImgH = maxH;
		new Thread(copyThread).start();
	}

	Runnable					copyThread	= new Runnable() {
												@Override
												public void run() {
													log.error("Copy runner started");
													for (;;) {
														CopyRequest copyRequest;
														try {
															log.debug("Waiting for copy request");
															copyRequest = copyRequests.takeFirst();
															log.debug("Woken by copy request, " + copyRequests.size() + " queued");
															ImageDetails imageDetails = copyRequest.imageDetails;
															File dest = copyRequest.dest;
															try {
																BufferedImage scaledImage = imageDetails.loadScaledBufferedImage();
																if (scaledImage == null) {
																	log.error("Scaled image is null");
																} else {
																	try {
																		ImageIO.write(scaledImage, "jpg", dest);
																	} catch (IOException e) {
																		log.error("Failed to write image " + dest, e);
																	}
																	destFilenames.add(dest.getName().toLowerCase());
																	imageDetailsMap.put(imageDetails.getDigest(), imageDetails);
																	imageDetailsMap2.put(imageDetails.getDigest2(), imageDetails);

																	// potentially add another file with the same digest
																	imageDetails.discardImage();
																	copies++;
																}
															} catch (IOException e) {
																log.error("IOExc", e);
															}
														} catch (InterruptedException e1) {
															log.error("Failed to write image to file", e1);
														}
													}
												}
											};

	final private FileFilter	filter		= new FileFilter() {
												@Override
												public boolean accept(File pathname) {
													String extn = FilenameUtils.getExtension(pathname.getName());
													return pathname.isDirectory() || extn.equalsIgnoreCase("jpg") || extn.equalsIgnoreCase("png");
												}
											};

	public void loadDestImages() {
		for (File child : dstRootFolder.listFiles()) {
			assertTrue("Dest folder should not contain sub-folders", child.isFile());
			assertFalse("Dest folder should not contain sub-folders", child.isDirectory());
			String dstPath = child.getName();
			log.info(dstPath);
			destFilenames.add(dstPath);
			try {
				ImageDetails details = new ImageDetails(child, maxImgW, maxImgH);
				imageDetailsMap.put(details.getDigest(), details);
				imageDetailsMap2.put(details.getDigest2(), details);
				details.discardImage();
			} catch (IOException e) {
				log.error("IOExc, skipt to next file");
			}
			// dstFiles.add(dstPath);
		}
	}

	protected void recurseDownFolderAndCopyToDest(File srcPath) {
		if (aborting)
			return;
		log.info("<<" + srcPath.getName() + ">>");

		for (File srcFile : srcPath.listFiles(filter)) {
			visited++;
			try {
				if (srcFile.isDirectory()) {
					recurseDownFolderAndCopyToDest(srcFile);
				} else {
					checkFileAndCopy(srcFile);
				}
				if (visited % 10 == 0) {
					visualCallbacks.setIterLbl("Total visited " + visited + ", copied " + (renames + copies));
					if (visited % 200 == 0) {
						takeStock();
					}
				}
				// if (renames > 3)
				// break;
			} catch (Exception e) {
				log.error("Exc", e);
			}
		}
	}

	private void checkFileAndCopy(File srcFile) throws IOException {
		if (srcFile.length() > 32768) {
			String srcName = srcFile.getName();

			ImageDetails imageDetails = new ImageDetails(srcFile, maxImgW, maxImgH);
			long digest = imageDetails.getDigest();
			long digest2 = imageDetails.getDigest2();

			if (imageDetailsMap.has(digest)) {
				log.warn("Clash with digest " + digest);
				Set<ImageDetails> candidates = imageDetailsMap.get(digest);
				if (checkCollisions(srcFile, candidates, srcName, imageDetails))
					return;
				log.info("OK, it's different");
			} else if (imageDetailsMap2.has(digest2)) {
				log.warn("Clash with digest2 " + digest);
				Set<ImageDetails> candidates = imageDetailsMap2.get(digest2);
				if (checkCollisions(srcFile, candidates, srcName, imageDetails))
					return;
			} else {
				log.info("Digest not found");
				// problem is, it might be a digest miss, so we should do a different digest check
			}

			doCopy(imageDetails, srcFile);
		}

	}

	private static boolean checkCollisions(File srcFile, Set<ImageDetails> candidates, String srcName, ImageDetails imageDetails) throws IOException {
		for (ImageDetails candidate : candidates) {
			log.info("File may already exist: " + srcName + " as " + candidate.getFile().getName());
			// if (sameContents(imageDetails.getScaledBufferedImage(), candidate.getScaledBufferedImage())) {
			if (similar(imageDetails.loadScaledBufferedImage(), candidate.loadScaledBufferedImage())) {
				log.warn("File already exists");
				if (srcFile.length() > candidate.getFile().length()) {
					log.warn("Src file " + srcFile.length() + " bigger than " + candidate.getFile().length() + " so overwrite with bigger");
				} else {
					log.info("ignore");
				}
				// just return here, don't do any copy
				return true;
			}
		}
		return false;
	}

	private void doCopy(ImageDetails imageDetails, File srcFile) throws IOException {
		// What we want to do now is determine the hash of the output image that will be generated
		// after scaling and rotation,
		// then see if the hash already exists. If it does we need to discover what file(s)
		// that relates to so that we can drag those out and do a pixel compare of those.
		String srcName = srcFile.getName();
		String newFilename = srcName.toLowerCase();
		File dest = null;

		if (destFilenames.contains(newFilename)) {
			assertTrue(srcFile.isFile());
			String dstFileName = dstRootFolder + "/" + srcFile.getName();
			File existingDstFile = new File(dstFileName);

			ImageDetails dst = new ImageDetails(existingDstFile, maxImgW, maxImgH);
			long existingFileDigest = dst.getDigest();
			log.error("src " + imageDetails.getDigest() + ", dst " + existingFileDigest + " : " + srcName);
			System.exit(0);
			visualCallbacks.visualImageCompare(srcFile.getAbsolutePath(), dstFileName);

			String base = FilenameUtils.getBaseName(srcName);
			String baseExtn = FilenameUtils.getExtension(srcName);
			String newName = base;

			assertEquals(srcName, base + "." + baseExtn);

			log.info("Same name different contents, so we have to rename");
			for (int suf = 0;; suf++) {
				newName = base + "-" + suf + "." + "jpg";
				if (!destFilenames.contains(newName)) {
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
		bgCopy(imageDetails, dest);
	}

	private void takeStock() {
		log.warn("Filenames " + destFilenames.size() + ", map " + imageDetailsMap.size());
		int discards = 0;
		int keeps = 0;
		for (Set<ImageDetails> e : imageDetailsMap.values()) {
			for (ImageDetails id : e) {
				if (id.imageDiscarded()) {
					discards++;
				} else {
					keeps++;
				}
			}
		}
		log.warn("discarded " + discards + ", keeps " + keeps);
	}

	private void bgCopy(ImageDetails imageDetails, File dest) {
		copyRequests.add(new CopyRequest(imageDetails, dest));
	}

	static boolean similar(BufferedImage rba, BufferedImage rbb) {
		try {
			ComparisonOptions options = new ComparisonOptions();
			options.addFilterBoth(new BlurFilter(5));
			ImageCompareResult res = Rainbow4J.compare(rba, rbb, options);
			return (res.getPercentage() > 50.0);

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

}
