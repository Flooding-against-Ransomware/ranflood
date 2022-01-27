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

import static org.ranflood.common.RanFloodLogger.log;

import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.tasks.FileTask;
import org.ranflood.daemon.flooders.tasks.FloodTask;
import org.ranflood.daemon.flooders.tasks.FloodTaskGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FloodTaskExecutor {

	static private final FloodTaskExecutor INSTANCE = new FloodTaskExecutor();
	static private final LinkedList< TaskStateManager > taskList = new LinkedList<>();
	private final ReentrantReadWriteLock taskListLock = new ReentrantReadWriteLock();

	private static class TaskStateManager {
		final private FloodTaskGenerator main;
		final private List< FileTask > tasks;

		public TaskStateManager( FloodTaskGenerator main ) {
			this.main = main;
			this.tasks = new LinkedList<>();
		}

		public FileTask getNextTask() {
			if ( tasks.isEmpty() ) {
				tasks.addAll( main.getFileTasks() );
			}
			return tasks.remove( 0 );
		}

		public FloodTaskGenerator main() {
			return main;
		}
	}

	public static FloodTaskExecutor getInstance() {
		return INSTANCE;
	}

	public void addTask( FloodTaskGenerator t ) {
		taskListLock.writeLock().lock();
		taskList.add( new TaskStateManager( t ) );
		taskListLock.writeLock().unlock();
		signalExecution();
	}

	private void signalExecution() {
		RanFloodDaemon.executeIORunnable( () -> {
			taskListLock.readLock().lock();
			if ( taskList.isEmpty() ) {
				taskListLock.readLock().unlock();
			} else {
				taskListLock.readLock().unlock();
				taskListLock.writeLock().lock();
				TaskStateManager t = taskList.remove( 0 );
				taskListLock.writeLock().unlock();
				FileTask ft = t.getNextTask();
				taskListLock.writeLock().lock();
				taskList.add( t );
				taskListLock.writeLock().unlock();
				signalExecution();
				ft.getRunnableTask().run();
			}
		} );
	}

	public void removeTask( FloodTask t ) {
		taskListLock.writeLock().lock();
		taskList.removeIf( tsm -> tsm.main() == t );
		taskListLock.writeLock().unlock();
	}

	public void shutdown() {
		log( "Shutting down the FloodTaskExecutor" );
//		emitter.onComplete();
//		scheduler.shutdown();
	}

}
