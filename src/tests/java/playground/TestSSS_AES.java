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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.SecureRandom;
import java.util.Map;


public class TestSSS_AES {


	private record SSSParameters(int n, int k, int bytes, int tries) {

	}

	private static SSSParameters[] SSS_PARAMETERS = new SSSParameters[]{
		new SSSParameters(3, 3, 1000, 1000),
		new SSSParameters(3, 3, 2000, 1000),
		new SSSParameters(3, 3, 5000, 1000),
		new SSSParameters(3, 3, 10000, 1000),
		new SSSParameters(3, 3, 100000, 1000),
		new SSSParameters(3, 3, 1000000, 1000),
	};


	public static void main( String[] args ) throws InterruptedException, IOException {


		SecureRandom random = new SecureRandom();
		Cipher cipher = null;
		KeyGenerator keyGen = null;
		SecretKey secretKey = null;

		try {
			keyGen = KeyGenerator.getInstance( "AES" );
		} catch ( Exception e ) {
			System.out.println( "Impossibile generare keyGenerator" );
			System.exit( 0 );
		}
		keyGen.init( 256 );

		secretKey = keyGen.generateKey();

		try {
			cipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
			cipher.init( Cipher.ENCRYPT_MODE, secretKey );
		} catch ( Exception e ) {
			System.out.println( "Error creating cipher" );
			System.exit( -2 );
		}



		for(SSSParameters params : SSS_PARAMETERS) {

			long time_split	= 0;
			long time_aes	= 0;

			for (int t = 0; t < params.tries; t++) {

				Scheme sss = new Scheme(random, params.n, params.k);
				byte[] bytes = new byte[params.bytes];
				random.nextBytes(bytes);

				long time_start = System.nanoTime();
				Map<Integer, byte[]> parts = sss.split(bytes);
				long time_end = System.nanoTime();

				time_split += (time_end - time_start);

				time_start = System.nanoTime();
				encrypt(bytes, cipher, secretKey);
				time_end = System.nanoTime();

				time_aes += (time_end - time_start);
			}

			double time_split_ms	= (double)time_split	/ params.tries / 1000 / 1000 / (params.bytes / 1000D);
			double time_aes_ms		= (double)time_aes		/ params.tries / 1000 / 1000 / (params.bytes / 1000D);

			// round to 3 digits
			time_split_ms	= round(time_split_ms, 4);
			time_aes_ms		= round(time_aes_ms, 4);

			System.out.println(
					"n, k, bytes = " + params.n + ", " + params.k + ", " + params.bytes +
					"\tsplit (ms/KB) = " + time_split_ms + "\taes (ms/KB) = " + time_aes_ms
			);

		}



	}



	public static byte[] encrypt(byte[] bytes, Cipher cipher, SecretKey secretKey ) {

		try {
			return cipher.doFinal( bytes );
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}
	}


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