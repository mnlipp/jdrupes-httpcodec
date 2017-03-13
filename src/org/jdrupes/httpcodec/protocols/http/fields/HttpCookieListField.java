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

package org.jdrupes.httpcodec.protocols.http.fields;

import java.net.HttpCookie;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import org.jdrupes.httpcodec.types.Converters;

/**
 * Represents the list of cookies to be sent from client to server.
 * 
 * @see "[RFC 6265](https://tools.ietf.org/html/rfc6265)"
 */
public class HttpCookieListField extends HttpListField<HttpCookie> {

	/**
	 * Creates a new object with the field name "Cookie" and the given
	 * cookies.
	 * 
	 * @param value the cookies
	 */
	public HttpCookieListField(List<HttpCookie> value) {
		super(HttpField.COOKIE, value, Converters.COOKIE_LIST_CONVERTER);
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param text the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpCookieListField fromString(String text) 
			throws ParseException {
		return new HttpCookieListField(Converters.COOKIE_LIST_CONVERTER.fromFieldValue(text));
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
