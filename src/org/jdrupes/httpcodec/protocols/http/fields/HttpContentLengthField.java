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

/**
 * A specialization of {@link HttpIntField} that represents the
 * content-length.
 */
public class HttpContentLengthField extends HttpIntField
	implements Cloneable {

	/**
	 * Creates a new content-length field with the given value.
	 * 
	 * @param value the value
	 */
	public HttpContentLengthField(long value) {
		super(CONTENT_LENGTH, value);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpField#clone()
	 */
	@Override
	public HttpContentLengthField clone() {
		return (HttpContentLengthField)super.clone();
	}

	/**
	 * Creates a new object with a value obtained by parsing the given
	 * String.
	 * 
	 * @param text the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpContentLengthField fromString(String text)
			throws ParseException {
		long value;
		try {
			value = Long.parseLong(unquote(text));
		} catch (NumberFormatException e) {
			throw new ParseException(text, 0);
		}
		return new HttpContentLengthField(value);
	}
}
