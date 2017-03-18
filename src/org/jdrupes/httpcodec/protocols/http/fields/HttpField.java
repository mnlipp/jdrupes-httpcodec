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
import java.util.Map;
import java.util.TreeMap;

import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.Converters;

/**
 * A base class for all kinds of header field values.
 * 
 * @param <T> the type of the header field's value 
 * 
 * @see "[MessageHeaders](https://www.iana.org/assignments/message-headers/message-headers.xhtml)"
 */
public abstract class HttpField<T> {

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
		case HttpField.ACCEPT:
			return HttpWeightedListField.fromString(
					fieldName, fieldValue, Converters.MEDIA_RANGE_CONVERTER);
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
	 * Sets the header field's value.
	 * 
	 * @param value the new value
	 * @return the field for easy chaining
	 */
	public HttpField<T> setValue(T value) {
		this.value = value;
		return this;
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
}
