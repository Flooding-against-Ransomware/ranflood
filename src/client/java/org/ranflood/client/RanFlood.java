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

package org.ranflood.client;

import org.ranflood.client.subcommands.Flood;
import org.ranflood.client.subcommands.Snapshot;
import org.ranflood.common.utils.ProjectPropertiesLoader;
import picocli.CommandLine;

import java.util.Properties;
import java.util.concurrent.Callable;


@CommandLine.Command(
				name = "ranflood",
				mixinStandardHelpOptions = true,
				versionProvider = RanFlood.VersionProvider.class,
				description = { "The RanFlood client" },
				subcommands = {
								Snapshot.class,
								Flood.class
				}
)
public class RanFlood implements Callable< Integer > {

	public static void main( String[] args ) {
		System.exit( run( args ) );
	}

	public static int run( String[] args ) {
		CommandLine c = new CommandLine( new RanFlood() );
		c.setCaseInsensitiveEnumValuesAllowed( true );
		return c.execute( args );
	}

	@Override
	public Integer call() {
		new CommandLine( this ).usage( System.err );
		return 1;
	}

	static class VersionProvider implements CommandLine.IVersionProvider {

		@Override
		public String[] getVersion() throws Exception {
			Properties properties = ProjectPropertiesLoader.loadPropertyFile( RanFlood.class.getClassLoader() );
			return new String[]{ "Ranflood client version " + properties.getProperty( "version" ) };
		}
	}

}
