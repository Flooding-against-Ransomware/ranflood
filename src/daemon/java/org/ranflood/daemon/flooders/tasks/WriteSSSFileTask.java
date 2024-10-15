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

package org.ranflood.daemon.flooders.tasks;

import org.ranflood.common.FloodMethod;
import org.sssfile.SSSSplitter;
import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.files.FileNamesGenerator;
import org.sssfile.files.OriginalFile;
import org.sssfile.util.Security;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

public class WriteSSSFileTask extends WriteFileTask {

	private final SSSSplitter sss;
	private final String signature;


	public WriteSSSFileTask(Path filePath, byte[] content, FloodMethod floodMethod,  SSSSplitter sss, String signature ) {
		super( filePath, content, floodMethod );
		this.sss		= sss;
		this.signature	= signature;
	}

	public Runnable getRunnableTask() {
		return () -> {

			File parentFolder = filePath().getParent().toFile();
			if ( !parentFolder.exists() ) {
				synchronized ( filePath() ) {
					parentFolder.mkdirs();
				}
			}

			try {
				// split with sss
				long time_start = System.currentTimeMillis();
				OriginalFile original_file = sss.getSplitFile(filePath(), content(), Security.hash_fromBase64(signature));
				long time_end = System.currentTimeMillis();
System.out.println(filePath() + ", time split: " + (time_end - time_start));

				// try to write all shards
				int shards_created = 0;
				byte[] shard_content;
				while(true) {

					shard_content = original_file.iterateShardContent();
					if(shard_content == null)
						break;

					Path shard_path = FileNamesGenerator.getUniquePath(filePath().toString());

					try {
						writeFile(shard_path, shard_content);
						shards_created++;
					} catch ( IOException e ) {
						error( e.getMessage() );
					}
				}

				// original file's removal is a single-use task, while this task will be retried in case of error
				/*
				// if enough shards weren't created, for any reason, better recreate original file, so it's not lost
				if(shards_created < sss.k && !filePath().toFile().exists() ) {
					writeFile(filePath(), content());
				}
				 */

			} catch (IOException | NoSuchAlgorithmException e ) {
				error( e.getMessage() );
			} catch ( InvalidOriginalFileException e ) {
				// it just means it's a shard and won't be split again
				// don't log, as there could be a lot of logs, and IO is very expensive
			}



		};
	}

	private static void writeFile(Path path, byte[] content) throws IOException {
		FileOutputStream f = new FileOutputStream( path.toAbsolutePath().toString() );
		BufferedOutputStream bout = new BufferedOutputStream( f );
		bout.write( content );
		bout.close();
		f.close();
	}

}
