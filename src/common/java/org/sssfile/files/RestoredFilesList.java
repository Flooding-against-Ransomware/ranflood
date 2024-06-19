package org.sssfile.files;

import java.util.*;


public class RestoredFilesList extends LinkedHashMap<Integer, OriginalFile> {


	public void addOriginalFile(OriginalFile original_file) {
		put(original_file.hashCode(), original_file);
	}

	public void addShard(ShardFile shard) {

		OriginalFile original_file = get(shard.hashCode());
		if(original_file == null) {
			original_file = shard.getOriginalFile();
			addOriginalFile(original_file);
		}
		
		original_file.addPart(shard.key, shard.secret);
		original_file.addShardPath(shard.path);

	}


}
