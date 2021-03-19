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

package org.ranflood.client.binders;

import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.RanFloodType;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.ranflood.common.RanFloodLogger.*;

import static org.ranflood.common.RanFloodLogger.log;

public class ZMQ_JSON_Client {

	private static final ZMQ_JSON_Client INSTANCE = new ZMQ_JSON_Client();
	private final ZContext context;
	private final ZMQ.Socket socket;

	private ZMQ_JSON_Client() {
		String address = "tcp://localhost:7890";
		int timeout = 10_000;
		context = new ZContext();
		socket = context.createSocket( SocketType.REQ );
		log( "Connecting to address: " + address + " [HARD-CODED, MAKE PARAMETRIC]" );
		socket.connect( address );
		log( "Setting request timeout to: " + timeout + " [HARD-CODED, MAKE PARAMETRIC]" );
		socket.setReceiveTimeOut( timeout );
	}

	public static ZMQ_JSON_Client INSTANCE(){
		return INSTANCE;
	}

	public String sendCommand( SnapshotCommand.Add c ){
		return _sendCommand( c );
	}

	public String sendCommand( SnapshotCommand.Remove c ){
		return _sendCommand( c );
	}

	public String sendCommand( FloodCommand.Start c ){
		return _sendCommand( c );
	}

	public String sendCommand( FloodCommand.Stop c ){
		return _sendCommand( c );
	}

	public List< RanFloodType.Tagged > sendListCommand( FloodCommand.List c ) {
		try {
			return ( List< RanFloodType.Tagged > ) _sendList( JSONTranscoder.toJsonString( c ) );
		} catch ( ParseException e ) {
			error( e.getMessage() );
		}
		return Collections.emptyList();
	}

	public List< RanFloodType > sendListCommand( SnapshotCommand.List c ) {
		try {
			return ( List< RanFloodType > ) _sendList( JSONTranscoder.toJsonString( c ) );
		} catch ( ParseException e ) {
			error( e.getMessage() );
		}
		return Collections.emptyList();
	}

	public void shutdown() {
		socket.close();
		context.destroy();
	}

	// UTILITY METHODS

	private List< ? extends RanFloodType > _sendList( String listCommand ){
		try {
			String response = _rr( listCommand );
			return JSONTranscoder.parseFloodList( response );
		} catch ( IOException e ) {
			error( e.getMessage() );
			return Collections.emptyList();
		}
	}

	private String _sendCommand( Command<?> c ){
		try {
			return _rr( JSONTranscoder.toJsonString( c ) );
		} catch ( ParseException | IOException e ) {
			return e.getMessage();
		}
	}

	private String _rr( String s ) throws IOException {
		socket.send( s );
		byte[] response = socket.recv();
		if( response != null ){
			return new String( response, ZMQ.CHARSET );
		} else {
			throw new IOException( "Request timeout" );
		}
	}


}
