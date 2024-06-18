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

package org.ranflood.daemon.flooders.SSS;


import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.AbstractSnapshotFlooder;
import org.ranflood.daemon.flooders.FlooderException;
import org.sssfile.SSSSplitter;
import org.ranflood.daemon.flooders.SnapshotException;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;



public class SSSFlooder extends AbstractSnapshotFlooder {

	public static final class Parameters {

		private static final int DFLT_RANSOMWARE_N = 100;
		private static final int DFLT_RANSOMWARE_K = 2;
		private static final boolean DFLT_RANSOMWARE_REMOVE_ORIGINALS = false;

		private static final int DFLT_EXFILTRATION_N = 100;
		private static final int DFLT_EXFILTRATION_K = 100;
		private static final boolean DFLT_EXFILTRATION_REMOVE_ORIGINALS = true;

		public Integer n, k;
		public Boolean remove_originals;

		public Parameters(Integer n, Integer k, Boolean remove_originals) {
			this.n = n;
			this.k = k;
			this.remove_originals = remove_originals;
		}
	}



	private final FloodMethod METHOD;
	private final Path snapshotDBPath;
	private final Set< String > exclusionList;

	private final Parameters parameters;



	public SSSFlooder(
			Path snapshotDBPath, Set< String > exclusionList, FloodMethod mode, Parameters parameters
	) {
		this.METHOD = mode;
		this.snapshotDBPath = snapshotDBPath;
		this.exclusionList = exclusionList;
		if(mode == FloodMethod.SSS_RANSOMWARE)
			this.parameters = new Parameters(
				(parameters.n != null) ? parameters.n : Parameters.DFLT_RANSOMWARE_N,
				(parameters.n != null) ? parameters.k : Parameters.DFLT_RANSOMWARE_K,
				(parameters.remove_originals != null) ? parameters.remove_originals : Parameters.DFLT_RANSOMWARE_REMOVE_ORIGINALS
			);
		else
			this.parameters = new Parameters(
				(parameters.n != null) ? parameters.n : Parameters.DFLT_EXFILTRATION_N,
				(parameters.n != null) ? parameters.k : Parameters.DFLT_EXFILTRATION_K,
				(parameters.remove_originals != null) ? parameters.remove_originals : Parameters.DFLT_EXFILTRATION_REMOVE_ORIGINALS
			);
	}

	@Override
	public UUID flood( Path targetFolder ) throws FlooderException {
		SSSSplitter sss = new SSSSplitter(parameters.n, parameters.k);
		SSSFloodTask t = new SSSFloodTask( targetFolder, METHOD, sss, parameters.remove_originals );
		UUID id = UUID.randomUUID();
		LabeledFloodTask lft = new LabeledFloodTask( id, t );
		addRunningTask( lft );
		Ranflood.daemon().floodTaskExecutor().addTask( lft );
		return id;
	}

	@Override
	public void takeSnapshot( Path filepath ) throws SnapshotException {
		SSSSnapshooter.takeSnapshot( filepath );
	}

	@Override
	public void removeSnapshot( Path filepath ) {
		SSSSnapshooter.removeSnapshot( filepath );
	}

	@Override
	public List< Path > listSnapshots() {
		return SSSSnapshooter.listSnapshots();
	}

	@Override
	public void shutdown() {
		SSSSnapshooter.shutdown();
	}

	public Path snapshotDBPath() {
		return snapshotDBPath;
	}

	public Set< String > exclusionList() {
		return exclusionList;
	}
}
