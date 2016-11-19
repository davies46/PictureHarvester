package com.re4ct.fileflatten;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileCompare {

	@SuppressWarnings("static-method")
	@Test
	public void test() throws IOException {
		File f1 = new File("R:/pix/01-01 SouthAfrica/dscf0193.jpg");
		assertTrue(f1.exists());
		File f2 = new File("D:/Flat pics/dscf0193.jpg");
		assertTrue(f2.exists());
		File f3 = new File("R:/pix/01-01 SouthAfrica/dscf0192.jpg");
		assertTrue(f2.exists());
		ImageDetails details1 = new ImageDetails(f1, 1024, 1024);
		ImageDetails details2 = new ImageDetails(f2, 1024, 1024);
		ImageDetails details3 = new ImageDetails(f3, 1024, 1024);
		assertEquals(details1.getDigest(), details2.getDigest());
		assertNotEquals(details1.getDigest(), details3.getDigest());
	}

}
