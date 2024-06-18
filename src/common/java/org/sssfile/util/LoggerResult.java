package org.sssfile.util;

import org.ranflood.common.utils.Pair;
import org.sssfile.files.OriginalFile;

import java.util.LinkedList;



public class LoggerResult {

	/* stats */
	public int n_analyzed				= 0;
	public int n_original_files		    = 0;
	public int n_errors				    = 0;
	public int n_files_bad_hash		    = 0;
	public int n_files_restored		    = 0;
	public int n_files_unrecoverable	= 0;
	public int n_shards_deleted		    = 0;
	public int n_shards_valid			= 0;

	/* Restored files, for report */
	public final FileInfoList files_recovered			= new FileInfoList();
	public final FileInfoList files_error_insufficient	= new FileInfoList();
	public final FileInfoList files_error_checksum		= new FileInfoList();
	public final FileInfoList files_error_other		    = new FileInfoList();


    public static class FileInfo extends Pair<OriginalFile, String> {

        public FileInfo(OriginalFile file, String info) {
            super(file, info);
        }

        public OriginalFile getFile() {
            return this.left();
        }
        public String getInfo() {
            return this.right();
        }
        public String getPathString() {
            return getFile().path.toString();
        }
    }

	public static class FileInfoList extends LinkedList<FileInfo> {

	}

}
