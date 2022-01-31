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

package org.ranflood.daemon.flooders.onTheFly;

import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.SnapshotException;
import org.ranflood.daemon.flooders.tasks.FloodTask;
import org.ranflood.daemon.flooders.tasks.FloodTaskGenerator;
import org.ranflood.daemon.flooders.tasks.WriteCopyFileTask;
import org.ranflood.daemon.flooders.tasks.WriteFileTask;

import static org.ranflood.common.RanFloodLogger.error;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnTheFlyFloodTask extends FloodTaskGenerator {

	private final List< WriteFileTask > tasks;

	public OnTheFlyFloodTask( Path filePath, FloodMethod floodMethod ) throws FlooderException {
		super( filePath, floodMethod );
		tasks = getWriteFileTasks( filePath(), filePath() );
	}

	@Override
	public List< WriteFileTask > getFileTasks() {
		return tasks;
	}

	@Override
	public Runnable getRunnableTask() {
		throw new UnsupportedOperationException( "OnTheFlyFloodTask should not be run as a normal task" );
//		return () ->
//						tasks.forEach( t ->
//										RanFloodDaemon.executeIORunnable( t.getRunnableTask() )
//						);
	}

	private List< WriteFileTask > getWriteFileTasks( Path parentFilePath, Path filePath ) throws FlooderException {
		File file = filePath.toFile();
		try {
			if ( file.isFile() ) {
				byte[] bytes;
				try ( InputStream input = new FileInputStream( filePath.toFile() ) ) {
					bytes = input.readAllBytes();
				}
				if ( OnTheFlySnapshooter.getBytesSignature( bytes ).equals(
								OnTheFlySnapshooter.getSnapshot( parentFilePath, filePath ) ) ) {
					return Collections.singletonList( new WriteCopyFileTask(
									filePath, bytes, floodMethod() )
					);
				}
			} else { //
				return Arrays.stream( Objects.requireNonNull( file.listFiles() ) )
								.parallel()
								.flatMap( f -> {
									try {
										return getWriteFileTasks( parentFilePath, f.toPath() ).stream();
									} catch ( FlooderException e ) {
										error( "Error in instantiating writing task for "
														+ f.toPath().toAbsolutePath() + " in " + parentFilePath.toAbsolutePath()
														+ ": " + e.getMessage()
										);
										return Stream.empty();
									}
								} )
								.collect( Collectors.toList() );
			}
		} catch ( IOException e ) {
			throw new FlooderException(
							"Could not open file or folder " + file.getAbsolutePath() + " while trying to create " + floodMethod() + " task"
			);
		} catch ( NoSuchAlgorithmException e ) {
			throw new FlooderException(
							"Error in using signatures algorithm " + e.getMessage()
			);
		} catch ( SnapshotException e ) {
			throw new FlooderException( "Could not find a snapshot of file " + file.getAbsolutePath() );
		}

		return Collections.emptyList();
	}

}
