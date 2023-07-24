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

package org.ranflood.daemon.flooders.random;

import com.oblac.nomen.Nomen;
import org.ranflood.common.utils.Pair;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.tasks.FloodTaskGenerator;
import org.ranflood.daemon.flooders.tasks.WriteFileTask;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomFloodTask extends FloodTaskGenerator {

	private static final Map< String, String[] > signatures = new HashMap<>();
	private static final ArrayList< String > FILE_EXTESIONS = new ArrayList<>();
	private final RandomFlooder flooder;

	public RandomFloodTask( RandomFlooder flooder, Path filePath, FloodMethod floodMethod ) {
		super( filePath, floodMethod );
		this.flooder = flooder;
	}

	@Override
	public List< WriteFileTask > getFileTasks() {
		return IntStream.range( 0, 100 ).mapToObj( i -> {
			String extension = FILE_EXTESIONS.get( rng.nextInt( FILE_EXTESIONS.size() ) );
			Path filePath = Path.of( this.filePath().toAbsolutePath() + File.separator + Nomen.randomName() + extension );
			return new WriteFileTask( filePath, getCachedRandomBytes( extension ), this.floodMethod() );
		} ).collect( Collectors.toList() );
	}

	private static final Random rng = new Random();
	private static final ReentrantLock randomCacheLock = new ReentrantLock();
	private static final Pair< AtomicInteger, byte[] >[] randomCache = ( Pair< AtomicInteger, byte[] >[] ) new Pair< ?, ? >[ 64 ];
	private static int cacheCursor = 0;
	private static final int cache_value_max_usage = 8;

	private byte[] getCachedRandomBytes( String extension ) {
		byte[] content;
		randomCacheLock.lock();
		cacheCursor = ( cacheCursor + 1 ) % randomCache.length;
		if ( randomCache[ cacheCursor ] == null
						|| randomCache[ cacheCursor ].left().get() > cache_value_max_usage ) {
			int randomSize = Math.max(
							rng.nextInt( Double.valueOf( flooder.maxSize() ).intValue() ),
							Long.BYTES
			);
			ByteBuffer b = ByteBuffer.allocate( randomSize );
			long seed = System.nanoTime();
			for ( int i = 0; i < b.capacity() / Long.BYTES; i++ ) {
				seed ^= ( seed << 13 );
				seed ^= ( seed >>> 7 );
				seed ^= ( seed << 17 );
				b.putLong( seed );
			}
			if ( b.remaining() > 0 ) {
				byte[] r = new byte[ b.remaining() ];
				new Random().nextBytes( r );
				b.put( r );
			}
			content = b.array();
			randomCache[ cacheCursor ] = new Pair<>( new AtomicInteger( 1 ), content );
		} else {
			content = randomCache[ cacheCursor ].right();
			randomCache[ cacheCursor ].left().incrementAndGet();
		}
		randomCacheLock.unlock();
		String[] signature = signatures.get( extension );
		ByteBuffer b = ByteBuffer.allocate( content.length + ( int ) Math.ceil( signature.length ) );
		Arrays.stream( signature ).forEach( s ->
						b.put( ( byte ) Integer.parseInt( s, 16 ) )
		);
		b.put( ByteBuffer.wrap( content ) );
		return b.array();
	}

	@Override
	public Runnable getRunnableTask() {
		throw new UnsupportedOperationException( "RandomFloodTask should not be run as a normal task" );
//		return () -> {
//			Path filePath = Path.of(
//							this.filePath().toAbsolutePath() + File.separator
//											+ Nomen.randomName()
//											+ FILE_EXTESIONS.get( rng.nextInt( FILE_EXTESIONS.size() ) )
//			);
//			WriteFileTask d = new WriteFileTask( filePath, getCachedRandomBytes(), this.floodMethod() );
//			RanfloodDaemon.executeIORunnable( d.getRunnableTask() );
//		};
	}

	public static String[] getChunks( String s, int c_size ){
		return IntStream.iterate( 0, i -> i + c_size )
						.limit( ( int ) Math.ceil( s.length() / c_size ) )
						.mapToObj( i -> s.substring( i, Math.min( i + c_size, s.length() ) ) )
						.toArray( String[]::new );
	}

	static {
		signatures.put( ".avi", getChunks( "52494646", 2 ) );
		signatures.put( ".doc", getChunks( "D0CF11E0A1B11AE1", 2 ) );
		signatures.put( ".docx", getChunks( "504B030414000600", 2 ) );
		signatures.put( ".gif", getChunks( "47494638", 2 ) );
		signatures.put( ".jpeg", getChunks( "FFD8FFE0", 2 ) );
		signatures.put( ".mov", getChunks( "6D6F6F76", 2 ) );
		signatures.put( ".mp3", getChunks( "494433", 2 ) );
		signatures.put( ".mp4", getChunks( "6674797069736F6D", 2 ) );
		signatures.put( ".mpeg", getChunks( "000001B3", 2 ) );
		signatures.put( ".ods", getChunks( "504B0304", 2 ) );
		signatures.put( ".odt", getChunks( "504B0304", 2 ) );
		signatures.put( ".ogg" , getChunks( "4F67675300020000", 2 ) );
		signatures.put( ".pdf", getChunks( "255044462D312E36", 2 ) );
		signatures.put( ".png", getChunks( "89504E470D0A1A0A", 2 ) );
		signatures.put( ".ppt", getChunks( "D0CF11E0A1B11AE1", 2 ) );
		signatures.put( ".pptx", getChunks( "504B0304", 2 ) );
		signatures.put( ".txt", getChunks( "EFBBBF", 2 ) );
		signatures.put( ".wav", getChunks( "52494646", 2 ) );
		signatures.put( ".xls", getChunks( "D0CF11E0A1B11AE1", 2 ) );
		signatures.put( ".xlsx", getChunks( "504B0304", 2 ) );
		FILE_EXTESIONS.addAll( signatures.keySet() );
	}

}
