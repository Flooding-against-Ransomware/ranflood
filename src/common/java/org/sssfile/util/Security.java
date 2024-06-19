package org.sssfile.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class Security {

	private static final String ALGORITHM = "SHA-1";



	public static byte[] hashBytes(byte[] bytes) throws NoSuchAlgorithmException {

		MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        return digest.digest(bytes);
	}

	public static String hashBytesB64(byte[] bytes) throws NoSuchAlgorithmException {

		MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        return Base64.getEncoder().encodeToString( digest.digest(bytes) );
	}

	public static byte[] hashFileContent(Path path) throws IOException, NoSuchAlgorithmException {
		return hashBytes(Files.readAllBytes(path));
	}	

	public static byte[] hashPath(Path path) throws NoSuchAlgorithmException {
		return hashBytes(path.toString().getBytes());
	}	

	public static byte[] hashSecret(int key, byte[] secret) throws NoSuchAlgorithmException {

		MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
				digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(key).array());
				digest.update(secret);
        return digest.digest();
	}	
	
}
