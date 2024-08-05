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
	public SSSSplitter(int n, int k) {
        SecureRandom random_generator = new SecureRandom();
		scheme = new Scheme(random_generator, n, k);
		this.n = n;
		this.k = k;
		this.generation = System.nanoTime();	// unique for each flood (for this instance)
	}


	/**
	 *
	 * @param path file path
	 * @param content file content
	 * @param checksum sha1 checksum of the original file - can be read with {@link OriginalFile}.readHash()
	 * @return the OriginalFile object
	 * @throws IOException while reading file content
	 * @throws InvalidOriginalFileException if the file is a shard
	 */
	public OriginalFile getSplitFile(
			Path path, byte[] content, byte[] checksum
	) throws IOException, InvalidOriginalFileException {

		if(ShardFile.isValid(content)) {
			throw new InvalidOriginalFileException("Can't split a shard again.");
		}

		Map<Integer, byte[]> parts = scheme.split(content);
		return new OriginalFile(path, checksum, parts, n, k, generation);
	}

}
