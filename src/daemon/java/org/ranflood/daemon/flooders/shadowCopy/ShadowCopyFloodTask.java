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

package org.ranflood.daemon.flooders.shadowCopy;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.tasks.FloodTaskGenerator;
import org.ranflood.daemon.flooders.tasks.WriteCopyFileTask;
import org.ranflood.daemon.flooders.tasks.WriteFileTask;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.ranflood.common.RanfloodLogger.error;

public class ShadowCopyFloodTask extends FloodTaskGenerator {

	private final List< WriteFileTask > tasks;
	private final ReentrantReadWriteLock lock;
	private int taskListResponseRetriesCounter = 0;

	public ShadowCopyFloodTask( Path filePath, FloodMethod floodMethod, Path tarFilePath ) {
		super( filePath, floodMethod );
		lock = new ReentrantReadWriteLock();
		tasks = new LinkedList<>();
		loadWriteFileTasks( tarFilePath );
	}

	@Override
	public Runnable getRunnableTask() {
		throw new UnsupportedOperationException( "ShadowCopyFloodTask should not be run as a normal task" );
//		return () ->
//						tasks.forEach( t ->
//										RanfloodDaemon.executeIORunnable( t.getRunnableTask() )
//						);
	}

	private void loadWriteFileTasks( Path tarFilePath ) {
		Ranflood.daemon().executeCommand( () -> {
			try ( TarArchiveInputStream tarIn = new TarArchiveInputStream(
							new BufferedInputStream( new FileInputStream( tarFilePath.toFile() ) ) ) ) {
				TarArchiveEntry entry = tarIn.getNextTarEntry();
				while ( entry != null ) {
					lock.writeLock().lock();
					try {
						tasks.add( new WriteCopyFileTask(
										filePath().resolve( entry.getName() ),
										IOUtils.toByteArray( tarIn ),
										floodMethod()
						) );
					} catch ( Exception e ) {
						error( "Error copying the content of file " + entry.getName() + ", " + e.getMessage() );
					}
					lock.writeLock().unlock();
					entry = tarIn.getNextTarEntry();
				}
			} catch ( IOException e ) {
				error( "Could not open the archive at path " + tarFilePath.toAbsolutePath() );
			}
		} );
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
}
