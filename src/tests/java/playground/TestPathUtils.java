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

package playground;

import com.oblac.nomen.Nomen;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.file.PathVisitor;
import org.apache.commons.io.filefilter.*;
import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.FlooderException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TestPathUtils {

	public static void main( String[] args ) throws FlooderException, IOException {
		Path root = Path.of( "/Users/thesave/Desktop/test_site" );
		List< String > exclude = List.of( "Application Data" );
		Files.walkFileTree( root, new FileVisitor< Path >() {
			@Override
			public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
				if( exclude.contains( dir.getFileName().toString() ) ){
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					return FileVisitResult.CONTINUE;
				}
			}

			@Override
			public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
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


	}

}
