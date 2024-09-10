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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;



/**
 * Don't print in execution, but only when asked (or at the end), as IO is slow.
 * Turn on DEBUG_FLOOD to print in real time or collect more information
 * during execution.
 */
public class LoggerFlood extends Logger {

	/* logging levels */
	private final boolean debug_flood;
	
	// where to print restoration report
	private final Path path_report;

	/* stats */
	private int n_files_analyzed	= 0;
	private int n_files_deleted		= 0;
	private int n_files_encrypted	= 0;
	private int n_shards_created	= 0;
	private int n_shards_found		= 0;
	

	
	
	public LoggerFlood(int n, int k) {
		super(n, k);
		path_report = null;
		debug_flood = false;
	}

	/**
	 * 
	 * @param n
	 * @param k
	 * @param log path of logging file
	 * @param report_flood path of report file for flood
	 * @param debug_dev if true, print additional logs
	 * @param debug_flood if true, print logs in real time (slows the flooding)
	 */
	public LoggerFlood(
		int n, int k,
		Path log, Path report_flood,
		boolean debug_dev, boolean debug_flood
	) {
		super(n, k, log, debug_dev);

		if(report_flood != null)
			// create copy
			path_report = report_flood.toAbsolutePath();
		else
			path_report = null;

		this.debug_flood = debug_flood;
	}



	/**
	 * Add to report. Ignore `debug_flood`.
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
	 * Print summary. Ignore `debug_flood`.
	 */
	public void summary() {

		LocalDateTime now = LocalDateTime.now();
		Duration time_elapsed = Duration.between( time_start, now );
		String msg =
			"--- Summary ---\n" +
			now + "\t- total time elapsed (nanosecs):\t" + time_elapsed.toNanos()	+ "\n" +
			now + "\t- total time elapsed (millisecs):\t" + time_elapsed.toMillis()	+ "\n" +
			"Files:\t"	+ n_files_encrypted	+ " / " + n_files_analyzed + "\tencrypted out of total analyzed\n" +
			"\t"		+ n_files_deleted	+ " / " + n_files_analyzed + "\tdeleted out of total analyzed\n" +
			"Shards:\t"	+ n_shards_created	+ " created\n" +
			"\t"		+ n_shards_found	+ " found\n" +
			"--- End of Summary ---\n";
		logLine(msg);
		report(msg);
	}
	
	
	/* actions */


	public void deleteFile(Path path, boolean success) {

		if(success) n_files_deleted++;
			
		if(debug_flood) {
			String msg = success
				? "[Deleted]\tFile deleted: " + path
				: "[Error]\tCould not delete file, IO exception: " + path;
			logLine(msg);
			report(msg);
		}
	}

	public void encryptedOriginalFile(Path path, int shards_created) {
		n_files_encrypted++;
		n_shards_created += shards_created;
		if(debug_flood) {
			String msg = (shards_created > k)
				? "[Encrypted]\tFile encrypted, created " + shards_created + "/" + k + " shards: " + path
				: "[Error]\tCould not encrypt file, could only create " + shards_created + "/" + k + " shards: " + path ;
			logDebug(msg);
			report(msg);
		}
	}

	public void foundOriginalFile(Path path) {
		n_files_analyzed++;
		if(debug_flood) {
			String msg = "Found new original file: " + path;
			logDebug(msg);
		}
	}
	
	public void foundShard(Path path) {
		n_files_analyzed++;
		n_shards_found++;
		if(debug_flood) {
			String msg = "Found shard, skipping: " + path;
			logDebug(msg);
		}
	}


}
