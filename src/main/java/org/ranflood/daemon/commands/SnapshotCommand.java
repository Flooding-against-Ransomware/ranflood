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

package org.ranflood.daemon.commands;

import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.commands.types.CommandResult;
import org.ranflood.daemon.commands.types.RanFloodType;
import org.ranflood.daemon.flooders.FloodMethod;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooderException;

import java.util.LinkedList;

public class SnapshotCommand {

	private SnapshotCommand(){}

	public static class Add extends AbstractCommand< CommandResult > {

		public Add( RanFloodType type ) {
			super( type, "Snapshot add" );
		}

		// TODO: finish implementing this
		@Override
		public CommandResult execute() {
			switch ( this.type().method() ){
				case RANDOM:
					return new CommandResult.Failed( "Cannot create a snapshot for the random flooder" );
				case ON_THE_FLY:
					try {
						RanFlood.daemon().onTheFlyFlooder().takeSnapshot( this.type().path() );
						return new CommandResult.Successful( "Creation snapshot for the on-the-fly flooder" );
					} catch ( OnTheFlyFlooderException e ) {
						return new CommandResult.Failed( "Could not issue the creation of a snapshot for the on-the-fly flooder: " + e.getMessage() );
					}
				case SHADOW_COPY:
					return new CommandResult.Failed( "Method 'SHADOW_COPY' not implemented" );
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.type().method().name() );
			}
		}

	}

	public static class Remove extends AbstractCommand< CommandResult > {

		public Remove( RanFloodType type ) {
			super( type, "Snapshot remove" );
		}

		// TODO: finish implementing this
		@Override
		public CommandResult execute() {
			switch ( this.type().method() ){
				case RANDOM:
					return new CommandResult.Failed( "Cannot delete a snapshot for the random flooder (there are none)" );
				case ON_THE_FLY:
					RanFlood.daemon().onTheFlyFlooder().removeSnapshot( this.type().path() );
					return new CommandResult.Successful( "Issued the removal of the snapshot of the on-the-fly flooder" );
				case SHADOW_COPY:
					return new CommandResult.Failed( "Method 'SHADOW_COPY' not implemented" );
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.type().method().name() );
			}
		}

	}

	public static class List implements Command< java.util.List< RanFloodType > > {

		// TODO: finish implementing this
		@Override
		public java.util.List< RanFloodType > execute() {
			LinkedList< RanFloodType > l = new LinkedList<>();
			RanFlood.daemon().onTheFlyFlooder().listSnapshots()
							.forEach( p -> l.add( new RanFloodType( FloodMethod.ON_THE_FLY, p ) ) );
			// TODO: include also ShadowCopy
			return l;
		}

		@Override
		public String name() {
			return "Snapshot list";
		}

		@Override
		public boolean isAsync() {
			return false;
		}

	}

}
