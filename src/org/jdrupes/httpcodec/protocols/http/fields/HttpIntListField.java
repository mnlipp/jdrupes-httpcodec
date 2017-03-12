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
import java.util.Arrays;
import java.util.List;

/**
 * An HTTP field value that consists of a comma separated list of 
 * integers. The class provides a "list of integers" view
 * of the values.
 */
public class HttpIntListField extends HttpListField<Long>
	implements Cloneable {

	public static final Converter<List<Long>> INT_LIST_CONVERTER 
        = new HttpListField.ListConverter<Long>(HttpIntField.INT_CONVERTER);

	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this
	 * initial state, the field is invalid and no string representation
	 * can be generated. This constructor must be followed by method invocations
	 * that add values.
	 * 
	 * @param name the field name
	 */
	public HttpIntListField(String name) {
		super(name, INT_LIST_CONVERTER);
	}

	/**
	 * Creates the new object from the given values.
	 * 
	 * @param name the field name
	 * @param value the first value
	 * @param values more values
	 */
	public HttpIntListField(String name, Long value, Long... values) {
		super(name, INT_LIST_CONVERTER);
		add(value);
		addAll(Arrays.asList(values));
	}

	private HttpIntListField(String name, List<Long> value) {
		super(name, value, INT_LIST_CONVERTER);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpListField#clone()
	 */
	@Override
	public HttpIntListField clone() {
		return (HttpIntListField)super.clone();
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
	public static HttpIntListField fromString(String name, String text) 
			throws ParseException {
		return new HttpIntListField(name, INT_LIST_CONVERTER.fromFieldValue(text));
	}

}
