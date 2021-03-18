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

import static org.ranflood.daemon.RanFloodDaemon.log;

import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.tasks.FloodTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloodTaskExecutor {

	private final LinkedBlockingQueue< FloodTask > floodTaskList;
	private final ExecutorService scheduler;
	private final AtomicBoolean POISON_PILL;
	static private final FloodTaskExecutor INSTANCE = new FloodTaskExecutor();

	private FloodTaskExecutor() {
		this.floodTaskList = new LinkedBlockingQueue<>();
		POISON_PILL = new AtomicBoolean( true );
		scheduler = Executors.newSingleThreadExecutor();
		scheduler.submit( () -> {
			while ( POISON_PILL.get() ) {
				floodTaskList.forEach( t -> {
//					log( "Issuing execution of FloodTask " + t.hashCode() );
					RanFloodDaemon.executeIORunnable( t.getRunnableTask() );
				});
			}
		});
	}

	public static FloodTaskExecutor getInstance(){
		return INSTANCE;
	}

	public void addTask( FloodTask t ) {
		floodTaskList.add( t );
	}

	public void removeTask( FloodTask t ){
		floodTaskList.remove( t );
	}

	public void shutdown() {
		log( "Shutting down the FloodTaskExecutor" );
		POISON_PILL.set( false );
		scheduler.shutdown();
	}

}
