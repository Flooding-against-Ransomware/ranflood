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

import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.FloodMethod;
import org.ranflood.daemon.flooders.Flooder;
import org.ranflood.daemon.flooders.TaskNotFoundException;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import static org.ranflood.daemon.RanFloodDaemon.log;
import static org.ranflood.daemon.RanFloodDaemon.error;

public class RandomFlooder implements Flooder {

	private final static FloodMethod METHOD = FloodMethod.RANDOM;
	private final LinkedList< LabeledFloodTask > runningTasksList;
	private final static RandomFlooder INSTANCE = new RandomFlooder();

	private RandomFlooder(){
		runningTasksList = new LinkedList<>();
	}

	private List< String > getRunningTasks(){
		return runningTasksList.stream()
						.map( t ->
										METHOD
														+ ", " + t.floodTask().filePath().toAbsolutePath().toString()
														+ ", " + t.label().toString() )
						.collect( Collectors.toList() );
	}

	public static UUID flood( Path targetFolder ){
		RandomFloodTask t = new RandomFloodTask( targetFolder, METHOD );
		UUID id = UUID.randomUUID();
		log( "Adding task: " + id );
		INSTANCE.runningTasksList.add( new LabeledFloodTask( id, t ) );
		RanFloodDaemon.floodTaskExecutor().addTask( t );
		return id;
	}

	public static void stopFlood( UUID id ) {
		Optional< LabeledFloodTask > task = INSTANCE.runningTasksList.stream()
						.filter( t -> t.label().equals( id ) ).findAny();
		if( task.isPresent() ){
			log( "Removing task: " + id );
			INSTANCE.runningTasksList.remove( task.get() );
			RanFloodDaemon.floodTaskExecutor().removeTask( task.get().floodTask() );
		} else {
			error( "Could not find and remove '" + METHOD + "' task: " + id );
		}
	}

}
