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

 package org.sssfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;

import com.codahale.shamir.Scheme;
import org.ranflood.common.RanfloodLogger;
import org.ranflood.common.utils.Pair;
import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.exceptions.InvalidShardException;
import org.sssfile.exceptions.UnrecoverableOriginalFileException;
import org.sssfile.files.OriginalFile;
import org.sssfile.files.ShardFile;
import org.sssfile.files.RestoredFilesList;
import org.sssfile.util.LoggerRestore;
import org.sssfile.util.LoggerResult;
import org.sssfile.util.Security;
import org.zeromq.Utils;


public class SSSRestorer {

	private final LoggerRestore logger;

	SecureRandom random_generator;
	private Scheme scheme;
	private final Path root;

	// group shards by original file name
	private final RestoredFilesList shard_groups;

	private Iterator<OriginalFile> iterator = null;
	private int iterator_count = 0;



	public SSSRestorer(Path dir,
		Path file_log, boolean debug, boolean debug_dev
	) {
		random_generator = new SecureRandom();
		logger			= new LoggerRestore(file_log, debug_dev, debug);
		root			= dir;
		shard_groups	= new RestoredFilesList();
	}





	/**
	 * Find all shards in a directory, and group them by original file in `shard_groups`.
	 */
	public void findShards() throws IOException {
		logger.start();
		findShards(root);
		logger.summary();
	}

	private void findShards(Path dir) throws IOException {

		try ( DirectoryStream<Path> stream = Files.newDirectoryStream(dir) ) {
			for (Path file : stream) {

				if (Files.isDirectory(file)) {
					try {
						findShards(file);
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
					continue;
				}

				ShardFile shard;
				try {
					shard = ShardFile.fromFile(file);
				} catch (InvalidShardException e) {
					logger.foundShard(file, false);
					continue;
				} catch (IOException e) {
					logger.fileErrorReading(file);
					continue;
				}

				if (shard_groups.get(shard.hashCode()) == null) {
					logger.foundOriginalFile(shard.original_file);
				}

				shard_groups.addShard(shard);

				logger.foundShard(shard.path, true);
				logger.logDebug("New shards of " + shard_groups.get(shard.hashCode()).path + ": " + shard_groups.get(shard.hashCode()).parts.size());

			}
		}

	}

	/**
	 *
	 * @return a pair containing the OriginalFile object and its content (bytes)
	 * @throws InvalidOriginalFileException if content has wrong checksum
	 * @throws NoSuchAlgorithmException if happens in calculating checksums
	 * @throws UnrecoverableOriginalFileException if not enough shards
	 */
	public Pair<OriginalFile, byte[]> iterateOriginalFile() throws
			InvalidOriginalFileException,
			NoSuchAlgorithmException,
			UnrecoverableOriginalFileException
	{
		if(iterator == null) {
			iterator = shard_groups.values().iterator();
			iterator_count = 0;
		}

		if(!iterator.hasNext()) {
			iterator = null;
			return null;
		}
		OriginalFile original_file = iterator.next();
		iterator_count++;

		if(original_file.parts.size() < original_file.k) {
			logger.fileErrorUnrecoverable(original_file.path, original_file.k, original_file.parts.size());
			throw new UnrecoverableOriginalFileException(
					"Can't restore file with only " + original_file.parts.size() + " / " + original_file.k + " parts: " + original_file.path);
		}

		logger.logDebug("Recovering file: " + original_file.path);
		scheme = new Scheme(random_generator, original_file.n, original_file.k);
		byte[] recovered = scheme.join(original_file.parts);

		if(!original_file.isValid(recovered)) {
			String hash_found;
            hash_found = Security.hashBytesB64(recovered);
            logger.fileErrorBadHash(original_file, hash_found );
			throw new InvalidOriginalFileException(
					"Restored checksum doesn't match (got " + original_file.getHashBase64() + " , found " + hash_found + " ): " + original_file.path);
		}

		logger.fileRestored(original_file.path);
		return new Pair<>(original_file, recovered);

	}


	public int getIteratorPercentage() {
		return (shard_groups.isEmpty()) ?
				100
				: iterator_count * 100 / shard_groups.size();
	}

	public LoggerResult getStats() {
		return logger.getStats();
	}


}
