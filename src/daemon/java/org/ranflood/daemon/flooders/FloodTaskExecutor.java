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

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.RanfloodDaemon;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFloodTask;
import org.ranflood.daemon.flooders.random.RandomFloodTask;
import org.ranflood.daemon.flooders.shadowCopy.ShadowCopyFloodTask;
import org.ranflood.daemon.flooders.tasks.FileTask;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FloodTaskExecutor {

	static private final FloodTaskExecutor INSTANCE = new FloodTaskExecutor();
	static private final LinkedList< TaskStateManager > taskList = new LinkedList<>();
	private final ReentrantLock taskListLock = new ReentrantLock();

	private static class TaskStateException extends Exception {
		public TaskStateException( String message ) {
			super( message );
		}
	}

	private static class TaskStateManager {
		final private LabeledFloodTask main;
		final private List< FileTask > tasks;

		public TaskStateManager( LabeledFloodTask main ) {
			this.main = main;
			this.tasks = new LinkedList<>();
		}

		public FileTask getNextTask() throws TaskStateException {
			if ( tasks.isEmpty() ) {
				tasks.addAll( main.floodTask().getFileTasks() );
			}
			if( tasks.isEmpty() ){
				throw new TaskStateException( "Cannot load new tasks" );
			} else {
				return tasks.remove( 0 );
			}
		}

		public LabeledFloodTask main() {
			return main;
		}
	}

	public static FloodTaskExecutor getInstance() {
		return INSTANCE;
	}

	public void addTask( LabeledFloodTask t ) {
		taskListLock.lock();
		taskList.add( new TaskStateManager( t ) );
		taskListLock.unlock();
		signalExecution();
	}

	private void signalExecution() {
		RanfloodDaemon.executeIORunnable( () -> {
			taskListLock.lock();
			if ( taskList.isEmpty() ) {
				taskListLock.unlock();
			} else {
				TaskStateManager t = taskList.remove( 0 );
				taskListLock.unlock();
				try {
					FileTask ft = t.getNextTask(); // getNextTask can be IO/computation heavy
					taskListLock.lock();
					taskList.add( t );
					taskListLock.unlock();
					signalExecution(); // we launch the next iteration
					ft.getRunnableTask().run(); // we execute this FileTask
				} catch ( TaskStateException e ) {
					log( "Task " + t.main.label() + " has 0 remaining tasks, stopping it" );
					try {
						if( t.main.floodTask() instanceof OnTheFlyFloodTask ){
							Ranflood.daemon().onTheFlyFlooder().stopFlood( t.main.label() );
						} else if( t.main.floodTask() instanceof ShadowCopyFloodTask ){
							Ranflood.daemon().shadowCopyFlooder().stopFlood( t.main.label() );
						} else if( t.main.floodTask() instanceof RandomFloodTask ){
							Ranflood.daemon().randomFlooder().stopFlood( t.main.label() );
						}
					} catch ( FlooderException ex ) {
						error( ex.getMessage() );
					}
				}
			}
		} );
	}

	public void removeTask( LabeledFloodTask t ) {
		taskListLock.lock();
		taskList.removeIf( tsm -> tsm.main() == t );
		taskListLock.unlock();
	}

	public void shutdown() {
		log( "Shutting down the FloodTaskExecutor" );
//		emitter.onComplete();
//		scheduler.shutdown();
	}

}
