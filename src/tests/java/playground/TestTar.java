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
import jetbrains.exodus.util.IOUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.FlooderException;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.UUID;

public class TestTar {

	public static void main( String[] args ) throws FlooderException, IOException {
		Path root = Path.of( "/Users/thesave/Desktop/ranflood_testsite/" );
		Path tarTarget = root.resolve( "attackedFolder" );
		Path tarFile = root.resolve( "archive" ).resolve( "attackedFolder.tar" );
		if ( !tarFile.getParent().toFile().exists() ) {
			Files.createDirectories( tarFile.getParent() );
		}

		try ( TarArchiveOutputStream tarOut =
									new TarArchiveOutputStream(
													new BufferedOutputStream(
																	Files.newOutputStream( tarFile ) )
									) ) {
			long start = System.nanoTime();
			Files.walk( tarTarget )
							.map( Path::toFile )
							.filter( file -> !file.isDirectory() )
							.forEach( f -> {
								TarArchiveEntry e = new TarArchiveEntry( f, root.relativize( f.toPath() ).toString() );
								try ( FileInputStream is = new FileInputStream( f ) ) {
									tarOut.putArchiveEntry( e );
									IOUtils.copy( is, tarOut );
									tarOut.closeArchiveEntry();
								} catch ( IOException exception ) {
									exception.printStackTrace();
								}
							} );
//			tarOut.close();
			System.out.println( "Done, " + ( System.nanoTime() - start ) );
		}

		untarArchive( root, tarFile );
		untarArchive( root, tarFile );

	}

	private static void untarArchive( Path root, Path tarFile ) {
		try ( TarArchiveInputStream tarIn = new TarArchiveInputStream(
						new BufferedInputStream( new FileInputStream( tarFile.toFile() ) ) ) ) {
			TarArchiveEntry entry = tarIn.getNextTarEntry();
			while ( entry != null ) {
				Path filePath = root.resolve( "archive" ).resolve( entry.getName() );
				String originalFileName = filePath.getFileName().toString();
				String fileName = originalFileName.substring( 0, originalFileName.lastIndexOf( "." ) );
				String extension = originalFileName.substring( originalFileName.lastIndexOf( "." ) );
				filePath = filePath.getParent().resolve( fileName + Nomen.est().literal( "" ).adjective().get() + extension );
				if ( !filePath.getParent().toFile().exists() )
					// this should be synchronized if we want to have it running in parallel
					filePath.getParent().toFile().mkdirs();
				Files.createFile( filePath );
				IOUtils.copy( tarIn, new FileOutputStream( filePath.toFile() ) );
				entry = tarIn.getNextTarEntry();
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	private static void generateRandomFolderStructure() {
		RanFlood.main( TestCommons.getArgs() );
		RanFloodDaemon daemon = RanFlood.daemon();
		Path root = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/" );
		Arrays.asList( "folder1", "folder1/folder2", "folder3", "folder4" )
						.forEach( p -> randomFlood( daemon, root.resolve( p ), 250 ) );
		daemon.shutdown();
	}

	private static void randomFlood( RanFloodDaemon daemon, Path target, long timeout ) {
		UUID id = daemon.randomFlooder().flood( target );
		try {
			Thread.sleep( timeout );
			daemon.randomFlooder().stopFlood( id );
		} catch ( InterruptedException | FlooderException e ) {
			e.printStackTrace();
		}
	}

	private static Path copyFilePath( File f ) {
		String originalFileName = f.getName();
		String fileName = originalFileName.substring( 0, originalFileName.lastIndexOf( "." ) );
		String extension = originalFileName.substring( originalFileName.lastIndexOf( "." ) );
		return f.toPath().getParent().resolve( File.separator
						+ fileName
						+ Nomen.est().literal( "" ).adjective().get()
						+ extension );
	}

}
