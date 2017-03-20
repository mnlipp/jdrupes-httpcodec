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

import org.jdrupes.httpcodec.protocols.http.HttpConstants;
import org.jdrupes.httpcodec.types.Converter;

/**
 * A base class for all kinds of header field values.
 * 
 * @param <T> the type of the header field's value 
 * 
 * @see "[MessageHeaders](https://www.iana.org/assignments/message-headers/message-headers.xhtml)"
 */
public abstract class HttpField<T> {

	/** @see "[RFC 7231, 5.3.2](https://tools.ietf.org/html/rfc7231#section-5.3.2)" */
	public static final String ACCEPT = "Accept";
	/** @see "[RFC 7231, 5.3.3](https://tools.ietf.org/html/rfc7231#section-5.3.3)" */
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	/** @see "[RFC 7231, 5.3.4](https://tools.ietf.org/html/rfc7231#section-5.3.4)" */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	/** @see "[RFC 7231, 5.3.5](https://tools.ietf.org/html/rfc7231#section-5.3.5)" */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	/** @see "[RFC 7231, 7.4.1](https://tools.ietf.org/html/rfc7231#section-7.4.1)" */
	public static final String ALLOW = "Allow";
	public static final String COOKIE = "Cookie";
	/** @see "[RFC 7230, 6.1](https://tools.ietf.org/html/rfc7230#section-6.1)" */
	public static final String CONNECTION = "Connection";
	/** @see "[RFC 7230, 3.3.2](https://tools.ietf.org/html/rfc7230#section-3.3.2)" */
	public static final String CONTENT_LENGTH = "Content-Length";
	/** @see "[RFC 7231, 3.1.4.2](https://tools.ietf.org/html/rfc7231#section-3.1.4.2)" */
	public static final String CONTENT_LOCATION = "Content-Location";
	/** @see "[RFC 7231, 3.1.1.5](https://tools.ietf.org/html/rfc7231#section-3.1.1.5)" */
	public static final String CONTENT_TYPE = "Content-Type";
	/** @see "[RFC 7231, 7.1.1.2](https://tools.ietf.org/html/rfc7231#section-7.1.1.2)" */
	public static final String DATE = "Date";
	public static final String ETAG = "ETag";
	/** @see "[RFC 7231, 5.1.1](https://tools.ietf.org/html/rfc7231#section-5.1.1)" */
	public static final String EXPECT = "Expect";
	/** @see "[RFC 7231, 5.5.1](https://tools.ietf.org/html/rfc7231#section-5.5.1)" */
	public static final String FROM = "From";
	/** @see "[RFC 7230, 5.4](https://tools.ietf.org/html/rfc7230#section-5.4)" */
	public static final String HOST = "Host";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String LAST_MODIFIED = "Last-Modified";
	/** @see "[RFC 7231, 7.1.2](https://tools.ietf.org/html/rfc7231#section-7.1.2)" */
	public static final String LOCATION = "Location";
	/** @see "[RFC 7231, 5.1.2](https://tools.ietf.org/html/rfc7231#section-5.1.2)" */
	public static final String MAX_FORWARDS = "Max-Forwards";
	/** @see "[RFC 7231, 7.1.3](https://tools.ietf.org/html/rfc7231#section-7.1.3)" */
	public static final String RETRY_AFTER = "Retry-After";
	/** @see "[RFC 7231, 7.4.2](https://tools.ietf.org/html/rfc7231#section-7.4.2)" */
	public static final String SERVER = "Server";
	public static final String SET_COOKIE = "Set-Cookie";
	/** @see "[RFC 7230, 4.3](https://tools.ietf.org/html/rfc7230#section-4.3)" */
	public static final String TE = "TE";
	/** @see "[RFC 7230, 4.4](https://tools.ietf.org/html/rfc7230#section-4.4)" */
	public static final String TRAILER = "Trailer";
	/** @see "[RFC 7230, 3.3.1](https://tools.ietf.org/html/rfc7230#section-3.3.1)" */
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	/** @see "[RFC 7230, 6.7](https://tools.ietf.org/html/rfc7230#section-6.7)" */
	public static final String UPGRADE = "Upgrade";
	/** @see "[RFC 7231, 5.5.3](https://tools.ietf.org/html/rfc7231#section-5.5.3)" */
	public static final String USER_AGENT = "User-Agent";
	/** @see "[RFC 7230,5.7.1](https://tools.ietf.org/html/rfc7230#section-5.7.1)" */
	public static final String VIA = "Via";
	
	private static Map<String, String> fieldNameMap = new TreeMap<>(
	        String.CASE_INSENSITIVE_ORDER);
	
