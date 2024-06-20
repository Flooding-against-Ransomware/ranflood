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
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.tasks.FileTask;
import org.ranflood.daemon.flooders.tasks.RemoveFileTask;
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

	private final List< FileTask > tasks;
	private final List< FileTask > tasks_single_use;
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
		tasks_single_use = new LinkedList<>();
		loadWriteFileTasks( filePath(), filePath() );
	}


	private List< FileTask > _getFileTasks(List< FileTask > tasks) {
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
		List< FileTask > t = new LinkedList<>(tasks);
		lock.readLock().unlock();
		if ( t.isEmpty() && taskListResponseRetriesCounter < maxTaskListResponseRetries ) {
			taskListResponseRetriesCounter++;
			// we retry to
			return _getFileTasks(tasks);
		}
		return t;
	}

	@Override
	public List< FileTask > getFileTasks() {
		return _getFileTasks(tasks);
	}

	@Override
	public List< FileTask > getSingleUseFileTasks() {
		List< FileTask > res = _getFileTasks(tasks_single_use);
		lock.writeLock().lock();
		tasks_single_use.removeAll(res);
		lock.writeLock().unlock();
		return res;
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

				String signature = SSSSnapshooter.getBytesSignature( bytes );
				String signature_snapshot = null;
				try {
					signature_snapshot = SSSSnapshooter.getSnapshot( parentFilePath, filePath );
				} catch ( SnapshotException e ) {
					// don't log, as there could be a lot of logs, and IO is very expensive
					//throw new FlooderException( "Could not find a snapshot of file " + file.getAbsolutePath());
				}

				// only encrypt if signature still valid (so ransomware didn't corrupt the file),
				// or if we don't have a signature (didn't take a snapshot): will work anyway
				if ( signature_snapshot == null || signature_snapshot.equals( signature ) ) {

					// split with sss
					OriginalFile original_file = sss.getSplitFile(filePath, Security.hash_fromBase64(signature));

					// try to write all shards
					int shards_created = 0;
					byte[] shard_content;
					while(true) {

						shard_content = original_file.iterateShardContent();
						if(shard_content == null)
							break;

						Path shard_path = IO.createUniqueFile(secure_random, filePath, true);

						lock.writeLock().lock();
						tasks.add(new WriteFileTask(shard_path, shard_content, floodMethod()));
						lock.writeLock().unlock();
						shards_created++;
					}

					// remove original file, if created enough shards
					if(remove_originals && shards_created >= sss.k) {
						lock.writeLock().lock();
						tasks_single_use.add(new RemoveFileTask(filePath, floodMethod()));
						lock.writeLock().unlock();
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
							"Error in using signatures algorithm " + e.getMessage() );
		} catch ( InvalidOriginalFileException e ) {
			// it just means it's a shard and won't be split again
			// don't log, as there could be a lot of logs, and IO is very expensive
		} catch ( IllegalArgumentException e ) {
			e.printStackTrace();
			throw new FlooderException(
							"Snapshot's signature was not in base 64: " + e.getMessage());
		}
	}

}
