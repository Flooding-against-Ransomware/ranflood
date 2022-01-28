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

package org.ranflood.common;

import org.ranflood.common.commands.transcoders.ParseException;

public enum FloodMethod {

	RANDOM,
	ON_THE_FLY,
	SHADOW_COPY;

	public static FloodMethod getMethod( String method ) throws ParseException {
		switch ( method ) {
			case "RANDOM":
				return FloodMethod.RANDOM;
			case "ON_THE_FLY":
				return FloodMethod.ON_THE_FLY;
			case "SHADOW_COPY":
				return FloodMethod.SHADOW_COPY;
			default:
				throw new ParseException( "Unrecognized method " + method );
		}
	}

}
