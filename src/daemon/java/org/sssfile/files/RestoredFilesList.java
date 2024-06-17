package org.sssfile.files;

import java.util.HashMap;



public class RestoredFilesList extends HashMap<Integer, OriginalFile> {


	public void addShard(ShardFile shard) {

		OriginalFile original_file = get(shard.getOriginalFileHash());
		if(original_file == null) {
			original_file = shard.getOriginalFile();
			put(original_file.hashCode(), original_file);
		}
		
		original_file.addPart(shard.key, shard.secret);
		original_file.addShardPath(shard.path);

	}

}
