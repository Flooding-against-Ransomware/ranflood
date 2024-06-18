package org.sssfile.files;

import org.ranflood.common.utils.Pair;

import java.util.*;


public class RestoredFilesList extends HashMap<Integer, OriginalFile> {


	public void addOriginalFile(OriginalFile original_file) {
		put(original_file.hashCode(), original_file);
	}

	public void addShard(ShardFile shard) {

		OriginalFile original_file = get(shard.getOriginalFileHash());
		if(original_file == null) {
			original_file = shard.getOriginalFile();
			addOriginalFile(original_file);
		}
		
		original_file.addPart(shard.key, shard.secret);
		original_file.addShardPath(shard.path);

	}


}
