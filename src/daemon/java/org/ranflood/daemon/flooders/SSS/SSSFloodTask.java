/******************************************************************************
 * Copyright 2021 (C) by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>  *
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

package org.ranflood.daemon.flooders.SSS;

import org.ranflood.common.FloodMethod;
import org.ranflood.common.RanfloodLogger;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.FlooderException;
import org.sssfile.SSSSplitter;
import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.files.OriginalFile;
import org.ranflood.daemon.flooders.SnapshotException;
import org.ranflood.daemon.flooders.tasks.FloodTaskGenerator;
import org.ranflood.daemon.flooders.tasks.WriteFileTask;
import org.sssfile.util.IO;
import org.sssfile.util.Security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;



import static org.ranflood.common.RanfloodLogger.error;

public class SSSFloodTask extends FloodTaskGenerator {

	private final SSSSplitter sss;
	private final boolean remove_originals;

	private final List< WriteFileTask > tasks;
	private final ReentrantReadWriteLock lock;
	private int taskListResponseRetriesCounter = 0;

	public SSSFloodTask(
			Path filePath, FloodMethod floodMethod, SSSSplitter sss, boolean remove_originals
	) throws FlooderException {
		super( filePath, floodMethod );
		this.sss = sss;
		this.remove_originals = remove_originals;
		lock = new ReentrantReadWriteLock();
		tasks = new LinkedList<>();
		loadWriteFileTasks( filePath(), filePath() );
	}

	@Override
	public List< WriteFileTask > getFileTasks() {
		int taskListResponseRetriesTimeout = 100; // milliseconds
		int maxTaskListResponseRetries = 5;
		if ( taskListResponseRetriesCounter > 0 ) {
			try {
				Thread.sleep( taskListResponseRetriesTimeout );
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		}
		lock.readLock().lock();
		List< WriteFileTask > t = new LinkedList<>( tasks );
		lock.readLock().unlock();
		if ( t.isEmpty() && taskListResponseRetriesCounter < maxTaskListResponseRetries ) {
			taskListResponseRetriesCounter++;
			// we retry to
			return getFileTasks();
		}
		return t;
	}

	@Override
	public Runnable getRunnableTask() {
		throw new UnsupportedOperationException( "SSSFloodTask should not be run as a normal task" );
//		return () ->
//						tasks.forEach( t ->
//										RanfloodDaemon.executeIORunnable( t.getRunnableTask() )
//						);
	}

	private void loadWriteFileTasks( Path parentFilePath, Path filePath ) throws FlooderException {

		SecureRandom secure_random = new SecureRandom();
		File file = filePath.toFile();

		try {
			if ( file.isFile() ) {
				byte[] bytes;
				try ( InputStream input = new FileInputStream( file ) ) {
					bytes = input.readAllBytes();
				}
				// only encrypt if signature still valid
				if ( SSSSnapshooter.getBytesSignature( bytes ).equals(
								SSSSnapshooter.getSnapshot( parentFilePath, filePath ) ) ) {

					// split with sss
					OriginalFile original_file = sss.getSplitFile(filePath);
					original_file.hash_original_file = Security.hashBytes(bytes);

					// try to write all shards
					int shards_created = 0;
					byte[] shard_content = new byte[0];
					do {
						try {
							shard_content = original_file.iterateShardContent();
						} catch (IOException e) {
							RanfloodLogger.error("Couldn't get shard content: " + e.getMessage());
							continue;
						}
						Path shard_path = IO.createUniqueFile(secure_random, filePath);

						lock.writeLock().lock();
						tasks.add(new WriteFileTask(shard_path, shard_content, floodMethod()));
						lock.writeLock().unlock();
					} while(shard_content != null);

					// remove original file, if created enough shards
					RanfloodLogger.log("Created " + shards_created + " shards for " + filePath);
					if(remove_originals && shards_created >= sss.k) {
						try {
							Files.delete(filePath);
							RanfloodLogger.log("Deleted original file");
						} catch (IOException e) {
							RanfloodLogger.error("Error deleting original file: " + e.getMessage());
						}
					}
				}
			} else {	// recursion on directory
				Arrays.stream( Objects.requireNonNull( file.listFiles() ) )
								.forEach( f ->
												Ranflood.daemon().executeCommand( () -> {
													try {
														loadWriteFileTasks( parentFilePath, f.toPath() );
													} catch ( FlooderException e ) {
														error( "Error in instantiating writing task for "
																		+ f.toPath().toAbsolutePath() + " in " + parentFilePath.toAbsolutePath()
																		+ ": " + e.getMessage()
														);
													}
												} )
								);
			}
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new FlooderException(
							"Could not open file or folder " + file.getAbsolutePath() + " while trying to create " + floodMethod() + " task"
			);
		} catch ( NoSuchAlgorithmException e ) {
			e.printStackTrace();
			throw new FlooderException(
							"Error in using signatures algorithm " + e.getMessage()
			);
		} catch ( InvalidOriginalFileException e ) {
			e.printStackTrace();
			throw new FlooderException("Invalid original file: " + e.getMessage() );
		} catch ( SnapshotException e ) {
			e.printStackTrace();
			throw new FlooderException("Could not find a snapshot of file " + file.getAbsolutePath());
		}
	}

}
