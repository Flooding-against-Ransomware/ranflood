package org.sssfile.files;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.sssfile.util.Security;


public class OriginalFile {

	public final Path path;
	public final LinkedHashMap<Integer, byte[]> parts;
	private LinkedList<Path> shards_paths = null;
	public byte[] hash_original_file;

	public final int	n,
						k;
	public final long	generation;

	private Iterator<Map.Entry<Integer, byte[]>> iterator;

	

	public OriginalFile(
			Path path, Map<Integer, byte[]> parts,
			int n, int k, long generation
	) {
		this.path	= path;
		this.parts	= new LinkedHashMap<>(parts);
		hash_original_file = null;
		this.n = n;
		this.k = k;
		this.generation = generation;
	}

	public OriginalFile(
			Path path, byte[] hash_original_file,
			int n, int k, long generation
	) {
		this.path	= path;
		this.parts	= new LinkedHashMap<>();
		this.hash_original_file = hash_original_file;
		this.n = n;
		this.k = k;
		this.generation = generation;
	}


	public void addPart(Integer part_number, byte[] part) {
		parts.put(part_number, part);
	}

	public void addShardPath(Path shard_path) {
		if(shards_paths == null) {
			shards_paths = new LinkedList<Path>();
		}
		shards_paths.add(shard_path);
	}

	public LinkedList<Path> getShardsPaths() {
		return shards_paths;
	}


	/**
	 * Used to iteratively return the content of a shard to write.
	 * If the hash is not given, it's calculated.
	 * @return the content of the next shard, or null if got all
	 * @throws IOException on reading content to compute hash (if was not already given)
	 * @throws NoSuchAlgorithmException on computing hashes
	 */
	public byte[] iterateShardContent() throws IOException, NoSuchAlgorithmException {

		if(iterator == null)
			iterator = parts.entrySet().iterator();

		if(!iterator.hasNext()) {
			iterator = null;
			return null;
		}
		Map.Entry<Integer, byte[]> entry = iterator.next();

		FileNamesGenerator file_names_generator = new FileNamesGenerator(path);

		if(hash_original_file == null) {
			readHash();
		}

		ShardFile shard = new ShardFile(
			Path.of( file_names_generator.generateShardPath() ), path, hash_original_file,
			n, k, generation,
			entry.getKey(), entry.getValue()
		);

		return shard.getContent();
	}

	/**
	 * Read this path's content, then calculate and save the hash.
	 * @throws IOException -
	 * @throws NoSuchAlgorithmException -
	 */
	private void readHash() throws IOException, NoSuchAlgorithmException {
		hash_original_file = Security.hashFileContent(path);
	}


	/*
	 * Get
	 */

	public String getHashBase64() {
		return Base64.getEncoder().encodeToString( hash_original_file );
	}
	public boolean isValid(byte[] content) throws NoSuchAlgorithmException {
		return Arrays.equals(hash_original_file, Security.hashBytes(content));
	}


	/*
	 * Object
	 */

	@Override
	public boolean equals(Object obj) {
        if (this == obj)
            return true;

		if (obj == null || getClass() != obj.getClass())
            return false;

        OriginalFile that = (OriginalFile)obj;
        return Arrays.equals(hash_original_file, that.hash_original_file);
    }

	
}
