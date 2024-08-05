/******************************************************************************
 * Copyright 2021 (C) by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>  *
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

package playground;

import com.codahale.shamir.Scheme;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;



public class TestSSS {


	private record SSSParameters(int n, int k, int bytes, int tries) {

	}

	private static final int TRIES = 1000;

	private static final SSSParameters[] SSS_PARAMETERS = new SSSParameters[]{
		//new SSSParameters(255, 2, 1000, TRIES),
		//new SSSParameters(255, 10, 1000, TRIES),
		//new SSSParameters(255, 50, 1000, TRIES),
		//new SSSParameters(255, 100, 1000, TRIES),
		//new SSSParameters(255, 150, 1000, TRIES),
		//new SSSParameters(255, 200, 1000, TRIES),
		//new SSSParameters(255, 255, 1000, TRIES),
		//new SSSParameters(2, 2, 1000, TRIES),
		//new SSSParameters(10, 2, 1000, TRIES),
		//new SSSParameters(50, 2, 1000, TRIES),
		//new SSSParameters(100, 2, 1000, TRIES),
		//new SSSParameters(200, 2, 1000, TRIES),
		//new SSSParameters(100, 2, 1000, TRIES),
		//new SSSParameters(200, 3, 1000, TRIES),
		//new SSSParameters(250, 10, 1000, TRIES),
		//new SSSParameters(10, 10, 1000, TRIES),
		//new SSSParameters(50, 50, 1000, TRIES),
		//new SSSParameters(100, 100, 1000, TRIES),
		//new SSSParameters(200, 190, 1000, TRIES),
		//new SSSParameters(200, 200, 1000, TRIES),
		//new SSSParameters(10, 10, 1000, TRIES),
		//new SSSParameters(20, 10, 1000, TRIES),
		//new SSSParameters(30, 10, 1000, TRIES),
		//new SSSParameters(40, 10, 1000, TRIES),
		//new SSSParameters(50, 10, 1000, TRIES),
		//new SSSParameters(100, 10, 1000, TRIES),
		//new SSSParameters(150, 10, 1000, TRIES),
		//new SSSParameters(200, 10, 1000, TRIES),
		//new SSSParameters(150, 5, 1000, TRIES),
		//new SSSParameters(150, 6, 1000, TRIES),
		//new SSSParameters(150, 7, 1000, TRIES),
		//new SSSParameters(150, 8, 1000, TRIES),
		//new SSSParameters(150, 9, 1000, TRIES),
		//new SSSParameters(150, 10, 1000, TRIES),
		//new SSSParameters(210, 2, 1000, TRIES),
		//new SSSParameters(220, 2, 1000, TRIES),
		//new SSSParameters(230, 2, 1000, TRIES),
		//new SSSParameters(240, 2, 1000, TRIES),
		//new SSSParameters(250, 2, 1000, TRIES),
		new SSSParameters(250, 10, 10000, TRIES),
		new SSSParameters(50, 50, 10000, TRIES),
	};


	/*
	// for 255, 2, 1000, 1000
	private static double RATIO_SPLIT	= 3.03 / 1000;
	private static double RATIO_JOIN	= 5.55 / 1000;

	// for 50, 50, 1000, 1000, relative to n*n=k*k=n*k (let's say n*k)
	private static double RATIO_DIFF_SPLIT	= 2.118 / 1000;
	// for 50, 48, 1000, 1000, relative to n*n
	private static double RATIO_DIFF_JOIN	= 0.94719 / 1000;
	*/



	public static void main( String[] args ) throws InterruptedException, IOException {

		SecureRandom random = new SecureRandom();


		for(SSSParameters params : SSS_PARAMETERS) {

			long time_split	= 0;
			long time_join	= 0;

			for (int t = 0; t < params.tries; t++) {

				Scheme sss = new Scheme(random, params.n, params.k);
				byte[] bytes = new byte[params.bytes];
				random.nextBytes(bytes);

				long time_start = System.nanoTime();
				Map<Integer, byte[]> parts = sss.split(bytes);
				long time_end = System.nanoTime();

				time_split += (time_end - time_start);

				time_start = System.nanoTime();
				sss.join(parts);
				time_end = System.nanoTime();

				time_join += (time_end - time_start);
			}

			double time_split_ms	= (double)time_split	/ params.tries / 1000 / 1000;
			double time_join_ms		= (double)time_join		/ params.tries / 1000 / 1000;

			/*
			double ratio_split	= time_split_ms	/ (params.n * params.k);
			double ratio_join	= time_join_ms	/ (params.n * params.n);
			*/

			// round to 3 digits
			time_split_ms	= round(time_split_ms, 3);
			time_join_ms	= round(time_join_ms, 3);

			/*
			// round to 5 digits and multiply by 1000
			ratio_split	= round(ratio_split, 5)	* 1000;
			ratio_join	= round(ratio_join, 5)		* 1000;

			double test_split	= testSplit(params.n, params.k);
			double test_join	= testJoin(params.n, params.n);

			// round
			test_split	= round(test_split, 3);
			test_join	= round(test_join, 3);

			double	diff_split		= time_split_ms - test_split;
			double	diff_split_n	= diff_split / (params.n) * 1000,
					diff_split_k	= diff_split / (params.k) * 1000,
					diff_split_nk	= diff_split / (params.n * params.k) * 1000,
					diff_split_nn	= diff_split / (params.n * params.n) * 1000,
					diff_split_kk	= diff_split / (params.n * params.n) * 1000;
			double	diff_join		= time_join_ms - test_join;
			double	diff_join_n		= diff_join / (params.n) * 1000,
					diff_join_k		= diff_join / (params.k) * 1000,
					diff_join_nk	= diff_join / (params.n * params.k) * 1000,
					diff_join_nn	= diff_join / (params.n * params.n) * 1000,
					diff_join_kk	= diff_join / (params.n * params.n) * 1000;

			// round
			diff_split		= round(diff_split, 5);
			diff_split_n	= round(diff_split_n, 5);
			diff_split_k	= round(diff_split_k, 5);
			diff_split_nk	= round(diff_split_nk, 5);
			diff_split_nn	= round(diff_split_nn, 5);
			diff_split_kk	= round(diff_split_kk, 5);

			diff_join		= round(diff_join, 5);
			diff_join_n		= round(diff_join_n, 5);
			diff_join_k		= round(diff_join_k, 5);
			diff_join_nk	= round(diff_join_nk, 5);
			diff_join_nn	= round(diff_join_nn, 5);
			diff_join_kk	= round(diff_join_kk, 5);

			System.out.println(
					"n, k, bytes = " + params.n + ", " + params.k + ", " + params.bytes +
					"\tsplit, join (ms/KB) = " + time_split_ms + "\t " + time_join_ms +
					"\t(ratio/KB) = " + ratio_split + "\t" + ratio_join +
					"\tsplit (test) = " + test_split + "\tdiffs = " + diff_split + ", " +
					//diff_split_n + ", " + diff_split_k + ", " + diff_split_nk + ", " + diff_split_nn + ", " + diff_split_kk +
					"\tjoin (test) = " + test_join + "\tdiffs = " + diff_join + ", " //+
					//diff_join_n + ", " + diff_join_k + ", " + diff_join_nk + ", " + diff_join_nn + ", " + diff_join_kk
			);
			*/

			System.out.println(
					"n, k, bytes = " + params.n + ", " + params.k + ", " + params.bytes +
					"\tsplit, join (ms/KB) = " + time_split_ms + "\t " + time_join_ms
			);
		}



	}



	/*
	public static double testSplit(int n, int k) {
		return RATIO_SPLIT * (n * k) + RATIO_DIFF_SPLIT * (n);
	}

	/**
	 * considering n parts; if using k should change implementation with k's
	 * @param n
	 * @param k
	 * @return
	 */
	/*
	public static double testJoin(int n, int k) {
		return RATIO_JOIN * (n * n) + RATIO_DIFF_JOIN * (n);
	}
	*/

	public static double round(double n, int decimals) {
		return (double)(long)(n * Math.pow(10, decimals)) / Math.pow(10, decimals);
	}

	/*
	 * Memory
	 */

	public static void printMemory(Runtime runtime) {
		long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
		System.out.println("memory: max=" + runtime.maxMemory() + " " + ", allocated=" + runtime.totalMemory() + ", free=" + runtime.freeMemory() + ", realFree=" + freeMemory);
	}
	public static long freeMemory(Runtime runtime) {
		long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
		return freeMemory;
	}

}