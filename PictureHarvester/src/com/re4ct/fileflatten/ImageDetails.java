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

	final private int		maxImgW;
	final private int		maxImgH;

	private BufferedImage	scaledBufferedImage		= null;

	private BufferedImage	thumbnailBufferedImage	= null;

	private static int		computed				= 0;

	final int				THUMB					= 16;
	final int				COLREZ					= 8;
	final int				DIVIDER					= 256 / COLREZ;
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
		digest = calcDigest();
		log.debug("Digest " + digest);
	}

	private long calcDigest() throws IOException {

		// BufferedImage srcFileImage = ImageIO.read(file);
		// // DataBuffer img1DB = srcFileImage.getData().getDataBuffer();
		//
		// if (srcFileImage == null) {
		// log.error("Bad file " + file.getCanonicalFile());
		// return 0;
		// }

		getScaledBufferedImage();
		getThumbnailBufferedImage();

		final int[] data = ((DataBufferInt) thumbnailBufferedImage.getData().getDataBuffer()).getData();

		// Crc64 crc64 = new Crc64();
		// Spectrum spectrum = Rainbow4J.readSpectrum(thumbnailBufferedImage, 250);
		// List<ColorDistribution> dist = spectrum.getColorDistribution(1);
		// for (ColorDistribution cd : dist) {
		// int rgb = cd.getColor().getRGB();
		// double pc = cd.getPercentage();
		// crc64.add(rgb, pc);
		// }
		// computed++;
		// return crc64.getCrc();
		long hash = 0;
		log.info("Data len " + data.length);
		for (int i = 0; i < data.length; i++) {
			int d = data[i];
			System.out.print(d + " ");
			hash <<= 8;
			hash += d;
			int r = (256 + d % 256) / 2 / DIVIDER;
			d >>= 8;
			int g = (256 + d % 256) / 2 / DIVIDER;
			d >>= 8;
			int b = (256 + d % 256) / 2 / DIVIDER;
			colormap[r][g][b]++;
		}
		hash = Crc64.calculateCrc(colormap);
		log.info("Digest " + hash);

		return hash;

	}

	private void createScaledBufferedImage() throws IOException {
		BufferedImage srcFileImage = ImageIO.read(file);
		if (srcFileImage == null) {
			throw new IOException("Bad file");
		}
		int rawW = srcFileImage.getWidth();
		int rawH = srcFileImage.getHeight();
		double underWidth = 1.0 * maxImgW / rawW;
		double underHeight = 1.0 * maxImgH / rawH;
		// if it's overwidth by more than overheight then scale according to overwidth
		// if it's under both, same
		double scale = (underWidth < underHeight) ? underWidth : underHeight;
		int newW = (int) (rawW * scale);
		int newH = (int) (rawH * scale);

		if (newH == 0 || newW == 0) {
			log.error("Bad pic size for " + file.getName());
			log.error("Max " + maxImgW + "," + maxImgH);
		} else {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

			scaledBufferedImage = gc.createCompatibleImage(newW, newH);// new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

			Graphics2D gr = scaledBufferedImage.createGraphics();
			gr.drawImage(srcFileImage, 0, 0, newW, newH, null);
			gr.dispose();
		}
	}

	private void createThumbnailBufferedImage() throws IOException {
		BufferedImage srcFileImage = ImageIO.read(file);
		if (srcFileImage == null) {
			throw new IOException("Bad file");
		}
		int rawW = srcFileImage.getWidth();
		int rawH = srcFileImage.getHeight();
		double underWidth = 1.0 * THUMB / rawW;
		double underHeight = 1.0 * THUMB / rawH;
		// if it's overwidth by more than overheight then scale according to overwidth
		// if it's under both, same
		double scale = (underWidth < underHeight) ? underWidth : underHeight;
		int newW = (int) (rawW * scale);
		int newH = (int) (rawH * scale);

		if (newH == 0 || newW == 0) {
			log.error("Bad pic size for " + file.getName());
			log.error("Max " + maxImgW + "," + maxImgH);
		} else {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

			thumbnailBufferedImage = gc.createCompatibleImage(newW, newH);// new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

			Graphics2D gr = thumbnailBufferedImage.createGraphics();
			gr.drawImage(srcFileImage, 0, 0, newW, newH, null);
			gr.dispose();
		}
	}

	private void createScaledBufferedImageOpenCV() throws IOException {
		BufferedImage srcFileImage = ImageIO.read(file);

		DataBuffer buffer = srcFileImage.getRaster().getDataBuffer();
		log.error("Data " + buffer.getClass().getSimpleName());
		byte[] pixels = ((DataBufferByte) buffer).getData();
		Mat matImg = new Mat(srcFileImage.getHeight(), srcFileImage.getWidth(), CvType.CV_8UC3);
		matImg.put(0, 0, pixels);

		int rawW = srcFileImage.getWidth();
		int rawH = srcFileImage.getHeight();
		double underWidth = 1.0 * maxImgW / rawW;
		double underHeight = 1.0 * maxImgH / rawH;
		// if it's overwidth by more than overheight then scale according to overwidth
		// if it's under both, same
		double scale = (underWidth < underHeight) ? underWidth : underHeight;
		int newW = (int) (rawW * scale);
		int newH = (int) (rawH * scale);

		Size dsize = new Size(newH, newW);
		Mat dst = new Mat(newH, newW, CvType.CV_8UC3);
		Imgproc.resize(matImg, dst, dsize);

		// GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		// GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		scaledBufferedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_BINARY);

		int[] dstPixels = ((DataBufferInt) scaledBufferedImage.getRaster().getDataBuffer()).getData();

		dst.get(0, 0, dstPixels);
		// dst.

	}

	public BufferedImage getScaledBufferedImage() {
		if (scaledBufferedImage == null) {
			try {
				createScaledBufferedImage();
				// createScaledBufferedImageOpenCV();
			} catch (IOException e) {
				return null;
			}
		}
		return scaledBufferedImage;
	}

	public BufferedImage getThumbnailBufferedImage() {
		if (thumbnailBufferedImage == null) {
			try {
				createThumbnailBufferedImage();
			} catch (IOException e) {
				return null;
			}
		}
		return thumbnailBufferedImage;
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
}
