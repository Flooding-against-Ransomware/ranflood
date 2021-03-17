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

public class FloodCommand {

	private FloodCommand(){}

	public static class Start extends AbstractCommand< CommandResult >{

		public Start( RanFloodType type ) {
			super( type );
		}

		// todo: implement this
		@Override
		public CommandResult execute() {
			return null;
		}
	}

	public static class Stop implements Command< CommandResult > {

		private final String id;

		public Stop( String id ) {
			this.id = id;
		}

		public String id() {
			return id;
		}

		// todo: implement this
		@Override
		public CommandResult execute() {
			return null;
		}
	}

	public static class List implements Command< java.util.List< RanFloodType.Tagged > >{

		// todo: implement this
		@Override
		public java.util.List< RanFloodType.Tagged > execute() {
			return null;
		}
	}





}
