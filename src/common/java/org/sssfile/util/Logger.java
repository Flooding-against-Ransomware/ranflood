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
import java.time.LocalDateTime;



public class Logger {

	protected final int n;
	protected final int k;
	
	// where to print all logs
	protected final Path path_log;

	/* logging levels */
	protected final boolean debug_dev;

	protected LocalDateTime time_start;



	public Logger(int n, int k) {

		this.n = n;
		this.k = k;
		
		path_log = null;

		debug_dev = false;
	}

	public Logger(int n, int k, Path log, boolean debug_dev) {
		
		this.n = n;
		this.k = k;

		if(log != null)
			// create copy
			path_log = log.toAbsolutePath();
		else
			path_log = null;

		this.debug_dev = debug_dev;
	}


	
	/**
	 * Log, with a newline.
	 * @param message
	 */
	public void logLine(String message) {
		System.out.println(message);

		if(path_log != null) {
			try {
				Files.writeString( path_log,
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



}
