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

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.ranflood.common.RanfloodLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFlower {

	private static final ExecutorService t = Executors.newSingleThreadExecutor();
	private static final AtomicInteger c = new AtomicInteger( 0 );

	public static void main( String[] args ) throws InterruptedException {


		Observable.create( TestFlower::startEmitter )
						.toFlowable( BackpressureStrategy.BUFFER )
						.subscribeOn( Schedulers.computation() )
						.map( Object::toString )
						.subscribe( RanfloodLogger::log );

		Thread.sleep( 5000 );
		System.out.println( "10 time units passed, shutting down" );
		t.shutdownNow();
	}

	public static void startEmitter( @NonNull ObservableEmitter< Integer > e ) {
		t.execute( () -> {
			while ( !t.isShutdown() ) {
				e.onNext( c.incrementAndGet() );
			}
			e.onComplete();
		} );
	}


}
