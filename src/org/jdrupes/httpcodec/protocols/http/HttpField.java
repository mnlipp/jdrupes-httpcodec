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

package org.jdrupes.httpcodec.protocols.http;

import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;

import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MultiValueConverter;

/**
 * A base class for all kinds of header field values.
 * 
 * @param <T> the type of the header field's value 
 * 
 * @see "[MessageHeaders](https://www.iana.org/assignments/message-headers/message-headers.xhtml)"
 */
public class HttpField<T> {

	// RFC 7230 3.2, 3.2.4
	protected static final Pattern headerLinePatter = Pattern
	        .compile("^(" + TOKEN_REGEXP + "):(.*)$");

	/** @see "[RFC 7231, 5.3.2](https://tools.ietf.org/html/rfc7231#section-5.3.2)" */
	public static final String ACCEPT = "Accept";
	/** @see "[RFC 7231, 5.3.3](https://tools.ietf.org/html/rfc7231#section-5.3.3)" */
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	/** @see "[RFC 7231, 5.3.4](https://tools.ietf.org/html/rfc7231#section-5.3.4)" */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	/** @see "[RFC 7231, 5.3.5](https://tools.ietf.org/html/rfc7231#section-5.3.5)" */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	/** @see "[RFC 7234, 5.1](https://tools.ietf.org/html/rfc7234#section-5.1)" */
	public static final String AGE = "Age";
	/** @see "[RFC 7231, 7.4.1](https://tools.ietf.org/html/rfc7231#section-7.4.1)" */
	public static final String ALLOW = "Allow";
	/** @see "[RFC 7235, 4.2](https://tools.ietf.org/html/rfc7235#section-4.2)" */
	public static final String AUTHORIZATION = "Authorization";
	/** @see "[RFC 7234, 5.2](https://tools.ietf.org/html/rfc7234#section-5.2)" */
	public static final String CACHE_CONTROL = "Cache-Control";
	/** @see "[RFC 6265, 5.4](https://tools.ietf.org/html/rfc6265#section-5.4)" */
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
	/** @see "[RFC 7232, 2.3](https://tools.ietf.org/html/rfc7232#section-2.3)" */
	public static final String ETAG = "ETag";
	/** @see "[RFC 7231, 5.1.1](https://tools.ietf.org/html/rfc7231#section-5.1.1)" */
	public static final String EXPECT = "Expect";
	/** @see "[RFC 7234, 5.3](https://tools.ietf.org/html/rfc7234#section-5.3)" */
	public static final String EXPIRES = "Expires";
	/** @see "[RFC 7231, 5.5.1](https://tools.ietf.org/html/rfc7231#section-5.5.1)" */
	public static final String FROM = "From";
	/** @see "[RFC 7230, 5.4](https://tools.ietf.org/html/rfc7230#section-5.4)" */
	public static final String HOST = "Host";
	/** @see "[RFC 7232, 3.1](https://tools.ietf.org/html/rfc7232#section-3.1)" */
	public static final String IF_MATCH = "If-Match";
	/** @see "[RFC 7232, 3.2](https://tools.ietf.org/html/rfc7232#section-3.2)" */
	public static final String IF_NONE_MATCH = "If-None-Match";
	/** @see "[RFC 7232, 3.3](https://tools.ietf.org/html/rfc7232#section-3.3)" */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	/** @see "[RFC 7232, 3.4](https://tools.ietf.org/html/rfc7232#section-3.4)" */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	/** @see "[RFC 7232, 2.2](https://tools.ietf.org/html/rfc7232#section-2.2)" */
	public static final String LAST_MODIFIED = "Last-Modified";
	/** @see "[RFC 7231, 7.1.2](https://tools.ietf.org/html/rfc7231#section-7.1.2)" */
	public static final String LOCATION = "Location";
	/** @see "[RFC 7231, 5.1.2](https://tools.ietf.org/html/rfc7231#section-5.1.2)" */
	public static final String MAX_FORWARDS = "Max-Forwards";
	/** @see "[RFC 7234, 5.4](https://tools.ietf.org/html/rfc7234#section-5.4)" */
	public static final String PRAGMA = "Pragma";
	/** @see "[RFC 7235, 4.3](https://tools.ietf.org/html/rfc7235#section-4.3)" */
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
	/** @see "[RFC 7235, 4.4](https://tools.ietf.org/html/rfc7235#section-4.4)" */
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	/** @see "[RFC 7231, 7.1.3](https://tools.ietf.org/html/rfc7231#section-7.1.3)" */
	public static final String RETRY_AFTER = "Retry-After";
	/** @see "[RFC 7231, 7.4.2](https://tools.ietf.org/html/rfc7231#section-7.4.2)" */
	public static final String SERVER = "Server";
	/** @see "[RFC 6265, 4.1.1](https://tools.ietf.org/html/rfc6265#section-4.1.1)" */
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
	/** @see "[RFC 7231, 7.1.4](https://tools.ietf.org/html/rfc7231#section-7.1.4)" */
	public static final String VARY = "Vary";
	/** @see "[RFC 7230, 5.7.1](https://tools.ietf.org/html/rfc7230#section-5.7.1)" */
	public static final String VIA = "Via";
	/** @see "[RFC 7234, 5.5](https://tools.ietf.org/html/rfc7234#section-5.5)" */
	public static final String WARNING = "Warning";
	/** @see "[RFC 7235, 4.1](https://tools.ietf.org/html/rfc7235#section-4.1)" */
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	
	private static Map<String, String> fieldNameMap = new TreeMap<>(
	        String.CASE_INSENSITIVE_ORDER);
	
