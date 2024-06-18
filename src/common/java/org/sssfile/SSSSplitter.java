package org.sssfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;

import com.codahale.shamir.Scheme;

import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.exceptions.WriteShardException;
import org.sssfile.files.OriginalFile;
import org.sssfile.files.ShardFile;


public class SSSSplitter {

    private final Scheme scheme;

	public final int n;
	public final int k;


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
	}

	
	public OriginalFile getSplitFile(
			Path path
	) throws InvalidOriginalFileException, WriteShardException {

		if(ShardFile.isValid(path)) {
			throw new InvalidOriginalFileException("Can't split a shard again.");
		}

		byte[] secret;
		try ( InputStream input = new FileInputStream( path.toFile() ) ) {
			secret = input.readAllBytes();
		} catch (IOException e) {
			throw new WriteShardException("Couldn't read file: " + e.getMessage());
		}

		Map<Integer, byte[]> parts = scheme.split(secret);
		return new OriginalFile(path, parts);
	}


}
