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

package org.daemon.flooders;

import org.daemon.flooders.tasks.Task;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TasksExecutor {

	private final CopyOnWriteArrayList< Task > taskList;
	private final ExecutorService executors;
	private final ExecutorService scheduler;
	private final AtomicBoolean POISON_PILL;

	private TasksExecutor() {
		this.taskList = new CopyOnWriteArrayList<>();
		this.executors = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
		final Random rgn = new Random();
		POISON_PILL = new AtomicBoolean( true );
		scheduler = Executors.newSingleThreadExecutor();
		scheduler.submit( () -> {
			while ( POISON_PILL.get() ) {
				if ( taskList.size() > 0 ) {
					Task wt = taskList.get( rgn.nextInt( taskList.size() ) );
					executors.submit( wt.getCallableTask() );
				}
			}
		});
	}

	public static TasksExecutor getInstance(){
		return new TasksExecutor();
	}

	// TODO: this can be optimised to batch-collect multiple tasks within a given time window
	// and use taskBin.addAll( collectedTasks ) for batch stop-the-world load
	public void addTask( Task t ) {
		taskList.add( t );
	}

	// TODO: this can be optimised to batch-collect multiple tasks within a given time window
	// and use taskBin.removeAll( collectedTasks ) for batch stop-the-world removal
	public void removeTask( UUID id ){
		for( int i = 0; i < taskList.size(); i++ ){
			if( id.equals( taskList.get( i ).id() ) ){
				taskList.remove( i );
				return;
			}
		}
	}

	public ArrayList< Task > getTaskList() {
		return new ArrayList<>( taskList );
	}

	public void shutdown() {
		POISON_PILL.set( false );
		scheduler.shutdown();
		executors.shutdown();
	}

}
