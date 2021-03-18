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

import com.republicate.json.Json;
import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.commands.Command;
import org.ranflood.daemon.commands.FloodCommand;
import org.ranflood.daemon.commands.RanFloodType;
import org.ranflood.daemon.commands.SnapshotCommand;
import org.ranflood.daemon.commands.transcoders.JSONTranscoder;
import org.ranflood.daemon.commands.transcoders.ParseException;
import org.ranflood.daemon.flooders.FloodMethod;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestDaemon {

	public static void main( String[] args ) throws InterruptedException {

//		String settings_file = Paths.get( "src/test/java/playground/settings.ini" ).toAbsolutePath().toString();
//		RanFlood.main( new String[]{ settings_file } );
//		Thread.sleep( 1000 );
		sendCommand( new SnapshotCommand.Add(
						new RanFloodType( FloodMethod.RANDOM,
										Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder1" )
						)
		) );
		Thread.sleep( 1000 );
		sendCommand( new FloodCommand.Start(
						new RanFloodType(
										FloodMethod.RANDOM,
										Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder1" )
						)
		) );
		Thread.sleep( 1000 );
		sendString( "shutdown" );
//		String id = sendCommandList( new FloodCommand.List() );
//		sendCommand( new FloodCommand.Stop( id ) );
//
//		sendCommand( new SnapshotCommand.Add(
//						new RanFloodType( FloodMethod.ON_THE_FLY,
//										Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder2" )
//						)
//		) );
//		Thread.sleep( 1000 );
//		sendCommand( new FloodCommand.Start( F ) );
//		Thread.sleep( 3000 );

		//RanFlood.getDaemon().shutdown();
	}

	private static void sendCommand( Command< ? > c ) {
		try {
		sendString( JSONTranscoder.toJson( c ) );
		} catch ( ParseException e ) {
			e.printStackTrace();
		}
	}

	private static void sendString( String s ){
		try ( ZContext context = new ZContext() ) {
			System.out.println( "Sending request to server" );
			ZMQ.Socket socket = context.createSocket( SocketType.REQ );
			socket.connect( "tcp://localhost:7890" );
			socket.send( s );
			String response = new String( socket.recv(), ZMQ.CHARSET );
			System.out.println( "Client received [" + response + "]" );
			socket.close();
			context.destroy();
		}
	}

	private static String sendCommandList( Command< ? > c ) {
		try ( ZContext context = new ZContext() ) {
			System.out.println( "Sending request to server" );
			ZMQ.Socket socket = context.createSocket( SocketType.REQ );
			socket.connect( "tcp://localhost:7890" );
			String command = JSONTranscoder.toJson( c );
			socket.send( command );
			String response = new String( socket.recv(), ZMQ.CHARSET );
			socket.close();
			context.destroy();
			return Json.parse( response ).asObject().getArray( "list" ).getString( 0 );
		} catch ( ParseException | IOException e ) {
			e.printStackTrace();
			return "";
		}
	}

}
