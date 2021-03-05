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
				name = "snapshot",
				mixinStandardHelpOptions = true,
				description = { "management of folder/file snapshots" },
				subcommands = {
								Snapshot.Take.class,
								Snapshot.Remove.class,
								Snapshot.List.class
				}
)
public class Snapshot implements Callable< Integer > {

	@Override
	public Integer call() throws Exception {
		new CommandLine( this ).usage( System.err );
		return 1;
	}

	@CommandLine.Command(
					name = "take",
					mixinStandardHelpOptions = true,
					description = { "take a new snapshot" }
	)
	static class Take implements Callable< Integer > {

		@CommandLine.Parameters( index = "0", arity = "1" )
		String method;

		@CommandLine.Parameters( index = "1..*", arity = "1..*" )
		Collection< File > targetFolders;

		@Override
		public Integer call() throws Exception {
			System.out.println( "Requested snapshot of a new folder." );
			System.out.println( "Selected method: " + method );
			System.out.println( "Target folders: " + targetFolders.stream()
							.map( t -> t.toPath().toAbsolutePath().toString() )
							.collect( Collectors.joining( ",\n"
											+ String.format( "%" + "Target folders: ".length() + "s", "" )
							) ) );
			return 0;
		}
	}

	@CommandLine.Command(
					name = "remove",
					mixinStandardHelpOptions = true,
					description = { "removes monitored folders" }
	)
	static class Remove implements Callable< Integer > {

		@CommandLine.Parameters( index = "0", arity = "1" )
		String method;

		@CommandLine.Parameters( index = "1..*", arity = "1..*" )
		Collection< File > targetFolders;

		@Override
		public Integer call() throws Exception {
			System.out.println( "Requested remove of new folders." );
			System.out.println( "Selected method: " + method );
			String label = "Target folders: ";
			System.out.println( label + targetFolders.stream()
							.map( t -> t.toPath().toAbsolutePath().toString() )
							.collect( Collectors.joining( ",\n" + Utils.padLeft( label.length() ) ) ) );
			return 0;
		}
	}


	@CommandLine.Command(
					name = "--list",
					aliases = "-l",
					mixinStandardHelpOptions = true,
					description = { "list the snapshots currently saved" }
	)
	static class List implements Callable< Integer > {

		@Override
		public Integer call() throws Exception {
			System.out.println( "Requested the list of snapshots." );
			return 0;
		}
	}

}
