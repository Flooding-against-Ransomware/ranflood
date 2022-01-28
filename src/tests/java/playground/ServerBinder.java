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

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerBinder {

	public static void main( String[] args ) {

		ExecutorService serverThread = Executors.newSingleThreadExecutor();
		serverThread.submit( () -> {
			try ( ZContext context = new ZContext() ) {
				ZMQ.Socket socket = context.createSocket( SocketType.REP );
				socket.bind( "tcp://localhost:7890" );
				System.out.println( "Server started, accepting requests from clients" );
				while ( !context.isClosed() ) {
					System.out.println( "Server waiting for reception" );
					byte[] reply = socket.recv();
					String name = new String( reply, ZMQ.CHARSET );
					System.out.println( "Server received [" + name + "]" );
					String response = "Hello, " + name;
					socket.send( response.getBytes( ZMQ.CHARSET ), 0 );
					context.close();
				}
				context.destroy();
				serverThread.shutdown();
			}
		} );

		ExecutorService clientThread = Executors.newSingleThreadExecutor();
		clientThread.submit( () -> {
			try ( ZContext context = new ZContext() ) {
				System.out.println( "Client started, sending request to server" );
				ZMQ.Socket socket = context.createSocket( SocketType.REQ );
				socket.connect( "tcp://localhost:7890" );
				System.out.println( "Client started, sending request to server" );
				socket.send( "RanFlood".getBytes( ZMQ.CHARSET ) );
				System.out.println( "Client sent request" );
				byte[] byteReply = socket.recv();
				String response = new String( byteReply, ZMQ.CHARSET );
				System.out.println( "Client received [" + response + "]" );
				socket.close();
				context.destroy();
				clientThread.shutdown();
			}
		} );


	}

}
