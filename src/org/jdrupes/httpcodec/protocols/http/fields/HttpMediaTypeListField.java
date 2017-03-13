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

package org.jdrupes.httpcodec.protocols.http.fields;

import java.text.ParseException;
import java.util.Comparator;
import java.util.List;

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;

/**
 * A list of media types.
 */
public class HttpMediaTypeListField extends HttpListField<MediaType> {

	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this
	 * initial state, the field is invalid and no string representation
	 * can be generated. This constructor must be followed by method invocations
	 * that add values.
	 * 
	 * @param name the field name
	 */
	public HttpMediaTypeListField(String name) {
		super(name, Converters.MEDIA_TYPE_LIST_CONVERTER);
	}

	/**
	 * Creates a new header field object with the given field name and values.
	 * 
	 * @param name
	 *            the field name
	 * @param items
	 * 			  the items
	 */
	public HttpMediaTypeListField(String name, List<MediaType> items) {
		super(name, items, Converters.MEDIA_TYPE_LIST_CONVERTER);
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param name the field name
	 * @param text the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpMediaTypeListField fromString(String name, String text) 
			throws ParseException {
		return new HttpMediaTypeListField(
				name, Converters.MEDIA_TYPE_LIST_CONVERTER.fromFieldValue(text));
	}
	
	private static Comparator<MediaType> COMP = Comparator.nullsFirst(
			Comparator.comparing(mt -> mt.getParameter("q"),
					Comparator.nullsFirst(
							Comparator.comparing(Float::parseFloat)
							.reversed())));
			
	
	public void sortByWeightDesc() {
		sort(COMP);
	}
}
