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

import org.ranflood.client.binders.ZMQ_JSON_Client;
import org.ranflood.common.FloodMethod;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.RanFloodType;
import picocli.CommandLine;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.ranflood.client.subcommands.Utils.getMethod;
import static org.ranflood.common.RanFloodLogger.error;

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
	public Integer call() {
		new CommandLine( this ).usage( System.err );
		return 1;
	}

	@CommandLine.Command(
					name = "list",
					mixinStandardHelpOptions = true,
					description = { "list the running floods" }
	)
	static class List implements Callable< Integer > {

		@Override
		public Integer call() {
			System.out.println( "Requesting the list of the running floods." );
			FloodCommand.List c = new FloodCommand.List();
			java.util.List< RanFloodType.Tagged > l = ZMQ_JSON_Client.INSTANCE().sendListCommand( c );
			if ( l.isEmpty() ){
				System.out.println( "There are no running floods at the moment." );
			} else {
				System.out.println( l.stream()
								.map( r -> r.method().name() + " | " + r.path()	+ " | " + r.id() )
								.collect( Collectors.joining("\n")) );
			}
			return 0;
		}
	}

	@CommandLine.Command(
					name = "start",
					mixinStandardHelpOptions = true,
					description = { "start flooding a list of folders" }
	)
	static class Start implements Callable< Integer > {

		@CommandLine.Parameters(index = "0", arity = "1")
		String method;

		@CommandLine.Parameters(index = "1..*", arity = "1..*")
		Collection< File > targetFolders;

		@Override
		public Integer call() {
			System.out.println( "Requesting the start of the flooding." );
			try {
				FloodMethod m = getMethod( method );
				targetFolders.forEach( t -> {
					FloodCommand.Start c =
									new FloodCommand.Start( new RanFloodType( m, t.toPath().toAbsolutePath() ) );
					System.out.println( ZMQ_JSON_Client.INSTANCE().sendCommand( c ) );
				} );
				return 0;
			} catch ( ParseException e ) {
				error( "Method " + method + " not supported." );
				return 1;
			}
		}
	}

	@CommandLine.Command(
					name = "stop",
					mixinStandardHelpOptions = true,
					description = { "stop a running flood" }
	)
	static class Stop implements Callable< Integer > {

		@CommandLine.Parameters(index = "0", arity = "1")
		String method;

		@CommandLine.Parameters(index = "1", arity = "1")
		Collection< String > ids;

		@Override
		public Integer call() {
			System.out.println( "Requesting the stop of the flooding." );
			try {
				FloodMethod m = getMethod( method );
				ids.forEach( id -> {
					FloodCommand.Stop c =
									new FloodCommand.Stop( m, id );
					System.out.println( ZMQ_JSON_Client.INSTANCE().sendCommand( c ) );
				} );
				return 0;
			} catch ( ParseException e ) {
				error( "Method " + method + " not supported." );
				return 1;
			}
		}
	}
}
