package com.re4ct.fileflatten;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileCompare {

	@SuppressWarnings("static-method")
	@Test
	public void test() throws IOException {
		File f1 = new File("R:/Media/pix/1970-03-18 - 2013-09-28/1.jpg");
		assertTrue(f1.exists());
		File f2 = new File("D:/Flat pics/1.jpg");
		assertTrue(f2.exists());
		assertTrue("Files apparently not equal", FileFlattener.isEqual(f1, f2));
	}

}
