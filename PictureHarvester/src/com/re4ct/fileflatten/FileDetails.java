package com.re4ct.fileflatten;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class FileDetails {
	final static Logger	log	= Logger.getLogger(FileDetails.class);

	private File	file;
	private String	md5;

	public FileDetails(File file) {
		super();
		this.file = file;
		md5 = null;
	}

	public String getMd5() {
		if (md5 != null) {
			log.info("returning existing MD5");
			return md5;
		}
		md5 = getMd5(file);
		return md5;
	}

	public static String getMd5(File file) {
		try (FileInputStream fis = new FileInputStream(file);) {
			MessageDigest md = MessageDigest.getInstance("MD5");

			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;
			byte[] mdbytes = md.digest();

			// convert the byte to hex format method 2
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				String hex = Integer.toHexString(0xff & mdbytes[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			// log.info("Digest(in hex format):: " + hexString.toString());
			String md5 = hexString.toString();
			return md5;
		} catch (IOException | NoSuchAlgorithmException e) {
			return null;
		}
	}

	public boolean isFile() {
		return file.isFile();
	}

	public File getFile() {
		return file;
	}
}
