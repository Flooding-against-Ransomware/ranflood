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

import java.util.*;


public class RestoredFilesList extends LinkedHashMap<Integer, OriginalFile> {


	public void addShard(ShardFile shard) {

		OriginalFile original_file = get(shard.hashCode());
		if(original_file == null) {
			original_file = shard.getOriginalFile();
			put(shard.hashCode(), original_file);
		}
		
		original_file.addPart(shard.key, shard.shard);
		original_file.addShardPath(shard.path);

	}


}
