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

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;

/**
 * Represents a media type header field and provides methods for interpreting
 * its value.
 */
public class HttpMediaTypeField extends HttpField<MediaType> {

	/**
	 * Creates new header field object with the given type and subtype and no
	 * parameters.
	 * 
	 * @param name
	 *            the field name
	 * @param type
	 *            the type
	 * @param subtype
	 *            the sub type
	 */
	public HttpMediaTypeField(String name, String type, String subtype) {
		super(name, new MediaType(type, subtype), Converters.MEDIA_TYPE_CONVERTER);
	}

	/**
	 * Creates new header field object with the name and type.
	 * 
	 * @param name
	 *            the field name
	 * @param type
	 *            the type
	 */
	public HttpMediaTypeField(String name, MediaType type) {
		super(name, type, Converters.MEDIA_TYPE_CONVERTER);
	}
	
	/**
	 * Creates a new representation of a media type field value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpMediaTypeField fromString(String name, String value)
	        throws ParseException {
		return new HttpMediaTypeField(
				name, Converters.MEDIA_TYPE_CONVERTER.fromFieldValue(value));
	}
	
}
