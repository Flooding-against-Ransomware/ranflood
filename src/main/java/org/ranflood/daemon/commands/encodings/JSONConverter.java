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

package org.ranflood.daemon.commands.encodings;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.ranflood.daemon.commands.Command;
import org.ranflood.daemon.commands.FloodCommand;
import org.ranflood.daemon.commands.RanFloodType;
import org.ranflood.daemon.commands.SnapshotCommand;
import org.ranflood.daemon.flooders.FloodMethod;

import java.nio.file.Path;
import java.util.Optional;

public class JSONConverter {


	public static String toJson( Command< ? > c ) throws ParseException {
		JSONObject o = commandToJson( c );
		if( o.isEmpty() ){
			throw new ParseException( "Unable to find a conversion scheme for command of type " + c.getClass().getName() );
		} else {
			return o.toJSONString();
		}
	}

	private static JSONObject commandToJson( Command< ? > c ){
		JSONObject obj = new JSONObject();
		if( c instanceof SnapshotCommand.Add ){
			obj.put( "command", "snapshot" );
			obj.put( "subcommand", "add" );
			obj.put( "parameters", ranFloodTypeToJson( ( (SnapshotCommand.Add) c).type() ) );
		}
		if( c instanceof SnapshotCommand.Remove ){
			obj.put( "command", "snapshot" );
			obj.put( "subcommand", "remove" );
			obj.put( "parameters", ranFloodTypeToJson( ( (SnapshotCommand.Remove) c).type() ) );
		}
		if( c instanceof SnapshotCommand.List ){
			obj.put( "command", "snapshot" );
			obj.put( "subcommand", "list" );
		}
		if( c instanceof FloodCommand.Start ){
			obj.put( "command", "flood" );
			obj.put( "subcommand", "start" );
			obj.put( "parameters", ranFloodTypeToJson( ( (FloodCommand.Start) c).type() ) );
		}
		if( c instanceof FloodCommand.Stop ){
			obj.put( "command", "flood" );
			obj.put( "subcommand", "stop" );
			JSONObject o = new JSONObject();
			o.put( "id", ( ( (FloodCommand.Stop) c).id() ) );
			obj.put( "parameters", o );
		}
		if( c instanceof FloodCommand.List ){
			obj.put( "command", "flood" );
			obj.put( "subcommand", "list" );
		}
		return obj;
	}

	private static JSONObject ranFloodTypeToJson( RanFloodType t ){
		JSONObject parameters = new JSONObject();
		parameters.put( "method", t.method().name() );
		parameters.put( "path", t.path().toAbsolutePath().toString() );
		return parameters;
	}

	/*
		{
			"command" : String,
			"subcommand" : String,
			"parameters" : JSONObject? {
				// Command- and subcommand- dependent
			}

	 */

	private final String m;

	private JSONConverter( String m ){
		this.m = m;
	}

	public static Command< ? > fromJson( String m ) throws ParseException {
		return new JSONConverter( m ).fromJson();
	}

	private Command< ? > fromJson() throws ParseException {
		JSONParser parser = new JSONParser();
		try {
			JSONObject jsonObject = ( JSONObject ) parser.parse( m.toLowerCase() );
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
		} catch ( org.json.simple.parser.ParseException e ) {
			throw new ParseException( e.getMessage() );
		}
	}

	private Command< ? > parseSnapshotCommand( JSONObject jsonObject, String subcommand ) throws ParseException {
		switch ( subcommand ) {
			case "add" :
				return new SnapshotCommand.Add( parseRanFloodType( getJSONObject( jsonObject, "parameters" ) ) );
			case "remove" :
				return new SnapshotCommand.Remove( parseRanFloodType( getJSONObject( jsonObject, "parameters" ) ) );
			case "list" :
				return new SnapshotCommand.List();
			default:
				throw new ParseException( "Unrecognized subcommand '" + subcommand + "'" );
		}
	}

	private Command< ? > parseFloodCommand( JSONObject jsonObject, String subcommand ) throws ParseException {
		switch ( subcommand ) {
			case "start" :
				return new FloodCommand.Start( parseRanFloodType( getJSONObject( jsonObject, "parameters" ) ) );
			case "stop" :
				return new FloodCommand.Stop( getString( getJSONObject( jsonObject, "parameters" ), "id" ) );
			case "list" :
				return new FloodCommand.List();
			default:
				throw new ParseException( "Unrecognized subcommand '" + subcommand + "'" );
		}
	}

	private RanFloodType parseRanFloodType( JSONObject jsonObject ) throws ParseException {
		Path path = Path.of( getString( jsonObject, "path" ) );
		FloodMethod method = getMethod( getString( jsonObject, "method" ) );
		return new RanFloodType( method, path );
	}

	private String getString( JSONObject o, String key ) throws ParseException {
		Object s = Optional.of( o.get( key ) )
						.orElseThrow( () -> new ParseException( "Missing '" + key + "' node in " + m ) );
		if( s instanceof String ){
			return (String) s;
		} else {
			throw new ParseException( "Wrong type of argument for '" + key + "' node in " + m );
		}
	}

	private JSONObject getJSONObject( JSONObject o, String key ) throws ParseException {
		Object jo = Optional.of( o.get( key ) )
						.orElseThrow( () -> new ParseException( "Missing '" + key + "' node in " + m ) );
		if( jo instanceof JSONObject ){
			return (JSONObject) jo;
		} else {
			throw new ParseException( "Wrong type of argument for '" + key + "' node in " + m );
		}
	}

	private FloodMethod getMethod( String method ) throws ParseException {
		switch ( method ){
			case "random":
				return FloodMethod.RANDOM;
			case "on-the-fly":
				return FloodMethod.ON_THE_FLY;
			case "shadow-copy":
				return FloodMethod.SHADOW_COPY;
			default:
				throw new ParseException( "Unrecognized method " + method );
		}
	}

}
