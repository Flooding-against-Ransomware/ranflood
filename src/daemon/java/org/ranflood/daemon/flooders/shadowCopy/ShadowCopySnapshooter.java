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

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.Snapshooter;
import org.ranflood.daemon.flooders.SnapshotException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

public class ShadowCopySnapshooter extends Snapshooter {

	private static final FloodMethod METHOD = FloodMethod.SHADOW_COPY;
	private final Path archiveRoot;
	private final Set< String > exclusionList;
	private final Environment archiveDatabase;
	private final static ShadowCopySnapshooter INSTANCE = new ShadowCopySnapshooter();
	private final static String archiveDatabaseName = "archives";

	private ShadowCopySnapshooter() {
		this.archiveRoot = Ranflood.daemon().shadowCopyFlooder().archiveRoot();
		this.exclusionList = Ranflood.daemon().shadowCopyFlooder().exclusionList();
		if ( !archiveRoot.toFile().exists() ) {
			try {
				Files.createDirectories( archiveRoot );
			} catch ( IOException exception ) {
				error( "Could not create the SHADOW_COPY archive folder at: " + archiveRoot + ": " + exception.getMessage() );
			}
		}
		archiveDatabase = Environments.newInstance( Ranflood.daemon().shadowCopyFlooder().archiveDatabase().toFile() );
	}

