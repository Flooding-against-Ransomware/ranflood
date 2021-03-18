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

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.flooders.FloodMethod;
import org.ranflood.daemon.flooders.Snapshooter;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.ranflood.daemon.RanFloodDaemon.error;
import static org.ranflood.daemon.RanFloodDaemon.log;

public class OnTheFlySnapshooter implements Snapshooter {

	private static final FloodMethod METHOD = FloodMethod.ON_THE_FLY;
	private static final OnTheFlySnapshooter INSTANCE = new OnTheFlySnapshooter();
	private final DB signaturesDatabase;

	private OnTheFlySnapshooter() {
		signaturesDatabase = DBMaker
						// TODO: take the DB path from the daemon's settings
						.fileDB( RanFlood.getDaemon().getOnTheFlyFlooder().snapshotDBPath().toFile() )
						.make();
	}

	static void takeSnapshot( Path filePath ) {
		log( "Taking snapshopt " + filePath );
		File file = filePath.toFile();
		if ( file.isDirectory() && file.exists() ) {
			Map< String, String > targetDB = INSTANCE.signaturesDatabase.hashMap( file.getAbsolutePath() )
							.keySerializer( Serializer.STRING )
							.valueSerializer( Serializer.STRING )
							.createOrOpen();
			recordSignatures( filePath, targetDB );
			INSTANCE.signaturesDatabase.commit();
		} else {
			error( "Could not take " + METHOD + " snapshot of non-existent or single files, filepath " + filePath.toAbsolutePath() );
		}
	}

	static void removeSnapshot( Path filePath ) {
		String key = filePath.toAbsolutePath().toString();
		INSTANCE.signaturesDatabase.getAll().remove( key );
	}

	static List< Path > listSnapshots(){
		return INSTANCE.signaturesDatabase.getAll().keySet().stream()
						.map( Path::of ).collect( Collectors.toList());
	}

	static private void recordSignatures( Path filepath, Map< String, String > db ) {
		File folder = filepath.toFile();
		Arrays.stream( Objects.requireNonNull( folder.listFiles() ) )
						.parallel()
						.forEach( f -> {
							try {
								db.put( f.getAbsolutePath(), getFileSignature( f.toPath() ) );
							} catch ( IOException | NoSuchAlgorithmException e ) {
								error( "An error occurred when taking the signature for "
												+ METHOD + " of " + f.getAbsolutePath()
												+ " : " + e.getMessage()
								);
							}
						} );
	}

	static String getSnapshot( Path snapshotParent, Path filepath ) throws SnapshotException {
		if ( filepath.toFile().isDirectory() ) {
			throw new SnapshotException( "Snapshots correspond only to files, passed directory " + filepath.toAbsolutePath().toString() );
		} else {
			String dbKey = snapshotParent.toAbsolutePath().toString();
			String key = filepath.toAbsolutePath().toString();
			if( INSTANCE.signaturesDatabase.get( dbKey ) != null ){
				Map< String, String > db = INSTANCE.signaturesDatabase.hashMap( dbKey )
								.keySerializer( Serializer.STRING )
								.valueSerializer( Serializer.STRING )
								.open();
				if( db.containsKey( key ) ){
					return db.get( key );
				} else {
					throw new SnapshotException( "Could not find a signature corresponding to file: "
									+ filepath.toAbsolutePath().toString() );
				}
			} else {
				throw new SnapshotException( "Could not find a snapshot corresponding to the parent folder: "
								+ snapshotParent.toAbsolutePath().toString() );
			}
		}
	}

	public static String getFileSignature( Path filePath ) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = new FileInputStream( filePath.toFile() ) ) {
			byte[] bytes = input.readAllBytes();
			input.close();
			MessageDigest digest = MessageDigest.getInstance( "MD5" );
			digest.update( bytes );
			return Base64.getEncoder().encodeToString( digest.digest() );
		}
	}

	public static void shutdown(){
		INSTANCE.signaturesDatabase.close();
	}

}
