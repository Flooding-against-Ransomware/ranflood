package org.sssfile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import com.codahale.shamir.Scheme;
import org.sssfile.exceptions.InvalidShardException;
import org.sssfile.files.OriginalFile;
import org.sssfile.files.ShardFile;
import org.sssfile.files.ShardFileGenerator;
import org.sssfile.files.RestoredFilesList;
import org.sssfile.util.LoggerRestore;


public class SSSRestorer {


	private final LoggerRestore logger;

    private final ShardFileGenerator shard_generator;
	private final Scheme scheme;
	private final Path root;

	private final boolean remove_shards;
	
	// group shards by original file name
	private final RestoredFilesList shard_groups;

	

	public SSSRestorer(int n, int k, Path dir,
		LoggerRestore logger, ShardFileGenerator shard_generator,
		boolean remove_shards
	) {
		this.logger = logger;
		this.shard_generator = shard_generator;

        SecureRandom random_generator = new SecureRandom();
		scheme = new Scheme(random_generator, n, k);
		root = dir;

		this.remove_shards = remove_shards;

		shard_groups = new RestoredFilesList();
	}

	
	public void start() {
		logger.start();
		
		findShards(root);
		dirJoin(root);

		logger.summary();
	}



	/*
	 * SSS
	 */

	/**
	 * Join all shards in a directory.
	 * @param dir
	 */
	private void dirJoin(Path dir) {

		for (OriginalFile original_file : shard_groups.values()) {
			fileJoin(original_file);
		}
		
	}


	/**
	 * Join all shards of a file.
	 */
	private void fileJoin(OriginalFile original_file) {

		if(original_file.parts.size() < scheme.k()) {
			logger.fileUnrecoverable(original_file.path, original_file.parts.size());
			return;
		}

		logger.logDebug("Recovering file: " + original_file.path);

		byte[] recovered = scheme.join(original_file.parts);
		
		try {
			Files.write( original_file.path, recovered );
		} catch (IOException e) {
			logger.errorIO(original_file.path);
			return;
		}

		if(!original_file.isValid(recovered)) {
			logger.fileBadHash(original_file.path);
			return;
		}

		logger.fileRestored(original_file.path);

		if(remove_shards) {
			for(Path shard_path : original_file.getShardsPaths()) {
				try {
					Files.delete(shard_path);
					logger.deleteShard(shard_path, true);
				} catch (IOException e) {
					logger.deleteShard(shard_path, false);
				}
			}
		}
	}
	

	/**
	 * Find all shards in a directory, and group them by original file in `shard_groups`.
	 * @param dir
	 */
	private void findShards(Path dir) {

		try ( DirectoryStream<Path> stream = Files.newDirectoryStream(dir) ) {
			for (Path file : stream) {

				if(Files.isDirectory(file)) {
					findShards(file);
					continue;
				}

				ShardFile shard;
				try {
					shard = shard_generator.fromFile(file);
				} catch (InvalidShardException e) {
					logger.foundShard(file, false);
					continue;
				}

				if(shard_groups.get(shard.getOriginalFileHash()) == null) {
					logger.foundOriginalFile(shard.getOriginalPath());
				}
				
				shard_groups.addShard(shard);
				
				logger.foundShard(shard.path, true);
				logger.logDebug("New shards of " + shard_groups.get(shard.getOriginalFileHash()).path + ": " + shard_groups.get(shard.getOriginalFileHash()).parts.size() );

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
