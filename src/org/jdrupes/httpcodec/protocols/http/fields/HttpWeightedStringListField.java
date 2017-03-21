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
import java.util.List;

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.ListConverter_1;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jdrupes.httpcodec.types.ParameterizedValue.ParameterizedValueConverter;

/**
 * A weighted list field with string values. 
 */
public class HttpWeightedStringListField 
	extends HttpWeightedListField<ParameterizedValue<String>> {

	private static final ListConverter_1<ParameterizedValue<String>>
		LIST_CONVERTER = new ListConverter_1<>(
				new ParameterizedValueConverter<>(
						Converters.STRING));
	
	/**
	 * Creates a new instance.
	 * 
	 * @param name the field name
	 * @param items the content
	 */
	public HttpWeightedStringListField(
			String name, List<ParameterizedValue<String>> items) {
		super(name, items, LIST_CONVERTER);
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
	public static HttpWeightedStringListField fromString(
			String name, String text) throws ParseException {
		return new HttpWeightedStringListField(
				name, LIST_CONVERTER.fromFieldValue(text));
	}

}
