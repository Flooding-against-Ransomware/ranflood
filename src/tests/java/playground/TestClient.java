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

import org.ranflood.client.RanFlood;

public class TestClient {

	public static void main( String[] args ) throws InterruptedException {

		String folder1 = "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder1";


//		callClient( "snapshot", "list" );
//		callClient( "flood", "list" );
//		callClient( "flood", "random", "" );

		String UUID = "3a586d04-9670-462f-8fb4-76809eb4d34a";

		if ( UUID.isEmpty() ) {
			callClient( "flood", "start", "random", folder1 );
			Thread.sleep( 1_000 );
			callClient( "flood", "list" );
		} else {
			callClient( "flood", "stop", "random", UUID );
		}

	}

	private static void callClient( String... s ) {
		RanFlood.run( s );
	}

}
