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
import java.util.ArrayList;
import java.util.List;

import org.jdrupes.httpcodec.types.CommentedValue;
import org.jdrupes.httpcodec.types.CommentedValue.CommentedValueConverter;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.ListConverter_1;

/**
 * Represents products descriptions as used in the `User-Agent`
 * or `Server` field.
 */
public class HttpProductsDescriptionField 
	extends HttpListField<CommentedValue<String>> {

	public static final ProductsDescriptionConverter DESCRIPTIONS_CONVERTER 
		= new ProductsDescriptionConverter();
	
	public HttpProductsDescriptionField(String name, 
			List<CommentedValue<String>> items) {
		super(name, items, DESCRIPTIONS_CONVERTER);
	}

	/**
	 * Creates a new list field with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param name the field name
	 * @param text the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpProductsDescriptionField fromString(
			String name, String text) throws ParseException {
		return new HttpProductsDescriptionField(name, 
				DESCRIPTIONS_CONVERTER.fromFieldValue(text));
	}
	
	public static class ProductsDescriptionConverter 
		extends ListConverter_1<CommentedValue<String>> {

		private static final CommentedValueConverter<String> ITEM_CONVERTER
			= new CommentedValueConverter<>(Converters.STRING);
		
		public ProductsDescriptionConverter() {
			super(ITEM_CONVERTER, " ");
		}

		/* (non-Javadoc)
		 * @see org.jdrupes.httpcodec.types.Converter#fromFieldValue(java.lang.String)
		 */
		@Override
		public List<CommentedValue<String>> fromFieldValue(String text)
		        throws ParseException {
			List<CommentedValue<String>> result = new ArrayList<>();
			int pos = 0;
			while (pos < text.length()) {
				int length = HttpField.tokenLength(text, pos);
				if (length == 0) {
					throw new ParseException(
							"Must start with token: " + text, pos);
				}
				String product = text.substring(pos, pos + length);
				pos += length;
				if (pos < text.length() && text.charAt(pos) == '/') {
					pos += 1;
					length = HttpField.tokenLength(text, pos);
					if (length == 0) {
						throw new ParseException(
								"Token expected: " + text, pos);
					}
					product = product + text.substring(pos - 1, pos + length);
					pos += length;
				}
				List<String> comments = new ArrayList<>();
				while (pos < text.length()) {
					length = HttpField.whiteSpaceLength(text, pos);
					if (length == 0) {
						throw new ParseException(
								"Whitespace expected: " + text, pos);
					}
					pos += length;
					if (text.charAt(pos) != '(') {
						break;
					}
					length = HttpField.commentLength(text, pos);
					if (text.charAt(pos + length - 1) != ')') {
						throw new ParseException(
								"Comment end expected: " + text, pos + length - 1);
					}
					comments.add(text.substring(pos + 1, pos + length - 1));
					pos += length;
				}
				result.add(new CommentedValue<String>(product, 
						comments.size() == 0 ? null
						: comments.toArray(new String[comments.size()])));
			}
			return result;
		}
		
	}
}
