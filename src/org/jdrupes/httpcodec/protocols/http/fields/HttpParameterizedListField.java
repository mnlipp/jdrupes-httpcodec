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

import java.util.List;

import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.ListConverter;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jdrupes.httpcodec.types.ParameterizedValue.ParameterizedValueConverter;

/**
 * A HTTP header field with a list of values that can have
 * parameters.
 * 
 * @param <U> the type of the unparameterized value
 */
public class HttpParameterizedListField<U> 
	extends HttpListField<ParameterizedValue<U>> {

	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this initial state, the field is invalid and no string 
	 * representation can be generated. This constructor must be followed 
	 * by method invocations that add values.
	 * 
	 * @param name the field name
	 * @param listConverter the converter for the complete list content
	 */
	public HttpParameterizedListField(String name, 
			ListConverter<ParameterizedValue<U>> listConverter) {
		super(name, listConverter);
	}

	/**
	 * Creates a new header field object with the given field name and values.
	 * 
	 * @param name
	 *            the field name
	 * @param items
	 * 			  the items
	 * @param listConverter the converter for the complete list content
	 */
	public HttpParameterizedListField(String name,
			List<ParameterizedValue<U>> items,
			ListConverter<ParameterizedValue<U>> listConverter) {
		super(name, items, listConverter);
	}

	/**
	 * Convenience function to create a converter for a list 
	 * of parameterized values from a converter for a
	 * value (without parameters) in the list. 
	 * 
	 * @param valueConverter the converter for a value (without parameters)
	 * @return the converter for a list of parameterized values
	 */
	public static <T> ListConverter<ParameterizedValue<T>>
		createListConverter(Converter<T> valueConverter) {
		return new ListConverter<ParameterizedValue<T>>(
				new ParameterizedValueConverter<T>(valueConverter));
	}
}
