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

package org.ranflood.daemon.flooders.onTheFly;


import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.flooders.AbstractSnapshotFlooder;
import org.ranflood.daemon.flooders.FloodMethod;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class OnTheFlyFlooder extends AbstractSnapshotFlooder {

	private final FloodMethod METHOD = FloodMethod.ON_THE_FLY;
	private final Path snapshotDBPath;

	public OnTheFlyFlooder( Path snapshotDBPath ){
		this.snapshotDBPath = snapshotDBPath;
	}

	@Override
	public UUID flood( Path targetFolder ) {
		OnTheFlyFloodTask t = new OnTheFlyFloodTask( targetFolder, METHOD );
		UUID id = UUID.randomUUID();
//		log( "Adding task: " + id );
		getRunningTasksList().add( new LabeledFloodTask( id, t ) );
		RanFlood.getDaemon().floodTaskExecutor().addTask( t );
		return id;
	}

	@Override
	public void takeSnapshot( Path filepath ) {
		OnTheFlySnapshooter.takeSnapshot( filepath );
	}

	@Override
	public void removeSnapshot( Path filepath ) {
		OnTheFlySnapshooter.removeSnapshot( filepath );
	}

	@Override
	public List< Path > listSnapshots() {
		return OnTheFlySnapshooter.listSnapshots();
	}

	@Override
	public void shutdown() {
		OnTheFlySnapshooter.shutdown();
	}

	public Path snapshotDBPath() {
		return snapshotDBPath;
	}
}
