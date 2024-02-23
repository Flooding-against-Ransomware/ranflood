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

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.*;
import org.ranflood.daemon.Ranflood;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.Snapshooter;
import org.ranflood.daemon.flooders.SnapshotException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

public class OnTheFlySnapshooter extends Snapshooter {

	private static final FloodMethod METHOD = FloodMethod.ON_THE_FLY;
	private static final OnTheFlySnapshooter INSTANCE = new OnTheFlySnapshooter();
	private final Environment signaturesDatabase;
	private final Set< String > exclusionList;

	private OnTheFlySnapshooter() {
		signaturesDatabase = Environments
						.newInstance( Ranflood.daemon().onTheFlyFlooder().snapshotDBPath().toFile() );
		exclusionList = Ranflood.daemon().onTheFlyFlooder().exclusionList();
	}

	static void takeSnapshot( Path filePath ) throws SnapshotException {
		log( "Taking ON_THE_FLY snapshot " + filePath );
		File file = filePath.toFile();
		if ( file.exists() && file.isDirectory() && !Files.isSymbolicLink( filePath ) ) {
			INSTANCE.signaturesDatabase.executeInExclusiveTransaction( t -> {
				final Store targetDB = INSTANCE.signaturesDatabase
								.openStore( file.getAbsolutePath(), StoreConfig.WITHOUT_DUPLICATES, t );
				recordSignatures( filePath, targetDB, t );
			} );
			log( "Terminated recording of ON_THE_FLY snapshot " + filePath );
		} else {
			throw new SnapshotException( "Could not take " + METHOD + " snapshot of non-existent or single files, filepath " + filePath.toAbsolutePath() );
		}
	}

	static void removeSnapshot( Path filePath ) {
		String key = filePath.toAbsolutePath().toString();
		INSTANCE.signaturesDatabase.executeInExclusiveTransaction( t ->
						INSTANCE.signaturesDatabase.removeStore( key, t )
		);
	}

	static List< Path > listSnapshots() {
		final LinkedList< Path > l = new LinkedList<>();
		INSTANCE.signaturesDatabase.executeInExclusiveTransaction( t ->
						INSTANCE.signaturesDatabase.getAllStoreNames( t ).forEach( s -> l.add( Path.of( s ) ) )
		);
		return l;
	}

	static private void recordSignatures( Path filepath, Store db, Transaction transaction ) {
		File folder = filepath.toFile();
		try {
			Arrays.stream( Objects.requireNonNull( folder.listFiles() ) )
							.filter( File::canRead )
							.filter( f -> !Files.isSymbolicLink( f.toPath() ) )
							.forEach( f -> {
								if ( f.isFile() ) {
									try {
										db.put( transaction,
														StringBinding.stringToEntry( f.getAbsolutePath() ),
														StringBinding.stringToEntry( OnTheFlySnapshooter.getFileSignature( f.toPath() ) )
										);
									} catch ( IOException | NoSuchAlgorithmException e ) {
										error( "An error occurred when taking the signature for "
														+ METHOD + " of " + f.getAbsolutePath()
														+ " : " + e.getMessage()
										);
									}
								}
								if ( f.isDirectory() && !INSTANCE.exclusionList.contains( f.getName() ) ) {
									recordSignatures( f.toPath(), db, transaction );
								}
							} );
		} catch ( Exception e ){
			error( "An error occurred when taking the signature for "
							+ METHOD + " of some files in " + filepath
							+ " : " + e.getMessage()
			);
		}

	}

	static String getSnapshot( Path snapshotParent, Path filepath ) throws SnapshotException {
		if ( filepath.toFile().isDirectory() ) {
			throw new SnapshotException( "Snapshots correspond only to files, passed directory " + filepath.toAbsolutePath() );
		} else {
			String dbKey = snapshotParent.toAbsolutePath().toString();
			String key = filepath.toAbsolutePath().toString();
			Transaction transaction = INSTANCE.signaturesDatabase.beginExclusiveTransaction();
			if ( INSTANCE.signaturesDatabase.storeExists( dbKey, transaction ) ) {
				Store db = INSTANCE.signaturesDatabase.openStore( dbKey, StoreConfig.WITHOUT_DUPLICATES, transaction );
				ByteIterable snapshot = db.get( transaction, StringBinding.stringToEntry( key ) );
				transaction.commit();
				if ( snapshot != null ) {
					return StringBinding.entryToString( snapshot );
				} else {
					throw new SnapshotException( "Could not find a signature corresponding to file: "
									+ filepath.toAbsolutePath() );
				}
			} else {
				transaction.commit();
				throw new SnapshotException( "Could not find a snapshot corresponding to the parent folder: "
								+ snapshotParent.toAbsolutePath() );
			}
		}
	}

	public static void shutdown() {
		log( "Closing the OnTheFlySnapshooter DB" );
		INSTANCE.signaturesDatabase.close();
	}

}
