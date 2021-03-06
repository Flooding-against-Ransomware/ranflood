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

package org.ranflood.daemon.flooders;

import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.ranflood.daemon.RanFloodDaemon.error;
import static org.ranflood.daemon.RanFloodDaemon.log;

public class AbstractFlooder {

	private final LinkedList< LabeledFloodTask > runningTasksList;

	public AbstractFlooder(){
		runningTasksList = new LinkedList<>();
	}

	protected List< LabeledFloodTask > getRunningTasksList(){
		return runningTasksList;
	}

	public List< String > getRunningTasks(){
		return runningTasksList.stream()
						.map( t ->
										t.floodTask().filePath().toAbsolutePath().toString()
										+ ", " + t.label().toString() )
						.collect( Collectors.toList() );
	}

	public UUID flood( Path targetFolder ){
		throw new UnsupportedOperationException( "Flooders should override this method" );
	};

	public void stopFlood( UUID id ) {
		Optional< LabeledFloodTask > task = getRunningTasksList().stream()
						.filter( t -> t.label().equals( id ) ).findAny();
		if( task.isPresent() ){
			log( "Removing task: " + id );
			getRunningTasksList().remove( task.get() );
			RanFlood.getDaemon().floodTaskExecutor().removeTask( task.get().floodTask() );
		} else {
			error( "Could not find and remove '" + this.getClass().getName() + "' task: " + id );
		}
	}

	public void shutdown(){}

}