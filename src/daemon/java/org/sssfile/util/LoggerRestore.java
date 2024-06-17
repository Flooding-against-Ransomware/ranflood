package org.sssfile.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;



public class LoggerRestore extends Logger {

		/* logging levels */
		private final boolean debug_restore;
	
	// where to print restoration report
	private final Path path_report;

	/* stats */
	private int n_analyzed				= 0;
	private int n_original_files		= 0;
	private int n_errors				= 0;
	private int n_files_bad_hash		= 0;
	private int n_files_restored		= 0;
	private int n_files_unrecoverable	= 0;
	private int n_shards_deleted		= 0;
	private int n_shards_valid			= 0;
	

	
	public LoggerRestore(int n, int k) {
		super(n, k);
		path_report = null;
		debug_restore = false;
	}

	/**
	 * 
	 * @param n
	 * @param k
	 * @param log path of logging file
	 * @param report_restore path of report file for restore
	 * @param debug_dev if true, print additional logs
	 */
	public LoggerRestore(
		int n, int k,
		Path log, Path report_restore,
		boolean debug_dev, boolean debug_restore
	) {
		super(n, k, log, debug_dev);

		if(report_restore != null)
			// create copy
			path_report = report_restore.toAbsolutePath();
		else
			path_report = null;

		this.debug_restore = debug_restore;
	}


	
	/**
	 * Add to report. Ignore `debug_restore`.
	 * @param message
	 */
	public void report(String message) {

		if(path_report != null) {
			try {
				Files.writeString( path_report,
					message + System.lineSeparator(),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND
				);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

			
	/**
	 * Print summary. Ignore `debug_restore`.
	 */
	public void summary() {

		LocalDateTime now = LocalDateTime.now();
		Duration time_elapsed = Duration.between( time_start, now );
		String msg =
			"--- Summary ---\n" +
			now + "\t-\ttotal time elapsed (nanosecs): "	+ time_elapsed.toNanos()	+ "\n" +
			now + "\t-\ttotal time elapsed (millisecs): "	+ time_elapsed.toMillis()	+ "\n" +
			"Files:\t\t"		+ n_original_files		+ "\tpossible original files found\n" +
			"Recovered:\t"		+ n_files_restored		+ "\tfiles\n" +
			"Failed:\t"			+ n_files_unrecoverable	+ "\tfiles unrecoverable\n" +
			"\t"				+ n_files_bad_hash		+ "\tfiles have wrong checksum\n" +
			"Errors:\t\t"		+ n_errors				+ "\tfiles\n" +
			"Shards:\t"			+ n_shards_valid		+ " / " + n_analyzed		+ "\tvalid out of total files analyzed\n" +
			"\t"				+ n_shards_deleted		+ " / " + n_shards_valid	+ "\tdeleted out of total valid shards found\n" +
			"--- End of Summary ---\n";
		logLine(msg);
		report(msg);
	}


	/* actions */

	public void deleteShard(Path path, boolean success) {

		if(success) n_shards_deleted++;
			
		if(debug_restore) {
			String msg = success
				? "[Deleted]\tShard deleted: " + path
				: "[Error]\tCould not delete shard, IO exception: " + path;
			logLine(msg);
			report(msg);
		}
	}
	
	public void errorIO(Path original_file) {
		n_errors++;
		if(debug_restore) {
			String msg = "[Error]\tCould not write recovered file, IO exception: " + original_file;
			logLine(msg);
			report(msg);
		}
	}
	
	public void fileBadHash(Path original_file) {
		n_files_bad_hash++;
		if(debug_restore) {
			String msg ="[Bad hash]\tRecovered file's checksum is incorrect: " + original_file;
			logLine(msg);
			report(msg);
		}
	}

	public void fileRestored(Path original_file) {
		n_files_restored++;
		if(debug_restore) {
			String msg ="[Restored]\tRecovered file: " + original_file;
			logLine(msg);
			report(msg);
		}
	}
	
	public void fileUnrecoverable(Path original_file, int parts_recovered) {
		n_files_unrecoverable++;
		if(debug_restore) {
			String msg =
				"[Unrecoverable]\tUnrecoverable file, with " + parts_recovered +
				"/" + k + " parts: " + original_file;
			logLine(msg);
			report(msg);
		}

	}

	public void foundOriginalFile(Path path) {
		n_original_files++;
		if(debug_restore) {
			String msg = "Found shard for new original file: " + path;
			logDebug(msg);
		}
	}

	public void foundShard(Path path, boolean valid) {
		n_analyzed++;
		if(valid) n_shards_valid++;
		if(debug_restore) {
			String msg = "Analyzed shard: " + path + "; valid shard = " + valid;
			logDebug(msg);
		}
	}
	
	
}
