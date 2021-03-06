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

import org.ranflood.daemon.flooders.FloodMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import static org.ranflood.daemon.RanFloodDaemon.error;

public class WriteFileTask implements FileTask {
	private final Path filePath;
	private final byte[] content;
	private final FloodMethod floodMethod;

	public WriteFileTask( Path filePath, byte[] content, FloodMethod floodMethod ) {
		this.filePath = filePath;
		this.content = content;
		this.floodMethod = floodMethod;
	}

	public Path filePath() {
		return filePath;
	}

	public byte[] content() {
		return content;
	}

	public FloodMethod floodMethod() {
		return floodMethod;
	}

	public Runnable getRunnableTask() {
		return () -> {
			try {
				File parentFolder = filePath.getParent().toFile();
				if( !parentFolder.exists() ){
					synchronized ( filePath ){
						parentFolder.mkdirs();
					}
				}
				FileOutputStream f = new FileOutputStream( filePath.toAbsolutePath().toString() );
				f.write( content );
				f.close();
			} catch ( IOException e ) {
					error( e.getMessage() );
			}
		};
	}

}
