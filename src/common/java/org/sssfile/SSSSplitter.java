package org.sssfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;

import com.codahale.shamir.Scheme;

import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.files.OriginalFile;
import org.sssfile.files.ShardFile;



public class SSSSplitter {

    private final Scheme scheme;

	public final int	n,
						k;
	public final long	generation;


	/**
	 * 
	 * @param n number of shards created
	 * @param k minimum number of shards required to rebuild the original file (0<=k<=n)
	 */
	public SSSSplitter(int n, int k, long generation) {
        SecureRandom random_generator = new SecureRandom();
		scheme = new Scheme(random_generator, n, k);
		this.n = n;
		this.k = k;
		this.generation = generation;
	}


	/**
	 *
	 * @param path file path
	 * @return the OriginalFile object
	 * @throws IOException while reading file content
	 * @throws InvalidOriginalFileException if the file is a shard
	 */
	public OriginalFile getSplitFile(
			Path path
	) throws IOException, InvalidOriginalFileException {

		if(ShardFile.isValid(path)) {
			throw new InvalidOriginalFileException("Can't split a shard again.");
		}

		byte[] secret;
		try ( InputStream input = new FileInputStream( path.toFile() ) ) {
			secret = input.readAllBytes();
		}

		Map<Integer, byte[]> parts = scheme.split(secret);
		return new OriginalFile(path, parts, n, k, generation);
	}


}
