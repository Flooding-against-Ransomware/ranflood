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

package org.ranflood.daemon.flooders.random;

import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.flooders.AbstractFlooder;
import org.ranflood.common.FloodMethod;
import org.ranflood.daemon.flooders.tasks.LabeledFloodTask;

import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomFlooder extends AbstractFlooder {

	private final static FloodMethod METHOD = FloodMethod.RANDOM;
	private final double maxSize;

	public RandomFlooder() {
		this.maxSize = Math.pow( 10, 20.6 ); // ~4 MB
	}

	public double maxSize() {
		return maxSize;
	}

	public RandomFlooder( String maxSizeOption ) {
		Matcher m = Pattern.compile( "(\\d+)\\s*([mkMK])[bB]" ).matcher( maxSizeOption );
		if ( m.matches() ){
			int base = Integer.parseInt( m.group( 1 ) );
			int exp = m.group( 2 ).equalsIgnoreCase( "K" ) ? 3 : 6;
			maxSize = base * Math.pow( 10, exp );
		} else {
			throw new RuntimeException( "MaxFileSize option " + maxSizeOption + "does not conform to the expected format: \\d+\\s*[mkMK][bB]" );
		}
	}

	@Override
	public UUID flood( Path targetFolder ) {
		RandomFloodTask t = new RandomFloodTask( this, targetFolder, METHOD );
		UUID id = UUID.randomUUID();
		LabeledFloodTask lft = new LabeledFloodTask( id, t );
		addRunningTask( lft );
		Ranflood.daemon().floodTaskExecutor().addTask( lft );
		return id;
	}

}
