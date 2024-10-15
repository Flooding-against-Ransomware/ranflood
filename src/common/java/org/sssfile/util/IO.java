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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;


public class IO {



	/**
	 * Create a file with the specified path.
	 * If the filename is already used, add a `(N)` before the first `.`
	 * (or at the end if there isn't any `.`),
	 * for the first number N available.
	 * @param path -
	 * @return -
	 * @throws IOException -
	 */
	public static Path createUniqueFile(Path path) throws IOException {
		Path parent	= path.getParent();
		String filename	= path.getFileName().toString();
		String name, ext;
		if(filename.indexOf('.') != -1) {
			name	= filename.substring(0, filename.indexOf('.'));
			ext		= filename.substring(filename.indexOf('.'));
		} else {
			name	= filename;
			ext		= "";
		}

		long i = 1;
		while(Files.exists(parent.resolve(filename))) {
			filename = name + "_" + i + ext;
			i++;
		}
		return Files.createFile( parent.resolve(filename) );
	}

	/**
	 * Create a file with the specified path.
	 * If the filename is already used, add a `(N)` before the first `.`
	 * (or at the end if there isn't any `.`),
	 * for the first number N available, starting from a random number.
	 * @param path -
	 * @param allow_original if true, use the given path if it's available
	 * @return -
	 * @throws IOException -
	 */
	public static Path createUniqueFile(
			SecureRandom random_generator, Path path, boolean allow_original
	) throws IOException {
		Path parent	= path.getParent();
		String filename	= path.getFileName().toString();
		String name, ext;
		if(filename.indexOf('.') != -1) {
			name	= filename.substring(0, filename.indexOf('.'));
			ext		= filename.substring(filename.indexOf('.'));
		} else {
			name	= filename;
			ext		= "";
		}

		long i = random_generator.nextLong();
		if(!allow_original) {
			filename = name + "_" + i + ext;
			i++;
		}
		while(Files.exists(parent.resolve(filename))) {
			filename = name + "_" + i + ext;
			i++;
		}
		return Files.createFile( parent.resolve(filename) );
	}

	/**
	 * Read data from file.
	 * @param path -
	 */
	public static byte[] readFile(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Read data from file.
	 * @param path -
	 * @param num number of bytes to read
	 * @param position starting byte to read
	 * @return -
	 */
	public static byte[] readBytes(Path path, int num, int position) throws IOException {
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.allocate(num);
			channel.read(buffer, position);
			return buffer.array();
		}
	}

	public static ByteBuffer readChannel(FileChannel channel, int num) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(num);
		channel.read(buffer);
		return buffer;
	}


}
