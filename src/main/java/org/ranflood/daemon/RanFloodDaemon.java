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

package org.ranflood.daemon;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ranflood.daemon.flooders.FloodTaskExecutor;

public class RanFloodDaemon {

	static final private FloodTaskExecutor floodTaskExecutor = FloodTaskExecutor.getInstance();
	static final private Logger log = LogManager.getLogger( RanFloodDaemon.class );

	static {
		PropertyConfigurator.configure( "src/main/resources/log4j.properties" );
	}

	// TODO: check the Flowable API whether there's a more efficient way to feed runnables to the subscribers
	public static void executeIORunnable( Runnable r ){
		Flowable.fromRunnable( r )
						.subscribeOn( Schedulers.io() )
						.subscribe();
	}

	public static FloodTaskExecutor floodTaskExecutor() {
		return floodTaskExecutor;
	}

	public static void shutdown(){
		log( "Shutting down the flood task executor");
		floodTaskExecutor.shutdown();
	}

	public static void log( String s ){
		log.info( s );
	}

	public static void error( String s ){
		log.error( s );
	}

}
