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

import picocli.CommandLine;
import org.ranflood.filechecker.runtime.Check;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(
				name = "check",
				mixinStandardHelpOptions = true,
				description = { "Check files against a checksum and produce a report" }
)
public class CheckCommand implements Callable< Integer > {

	@CommandLine.Parameters(
					index = "0",
					description = "the path to the checksum file"
	)
	private File checksumFile;

	@CommandLine.Parameters(
					index = "1",
					description = "the path to the report file"
	)
	private File reportFile;

	@CommandLine.Parameters(
					index = "2",
					description = "the path to the root folder of the files to check"
	)
	private File folder;

	@CommandLine.Option(
					names = { "--deep" },
					description = "Perform a deep check, comparing files absent from the checksum to find possible duplicates (coinciding signatures)"
	)
	private final Boolean deep = false;

	@Override
	public Integer call() {
		try {
			Check.run( checksumFile, folder, reportFile, deep );
			System.out.println( "Report of the check of folder " + folder + " saved in file " + reportFile.getAbsolutePath() );
		} catch ( IOException e ) {
			System.err.println( "Problem writing the report, " + e.getMessage() );
		}
		return 0;
	}

}
