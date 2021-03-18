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
import org.ranflood.daemon.binders.ZMQ_JSON_Server;
import org.ranflood.daemon.flooders.FloodTaskExecutor;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooder;
import org.ranflood.daemon.flooders.random.RandomFlooder;
import org.ranflood.daemon.utils.IniParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RanFloodDaemon {

	final static private Logger log = LogManager.getLogger( RanFloodDaemon.class );

	static {
		PropertyConfigurator.configure( "src/main/resources/log4j.properties" );
	}

	private final FloodTaskExecutor floodTaskExecutor = FloodTaskExecutor.getInstance();
	private final ExecutorService commandExecutor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
	private final RandomFlooder RANDOM_FLOODER = new RandomFlooder();
	private final OnTheFlyFlooder ON_THE_FLY_FLOODER;
	IniParser settings = null;

	public RanFloodDaemon( Path settingsFilePath ) {
		try {
			settings = new IniParser( settingsFilePath.toAbsolutePath().toString() );
		} catch ( IOException e ) {
			error( "Cloud not find setting file at: " + settingsFilePath.toAbsolutePath() );
		}
		Optional< String > opt = ( ( settings != null )
						? settings.getValue( "OnTheFlyFlooder", "Signature_DB" )
						: Optional.empty()
		);
		ON_THE_FLY_FLOODER = new OnTheFlyFlooder( Path.of( opt.orElseGet( () -> {
					String signaturesDBpath = Paths.get( "" ).toAbsolutePath().toString() + File.separator + "signatures.db";
					error( "OnTheFlyFlooder -> Signature_DB not found in the settings file. Using " + signaturesDBpath );
					return signaturesDBpath;
				} )
			)
		);
	}

	// TODO: check the Flowable API whether there's a more efficient way to feed runnables to the subscribers
	public static void executeIORunnable( Runnable r ) {
		Flowable.fromRunnable( r )
						.subscribeOn( Schedulers.io() )
						.subscribe();
	}

	public void executeCommand( Runnable r ) {
		commandExecutor.submit( r );
	}

	public static void log( String s ) {
		log.info( s );
	}

	public static void error( String s ) {
		log.error( s );
	}

	public FloodTaskExecutor floodTaskExecutor() {
		return floodTaskExecutor;
	}

	public RandomFlooder getRandomFlooder() {
		return RANDOM_FLOODER;
	}

	public OnTheFlyFlooder getOnTheFlyFlooder() {
		return ON_THE_FLY_FLOODER;
	}

	public void shutdown() {
		log( "Shutting down the flood task executor" );
		ZMQ_JSON_Server.stop();
		floodTaskExecutor.shutdown();
		commandExecutor.shutdown();
		ON_THE_FLY_FLOODER.shutdown();
	}

	public void start() {
		ZMQ_JSON_Server.start( settings.getValue( "ZMQ_JSON_Server", "address" ).orElse( "" ) );
	}
}
