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
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * A converter that converts between date/time representatiosn and
 * {@link Instant}.
 */
public class InstantConverter implements Converter<Instant> {

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

	@Override
	public String asFieldValue(Instant value) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(
				value.atZone(ZoneId.of("GMT")));	
	}

	@Override
	public Instant fromFieldValue(String text) throws ParseException {
		try {
			return Instant.from(
					DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
		} catch (DateTimeParseException e) {
			try {
				return Instant.from(RFC_850_DATE_TIME.parse(text));
			} catch (DateTimeParseException e1) {
				try {
					return Instant.from(ANSI_DATE_TIME.parse(text));
				} catch (DateTimeParseException e2) {
					throw new ParseException(text, 0);
				}
			}
		}
	}
}