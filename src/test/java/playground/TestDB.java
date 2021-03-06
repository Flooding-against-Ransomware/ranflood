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

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class TestDB {

	public static void main( String[] args ) {

		Path filePath = Path.of( "/Users/thesave/Desktop/ranflood_testsite/attackedFolder/folder1" );

		DB db = DBMaker
						.fileDB( "/Users/thesave/Desktop/ranflood_testsite/signature.db" )
						.checksumHeaderBypass()
						.make();

		db.getAll().keySet().forEach( System.out::println );

		Map< String, String > signatures = db
						.hashMap( "signatures" )
						.keySerializer( Serializer.STRING )
						.valueSerializer( Serializer.STRING )
						.createOrOpen();

//		signatures.putAll( TestSnapshot.getReadSignatures( filePath ) );

		String key = new ArrayList<>( signatures.keySet() ).get( 150 );

		System.out.println( "Key of " + key + " is " + signatures.get( key ) );

		db.close();

	}

}
