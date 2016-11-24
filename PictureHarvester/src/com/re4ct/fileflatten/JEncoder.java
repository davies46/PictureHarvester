package com.re4ct.fileflatten;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;

//import GrafixTools.*;

public class JEncoder extends Frame {
	Image	image;
	int		outputArray[][]	= new int[320][200];	// This is the image, 0-255

	// GrafixTools GT;
	DCT		dctTrans;

	public JEncoder() {
		setLocation(100, 10); // move(100,00);
		setSize(320, 240); // resize(320,240);
		repaint();

		// Load in an image
		image = Toolkit.getDefaultToolkit().getImage("steve.jpg");

		// Make sure it gets loaded
		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(image, 0);
		System.out.println("Initialized tracker.");
		try {
			System.out.println("Waiting for tracker...");
			tracker.waitForID(0);
		} catch (Exception e) {
			return;
		}
		System.out.println("Loaded image.");

		// GT = new GrafixTools(image);
		// System.out.println("Dimensions: " + GT.imageHeight + "," + GT.imageWidth);

		// System.out.println("Getting array information");
		// outputArray = GT.getRedArray();

		System.out.println("Initializing DCT");

		dctTrans = new DCT(20);
		System.out.println("Done initializing DCT");

		compressImage();
	}

	public void compressImage() {
		int i = 0;
		int j = 0;
		int a = 0;
		int b = 0;
		int x = 0;
		int y = 0;
		int counter = 0;

		int temp1 = 0, temp2 = 0, count = 0;

		int xpos;
		int ypos;

		char dctArray1[][] = new char[8][8];
		int dctArray2[][] = new int[8][8];
		int dctArray3[][] = new int[8][8];
		int dctArray4[][] = new int[8][8];

		int reconstImage[][] = new int[320][240];

		System.out.println("Initializing compression - DCT & Qualitization");

		for (i = 0; i < 40; i++) {
			for (j = 0; j < 30; j++) {
				// Read in a 8x8 block, starting at (i * 8) -> 8;
				xpos = i * 8;
				ypos = j * 8;

				for (a = 0; a < 8; a++) {
					for (b = 0; b < 8; b++) {
						dctArray1[a][b] = (char) outputArray[xpos + a][ypos + b];
					}
				}

				// Apply the forward DCT to the block
				dctArray2 = dctTrans.forwardDCT(dctArray1);

				// Quantitize
				dctArray3 = dctTrans.quantitizeImage(dctArray2, false);

				// Reconstruct the compressed data in an image...
				for (a = 0; a < 8; a++) {
					for (b = 0; b < 8; b++) {
						reconstImage[xpos + a][ypos + b] = dctArray3[a][b];
					}
				}

				counter++;
				System.out.print("Blocks: ");
				System.out.print(counter + "\r");
			}
		}

		// Test RLE Encoding

		int one[] = new int[320 * 240];
		System.out.println("Array Conversion & RLE Start");

		int r = 0;
		int s = 0;
		int index = 0;

		for (s = 0; s < 320; s++) {
			for (r = 0; r < 240; r++) {
				one[index] = reconstImage[s][r];
				index++;
			}

		}

		// Run the compressor
		int compressed[] = new int[320 * 240];
		compressed = dctTrans.compressImage(one, true);
		System.out.println();

		// Run the decompressor
		int decompressed[] = new int[320 * 240];
		decompressed = dctTrans.decompressImage(compressed, true);

		if (one != decompressed) {
			System.out.println("Compression error");
		}

		System.out.println("Converting decompressed image array to 2D");

		index = 0;

		for (s = 0; s < 320; s++) {
			for (r = 0; r < 240; r++) {
				reconstImage[s][r] = decompressed[index];
				index++;
			}

		}

		/*
		 * // Figure how just how good we are int nonzero = 0; for (i=0; i<320; i++) { for (j=0; j<240; j++) { if (reconstImage[i][j] != 0)
		 * { nonzero++; System.out.print("Signifigant Pixels: " + nonzero); System.out.print("\r"); } } } System.out.println(); double cr;
		 * cr = ((double)nonzero / (double)(320*240)) * 100; System.out.println("Tossed percentage: " + (100.0 - cr) + "%");
		 */
		System.out.println("Initializing decompression - DeQualitization & Inverse DCT");
		counter = 0;

		for (i = 0; i < 40; i++) {
			for (j = 0; j < 30; j++) {
				// Read in a 8x8 block, starting at (i * 8) -> 8;
				xpos = i * 8;
				ypos = j * 8;

				for (a = 0; a < 8; a++) {
					for (b = 0; b < 8; b++) {
						dctArray2[a][b] = reconstImage[xpos + a][ypos + b];
					}
				}

				// Run the dequantitizer
				dctArray3 = dctTrans.dequantitizeImage(dctArray2, false);

				// Run the inverse DCT
				dctArray4 = dctTrans.inverseDCT(dctArray3);

				// Overwrite with a new reconstructed image
				for (a = 0; a < 8; a++) {
					for (b = 0; b < 8; b++) {
						reconstImage[xpos + a][ypos + b] = dctArray4[a][b];
					}
				}

				counter++;
				System.out.print("Blocks: ");
				System.out.print(counter + "\r");
			}
		}

		System.out.println();
		System.out.println("Constructing the image..");

		makeImage(reconstImage);

		System.out.println("Hit enter");
		try {
			System.in.read();
		} catch (Exception e) {
		}
		System.exit(0);

	}

	public void makeImage(int[][] image) {
		int i;
		int j;
		int k = 0;

		int one[] = new int[320 * 240];

		System.out.println("Calling conversion");
		// one = GT.convertGrayToArray(image, true);
		// one = GT.convertGrayToArray(GlobalRed, true);

		System.out.println();
		System.out.println("Completed one dimensional array conversion");

		ImageWindow iw = new ImageWindow(one);
	}

	/************** Main Method ****************/
	public static void main(String args[]) {
		try {
			new JEncoder();
		} catch (Exception e) {
		}
	}
}

class ImageWindow extends Frame {
	Image	image;
	Image	image2;
	int		imageArray_[]	= new int[320 * 240];

	public ImageWindow(int[] imageArray) {
		super("DCT Window");
		imageArray_ = imageArray;
		setLocation(100, 100); // move(100,100);
		setSize(700, 300); // resize(700,300);

		System.out.println("Calculating and displaying image..");
		image = this.createImage(new MemoryImageSource(320, 240, imageArray_, 0, 320));

		// Make sure it gets loaded
		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(image, 1);
		try {
			tracker.waitForID(1);
		} catch (Exception e) {
			return;
		}

		// Load in original image
		image2 = Toolkit.getDefaultToolkit().getImage("steve.jpg");
		tracker = new MediaTracker(this);
		tracker.addImage(image, 0);
		try {
			tracker.waitForID(0);
		} catch (Exception e) {
			return;
		}

		this.show();
	}

	public void paint(Graphics g) {
		g.drawImage(image2, 0, 20, this);
		g.drawImage(image, 350, 20, this);
	}
}