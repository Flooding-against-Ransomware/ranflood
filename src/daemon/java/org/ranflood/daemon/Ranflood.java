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

package org.ranflood.daemon;

import org.ranflood.common.utils.ProjectPropertiesLoader;

import java.io.IOException;
import java.nio.file.Path;

import static org.ranflood.common.RanfloodLogger.error;

public class Ranflood {

	private static RanfloodDaemon daemon;
	private static String version;

	static {
		try {
			version = ProjectPropertiesLoader.loadPropertyFile( Ranflood.class.getClassLoader() ).getProperty( "version" );
		} catch ( IOException exception ) {
			exception.printStackTrace();
		}
	}

	public static void main( String[] args ) {
		if ( args.length < 1 ) {
			error( "Expected 1 argument, path to the settings ini file." );
			System.exit( 1 );
		}
		try {
			daemon = new RanfloodDaemon( Path.of( args[ 0 ] ) );
			daemon.start();
		} catch ( IOException e ) {
			error( e.getMessage() );
		}
	}

	public static RanfloodDaemon daemon() {
		return daemon;
	}

	public static String version() {
		return version;
	}
}
