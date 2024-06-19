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

package org.ranflood.filechecker.subcommands;

import org.ranflood.filechecker.runtime.Restore;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(
				name = "restore",
				mixinStandardHelpOptions = true,
				description = { "Restore the original files after a flooding (specifically for SSS)." }
)
public class RestoreCommand implements Callable< Integer > {

	@CommandLine.Parameters(
			index = "0",
			description = "the path to the checksum file"
	)
	private File checksumFile;

	@CommandLine.Parameters(
					index = "1",
					description = "the path to the report file"
	)
	private File report_file;

	@CommandLine.Parameters(
					index = "2",
					description = "the path to the root folder of the files to check"
	)
	private File folder;

	@CommandLine.Option(
					names = { "--debug" },
					description = "If also specified --logfile, print more debugging logs."
	)
	private Boolean debug = false;

	@CommandLine.Option(
					names = { "--delete" },
					description = "Also remove files created during flooding"
	)
	private Boolean delete = false;

	@CommandLine.Option(
					names = { "-l", "--logfile" },
					description = "Enable more logs and specify the log file where to print them."
	)
	private File log_file = null;


	@Override
	public Integer call() {
		try {
			Restore.run( checksumFile, folder, report_file, delete, log_file, debug );
			System.out.println( "Report of the check of folder " + folder + " saved in file " + report_file.getAbsolutePath() );
		} catch ( IOException e ) {
			System.err.println( "Problem writing the report, " + e.getMessage() );
		}
		return 0;
	}

}
