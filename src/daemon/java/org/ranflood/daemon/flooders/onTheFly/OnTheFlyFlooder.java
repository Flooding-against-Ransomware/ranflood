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


import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.AbstractSnapshotFlooder;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.SnapshotException;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OnTheFlyFlooder extends AbstractSnapshotFlooder {

	private final FloodMethod METHOD = FloodMethod.ON_THE_FLY;
	private final Path snapshotDBPath;
	private final Set< String > exclusionList;

	public OnTheFlyFlooder( Path snapshotDBPath, Set< String > exclusionList ) {
		this.snapshotDBPath = snapshotDBPath;
		this.exclusionList = exclusionList;
	}

	@Override
	public UUID flood( Path targetFolder ) throws FlooderException {
		OnTheFlyFloodTask t = new OnTheFlyFloodTask( targetFolder, METHOD );
		UUID id = UUID.randomUUID();
		LabeledFloodTask lft = new LabeledFloodTask( id, t );
		addRunningTask( lft );
		Ranflood.daemon().floodTaskExecutor().addTask( lft );
		return id;
	}

	@Override
	public void takeSnapshot( Path filepath ) throws SnapshotException {
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

	public Set< String > exclusionList() {
		return exclusionList;
	}
}
