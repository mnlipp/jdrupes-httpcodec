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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.ListConverter;
import org.jdrupes.httpcodec.types.ParameterizedValue;

/**
 * An HTTP field value that consists of a list of parameterized 
 * values with an optional "q" parameter.
 * 
 * @param <T> the type of the parameterized value
 */
public class HttpWeightedListField<T extends ParameterizedValue<?>>
	extends HttpListField<T> {

	/**
	 * See {@see #sortByWeightDesc()}.
	 */
	private static Comparator<ParameterizedValue<?>> COMP 
		= Comparator.nullsFirst(
			Comparator.comparing(mt -> mt.getParameter("q"),
					Comparator.nullsFirst(
							Comparator.comparing(Float::parseFloat)
							.reversed())));
	
	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this
	 * initial state, the field is invalid and no string representation
	 * can be generated. This constructor must be followed by method invocations
	 * that add values.
	 * 
	 * @param name the field name
	 * @param converter the converter for the list
	 */
	public HttpWeightedListField(String name, ListConverter<T> converter) {
		super(name, new ArrayList<T>(), converter);
	}

	/**
	 * Creates a new header field object with the given field name and values.
	 * 
	 * @param name
	 *            the field name
	 * @param items
	 * 			  the items
	 * @param converter 
	 * 			  the converter for the list
	 */
	public HttpWeightedListField(String name, List<T> items, 
			ListConverter<T> converter) {
		super(name, items, converter);
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param name the field name
	 * @param text the string to parse
	 * @param converter the converter for the list of items
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static <T extends ParameterizedValue<?>> 
		HttpWeightedListField<T> fromString(
			String name, String text, ListConverter<T> converter) 
			throws ParseException {
		
		return new HttpWeightedListField<>(
				name, (List<T>)converter.fromFieldValue(text), 
				(ListConverter<T>)converter);
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param name the field name
	 * @param text the string to parse
	 * @param itemConverter the converter for the items in the list
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static <T extends ParameterizedValue<?>> HttpWeightedListField<T> 
		fromString(String name, String text, Converter<T> itemConverter) 
			throws ParseException {
		
		ListConverter<T> listConverter = new ListConverter<>(itemConverter);
		return new HttpWeightedListField<>(
				name, listConverter.fromFieldValue(text), listConverter);
	}

	public void sortByWeightDesc() {
		sort(COMP);
	}
}
