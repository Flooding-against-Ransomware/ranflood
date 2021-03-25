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
import org.ranflood.daemon.RanFloodDaemon;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.tasks.FloodTask;
import org.ranflood.daemon.flooders.tasks.WriteFileTask;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class RandomFloodTask extends FloodTask {

	public RandomFloodTask( Path filePath, FloodMethod floodMethod ) {
		super( filePath, floodMethod );
	}

	private static final ArrayList< String > FILE_EXTESIONS = new ArrayList<>(
			Arrays.asList( ".doc", ".docx", ".odt", ".txt", ".pdf", ".xls", ".xlsx", ".ods",
			".ppt", ".pptx", ".jpeg", ".jps", ".gif", ".png", ".mov", ".avi",
			".mp4", ".mpeg", ".mp3", ".wav", ".ogg" )
	);

	private static final Random rng = new Random();
	private static final ReentrantLock randomCacheLock = new ReentrantLock();
	private static final Pair< AtomicInteger, byte[] >[] randomCache = ( Pair< AtomicInteger, byte[] >[] ) new Pair< ?, ? >[ 64 ];
	private static int cacheCursor = 0;
	private static final int cache_value_max_usage = 8;

	private static byte[] getCachedRandomBytes(){
		byte[] content;
		randomCacheLock.lock();
		cacheCursor = ( cacheCursor + 1 ) % randomCache.length;
		if( randomCache[ cacheCursor ] == null
						|| randomCache[ cacheCursor ].left().get() > cache_value_max_usage ){
			int randomSize = rng.nextInt( Double.valueOf( Math.pow( 2, 22 ) ).intValue() )
							+ Double.valueOf( Math.pow( 2, 7 ) ).intValue();
			ByteBuffer b = ByteBuffer.allocate( randomSize );
			long seed = System.nanoTime();
			for ( int i = 0; i < b.capacity() / Long.BYTES; i++ ) {
				seed ^= ( seed << 13 );
				seed ^= ( seed >>> 7 );
				seed ^= ( seed << 17 );
				b.putLong( seed );
			}
			if( b.remaining() > 0 ){
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
		return content;
	}

	@Override
	public Runnable getRunnableTask() {
		return () -> {
			Path filePath = Path.of(
							this.filePath().toAbsolutePath() + File.separator
											+ Nomen.randomName()
											+ FILE_EXTESIONS.get( rng.nextInt( FILE_EXTESIONS.size() ) )
			);
			WriteFileTask d = new WriteFileTask( filePath, getCachedRandomBytes(), this.floodMethod() );
			RanFloodDaemon.executeIORunnable( d.getRunnableTask() );
		};
	}

}
