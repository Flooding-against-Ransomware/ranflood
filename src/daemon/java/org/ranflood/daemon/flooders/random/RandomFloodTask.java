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

package org.ranflood.daemon.flooders.random;

import com.oblac.nomen.Nomen;
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.tasks.FloodTask;
import org.ranflood.daemon.flooders.tasks.WriteFileTask;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class RandomFloodTask extends FloodTask {

	public RandomFloodTask( Path filePath, FloodMethod floodMethod ) {
		super( filePath, floodMethod );
	}

	private static final ArrayList< String > FILE_EXTESIONS = new ArrayList<>(
			Arrays.asList( ".doc", ".docx", ".odt", ".txt", ".pdf", ".xls", ".xlsx", ".ods",
			".ppt", ".pptx", ".jpeg", ".jps", ".gif", ".png", ".mov", ".avi",
			".mp4", ".mpeg", ".mp3", ".wav", ".ogg" )
	);
	private static final Random rng = new Random();

	@Override
	public Runnable getRunnableTask() {
		return () -> {
			byte[] content = new byte[ new Random().nextInt( Double.valueOf( Math.pow( 2, 22 ) ).intValue() ) + Double.valueOf( Math.pow( 2, 7 ) ).intValue() ];
			new Random().nextBytes( content );
			Path filePath = Path.of(
							this.filePath().toAbsolutePath() + File.separator
											+ Nomen.randomName()
											+ FILE_EXTESIONS.get( rng.nextInt( FILE_EXTESIONS.size() ) )
			);
			WriteFileTask d = new WriteFileTask( filePath, content, this.floodMethod() );
			RanFloodDaemon.executeIORunnable( d.getRunnableTask() );
		};
	}

}
