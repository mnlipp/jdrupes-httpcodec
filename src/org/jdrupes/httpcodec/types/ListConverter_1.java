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
import java.util.ArrayList;
import java.util.List;

import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * Used by by classes that convert field values which consist
 * of lists of values.
 * 
 * @param <I> the type of the items
 */
public class ListConverter_1<I> implements Converter<List<I>> {

	private Converter<I> itemConverter;
	// Used by default in RFC 7230, see section 7.
	private String delimiters = ",";
	
	/**
	 * Creates a new list converter with the given converter for the items
	 * and comma as item delimiter.
	 * 
	 * @param itemConverter the converter for the items
	 * @see "[ABNF List Extension](https://tools.ietf.org/html/rfc7230#section-7)"
	 */
	public ListConverter_1(Converter<I> itemConverter) {
		this.itemConverter = itemConverter;
	}

	/**
	 * Creates a new list converter with the given converter for the items
	 * and the given delimiters for the items. Any character in `delimiters`
	 * is considered a delimiter when parsing the string. The first
	 * character will be used when items are joined in a
	 * textual representation.
	 * 
	 * @param itemConverter the converter for the items
	 * @param delimiters the delimiters
	 */
	public ListConverter_1(Converter<I> itemConverter, String delimiters) {
		this.itemConverter = itemConverter;
		this.delimiters = delimiters;
	}

	/**
	 * Returns the item converter.
	 * 
	 * @return the itemConverter
	 */
	public Converter<I> getItemConverter() {
		return itemConverter;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.Converter#asFieldValue(java.lang.Object)
	 */
	@Override
	public String asFieldValue(List<I> value) {
		if (value.size() == 0) {
			throw new IllegalStateException(
			        "Field with list value may not be empty.");
		}
		boolean first = true;
		StringBuilder result = new StringBuilder();
		for (I e: value) {
			if (first) {
				first = false;
			} else {
				result.append(delimiters.charAt(0));
				result.append(' ');
			}
			result.append(itemConverter.asFieldValue(e));
		}
		return result.toString();
	}

	
	/* (non-Javadoc)
	 * @see Converter#fromFieldValue(java.lang.String)
	 */
	@Override
	public List<I> fromFieldValue(String text) throws ParseException {
		List<I> result = new ArrayList<>();
		ListItemizer itemizer = new ListItemizer(text, delimiters);
		while (true) {
			String nextRepr = itemizer.nextItem();
			if (nextRepr == null) {
				break;
			}
			result.add(itemConverter.fromFieldValue(nextRepr));
		}
		return result;
	}
	
}