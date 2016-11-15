package com.re4ct.fileflatten;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.apache.sanselan.ImageParser;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.formats.jpeg.JpegImageParser;
import org.junit.Test;

import com.google.gson.Gson;

public class Imaging {
	final static Logger	log		= Logger.getLogger(Imaging.class);
	final static int	BUFSIZE	= 1024 * 1024;

	@SuppressWarnings("static-method")
	@Test
	public void test() throws IOException {
		ImageParser[] ips = ImageParser.getAllImageParsers();
		for (ImageParser imageParser : ips) {
			log.info(imageParser.getName() + ": " + imageParser.getClass().getSimpleName());
		}
		ImageParser parser = new JpegImageParser();
		try (InputStream is = new FileInputStream("D:/Flat pics/1.jpg");) {
			byte[] buf = parser.readBytes(is, BUFSIZE);

		} catch (ImageReadException e) {
			log.error("IRE " + e.getCause(), e);
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void pure() throws IOException {
		BufferedImage img1 = ImageIO.read(new File("D:/Flat pics/1.jpg"));
		BufferedImage img2 = ImageIO.read(new File("D:/Flat pics/1-0.jpg"));
		long start = System.currentTimeMillis();
		boolean same = bufferedImagesEqual2(img1, img2);
		long finish = System.currentTimeMillis();
		System.out.println("Compared in " + (finish - start) + "ms");

		start = System.currentTimeMillis();
		same = bufferedImagesEqual(img1, img2);
		finish = System.currentTimeMillis();
		System.out.println("Compared in " + (finish - start) + "ms");

		assertTrue("Images differ", same);
	}

	static boolean bufferedImagesEqual2(BufferedImage img1, BufferedImage img2) {
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

		for (int bank = 0; bank < numBanks; bank++) {
			for (int el = 0; el < numEls; el++) {
				if (img1DB.getElem(bank, el) != img2DB.getElem(bank, el))
					return false;
			}
		}
		return true;
	}

	static boolean bufferedImagesEqual3(BufferedImage img1, BufferedImage img2) {
		DataBuffer db1 = img1.getRaster().getDataBuffer();
		DataBuffer db2 = img1.getRaster().getDataBuffer();
		byte[][] bankDataImg1 = ((DataBufferByte) db1).getBankData();
		byte[][] bankDataImg2 = ((DataBufferByte) db2).getBankData();

		if (bankDataImg1.length != bankDataImg2.length) {
			return false;
		}

		for (int idx = 0; idx < bankDataImg1.length; idx++) {
			byte[] bankRowImg1 = bankDataImg1[idx];
			byte[] bankRowImg2 = bankDataImg2[idx];
			if (!Arrays.equals(bankRowImg1, bankRowImg2)) {
				return false;
			}
		}

		return true;
	}

	static boolean bufferedImagesEqualHead(BufferedImage img1, BufferedImage img2) {
		DataBuffer img1DB = img1.getData().getDataBuffer();
		DataBuffer img2DB = img2.getData().getDataBuffer();

		int numBanks = img1DB.getNumBanks();
		// log.info("Banks " + numBanks);

		assertEquals(numBanks, img2DB.getNumBanks());

		int numEls = img1DB.getSize();
		// log.info("numEls " + numEls);
		assertEquals(numEls, img2DB.getSize());

		int remaining = 1024;
		for (int bank = 0; bank < numBanks; bank++) {
			for (int el = 0; el < numEls; el++) {
				assertEquals(img1DB.getElem(bank, el), img2DB.getElem(bank, el));
				if (--remaining == 0)
					break;
			}
		}
		return true;
	}

	static boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
		if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
			for (int x = 0; x < img1.getWidth(); x++) {
				for (int y = 0; y < img1.getHeight(); y++) {
					if (img1.getRGB(x, y) != img2.getRGB(x, y))
						return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	static boolean sameContents1(File f1, File f2) throws IOException {
		BufferedImage img1 = ImageIO.read(f1);
		BufferedImage img2 = ImageIO.read(f2);
		return bufferedImagesEqual3(img1, img2);
	}

	Map<String, File>	digestMap;
	Map<String, String>	fnameToDigest;

	@Test
	public void dupes() throws NoSuchAlgorithmException, IOException {
		ImageIO.setUseCache(false);
		digestMap = new HashMap<>();

		Gson gson = new Gson();

		try {
			Reader r = new FileReader("fnameToDigest.json");
			fnameToDigest = gson.fromJson(r, HashMap.class);
			if (fnameToDigest == null) {
				fnameToDigest = new HashMap<>();

			}
			r.close();
		} catch (FileNotFoundException e) {
			fnameToDigest = new HashMap<>();
		}

		File root = new File("D:/Flat pics");
		MessageDigest digest = MessageDigest.getInstance("SHA-1");

		int fileNo = 0;
		for (File child : root.listFiles()) {
			// log.info(child);
			if (++fileNo % 100 == 0)
				System.out.println();

			try {
				assertTrue(child.isFile());
				String hash = fnameToDigest.get(child.getName());

				if (hash == null) {
					BufferedImage img1 = ImageIO.read(child);
					if (img1 == null) {
						log.debug("Deleting unsupported file " + child);
						child.delete();

						continue;
					}
					System.out.print('.');

					DataBuffer db = img1.getRaster().getDataBuffer();
					digest.reset();
					byte[][] bankData = ((DataBufferByte) db).getBankData();
					for (byte[] bankRow : bankData) {
						digest.update(bankRow);
					}

					hash = Hex.encodeHexString(digest.digest());

					fnameToDigest.put(child.getName(), hash);

					if (fnameToDigest.size() % 200 == 0) {
						BufferedWriter bw = new BufferedWriter(new FileWriter("fnameToDigest.json"));
						String str = gson.toJson(fnameToDigest);
						bw.write(str);
						bw.close();
						System.out.println("Hashed files " + fnameToDigest.size());
					}
				} else {
					System.out.print('+');
				}

				File existing = digestMap.get(hash);
				if (existing == null) {
					digestMap.put(hash, child);
				} else {
					// log.info(child + " collision with " + existing + " - are they the same?");
					if (sameContents1(child, existing)) {
						// log.warn("Yep, same");
						// we can delete either file. Delete the file we've just picked up would be easier because it leaves the hash
						// pointing to the original
						// deleting the file with the shorter filename should be more user friendly
						if (child.getName().length() < existing.getName().length()) {
							// replace
							log.warn(child + " is dupe, delete " + existing);
							existing.delete();
							// replace the hash
							digestMap.put(hash, child);
						} else {
							// delete latest
							log.warn(existing + " is duped, delete " + child);
							child.delete();
						}
					} else {
						log.error("Nope, different");
						fail("Nope, different");
					}
				}
			} catch (Exception e) {
				log.error("Failed to read pic file " + child);
				// fail("IO");
			}
		}
	}

	@Test
	public void createDigests() throws NoSuchAlgorithmException, IOException {

		ImageIO.setUseCache(false);
		digestMap = new HashMap<>();

		Gson gson = new Gson();

		try {
			Reader r = new FileReader("fnameToDigest.json");
			fnameToDigest = gson.fromJson(r, HashMap.class);
			if (fnameToDigest == null) {
				fnameToDigest = new HashMap<>();

			}
			r.close();
		} catch (FileNotFoundException e) {
			fnameToDigest = new HashMap<>();
		}

		File root = new File("D:/Flat pics");
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		long timeToRead = 0;
		long timeToHash = 0;
		int fileNo = 0;
		for (File child : root.listFiles()) {
			// log.info(child);
			try {
				assertTrue(child.isFile());
				String hash = fnameToDigest.get(child.getName());

				if (hash == null) {
					long start = System.currentTimeMillis();
					BufferedImage img1 = ImageIO.read(child);
					if (img1 == null) {
						System.out.print('-');
						log.debug("Skipping unsupported file " + child);
						continue;
					}
					System.out.print('.');
					// DataBuffer img1DB = img1.getData().getDataBuffer();
					DataBuffer db = img1.getRaster().getDataBuffer();
					long finish = System.currentTimeMillis();
					timeToRead += (finish - start);

					digest.reset();
					byte[][] bankData = ((DataBufferByte) db).getBankData();
					for (byte[] bankRow : bankData) {
						digest.update(bankRow);
					}

					// int numBanks = img1DB.getNumBanks();
					// // log.info("Banks " + numBanks);
					//
					// int numEls = img1DB.getSize();
					// // log.info("numEls " + numEls);
					//
					// start = System.currentTimeMillis();
					// digest.reset();
					// for (int bankNum = 0; bankNum < numBanks; bankNum++) {
					// for (int elNum = 0; elNum < numEls; elNum++) {
					// int el = img1DB.getElem(bankNum, elNum);
					// // System.out.println(el);
					// digest.update((byte) el);
					// }
					// }

					hash = Hex.encodeHexString(digest.digest());
					finish = System.currentTimeMillis();
					timeToHash += (finish - start);
					fnameToDigest.put(child.getName(), hash);

					if (fnameToDigest.size() % 200 == 0) {
						BufferedWriter bw = new BufferedWriter(new FileWriter("fnameToDigest.json"));
						String str = gson.toJson(fnameToDigest);
						bw.write(str);
						bw.close();
						System.out.println("Time to read " + timeToRead + ", time to hash " + timeToHash);
					}
				} else {
					System.out.print('+');
				}
			} catch (Exception e) {
				log.error("Failed to read pic file " + child);
				// fail("IO");
			}
			if (++fileNo % 100 == 0)
				System.out.println();
		}
	}

	@Test
	public void imgRead() throws NoSuchAlgorithmException {
		ImageIO.setUseCache(false);
		digestMap = new HashMap<>();

		File root = new File("D:/Flat pics");

		int fileNo = 0;
		long time = 0;
		for (File child : root.listFiles()) {
			// log.info(child);
			if (fileNo > 200)
				break;
			System.out.print('.');
			if (++fileNo % 100 == 0)
				System.out.println();
			assertTrue(child.isFile());
			time += timeReadFile(child);
		}
		log.error("Total time " + time + "ms");
	}

	@SuppressWarnings("static-method")
	private long timeReadFile(File child) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		long time = 0;
		try {
			assertTrue(child.isFile());
			long start = System.currentTimeMillis();
			// InputStream is = new FileInputStream(child);
			// BufferedInputStream bis = new BufferedInputStream(is);
			// BufferedImage img1 = ImageIO.read(bis);
			BufferedImage img1 = ImageIO.read(child);
			if (img1 == null) {
				log.debug("Skipping unsupported file " + child);
				return 0;
			}
			DataBuffer img1DB = img1.getData().getDataBuffer();
			long finish = System.currentTimeMillis();
			time = finish - start;

			int numBanks = img1DB.getNumBanks();
			// log.info("Banks " + numBanks);

			int numEls = img1DB.getSize();
			// log.info("numEls " + numEls);
			digest.reset();
			for (int bankNum = 0; bankNum < numBanks; bankNum++) {
				for (int elNum = 0; elNum < numEls; elNum++) {
					int el = img1DB.getElem(bankNum, elNum);

					digest.update((byte) (el & 0xFF));
					digest.update((byte) ((el >> 8) & 0xFF));
					digest.update((byte) ((el >> 16) & 0xFF));
					digest.update((byte) ((el >> 24)));
				}
			}
		} catch (Exception e) {
			log.error("Failed to read pic file " + child);
			// fail("IO");
		}
		return time;
	}
}
