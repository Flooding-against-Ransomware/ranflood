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

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class TestHeader {

	public static String[] getChunks( String s, int c_size ){
		return IntStream.iterate( 0, i -> i + c_size )
						.limit( ( int ) Math.ceil( s.length() / c_size ) )
						.mapToObj( i -> s.substring( i, Math.min( i + c_size, s.length() ) ) )
						.toArray( String[]::new );
	}

	public static void main( String[] args ) throws IOException, InterruptedException {
		Map< String, String[] > signatures = new HashMap<>();
		String extension = "wav";
		signatures.put( "avi", getChunks( "52494646", 2 ) );
		signatures.put( "doc", getChunks( "D0CF11E0A1B11AE1", 2 ) );
		signatures.put( "docx", getChunks( "504B030414000600", 2 ) );
		signatures.put( "gif", getChunks( "47494638", 2 ) );
		signatures.put( "jpeg", getChunks( "FFD8FFE0", 2 ) );
		signatures.put( "mov", getChunks( "6D6F6F76", 2 ) );
		signatures.put( "mp3", getChunks( "494433", 2 ) );
		signatures.put( "mp4", getChunks( "6674797069736F6D", 2 ) );
		signatures.put( "mpeg", getChunks( "000001B3", 2 ) );
		signatures.put( "ods", getChunks( "504B0304", 2 ) );
		signatures.put( "odt", getChunks( "504B0304", 2 ) );
		signatures.put( "ogg" , getChunks( "4F67675300020000", 2 ) );
		signatures.put( "pdf", getChunks( "255044462D312E36", 2 ) );
		signatures.put( "png", getChunks( "89504E470D0A1A0A", 2 ) );
		signatures.put( "ppt", getChunks( "D0CF11E0A1B11AE1", 2 ) );
		signatures.put( "pptx", getChunks( "504B0304", 2 ) );
		signatures.put( "txt", getChunks( "EFBBBF", 2 ) );
		signatures.put( "wav", getChunks( "52494646", 2 ) );
		signatures.put( "xls", getChunks( "D0CF11E0A1B11AE1", 2 ) );
		signatures.put( "xlsx", getChunks( "504B0304", 2 ) );
		String[] signature = signatures.get( extension );
//		System.out.println( Arrays.toString( signature ) );
		ByteBuffer b = ByteBuffer.allocate( 16 );
		Arrays.stream( signature ).forEach( s ->
			b.put( ( byte ) Integer.parseInt( s, 16 ) )
		);
		FileOutputStream outputStream = new FileOutputStream( "/Users/thesave/Desktop/test." + extension );
		outputStream.write( b.array() );
		outputStream.close();
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("file /Users/thesave/Desktop/test." + extension );
		pr.waitFor();
		new BufferedReader( new InputStreamReader( pr.getInputStream() ) ).lines().forEach( System.out::println );
		new File( "/Users/thesave/Desktop/test." + extension ).delete();
	}
}