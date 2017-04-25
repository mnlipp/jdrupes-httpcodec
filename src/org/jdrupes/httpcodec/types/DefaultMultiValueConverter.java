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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * Used by by converters that convert header fields with a list of values.
 * 
 * Minimal restrictions are imposed on the type used as container for the 
 * values. It must be {@link Iterable} to provide read access. A supplier
 * and a function for appending values provide the required write access.
 * 
 * @param <T> the container for the values
 * @param <V> the type of the values
 */
public class DefaultMultiValueConverter<T extends Iterable<V>, V> 
	implements MultiValueConverter<T, V> {

	private Supplier<T> containerSupplier;
	private BiConsumer<T, V> valueAdder;
	private Converter<V> valueConverter;
	// Used by default in RFC 7230, see section 7.
	private String delimiters = ",";
	private boolean separateValues = false;
	
	/**
	 * Create a new converter.
	 * 
	 * @param containerSupplier a function that creates a new empty container
	 * @param valueAdder a function that adds a value to the sequence
	 * @param valueConverter the converter for the individual values
	 * @param delimiters the delimiters
	 * @param separateValues indicates that each value should be represented
	 * by a header field of its own in a message header
	 */
	public DefaultMultiValueConverter(Supplier<T> containerSupplier, 
			BiConsumer<T, V> valueAdder, Converter<V> valueConverter, 
			String delimiters, boolean separateValues) {
		this.containerSupplier = containerSupplier;
		this.valueAdder = valueAdder;
		this.valueConverter = valueConverter;
		this.delimiters = delimiters;
		this.separateValues = separateValues;
	}

	/**
	 * Create a new converter for a container that implements {@link Collection}
	 * and does not generate separate header fields.
	 * 
	 * @param containerSupplier a function that creates a new empty list
	 * @param valueConverter the converter for the items
	 * @param delimiters the delimiters
	 */
	public DefaultMultiValueConverter(Supplier<T> containerSupplier, 
			Converter<V> valueConverter, String delimiters) {
		this(containerSupplier, 
				(left, right) -> { ((Collection<V>)left).add(right); },
				valueConverter, delimiters, false);
	}

	/**
	 * Create a new converter for a container that implements {@link Collection},
	 * does not generate separate header fields and uses a comma as separator.
	 * 
	 * @param containerSupplier a function that creates a new empty list
	 * @param itemConverter the converter for the items
	 * @see "[ABNF List Extension](https://tools.ietf.org/html/rfc7230#section-7)"
	 */
	public DefaultMultiValueConverter(
			Supplier<T> containerSupplier, Converter<V> itemConverter) {
		this(containerSupplier, (left, right) -> { ((List<V>)left).add(right); },
				itemConverter, ",", false);
	}

	@Override
	public boolean separateValues() {
		return separateValues;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.types.ListConverter#containerSupplier()
	 */
	@Override
	public Supplier<T> containerSupplier() {
		return containerSupplier;
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.types.ListConverter#valueAdder()
	 */
	@Override
	public BiConsumer<T, V> valueAdder() {
		return valueAdder;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.types.ListConverter#itemConverter()
	 */
	@Override
	public Converter<V> valueConverter() {
		return valueConverter;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.Converter#asFieldValue(java.lang.Object)
	 */
	@Override
	public String asFieldValue(T value) {
		Iterator<V> iterator = value.iterator();
		if (!iterator.hasNext()) {
			throw new IllegalStateException(
			        "Field with list value may not be empty.");
		}
		boolean first = true;
		StringBuilder result = new StringBuilder();
		while (iterator.hasNext()) {
			V element = iterator.next();
			if (first) {
				first = false;
			} else {
				result.append(delimiters.charAt(0));
				result.append(' ');
			}
			result.append(valueConverter.asFieldValue(element));
		}
		return result.toString();
	}

	
	/* (non-Javadoc)
	 * @see Converter#fromFieldValue(java.lang.String)
	 */
	@Override
	public T fromFieldValue(String text) throws ParseException {
		T result = containerSupplier.get();
		ListItemizer itemizer = new ListItemizer(text, delimiters);
		while (true) {
			String nextRepr = itemizer.nextItem();
			if (nextRepr == null) {
				break;
			}
			valueAdder.accept(result, valueConverter.fromFieldValue(nextRepr));
		}
		return result;
	}

}