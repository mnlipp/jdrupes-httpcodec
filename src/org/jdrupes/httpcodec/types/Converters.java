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

import java.net.HttpCookie;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.jdrupes.httpcodec.protocols.http.HttpConstants;
import org.jdrupes.httpcodec.types.MediaBase.MediaTypePair;
import org.jdrupes.httpcodec.types.MediaBase.MediaTypePairConverter;
import org.jdrupes.httpcodec.types.MediaRange.MediaRangeConverter;
import org.jdrupes.httpcodec.types.MediaType.MediaTypeConverter;

/**
 * Utility methods and singletons for converters.
 */
public final class Converters {

	/*
	 * Note that the initialization sequence is important.
	 * Converters used by others must be defined first.
	 */

	/**
	 * A noop converter, except that text is trimmed when converted to
	 * a value.
	 */
	public static final Converter<String> UNQUOTED_STRING_CONVERTER 
		= new Converter<String>() {
	
		@Override
		public String asFieldValue(String value) {
			return value;
		}
	
		@Override
		public String fromFieldValue(String text) throws ParseException {
			return text.trim();
		}
	};
	
	/**
	 * A noop converter, except that text is trimmed and unquoted
	 * when converted to a value.
	 */
	public static final Converter<String> UNQUOTE_ONLY_CONVERTER 
		= new Converter<String>() {
	
		@Override
		public String asFieldValue(String value) {
			return value;
		}
	
		@Override
		public String fromFieldValue(String text) throws ParseException {
			return unquoteString(text.trim());
		}
	};
	
	/**
	 * A converter that quotes and unquoted strings as necessary.
	 */
	public static final Converter<String> STRING_CONVERTER 
		= new Converter<String>() {
	
		@Override
		public String asFieldValue(String value) {
			return quoteIfNecessary(value);
		}
	
		@Override
		public String fromFieldValue(String text) throws ParseException {
			return unquoteString(text.trim());
		}
	};
	
	public static final ListConverter<String> STRING_LIST_CONVERTER 
		= new ListConverter<String>(Converters.STRING_CONVERTER);

	/**
	 * An integer converter.
	 */
	public static final Converter<Long> INT_CONVERTER = new Converter<Long>() {

		@Override
		public String asFieldValue(Long value) {
			return value.toString();
		}

		@Override
		public Long fromFieldValue(String text) throws ParseException {
			try {
				return Long.parseLong(unquoteString(text));
			} catch (NumberFormatException e) {
				throw new ParseException(text, 0);
			}
		}
	};

	/**
	 * An integer list converter.
	 */
	public static final ListConverter<Long> INT_LIST_CONVERTER 
		= new ListConverter<Long>(INT_CONVERTER);

	/**
	 * A date/time converter.
	 */
	public static final Converter<Instant> DATE_TIME_CONVERTER 
		= new InstantConverter();


	/**
	 * A converter for cookies.
	 */
	public static final ListConverter<HttpCookie> COOKIE_CONVERTER 
		= new ListConverter<HttpCookie>(null) {

		@Override
		public List<HttpCookie> fromFieldValue(String text)
				throws ParseException {
			try {
				return HttpCookie.parse(text);
			} catch (IllegalArgumentException e) {
				throw new ParseException(text, 0);
			}
		}

		@Override
		public String asFieldValue(List<HttpCookie> value) {
			throw new UnsupportedOperationException();
		}
	};

	/**
	 * A converter for a list of cookies.
	 */
	public static final ListConverter<HttpCookie> COOKIE_LIST_CONVERTER 
		= new ListConverter<HttpCookie>(new Converter<HttpCookie>() {
	
		@Override
		public String asFieldValue(HttpCookie value) {
			return value.toString();
		}

		@Override
		public HttpCookie fromFieldValue(String text)
				throws ParseException {
			try {
				return HttpCookie.parse(text).get(0);
			} catch (IllegalArgumentException e) {
				throw new ParseException(text, 0);
			}
		}
		}, ";");

	/**
	 * A converter for a language or language range. 
	 * Language range "`*`" is converted to a Locale with an empty language.
	 */
	public static final Converter<Locale> LANGUAGE_CONVERTER 
		= new Converter<Locale>() {
		
		@Override
		public String asFieldValue(Locale value) {
			return value.getCountry().length() == 0
					? value.getLanguage()
					: (value.getLanguage() + "-" + value.getCountry());
		}
	
		@Override
		public Locale fromFieldValue(String text) throws ParseException {
			return Locale.forLanguageTag(text);
		}
	};
	
	/**
	 * A converter for the media "topLevelType/Subtype" pair.
	 */
	public static final Converter<MediaTypePair> MEDIA_TYPE_PAIR_CONVERTER
		= new MediaTypePairConverter();

	/**
	 * A converter for a media type pair with parameters.
	 */
	public static final Converter<MediaRange> MEDIA_RANGE_CONVERTER 
		= new MediaRangeConverter();

	/**
	 * A converter for a media type pair with parameters.
	 */
	public static final Converter<MediaType> MEDIA_TYPE_CONVERTER 
		= new MediaTypeConverter();

	private Converters() {
	}

	/**
	 * If the string contains a char with a backslash before it,
	 * remove the backslash.
	 * 
	 * @param value the value to unquote
	 * @return the unquoted value
	 * @throws ParseException if the input violates the field format
	 * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
	 */
	public static String unquote(String value) {
		StringBuilder result = new StringBuilder();
		boolean pendingBackslash = false;
		for(char ch: value.toCharArray()) {
			switch (ch) {
			case '\\':
				if (pendingBackslash) {
					result.append(ch);
				} else {
					pendingBackslash = true;
					continue;
				}
			break;
			
			default:
				result.append(ch);
				break;
			}
			pendingBackslash = false;
		}
		return result.toString();
	}

	/**
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @param value the value to unquote
	 * @return the unquoted value
	 * @throws ParseException if the input violates the field format
	 * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
	 */
	public static String unquoteString(String value) throws ParseException {
		if (value.length() == 0 || value.charAt(0) != '\"') {
			return value;
		}
		String unquoted = unquote(value);
		if (!unquoted.endsWith("\"")) {
			throw new ParseException(value, value.length() - 1);
		}
		return unquoted.substring(1, unquoted.length() - 1);
	}

	/**
	 * Returns the given string as double quoted string if necessary.
	 * 
	 * @param value the value to quote if necessary
	 * @return the result
	 * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
	 */
	public static String quoteIfNecessary(String value) {
		StringBuilder result = new StringBuilder();
		boolean needsQuoting = false;
		result.append('"');
		for (char ch: value.toCharArray()) {
			if (!needsQuoting && HttpConstants.TOKEN_CHARS.indexOf(ch) < 0) {
				needsQuoting = true;
			}
			switch(ch) {
			case '"':
				// fall through
			case '\\':
				result.append('\\');
				// fall through
			default:
				result.append(ch);
				break;
			}
		}
		result.append('\"');
		if (needsQuoting) {
			return result.toString();
		}
		return value;
	}
	
	/**
	 * Return a new string in which all characters from `toBeQuoted`
	 * are prefixed with a backslash. 
	 * 
	 * @param value the string
	 * @param toBeQuoted the characters to be quoted
	 * @return the result
	 * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
	 */
	public static String quote(String value, String toBeQuoted) {
		StringBuilder result = new StringBuilder();
		for (char ch: value.toCharArray()) {
			if (toBeQuoted.indexOf(ch) >= 0) {
				result.append('\\');
			}
			result.append(ch);
		}
		return result.toString();
	}
}
