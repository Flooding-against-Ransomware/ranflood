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

import org.ranflood.common.utils.Pair;

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


    public static class FileInfo extends Pair<String, String> {

        public FileInfo(String absolute_path, String info) {
            super(absolute_path, info);
        }

        public String getAbsolutePath() {
            return this.left();
        }
        public String getInfo() {
            return this.right();
        }
    }

	public static class FileInfoList extends LinkedList<FileInfo> {

	}

}
