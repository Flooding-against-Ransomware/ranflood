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

package org.ranflood.common.commands.types;

import org.ranflood.common.FloodMethod;

import java.nio.file.Path;

public class RanfloodType {

	final private FloodMethod method;
	final private Path path;

	public RanfloodType( FloodMethod method, Path path ) {
		this.method = method;
		this.path = path;
	}

	public FloodMethod method() {
		return method;
	}

	public Path path() {
		return path;
	}

	public static class Tagged extends RanfloodType {

		private final String id;

		public Tagged( FloodMethod method, Path path, String id ) {
			super( method, path );
			this.id = id;
		}

		public String id() {
			return id;
		}
	}

}
