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
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jdrupes.httpcodec.protocols.http.HttpConstants;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.types.CommentedValue.CommentedValueConverter;
import org.jdrupes.httpcodec.types.Directive.DirectiveConverter;
import org.jdrupes.httpcodec.types.Etag.EtagConverter;
import org.jdrupes.httpcodec.types.MediaBase.MediaTypePair;
import org.jdrupes.httpcodec.types.MediaBase.MediaTypePairConverter;
import org.jdrupes.httpcodec.types.MediaRange.MediaRangeConverter;
import org.jdrupes.httpcodec.types.MediaType.MediaTypeConverter;
import org.jdrupes.httpcodec.types.ParameterizedValue.ParamValueConverterBase;

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
	public static final Converter<String> UNQUOTED_STRING 
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
	public static final Converter<String> UNQUOTE_ONLY 
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
	public static final Converter<String> STRING 
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
	
	public static final Converter<StringList> STRING_LIST 
		= new ListConverter<>(StringList::new, STRING);

	/**
	 * A converter that quotes strings.
	 */
	public static final Converter<String> QUOTED_STRING 
		= new Converter<String>() {
	
		@Override
		public String asFieldValue(String value) {
			return quoteString(value);
		}
	
		@Override
		public String fromFieldValue(String text) throws ParseException {
			return unquoteString(text.trim());
		}
	};
	
	public static final Converter<StringList> QUOTED_STRING_LIST 
		= new ListConverter<>(StringList::new, QUOTED_STRING);

	/**
	 * An integer converter.
	 */
	public static final Converter<Long> LONG = new Converter<Long>() {

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
	public static final ListConverter<List<Long>, Long> LONG_LIST 
		= new ListConverter<>(ArrayList<Long>::new, LONG);

	/**
	 * A date/time converter.
	 */
	public static final Converter<Instant> DATE_TIME 
		= new InstantConverter();

	/**
	 * A converter for set cookies.
	 */
	public static final ListConverter<CookieList, HttpCookie> SET_COOKIE 
		= new ListConverter<CookieList, HttpCookie>(
				CookieList::new, new Converter<HttpCookie>() {
					
					@Override
					public String asFieldValue(HttpCookie value) {
						StringBuilder result = new StringBuilder();
						result.append(value.toString());
						if (value.getMaxAge() > 0) {
							result.append("; Max-Age=");
							result.append(Long.toString(value.getMaxAge()));
						}
						if (value.getDomain() != null) {
							result.append("; Domain=");
							result.append(value.getDomain());
						}
						if (value.getPath() != null) {
							result.append("; Path=");
							result.append(value.getPath());
						}
						if (value.getSecure()) {
							result.append("; Secure");
						}
						if (value.isHttpOnly()) {
							result.append("; HttpOnly");
						}
						return result.toString();
					}

					@Override
					public HttpCookie fromFieldValue(String text)
							throws ParseException {
						throw new UnsupportedOperationException();
					}
				}, ",", true) {

		@Override
		public CookieList fromFieldValue(String text)
				throws ParseException {
			try {
				return new CookieList(HttpCookie.parse(text));
			} catch (IllegalArgumentException e) {
				throw new ParseException(text, 0);
			}
		}

		@Override
		public String asFieldValue(CookieList value) {
			throw new UnsupportedOperationException();
		}
	};

	/**
	 * A converter for a list of cookies.
	 */
	public static final ListConverter<CookieList, HttpCookie> COOKIE_LIST 
		= new ListConverter<CookieList, HttpCookie>(CookieList::new,
				new Converter<HttpCookie>() {
	
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
	public static final Converter<Locale> LANGUAGE 
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
	 * A converter for a weighted list of languages.
	 */
	public static final ListConverter<List<ParameterizedValue<Locale>>,
		ParameterizedValue<Locale>> LANGUAGE_LIST 
			= new ListConverter<List<ParameterizedValue<Locale>>,
					ParameterizedValue<Locale>>(ArrayList::new,
							new ParamValueConverterBase
								<ParameterizedValue<Locale>, Locale>(
										LANGUAGE, ParameterizedValue<Locale>::new) {
					});

	/**
	 * A converter for a weighted list of strings.
	 */
	public static final ListConverter<List<ParameterizedValue<String>>,
		ParameterizedValue<String>> WEIGHTED_STRINGS 
			= new ListConverter<List<ParameterizedValue<String>>,
					ParameterizedValue<String>>(ArrayList::new,
							new ParamValueConverterBase
								<ParameterizedValue<String>, String>(
										STRING, ParameterizedValue<String>::new) {
					});

	/**
	 * A converter for the media "topLevelType/Subtype" pair.
	 */
	public static final Converter<MediaTypePair> MEDIA_TYPE_PAIR
		= new MediaTypePairConverter();

	/**
	 * A converter for a media type pair with parameters.
	 */
	public static final Converter<MediaRange> MEDIA_RANGE 
		= new MediaRangeConverter();

	/**
	 * A converter for a list of media ranges.
	 */
	public static final ListConverter<List<MediaRange>,
		MediaRange> MEDIA_RANGE_LIST 
			= new ListConverter<List<MediaRange>,
				MediaRange>(ArrayList::new, MEDIA_RANGE);

	/**
	 * A converter for a media type pair with parameters.
	 */
	public static final Converter<MediaType> MEDIA_TYPE 
		= new MediaTypeConverter();

	/**
	 * A converter for a directive.
	 */
	public static final DirectiveConverter DIRECTIVE
		= new DirectiveConverter();

	/**
	 * A converter for a list of directives.
	 */
	public static final ListConverter<List<Directive>, Directive>
		DIRECTIVE_LIST = new ListConverter<List<Directive>, Directive>(
				ArrayList::new, DIRECTIVE);
	
	/**
	 * A converter for a URI.
	 */
	public static final Converter<URI> URI_CONV 
		= new Converter<URI>() {
		
		@Override
		public String asFieldValue(URI value) {
			return value.toString();
		}
	
		@Override
		public URI fromFieldValue(String text) throws ParseException {
			try {
				return URI.create(text);
			} catch (IllegalArgumentException e) {
				throw new ParseException(e.getMessage(), 0);
			}
		}
	};

	/**
	 * A converter for product descriptions as used in the `User-Agent`
	 * and `Server` header fields.
	 */
	public static final ProductDescriptionConverter PRODUCT_DESCRIPTIONS 
		= new ProductDescriptionConverter(); 
		
	/**
	 * Used by the {@link EtagConverter} to unambiguously denote
	 * a decoded wildcard. If the result of `fromFieldValue` == 
	 * `WILDCARD`, the field value was an unquoted asterisk.
	 * If the result `equals("*")`, it may also have been
	 * a quoted asterisk.
	 */
	public static final String WILDCARD = "*";
	
	/**
	 * A converter for an ETag header.
	 */
	public static final EtagConverter ETAG = new EtagConverter();
	
	public static final Converter<List<Etag>> ETAG_LIST 
		= new ListConverter<>(ArrayList::new, ETAG);

	
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
	 * Returns the given string as double quoted string.
	 * 
	 * @param value the value to quote
	 * @return the result
	 */
	public static String quoteString(String value) {
		StringBuilder result = new StringBuilder();
		result.append('"');
		for (char ch: value.toCharArray()) {
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
		return result.toString();
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
	
	private static class ProductDescriptionConverter 
		extends ListConverter<List<CommentedValue<String>>, CommentedValue<String>> {
		
		public ProductDescriptionConverter() {
			super(ArrayList<CommentedValue<String>>::new, 
				new CommentedValueConverter<>(Converters.STRING));
		}

		/* (non-Javadoc)
		 * @see Converter#fromFieldValue(java.lang.String)
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
