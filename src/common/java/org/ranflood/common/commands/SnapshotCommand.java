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

package org.ranflood.common.commands;

import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.common.commands.types.CommandResult;

import static org.ranflood.common.RanfloodLogger.error;

public class SnapshotCommand {

	private SnapshotCommand() {
	}

	public static class Add extends AbstractCommand< CommandResult > {

		public Add( RanfloodType type ) {
			super( type, "Snapshot add" );
		}

	}

	public static class Remove extends AbstractCommand< CommandResult > {

		public Remove( RanfloodType type ) {
			super( type, "Snapshot remove" );
		}

	}

	public static class List implements Command< java.util.List< RanfloodType > > {

		@Override
		public java.util.List< RanfloodType > execute() {
			error( "Unsupported exception" );
			throw new UnsupportedOperationException( "Execution is not implemented by the AbstractCommand class" );
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
