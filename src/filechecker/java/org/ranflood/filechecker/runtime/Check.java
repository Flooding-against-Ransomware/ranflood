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

package org.ranflood.filechecker.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.ranflood.filechecker.runtime.Utils.getFileSignature;

public class Check {

	public static void run( File checksum, File folder, File report, Boolean deep ) throws IOException {
		if ( !Files.exists( checksum.toPath().toAbsolutePath().getParent() ) )
			throw new IOException( "could not file checksum file " + checksum.toPath() );
		if ( !Files.exists( folder.toPath() ) )
			throw new IOException( "folder " + folder + " does not exist" );
		if ( !Files.isDirectory( folder.toPath() ) )
			throw new IOException( folder + " is not a directory" );
		Map< String, String > checksumMap =
						new HashMap<>( Files.readAllLines( checksum.toPath() ).stream()
										.map( l -> l.split( "," ) )
										.collect( Collectors.toUnmodifiableMap( s -> s[ 0 ], s -> s[ 1 ] ) ) );
		// first we check if we can find all files
		Set< String > found = new HashSet<>();
		for( Map.Entry< String, String > entry : new HashMap<>( checksumMap ).entrySet() ){
			try {
				String signature = getFileSignature( folder.toPath().resolve( Path.of( entry.getKey() ) ) );
				if( signature.equals( entry.getValue() ) ){
					checksumMap.remove( entry.getKey() );
					found.add( entry.getKey() );
				}
			} catch ( IOException | NoSuchAlgorithmException ignored ) {}
		}
		// if needed, and we did not find some files, we check if we can find them with the deep search
		HashSet< String > missingSignatures = new HashSet<>( checksumMap.values() );
		if( deep && ! checksumMap.isEmpty() ){
			Files.walk( folder.toPath().toAbsolutePath() )
							.filter( f -> Files.isRegularFile( f, LinkOption.NOFOLLOW_LINKS ) )
							.filter( f -> ! found.contains( f.toString() ) )
							.forEach( f -> {
								try {
									String signature = getFileSignature( f );
									if( missingSignatures.contains( signature ) ){
										missingSignatures.remove( signature );
										found.add( folder.toPath().toAbsolutePath().relativize( f ).toString() );
									}
								} catch ( IOException | NoSuchAlgorithmException ignored ) {}
							} );
		}
		String reportContent = String.join( "\n", found );
		Files.writeString( report.toPath(), reportContent );
	}

}
