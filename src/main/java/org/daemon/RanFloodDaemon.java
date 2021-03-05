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

package org.daemon;

import org.daemon.flooders.executors.FileTaskExecutor;
import org.daemon.flooders.executors.FloodTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RanFloodDaemon {

	static final private ExecutorService executors = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
	static final private FileTaskExecutor fileTaskExecutor = FileTaskExecutor.getInstance();
	static final private FloodTaskExecutor floodTaskExecutor = FloodTaskExecutor.getInstance();

	public static void execute( Runnable r ){
		executors.submit( r );
	}

	public static FileTaskExecutor fileTaskExecutor() {
		return fileTaskExecutor;
	}

	public static FloodTaskExecutor floodTaskExecutor() {
		return floodTaskExecutor;
	}

	public static void shutdown(){
		System.out.println( "Shutting down the executor pool");
		executors.shutdown();
		System.out.println( "Shutting down the file task executor");
		fileTaskExecutor.shutdown();
		System.out.println( "Shutting down the flood task executor");
		floodTaskExecutor.shutdown();
	}

}
