package org.sssfile.util;

import org.ranflood.common.utils.Pair;
import org.sssfile.files.OriginalFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;


public class LoggerRestore {

	private final int n;
	private final int k;

	// where to print restoration report
	private final Path path_report;

	private final LoggerResult stats = new LoggerResult();

	/* logging levels */
	private final boolean debug_dev;
	private final boolean debug_restore;

	/* implementation */
	protected LocalDateTime time_start;



	public LoggerRestore(int n, int k) {
		this.n = n;
		this.k = k;
		path_report		= null;
		debug_dev		= false;
		debug_restore	= false;
	}

	/**
	 * 
	 * @param n
	 * @param k
	 * @param report_restore path of report file for restore
	 * @param debug_dev if true, print additional logs
	 */
	public LoggerRestore(
		int n, int k,
		Path report_restore,
		boolean debug_dev, boolean debug_restore
	) {
		this.n = n;
		this.k = k;
		this.debug_dev = debug_dev;
		this.debug_restore = debug_restore;

		if(report_restore != null)
			// create copy
			path_report = report_restore.toAbsolutePath();
		else
			path_report = null;
	}


	public LoggerResult getStats() {
		return stats;
	}



	/* simple logging */

	/**
	 * Log, with a newline.
	 * @param message
	 */
	public void logLine(String message) {
		System.out.println(message);

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
	 * Log at debug level.
	 * @param message
	 */
	public void logDebug(String message) {
		if(debug_dev)
			logLine(message);
	}



	/* actions */

	public void start() {
		time_start = LocalDateTime.now();
		logLine("Start - (n, k) = (" + n + ", " + k + ") - " + time_start);
	}

	
	/**
	 * Add to report. Ignore `debug_restore`.
	 * @param message
	 */
	public void report(String message) {

		if(path_report != null) {
			try {
				Files.writeString(path_report,
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
			"Files:\t\t"		+ stats.n_original_files		+ "\tpossible original files found\n" +
			"Recovered:\t"		+ stats.n_files_restored		+ "\tfiles\n" +
			"Failed:\t"			+ stats.n_files_unrecoverable	+ "\tfiles unrecoverable\n" +
			"\t"				+ stats.n_files_bad_hash		+ "\tfiles have wrong checksum\n" +
			"Errors:\t\t"		+ stats.n_errors				+ "\tfiles\n" +
			"Shards:\t"			+ stats.n_shards_valid			+ " / " + stats.n_analyzed		+ "\tvalid out of total files analyzed\n" +
			"\t"				+ stats.n_shards_deleted		+ " / " + stats.n_shards_valid	+ "\tdeleted out of total valid shards found\n" +
			"--- End of Summary ---\n";
		logLine(msg);
		report(msg);
	}


	/* actions */

	/* on original files */

	public void deleteShard(Path path, boolean success) {

		if(success) stats.n_shards_deleted++;
			
		if(debug_restore) {
			String msg = success
				? "[Deleted]\tShard deleted: " + path
				: "[Error]\tCould not delete shard, IO exception: " + path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileErrorBadHash(OriginalFile original_file, String hash_found) {

		stats.n_files_bad_hash++;
		stats.files_error_checksum.add( new LoggerResult.FileInfo(
				original_file,
				"Wrong checksum after recover (" + original_file.getHashString() + "), expected was " + hash_found
		));
		if(debug_restore) {
			String msg ="[Bad hash]\tRecovered file's checksum is incorrect: " + original_file.path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileErrorUnrecoverable(OriginalFile original_file, int parts_recovered) {

		stats.n_files_unrecoverable++;
		stats.files_error_insufficient.add( new LoggerResult.FileInfo(
				original_file,
				"Can't recover with only " + parts_recovered + " / " + k + " parts found"
		));
		if(debug_restore) {
			String msg =
				"[Unrecoverable]\tUnrecoverable file, with " + parts_recovered +
				"/" + k + " parts: " + original_file.path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileErrorWriting(OriginalFile original_file) {

		stats.n_errors++;
		stats.files_error_other.add( new LoggerResult.FileInfo(
				original_file,
				"Got an error and couldn't write the recovered file (try again...)"
		));
		if(debug_restore) {
			String msg = "[Error]\tCould not write recovered file, IO exception: " + original_file.path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileRestored(OriginalFile original_file) {

		stats.n_files_restored++;
		stats.files_recovered.add( new LoggerResult.FileInfo(
				original_file,
				"Restored"
		));
		if(debug_restore) {
			String msg ="[Restored]\tRecovered file: " + original_file.path;
			logLine(msg);
			report(msg);
		}
	}


	/* found files - for debug */

	public void foundOriginalFile(Path path) {

		stats.n_original_files++;
		if(debug_restore) {
			String msg = "Found shard for new original file: " + path;
			logDebug(msg);
		}
	}
	public void foundShard(Path path, boolean valid) {

		stats.n_analyzed++;
		if(valid) stats.n_shards_valid++;
		if(debug_restore) {
			String msg = "Analyzed shard: " + path + "; valid shard = " + valid;
			logDebug(msg);
		}
	}
	
	
}
