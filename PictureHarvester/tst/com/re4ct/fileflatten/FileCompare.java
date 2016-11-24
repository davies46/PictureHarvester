package com.re4ct.fileflatten;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import kanzi.IntTransform;
import kanzi.SliceIntArray;
import kanzi.transform.DCT8;
import kanzi.transform.DWT_CDF_9_7;

public class FileCompare {

	@SuppressWarnings("static-method")
	@Test
	public void test() throws IOException {
		File f1 = new File("R:/pix/01-01 SouthAfrica/dscf0190.jpg");
		assertTrue(f1.exists());
		File f2 = new File("D:/Flat pics/dscf0190.jpg");
		assertTrue(f2.exists());
		File f3 = new File("R:/pix/01-01 SouthAfrica/dscf0192.jpg");
		assertTrue(f2.exists());
		ImageDetails details1 = new ImageDetails(f1, 1024, 1024);
		ImageDetails details2 = new ImageDetails(f2, 1024, 1024);
		ImageDetails details3 = new ImageDetails(f3, 1024, 1024);
		assertEquals(details1.getDigest(), details2.getDigest());
		assertNotEquals(details1.getDigest(), details3.getDigest());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testDct() throws IOException {
		File f1 = new File("D:/Flat pics/thumb.jpg");
		assertTrue(f1.exists());
		ImageDetails details1 = new ImageDetails(f1, 1024, 1024);
		assertNotNull(details1);
		DCT dct = new DCT(20);

		BufferedImage img = ImageIO.read(f1);
		assertNotNull(img);

		int w = img.getWidth();
		assertEquals(16, w);

		int h = img.getHeight();
		assertEquals(16, h);

		byte[] db = ((DataBufferByte) (img.getData().getDataBuffer())).getData();
		assertEquals(16 * 16 * 3, db.length);

		char dctArray1[][] = new char[8][8];
		int dctArray2[][] = new int[8][8];
		int dctArray3[][] = new int[8][8];
		// int dctArray4[][] = new int[8][8];

		int xpos, ypos;
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				// Read in a 8x8 block, starting at (i * 8) -> 8;
				xpos = i * 8;
				ypos = j * 8;

				for (int a = 0; a < 8; a++) {
					for (int b = 0; b < 8; b++) {
						char ch = (char) (db[(xpos + a) * 16 * 3 + (ypos + b) * 3] & 0xFF);
						dctArray1[a][b] = ch;
					}
				}

				// Apply the forward DCT to the block
				dctArray2 = dct.forwardDCT(dctArray1);

				// Quantitize
				dctArray3 = dct.quantitizeImage(dctArray2, false);

				assertNotNull(dctArray3);
			}
		}
	}

	@SuppressWarnings({ "unused", "static-method" })
	@Test
	public void testDct2() throws IOException, InterruptedException {
		SliceIntArray dctA = getDct8("R:/pix/01-01 SouthAfrica/dscf0190.jpg");
		SliceIntArray dctB = getDct8("D:/Flat pics/dscf0190.jpg");
		SliceIntArray dctC = getDct8("R:/pix/01-01 SouthAfrica/dscf0192.jpg");
		long digestA = getDigest(dctA);
		long digestB = getDigest(dctB);
		long digestC = getDigest(dctC);
		System.out.println("Digests " + digestA + "," + digestB + "," + digestC);
	}

	private static long getDigest(SliceIntArray dct) {
		long digest = 0;
		int[] ints = dct.array;
		for (int i = 0; i < 16; i++) {
			digest <<= 1;
			digest = ints[i] > 0 ? 1 : 0;
		}
		return digest;
	}

	private static SliceIntArray getDct8(String path) throws IOException {
		File f1 = new File(path);
		assertTrue(f1.exists());
		BufferedImage img = ImageIO.read(f1);
		assertNotNull(img);

		// int w = img.getWidth();
		// assertEquals(16, w);

		// int h = img.getHeight();
		// assertEquals(16, h);

		BufferedImage bufferedImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_BGR);
		Graphics2D gr = bufferedImage.createGraphics();
		gr.drawImage(img, 0, 0, 8, 8, null);
		gr.dispose();

		System.out.println("Type " + bufferedImage.getType());
		int[] db = ((DataBufferInt) (bufferedImage.getData().getDataBuffer())).getData();
		SliceIntArray sliceIntArray = new SliceIntArray(db, 0);
		int[] dstb = new int[64];
		SliceIntArray dst = new SliceIntArray(dstb, 0);

		DCT8 dct8 = new DCT8();
		dct8.forward(sliceIntArray, dst);

		// System.out.println(dct8);
		return dst;
	}

	@SuppressWarnings({ "unused" })
	@Test
	public void testDwt() throws IOException, InterruptedException {
		DWT_CDF_9_7 dctA = getDwt("R:/pix/01-01 SouthAfrica/dscf0190.jpg");
		DWT_CDF_9_7 dctB = getDwt("D:/Flat pics/dscf0190.jpg");
		System.out.println("Hi");
		// Thread.sleep(10000);
	}

	@SuppressWarnings("static-method")
	private DWT_CDF_9_7 getDwt(String path) throws IOException {
		File f1 = new File(path);
		assertTrue(f1.exists());
		BufferedImage img = ImageIO.read(f1);
		assertNotNull(img);

		// int w = img.getWidth();
		// assertEquals(16, w);

		// int h = img.getHeight();
		// assertEquals(16, h);

		BufferedImage bufferedImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_BGR);
		Graphics2D gr = bufferedImage.createGraphics();
		gr.drawImage(img, 0, 0, 8, 8, null);
		gr.dispose();

		System.out.println("Type " + bufferedImage.getType());
		int[] db = ((DataBufferInt) (bufferedImage.getData().getDataBuffer())).getData();
		SliceIntArray sliceIntArray = new SliceIntArray(db, 0);
		SliceIntArray dst = new SliceIntArray();

		DWT_CDF_9_7 dwt = new DWT_CDF_9_7(8, 8, 48);

		dwt.forward(sliceIntArray, dst);

		// System.out.println(dct8);
		return dwt;
	}

	private static int[] transform(IntTransform transform, int w, int h, int[] rgb, int dimW, int dimH, String title, int xx, int yy, int iter,
			boolean dropSubBands) {
		int len = w * h;
		int[] rgb2 = new int[len];
		int[] data = new int[dimW * dimH];
		long sum = 0L;
		SliceIntArray iia = new SliceIntArray(data, 0);

		for (int ii = 0; ii < iter; ii++) {
			for (int y = 0; y < h; y += dimH) {
				for (int x = 0; x < w; x += dimW) {
					int idx = 0;

					for (int j = y; j < y + dimH; j++) {
						int offs = j * w;

						for (int i = x; i < x + dimW; i++)
							data[idx++] = rgb[offs + i] & 0xFF;
					}

					long before = System.nanoTime();
					iia.index = 0;
					transform.forward(iia, iia);

					if (dropSubBands) {
						for (int j = 0; j < dimH; j++)
							for (int i = 0; i < dimW; i++)
								if ((i >= dimW / 2) || (j >= dimH / 2))
									iia.array[j * dimW + i] = 0;
					}

					iia.index = 0;
					transform.inverse(iia, iia);
					long after = System.nanoTime();
					sum += (after - before);

					idx = 0;

					for (int j = y; j < y + dimH; j++) {
						int offs = j * w;

						for (int i = x; i < x + dimW; i++) {
							int val = (data[idx] >= 255) ? 255 : ((data[idx] <= 0) ? 0 : data[idx]);
							rgb2[offs + i] = (val << 16) | (val << 8) | val;
							idx++;
						}
					}
				}
			}
		}

		// int psnr1024 = new ImageQualityMonitor(w, h).computePSNR(rgb, rgb2);
		// int ssim1024 = new ImageQualityMonitor(w, h).computeSSIM(rgb, rgb2);
		// // System.out.println("PSNR: "+(float) psnr256 / 256);
		// title += " - PSNR: ";
		// title += (psnr1024 < 1024) ? "Infinite" : ((float) psnr1024 / 1024);
		// title += " - SSIM: ";
		// title += ((float) ssim1024 / 1024);
		// System.out.println(title);
		//
		// if (iter > 1)
		// System.out.println("Elapsed time for " + iter + " iterations [ms]: " + sum / 1000000L);
		//
		// java.awt.GraphicsDevice gs = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
		// java.awt.GraphicsConfiguration gc = gs.getDefaultConfiguration();
		// java.awt.image.BufferedImage img = gc.createCompatibleImage(w, h, java.awt.Transparency.OPAQUE);
		// img.getRaster().setDataElements(0, 0, w, h, rgb2);
		// javax.swing.ImageIcon icon = new javax.swing.ImageIcon(img);
		// javax.swing.JFrame frame = new javax.swing.JFrame(title);
		// frame.setBounds(xx, yy, w, h);
		// frame.add(new javax.swing.JLabel(icon));
		// frame.setVisible(true);

		return rgb;
	}

}
