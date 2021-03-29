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

import com.oblac.nomen.Nomen;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.tasks.FloodTask;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.ranflood.common.RanFloodLogger.error;

public class ShadowCopyFloodTask extends FloodTask {

	private final Path tarFilePath;

	public ShadowCopyFloodTask( Path filePath, FloodMethod floodMethod, Path tarFilePath ) {
		super( filePath, floodMethod );
		this.tarFilePath = tarFilePath;
	}

	@Override
	public Runnable getRunnableTask() {
		return () -> {
			try ( TarArchiveInputStream tarIn = new TarArchiveInputStream(
							new BufferedInputStream( new FileInputStream( tarFilePath.toFile() ) ) ) ) {
				TarArchiveEntry entry = tarIn.getNextTarEntry();
				while ( entry != null ) {
					Path file = filePath().resolve( entry.getName() );
					String originalFileName = file.getFileName().toString();
					String fileName = originalFileName.substring( 0, originalFileName.lastIndexOf( "." ) );
					String extension = originalFileName.substring( originalFileName.lastIndexOf( "." ) );
					file = file.getParent().resolve( fileName + Nomen.est().literal( "" ).adjective().get() + extension );
					if ( !file.getParent().toFile().exists() )
						// this should be synchronized if we want to have it running in parallel
						file.getParent().toFile().mkdirs();
					Files.createFile( file );
					IOUtils.copy( tarIn, new FileOutputStream( file.toFile() ) );
					entry = tarIn.getNextTarEntry();
				}
			} catch ( IOException e ) {
				error( "Could not open the archive at path " + tarFilePath.toAbsolutePath() );
			}
		};
	}

}
