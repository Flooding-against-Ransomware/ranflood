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

package org.ranflood.daemon.flooders.shadowCopy;

import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.AbstractSnapshotFlooder;
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.SnapshotException;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ShadowCopyFlooder extends AbstractSnapshotFlooder {

	private final FloodMethod METHOD = FloodMethod.SHADOW_COPY;
	private final Path archiveRoot;
	private final Path archiveDatabase;
	private final Set< String > exclusionList;

	public ShadowCopyFlooder( Path archiveRoot, Path archiveDatabase, Set< String > exclusionList ) {
		this.archiveRoot = archiveRoot;
		this.archiveDatabase = archiveDatabase;
		this.exclusionList = exclusionList;
	}

	@Override
	public UUID flood( Path targetFolder ) throws FlooderException {
		try {
			ShadowCopyFloodTask t =
							new ShadowCopyFloodTask( targetFolder, METHOD,
											ShadowCopySnapshooter.getSnapshotArchivePath( targetFolder ) );
			UUID id = UUID.randomUUID();
			LabeledFloodTask lft = new LabeledFloodTask( id, t );
			addRunningTask( lft );
			Ranflood.daemon().floodTaskExecutor().addTask( lft );
			return id;
		} catch ( SnapshotException e ) {
			throw new FlooderException( e.getMessage() );
		}
	}

	@Override
	public void takeSnapshot( Path filepath ) throws SnapshotException {
		ShadowCopySnapshooter.takeSnapshot( filepath );
	}

	@Override
	public void removeSnapshot( Path filepath ) {
		ShadowCopySnapshooter.removeSnapshot( filepath );
	}

	@Override
	public List< Path > listSnapshots() {
		return ShadowCopySnapshooter.listSnapshots();
	}

	@Override
	public void shutdown() {
		ShadowCopySnapshooter.shutdown();
	}

	public Path archiveRoot() {
		return archiveRoot;
	}

	public Path archiveDatabase() {
		return archiveDatabase;
	}

	public Set< String > exclusionList() {
		return exclusionList;
	}
}
