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

package org.ranflood.daemon.flooders;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public abstract class Snapshooter {

	public static String getFileSignature( Path filePath ) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = new FileInputStream( filePath.toFile() ) ) {
			byte[] bytes = input.readAllBytes();
			input.close();
			return getBytesSignature( bytes );
		}
	}

	public static String getBytesSignature( byte[] bytes ) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
		digest.update( bytes );
		return Base64.getEncoder().encodeToString( digest.digest() );
	}

	public static String getPathSignature( Path p ) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
		digest.update( p.toAbsolutePath().toString().getBytes( StandardCharsets.UTF_8 ) );
		return Base64.getEncoder().encodeToString( digest.digest() )
						.replaceAll( "[/:=]", "" );
	}

}
