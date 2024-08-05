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
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.common.utils.Pair;
import org.ranflood.daemon.Ranflood;
import org.sssfile.files.ShardFile;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;


public class TestCompare {

	// these paths must already exist
	private static final Path	root_test		= Path.of("/tmp/ranflood_testsite/"),
								path_signatures	= root_test.resolve("signatures.db"),
								path_attacked	= root_test.resolve("attackedFolder/"),
								path_report		= root_test.resolve("report_" + LocalDateTime.now());
	// also print stats to a report file, so main progress is saved

	private record Settings(
			Path path,
			int ransomware_n,	int ransomware_k,	boolean ransomware_remove,
			int exfiltration_n,	int exfiltration_k,	boolean exfiltration_remove
			// k in percentage on n
	) {
	}

	// shutdown gives errors, so can't reinstantiate ranflood daemon in single execution
	private static final Settings[] paths_settings	= new Settings[]{
			//new Settings(root_test.resolve("settings1.ini"), 255, 2, false, 255, 90, true ),
			//new Settings(root_test.resolve("settings2.ini"), 50, 2, false, 50, 90, true ),
			//new Settings(root_test.resolve("settings3.ini"), 255, 90, false, 255, 2, true ),
			new Settings(root_test.resolve("settings4.ini"), 8, 2, false, 8, 90, true ),
	};

	private static Runtime runtime;


	private record TestEntry(FloodMethod method, boolean snapshots, int tries) {
	}
	private record TestTimes(int flood_ms, int init_ms, int snapshot_ms) {
	}
	// result in a format easy to use to build plots (with python/matplotlib)
	private static class TestResult {
		public final FloodMethod	method;
		public final boolean		snapshots;
		public final boolean		remove;
		public final int			n,
									k;
		public final ArrayList<Long>	init,
										files,
										shards,
										free_mem;

		public TestResult(TestEntry entry, boolean remove, int n, int k) {
			method		= entry.method;
			snapshots	= entry.snapshots;
			this.remove	= remove;
			this.n		= n;
			this.k		= k;
			init		= new ArrayList<>();
			files		= new ArrayList<>();
			shards		= new ArrayList<>();
			free_mem	= new ArrayList<>();
		}
		public String label() {
			return method + "_" + snapshots + "_" + n + "_" + k + "_" + remove;
		}
	}
	private static class TestResults extends LinkedHashMap<String, TestResult> {

		public void add(
				TestEntry entry,
				boolean remove, int n, int k,
				long init, long files, long shards, long free_mem
		) {
			TestResult result = get(entry);
			if(result == null) {
				result = new TestResult(entry, remove, n, k);
				put(result.label(), result);
			}
			result.init.add(init);
			result.files.add(files);
			result.shards.add(shards);
			result.free_mem.add(free_mem);
		}
	}

	private static final TestEntry[] tests = new TestEntry[]{
		//new TestEntry(FloodMethod.SSS_RANSOMWARE,	true	,	1),
		new TestEntry(FloodMethod.SSS_RANSOMWARE,	true	,	4),
		new TestEntry(FloodMethod.SSS_RANSOMWARE,	false	,	4),
		new TestEntry(FloodMethod.SSS_EXFILTRATION,	true	,	4),
		new TestEntry(FloodMethod.SSS_EXFILTRATION,	false	,	4),
		new TestEntry(FloodMethod.ON_THE_FLY,		true	,	4),
	};

	private static final TestTimes[] times = new TestTimes[]{
			new TestTimes(500, 2000, 5000),
			//new TestTimes(1500, 2000, 5000)
	};

	private static final TestResults results = new TestResults();




	public static void main( String[] args ) throws InterruptedException, IOException {

		runtime = Runtime.getRuntime();
		printMemory(runtime);

		for(Settings settings : paths_settings) {

			Path settings_path = Path.of(args[0]);

			Ranflood.main(new String[]{
					settings_path.toAbsolutePath().toString()
					//settings.path.toAbsolutePath().toString()
			});
			Thread.sleep(1000);

			for(TestTimes time : times) {
				for(TestEntry test : tests) {

					boolean remove =
							(test.method == FloodMethod.SSS_EXFILTRATION) ? settings.exfiltration_remove
							: (test.method == FloodMethod.SSS_RANSOMWARE) ? settings.ransomware_remove
							: false;
					int	n =
							(test.method == FloodMethod.SSS_EXFILTRATION) ? settings.exfiltration_n
									: (test.method == FloodMethod.SSS_RANSOMWARE) ? settings.ransomware_n
									: 0,
						k =
							(test.method == FloodMethod.SSS_EXFILTRATION) ? settings.exfiltration_k
									: (test.method == FloodMethod.SSS_RANSOMWARE) ? settings.ransomware_k
									: 0;

					for(int t = 0; t < test.tries; t++) {

						long files_init = resetTestStructure(time.init_ms);
						if (test.snapshots)
							snapshotTake(test.method, path_attacked, time.snapshot_ms);
						Pair<Long, Long> files_mem = floodAndCountFilesAndMemory(
								test.method, time.flood_ms, path_attacked );
						long shards = fileCountShards(path_attacked);

						String log = "DONE: "		+ test.method + "\n" +
								"\tflood time: "	+ time.flood_ms + "\ttime init: " + time.init_ms + "\ttime snapshot: " + time.snapshot_ms + "\n" +
								"\tn: "				+ n + "\tk: " + k + "\tremove: " + remove + "\tsnapshots: " + test.snapshots + "\n" +
								"\tfiles init: "	+ files_init + "\tfiles: " + files_mem.left() + "\tshards: " + shards + "\n" +
								"\tfree memory: "	+ files_mem.right() + "\n";
						System.out.println( log );
						printReport( log );

						results.add(
								test,
								remove, n, k,
								files_init, files_mem.left(), shards, files_mem.right()
						);
					}
				}
			}

			Thread.sleep(2000);
			// gives error
			//sendString("shutdown");
			//Thread.sleep(2000);
		}


		System.out.println("ENDED");
		printReport("ENDED\n");


		// print results in python-friendly format
		String out = "{\n";
		for( TestResult result : results.values() ) {
			out +=
					result.label() + " : {\n" +
					"init : "		+ result.init.toString()		+ ",\n" +
					"files : "		+ result.files.toString()		+ ",\n" +
					"shards : "		+ result.shards.toString()		+ ",\n" +
					"free_mem : "	+ result.free_mem.toString()	+ "\n"	+
					"},\n";
		}
		out += "}\n";

		System.out.println( out );
		printReport( out );

	}




