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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

public class TestWrites {

	public static void main( String[] args ) {
		String root =  "/Users/thesave/Desktop/test_folder";
		long start = System.currentTimeMillis();
		int file_number = 1000;
		IntStream.range( 0, file_number )
						.parallel()
						.forEach( i -> {
							try {
								FileOutputStream f = new FileOutputStream( root + String.valueOf( i ) + ".txt" );
								byte[] b = new byte[ new Random().nextInt( Double.valueOf( Math.pow( 2, 22 ) ).intValue() ) + Double.valueOf( Math.pow( 2, 7 ) ).intValue() ];
								new Random().nextBytes( b );
								f.write( b );
								f.close();
							} catch ( IOException e ) {
								e.printStackTrace();
							}
						});
		System.out.println( "File generation took: " + ( System.currentTimeMillis() - start ) + "ms" );
	}
}