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

import java.text.ParseException;
import java.time.Instant;

import org.jdrupes.httpcodec.types.Converters;

/**
 * An HTTP Date/Time field as specified by 
 * [RFC 7231, Section 7.1.1.1](https://tools.ietf.org/html/rfc7231#section-7.1.1.1)
 */
public class HttpDateTimeField extends HttpField<Instant> {

	/**
	 * Creates a header field object with the given value.
	 * 
	 * @param name the field name
	 * @param value
	 *            the field value
	 */
	public HttpDateTimeField(String name, Instant value) {
		super(name, value, Converters.DATE_TIME_CONVERTER);
	}

	/**
	 * Creates a new header field object with a value obtained by parsing the
	 * given String.
	 * 
	 * @param name
	 *            the field name
	 * @param text
	 *            the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpDateTimeField fromString(String name, String text)
			throws ParseException {
		return new HttpDateTimeField(
				name, Converters.DATE_TIME_CONVERTER.fromFieldValue(text));
	}
	
}
