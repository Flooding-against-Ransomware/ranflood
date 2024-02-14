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

import com.republicate.json.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.ranflood.filechecker.runtime.Utils.getFileSignature;

public class Check {

  public static void run( File checksum, File folder, File report, Boolean deep ) throws IOException {
    Map< String, String > reportContent = new HashMap<>();
    if ( !Files.exists( checksum.toPath().toAbsolutePath().getParent() ) )
      throw new IOException( "could not file checksum file " + checksum.toPath() );
    if ( !Files.exists( folder.toPath() ) )
      throw new IOException( "folder " + folder + " does not exist" );
    if ( !Files.isDirectory( folder.toPath() ) )
      throw new IOException( folder + " is not a directory" );
    Json jsonChecksum = Json.parse( Files.readString( checksum.toPath() ) );
    Map< String, String > checksumMap = jsonChecksum.asArray().
        stream()
        .map( e -> ( Json.Object ) e )
        .collect( Collectors.toMap(
            e -> e.get( "path" ).toString(),
            e -> e.get( "checksum" ).toString() )
        );
//		Map< String, String > checksumMap =
//						new HashMap<>( Files.readAllLines( checksum.toPath() ).stream()
//										.map( l -> l.split( "," ) )
//										.collect( Collectors.toUnmodifiableMap( s -> s[ 0 ], s -> s[ 1 ] ) ) );
    // first we check if we can find all files
    for ( Map.Entry< String, String > entry : new HashMap<>( checksumMap ).entrySet() ) {
      try {
        String signature = getFileSignature( folder.toPath().resolve( Path.of( entry.getKey() ) ) );
        if ( signature.equals( entry.getValue() ) ) {
          checksumMap.remove( entry.getKey() );
          reportContent.put( entry.getKey(), signature );
        }
      } catch ( Exception e ) {
        System.err.println( "Error '" + e.getMessage() + "' with file " + folder.toPath().resolve( Path.of( entry.getKey() ) ).toAbsolutePath() + ", skipping it." );
      }
    }
    // if needed, and we did not find some files, we check if we can find them with the deep search
    if ( deep && !checksumMap.isEmpty() ) {
      HashSet< String > missingSignatures = new HashSet<>( checksumMap.values() );
      List< Path > files = Files.walk( folder.toPath().toAbsolutePath() )
          .filter( f -> {
            try {
              return Files.isRegularFile( f, LinkOption.NOFOLLOW_LINKS ) && !reportContent.containsKey( f.toString() );
            } catch ( Exception e ) {
              System.err.println( "Problem processing file: " + f + ", " + e.getMessage() );
              return false;
            }
          } ).toList();
      for ( Path f : files ) {
        try {
          String signature = getFileSignature( f );
          if ( missingSignatures.contains( signature ) ) {
            missingSignatures.remove( signature );
            reportContent.put( folder.toPath().toAbsolutePath().relativize( f ).toString(), signature );
          }
        } catch ( Exception e ) {
          System.err.println( "Error '" + e.getMessage() + "' with file " + f.toAbsolutePath() + ", skipping it." );
        }
        if ( checksumMap.isEmpty() )
          break;
      }
    }
//			Files.walk( folder.toPath().toAbsolutePath() )
//							.filter( f -> Files.isRegularFile( f, LinkOption.NOFOLLOW_LINKS ) )
//							.filter( f -> ! found.contains( f.toString() ) )
//							.forEach( f -> {
//								try {
//									String signature = getFileSignature( f );
//									if( missingSignatures.contains( signature ) ){
//										missingSignatures.remove( signature );
//										found.add( folder.toPath().toAbsolutePath().relativize( f ).toString() + ","  );
//									}
//								} catch ( IOException | NoSuchAlgorithmException ignored ) {}
//							} );
//		}
//		}
//  String reportContentString = reportContent.entrySet().stream()
//    .map( e -> e.getKey() + "," + e.getValue() )
//    .collect( Collectors.joining( "\n" ) );
//  Files.writeString( report.toPath(), reportContentString );
    Json.Array a = new Json.Array();
    reportContent.forEach( ( key, value ) -> {
      Json.Object o = new Json.Object();
      o.put( "path", key );
      o.put( "checksum", value );
      a.add( o );
    } );
    Files.writeString( report.toPath(), a.toString() );
  }
}
