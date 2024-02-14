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

package org.ranflood.filechecker;

import picocli.CommandLine;
import org.ranflood.filechecker.subcommands.CheckCommand;
import org.ranflood.filechecker.subcommands.SaveCommand;

import java.util.concurrent.Callable;

@CommandLine.Command(
				name = "filechecker",
				mixinStandardHelpOptions = true,
				versionProvider = FileChecker.VersionProvider.class,
				description = { "Filechecker" },
				subcommands = {
								SaveCommand.class,
								CheckCommand.class
				}
)
public class FileChecker implements Callable< Integer > {

	public static final String version = "0.3";

	public static void main( String[] args ) {
		System.exit( run( args ) );
	}

	public static int run( String[] args ) {
		CommandLine c = new CommandLine( new FileChecker() );
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
			return new String[]{ "Filechecker version " + version };
		}
	}

}
