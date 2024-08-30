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
import org.ranflood.common.commands.*;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.common.commands.types.RequestStatus;
import org.ranflood.daemon.binders.ZMQ_JSON_Server;
import org.ranflood.daemon.commands.BufferCommandImpl;
import org.ranflood.daemon.commands.FloodCommandImpl;
import org.ranflood.daemon.commands.SnapshotCommandImpl;
import org.ranflood.daemon.commands.VersionCommandImpl;
import org.ranflood.daemon.flooders.FloodTaskExecutor;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooder;
import org.ranflood.daemon.flooders.random.RandomFlooder;
import org.ranflood.common.utils.IniParser;
import org.ranflood.daemon.flooders.shadowCopy.ShadowCopyFlooder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class RanfloodDaemon {

	private final FloodTaskExecutor floodTaskExecutor = FloodTaskExecutor.getInstance();
	private static final ExecutorService serverExecutor = Executors.newFixedThreadPool( 1 );
	private static final ExecutorService commandExecutor = Executors.newFixedThreadPool( 2 * Runtime.getRuntime().availableProcessors() );
	private static final ExecutorService scheduler = Executors.newFixedThreadPool( 2 * Runtime.getRuntime().availableProcessors() );
	private final RandomFlooder RANDOM_FLOODER;
	private final OnTheFlyFlooder ON_THE_FLY_FLOODER;
	private final ShadowCopyFlooder SHADOW_COPY_FLOODER;
	static private Emitter< Runnable > emitter;

	private static final ScheduledExecutorService bufferScheduler = Executors.newScheduledThreadPool(1);

	private HttpServer httpServer;

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
		httpServer.createContext("/status", new StatusHandler());
		httpServer.createContext("/command", new CommandHandler());
		httpServer.setExecutor(Executors.newFixedThreadPool(2)); // or use another ExecutorService
		httpServer.start();
		log("HTTP server started on port " + port);
	}

	static class StatusHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String response = "Server is running";
			exchange.sendResponseHeaders(200, response.getBytes().length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.getBytes());
			}
		}
	}

	static class CommandHandler implements HttpHandler {

		public CommandHandler() {
			long expirationTimeInSeconds = 3600;
			long cleanupInterval = 10;

			bufferScheduler.scheduleAtFixedRate(() -> {
				RequestsLogBuffer.cleanUpExpiredRequests(expirationTimeInSeconds);
				log("Cleaned up expired requests from buffer");
			}, cleanupInterval, cleanupInterval, TimeUnit.MINUTES);
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("OPTIONS".equals(exchange.getRequestMethod())) {
				handleCors(exchange);
			} else if ("POST".equals(exchange.getRequestMethod())) {
				String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				UUID id = UUID.randomUUID();

				Ranflood.daemon().executeCommand(() -> {
					log("HTTP server received [" + request + "]");
					try {
						Command< ? > command = bindToImpl( JSONTranscoder.fromJson( request ) );

						if ( command.isAsync() ) {
							RequestsLogBuffer.addRequest(id, command); //aggiungiamo al buffer solo le richieste async, le richieste sincrone contengon l'esito nella risposta
							Ranflood.daemon().executeCommand( () -> {
								Object result = command.execute(id);
								if ( result instanceof CommandResult.Successful ) {
									log( ( ( CommandResult.Successful ) result ).message());
									RequestsLogBuffer.updateStatus(id, "success");
								} else {
									error( ( ( CommandResult.Failed ) result ).message());
									RequestsLogBuffer.updateStatus(id, "error");
									RequestsLogBuffer.setErrorMsg(id, ( ( CommandResult.Failed ) result ).message());
								}
							} );
							sendResponse( exchange, 200, "{\"id\": \"" + id.toString() + "\"}");
						} else {
							if ( command instanceof  VersionCommand.Get) {
								String version = ( ( VersionCommandImpl.Get ) command ).execute(id);
								sendResponse( exchange, 200, version);
							}
							else if ( command instanceof  BufferCommand.Get){
								RequestStatus status = ( ( BufferCommand.Get ) command ).execute(id);
								if (status == null) {
									sendResponse(exchange, 400, "Request not found");
								}
								else {

									sendResponse(exchange, 200, JSONTranscoder.requestStatusToJson(status));
								}
							}
							else {
								List< ? extends RanfloodType> l =
										( command instanceof SnapshotCommand.List ) ?
												( ( SnapshotCommandImpl.List ) command ).execute(id)
												: ( ( FloodCommandImpl.List ) command ).execute(id);
								sendResponse( exchange, 200, JSONTranscoder.wrapListRanfloodType( l ));
							}
						}
					} catch (ParseException e) {
						error(e.getMessage());
                        try {
                            sendResponse(exchange, 400, JSONTranscoder.wrapError(e.getMessage()));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } catch (Exception e) {
						error(e.getMessage());
                        try {
                            sendResponse(exchange, 500, e.getMessage());
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
				});
			} else {
				sendResponse(exchange, 405, "Method Not Allowed");
			}
		}

		private void handleCors(HttpExchange exchange) throws IOException {
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
			exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
			exchange.sendResponseHeaders(204, -1);
		}

		private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	private static Command< ? > bindToImpl(Command< ? > command ) {
		if ( command instanceof SnapshotCommand.Add ) {
			return new SnapshotCommandImpl.Add( ( ( SnapshotCommand.Add ) command ).type() );
		}
		if ( command instanceof SnapshotCommand.Remove ) {
			return new SnapshotCommandImpl.Remove( ( ( SnapshotCommand.Remove ) command ).type() );
		}
		if ( command instanceof SnapshotCommand.List ) {
			return new SnapshotCommandImpl.List();
		}
		if ( command instanceof FloodCommand.Start ) {
			return new FloodCommandImpl.Start( ( ( FloodCommand.Start ) command ).type() );
		}
		if ( command instanceof FloodCommand.Stop ) {
			return new FloodCommandImpl.Stop(
					( ( FloodCommand.Stop ) command ).method(),
					( ( FloodCommand.Stop ) command ).id()
			);
		}
		if ( command instanceof FloodCommand.List ) {
			return new FloodCommandImpl.List();
		}
		if ( command instanceof VersionCommand.Get ) {
			return new VersionCommandImpl.Get();
		}
		if ( command instanceof BufferCommand.Get ) {
			return new BufferCommandImpl.Get(( ( BufferCommand.Get ) command ).id());
		}
		throw new UnsupportedOperationException( "" );
	}


	public void shutdown() {
		ZMQ_JSON_Server.shutdown();
		floodTaskExecutor.shutdown();
		emitter.onComplete();
		serverExecutor.shutdown();
		commandExecutor.shutdown();
		scheduler.shutdown();
		ON_THE_FLY_FLOODER.shutdown();

		bufferScheduler.shutdown();
		try {
			if (!bufferScheduler.awaitTermination(1, TimeUnit.MINUTES)) {
				bufferScheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			bufferScheduler.shutdownNow();
		}


		if (httpServer != null) {
			httpServer.stop(0);
			log("HTTP server stopped.");
		}

		System.exit( 0 );
	}

	public void start() {
		// Start the ZMQ_JSON_Server
		ZMQ_JSON_Server.start(settings.getValue("ZMQ_JSON_Server", "address").orElse(""));

		// Start the HTTP server
		try {
			startHttpServer(8080);
		} catch (IOException e) {
			error("Failed to start HTTP server: " + e.getMessage());
		}
	}
}