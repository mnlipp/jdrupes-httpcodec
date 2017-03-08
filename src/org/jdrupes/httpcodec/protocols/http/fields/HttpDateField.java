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

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * An HTTP Date field as specified by 
 * [RFC 7231, Section 7.1.1.1](https://tools.ietf.org/html/rfc7231#section-7.1.1.1)
 */
public class HttpDateField extends HttpField<Instant> {

	// "Sunday, 06-Nov-94 08:49:37 GMT"
	private static final DateTimeFormatter RFC_850_DATE_TIME
		= new DateTimeFormatterBuilder().appendPattern("EEEE, dd-MMM-")
		.appendValueReduced(ChronoField.YEAR, 2, 2, Year.now().getValue() - 50)
		.appendPattern(" HH:mm:ss zz")
		.toFormatter().withZone(ZoneId.of("GMT"))
		.withLocale(Locale.US);
	// "Sun Nov  6 08:49:37 1994"
	private static final DateTimeFormatter ANSI_DATE_TIME
		= DateTimeFormatter.ofPattern("EEE MMM ppd HH:mm:ss yyyy", Locale.US)
		.withZone(ZoneId.of("GMT"));
	
	private Instant value;

	/**
	 * Creates a header field object with the given value.
	 * 
	 * @param name the field name
	 * @param value
	 *            the field value
	 */
	public HttpDateField(String name, Instant value) {
		super(name);
		this.value = value;
	}

	/**
	 * Creates a header field object with name "Date" and the current Time.
	 */
	public HttpDateField() {
		this(HttpField.DATE, Instant.now());
	}

	protected static <T extends HttpDateField> T fromString(Class<T> type,
	        String name, String text) throws ParseException {
		try {
			T result = type.getConstructor(String.class, Instant.class)
			        .newInstance(name, Instant.EPOCH);
			try {
				((HttpDateField) result).value = Instant
				        .from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
			} catch (DateTimeParseException e) {
				try {
					((HttpDateField) result).value = Instant
					        .from(RFC_850_DATE_TIME.parse(text));
				} catch (DateTimeParseException e1) {
					try {
						((HttpDateField) result).value = Instant
						        .from(ANSI_DATE_TIME.parse(text));
					} catch (DateTimeParseException e2) {
						throw new ParseException(text, 0);
					}
				}
			}
			return result;
		} catch (InstantiationException | IllegalAccessException
		        | IllegalArgumentException | InvocationTargetException
		        | NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Creates a new object with a value obtained by parsing the given String.
	 * 
	 * @param name
	 *            the field name
	 * @param text
	 *            the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpDateField fromString(String name, String text)
	        throws ParseException {
		return fromString(HttpDateField.class, name, text);
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	@Override
	public Instant getValue() {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asFieldValue() {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(
				value.atZone(ZoneId.of("GMT")));	
	}

}
