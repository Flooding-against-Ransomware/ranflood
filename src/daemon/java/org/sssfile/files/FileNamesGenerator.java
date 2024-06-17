package org.sssfile.files;

import java.nio.file.Path;



public class FileNamesGenerator {

	private Path root;
	private int counter;


	public FileNamesGenerator(Path path) {
		root = path;
		counter = 0;
	}


	/**
	 * Generate `n` file names.
	 * @return the filenames
	 */
	public String generateShardPath() {

		return root.toString() + "_" + (counter++);
	}

}
