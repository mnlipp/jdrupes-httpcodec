/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2017 Michael N. Lipp
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

package org.jdrupes.httpcodec.types;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Represents a list of cookies with some additional methods.
 */
@SuppressWarnings("serial")
public class CookieList extends ArrayList<HttpCookie> {

	/**
	 * Creates a new empty list of strings.
	 */
	public CookieList() {
	}

	/**
	 * Creates a new empty list of strings with an initial capacity.
	 * 
	 * @param initialCapacity the capacity
	 */
	public CookieList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new list with items copied from the existing collection.
	 * 
	 * @param existing the existing collection
	 */
	public CookieList(Collection<HttpCookie> existing) {
		super(existing);
	}

	/**
	 * Returns the value for the cookie with the given name.
	 * 
	 * @param name the name
	 * @return the value if a cookie with the given name exists
	 */
	public Optional<String> valueForName(String name) {
		return stream().filter(cookie -> cookie.getName().equals(name))
				.findFirst().map(HttpCookie::getValue);
	}
}
