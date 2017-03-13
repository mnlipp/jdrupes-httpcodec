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

package org.jdrupes.httpcodec.types;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * A converter for values with parameter. Converts field values
 * such as `value; param1=value1; param2=value2`.
 * 
 * @param <T> the type of the value
 */
public class ParameterizedValueConverter<T>
	implements Converter<ParameterizedValue<T>> {

	private Converter<T> valueConverter;
	private Converter<String> paramValueConverter;
	boolean quoteIfNecessary = false;

	/**
	 * Creates a new converter by extending the given value converter
	 * with functionality for handling the parameters. Parameter
	 * values are used literally (no quoting).
	 * 
	 * @param valueConverter the converter for a value (without parameters)
	 */
	public ParameterizedValueConverter(Converter<T> valueConverter) {
		this(valueConverter, Converters.UNQUOTED_STRING_CONVERTER);
	}

	/**
	 * Creates a new converter by extending the given value converter
	 * with functionality for handling the parameters.
	 * 
	 * @param valueConverter the converter for a value (without parameters)
	 * @param paramValueConverter the converter for parameterValues
	 */
	public ParameterizedValueConverter(	Converter<T> valueConverter, 
			Converter<String> paramValueConverter) {
		this.valueConverter = valueConverter;
		this.paramValueConverter = paramValueConverter;
	}

	@Override
	public String asFieldValue(ParameterizedValue<T> value) {
		StringBuilder result = new StringBuilder();
		result.append(valueConverter.asFieldValue(value.getValue()));
		for (Entry<String, String> e: value.getParameters().entrySet()) {
			result.append("; ");
			result.append(e.getKey());
			result.append('=');
			result.append(paramValueConverter.asFieldValue(e.getValue()));
		}
		return result.toString();
	}

	@Override
	public ParameterizedValue<T> fromFieldValue(String text)
			throws ParseException {
		return fromFieldValue(text, ParameterizedValue<T>::new);
	}
	
	/**
	 * Used by classes that inherit from {@link ParameterizedValue}
	 * to return a instance of the derived type as result of the
	 * conversion.
	 * 
	 * @param text the textual representation
	 * @param resultConstructor a method that creates the result
	 * from an instance of the type and a map of parameters.
	 * @return the result
	 * @throws ParseException if the text is ill-formed
	 */
	public <R extends ParameterizedValue<T>> R fromFieldValue(String text, 
			BiFunction<T, Map<String,String>, R> resultConstructor)
			throws ParseException {
		ListItemizer li = new ListItemizer(text, ";");
		String valueRepr = li.nextItem();
		if (valueRepr == null) {
			throw new ParseException("Value may not be empty", 0);
		}
		T value = valueConverter.fromFieldValue(valueRepr);
		Map<String,String> params = new HashMap<>();
		while (true) {
			String param = li.nextItem();
			if (param == null) {
				break;
			}
			ListItemizer pi = new ListItemizer(param, "=");
			String paramKey = pi.nextItem().trim().toLowerCase();
			if (paramKey == null) {
				throw new ParseException("parameter may not be empty", 0);
			}
			String paramValue = pi.nextItem();
			if (paramValue != null) {
				paramValue = paramValueConverter.fromFieldValue(paramValue);
			}
			params.put(paramKey, paramValue);
		}
		return resultConstructor.apply(value, params);
	}
}