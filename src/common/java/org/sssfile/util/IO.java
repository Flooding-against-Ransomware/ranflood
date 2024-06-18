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
			filename = name + "(" + i + ")" + ext;
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
	 * @return -
	 * @throws IOException -
	 */
	public static Path createUniqueFile(SecureRandom random_generator, Path path) throws IOException {
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
		while(Files.exists(parent.resolve(filename))) {
			filename = name + "(" + i + ")" + ext;
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
