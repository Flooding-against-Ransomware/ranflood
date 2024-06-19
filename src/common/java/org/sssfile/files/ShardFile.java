package org.sssfile.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.sssfile.exceptions.InvalidShardException;
import org.sssfile.util.IO;
import org.sssfile.util.Security;


/**
 * The content of a shard is made of:
 * - the header signature,
 * - n, k, generation number, key (padded to int size)
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

		public static final int LEN_N					= Integer.BYTES;
		public static final int LEN_K					= Integer.BYTES;
		public static final int LEN_GENERATION			= Long.BYTES;
		public static final int LEN_KEY					= Integer.BYTES;
		public static final int LEN_HASH				= 160 / 8;
		public static final int LEN_ORIGINAL_PATH_LEN	= Integer.BYTES;

		public static final int OFFSET_N					= HEADER_SIGNATURE.length;
		public static final int OFFSET_K					= OFFSET_N					+ LEN_N;
		public static final int OFFSET_GENERATION			= OFFSET_K					+ LEN_K;
		public static final int OFFSET_KEY					= OFFSET_GENERATION			+ LEN_GENERATION;
		public static final int OFFSET_HASH_ORIGINAL_FILE	= OFFSET_KEY				+ LEN_KEY;
		public static final int OFFSET_HASH_SECRET			= OFFSET_HASH_ORIGINAL_FILE	+ LEN_HASH;
		public static final int OFFSET_ORIGINAL_PATH_LEN	= OFFSET_HASH_SECRET		+ LEN_HASH;
		public static final int OFFSET_ORIGINAL_PATH		= OFFSET_ORIGINAL_PATH_LEN	+ LEN_ORIGINAL_PATH_LEN;

		public static int OFFSET_SECRET(int original_path_len) {
			return OFFSET_ORIGINAL_PATH + original_path_len;
		}

		public static final int LEN_MIN = OFFSET_ORIGINAL_PATH;
	}


	public final Path path;
	public final int	n,
						k,
						key;
	public final long 	generation;
	public final byte[] secret;

	private final byte[]	hash_original_file;
	public final Path		original_file;
	
	

	public ShardFile(
		Path path, Path original_file, byte[] hash_original_file,
		int n, int k, long generation,
		int key, byte[] secret
	) {
		this.path	= path;
		this.hash_original_file	= hash_original_file;
		this.original_file		= original_file;

		this.n = n;
		this.k = k;
		this.generation = generation;

		this.key	= key;
		this.secret	= secret;
	}


	/*
	 * Read/Write content
	 */

	/**
	 *
	 * @param path -
	 * @return -
	 * @throws IOException while reading file content
	 * @throws InvalidShardException -
	 */
	public static ShardFile fromFile(Path path) throws
			IOException, InvalidShardException {

		// check correct length
		if(path.toFile().length() < Sections.LEN_MIN) {
			throw new InvalidShardException("Shard file is too short: " + path);
		}

		/* read */
		int		n, k,
				key,
				original_path_len;
		long	generation;
		byte[]	header,
				hash_original_file,
				hash_secret,
				original_path,
				secret;

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			header = IO.readChannel(channel, Sections.HEADER_SIGNATURE.length ).array();
			if (!Arrays.equals(header, Sections.HEADER_SIGNATURE)) {
				throw new InvalidShardException("Shard header is incorrect: " + path);
			}

			n						= IO.readChannel( channel, Sections.LEN_N			).flip().getInt();
			k						= IO.readChannel( channel, Sections.LEN_K			).flip().getInt();
			generation				= IO.readChannel( channel, Sections.LEN_GENERATION	).flip().getLong();
			key						= IO.readChannel( channel, Sections.LEN_KEY			).flip().getInt();
			hash_original_file		= IO.readChannel( channel, Sections.LEN_HASH ).array();
			hash_secret				= IO.readChannel( channel, Sections.LEN_HASH ).array();
			original_path_len		= IO.readChannel( channel, Sections.LEN_ORIGINAL_PATH_LEN ).flip().getInt();
			original_path			= IO.readChannel( channel, original_path_len ).array();
			secret	= IO.readChannel( channel,
					(int) path.toFile().length() - Sections.OFFSET_SECRET(original_path_len)
			).array();
		}

		/* save shard fields */
		try {
			byte[] hash_secret_test = Security.hashSecret(key, secret);

			if (!Arrays.equals(hash_secret, hash_secret_test)) {
				throw new InvalidShardException("Secret hash doesn't match: " + path);
			} else {
				return new ShardFile( path, Path.of(new String(original_path, StandardCharsets.UTF_8)), hash_original_file,
						n, k, generation,
						key, secret
				);
			}

		} catch (NoSuchAlgorithmException e) {
			throw new InvalidShardException("Error in calculating secret's hash, couldn't verify: " + path +
					"\n\t" + e.getMessage()
			);
		}

	}


	/**
	 * Get the content to write in this shard's file.
	 * The content is made of the header signature, the key (padded to int size)
	 * and the secret.
	 */
	public byte[] getContent() throws IOException, NoSuchAlgorithmException {

			ByteBuffer buffer = ByteBuffer.allocate(getContentLength());
			byte[] hash_secret;

			hash_secret = Security.hashSecret(key, secret);

			buffer.put(ByteBuffer.wrap( Sections.HEADER_SIGNATURE));						// header
			buffer.put(ByteBuffer.allocate(Sections.LEN_N			).putInt(n				).flip());		// padded to int size
			buffer.put(ByteBuffer.allocate(Sections.LEN_K			).putInt(k				).flip());
			buffer.put(ByteBuffer.allocate(Sections.LEN_GENERATION	).putLong(generation	).flip());
			buffer.put(ByteBuffer.allocate(Sections.LEN_KEY			).putInt(key			).flip());
			buffer.put(ByteBuffer.wrap(hash_original_file));						// hash of the original file
			buffer.put(ByteBuffer.wrap(hash_secret));								// hash of the key and secret
			buffer.put(ByteBuffer.allocate(Sections.LEN_ORIGINAL_PATH_LEN)
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


	/*
	 * Getters
	 */

	public OriginalFile getOriginalFile() {
		if(original_file == null) return null;
		else return new OriginalFile(original_file, hash_original_file, n, k, generation);
	}

	public static Boolean isValid(Path path) throws IOException {
		return Arrays.equals(
				IO.readBytes(path, Sections.HEADER_SIGNATURE.length, 0),
				Sections.HEADER_SIGNATURE
		);
	}


	/*
	 * Object
	 */

	@Override
	public int hashCode() {
		return Arrays.hashCode(hash_original_file);
	}


}
