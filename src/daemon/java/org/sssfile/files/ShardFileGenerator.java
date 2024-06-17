package org.sssfile.files;

import org.sssfile.exceptions.InvalidShardException;
import org.sssfile.util.IO;
import org.sssfile.files.ShardFile.HashFields;
import org.sssfile.util.Security;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;




public class ShardFileGenerator {



	public ShardFile getInstance(Path path, Integer key, byte[] secret) {
		return new ShardFile(path, key, secret);
	}

	public ShardFile getInstance(
		Path path, Integer key, byte[] secret, Path original_file, byte[] original_file_hash
	) {
		return new ShardFile(path, key, secret, original_file, original_file_hash);
	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws InvalidShardException
	 */
	public ShardFile fromFile(Path path) throws InvalidShardException {

		// check correct length
		if(path.toFile().length() < ShardFile.HashFields.LEN_MIN) {
			throw new InvalidShardException("Shard file is too short: " + path);
		}

		byte[] header = IO.readBytes(path, ShardFile.HashFields.HEADER_SIGNATURE.length, 0);
		if (!Arrays.equals(header, HashFields.HEADER_SIGNATURE)) {
			throw new InvalidShardException("Shard header is incorrect: " + path);
		}

		byte[] bytes_key				= IO.readBytes(path, HashFields.LEN_KEY, HashFields.OFFSET_KEY);
		int key							= ByteBuffer.wrap(bytes_key).getInt();
		byte[] hash_original_file		= IO.readBytes(path, HashFields.LEN_HASH, HashFields.OFFSET_HASH_ORIGINAL_FILE);
		byte[] hash_secret				= IO.readBytes(path, HashFields.LEN_HASH, HashFields.OFFSET_HASH_SECRET);
		byte[] bytes_original_path_len	= IO.readBytes(path, HashFields.LEN_ORIGINAL_PATH_LEN, HashFields.OFFSET_ORIGINAL_PATH_LEN);
		int original_path_len			= ByteBuffer.wrap(bytes_original_path_len).getInt();
		byte[] original_path			= IO.readBytes(path, original_path_len, HashFields.OFFSET_ORIGINAL_PATH);
		byte[] secret	= IO.readBytes( path,
			(int) path.toFile().length() - HashFields.OFFSET_SECRET(original_path_len),
			HashFields.OFFSET_SECRET(original_path_len)
		);

		try {
			byte[] hash_secret_test = Security.hashSecret(key, secret);

			if (!Arrays.equals(hash_secret, hash_secret_test)) {
				throw new InvalidShardException("Secret hash doesn't match: " + path);
			} else {
				return new ShardFile( path, key, secret,
					Path.of(new String(original_path, StandardCharsets.UTF_8)), hash_original_file
				);
			}

		} catch (IOException | NoSuchAlgorithmException e) {
			throw new InvalidShardException("Error in calculating secret's hash, couldn't verify: " + path +
				"\n\t" + e.getMessage()
			);
		}

	}

}

