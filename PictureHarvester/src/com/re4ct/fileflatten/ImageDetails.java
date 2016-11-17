package com.re4ct.fileflatten;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public class ImageDetails {
	final static Logger		log			= Logger.getLogger(ImageDetails.class);

	private File			file;
	private long			digest;

	final private int		maxImgW;
	final private int		maxImgH;

	private BufferedImage	scaledBufferedImage;
	private static int		computed	= 0;

	final int				COLREZ		= 16;
	final int				DIVIDER		= 256 / COLREZ;
	final int[][][]			colormap;

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
		calcDigest();
	}

	private long calcDigest() throws IOException {

		BufferedImage srcFileImage = ImageIO.read(file);
		// DataBuffer img1DB = srcFileImage.getData().getDataBuffer();

		if (srcFileImage == null) {
			log.error("Bad file " + file.getCanonicalFile());
			return 0;
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

			scaledBufferedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = scaledBufferedImage.createGraphics();
			gr.drawImage(srcFileImage, 0, 0, newW, newH, null);

			final int[] data = ((DataBufferInt) scaledBufferedImage.getData().getDataBuffer()).getData();

			digest = 0;
			for (int i = 0; i < data.length; i++) {
				int d = data[i];
				int r = d % 256 / DIVIDER;
				d >>= 8;
				int g = d % 256 / DIVIDER;
				d >>= 8;
				int b = d % 256 / DIVIDER;
				colormap[r][g][b]++;
			}
			digest = Crc64.calculateCrc(colormap);
			log.info("Digest " + digest);
			// Spectrum sp = Rainbow4J.readSpectrum(scaledBufferedImage, 250);
			// List<ColorDistribution> cds = sp.getColorDistribution(1);// min %
			// long total = 0;
			// for (ColorDistribution cd : cds) {
			// log.info("CD " + cd.getPercentage());
			// total *= 100;
			// total += (int) cd.getPercentage();
			// }
			// digest = total;
			// log.info("Digest " + digest);

			//
			// DataBuffer img1DB = scaledBufferedImage.getData().getDataBuffer();
			//
			// digest = Crc64.calculateCrc(img1DB);
			computed++;
		}
		return digest;

	}

	// private void calcDigest(DataBuffer img1DB) {
	// int numBanks = img1DB.getNumBanks();
	//
	// int numEls = img1DB.getSize();
	// digest = 0;
	// for (int bank = 0; bank < numBanks; bank++) {
	// for (int el = 0; el < numEls; el++) {
	// long el1 = img1DB.getElem(bank, el);
	// digest += el1;
	// digest = digest << 3 | digest >> (64 - 3);
	// digest ^= -1;
	// }
	// }
	// if (digest == 0) {
	// log.error("Digest 0 after computing " + computed);
	// }
	// computed++;
	// }

	public BufferedImage getScaledBufferedImage() {
		return scaledBufferedImage;
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
}
