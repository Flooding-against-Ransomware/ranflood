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

package playground;

import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.Ranflood;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.ranflood.common.RanfloodLogger.log;
import static org.ranflood.common.commands.transcoders.JSONTranscoder.parseDaemonCommandList;

public class TestDaemon {

	@SuppressWarnings("unchecked")
	public static void main( String[] args ) throws InterruptedException, IOException, ParseException {

		String settings_file = Paths.get( "src/tests/java/playground/settings.ini" ).toAbsolutePath().toString();
		Ranflood.main( new String[]{ settings_file } );
		Thread.sleep( 1000 );

		Path folder1 = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder1" );

//		// THIS MUST RETURN AN ERROR
//		sendCommand( new SnapshotCommand.Add(
//						new RanfloodType( FloodMethod.RANDOM, folder1 ) )
//		);
//		Thread.sleep( 1000 );

		// THIS SHOULD BE OK
		sendCommand( new FloodCommand.Start(
						new RanfloodType( FloodMethod.RANDOM, folder1 ) )
		);
		Thread.sleep( 1000 );

		// THIS SHOULD BE OK
		String runningList = sendCommandList( new FloodCommand.List() );
		log( runningList );
		List< RanfloodType.Tagged > list = ( List< RanfloodType.Tagged > ) parseDaemonCommandList( runningList );

		Thread.sleep( 2000 );

		// THIS SHOULD BE OK
		sendCommand( new FloodCommand.Stop(
						list.get( 0 ).method(),
						list.get( 0 ).id()
		) );

		// THIS SHOULD BE OK
		sendCommand( new SnapshotCommand.Add(
						new RanfloodType( FloodMethod.ON_THE_FLY, folder1 )
		) );
		Thread.sleep( 1000 );

		do {
			log( "Retrieving list of taken snapshots" );
			String snapshotList = sendCommandList( new SnapshotCommand.List() );
			log( snapshotList );
			list = ( List< RanfloodType.Tagged > ) parseDaemonCommandList( runningList );
			Thread.sleep( 1000 );
		} while ( list.isEmpty() );

		// THIS SHOULD BE OK
		sendCommand( new FloodCommand.Start(
						new RanfloodType( FloodMethod.ON_THE_FLY, folder1 )
		) );

		// THIS SHOULD BE OK
		do {
			log( "Retrieving list of running floods" );
			runningList = sendCommandList( new FloodCommand.List() );
			log( runningList );
			list = ( List< RanfloodType.Tagged > ) parseDaemonCommandList( runningList );
			Thread.sleep( 500 );
		} while ( list.isEmpty() );

		Thread.sleep( 5000 );

		// THIS SHOULD BE OK
		sendCommand( new FloodCommand.Stop(
						list.get( 0 ).method(),
						list.get( 0 ).id()
		) );
		Thread.sleep( 1000 );

		sendString( "shutdown" );

//		Thread.sleep( 1000 );
		//Ranflood.getDaemon().shutdown();
	}

	private static void sendCommand( Command< ? > c ) {
		try {
			sendString( JSONTranscoder.toJsonString( c ) );
		} catch ( ParseException e ) {
			e.printStackTrace();
		}
	}

	private static void sendString( String s ) {
		try ( ZContext context = new ZContext() ) {
			ZMQ.Socket socket = context.createSocket( SocketType.REQ );
			socket.connect( "tcp://localhost:7890" );
			socket.send( s );
			String response = new String( socket.recv(), ZMQ.CHARSET );
			log( "Client received [" + response + "]" );
			socket.close();
			context.destroy();
		}
	}

	private static String sendCommandList( Command< ? > c ) {
		try ( ZContext context = new ZContext() ) {
			ZMQ.Socket socket = context.createSocket( SocketType.REQ );
			socket.connect( "tcp://localhost:7890" );
			String command = JSONTranscoder.toJsonString( c );
			socket.send( command );
			String response = new String( socket.recv(), ZMQ.CHARSET );
			socket.close();
			context.destroy();
			return response;
//			return Json.parse( response ).asObject().getArray( "list" ).getString( 0 );
		} catch ( ParseException e ) {
			e.printStackTrace();
			return "";
		}
	}

}
