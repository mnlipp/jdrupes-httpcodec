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
import java.util.stream.Collectors;

/**
 * Represents the list of cookies to be sent from client to server.
 * 
 * @see "[RFC 6265](https://tools.ietf.org/html/rfc6265)"
 */
public class HttpCookieListField extends HttpListField<HttpCookie>
	implements Cloneable {

	public static final ListConverter<HttpCookie> COOKIE_LIST_CONVERTER 
		= new ListConverter<HttpCookie>(new Converter<HttpCookie>() {
			
			@Override
			public String asFieldValue(HttpCookie value) {
				return value.toString();
			}

			@Override
			public HttpCookie fromFieldValue(String text)
			        throws ParseException {
				try {
					return HttpCookie.parse(text).get(0);
				} catch (IllegalArgumentException e) {
					throw new ParseException(text, 0);
				}
			}
		}, ";");
	
	/**
	 * Creates a new object with the field name "Cookie" and the given
	 * cookies.
	 * 
	 * @param value the cookies
	 */
	public HttpCookieListField(List<HttpCookie> value) {
		super(HttpField.COOKIE, value, COOKIE_LIST_CONVERTER);
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
		return new HttpCookieListField(COOKIE_LIST_CONVERTER.fromFieldValue(text));
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpListField#clone()
	 */
	@Override
	public HttpCookieListField clone() {
		return (HttpCookieListField)super.clone();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpField#cloneValue()
	 */
	@Override
	protected List<HttpCookie> cloneValue() {
		return getValue().stream()
				.map(c -> (HttpCookie)c.clone()).collect(Collectors.toList());
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
