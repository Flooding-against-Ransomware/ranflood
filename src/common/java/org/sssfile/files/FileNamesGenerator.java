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

 package org.sssfile.files;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;


public class FileNamesGenerator {

	private long counter;
	private static final FileNamesGenerator INSTANCE = new FileNamesGenerator();

	private FileNamesGenerator() {
		counter = 0;
	}


	/**
	 * Get a path "unique" relatively to FileNamesGenerator (using a counter).
	 * The new path is obtained from `base_path` inserting _COUNTER before the extension (last dot),
	 * or at the end if there's no extension.
	 * @param base_path -
	 * @return the filename
	 */
	public static Path getUniquePath(String base_path) {

		int ext_idx = base_path.indexOf('.');
		ext_idx = (ext_idx != -1) ? ext_idx : base_path.length();
		String	name	= base_path.substring(0, ext_idx),
				ext		= base_path.substring(ext_idx);
		do {
			INSTANCE.counter++;
		} while( Files.exists(Path.of(name + (INSTANCE.counter - 1) + ext)) );
		return Path.of(name + (INSTANCE.counter - 1) + ext);
	}

	/**
	 *
	 * @param base_path -
	 * @return base_path if such file doesn't exist, else getUniquePath(base_path)
	 */
	public static Path getUniquePathIfExists(Path base_path) {

		if(Files.exists(base_path)) return getUniquePath(base_path.toString());
		return base_path;
	}

}
