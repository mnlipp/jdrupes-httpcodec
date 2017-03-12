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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jdrupes.httpcodec.protocols.http.HttpConstants;
import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * A base class for all kinds of header field values.
 * 
 * @see "[MessageHeaders](https://www.iana.org/assignments/message-headers/message-headers.xhtml)"
 */
public abstract class HttpField<T> implements Cloneable {

	// https://tools.ietf.org/html/rfc7231#section-5.3.2
	public static final String ACCEPT = "Accept";
	public static final String COOKIE = "Cookie";
	// https://tools.ietf.org/html/rfc7230#section-6.1
	public static final String CONNECTION = "Connection";
	// https://tools.ietf.org/html/rfc7230#section-3.3.2
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String DATE = "Date";
	public static final String ETAG = "ETag";
	// https://tools.ietf.org/html/rfc7230#section-5.4
	public static final String HOST = "Host";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String LAST_MODIFIED = "Last-Modified";
	public static final String SET_COOKIE = "Set-Cookie";
	// https://tools.ietf.org/html/rfc7230#section-4.3
	public static final String TE = "TE";
	// https://tools.ietf.org/html/rfc7230#section-4.4
	public static final String TRAILER = "Trailer";
	// https://tools.ietf.org/html/rfc7230#section-3.3.1
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	// https://tools.ietf.org/html/rfc7230#section-6.7
	public static final String UPGRADE = "Upgrade";
	// https://tools.ietf.org/html/rfc7230#section-5.7.1
	public static final String VIA = "Via";
	
	private static Map<String, String> fieldNameMap = new TreeMap<>(
	        String.CASE_INSENSITIVE_ORDER);
	
	static {
		fieldNameMap.put(ACCEPT, ACCEPT);
		fieldNameMap.put(COOKIE, COOKIE);
		fieldNameMap.put(CONNECTION, CONNECTION);
		fieldNameMap.put(CONTENT_LENGTH, CONTENT_LENGTH);
		fieldNameMap.put(CONTENT_TYPE, CONTENT_TYPE);
		fieldNameMap.put(DATE, DATE);
		fieldNameMap.put(ETAG, ETAG);
		fieldNameMap.put(HOST, HOST);
		fieldNameMap.put(IF_MATCH, IF_MATCH);
		fieldNameMap.put(IF_NONE_MATCH, IF_NONE_MATCH);
		fieldNameMap.put(IF_MODIFIED_SINCE, IF_MODIFIED_SINCE);
		fieldNameMap.put(IF_UNMODIFIED_SINCE, IF_UNMODIFIED_SINCE);
		fieldNameMap.put(LAST_MODIFIED, LAST_MODIFIED);
		fieldNameMap.put(SET_COOKIE, SET_COOKIE);
		fieldNameMap.put(TE, TE);
		fieldNameMap.put(TRAILER, TRAILER);
		fieldNameMap.put(TRANSFER_ENCODING, TRANSFER_ENCODING);
		fieldNameMap.put(UPGRADE, UPGRADE);
		fieldNameMap.put(VIA, VIA);
	}
	
	/**
	 * A noop converter, except that text is trimmed when converted to
	 * a value.
	 */
	public static final Converter<String> UNQUOTED_STRING_CONVERTER 
		= new Converter<String>() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see Converter#asFieldValue(java.lang.Object)
		 */
		@Override
		public String asFieldValue(String value) {
			return value;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see Converter#fromFieldValue(java.lang.String)
		 */
		@Override
		public String fromFieldValue(String text) throws ParseException {
			return text.trim();
		}
	};

	/**
	 * A converter that quotes and unquoted strings as necessary.
	 */
	public static final Converter<String> STRING_CONVERTER 
		= new Converter<String>() {

		/* (non-Javadoc)
		 * @see Converter#asFieldValue(java.lang.Object)
		 */
		@Override
		public String asFieldValue(String value) {
			return quoteIfNecessary(value);
		}

		/* (non-Javadoc)
		 * @see Converter#fromFieldValue(java.lang.String)
		 */
		@Override
		public String fromFieldValue(String text) throws ParseException {
			return unquote(text.trim());
		}
};


	private final String name;
	private T value;
	private Converter<T> converter;
	
