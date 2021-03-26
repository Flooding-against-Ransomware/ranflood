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
import org.ranflood.daemon.flooders.tasks.FloodTask;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FloodTaskExecutor {

	private final HashSet< FloodTask > floodTaskList = new HashSet<>();
	private final ReentrantReadWriteLock taskListLock = new ReentrantReadWriteLock();
	static private final FloodTaskExecutor INSTANCE = new FloodTaskExecutor();

	public static FloodTaskExecutor getInstance(){
		return INSTANCE;
	}

	public void addTask( FloodTask t ) {
		taskListLock.writeLock().lock();
		floodTaskList.add( t );
		taskListLock.writeLock().unlock();
		launchRecursiveCallable( t );
	}

	private void launchRecursiveCallable( FloodTask t ){
		RanFloodDaemon.executeIORunnable( () -> {
			if ( hasTask( t ) ){
				launchRecursiveCallable( t );
			}
			RanFloodDaemon.executeIORunnable( t.getRunnableTask() );
		});
	}

	public boolean hasTask( FloodTask t ){
		boolean present;
		taskListLock.readLock().lock();
		present = floodTaskList.contains( t );
		taskListLock.readLock().unlock();
		return present;
	}

	public void removeTask( FloodTask t ){
		taskListLock.writeLock().lock();
		floodTaskList.remove( t );
		taskListLock.writeLock().unlock();
	}

	public void shutdown() {
		log( "Shutting down the FloodTaskExecutor" );
//		emitter.onComplete();
//		scheduler.shutdown();
	}

}
