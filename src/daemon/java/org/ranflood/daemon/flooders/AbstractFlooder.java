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

import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.ranflood.common.RanfloodLogger.log;

public class AbstractFlooder {

	private final ConcurrentHashMap< UUID, LabeledFloodTask > runningTasks;

	public AbstractFlooder() {
		runningTasks = new ConcurrentHashMap<>();
	}

	public UUID flood( Path targetFolder ) throws FlooderException {
		throw new UnsupportedOperationException( "Flooders should override this method" );
	}

	protected void addRunningTask( LabeledFloodTask t ) {
		runningTasks.put( t.label(), t );
	}

	public List< LabeledFloodTask > currentRunningTasksSnapshotList() {
		return new LinkedList<>( runningTasks.values() );
	}

	public void stopFlood( UUID id ) throws FlooderException {
		LabeledFloodTask task = runningTasks.remove( id );
		if ( task != null ) {
			Ranflood.daemon().floodTaskExecutor().removeTask( task );
			log( "Removed flood task: " + id );
		} else {
			throw new FlooderException( "Could not find and remove task: " + id );
		}
	}

	public void shutdown() {
	}

}