	private static long resetTestStructure(int time_init) throws IOException {

		try {
			List<Path> filePaths = List.of(
					path_attacked.resolve("Other")
					//, path_attacked.resolve( "Other 2" )
			);

			// clear first
			if (Files.exists(path_attacked)) {
				fileDeleteRecursive(path_attacked);
				Thread.sleep(500);
			}
			path_attacked.toFile().mkdirs();

			// snapshots
			if (Files.exists(path_signatures)) {
				fileDeleteRecursive(path_signatures);
				Thread.sleep(500);
			}
			// we set the folders up for the copy
			for (Path filePath : filePaths) {
				if (!filePath.toFile().exists()
						|| !filePath.toFile().isDirectory()
						|| Objects.requireNonNull(filePath.toFile().listFiles()).length < 1
				) {
					createTestStructure(filePath, time_init);
				}
			}

			return fileCount(path_attacked);

		} catch (InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private static void createTestStructure( Path root, int time_init ) {
		System.out.println( "Creating test folders structure" );
		List< Path > l = Arrays.asList(
				//root.resolve( "Application Data" ), root.resolve( "Application Data" ).resolve( "Other" ),
			   // root.resolve( "Other 2" ),
				root.resolve( "Other" )
		);
		l.forEach( f -> {
			floodAndCountFilesAndMemory(FloodMethod.RANDOM, time_init, root);
		} );
	}

	private static Long flood(FloodMethod method, int duration, Path root) throws OutOfMemoryError {
		try {
			sendCommand( new FloodCommand.Start(
					new RanfloodType( method, root )) );

			Thread.sleep( 500 );
			UUID id = getFloodId(method);
			System.out.println("Started " + method + " with id " + id);

			Thread.sleep( duration - 500 );
			Long free_memory = freeMemory(runtime);
			sendCommand( new FloodCommand.Stop(
					method, id.toString()
			));
			Thread.sleep( 4000 );

			return free_memory;
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Also run gc().
	 * @param method
	 * @param duration
	 * @param root
	 * @return files count and free memory before stopping the flooder
	 */
	private static Pair<Long, Long> floodAndCountFilesAndMemory(FloodMethod method, int duration, Path root) {

		runtime.gc();
		Long free_memory = flood(method, duration, root);

		try {
			long files_created = fileCount(path_attacked);
			Thread.sleep(500);
			System.out.println(method + " (" + duration + "ms) - files in " + path_attacked + ": " + files_created);
			long files_created_test = fileCount(path_attacked);
			Thread.sleep(500);
			System.out.println(method + " (" + duration + "ms) - files in " + path_attacked + ": " + files_created_test);

			// check it stopped adding files
			assert files_created == files_created_test;

			return new Pair<>(files_created, free_memory);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void snapshotTake(FloodMethod method, Path root, int time_snapshot) {
		try {
			sendCommand( new SnapshotCommand.Add(
					new RanfloodType(method, root)) );

			Thread.sleep( time_snapshot );

			String response = sendCommand( new SnapshotCommand.List() );
			Thread.sleep( time_snapshot );

			System.out.println("Snapshots: " + response);
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
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
			System.out.println( "Client received [" + response + "]" );
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

	private static long fileCountShards(Path dir) throws IOException {
		try( Stream<Path> files = Files.walk(dir) ) {
			return files.parallel()
					.filter(p -> {
						try {
							return !p.toFile().isDirectory() && ShardFile.isValid(p);
						} catch (IOException e) {
							System.err.println("ERROR ShardFile.isValid(): " + e.getMessage() );
							return false;
						}
					})
					.count();
		}
	}

	public static void fileDeleteRecursive(Path dir) throws IOException {
		try( Stream<Path> files = Files.walk(dir) ) {
			files.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

	/*
	 * IO
	 */

	private static void printReport(String text) {
		try( BufferedWriter writer = new BufferedWriter(
				new FileWriter(path_report.toAbsolutePath().toString(), true)
		)) {
			writer.write(text);
		} catch (IOException e) {
			System.err.println("Error in writing to report: " + e.getMessage());
		}
	}

	/*
	 * Memory
	 */

	public static void printMemory(Runtime runtime) {
		long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
		System.out.println("memory: max=" + runtime.maxMemory() + " " + ", allocated=" + runtime.totalMemory() + ", free=" + runtime.freeMemory() + ", realFree=" + freeMemory);
	}
	public static long freeMemory(Runtime runtime) {
		long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
		return freeMemory;
	}

}