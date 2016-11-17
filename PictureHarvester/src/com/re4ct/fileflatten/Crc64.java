package com.re4ct.fileflatten;

import java.awt.image.DataBuffer;

public class Crc64 {
	static long[]		table	= new long[256];
	final static long	poly	= 0xC96C5795D7870F42l;

	static {
		for (int i = 0; i < 256; ++i) {
			long crc = i;

			for (int j = 0; j < 8; ++j) {
				// is current coefficient set?
				if ((crc & 1) != 0) {
					// yes, then assume it gets zero'd (by implied x^64 coefficient of dividend)
					crc >>= 1;

					// and add rest of the divisor
					crc ^= poly;
				} else {
					// no? then move to next coefficient
					crc >>= 1;
				}
			}

			table[i] = crc;
		}
	}

	static long calculateCrc(DataBuffer img1DB) {
		long crc = 0;
		int numBanks = img1DB.getNumBanks();
		int numEls = img1DB.getSize();
		for (int bank = 0; bank < numBanks; bank++) {
			for (int el = 0; el < numEls; el++) {
				int el1 = img1DB.getElem(bank, el);
				while (el1 > 0) {
					int index = (int) ((((el1 ^ crc) % 256) + 256) / 2);
					el1 >>= 8;
					long lookup = table[index];
					crc >>= 8;
					crc ^= lookup;
				}
			}
		}

		return crc;
	}

	public static long calculateCrc(int[][][] colormap) {
		long crc = 0;
		int numR = colormap.length;
		int numG = colormap[0].length;
		int numB = colormap[0][0].length;
		for (int r = 0; r < numR; r++) {
			for (int g = 0; g < numG; g++) {
				for (int b = 0; b < numB; b++) {
					int el1 = colormap[r][g][b];
					while (el1 > 0) {
						int index = (int) ((((el1 ^ crc) % 256) + 256) / 2);
						el1 >>= 8;
						long lookup = table[index];
						crc >>= 8;
						crc ^= lookup;
					}
				}
			}
		}
		return crc;
	}

}
