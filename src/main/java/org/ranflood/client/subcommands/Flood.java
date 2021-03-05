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

import picocli.CommandLine;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
				name = "flood",
				mixinStandardHelpOptions = true,
				description = { "flood management" },
				subcommands = {
								Flood.Start.class,
								Flood.Stop.class,
								Flood.List.class
				}
)
public class Flood implements Callable< Integer > {

	@Override
	public Integer call() throws Exception {
		new CommandLine( this ).usage( System.err );
		return 1;
	}

	@CommandLine.Command(
					name = "--list",
					aliases = "-l",
					mixinStandardHelpOptions = true,
					description = { "list the running floods" }
	)
	static class List implements Callable< Integer > {

		@Override
		public Integer call() throws Exception {
			System.out.println( "Requested the list of the running floods." );
			return 0;
		}
	}

	@CommandLine.Command(
					name = "start",
					mixinStandardHelpOptions = true,
					description = { "start flooding a list of folders" }
	)
	static class Start implements Callable< Integer > {

		@CommandLine.Parameters( index = "0", arity = "1" )
		String method;

		@CommandLine.Parameters( index = "1..*", arity = "1..*" )
		Collection< File > targetFolders;

		@Override
		public Integer call() throws Exception {
			System.out.println( "Requested the flooding of folders." );
			System.out.println( "Selected method: " + method );
			String label = "Target folders: ";
			System.out.println( label + targetFolders.stream()
							.map( t -> t.toPath().toAbsolutePath().toString() )
							.collect( Collectors.joining( ",\n" + Utils.padLeft( label.length() ) ) ) );
			return 0;
		}
	}

	@CommandLine.Command(
					name = "stop",
					mixinStandardHelpOptions = true,
					description = { "stop a running flood" }
	)
	static class Stop implements Callable< Integer > {

		@CommandLine.Parameters( index = "0", arity = "1" )
		String id;

		@Override
		public Integer call() throws Exception {
			System.out.println( "Requested stopping of flooding id: " + id );
			return 0;
		}
	}
}