	static void takeSnapshot( Path filePath ) throws SnapshotException {
		log( "Taking SHADOW_COPY archive " + filePath );
		File file = filePath.toFile();
		if ( file.exists() && file.isDirectory() && !Files.isSymbolicLink( filePath ) ) {
			Path tarFile;
			try {
				tarFile = INSTANCE.archiveRoot.resolve( ShadowCopySnapshooter.getPathSignature( filePath ) );
			} catch ( NoSuchAlgorithmException e ) {
				throw new SnapshotException( "An error occurred when taking the signature for "
								+ METHOD + " of " + filePath.toAbsolutePath() + " : " + e.getMessage()
				);
			}
			synchronized ( METHOD ) {
				if ( tarFile.toFile().exists() ) {
					throw new SnapshotException( "Error, archive for path " + filePath + " and signature "
									+ tarFile.getFileName() + " already exists. Remove the existing one and take a new snapshot." );
				}
				try ( TarArchiveOutputStream tarOut = new TarArchiveOutputStream(
								new BufferedOutputStream( Files.newOutputStream( tarFile ) ) ) ) {
					tarOut.setLongFileMode( TarArchiveOutputStream.LONGFILE_POSIX );
					Files.walkFileTree( filePath, new FileVisitor<>() {
										@Override
										public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
											if ( INSTANCE.exclusionList.contains( dir.getFileName().toString() ) || Files.isSymbolicLink( dir ) ) {
												return FileVisitResult.SKIP_SUBTREE;
											} else {
												return FileVisitResult.CONTINUE;
											}
										}

										@Override
										public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
											if ( !Files.isSymbolicLink( file ) && file.toFile().canRead() ) {
												TarArchiveEntry e = new TarArchiveEntry( file, filePath.relativize( file ).toString() );
												try ( FileInputStream is = new FileInputStream( file.toFile() ) ) {
													tarOut.putArchiveEntry( e );
													IOUtils.copy( is, tarOut );
													tarOut.closeArchiveEntry();
												} catch ( IOException exception ) {
													error( "Could not include file " + file.toAbsolutePath() + " in the archive: " +
																	exception.getMessage()
													);
												}
											}
											return FileVisitResult.CONTINUE;
										}

										@Override
										public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException {
											return FileVisitResult.CONTINUE;
										}

										@Override
										public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
											return FileVisitResult.CONTINUE;
										}
									}
					);
					tarOut.flush();
					INSTANCE.archiveDatabase.executeInExclusiveTransaction( t -> {
						final Store targetDB = INSTANCE.archiveDatabase
										.openStore( archiveDatabaseName, StoreConfig.WITHOUT_DUPLICATES, t );
						targetDB.put( t,
										StringBinding.stringToEntry( filePath.toAbsolutePath().toString() ),
										StringBinding.stringToEntry( tarFile.getFileName().toString() )
						);
					} );
				} catch ( IOException exception ) {
					throw new SnapshotException( "Error occurred while creating the archive for " + filePath + ": " + exception.getMessage() );
				}
			}
			log( "Terminated recording of " + METHOD + " snapshot " + filePath );
		} else {
			throw new SnapshotException( "Could not take " + METHOD + " snapshot of non-existent or single files, filepath " + filePath.toAbsolutePath() );
		}
	}

	static void removeSnapshot( Path filePath ) {
		AtomicReference< Optional< String > > archiveName = new AtomicReference<>();
		INSTANCE.archiveDatabase.executeInExclusiveTransaction( t -> {
			final Store targetDB = INSTANCE.archiveDatabase
							.openStore( archiveDatabaseName, StoreConfig.WITHOUT_DUPLICATES, t );
			ByteIterable b = targetDB.get( t, StringBinding.stringToEntry( filePath.toAbsolutePath().toString() ) );
			archiveName.set( ( b == null ) ? Optional.empty() : Optional.of( StringBinding.entryToString( b ) ) );
		} );
		Path tarFile;
		try {
			if ( archiveName.get().isPresent() ) {
				tarFile = INSTANCE.archiveRoot.resolve( archiveName.get().get() );
			} else {
				error( "Could not find an entry correspondent to " + filePath + " in the archive database. " +
								"Attempting to optimistically delete unregistered copies of the archive" );
				tarFile = INSTANCE.archiveRoot.resolve( ShadowCopySnapshooter.getPathSignature( filePath ) );
			}
			synchronized ( METHOD ) {
				if ( tarFile.toFile().exists() ) {
					Files.delete( tarFile );
					if ( archiveName.get().isPresent() ) {
						INSTANCE.archiveDatabase.executeInExclusiveTransaction( t -> {
							final Store targetDB = INSTANCE.archiveDatabase
											.openStore( archiveDatabaseName, StoreConfig.WITHOUT_DUPLICATES, t );
							targetDB.delete( t, StringBinding.stringToEntry( filePath.toAbsolutePath().toString() ) );
						} );
					}
					log( "Snapshot " + METHOD + " of " + filePath + " deleted" );
				} else {
					error( "Issued the removal of " + METHOD + " snapshot for " + filePath + " but no copy was found." );
				}
			}
		} catch ( IOException | NoSuchAlgorithmException e ) {
			error( "Could not take obtain the signature for " + filePath + ": " + e.getMessage() );
		}
	}

	static List< Path > listSnapshots() {
		final LinkedList< Path > l = new LinkedList<>();
		INSTANCE.archiveDatabase.executeInExclusiveTransaction( t -> {
			final Store targetDB = INSTANCE.archiveDatabase
							.openStore( archiveDatabaseName, StoreConfig.WITHOUT_DUPLICATES, t );
			Cursor c = targetDB.openCursor( t );
			while ( c.getNext() ) {
				l.add( Path.of( StringBinding.entryToString( c.getKey() ) ) );
			}
		} );
		return l;
	}

	static Path getSnapshotArchivePath( Path filePath ) throws SnapshotException {
		AtomicReference< Optional< String > > archiveName = new AtomicReference<>();
		INSTANCE.archiveDatabase.executeInTransaction( t -> {
			final Store targetDB = INSTANCE.archiveDatabase
							.openStore( archiveDatabaseName, StoreConfig.WITHOUT_DUPLICATES, t );
			ByteIterable b = targetDB.get( t, StringBinding.stringToEntry( filePath.toAbsolutePath().toString() ) );
			archiveName.set( ( b == null ) ? Optional.empty() : Optional.of( StringBinding.entryToString( b ) ) );
		} );
		if ( archiveName.get().isPresent() ) {
			return INSTANCE.archiveRoot.resolve( archiveName.get().get() );
		} else {
			throw new SnapshotException( "Could not find an entry correspondent to " + filePath + " in the archive database. " );
		}
	}

	public static void shutdown() {
		log( "Closing the " + METHOD + " ArchiveDatabase DB" );
		INSTANCE.archiveDatabase.close();
	}

}
