package org.sssfile.files;

import org.sssfile.exceptions.InvalidShardException;
import org.sssfile.exceptions.ReadShardException;
import org.sssfile.util.IO;
import org.sssfile.files.ShardFile.Sections;
import org.sssfile.util.Security;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;




public class ShardFileGenerator {


	/**
	 * 
	 * @param path
	 * @return
	 * @throws InvalidShardException
	 */
	public ShardFile fromFile(Path path) throws InvalidShardException, ReadShardException {

		// check correct length
		if(path.toFile().length() < Sections.LEN_MIN) {
			throw new InvalidShardException("Shard file is too short: " + path);
		}

		/* read */
		int		key,
				original_path_len;
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

			key						= IO.readChannel( channel, Sections.LEN_KEY ).flip().getInt();
			hash_original_file		= IO.readChannel( channel, Sections.LEN_HASH ).array();
			hash_secret				= IO.readChannel( channel, Sections.LEN_HASH ).array();
			original_path_len		= IO.readChannel( channel, Sections.LEN_ORIGINAL_PATH_LEN ).flip().getInt();
			original_path			= IO.readChannel( channel, original_path_len ).array();
			secret	= IO.readChannel( channel,
					(int) path.toFile().length() - Sections.OFFSET_SECRET(original_path_len)
			).array();

		} catch (IOException e) {
			throw new ReadShardException("IO Exception, couldn't read shard's content for " + path + " : " + e.getMessage());
		}

		/* save shard fields */
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

	public static Boolean isValid(Path path) throws IOException {
		return Arrays.equals(
				IO.readBytes(path, Sections.HEADER_SIGNATURE.length, 0),
				Sections.HEADER_SIGNATURE
		);
	}

}

