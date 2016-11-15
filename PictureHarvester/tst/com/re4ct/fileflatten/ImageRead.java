package com.re4ct.fileflatten;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sanselan.ImageParser;
import org.apache.sanselan.ImageReadException;

class ImageReadExample {
	public static BufferedImage imageReadExample(final File file)
            throws ImageReadException, IOException {
        final Map<String, Object> params = new HashMap<>();

        
        // set optional parameters if you like
		// params.put(SanselanConstants.BUFFERED_IMAGE_FACTORY,
		// new ManagedImageBufferedImageFactory());

        // params.put(ImagingConstants.PARAM_KEY_VERBOSE, Boolean.TRUE);

         ImageParser[] ips = ImageParser.getAllImageParsers();
        for (ImageParser imageParser : ips) {
			imageParser.getName();
        }
        // read image
		// final BufferedImage image = Imaging.getBufferedImage(file, params);

		// return image;
		return null;
    }

	//
	// public static class ManagedImageBufferedImageFactory implements BufferedImageFactory {
	//
	// public BufferedImage getColorBufferedImage(final int width, final int height, final boolean hasAlpha) {
	// final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	// final GraphicsDevice gd = ge.getDefaultScreenDevice();
	// final GraphicsConfiguration gc = gd.getDefaultConfiguration();
	// return gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
	// }
	//
	// public BufferedImage getGrayscaleBufferedImage(final int width, final int height, final boolean hasAlpha) {
	// return getColorBufferedImage(width, height, hasAlpha);
	// }
	// }

}