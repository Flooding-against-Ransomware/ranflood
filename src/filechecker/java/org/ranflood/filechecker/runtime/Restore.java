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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import org.ranflood.common.RanfloodLogger;
import org.ranflood.common.utils.Pair;
import org.sssfile.SSSRestorer;
import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.exceptions.UnrecoverableOriginalFileException;
import org.sssfile.files.OriginalFile;
import org.sssfile.util.IO;
import org.sssfile.util.LoggerResult;


public class Restore {

	public static void run( File checksum, File folder, File report,
							Boolean remove_shards,
							File log, Boolean debug
	) throws IOException {

		SecureRandom secure_random = new SecureRandom();

		/* check params */
		if ( !Files.exists( checksum.toPath().toAbsolutePath().getParent() ) )
			throw new IOException( "could not file checksum file " + checksum.toPath() );
		if ( !Files.exists( folder.toPath() ) )
			throw new IOException( "folder " + folder + " does not exist" );
		if ( !Files.isDirectory( folder.toPath() ) )
			throw new IOException( folder + " is not a directory" );
		Json checksum_json = Json.parse( Files.readString( checksum.toPath() ) );
		Map< String, String > checksum_map = checksum_json.asArray().
				stream()
				.map( e -> ( Json.Object ) e )
				.collect( Collectors.toMap(
						e -> e.get( "path" ).toString(),
						e -> e.get( "checksum" ).toString() )
				);

		/* run sss search */
		Path file_log = (log != null) ? log.toPath() : null;
		SSSRestorer sss = new SSSRestorer(folder.toPath(), file_log, file_log != null, debug );

		sss.findShards();
		LoggerResult stats = sss.getStats();

		/* write original files */
		LinkedList<Pair<Path, Path>>	files_path_conflict	= new LinkedList<>(),	// old/new path
										files_wrong_snapshot	= new LinkedList<>();
		LinkedList<Path>				files_error_io			= new LinkedList<>(),
										shards_error_delete				= new LinkedList<>();
		int shards_tot					= 0,
			original_files_error_get	= 0;
		int iterator_percentage = 0;

		Pair<OriginalFile, byte[]> original_file;
		while(true) {
			try {
				original_file = sss.iterateOriginalFile();	// also checks with original hash (saved in shards)
			} catch (InvalidOriginalFileException | UnrecoverableOriginalFileException e) {
				RanfloodLogger.error(e.getMessage());
				continue;
			} catch (NoSuchAlgorithmException e) {
				RanfloodLogger.error(e.getMessage());
				original_files_error_get++;
				continue;
			}
			if(original_file == null)
				break;

			// display completion percentage
			if(sss.getIteratorPercentage() > iterator_percentage) {
				iterator_percentage = sss.getIteratorPercentage();
				System.out.println("Writing original files: " + iterator_percentage + "%");
			}

            Path file_path = original_file.left().path.toAbsolutePath();
			String signature_snapshot = checksum_map.get(file_path.toString());
			String signature_found = null;

			// if a file with the same name already exists: if it has the same checksum skip, otherwise write with a new name
			boolean file_exists = Files.exists(file_path);
			if(file_exists) {
				try {
					signature_found = Utils.getFileSignature(file_path);
				} catch (NoSuchAlgorithmException | Utils.OutOfMemoryException e) {
					System.err.println(e.getMessage());
				}
			}

			if( signature_found == null || !signature_found.equals(original_file.left().getHashBase64()) ) {
				// change name if path or snapshot conflict

				/*	if snapshot doesn't contain the checksum, just continue writing the file (shards contain original hash);
					if snapshot doesn't match with checksum, use another name (since we already checked with shard's hash,
					snapshot was not up-to-date)
				 */
				if ( (signature_snapshot != null && !signature_snapshot.equals(original_file.left().getHashBase64())) ) {
					file_path = IO.createUniqueFile(secure_random, file_path, false);	// also avoid other name conflicts for already existing files
					files_wrong_snapshot.add(new Pair<>(original_file.left().path.toAbsolutePath(), file_path));
				} else if( file_exists ) {
					file_path = IO.createUniqueFile(secure_random, file_path, false);
					files_path_conflict.add(new Pair<>(original_file.left().path.toAbsolutePath(), file_path));
				}

				// don't write only if file with same checksum was found
				try {
					Files.write( file_path, original_file.right() );
				} catch (IOException e) {
					files_error_io.add(original_file.left().path.toAbsolutePath());
					// don't delete shards if original file is missing and couldn't be written
					continue;
				}
			}

			if(remove_shards) {
				for(Path shard_path : original_file.left().getShardsPaths()) {
					shards_tot++;
					try {
						Files.delete(shard_path);
					} catch (IOException e) {
						shards_error_delete.add(shard_path);
					}
				}
			}
		}

		/* collect and report logs */

		Json.Object json_files_recovered = new Json.Object();
		stats.files_recovered.forEach( (file_info) ->
				json_files_recovered.put(file_info.getAbsolutePath(), file_info.getInfo())
		);
		Json.Object json_files_path_conflict = new Json.Object();
		files_path_conflict.forEach( (file_info) ->
				json_files_path_conflict.put(file_info.right().toString(), "Original path was: " + file_info.right())
		);
		Json.Object json_files_wrong_snapshot = new Json.Object();
		files_wrong_snapshot.forEach( (file_info) ->
				json_files_wrong_snapshot.put(file_info.right().toString(), "Original path was: " + file_info.right())
		);
		Json.Object json_files_error_checksum = new Json.Object();
		stats.files_error_checksum.forEach( (file_info) ->
				json_files_error_checksum.put(file_info.getAbsolutePath(), file_info.getInfo())
		);
		Json.Object json_files_error_io = new Json.Object();
		files_error_io.forEach( (path) ->
				json_files_error_io.put(path.toString(), "Couldn't write")
		);
		Json.Object json_shards_error_delete = new Json.Object();
		shards_error_delete.forEach( (path) ->
				json_shards_error_delete.put(path.toString(), "Couldn't delete")
		);
		Json.Object json_files_error_insufficient = new Json.Object();
		stats.files_error_insufficient.forEach( (file_info) ->
				json_files_error_insufficient.put(file_info.getAbsolutePath(), file_info.getInfo())
		);
		Json.Object json_files_error_other = new Json.Object();
		stats.files_error_other.forEach( (file_info) ->
				json_files_error_other.put(file_info.getAbsolutePath(), file_info.getInfo())
		);
		json_files_error_other.put("Shards total", shards_tot);
		json_files_error_other.put("Shards deleted", shards_tot - shards_error_delete.size());
		json_files_error_other.put("Restored files not written for other errors", original_files_error_get);

		Json.Object report_content = new Json.Object();
		report_content.put("Recovered", json_files_recovered);
		report_content.put("Recovered, but changed name because a different file with the same name was found", json_files_path_conflict);
		report_content.put("Recovered, but changed name because snapshot has a different checksum", json_files_wrong_snapshot);
		report_content.put("Couldn't write these files, retry.", json_files_error_io);
		report_content.put("Couldn't delete these shards, but they can be removed safely as they were already recovered", json_shards_error_delete);
		report_content.put("Error checksum", json_files_error_checksum);
		report_content.put("Error insufficient shards", json_files_error_insufficient);
		report_content.put("Error other", json_files_error_other);
		report_content.put("Stats", json_files_error_other);

		Files.writeString( report.toPath(), report_content.toString() );

	}

}
