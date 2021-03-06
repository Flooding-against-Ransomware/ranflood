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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class TestSnapshot {

	public static void main( String[] args ) {

		Path filePath = Path.of( "/Users/thesave/Desktop/attackedFolder/folder1" );


		Arrays.stream( Objects.requireNonNull( filePath.toFile().listFiles() ) )
						.parallel().forEach( ( f ) -> {
							try ( InputStream input = new FileInputStream( f ) ) {
								byte[] bytes = input.readAllBytes();
								input.close();

								MessageDigest digest = MessageDigest.getInstance( "MD5" );
								digest.update( bytes );
								System.out.println(
												f.getAbsolutePath()
																+ ": " + Base64.getEncoder().encodeToString( digest.digest() )
								);
							} catch ( FileNotFoundException e ) {
								e.printStackTrace();
							} catch ( IOException e ) {
								e.printStackTrace();
							} catch ( NoSuchAlgorithmException e ) {
								e.printStackTrace();
							}
						});
	}

}
