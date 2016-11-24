package com.re4ct.fileflatten;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageDetails {
	final static Logger		log						= Logger.getLogger(ImageDetails.class);

	private File			file;
	final private long		digest;
	final private long		digest2;

	final private int		maxImgW;
	final private int		maxImgH;

	private BufferedImage	scaledBufferedImage		= null;

	private BufferedImage	thumbnailBufferedImage	= null;
	private BufferedImage	thumbnail2BufferedImage	= null;

	final int				THUMB					= 8;
	final int				THUMB2					= 9;
	final int				COLREZ					= 8;
	final int				DIVISOR					= 256 / COLREZ;
	final int[][][]			colormap;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public ImageDetails(File file, int maxImgW, int maxImgH) throws IOException {
		super();
		this.file = file;
		this.maxImgW = maxImgW;
		this.maxImgH = maxImgH;
		if (maxImgH == 0) {
			log.error("WTF");
		}
		colormap = new int[COLREZ][][];
		for (int i = 0; i < COLREZ; i++) {
			colormap[i] = new int[COLREZ][];
			for (int j = 0; j < COLREZ; j++) {
				colormap[i][j] = new int[COLREZ];
			}
		}

		loadThumbnailBufferedImage();
		loadThumbnail2BufferedImage();
		digest = calcDigest(thumbnailBufferedImage, THUMB);
		digest2 = calcDigest(thumbnail2BufferedImage, THUMB2);

		log.debug("Digest " + digest + ", " + digest2);
		// createScaledBufferedImageOpenCV();
	}

	// private long calcDigest() throws IOException {
	// loadThumbnailBufferedImage();
	// return calcDigest(thumbnailBufferedImage);
	// }

	private long calcDigest(BufferedImage bufferedImage, int thumbsize) {
		final int[] data = ((DataBufferInt) bufferedImage.getData().getDataBuffer()).getData();

		DCT dct = new DCT(20);

		long hash = 0;
		int red = 0, green = 0, blue = 0;
		int total = 0;
		log.info("Data len " + data.length);
		// get total colour for each pixel
		for (int i = 0; i < data.length; i++) {
			int d = data[i];
			System.out.print(Integer.toHexString(d) + ",");
			int r = d & 0xFF;
			d >>= 8;
			int g = d & 0xFF;
			d >>= 8;
			int b = d & 0xFF;
			red += r;
			green += g;
			blue += b;
			total = total + r + g + b;
			// colormap[r][g][b]++;
		}

		System.out.println();
		int mean = total / (thumbsize * thumbsize);

		for (int i = 0; i < data.length; i++) {
			int d = data[i];
			int r = d & 0xFF;
			d >>= 8;
			int g = d & 0xFF;
			d >>= 8;
			int b = d & 0xFF;
			boolean above = (r + g + b) > mean;
			hash <<= 1;
			if (above) {
				hash = hash + 1;
			}
		}
		log.info("rgb " + red + "," + green + "," + blue);
		// hash = Crc64.calculateCrc(colormap);
		log.info("Digest " + hash);

		return hash;
	}

	private long calcDigest2() throws IOException {
		loadThumbnailBufferedImage();

		final int[] data = ((DataBufferInt) thumbnailBufferedImage.getData().getDataBuffer()).getData();

		int hash = 0;
		for (int i = 0; i < data.length; i++) {
			int d = data[i];
			int r = (d & 0xFF) / DIVISOR;
			d >>= 8;
			int g = (d & 0xFF) / DIVISOR;
			d >>= 8;
			int b = (d & 0xFF) / DIVISOR;
			colormap[r][g][b]++;
		}
		log.info("Digest2 " + hash);

		return hash;

	}

	private void createScaledBufferedImageOpenCV() throws IOException {
		BufferedImage srcFileImage = ImageIO.read(file);

		DataBuffer buffer = srcFileImage.getRaster().getDataBuffer();
		log.error("Data " + buffer.getClass().getSimpleName());
		byte[] pixels = ((DataBufferByte) buffer).getData();
		Mat matImg = new Mat(srcFileImage.getHeight(), srcFileImage.getWidth(), CvType.CV_8UC3);
		matImg.put(0, 0, pixels);

		Size dsize = new Size(THUMB, THUMB);
		Mat thumb = new Mat(THUMB, THUMB, CvType.CV_8UC3);
		Imgproc.resize(matImg, thumb, dsize);

		scaledBufferedImage = new BufferedImage(THUMB, THUMB, BufferedImage.TYPE_BYTE_BINARY);
		byte[] thumbPixels = ((DataBufferByte) scaledBufferedImage.getRaster().getDataBuffer()).getData();
		float[][] colours = new float[THUMB][];
		for (int i = 0; i < THUMB; i++) {
			colours[i] = new float[THUMB];
		}

		thumb.get(0, 0, thumbPixels);

		Mat dct = new Mat(THUMB, THUMB, CvType.CV_32FC1);

		org.opencv.core.Core.dct(thumb, dct);

		// GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		// GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		// dst.

	}

	public BufferedImage loadScaledBufferedImage() throws IOException {
		if (scaledBufferedImage == null) {
			scaledBufferedImage = loadScaledImage(maxImgW, maxImgH);
		}
		return scaledBufferedImage;
	}

	public BufferedImage loadThumbnailBufferedImage() throws IOException {
		if (thumbnailBufferedImage == null) {
			thumbnailBufferedImage = loadScaledImageIgnoreAspect(THUMB, THUMB);
		}
		return thumbnailBufferedImage;
	}

	public BufferedImage loadThumbnail2BufferedImage() throws IOException {
		if (thumbnail2BufferedImage == null) {
			thumbnail2BufferedImage = loadScaledImageIgnoreAspect(THUMB2, THUMB2);
		}
		return thumbnail2BufferedImage;
	}

	private BufferedImage loadScaledImage(int maxW, int maxH) throws IOException {
		BufferedImage finalImage = null;
		BufferedImage srcFileImage = ImageIO.read(file);
		if (srcFileImage == null) {
			throw new IOException("Bad file");
		}
		int rawW = srcFileImage.getWidth();
		int rawH = srcFileImage.getHeight();
		double underWidth = 1.0 * maxW / rawW;
		double underHeight = 1.0 * maxH / rawH;
		// if it's overwidth by more than overheight then scale according to overwidth
		// if it's under both, same
		double scale = (underWidth < underHeight) ? underWidth : underHeight;
		int newW = (int) (rawW * scale);
		int newH = (int) (rawH * scale);

		if (newH == 0 || newW == 0) {
			log.error("Bad pic size for " + file.getName());
			log.error("Max " + maxW + "," + maxH);
		} else {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

			finalImage = gc.createCompatibleImage(newW, newH);// new BufferedImage(newW, newH,
																// BufferedImage.TYPE_INT_ARGB);

			Graphics2D gr = finalImage.createGraphics();
			gr.drawImage(srcFileImage, 0, 0, newW, newH, null);
			gr.dispose();
		}
		return finalImage;
	}

	private BufferedImage loadScaledImageIgnoreAspect(int maxW, int maxH) throws IOException {
		BufferedImage finalImage = null;
		BufferedImage srcFileImage = ImageIO.read(file);
		if (srcFileImage == null) {
			throw new IOException("Bad file");
		}

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		finalImage = gc.createCompatibleImage(maxW, maxH);// new BufferedImage(newW, newH,
															// BufferedImage.TYPE_INT_ARGB);

		Graphics2D gr = finalImage.createGraphics();
		gr.drawImage(srcFileImage, 0, 0, maxW, maxH, null);
		gr.dispose();

		return finalImage;
	}

	public long getDigest() {
		return digest;
	}

	public boolean isFile() {
		return file.isFile();
	}

	public File getFile() {
		return file;
	}

	public void discardImage() {
		scaledBufferedImage = null;
	}

	public boolean imageDiscarded() {
		return scaledBufferedImage == null;
	}

	public long getDigest2() {
		return digest2;
	}
}
