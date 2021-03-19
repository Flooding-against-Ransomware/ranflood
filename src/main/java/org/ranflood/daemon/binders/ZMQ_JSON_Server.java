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
import org.ranflood.daemon.RanFlood;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanFloodType;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

import static org.ranflood.daemon.RanFloodDaemon.error;
import static org.ranflood.daemon.RanFloodDaemon.log;

public class ZMQ_JSON_Server {

	private ZMQ_JSON_Server() {}

	private final static ZContext context = new ZContext();

	public static void start( String addressParameter ) {
		final String address = ( addressParameter == null || addressParameter.isEmpty() ) ?
						"tcp://localhost:7890": addressParameter;
		ZMQ.Socket socket = context.createSocket( SocketType.REP );
		socket.bind( address );
		log( "Server started at " + address + ", accepting requests from clients" );
		RanFlood.daemon().executeCommand( () -> {
			while ( !context.isClosed() ) {
				String request = new String( socket.recv(), ZMQ.CHARSET );
				log( "Server received [" + request + "]" );
				try {
					Command< ? > command = JSONTranscoder.fromJson( request );
					if( command.isAsync() ){
						RanFlood.daemon().executeCommand( () -> {
							Object result = command.execute();
							if( result instanceof CommandResult.Successful ){
								log( ( ( CommandResult.Successful ) result ).message() );
							} else {
								error( ( ( CommandResult.Failed ) result ).message() );
							}
						} );
						socket.send( JSONTranscoder.wrapSuccess( "Command " + command.name() + " issued." ) );
					} else {
						List< ? extends RanFloodType > l =
										( command instanceof SnapshotCommand.List ) ?
														( ( SnapshotCommand.List ) command ).execute()
													: ( ( FloodCommand.List ) command ).execute();
						socket.send( JSONTranscoder.wrapListRanFloodType( l ) );
					}
				} catch ( ParseException e ) {
					error( e.getMessage() );
					socket.send( JSONTranscoder.wrapError( e.getMessage() ).getBytes( ZMQ.CHARSET ) );
					if( request.equals( "shutdown" ) ){
						error( "Cheat-code for shutdown, remove for release" );
						RanFlood.daemon().shutdown();
					}
				}
			}
		});
	}

	public static void shutdown() {
		log( "Shutting down ZMQ_JSON_Server" );
		context.close();
		context.destroy();
	}


}