	static {
		fieldNameMap.put(ACCEPT, ACCEPT);
		fieldNameMap.put(ACCEPT_CHARSET, ACCEPT_CHARSET);
		fieldNameMap.put(ACCEPT_ENCODING, ACCEPT_ENCODING);
		fieldNameMap.put(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
		fieldNameMap.put(ALLOW, ALLOW);
		fieldNameMap.put(COOKIE, COOKIE);
		fieldNameMap.put(CONNECTION, CONNECTION);
		fieldNameMap.put(CONTENT_LENGTH, CONTENT_LENGTH);
		fieldNameMap.put(CONTENT_LOCATION, CONTENT_LOCATION);
		fieldNameMap.put(CONTENT_TYPE, CONTENT_TYPE);
		fieldNameMap.put(DATE, DATE);
		fieldNameMap.put(ETAG, ETAG);
		fieldNameMap.put(EXPECT, EXPECT);
		fieldNameMap.put(FROM, FROM);
		fieldNameMap.put(HOST, HOST);
		fieldNameMap.put(IF_MATCH, IF_MATCH);
		fieldNameMap.put(IF_NONE_MATCH, IF_NONE_MATCH);
		fieldNameMap.put(IF_MODIFIED_SINCE, IF_MODIFIED_SINCE);
		fieldNameMap.put(IF_UNMODIFIED_SINCE, IF_UNMODIFIED_SINCE);
		fieldNameMap.put(LAST_MODIFIED, LAST_MODIFIED);
		fieldNameMap.put(LOCATION, LOCATION);
		fieldNameMap.put(MAX_FORWARDS, MAX_FORWARDS);
		fieldNameMap.put(RETRY_AFTER, RETRY_AFTER);
		fieldNameMap.put(SERVER, SERVER);
		fieldNameMap.put(SET_COOKIE, SET_COOKIE);
		fieldNameMap.put(TE, TE);
		fieldNameMap.put(TRAILER, TRAILER);
		fieldNameMap.put(TRANSFER_ENCODING, TRANSFER_ENCODING);
		fieldNameMap.put(UPGRADE, UPGRADE);
		fieldNameMap.put(USER_AGENT, USER_AGENT);
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
		case ACCEPT:
			return HttpWeightedMediaRangeListField.fromString(fieldName, fieldValue);
		case ACCEPT_CHARSET:
			return HttpWeightedStringListField.fromString(fieldName, fieldValue);
		case ACCEPT_ENCODING:
			return HttpWeightedStringListField.fromString(fieldName, fieldValue);
		case ACCEPT_LANGUAGE:
			return HttpWeightedLanguageListField.fromString(fieldName, fieldValue);
		case ALLOW:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case COOKIE:
			return HttpCookieListField.fromString(fieldValue);
		case CONNECTION:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case CONTENT_LENGTH:
			return HttpContentLengthField.fromString(fieldValue);
		case CONTENT_LOCATION:
			return HttpUriField.fromString(fieldName, fieldValue); 
		case CONTENT_TYPE:
			return HttpMediaTypeField.fromString(fieldName, fieldValue);
		case DATE:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case IF_MATCH:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case IF_MODIFIED_SINCE:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case IF_NONE_MATCH:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case IF_UNMODIFIED_SINCE:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case LAST_MODIFIED:
			return HttpDateTimeField.fromString(fieldName, fieldValue);
		case LOCATION:
			return HttpUriField.fromString(fieldName, fieldValue); 
		case MAX_FORWARDS:
			return HttpIntField.fromString(fieldName, fieldValue); 
		case RETRY_AFTER:
			return dateOrSpanField(fieldName, fieldValue);
		case SERVER:
			return HttpProductsDescriptionField
					.fromString(fieldName, fieldValue);
		case SET_COOKIE:
			return HttpSetCookieListField.fromString(fieldValue);
		case TRAILER:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case TRANSFER_ENCODING:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case UPGRADE:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case USER_AGENT:
			return HttpProductsDescriptionField
					.fromString(fieldName, fieldValue);
		case VIA:
			return HttpStringListField.fromString(fieldName, fieldValue);
		default:
			return HttpStringField.fromString(fieldName, fieldValue);
		}
	}
	
	private static HttpField<?> dateOrSpanField(String name, String value) 
			throws ParseException {
		if (Character.isDigit(value.charAt(0))) {
			return HttpIntField.fromString(name, value);
		}
		return HttpDateTimeField.fromString(name, value);
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
	 * Returns the cconverter used by this field.
	 * 
	 * @return the converter
	 */
	public Converter<T> getConverter() {
		return converter;
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

	/**
	 * Determines the length of a token.
	 * 
	 * @param text the text to parse
	 * @param startPos the start position
	 * @return the length of the token
	 */
	public static int tokenLength(String text, int startPos) {
		int pos = startPos;
		while (pos < text.length()
				&& HttpConstants.TOKEN_CHARS.indexOf(text.charAt(pos)) >= 0) {
			pos += 1;
		}
		return pos - startPos;
	}

	/**
	 * Determines the length of a white space sequence. 
	 * 
	 * @param text the test to parse 
	 * @param startPos the start position
	 * @return the length of the white space sequence
	 */
	public static int whiteSpaceLength(String text, int startPos) {
		int pos = startPos;
		while (pos < text.length()) {
			switch (text.charAt(pos)) {
			case ' ':
				// fall through
			case '\t':
				pos += 1;
				continue;
				
			default:
				break;
			}
			break;
		}
		return pos - startPos;
	}
	
	/**
	 * Determines the length of a comment.
	 * 
	 * @param text the text to parse
	 * @param startPos the staring position (must be the position of the
	 * opening brace)
	 * @return the length of the comment
	 */
	public static int commentLength(String text, int startPos) {
		int pos = startPos + 1;
		while (pos < text.length()) {
			switch(text.charAt(pos)) {
			case ')':
				return pos - startPos + 1;
				
			case '(':
				pos += commentLength(text, pos);
				break;
				
			case '\\':
				pos = Math.min(pos + 2, text.length());
				break;
				
			default:
				pos += 1;
				break;
			}
		}
		return pos - startPos;
	}
}
