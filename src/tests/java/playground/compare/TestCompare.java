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

package playground.compare;

import com.republicate.json.Json;
import org.ranflood.common.FloodMethod;
import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.daemon.Ranflood;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.ranflood.common.RanfloodLogger.log;


public class TestCompare {

	// these paths must already exist
	private static final Path	root_test = Path.of("/tmp/ranflood_testsite/"),
						        path_settings = root_test.resolve("settings.ini"),
						        path_attacked = root_test.resolve("attackedFolder/");

    private static final int TIME_FLOOD_MS  = 3000;
    private static final int TIME_INIT_MS   = 10000;


	public static void main( String[] args ) throws InterruptedException, IOException {


		Ranflood.main( new String[]{
				path_settings.toAbsolutePath().toString()
		});
		Thread.sleep( 1000 );



        /* On the fly */

        resetTestStructure();
        floodAndCountFiles(FloodMethod.ON_THE_FLY, TIME_FLOOD_MS, path_attacked);

        resetTestStructure();
        floodAndCountFiles(FloodMethod.ON_THE_FLY, TIME_FLOOD_MS * 2, path_attacked);

        /* SSS Ransomware */

        resetTestStructure();
        floodAndCountFiles(FloodMethod.SSS_RANSOMWARE, TIME_FLOOD_MS, path_attacked);

        resetTestStructure();
        floodAndCountFiles(FloodMethod.SSS_RANSOMWARE, TIME_FLOOD_MS * 2, path_attacked);

        /* SSS Exfiltration */

        resetTestStructure();
        floodAndCountFiles(FloodMethod.SSS_EXFILTRATION, TIME_FLOOD_MS, path_attacked);

        resetTestStructure();
        floodAndCountFiles(FloodMethod.SSS_EXFILTRATION, TIME_FLOOD_MS * 2, path_attacked);

	}




    private static void resetTestStructure() throws IOException {
        List< Path > filePaths = List.of(
                path_attacked.resolve( "Other" )
                //, path_attacked.resolve( "Other 2" )
        );

        // clear first
        if(Files.exists(path_attacked)) {
            deleteRecursive(path_attacked);
        }
        path_attacked.toFile().mkdirs();

        // we set the folders up for the copy
        for ( Path filePath : filePaths ) {
            if ( !filePath.toFile().exists()
                    || !filePath.toFile().isDirectory()
                    || Objects.requireNonNull( filePath.toFile().listFiles() ).length < 1
            ) {
                createTestStructure( filePath );
            }
        }
    }

	private static void createTestStructure( Path root ) {
		log( "Creating test folders structure" );
		List< Path > l = Arrays.asList(
				//root.resolve( "Application Data" ), root.resolve( "Application Data" ).resolve( "Other" ),
			   // root.resolve( "Other 2" ),
				root.resolve( "Other" )
		);
		l.forEach( f -> {
            floodAndCountFiles(FloodMethod.RANDOM, TIME_INIT_MS, root);
		} );
	}

    private static void flood(FloodMethod method, int duration, Path root) {
        try {
            sendCommand( new FloodCommand.Start(
                    new RanfloodType( method, root )) );

            Thread.sleep( 500 );
            UUID id = getFloodId(method);
            log("Started random with id " + id);

            Thread.sleep( duration - 500 );
            sendCommand( new FloodCommand.Stop(
                    method, id.toString()
            ));
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    private static long floodAndCountFiles(FloodMethod method, int duration, Path root) {
        flood(method, duration, root);

        try {
            long files_created = fileCount(path_attacked);
            Thread.sleep(500);
            log(method + " (" + duration + "ms) - files in " + path_attacked + ": " + files_created);
            long files_created_test = fileCount(path_attacked);
            Thread.sleep(500);
            log(method + " (" + duration + "ms) - files in " + path_attacked + ": " + files_created_test);

            // check it stopped adding files
            assert files_created == files_created_test;

            return files_created;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

	private static String sendCommand( Command< ? > c ) {
		try {
			return sendString( JSONTranscoder.toJsonString( c ) );
		} catch ( ParseException e ) {
			e.printStackTrace();
		}
		return "";
	}

	private static String sendString( String s ) {
		try ( ZContext context = new ZContext() ) {
			ZMQ.Socket socket = context.createSocket( SocketType.REQ );
			socket.connect( "tcp://localhost:7890" );
			socket.send( s );
			String response = new String( socket.recv(), ZMQ.CHARSET );
			log( "Client received [" + response + "]" );
			socket.close();
			context.destroy();
			return response;
		}
	}

	private static String sendCommandList( Command< ? > c ) {
		try ( ZContext context = new ZContext() ) {
			ZMQ.Socket socket = context.createSocket( SocketType.REQ );
			socket.connect( "tcp://localhost:7890" );
			String command = JSONTranscoder.toJsonString( c );
			socket.send( command );
			String response = new String( socket.recv(), ZMQ.CHARSET );
			socket.close();
			context.destroy();
			return response;
//			return Json.parse( response ).asObject().getArray( "list" ).getString( 0 );
		} catch ( ParseException e ) {
			e.printStackTrace();
			return "";
		}
	}

	private static UUID getFloodId( FloodMethod method ) {
		String _method;
		switch (method) {
			case RANDOM             -> _method = "RANDOM";
			case ON_THE_FLY         -> _method = "ON_THE_FLY";
			case SHADOW_COPY        -> _method = "SHADOW_COPY";
			case SSS_EXFILTRATION   -> _method = "SSS_EXFILTRATION";
			case SSS_RANSOMWARE     -> _method = "SSS_RANSOMWARE";
            default                 -> _method = "";
		}

		String response = sendCommand( new FloodCommand.List() );


        try {
            Json.Object json = (Json.Object) Json.parse(response) ;
            UUID id = null;
            for(Serializable item : json.getArray("list") ) {
                Json.Object obj = (Json.Object)item;
                if(obj.getString("method").equals(_method)) {
                    id = UUID.fromString(obj.getString("id"));
                    break;
                }
            }
            return id;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
	}

	private static long fileCount(Path dir) throws IOException {
		try( Stream<Path> files = Files.walk(dir) ) {
			return files.parallel()
					.filter(p -> !p.toFile().isDirectory())
					.count();
		}
	}

	public static void deleteRecursive(Path dir) throws IOException {
		try( Stream<Path> files = Files.walk(dir) ) {
			files.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

}