	static {
		fieldNameMap.put(ACCEPT, ACCEPT);
		fieldNameMap.put(ACCEPT_CHARSET, ACCEPT_CHARSET);
		fieldNameMap.put(ACCEPT_ENCODING, ACCEPT_ENCODING);
		fieldNameMap.put(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
		fieldNameMap.put(AGE, AGE);
		fieldNameMap.put(ALLOW, ALLOW);
		fieldNameMap.put(AUTHORIZATION, AUTHORIZATION);
		fieldNameMap.put(CACHE_CONTROL, CACHE_CONTROL);
		fieldNameMap.put(COOKIE, COOKIE);
		fieldNameMap.put(CONNECTION, CONNECTION);
		fieldNameMap.put(CONTENT_LENGTH, CONTENT_LENGTH);
		fieldNameMap.put(CONTENT_LOCATION, CONTENT_LOCATION);
		fieldNameMap.put(CONTENT_TYPE, CONTENT_TYPE);
		fieldNameMap.put(DATE, DATE);
		fieldNameMap.put(ETAG, ETAG);
		fieldNameMap.put(EXPECT, EXPECT);
		fieldNameMap.put(EXPIRES, EXPIRES);
		fieldNameMap.put(FROM, FROM);
		fieldNameMap.put(HOST, HOST);
		fieldNameMap.put(IF_MATCH, IF_MATCH);
		fieldNameMap.put(IF_NONE_MATCH, IF_NONE_MATCH);
		fieldNameMap.put(IF_MODIFIED_SINCE, IF_MODIFIED_SINCE);
		fieldNameMap.put(IF_UNMODIFIED_SINCE, IF_UNMODIFIED_SINCE);
		fieldNameMap.put(LAST_MODIFIED, LAST_MODIFIED);
		fieldNameMap.put(LOCATION, LOCATION);
		fieldNameMap.put(MAX_FORWARDS, MAX_FORWARDS);
		fieldNameMap.put(PRAGMA, PRAGMA);
		fieldNameMap.put(PROXY_AUTHENTICATE, PROXY_AUTHENTICATE);
		fieldNameMap.put(PROXY_AUTHORIZATION, PROXY_AUTHORIZATION);
		fieldNameMap.put(RETRY_AFTER, RETRY_AFTER);
		fieldNameMap.put(SERVER, SERVER);
		fieldNameMap.put(SET_COOKIE, SET_COOKIE);
		fieldNameMap.put(TE, TE);
		fieldNameMap.put(TRAILER, TRAILER);
		fieldNameMap.put(TRANSFER_ENCODING, TRANSFER_ENCODING);
		fieldNameMap.put(UPGRADE, UPGRADE);
		fieldNameMap.put(USER_AGENT, USER_AGENT);
		fieldNameMap.put(VARY, VARY);
		fieldNameMap.put(VIA, VIA);
		fieldNameMap.put(WARNING, WARNING);
		fieldNameMap.put(WWW_AUTHENTICATE, WWW_AUTHENTICATE);
	}
	
	private final String name;
	private T value;
	private Converter<T> converter;
	
	/**
	 * Creates a new representation of a header field with the 
	 * given value and converter. For fields with a 
	 * constant definition in this class, the name is normalized.
	 * 
	 * @param name the field name
	 * @param value the value
	 * @param converter the converter
	 */
	public HttpField(String name, T value, Converter<T> converter) {
		this.name = fieldNameMap.getOrDefault(name, name);
		this.converter = converter;
		this.value = value;
	}

	/**
	 * Creates a new representation of a header field from its textual
	 * representation. For fields with a 
	 * constant definition in this class, the name is normalized.
	 * 
	 * @param headerLine the header line
	 * @param converter the converter
	 * @throws ParseException if an error occurs while parsing the header line
	 */
	public HttpField(String headerLine, Converter<T> converter) 
			throws ParseException {
		this.converter = converter;
		Matcher hlp = headerLinePatter.matcher(headerLine);
		if (!hlp.matches()) {
			throw new ParseException("Invalid header: ", 0);
		}
		this.name = fieldNameMap.getOrDefault(hlp.group(1), hlp.group(1));
		// RFC 7230 3.2.4
		this.value = converter.fromFieldValue(hlp.group(2).trim());
	}

	/**
	 * Returns the proper converter for the header field with the given
	 * name. Works for all well known
	 * field names, i.e. the field names defined as constants in this class.
	 * If the field name is unknown, a string converter is returned.
	 * 
	 * @param fieldName
	 *            the field name
	 * @return the converter
	 */
	public static Converter<?> lookupConverter(String fieldName) {
		String normalizedFieldName = fieldNameMap
				.getOrDefault(fieldName, fieldName);
		switch (normalizedFieldName) {
		case ACCEPT:
			return Converters.MEDIA_RANGE;
		case ACCEPT_LANGUAGE:
			return Converters.LANGUAGE;
		case AGE:
			return Converters.LONG;
		case ALLOW:
			return Converters.STRING_LIST;
		case AUTHORIZATION:
			return Converters.CREDENTIALS;
		case CACHE_CONTROL:
			return Converters.CACHE_CONTROL_LIST;
		case COOKIE:
			return Converters.COOKIE_LIST;
		case CONNECTION:
			return Converters.STRING_LIST;
		case CONTENT_LENGTH:
			return Converters.LONG;
		case CONTENT_LOCATION:
			return Converters.URI_CONV; 
		case CONTENT_TYPE:
			return Converters.MEDIA_TYPE;
		case DATE:
			return Converters.DATE_TIME;
		case EXPIRES:
			return Converters.DATE_TIME;
		case IF_MATCH:
			return Converters.ETAG_LIST;
		case IF_MODIFIED_SINCE:
			return Converters.DATE_TIME;
		case IF_NONE_MATCH:
			return Converters.ETAG_LIST;
		case IF_UNMODIFIED_SINCE:
			return Converters.DATE_TIME;
		case LAST_MODIFIED:
			return Converters.DATE_TIME;
		case LOCATION:
			return Converters.URI_CONV; 
		case MAX_FORWARDS:
			return Converters.LONG; 
		case PRAGMA:
			return Converters.DIRECTIVE_LIST;
		case PROXY_AUTHENTICATE:
			return Converters.CHALLENGE_LIST;
		case PROXY_AUTHORIZATION:
			return Converters.CREDENTIALS;
		case RETRY_AFTER:
			return Converters.DATE_TIME;
		case SERVER:
			return Converters.PRODUCT_DESCRIPTIONS;
		case SET_COOKIE:
			return Converters.SET_COOKIE;
		case TRAILER:
			return Converters.STRING_LIST;
		case TRANSFER_ENCODING:
			return Converters.STRING_LIST;
		case UPGRADE:
			return Converters.STRING_LIST;
		case USER_AGENT:
			return Converters.PRODUCT_DESCRIPTIONS;
		case VIA:
			return Converters.STRING_LIST;
		case WARNING:
			return Converters.STRING;
		case WWW_AUTHENTICATE:
			return Converters.CHALLENGE_LIST;
		default:
			return Converters.STRING;
		}
	}
	
	/**
	 * Returns the header field name.
	 * 
	 * @return the name
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the header field's parsed value.
	 * 
	 * @return the field's value
	 */
	public T value() {
		return value;
	}
	
	/**
	 * Returns the cconverter used by this field.
	 * 
	 * @return the converter
	 */
	public Converter<T> converter() {
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
		if (!(converter instanceof MultiValueConverter)
				|| !((MultiValueConverter<?,?>)converter).separateValues()) {
			return name() + ": " + asFieldValue();
		}
		// Convert list of items to seperate fields
		@SuppressWarnings("unchecked")
		MultiValueConverter<Iterable<Object>, Object> seqConverter
			= (MultiValueConverter<Iterable<Object>, Object>)converter;
		Converter<Object> itemConverter	= seqConverter.valueConverter();
		@SuppressWarnings("unchecked")
		Iterable<Object> source = (Iterable<Object>)value();
		return StreamSupport.stream(source.spliterator(), false).map(
				item -> name() + ": " + itemConverter.asFieldValue(item))
				.collect(Collectors.joining("\r\n"));
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
