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

package org.ranflood.common.commands.transcoders;

import com.republicate.json.Json;
import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.FloodMethod;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JSONTranscoder {

	public static String wrapListRanfloodType( List< ? extends RanfloodType > l ) {
		Json.Object o = new Json.Object();
		Json.Array a = new Json.Array();
		l.forEach( i -> a.add( ranFloodTypeToJson( i ) ) );
		o.put( "list", a );
		return o.toString();
	}

	public static String wrapError( String m ) {
		Json.Object o = new Json.Object();
		o.put( "error", m );
		return o.toString();
	}

	public static String wrapSuccess( String m ) {
		Json.Object o = new Json.Object();
		o.put( "success", m );
		return o.toString();
	}

	public static String toJsonString( Command< ? > c ) throws ParseException {
		Json.Object o = commandToJson( c );
		if ( o.isEmpty() ) {
			throw new ParseException( "Unable to find a conversion scheme for command of type " + c.getClass().getName() );
		} else {
			return o.toString();
		}
	}

	private static Json.Object commandToJson( Command< ? > c ) {
		Json.Object obj = new Json.Object();
		if ( c instanceof SnapshotCommand.Add ) {
			obj.put( "command", "snapshot" );
			obj.put( "subcommand", "add" );
			obj.put( "parameters", ranFloodTypeToJson( ( ( SnapshotCommand.Add ) c ).type() ) );
		} else if ( c instanceof SnapshotCommand.Remove ) {
			obj.put( "command", "snapshot" );
			obj.put( "subcommand", "remove" );
			obj.put( "parameters", ranFloodTypeToJson( ( ( SnapshotCommand.Remove ) c ).type() ) );
		} else if ( c instanceof SnapshotCommand.List ) {
			obj.put( "command", "snapshot" );
			obj.put( "subcommand", "list" );
		} else if ( c instanceof FloodCommand.Start ) {
			obj.put( "command", "flood" );
			obj.put( "subcommand", "start" );
			obj.put( "parameters", ranFloodTypeToJson( ( ( FloodCommand.Start ) c ).type() ) );
		} else if ( c instanceof FloodCommand.Stop ) {
			obj.put( "command", "flood" );
			obj.put( "subcommand", "stop" );
			Json.Object o = new Json.Object();
			o.put( "id", ( ( ( FloodCommand.Stop ) c ).id() ) );
			o.put( "method", ( ( ( FloodCommand.Stop ) c ).method().name() ) );
			obj.put( "parameters", o );
		} else if ( c instanceof FloodCommand.List ) {
			obj.put( "command", "flood" );
			obj.put( "subcommand", "list" );
		}
		return obj;
	}

	private static < T extends RanfloodType > Json.Object ranFloodTypeToJson( T t ) {
		Json.Object parameters = new Json.Object();
		parameters.put( "method", t.method().name() );
		parameters.put( "path", t.path().toAbsolutePath().toString() );
		if ( t instanceof RanfloodType.Tagged ) {
			parameters.put( "id", ( ( RanfloodType.Tagged ) t ).id() );
		}
		return parameters;
	}

	/*
		{
			"command" : String,
			"subcommand" : String,
			"parameters" : Json.Object? {
				// Command- and subcommand- dependent
			}

	 */

	private final String m;

	private JSONTranscoder( String m ) {
		this.m = m;
	}

	public static Command< ? > fromJson( String m ) throws ParseException {
		return new JSONTranscoder( m ).fromJson();
	}

	private Command< ? > fromJson() throws ParseException {
		try {
			Json.Object jsonObject = Json.parse( m ).asObject();
			String command = getString( jsonObject, "command" );
			String subcommand = getString( jsonObject, "subcommand" );
			switch ( command ) {
				case "snapshot":
					return parseSnapshotCommand( jsonObject, subcommand );
				case "flood":
					return parseFloodCommand( jsonObject, subcommand );
				default:
					throw new ParseException( "Unrecognised command '" + command + "'." );
			}
		} catch ( IOException e ) {
			throw new ParseException( e.getMessage() );
		}
	}

	private Command< ? > parseSnapshotCommand( Json.Object jsonObject, String subcommand ) throws ParseException {
		switch ( subcommand ) {
			case "add":
				return new SnapshotCommand.Add( parseRanfloodType( getJsonObject( jsonObject, "parameters" ) ) );
			case "remove":
				return new SnapshotCommand.Remove( parseRanfloodType( getJsonObject( jsonObject, "parameters" ) ) );
			case "list":
				return new SnapshotCommand.List();
			default:
				throw new ParseException( "Unrecognized subcommand '" + subcommand + "'" );
		}
	}

	private Command< ? > parseFloodCommand( Json.Object jsonObject, String subcommand ) throws ParseException {
		switch ( subcommand ) {
			case "start":
				return new FloodCommand.Start( parseRanfloodType( getJsonObject( jsonObject, "parameters" ) ) );
			case "stop":
				return new FloodCommand.Stop(
								FloodMethod.getMethod( getString( getJsonObject( jsonObject, "parameters" ), "method" ) ),
								getString( getJsonObject( jsonObject, "parameters" ), "id" ) );
			case "list":
				return new FloodCommand.List();
			default:
				throw new ParseException( "Unrecognized subcommand '" + subcommand + "'" );
		}
	}

	@SuppressWarnings("unchecked")
	public < T extends RanfloodType > T parseRanfloodType( Json.Object jsonObject ) throws ParseException {
		Path path = Path.of( getString( jsonObject, "path" ) );
		FloodMethod method = FloodMethod.getMethod( getString( jsonObject, "method" ) );
		String id = jsonObject.getString( "id" );
		return ( T ) ( id == null ?
						new RanfloodType( method, path )
						: new RanfloodType.Tagged( method, path, id )
		);
	}

	private String getString( Json.Object o, String key ) throws ParseException {
		Object s = Optional.of( o.get( key ) )
						.orElseThrow( () -> new ParseException( "Missing '" + key + "' node in " + m ) );
		if ( s instanceof String ) {
			return ( String ) s;
		} else {
			throw new ParseException( "Wrong type of argument for '" + key + "' node in " + m );
		}
	}

	private Json.Object getJsonObject( Json.Object o, String key ) throws ParseException {
		Object jo = Optional.of( o.get( key ) )
						.orElseThrow( () -> new ParseException( "Missing '" + key + "' node in " + m ) );
		if ( jo instanceof Json.Object ) {
			return ( Json.Object ) jo;
		} else {
			throw new ParseException( "Wrong type of argument for '" + key + "' node in " + m );
		}
	}

	public static List< ? extends RanfloodType > parseDaemonCommandList( String list ) throws IOException {
		Json.Object object = Json.parse( list ).asObject();
		Json.Array array = object.getArray( "list" );
		return IntStream.range( 0, array.size() )
						.mapToObj( i -> {
							Json.Object e = array.getObject( i );
							try {
								FloodMethod m = FloodMethod.getMethod( e.getString( "method" ) );
								Path p = Path.of( e.getString( "path" ) );
								String id = e.getString( "id" );
								return ( id == null ) ? new RanfloodType( m, p ) : new RanfloodType.Tagged( m, p, id );
							} catch ( ParseException parseException ) {
								parseException.printStackTrace();
								return null;
							}
						} ).collect( Collectors.toList() );
	}

	public static List< ? extends RanfloodType > parseSnapshotList( String list ) throws IOException {
		Json.Object object = Json.parse( list ).asObject();
		Json.Array array = object.getArray( "list" );
		return IntStream.range( 0, array.size() )
						.mapToObj( i -> {
							Json.Object e = array.getObject( i );
							try {
								FloodMethod m = FloodMethod.getMethod( e.getString( "method" ) );
								Path p = Path.of( e.getString( "path" ) );
								String id = e.getString( "id" );
								return ( id == null ) ? new RanfloodType( m, p ) : new RanfloodType.Tagged( m, p, id );
							} catch ( ParseException parseException ) {
								parseException.printStackTrace();
								return null;
							}
						} ).collect( Collectors.toList() );
	}

}
