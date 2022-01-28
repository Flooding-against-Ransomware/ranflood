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

package org.ranflood.daemon.flooders.tasks;

import com.oblac.nomen.Nomen;
import org.ranflood.common.FloodMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.ranflood.common.RanFloodLogger.error;

public class WriteCopyFileTask extends WriteFileTask {
	public WriteCopyFileTask( Path filePath, byte[] content, FloodMethod floodMethod ) {
		super( filePath, content, floodMethod );
	}

	@Override
	public Runnable getRunnableTask() {
		return () -> {
			try {
				File parentFolder = filePath().getParent().toFile();
				if ( !parentFolder.exists() ) {
					parentFolder.mkdirs();
				}
				String originalFileName = filePath().getFileName().toString();
				int extensionIndex = originalFileName.indexOf( "." );
				extensionIndex = extensionIndex >= 0 ? extensionIndex : originalFileName.length();
				String fileName = originalFileName.substring( 0, extensionIndex );
				String extension = originalFileName.substring( extensionIndex );
				String copyFilePath = filePath().getParent().toAbsolutePath()
								+ File.separator
								+ fileName
								+ Nomen.est().literal( "" ).adjective().get()
								+ extension;
				FileOutputStream f = new FileOutputStream( copyFilePath );
				f.write( content() );
				f.close();
			} catch ( IOException e ) {
				error( e.getMessage() );
			}
		};
	}
}
