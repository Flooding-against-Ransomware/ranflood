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

package org.daemon.flooders.random;

import org.daemon.RanFloodDaemon;
import org.daemon.flooders.FloodMethod;
import org.daemon.flooders.tasks.DummyFileTask;
import org.daemon.flooders.tasks.FloodTask;

import java.nio.file.Path;
import java.util.Random;

public class RandomFloodTask extends FloodTask {

	public RandomFloodTask( Path filePath, FloodMethod floodMethod ) {
		super( filePath, floodMethod );
	}

	@Override
	public Runnable getRunnableTask() {
		return () -> {
			byte[] content = new byte[ new Random().nextInt( Double.valueOf( Math.pow( 2, 22 ) ).intValue() ) + Double.valueOf( Math.pow( 2, 7 ) ).intValue() ];
			new Random().nextBytes( content );
			// TODO: change to WriteFileTask after debugging
			DummyFileTask d = new DummyFileTask( this.filePath(), content, this.floodMethod() );
			RanFloodDaemon.fileTaskExecutor().addTask( d );
		};
	}

}
