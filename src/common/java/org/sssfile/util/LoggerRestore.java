/******************************************************************************
 * Copyright 2024 (C) by Daniele D'Ugo <danieledugo1@gmail.com>               *
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

 package org.sssfile.util;

import org.sssfile.files.OriginalFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;



public class LoggerRestore {

	// where to print restoration report
	private final Path path_report;

	private final LoggerResult stats = new LoggerResult();

	/* logging levels */
	private final boolean debug_dev;
	private final boolean debug_restore;

	/* implementation */
	protected LocalDateTime time_start;



	/**
	 * 
	 * @param report_restore path of report file for restore
	 * @param debug_dev if true, print additional logs
	 */
	public LoggerRestore(
		Path report_restore,
		boolean debug_dev, boolean debug_restore
	) {
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
	 * @param message -
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
				System.err.println("IO Exception: Couldn't write line to log.");
			}
		}
	}

	/**
	 * Log at debug level.
	 * @param message -
	 */
	public void logDebug(String message) {
		if(debug_dev)
			logLine(message);
	}



	/* actions */

	public void start() {
		time_start = LocalDateTime.now();
		logLine("Start - " + time_start);
	}

	
	/**
	 * Add to report. Ignore `debug_restore`.
	 * @param message -
	 */
	public void report(String message) {

		if(path_report != null) {
			try {
				Files.writeString(path_report,
					message + System.lineSeparator(),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND
				);
			} catch (IOException e) {
				System.err.println("IO Exception: Couldn't write line to log.");
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
				original_file.path.toAbsolutePath().toString(),
				"Wrong checksum after recover (" + original_file.getHashBase64() + "), expected was " + hash_found
		));
		if(debug_restore) {
			String msg ="[Bad hash]\tRecovered file's checksum is incorrect: " + original_file.path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileErrorUnrecoverable(Path path, int k, int parts_recovered) {

		stats.n_files_unrecoverable++;
		stats.files_error_insufficient.add( new LoggerResult.FileInfo(
				path.toAbsolutePath().toString(),
				"Can't recover with only " + parts_recovered + " / " + k + " parts found"
		));
		if(debug_restore) {
			String msg =
				"[Unrecoverable]\tUnrecoverable file, with " + parts_recovered +
				"/" + k + " parts: " + path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileErrorReading(Path path) {

		stats.n_errors++;
		stats.files_error_other.add( new LoggerResult.FileInfo(
				null,
				"Got an error and couldn't read shard at " + path
		));
		if(debug_restore) {
			String msg = "[Error]\tCould not read shard, IO exception: " + path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileErrorWriting(Path path) {

		stats.n_errors++;
		stats.files_error_other.add( new LoggerResult.FileInfo(
				path.toAbsolutePath().toString(),
				"Got an error and couldn't write the recovered file (try again...)"
		));
		if(debug_restore) {
			String msg = "[Error]\tCould not write recovered file, IO exception: " + path;
			logLine(msg);
			report(msg);
		}
	}
	public void fileRestored(Path path) {

		stats.n_files_restored++;
		stats.files_recovered.add( new LoggerResult.FileInfo(
				path.toAbsolutePath().toString(),
				"Restored"
		));
		if(debug_restore) {
			String msg ="[Restored]\tRecovered file: " + path;
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
