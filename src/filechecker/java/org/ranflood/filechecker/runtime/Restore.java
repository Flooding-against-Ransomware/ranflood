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

package org.ranflood.filechecker.runtime;

import com.republicate.json.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sssfile.SSSRestorer;
import org.sssfile.util.LoggerResult;



public class Restore {

	public static void run( File folder, File report,
							Boolean remove, Integer n, Integer k,
							File log, Boolean debug
	) throws IOException {

		/* check params */
		if ( !Files.exists( folder.toPath() ) )
			throw new IOException( "folder " + folder + " does not exist" );
		if ( !Files.isDirectory( folder.toPath() ) )
			throw new IOException( folder + " is not a directory" );

		/* run sss */
		Path file_log = (log != null) ? log.toPath() : null;
		SSSRestorer sss = new SSSRestorer(n, k, folder.toPath(), file_log, remove, file_log != null, debug );

		sss.start();
		LoggerResult stats = sss.getStats();

		/* collect and report logs */

		Json.Object files_recovered = new Json.Object();
		stats.files_recovered.forEach( (file_info) ->
				files_recovered.put(file_info.getPathString(), file_info.getInfo())
		);
		Json.Object files_error_checksum = new Json.Object();
		stats.files_error_checksum.forEach( (file_info) ->
				files_error_checksum.put(file_info.getPathString(), file_info.getInfo())
		);
		Json.Object files_error_insufficient = new Json.Object();
		stats.files_error_insufficient.forEach( (file_info) ->
				files_error_insufficient.put(file_info.getPathString(), file_info.getInfo())
		);
		Json.Object files_error_other = new Json.Object();
		stats.files_error_other.forEach( (file_info) ->
				files_error_other.put(file_info.getPathString(), file_info.getInfo())
		);

		Json.Object report_content = new Json.Object();
		report_content.put("Recovered", files_recovered);
		report_content.put("Error_checksum", files_error_checksum);
		report_content.put("Error_insufficient_shards", files_error_insufficient);
		report_content.put("Error_other", files_error_other);

		Files.writeString( report.toPath(), report_content.toString() );

	}

}
