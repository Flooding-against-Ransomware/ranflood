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

import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanFloodType;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.RanFlood;
import org.ranflood.daemon.flooders.FlooderException;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FloodCommandImpl {

	private FloodCommandImpl() {}

	public static class Start extends FloodCommand.Start {

		public Start( RanFloodType type ) {
			super( type );
		}

		// todo: implement this
		@Override
		public CommandResult execute() {
			String id;
			switch ( this.type().method() ) {
				case RANDOM:
					id = RanFlood.daemon().randomFlooder().flood( this.type().path() ).toString();
					return new CommandResult.Successful( "Launched " + this.type().method() + " flood, ID: " + id );
				case ON_THE_FLY:
					try {
						id = RanFlood.daemon().onTheFlyFlooder().flood( this.type().path() ).toString();
						return new CommandResult.Successful( "Launched " + this.type().method() + " flood, ID: " + id );
					} catch ( FlooderException e ) {
						return new CommandResult.Failed( "Error in launching " + this.type().method() + " flood: " + e.getMessage() );
					}
				case SHADOW_COPY:
					return new CommandResult.Failed( "Method 'SHADOW_COPY' not implemented" );
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.type().method().name() );
			}
		}

	}

	public static class Stop extends FloodCommand.Stop {

		public Stop( FloodMethod method, String id ) {
			super( method, id );
		}

		// todo: implement this
		@Override
		public CommandResult execute() {
			switch ( this.method() ) {
				case RANDOM:
					try {
						RanFlood.daemon().randomFlooder().stopFlood( UUID.fromString( this.id() ) );
						return new CommandResult.Successful( "Stopped " + this.method() + " flood, ID: " + id() );
					} catch ( FlooderException e ) {
						return new CommandResult.Failed( "Error trying to stop " + this.method() + " flood, ID: " + id() );
					}
				case ON_THE_FLY:
					try {
						RanFlood.daemon().onTheFlyFlooder().stopFlood( UUID.fromString( this.id() ) );
						return new CommandResult.Successful( "Stopped " + this.method() + " flood, ID: " + this.id() );
					} catch ( FlooderException e ) {
						return new CommandResult.Failed( "Error trying to stop " + this.method() + " flood, ID: " + this.id() );
					}
				case SHADOW_COPY:
					return new CommandResult.Failed( "Method 'SHADOW_COPY' not implemented" );
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.method().name() );
			}
		}

		@Override
		public String name() {
			return "Flood stop";
		}
	}

	public static class List extends FloodCommand.List {

		// todo: implement this
		@Override
		public java.util.List< RanFloodType.Tagged > execute() {
			return Stream.concat(
							RanFlood.daemon().randomFlooder()
											.currentRunningTasksSnapshotList().stream(),
							RanFlood.daemon().onTheFlyFlooder()
											.currentRunningTasksSnapshotList().stream()
			).map( t -> new RanFloodType.Tagged(
											t.floodTask().floodMethod(),
											t.floodTask().filePath().toAbsolutePath(),
											t.label().toString()
							)
			).collect( Collectors.toList() );
		}

	}

}
