/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jdrupes.httpcodec;

/**
 * Represents a message header (either request or response) which preceeds
 * the option payload data. Header and payload together form the complete
 * message.
 */
public interface MessageHeader {

	/**
	 * Returns {@code true} if the header is followed by a payload.
	 * 
	 * @return {@code true} if payload data follows
	 */
	boolean hasPayload();
	
	/**
	 * Returns true if this is the final message received or transmitted
	 * on a connection.
	 * 
	 * @return the result
	 */
	boolean isFinal();

}
