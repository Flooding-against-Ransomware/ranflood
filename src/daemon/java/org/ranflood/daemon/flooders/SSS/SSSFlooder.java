/******************************************************************************
 * Copyright 2024 (C) by Daniele D'Ugo <danieledugo1@gmail.com>               *
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

		private static final int	DFLT_RANSOMWARE_N = 200,
									DFLT_RANSOMWARE_K = 2;
		private static final boolean DFLT_RANSOMWARE_REMOVE_ORIGINALS = false;

		private static final int	DFLT_EXFILTRATION_N = 150,
									DFLT_EXFILTRATION_K = 6;
		private static final boolean DFLT_EXFILTRATION_REMOVE_ORIGINALS = true;

		public Integer  n,
						k;
		public Boolean remove_originals;

		public Parameters(Integer n, Integer k,
						  Boolean remove_originals) {
			this.n = n;
			this.k = k;
			this.remove_originals = remove_originals;
		}
	}


	private final FloodMethod METHOD;
	private final Set< String > exclusionList;

	private final Parameters parameters;


	/**
	 *
	 * @param exclusionList excluded dirs
	 * @param mode -
	 * @param params take k as percentage on n
	 */
	public SSSFlooder(
			Set< String > exclusionList, FloodMethod mode, Parameters params
	) {
		this.METHOD = mode;
		this.exclusionList = exclusionList;

		if(mode == FloodMethod.SSS_RANSOMWARE)
			this.parameters = new Parameters(
					(params.n != null) ? params.n : Parameters.DFLT_RANSOMWARE_N,
					(params.k != null) ? params.k : Parameters.DFLT_RANSOMWARE_K,
					(params.remove_originals != null) ? params.remove_originals : Parameters.DFLT_RANSOMWARE_REMOVE_ORIGINALS
			);
		else
			this.parameters = new Parameters(
					(params.n != null) ? params.n : Parameters.DFLT_EXFILTRATION_N,
					(params.k != null) ? params.k : Parameters.DFLT_EXFILTRATION_K,
					(params.remove_originals != null) ? params.remove_originals : Parameters.DFLT_EXFILTRATION_REMOVE_ORIGINALS
			);
		this.parameters.n = Math.max(this.parameters.n, 2);
		this.parameters.n = Math.min(this.parameters.n, 255);
		this.parameters.k = Math.max(this.parameters.k, 2);
		this.parameters.k = Math.min(this.parameters.k, this.parameters.n);
	}


	//@Override
	public UUID flood( Path targetFolder ) throws FlooderException {

		SSSSplitter sss = new SSSSplitter(parameters.n, parameters.k);

		SSSFloodTask t = new SSSFloodTask( exclusionList, targetFolder, METHOD, sss, parameters.remove_originals );
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

}
