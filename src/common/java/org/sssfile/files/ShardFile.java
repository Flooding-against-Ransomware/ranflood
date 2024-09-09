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
 * - the hash of the key and shard (sha1, 160 bits)
 * - the size (length) of the original file path (bytes)
 * - the original file path
 * - the shard
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
		public static final int OFFSET_HASH_SHARD			= OFFSET_HASH_ORIGINAL_FILE	+ LEN_HASH;
		public static final int OFFSET_ORIGINAL_PATH_LEN	= OFFSET_HASH_SHARD			+ LEN_HASH;
		public static final int OFFSET_ORIGINAL_PATH		= OFFSET_ORIGINAL_PATH_LEN	+ LEN_ORIGINAL_PATH_LEN;

		public static int OFFSET_SHARD(int original_path_len) {
			return OFFSET_ORIGINAL_PATH + original_path_len;
		}

		public static final int LEN_MIN = OFFSET_ORIGINAL_PATH;
	}


	public final Path path;
	public final int	n,
						k,
						key;
	public final long 	generation;
	public final byte[] shard;

	private final byte[]	hash_original_file;
	public final Path		original_file;
	
	

	public ShardFile(
		Path path, Path original_file, byte[] hash_original_file,
		int n, int k, long generation,
		int key, byte[] shard
	) {
		this.path	= path;
		this.hash_original_file	= hash_original_file;
		this.original_file		= original_file;

		this.n = n;
		this.k = k;
		this.generation = generation;

		this.key	= key;
		this.shard	= shard;
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
				hash_shard,
				original_path,
				shard;

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
			hash_shard				= IO.readChannel( channel, Sections.LEN_HASH ).array();
			original_path_len		= IO.readChannel( channel, Sections.LEN_ORIGINAL_PATH_LEN ).flip().getInt();
			original_path			= IO.readChannel( channel, original_path_len ).array();
			shard	= IO.readChannel( channel,
					(int) path.toFile().length() - Sections.OFFSET_SHARD(original_path_len)
			).array();
		}

		/* save shard fields */
		try {
			byte[] hash_shard_test = Security.hashShard(key, shard);

			if (!Arrays.equals(hash_shard, hash_shard_test)) {
				throw new InvalidShardException("Shard hash doesn't match: " + path);
			} else {
				return new ShardFile( path, Path.of(new String(original_path, StandardCharsets.UTF_8)), hash_original_file,
						n, k, generation,
						key, shard
				);
			}

		} catch (NoSuchAlgorithmException e) {
			throw new InvalidShardException("Error in calculating shard's hash, couldn't verify: " + path +
					"\n\t" + e.getMessage()
			);
		}

	}


	/**
	 * Get the content to write in this shard's file.
	 * The content is made of the header signature, the key (padded to int size)
	 * and the shard.
	 */
	public byte[] getContent() throws NoSuchAlgorithmException {

		ByteBuffer buffer = ByteBuffer.allocate(getContentLength());
		byte[] hash_shard;

		hash_shard = Security.hashShard(key, shard);

		buffer.put(ByteBuffer.wrap( Sections.HEADER_SIGNATURE));						// header
		buffer.put(ByteBuffer.allocate(Sections.LEN_N			).putInt(n				).flip());		// padded to int size
		buffer.put(ByteBuffer.allocate(Sections.LEN_K			).putInt(k				).flip());
		buffer.put(ByteBuffer.allocate(Sections.LEN_GENERATION	).putLong(generation	).flip());
		buffer.put(ByteBuffer.allocate(Sections.LEN_KEY			).putInt(key			).flip());
		buffer.put(ByteBuffer.wrap(hash_original_file));						// hash of the original file
		buffer.put(ByteBuffer.wrap(hash_shard));								// hash of the key and shard
		buffer.put(ByteBuffer.allocate(Sections.LEN_ORIGINAL_PATH_LEN)
				.putInt(original_file.toString().length()).flip());				// original file path length
		buffer.put(ByteBuffer.wrap(original_file.toString().getBytes()));		// original file path
		buffer.put(ByteBuffer.wrap(shard));										// shard

		buffer.rewind();
		byte[] array = new byte[buffer.remaining()];
		buffer.get(array);
		return array;
	}

	public int getContentLength() {
		return Sections.HEADER_SIGNATURE.length		// header signature
				+ Sections.LEN_N
				+ Sections.LEN_K
				+ Sections.LEN_GENERATION
				+ Sections.LEN_KEY
				+ Sections.LEN_HASH					// hash original file
				+ Sections.LEN_HASH					// hash shard
				+ Sections.LEN_ORIGINAL_PATH_LEN	// original path length
				+ original_file.toString().length()	// original path
				+ shard.length						// shard
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

	public static Boolean isValid(byte[] content) throws IOException {
		return Arrays.equals(
				Arrays.copyOfRange(content, 0, Sections.HEADER_SIGNATURE.length),
				Sections.HEADER_SIGNATURE
		);
	}


	/*
	 * Object
	 */

	@Override
	public int hashCode() {
		ByteBuffer buff = ByteBuffer.allocate(Long.BYTES + hash_original_file.length);
		buff.put(0, hash_original_file);
		buff.position(hash_original_file.length);
		buff.putLong(generation).flip();
		return Arrays.hashCode(buff.array());
	}


}
