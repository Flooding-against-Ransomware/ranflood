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

package org.ranflood.daemon.binders;

import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.daemon.Ranflood;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.daemon.commands.FloodCommandImpl;
import org.ranflood.daemon.commands.SnapshotCommandImpl;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;

public class ZMQ_JSON_Server {

	private ZMQ_JSON_Server() {
	}

	private final static ZContext context = new ZContext();

	public static void start( String addressParameter ) {
		final String address = ( addressParameter == null || addressParameter.isEmpty() ) ?
						"tcp://localhost:7890" : addressParameter;
		ZMQ.Socket socket = context.createSocket( SocketType.REP );
		socket.bind( address );
		log( "Server started at " + address + ", accepting requests from clients" );
		Ranflood.daemon().executeServer( () -> {
			while ( !context.isClosed() ) {
				try {
					String request = new String( socket.recv(), ZMQ.CHARSET );
					Ranflood.daemon().executeCommand( () -> {
						log( "Server received [" + request + "]" );
						try {
							Command< ? > command = bindToImpl( JSONTranscoder.fromJson( request ) );
							if ( command.isAsync() ) {
								Ranflood.daemon().executeCommand( () -> {
									Object result = command.execute();
									if ( result instanceof CommandResult.Successful ) {
										log( ( ( CommandResult.Successful ) result ).message() );
									} else {
										error( ( ( CommandResult.Failed ) result ).message() );
									}
								} );
								socket.send( JSONTranscoder.wrapSuccess( "Command " + command.name() + " issued." ) );
							} else {
								List< ? extends RanfloodType > l =
												( command instanceof SnapshotCommand.List ) ?
																( ( SnapshotCommandImpl.List ) command ).execute()
																: ( ( FloodCommandImpl.List ) command ).execute();
								socket.send( JSONTranscoder.wrapListRanfloodType( l ) );
							}
						} catch ( ParseException e ) {
							error( e.getMessage() );
							socket.send( JSONTranscoder.wrapError( e.getMessage() ).getBytes( ZMQ.CHARSET ) );
							if ( request.equals( "shutdown" ) ) {
								error( "Cheat-code for shutdown, remove for release" );
								Ranflood.daemon().shutdown();
							}
						} catch ( Exception e ) {
							error( e.getMessage() );
							socket.send( e.getMessage().getBytes( ZMQ.CHARSET ) );
						}
					} );
				} catch ( Exception e ) {
					error( e.getMessage() );
				}
			}
		} );
	}

	private static Command< ? > bindToImpl( Command< ? > command ) {
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
		throw new UnsupportedOperationException( "" );
	}

	public static void shutdown() {
		log( "Shutting down ZMQ_JSON_Server" );
		context.close();
		context.destroy();
	}


}
