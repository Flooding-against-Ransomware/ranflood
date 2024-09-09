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

package org.sssfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;

import com.codahale.shamir.Scheme;

import org.sssfile.exceptions.InvalidOriginalFileException;
import org.sssfile.files.OriginalFile;
import org.sssfile.files.ShardFile;



public class SSSSplitter {

    private final Scheme scheme;

	public final int	n,
						k;
	public final long	generation;


	/**
	 * 
	 * @param n number of shards created
	 * @param k minimum number of shards required to rebuild the original file (0<=k<=n)
	 */
	public SSSSplitter(int n, int k) {
        SecureRandom random_generator = new SecureRandom();
		scheme = new Scheme(random_generator, n, k);
		this.n = n;
		this.k = k;
		this.generation = System.nanoTime();	// unique for each flood (for this instance)
	}


	/**
	 *
	 * @param path file path
	 * @param content file content
	 * @param checksum sha1 checksum of the original file - can be read with {@link OriginalFile}.readHash()
	 * @return the OriginalFile object
	 * @throws IOException while reading file content
	 * @throws InvalidOriginalFileException if the file is a shard
	 */
	public OriginalFile getSplitFile(
			Path path, byte[] content, byte[] checksum
	) throws IOException, InvalidOriginalFileException {

		if(ShardFile.isValid(content)) {
			throw new InvalidOriginalFileException("Can't split a shard again.");
		}

		Map<Integer, byte[]> parts = scheme.split(content);
		return new OriginalFile(path, checksum, parts, n, k, generation);
	}

}
