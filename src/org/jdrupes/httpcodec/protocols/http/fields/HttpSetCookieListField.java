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
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdrupes.httpcodec.types.Converters;

/**
 * Represents all "Set-Cookie" fields in a Response header. Several cookies are
 * actually set with several headers. However, to provide uniform access to all
 * header fields, they are converted to a field with a list of values in the
 * internal representation.
 * 
 * @see "[RFC 6265](https://tools.ietf.org/html/rfc6265)"
 */
public class HttpSetCookieListField extends HttpListField<HttpCookie> {

	/**
	 * Creates a new header field object with the field name "Set-Cookie".
	 */
	public HttpSetCookieListField() {
		super(HttpField.SET_COOKIE, Converters.COOKIE_CONVERTER);
	}

	/**
	 * Creates a new object and adds the set-cookie obtained by parsing the
	 * given String.
	 * 
	 * @param text
	 *            the string to parse
	 * @return this object for easy chaining
	 * @throws ParseException
	 *             if the input violates the field format
	 */
	public static HttpSetCookieListField fromString(String text)
	        throws ParseException {
		HttpSetCookieListField result = new HttpSetCookieListField();
		result.addAll(Converters.COOKIE_CONVERTER.fromFieldValue(text));
		return result;
	}
	
	/**
	 * Returns the string representation of this header field as it appears in
	 * an HTTP message. Set-Cookie is special because each cookie
	 * has a header line of its own.
	 * 
	 * @return the field as it occurs in a header
	 * 
	 * @see "[RFC 7230, 3.2.2](https://tools.ietf.org/html/rfc7230#section-3.2.2)"
	 */
	@Override
	public String asHeaderField() {
		return stream().map(cookie -> getName() + ": " + cookie.toString())
				.collect(Collectors.joining("\r\n"));
	}

	/**
	 * Returns the value for the cookie with the given name.
	 * 
	 * @param name
	 *            the name
	 * @return the value if a cookie with the given name exists
	 */
	public Optional<String> valueForName(String name) {
		return stream().filter(cookie -> cookie.getName().equals(name))
			.findFirst().map(HttpCookie::getValue);
	}
}
