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
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooder;
import org.ranflood.daemon.flooders.random.RandomFlooder;
import org.ranflood.daemon.utils.IniParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class RanFloodDaemon {

	final static private Logger log = LogManager.getLogger( RanFloodDaemon.class );

	static {
		PropertyConfigurator.configure( "src/main/resources/log4j.properties" );
	}

	final private FloodTaskExecutor floodTaskExecutor = FloodTaskExecutor.getInstance();
	final private RandomFlooder RANDOM_FLOODER = new RandomFlooder();
	private OnTheFlyFlooder ON_THE_FLY_FLOODER;

	public RanFloodDaemon( Path settingsFilePath ) {
		IniParser settings = null;
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
		floodTaskExecutor.shutdown();
		ON_THE_FLY_FLOODER.shutdown();
	}

}
