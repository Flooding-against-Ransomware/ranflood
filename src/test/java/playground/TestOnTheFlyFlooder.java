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

import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooderException;

import java.nio.file.Path;
import java.util.UUID;

public class TestOnTheFlyFlooder {

	public static void main( String[] args ) throws FlooderException {
		RanFlood.main( TestCommons.getArgs() );
		RanFloodDaemon daemon = RanFlood.daemon();
		Path filePath = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder1" );
		// WE CREATE SOME FILES
		UUID idRandom = daemon.randomFlooder().flood( filePath );
		try {
			Thread.sleep( 2000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		daemon.randomFlooder().stopFlood( idRandom );

		// WE TAKE THE SIGNATURES OF THE FILES
		daemon.onTheFlyFlooder().takeSnapshot( filePath );

		// WE LAUNCH THE ON_THE_FLY FLOODER
		UUID id1 = daemon.onTheFlyFlooder().flood( filePath );
		try {
			Thread.sleep( 2000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		daemon.onTheFlyFlooder().stopFlood( id1 );
		daemon.onTheFlyFlooder().removeSnapshot( filePath );
		daemon.shutdown();
	}

}


