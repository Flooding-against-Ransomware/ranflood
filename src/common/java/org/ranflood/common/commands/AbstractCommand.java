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

package org.ranflood.common.commands;

import org.ranflood.common.commands.types.RanfloodType;

import java.util.UUID;

public abstract class AbstractCommand< T > implements Command< T > {

	private final RanfloodType type;
	private final String name;

	public AbstractCommand( RanfloodType type, String name ) {
		this.type = type;
		this.name = name;
	}

	public RanfloodType type() {
		return type;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public T execute(UUID id) {
		throw new UnsupportedOperationException( "Execution is not implemented by the AbstractCommand class" );
	}
}
