package org.sssfile.files;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.sssfile.exceptions.WriteShardException;
import org.sssfile.util.Security;


public class OriginalFile {

	public final Path path;
	public final Map<Integer, byte[]> parts;
	private LinkedList<Path> shards_paths = null;
	private byte[] hash_original_file;

	private int current_key = 1;

	

	protected OriginalFile(Path path) {
		this.path	= path;
		this.parts	= new HashMap<Integer, byte[]>();
		hash_original_file = null;
	}

	public OriginalFile(Path path, Map<Integer, byte[]> parts) {
		this.path	= path;
		this.parts	= parts;
		hash_original_file = null;
	}

	public OriginalFile(Path path, byte[] hash_original_file) {
		this.path	= path;
		this.parts	= new HashMap<Integer, byte[]>();
		this.hash_original_file = hash_original_file;
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
	 * @return the content of the next shard, or null if got all
	 */
	public byte[] iterateShardContent() throws WriteShardException {

		FileNamesGenerator file_names_generator = new FileNamesGenerator(path);

		try {
			readHash();
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new WriteShardException(e.getMessage());
		}

		byte[] entry = parts.get(current_key);

		ShardFile shard = new ShardFile(
			Path.of( file_names_generator.generateShardPath() ),
			current_key, parts.get(current_key),
			path,
			hash_original_file
		);
		current_key++;

		return shard.getContent();
	}


	public boolean isValid(byte[] content) {
		try {
			return Arrays.equals(hash_original_file, Security.hashBytes(content));
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Read this path's content, then calculate and save the hash.
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private void readHash() throws IOException, NoSuchAlgorithmException {
		hash_original_file = Security.hashFileContent(path);
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

	@Override
	public int hashCode() {
		return Arrays.hashCode(hash_original_file);
	}
	
}
