/******************************************************************************
 * Copyright 2024 (C) by Daniele D'Ugo <danieledugo1@gmail.com>               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU Library General Public License as            *
 * published by the Free Software Foundation; either version 2 of the         *
 * License, or (at your option) any later version.                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU Library General Public          *
 * License along with this program; if not, write to the                      *
 * Free Software Foundation, Inc.,                                            *
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                  *
 *                                                                            *
 * For details about the authors of this software, see the AUTHORS file.      *
 ******************************************************************************/

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

	public static byte[] hashShard(int key, byte[] secret) throws NoSuchAlgorithmException {

		MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
				digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(key).array());
				digest.update(secret);
        return digest.digest();
	}

	/**
	 *
	 * @param base64 hash string in base 64
	 * @throws IllegalArgumentException if src is not in valid Base64 scheme
	 */
	public static byte[] hash_fromBase64(String base64) throws IllegalArgumentException {
		return Base64.getDecoder().decode( base64 );
	}
	
}
