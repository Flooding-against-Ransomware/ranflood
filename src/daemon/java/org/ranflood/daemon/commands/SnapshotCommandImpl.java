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

import org.ranflood.common.FloodMethod;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.SnapshotException;

import java.util.LinkedList;

public class SnapshotCommandImpl {

	private SnapshotCommandImpl() {
	}

	public static class Add extends SnapshotCommand.Add {

		public Add( RanfloodType type ) {
			super( type );
		}

		@Override
		public CommandResult execute() {
			switch ( this.type().method() ) {
				case RANDOM:
					return new CommandResult.Failed( "Cannot create a snapshot for the random flooder" );
				case ON_THE_FLY:
					try {
						Ranflood.daemon().onTheFlyFlooder().takeSnapshot( this.type().path() );
						return new CommandResult.Successful( "Created snapshot for the on-the-fly flooder at " + this.type().path() );
					} catch ( SnapshotException e ) {
						return new CommandResult.Failed( "Could not issue the creation of a snapshot for the on-the-fly flooder: " + e.getMessage() );
					}
				case SHADOW_COPY:
					try {
						Ranflood.daemon().shadowCopyFlooder().takeSnapshot( this.type().path() );
						return new CommandResult.Successful( "Created snapshot for the shadow-copy flooder at " + this.type().path() );
					} catch ( SnapshotException e ) {
						return new CommandResult.Failed( "Could not issue the creation of a snapshot for the shadow-copy flooder: " + e.getMessage() );
					}
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.type().method().name() );
			}
		}

	}

	public static class Remove extends SnapshotCommand.Remove {

		public Remove( RanfloodType type ) {
			super( type );
		}

		@Override
		public CommandResult execute() {
			switch ( this.type().method() ) {
				case RANDOM:
					return new CommandResult.Failed( "Cannot delete a snapshot for the random flooder (there are none)" );
				case ON_THE_FLY:
					Ranflood.daemon().onTheFlyFlooder().removeSnapshot( this.type().path() );
					return new CommandResult.Successful( "Issued the removal of the snapshot of the on-the-fly flooder" );
				case SHADOW_COPY:
					Ranflood.daemon().shadowCopyFlooder().removeSnapshot( this.type().path() );
					return new CommandResult.Successful( "Issued the removal of the snapshot of the shadow-copy flooder" );
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.type().method().name() );
			}
		}

	}

	public static class List extends SnapshotCommand.List {

		@Override
		public java.util.List< RanfloodType > execute() {
			LinkedList< RanfloodType > l = new LinkedList<>();
			Ranflood.daemon().onTheFlyFlooder().listSnapshots()
							.forEach( p -> l.add( new RanfloodType( FloodMethod.ON_THE_FLY, p ) ) );
			Ranflood.daemon().shadowCopyFlooder().listSnapshots()
							.forEach( p -> l.add( new RanfloodType( FloodMethod.SHADOW_COPY, p ) ) );
			return l;
		}

	}

}
