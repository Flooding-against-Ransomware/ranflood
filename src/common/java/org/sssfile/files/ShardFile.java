package org.sssfile.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.sssfile.exceptions.WriteShardException;
import org.sssfile.util.Security;


/**
 * The content of a shard is made of:
 * - the header signature,
 * - the key (padded to int size)
 * - the hash of the original file (sha1, 160 bits)
 * - the hash of the key and secret (sha1, 160 bits)
 * - the size (length) of the original file path (bytes)
 * - the original file path
 * - the secret
 */
public class ShardFile {

	protected static class Sections {

		// a (hopefully) unique signature
		protected static final byte[] HEADER_SIGNATURE = new byte[] {
				(byte) 0x12,
				(byte) 0x34,
				(byte) 0x56,
				(byte) 0x78,
				(byte) 0x9A,
				(byte) 0xBC,
				(byte) 0xDE,
				(byte) 0xF0
		};

		public static final int LEN_KEY					= Integer.BYTES;
		public static final int LEN_HASH				= 160 / 8;
		public static final int LEN_ORIGINAL_PATH_LEN	= Integer.BYTES;
		
		public static final int OFFSET_KEY					= HEADER_SIGNATURE.length;
		public static final int OFFSET_HASH_ORIGINAL_FILE	= OFFSET_KEY + LEN_KEY;
		public static final int OFFSET_HASH_SECRET			= OFFSET_HASH_ORIGINAL_FILE + LEN_HASH;
		public static final int OFFSET_ORIGINAL_PATH_LEN	= OFFSET_HASH_SECRET + LEN_HASH;
		public static final int OFFSET_ORIGINAL_PATH		= OFFSET_ORIGINAL_PATH_LEN + LEN_ORIGINAL_PATH_LEN;

		public static int OFFSET_SECRET(int original_path_len) {
			return OFFSET_ORIGINAL_PATH + original_path_len;
		}

		public static final int LEN_MIN = OFFSET_ORIGINAL_PATH;
	}


	public final Path path;
	public final Integer key;
	public final byte[] secret;

	private final byte[]	hash_original_file;
	public final Path		original_file;
	
	

	public ShardFile(
		Path path, Integer key, byte[] secret, Path original_file, byte[] hash_original_file
	) {
		this.path	= path;
		this.key	= key;
		this.secret	= secret;
		this.hash_original_file	= hash_original_file;
		this.original_file		= original_file;
	}


	/**
	 * Get the content to write in this shard's file.
	 * The content is made of the header signature, the key (padded to int size)
	 * and the secret.
	 */
	public byte[] getContent() throws WriteShardException {

			ByteBuffer buffer = ByteBuffer.allocate(getContentLength());
			byte[] hash_secret;

			try {
				hash_secret = Security.hashSecret(key, secret);
			} catch (IOException | NoSuchAlgorithmException e) {
				throw new WriteShardException(e.getMessage());
			}

			buffer.put(ByteBuffer.wrap(getHeaderSignature()));						// header
			buffer.put(ByteBuffer.allocate(Integer.BYTES).putInt(key).flip());		// key padded to int size
			buffer.put(ByteBuffer.wrap(hash_original_file));						// hash of the original file
			buffer.put(ByteBuffer.wrap(hash_secret));								// hash of the key and secret
			buffer.put(ByteBuffer.allocate(Integer.BYTES)
					.putInt(original_file.toString().length()).flip());				// original file path length
			buffer.put(ByteBuffer.wrap(original_file.toString().getBytes()));		// original file path
			buffer.put(ByteBuffer.wrap(secret));									// secret

		buffer.rewind();
		byte[] array = new byte[buffer.remaining()];
		buffer.get(array);
		return array;
	}

	public int getContentLength() {
		return Sections.HEADER_SIGNATURE.length	// header signature
				+ Sections.LEN_KEY				// key
				+ Sections.LEN_HASH				// hash original file
				+ Sections.LEN_HASH				// hash secret
				+ Sections.LEN_ORIGINAL_PATH_LEN	// original path length
				+ original_file.toString().length()	// original path
				+ secret.length						// secret
		;
	}


	public static byte[] getHeaderSignature() {
		return Sections.HEADER_SIGNATURE;
	}

	public OriginalFile getOriginalFile() {
		if(original_file == null) return null;
		else return new OriginalFile(original_file, hash_original_file);
	}

	public int getOriginalFileHash() {
		return Arrays.hashCode(hash_original_file);
	}


}
