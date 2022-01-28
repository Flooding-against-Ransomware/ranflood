/******************************************************************************
 * Copyright 2021 (C) by Loris Onori                                          *
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

package org.ranflood.prototype;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Flooding {

	//figlio creato per sottocartelle
	private static class FloodingChild implements Runnable {

		private File dir;

		public FloodingChild( File dir ) {
			this.dir = dir;
		}

		@Override
		public void run() {
			Flooding.startingFunction( dir );
		}

	}

	public static void main( String[] args ) {

		//Il primo parametro è il path della cartella che è stata attaccata
		File dir = new File( args[ 0 ] );

		Flooding.startingFunction( dir );

	}


	//Inondo la cartella di file
	public static void floodDirectory( File dir ) {
		int i = 0;
		while ( true ) {

			File floodFile = new File( dir.getAbsolutePath() + "/floodFile" + i );
			System.out.println( "File creato : " + dir.getAbsolutePath() + "/floodFile" + i + ".txt" );
			i++;
			//Scrivo qualcosa nel file
			try {
				BufferedWriter bw = new BufferedWriter( new FileWriter( floodFile ) );
				for ( int k = 0; k < 50; k++ ) {
					bw.write( "fhalòjkaojdshfalkfahdf9a8sdyh034nhsaklhdfjkh0234lmdsnaso" );
				}
				bw.close();
			} catch ( Exception e ) {
				//Probabilmente non visibiel da nessuna parte
				System.out.println( "Errore apertura/scrittura" + floodFile.getAbsolutePath() );
			}
		}
	}

	//Funzione di partenza usata dal main e dai figli
	public static void startingFunction( File dir ) {
		File[] listFile = dir.listFiles();
		for ( File file : listFile ) {
			if ( file.isDirectory() ) {
				//Figlio
				FloodingChild child = new FloodingChild( file );
				new Thread( child ).start();
			}
		}
		//Ho creato tutti i figli nelle sotto cartella ora posso partire a inondare
		Flooding.floodDirectory( dir );
	}

}


//sdfg
