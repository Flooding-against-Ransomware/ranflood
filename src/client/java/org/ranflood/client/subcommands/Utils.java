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

package org.ranflood.client.subcommands;

import org.ranflood.common.FloodMethod;
import org.ranflood.common.commands.transcoders.ParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Utils {

	static private final Map< String, FloodMethod > translationMap = new HashMap<>();

	static {
		Arrays.asList( "random", "RANDOM" )
						.forEach( e -> translationMap.put( e, FloodMethod.RANDOM ) );
		Arrays.asList( "on-the-fly", "on_the_fly", "ON-THE-FLY", "ON_THE_FLY" )
						.forEach( e -> translationMap.put( e, FloodMethod.ON_THE_FLY ) );
		Arrays.asList( "shadow-copy", "shadow_copy", "SHADOW-COPY", "SHADOW_COPY" )
						.forEach( e -> translationMap.put( e, FloodMethod.SHADOW_COPY ) );
	}

	public static String padLeft( int padSize ) {
		return String.format( "%" + "Target folders: ".length() + "s", "" );
	}

	public static FloodMethod getMethod( String m ) throws ParseException {
		if ( translationMap.containsKey( m ) ) {
			return translationMap.get( m );
		} else {
			throw new ParseException( "Method " + m + " not supported" );
		}
	}

}
