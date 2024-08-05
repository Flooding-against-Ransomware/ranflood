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
