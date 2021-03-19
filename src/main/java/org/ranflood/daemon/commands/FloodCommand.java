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
import org.ranflood.daemon.flooders.FlooderException;
import org.ranflood.daemon.flooders.onTheFly.OnTheFlyFlooderException;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FloodCommand {

	private FloodCommand() {
	}

	public static class Start extends AbstractCommand< CommandResult > {

		public Start( RanFloodType type ) {
			super( type, "Flood start" );
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
					} catch ( OnTheFlyFlooderException e ) {
						return new CommandResult.Failed( "Error in launching " + this.type().method() + " flood: " + e.getMessage() );
					}
				case SHADOW_COPY:
					return new CommandResult.Failed( "Method 'SHADOW_COPY' not implemented" );
				default:
					return new CommandResult.Failed( "Unrecognized method: " + this.type().method().name() );
			}
		}

	}

	public static class Stop implements Command< CommandResult > {

		private final String id;
		private final FloodMethod method;

		public Stop( FloodMethod method, String id ) {
			this.id = id;
			this.method = method;
		}

		public String id() {
			return id;
		}

		public FloodMethod method() {
			return method;
		}

		// todo: implement this
		@Override
		public CommandResult execute() {
			switch ( this.method() ) {
				case RANDOM:
					try {
						RanFlood.daemon().randomFlooder().stopFlood( UUID.fromString( this.id ) );
						return new CommandResult.Successful( "Stopped " + this.method() + " flood, ID: " + id );
					} catch ( FlooderException e ) {
						return new CommandResult.Failed( "Error trying to stop " + this.method() + " flood, ID: " + id );
					}
				case ON_THE_FLY:
					try {
						RanFlood.daemon().onTheFlyFlooder().stopFlood( UUID.fromString( this.id ) );
						return new CommandResult.Successful( "Stopped " + this.method() + " flood, ID: " + id );
					} catch ( FlooderException e ) {
						return new CommandResult.Failed( "Error trying to stop " + this.method() + " flood, ID: " + id );
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

	public static class List implements Command< java.util.List< RanFloodType.Tagged > > {

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

		@Override
		public String name() {
			return "Flood list";
		}

		@Override
		public boolean isAsync() {
			return false;
		}
	}

}
