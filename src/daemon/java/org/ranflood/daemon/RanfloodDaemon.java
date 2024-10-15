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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.ranflood.daemon.binders.WebSocketServer;
import org.ranflood.daemon.binders.ZMQ_JSON_Server;
import org.ranflood.daemon.flooders.FloodTaskExecutor;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooder;
import org.ranflood.daemon.flooders.random.RandomFlooder;
import org.ranflood.common.utils.IniParser;
import org.ranflood.daemon.flooders.shadowCopy.ShadowCopyFlooder;
import org.ranflood.daemon.binders.HttpServer.CommandHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

import com.sun.net.httpserver.HttpServer;

public class RanfloodDaemon {

	private final FloodTaskExecutor floodTaskExecutor = FloodTaskExecutor.getInstance();
	private static final ExecutorService serverExecutor = Executors.newFixedThreadPool( 1 );
	private static final ExecutorService commandExecutor = Executors.newFixedThreadPool( 2 * Runtime.getRuntime().availableProcessors() );
	private static final ExecutorService scheduler = Executors.newFixedThreadPool( 2 * Runtime.getRuntime().availableProcessors() );
	private final RandomFlooder RANDOM_FLOODER;
	private final OnTheFlyFlooder ON_THE_FLY_FLOODER;
	private final ShadowCopyFlooder SHADOW_COPY_FLOODER;
	static private Emitter< Runnable > emitter;

	private HttpServer httpServer;
	private Server webSocketServer;

	static {
		Observable.< Runnable >create( e -> emitter = e )
						.toFlowable( BackpressureStrategy.BUFFER )
						.subscribeOn( Schedulers.io() )
						.observeOn( Schedulers.from( scheduler ) )
						.subscribe( Runnable::run );
	}

	IniParser settings = null;

	public RanfloodDaemon( Path settingsFilePath ) throws IOException {
		try {
			settings = new IniParser( settingsFilePath.toAbsolutePath().toString() );
		} catch ( IOException e ) {
			throw new IOException( "Cloud not find setting file at: " + settingsFilePath.toAbsolutePath() );
		}
		Optional< String > random_opt_max_size = settings.getValue( "RandomFlooder", "MaxFileSize" );
		RANDOM_FLOODER = random_opt_max_size.isEmpty() ? new RandomFlooder() : new RandomFlooder( random_opt_max_size.get() );
		Optional< String > on_the_fly_opt_signature_db = settings.getValue( "OnTheFlyFlooder", "Signature_DB" );
		Optional< String > on_the_fly_opt_exclude_folder_names = settings.getValue( "OnTheFlyFlooder", "ExcludeFolderNames" );
		ON_THE_FLY_FLOODER = new OnTheFlyFlooder(
						Path.of( on_the_fly_opt_signature_db.orElseGet( () -> {
							String signaturesDBpath = Paths.get( "" ).toAbsolutePath() + File.separator + "signatures.db";
							error( "OnTheFlyFlooder -> Signature_DB not found in the settings file. Using " + signaturesDBpath );
							return signaturesDBpath;
						} ) ),
						Arrays.stream( on_the_fly_opt_exclude_folder_names.orElse( "" ).split( "," ) ).map( String::trim ).collect( Collectors.toSet() )
		);
		Optional< String > shadow_copy_opt_archive_root = settings.getValue( "ShadowCopyFlooder", "ArchiveRoot" );
		Optional< String > shadow_copy_opt_archive_database = settings.getValue( "ShadowCopyFlooder", "ArchiveDatabase" );
		Optional< String > shadow_copy_opt_exclude_folder_names = settings.getValue( "ShadowCopyFlooder", "ExcludeFolderNames" );
		SHADOW_COPY_FLOODER = new ShadowCopyFlooder(
						Path.of( shadow_copy_opt_archive_root.orElseGet( () -> {
							String archiveRoot = Paths.get( "" ).toAbsolutePath() + File.separator + "archive";
							error( "ShadowCopyFlooder -> ArchiveRoot not found in the settings file. Using " + archiveRoot );
							return archiveRoot;
						} ) ),
						Path.of( shadow_copy_opt_archive_database.orElseGet( () -> {
							String archiveRoot = Paths.get( "" ).toAbsolutePath() + File.separator + "archive.db";
							error( "ShadowCopyFlooder -> ArchiveDatabase not found in the settings file. Using " + archiveRoot );
							return archiveRoot;
						} ) ),
						Arrays.stream( shadow_copy_opt_exclude_folder_names.orElse( "" ).split( "," ) ).map( String::trim ).collect( Collectors.toSet() )
		);
		log( "Ranflood Daemon (ranfloodd) version " + Ranflood.version() + " started." );
	}

	public static void executeIORunnable( Runnable r ) {
		emitter.onNext( r );
	}

	public void executeCommand( Runnable r ) {
		commandExecutor.submit( r );
	}

	public void executeServer( Runnable r ){
		serverExecutor.submit( r );
	}

	public FloodTaskExecutor floodTaskExecutor() {
		return floodTaskExecutor;
	}

	public RandomFlooder randomFlooder() {
		return RANDOM_FLOODER;
	}

	public OnTheFlyFlooder onTheFlyFlooder() {
		return ON_THE_FLY_FLOODER;
	}

	public ShadowCopyFlooder shadowCopyFlooder() {
		return SHADOW_COPY_FLOODER;
	}

	public void startHttpServer(int port) throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		httpServer.createContext("/command", new CommandHandler());
		httpServer.setExecutor(Executors.newFixedThreadPool(2)); // or use another ExecutorService
		httpServer.start();
		log("HTTP server started on port " + port);
	}

	public void startWebsocketServer(int port) throws IOException {
		webSocketServer = new Server(port);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		webSocketServer.setHandler(context);

		JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, serverContainer) -> {
			serverContainer.addEndpoint(WebSocketServer.class);

			serverContainer.setDefaultMaxSessionIdleTimeout(600000); // 10 minute
		});

		try{
			webSocketServer.start();
			webSocketServer.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void shutdown() {
		ZMQ_JSON_Server.shutdown();
		floodTaskExecutor.shutdown();
		emitter.onComplete();
		serverExecutor.shutdown();
		commandExecutor.shutdown();
		scheduler.shutdown();
		ON_THE_FLY_FLOODER.shutdown();

		if (httpServer != null) {
			httpServer.stop(0);
			log("HTTP server stopped.");
		}
		if (webSocketServer != null) {
			try{
				webSocketServer.stop();
				log("HTTP server stopped.");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		System.exit( 0 );
	}

	public void start() {
		// Start the ZMQ_JSON_Server
		ZMQ_JSON_Server.start(settings.getValue("ZMQ_JSON_Server", "address").orElse(""));

		// Start the HTTP server
		try {
			startHttpServer(8081);
		} catch (IOException e) {
			error("Failed to start HTTP server: " + e.getMessage());
		}

		// Start WebSocket server
		try {
			startWebsocketServer(8080);
		} catch (IOException e) {
			error("Failed to start websocket server: " + e.getMessage());
		}

	}
}