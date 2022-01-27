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

import org.ranflood.common.FloodMethod;
import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.RanFloodType;
import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.SnapshotException;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.ranflood.common.RanFloodLogger.log;
import static org.ranflood.common.commands.transcoders.JSONTranscoder.parseFloodList;


public class TestShadowCopyFlooder {

	public static void main( String[] args ) throws InterruptedException, IOException {

		String settings_file = Paths.get( "src/tests/java/playground/settings.ini" ).toAbsolutePath().toString();
		RanFlood.main( new String[]{ settings_file } );
		Thread.sleep( 1000 );
		Path filePath1 = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/Other" );
		Path filePath2 = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/Other 2" );

//		if ( Arrays.stream( filePath1.toFile().listFiles() ).filter( File::isDirectory ).count() < 1 ){
//			createTestStructure( filePath1 );
//		}
		for ( Path filePath : List.of( filePath1, filePath2 ) ) {

			sendCommand( new SnapshotCommand.Add(
							new RanFloodType( FloodMethod.SHADOW_COPY, filePath ) )
			);
			Thread.sleep( 1000 );

			log( sendCommandList( new SnapshotCommand.List() ) );
			Thread.sleep( 1000 );

			sendCommand( new FloodCommand.Start(
							new RanFloodType( FloodMethod.SHADOW_COPY, filePath )
			) );
			Thread.sleep( 1000 );
		}

		// THIS SHOULD BE OK
		List< RanFloodType.Tagged > list;
		do {
			log( "Retrieving list of running floods" );
			String runningList = sendCommandList( new FloodCommand.List() );
			log( runningList );
			list = ( List< RanFloodType.Tagged > ) parseFloodList( runningList );
			Thread.sleep( 1000 );
		} while ( list.isEmpty() );


		for ( RanFloodType.Tagged rftt : list ) {
			// THIS SHOULD BE OK
			sendCommand( new FloodCommand.Stop(
							rftt.method(),
							rftt.id()
			) );
		}
		Thread.sleep( 1000 );

		sendString( "shutdown" );

//		Thread.sleep( 1000 );
		//RanFlood.getDaemon().shutdown();
	}

	public static void _main( String[] args ) throws FlooderException, SnapshotException, InterruptedException {
		RanFlood.main( TestCommons.getArgs() );
		RanFloodDaemon daemon = RanFlood.daemon();
		Path filePath1 = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/Other" );
		Path filePath2 = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/Other 2" );

//			if ( Arrays.stream( filePath.toFile().listFiles() ).filter( File::isDirectory ).count() < 1 ){
//				createTestStructure( filePath );
//			}

		daemon.shadowCopyFlooder().takeSnapshot( filePath1 );
		daemon.shadowCopyFlooder().takeSnapshot( filePath2 );

		// WE LAUNCH THE ON_THE_FLY FLOODER
		UUID id1 = daemon.shadowCopyFlooder().flood( filePath1 );
		log( "Launched flooder: " + id1 );

		Thread.sleep( 1000 );

		UUID id2 = daemon.shadowCopyFlooder().flood( filePath2 );

		Thread.sleep( 1000 );

		log( "STOPPING" );
		daemon.shadowCopyFlooder().stopFlood( id1 );
		daemon.shadowCopyFlooder().stopFlood( id2 );

		Thread.sleep( 1000 );

		log( "REMOVING SNAPSHOTS" );
		daemon.shadowCopyFlooder().removeSnapshot( filePath1 );
		daemon.shadowCopyFlooder().removeSnapshot( filePath2 );
		daemon.shutdown();

	}

	private static void createTestStructure( Path root ) {
		log( "Creating test folders structure" );
		RanFloodDaemon daemon = RanFlood.daemon();
		List< Path > l = Arrays.asList(
						root.resolve( "Application Data" ), root.resolve( "Application Data" ).resolve( "Other" ),
						root.resolve( "Other" ), root.resolve( "Other 2" )
		);
		l.forEach( f -> {
			try {
				UUID id = daemon.randomFlooder().flood( f );
				Thread.sleep( 100 );
				daemon.randomFlooder().stopFlood( id );
			} catch ( InterruptedException | FlooderException e ) {
				e.printStackTrace();
			}
		} );
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


