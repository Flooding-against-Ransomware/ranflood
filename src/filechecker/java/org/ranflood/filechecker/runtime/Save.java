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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.ranflood.filechecker.runtime.Utils.getFileSignature;

public class Save {

	public static void run( File checksum, File folder ) throws IOException {
		Map< String, String > report = new HashMap<>();
		if ( ! Files.exists( folder.toPath() ) )
			throw new IOException( "folder " + folder + " does not exist" );
		if ( ! Files.isDirectory( folder.toPath() ) )
			throw new IOException( folder + " is not a directory" );
		if( ! Files.exists( checksum.toPath().toAbsolutePath() ) )
			throw new IOException( "path " + checksum.toPath().getParent() + " must exist to save the checksum file" );
		Files.walk( folder.toPath().toAbsolutePath() )
						.filter( f -> Files.isRegularFile( f, LinkOption.NOFOLLOW_LINKS ) )
						.forEach( f -> {
			try {
				report.put( folder.toPath().toAbsolutePath().relativize( f.toAbsolutePath() ).toString(), getFileSignature( f ) );
			} catch ( IOException | NoSuchAlgorithmException e ) {
				System.err.println( "Problem processing file: " + f + ", " + e.getMessage() );
			}
		} );
		String reportContent = report.entrySet().stream()
						.map( e -> e.getKey() + "," + e.getValue() )
						.collect( Collectors.joining( "\n" ) );
		Files.writeString( checksum.toPath(), reportContent );
	}

}