	/**
	 * Creates a new representation of a header field. For fields with
	 * a constant definition in this class, the name is normalized.
	 * 
	 * @param name the field name
	 * @param converter the converter
	 */
	protected HttpField(String name, Converter<T> converter) {
		this.name = fieldNameMap.getOrDefault(name, name);
		this.converter = converter;
	}

	/**
	 * Creates a new representation of a header field with the 
	 * given value and converter. For fields with a 
	 * constant definition in this class, the name is normalized.
	 * 
	 * @param name the field name
	 * @param value the value
	 * @param converter the converter
	 */
	protected HttpField(String name, T value, Converter<T> converter) {
		this(name, converter);
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public HttpField<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			HttpField<T> clone =  (HttpField<T>)super.clone();
			clone.value = cloneValue();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new IllegalArgumentException(e); 
		}
	}

	/**
	 * Called by {@link #clone()} to clone the value. Should
	 * be overridden by derived classes if the value is not immutable.
	 * 
	 * @return return the value
	 */
	protected T cloneValue() {
		return value;
	}
	
	/**
	 * Returns an HttpField that represents the given header field, using the
	 * best matching derived class in this package. Works for all well known
	 * field names, i.e. the field names defined as constants in this class. If
	 * the field name is unknown, the result will be of type
	 * {@link HttpStringField}.
	 * 
	 * @param fieldName
	 *            the field name
	 * @param fieldValue
	 *            the field value
	 * @return a typed representation
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpField<?> fromString(String fieldName,
	        String fieldValue) throws ParseException {
		String normalizedFieldName = fieldNameMap
				.getOrDefault(fieldName, fieldName);
		switch (normalizedFieldName) {
//		case HttpField.ACCEPT:
//			return 
		case HttpField.COOKIE:
			return HttpCookieListField.fromString(fieldValue);
		case HttpField.CONNECTION:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.CONTENT_LENGTH:
			return HttpContentLengthField.fromString(fieldValue);
		case HttpField.CONTENT_TYPE:
			return HttpMediaTypeField.fromString(fieldName, fieldValue);
		case HttpField.DATE:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case HttpField.IF_MATCH:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.IF_MODIFIED_SINCE:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case HttpField.IF_NONE_MATCH:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.IF_UNMODIFIED_SINCE:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case HttpField.LAST_MODIFIED:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case HttpField.SET_COOKIE:
			return HttpSetCookieListField.fromString(fieldValue);
		case HttpField.TRAILER:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.TRANSFER_ENCODING:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.UPGRADE:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.VIA:
			return HttpStringListField.fromString(fieldName, fieldValue);
		default:
			return HttpStringField.fromString(fieldName, fieldValue);
		}
	}
	
	/**
	 * Returns the header field name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the header field's parsed value.
	 * 
	 * @return the field's value
	 */
	public T getValue() {
		return value;
	}
	
	/**
	 * Returns the string representation of this field's value.
	 * 
	 * @return the field value as string
	 */
	public String asFieldValue() {
		return converter.asFieldValue(value);
	}
	
	/**
	 * Returns the string representation of this header field as it appears in
	 * an HTTP message. Note that the returned string may span several
	 * lines (may contain CR/LF), but is has no trailing CR/LF.
	 * 
	 * @return the field as it occurs in a header
	 */
	public String asHeaderField() {
		return getName() + ": " + asFieldValue();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [");
		result.append(asHeaderField().replace("\r\n", " CRLF "));
		result.append("]");
		return result.toString();
	}

	/**
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @param value the value to unquote
	 * @return the unquoted value
	 * @throws ParseException if the input violates the field format
	 * @see [Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)
	 */
	public static String unquote(String value) throws ParseException {
		if (value.length() == 0 || value.charAt(0) != '\"') {
			return value;
		}
		int startPosition = 1;
		int position = 1;
		try {
			StringBuilder result = new StringBuilder();
			while (true) {
				char ch = value.charAt(position);
				switch (ch) {
				case '\\':
					result.append(value.substring(startPosition, position));
					position += 1;
					result.append(value.charAt(position));
					position += 1;
					startPosition = position;
					continue;
				case '\"':
					if (position != value.length() - 1) {
						throw new ParseException(value, position);
					}
					result.append(value.substring(startPosition, position));
					return result.toString();
				default:
					position += 1;
					continue;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(value, position);
		}
	}

	/**
	 * Returns the given string as double quoted string if necessary.
	 * 
	 * @param value the value to quote if necessary
	 * @return the result
	 * @see [Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)
	 */
	public static String quoteIfNecessary(String value) {
		StringBuilder result = new StringBuilder();
		int position = 0;
		boolean needsQuoting = false;
		result.append('"');
		while (position < value.length()) {
			char ch = value.charAt(position++);
			if (!needsQuoting && HttpConstants.TOKEN_CHARS.indexOf(ch) < 0) {
				needsQuoting = true;
			}
			switch(ch) {
			case '"':
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
	 * Implemented by classes that convert between a value and its 
	 * string representation in the HTTP header field.
	 */
	public interface Converter<T> {

		/**
		 * Returns the representation of this value in a header field.
		 * 
		 * @return the representation
		 */
		String asFieldValue(T value);
		
		/**
		 * Parses the given text and returns the parsed value.
		 * 
		 * @param text the value from the header field
		 * @return the parsed value
		 * @throws ParseException if the value cannot be parsed
		 */
		T fromFieldValue(String text) throws ParseException;
	}
	
	/**
	 * Represents a parameterized value
	 * such as `value; param1=value1; param2=value2`.
	 * 
	 * @param <T> the type of the value
	 */
	public static class ParameterizedValue<T> {

		private T value;
		private Map<String, String> params;
		
		/**
		 * Creates a new object with the given value and parameters. 
		 * 
		 * @param value the value
		 * @param parameters the parameters
		 */
		public ParameterizedValue(T value, Map<String,String> parameters) {
			this.value = value;
			this.params = parameters;
		}

		/**
		 * Returns the value.
		 * 
		 * @return the value
		 */
		public T getValue() {
			return value;
		}

		/**
		 * Returns the parameters.
		 * 
		 * @return the parameters as unmodifiable map 
		 */
		public Map<String, String> getParameters() {
			return Collections.unmodifiableMap(params);
		}
	}

	/**
	 * A converter for values with parameter. Converts field values
	 * such as `value; param1=value1; param2=value2`.
	 * 
	 * @param <T> the type of the value
	 */
	public static class ParameterizedValueConverter<T>
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
			this(valueConverter, false);
		}

		/**
		 * Creates a new converter by extending the given value converter
		 * with functionality for handling the parameters.
		 * 
		 * @param valueConverter the converter for a value (without parameters)
		 * @param quoteIfNecessary if set to `true` applies
		 * {@link #quoteIfNecessary} and {@link #unquote} to parameter values
		 */
		public ParameterizedValueConverter(
				Converter<T> valueConverter, boolean quoteIfNecessary) {
			this.valueConverter = valueConverter;
			paramValueConverter = quoteIfNecessary
					? STRING_CONVERTER : UNQUOTED_STRING_CONVERTER;
		}

		@Override
		public String asFieldValue(ParameterizedValue<T> value) {
			StringBuilder result = new StringBuilder();
			result.append(valueConverter.asFieldValue(value.getValue()));
			for (Entry<String, String> e: value.getParameters().entrySet()) {
				result.append("; ");
				result.append(e.getKey());
				result.append('=');
				paramValueConverter.asFieldValue(e.getValue());
			}
			return null;
		}

		@Override
		public ParameterizedValue<T> fromFieldValue(String text)
				throws ParseException {
			ListItemizer li = new ListItemizer(text, ";");
			String valueRepr = li.nextItem();
			if (valueRepr == null) {
				throw new ParseException("Value may not be empty", 0);
			}
			T value = valueConverter.fromFieldValue(valueRepr);
			Map<String,String> params = new HashMap<>();
			while (true) {
				String param = li.nextItem().trim();
				if (param == null) {
					break;
				}
				ListItemizer pi = new ListItemizer(param, "=");
				String paramKey = pi.nextItem().trim();
				if (paramKey == null) {
					throw new ParseException("parameter may not be empty", 0);
				}
				String paramValue = pi.nextItem();
				if (paramValue != null) {
					paramValueConverter.fromFieldValue(paramValue);
				}
				params.put(paramKey, paramValue);
			}
			return new ParameterizedValue<>(value, params);
		}
	}
}
