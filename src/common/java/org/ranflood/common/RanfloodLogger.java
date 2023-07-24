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

package org.ranflood.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

public class RanfloodLogger {

	static private final Logger logger;

	static {
		System.setProperty( SimpleLogger.SHOW_LOG_NAME_KEY, "false" );
		System.setProperty( SimpleLogger.SHOW_DATE_TIME_KEY, "true" );
//		System.setProperty( SimpleLogger.DATE_TIME_FORMAT_KEY, "| yyyy-MM-dd'T'HH:mm:ss" );
		System.setProperty( SimpleLogger.DATE_TIME_FORMAT_KEY, "|" );
		System.setProperty( SimpleLogger.LOG_FILE_KEY, "System.out" );
		logger = LoggerFactory.getLogger( "Ranflood" );
	}

	public static void log( String s ) {
		logger.info( messagePrefix() + s + messageSuffix() );
	}

	public static void error( String s ) {
		logger.error( messagePrefix() + s + messageSuffix() );
	}

	private static String messagePrefix() {
		StackTraceElement t = Thread.currentThread()
						.getStackTrace()[ Math.min( Thread.currentThread().getStackTrace().length - 1, 4 ) ];
		return t.getClassName() + "." + t.getMethodName() + "\n| ";
	}

	private static String messageSuffix() {
		return "\n";
	}

}
