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
	final static Logger	log				= Logger.getLogger(CopyManager.class);
	File				dstRootFolder;
	File				srcRootFolder;
	Set<String>			destFilenames	= new HashSet<>();
	public int			maxImgW;
	public int			maxImgH;
	ImageDetailsMap[]	imageDetailsMap	= new ImageDetailsMap[3];
	// ImageDetailsMap imageDetailsMap2 = new ImageDetailsMap();
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

	public void doCopy(String src, String dst) {

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
			visualCallbacks.setIterLbl("Finished");
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
		for (int c = 0; c < 3; c++) {
			imageDetailsMap[c] = new ImageDetailsMap();
		}
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
																	long[] digest = imageDetails.getDigest();
																	for (int c = 0; c < 3; c++) {
																		imageDetailsMap[c].put(digest[c], imageDetails);
																	}
																	// imageDetailsMap2.put(imageDetails.getDigest2(), imageDetails);

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
				long[] digest = details.getDigest();
				for (int c = 0; c < 3; c++) {
					imageDetailsMap[c].put(digest[c], details);
				}
				// imageDetailsMap2.put(details.getDigest2(), details);
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
			log.info("[[" + srcFile.getName() + "]]");
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

			ImageDetails imageDetails = new ImageDetails(srcFile, maxImgW, maxImgH);
			long[] digest = imageDetails.getDigest();
			// long digest2 = imageDetails.getDigest2();
			// boolean same=false;
			for (int c = 0; c < 3; c++) {
				if (imageDetailsMap[c].has(digest[c])) {
					log.warn("Clash with digest " + digest[c]);
					Set<ImageDetails> candidates = imageDetailsMap[c].get(digest[c]);
					if (checkCollisions(srcFile, candidates, imageDetails))
						return;
					log.info("They're all different");
					break;
				}
			}

			{
				log.info("Digest not found");
				// problem is, it might be a digest miss, so we should do a different digest check
			}

			doCopy(imageDetails, srcFile);
		}

	}

	private boolean checkCollisions(File srcFile, Set<ImageDetails> candidates, ImageDetails imageDetails) throws IOException {
		for (ImageDetails candidate : candidates) {
			log.info("File may already exist: " + srcFile.getAbsolutePath() + " as " + candidate.getFile().getName());
			// if (sameContents(imageDetails.getScaledBufferedImage(), candidate.getScaledBufferedImage())) {
			boolean similar = similar(imageDetails.loadScaledBufferedImage(), candidate.loadScaledBufferedImage());
			long s1 = imageDetails.getSimpleDigest();
			long s2 = candidate.getSimpleDigest();
			long sameness = s1 ^ s2;
			int sameBits = 64 - Long.bitCount(sameness);
			boolean sameByDigest = s1 == s2;
			int greyDif = imageDetails.compareGreyscale(candidate);
			if (sameByDigest != similar) {
				if (sameByDigest) {
					log.error("Same by simple digest but not similar, grey dif " + greyDif);
				} else {
					log.error("Similar but different by simple digest, grey dif " + greyDif);
				}
				visualCallbacks.visualImageCompare(imageDetails.loadThumbnailBufferedImage(), candidate.loadThumbnailBufferedImage());
				visualCallbacks.setMsg(candidate.getFile().getName() + ", same " + sameBits + ", grey dif " + greyDif);
			}
			if (similar || sameBits > 59 || greyDif < 4000) {
				if (srcFile.getName().equalsIgnoreCase(candidate.getFile().getName())) {
					log.warn("File does already exist");
				} else {
					log.error("File does already exist");
				}
				if (srcFile.length() > candidate.getFile().length()) {
					// log.warn("Src file " + srcFile.length() + " bigger than " + candidate.getFile().length() + " so overwrite with
					// bigger");
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
			long[] existingFileDigest = dst.getDigest();
			for (int c = 0; c < 3; c++) {
				log.error("src " + Long.toBinaryString(imageDetails.getDigest()[c]) + ", dst " + Long.toBinaryString(existingFileDigest[c]) + " : " + srcName
						+ "->" + dst.getFile().getName());
			}
			visualCallbacks.visualImageCompare(imageDetails.loadThumbnailBufferedImage(), dst.loadThumbnailBufferedImage());
			// aborting = true;

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
		int discards = 0;
		int keeps = 0;
		for (int c = 0; c < 3; c++) {

			log.warn("Filenames " + destFilenames.size() + ", map " + imageDetailsMap[c].size());
			for (Set<ImageDetails> e : imageDetailsMap[c].values()) {
				for (ImageDetails id : e) {
					if (id.imageDiscarded()) {
						discards++;
					} else {
						keeps++;
					}
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
			options.setTolerance(10);

			ImageCompareResult res = Rainbow4J.compare(rba, rbb, options);
			double diffPercentage = res.getPercentage();
			boolean same = (diffPercentage < 10.0);
			return same;

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
