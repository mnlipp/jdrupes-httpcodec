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

package org.jdrupes.httpcodec.test.fields;

import java.text.ParseException;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Locale;

import org.jdrupes.httpcodec.protocols.http.fields.HttpDateTimeField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpWeightedListField;
import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.MediaRange;
import org.jdrupes.httpcodec.types.ParameterizedValue;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Field parsing tests.
 */
public class FieldParsingTests {

	@Test
	public void testString() throws ParseException {
		HttpField<?> fv = HttpStringField.fromString("Test", "Hello");
		assertEquals("Hello", fv.getValue());
	}

	@Test
	public void testStringList() throws ParseException {
		HttpStringListField fv = HttpStringListField.fromString("Test",
		        "How, are,you,  out, there");
		assertEquals("How", fv.get(0));
		assertEquals("are", fv.get(1));
		assertEquals("you", fv.get(2));
		assertEquals("out", fv.get(3));
		assertEquals("there", fv.get(4));
		assertEquals(5, fv.size());
	}

	@Test
	public void testQuoted() throws ParseException {
		HttpStringListField fv = HttpStringListField.fromString("Test",
				"\"How \\\"are\",you,  \"out, there\"");
		assertEquals("How \"are", fv.get(0));
		assertEquals("you", fv.get(1));
		assertEquals("out, there", fv.get(2));
		assertEquals(3, fv.size());
	}

	@Test
	public void testUnquote() throws ParseException {
		HttpField<?> fv = HttpStringField.fromString("Test", "How are you?");
		assertEquals("How are you?", fv.getValue());
		fv = HttpStringField.fromString("Test", "\"How \\\"are\"");
		assertEquals("How \"are", fv.getValue());
	}
	
	@Test
	public void testMediaType() throws ParseException {
		HttpMediaTypeField mt = HttpMediaTypeField.fromString("Test",
		        "text/html;charset=utf-8");
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
		mt = HttpMediaTypeField.fromString("Test",
		        "Text/HTML;Charset=\"utf-8\"");
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
		mt = HttpMediaTypeField.fromString("Test",
		        "text/html; charset=\"utf-8\"");
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
	}
	
	@Test
	public void testParseDateType() throws ParseException {
		String dateTime = "Tue, 15 Nov 1994 08:12:31 GMT";
		HttpDateTimeField field = HttpDateTimeField
				.fromString(HttpField.DATE, dateTime);
		ZonedDateTime value = field.getValue().atZone(ZoneId.of("GMT"));
		assertEquals(15, value.getDayOfMonth());
		assertEquals(Month.NOVEMBER, value.getMonth());
		assertEquals(1994, value.getYear());
		assertEquals(8, value.getHour());
		assertEquals(12, value.getMinute());
		assertEquals(31, value.getSecond());
		HttpDateTimeField back = new HttpDateTimeField(
				HttpField.DATE, value.toInstant());
		assertEquals(dateTime, back.asFieldValue());
	}
	
	@Test
	public void testParseDateRfc850() throws ParseException {
		String dateTime = "Sunday, 06-Nov-94 08:49:37 GMT";
		HttpDateTimeField field = HttpDateTimeField
				.fromString(HttpField.DATE, dateTime);
		ZonedDateTime value = field.getValue().atZone(ZoneId.of("GMT"));
		assertEquals(6, value.getDayOfMonth());
		assertEquals(Month.NOVEMBER, value.getMonth());
		assertEquals(1994, value.getYear());
		assertEquals(8, value.getHour());
		assertEquals(49, value.getMinute());
		assertEquals(37, value.getSecond());
	}
	
	@Test
	public void testParseDateAnsi() throws ParseException {
		String dateTime = "Sun Nov  6 08:49:37 1994";
		HttpDateTimeField field = HttpDateTimeField
				.fromString(HttpField.DATE, dateTime);
		ZonedDateTime value = field.getValue().atZone(ZoneId.of("GMT"));
		assertEquals(6, value.getDayOfMonth());
		assertEquals(Month.NOVEMBER, value.getMonth());
		assertEquals(1994, value.getYear());
		assertEquals(8, value.getHour());
		assertEquals(49, value.getMinute());
		assertEquals(37, value.getSecond());
	}
	
	@Test
	public void testIntFromString() throws ParseException {
		HttpIntField field = HttpIntField.fromString("test", "42");
		assertEquals(42, field.getValue().longValue());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testAccept() throws ParseException {
		HttpWeightedListField<MediaRange> field 
			= (HttpWeightedListField<MediaRange>)HttpField.fromString("Accept",
				"text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
		field.sortByWeightDesc();
		Iterator<MediaRange> itr = field.iterator();
		assertEquals("text/html", itr.next().toString());
		assertEquals("text/x-c", itr.next().toString());
		assertEquals("text/x-dvi; q=0.8", itr.next().toString());
		assertEquals("text/plain; q=0.5", itr.next().toString());
		// Second
		field = (HttpWeightedListField<MediaRange>)HttpField.fromString(
				"Accept", "audio/*; q=0.2, audio/basic");
		itr = field.iterator();
		assertEquals("audio/*; q=0.2", itr.next().toString());
		assertEquals("audio/basic", itr.next().toString());
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAcceptCharset() throws ParseException {
		HttpWeightedListField<ParameterizedValue<String>> field
			= (HttpWeightedListField<ParameterizedValue<String>>)HttpField
			.fromString("Accept-Charset", "iso-8859-5, unicode-1-1;q=0.8");
		field.sortByWeightDesc();
		Iterator<ParameterizedValue<String>> itr = field.iterator();
		assertEquals("iso-8859-5", itr.next().toString());
		assertEquals("unicode-1-1; q=0.8", itr.next().toString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAcceptLocale() throws ParseException {
		HttpWeightedListField<ParameterizedValue<Locale>> field
			= (HttpWeightedListField<ParameterizedValue<Locale>>)HttpField
			.fromString("Accept-Language", "da, en-gb;q=0.8, *; q=0.1, en;q=0.7");
		field.sortByWeightDesc();
		Converter<ParameterizedValue<Locale>> itemConverter 
			= field.getConverter().getItemConverter();
		Iterator<ParameterizedValue<Locale>> itr = field.iterator();
		assertEquals("da", itemConverter.asFieldValue(itr.next()));
		assertEquals("en-GB; q=0.8", 
				itemConverter.asFieldValue(itr.next()));
		assertEquals("en; q=0.7", 
				itemConverter.asFieldValue(itr.next()));
		assertEquals("; q=0.1", 
				itemConverter.asFieldValue(itr.next()));
	}
	
	@Test
	public void testAllow() throws ParseException {
		HttpStringListField field = (HttpStringListField)HttpField
				.fromString("Allow", "GET, HEAD, PUT");
		assertEquals("GET", field.get(0));
		assertEquals("HEAD", field.get(1));
		assertEquals("PUT", field.get(2));
	}
}
