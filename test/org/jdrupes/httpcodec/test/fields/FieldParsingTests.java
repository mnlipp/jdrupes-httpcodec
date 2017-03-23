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

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpMessageHeader;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.types.CommentedValue;
import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.ListConverter;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jdrupes.httpcodec.types.StringList;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Field parsing tests.
 */
public class FieldParsingTests {

	@Test
	public void testTokenLength() {
		assertEquals(5, HttpField.tokenLength("Hello? ", 0));
	}
	
	@Test
	public void testWhiteSpaceLength() {
		assertEquals(4, HttpField.whiteSpaceLength(" \t  Hallo? ", 0));
	}

	@Test
	public void testCommentLength() {
		assertEquals(30, HttpField.commentLength(
				"(Leading comment (as example)) Value", 0));
		assertEquals(23, HttpField.commentLength(
				"Value (Comment: \\), strange?) Rest", 6));
	}
	
	@Test
	public void testString() throws ParseException {
		HttpField<?> fv = new HttpField<String>("Test: Hello", Converters.STRING);
		assertEquals("Hello", fv.value());
	}

	@Test
	public void testStringList() throws ParseException {
		HttpField<StringList> fv = new HttpField<>(
				"Test: How, are,you,  out, there", Converters.STRING_LIST);
		assertEquals("How", fv.value().get(0));
		assertEquals("are", fv.value().get(1));
		assertEquals("you", fv.value().get(2));
		assertEquals("out", fv.value().get(3));
		assertEquals("there", fv.value().get(4));
		assertEquals(5, fv.value().size());
	}

	@Test
	public void testQuoted() throws ParseException {
		HttpField<StringList> fv = new HttpField<>(
				"Test: \"How \\\"are\",you,  \"out, there\"",
				Converters.STRING_LIST);
		assertEquals("How \"are", fv.value().get(0));
		assertEquals("you", fv.value().get(1));
		assertEquals("out, there", fv.value().get(2));
		assertEquals(3, fv.value().size());
	}

	@Test
	public void testUnquote() throws ParseException {
		HttpField<?> fv = new HttpField<String>(
				"Test: How are you?", Converters.STRING);
		assertEquals("How are you?", fv.value());
		fv = new HttpField<String>(
				"Test: \"How \\\"are\"", Converters.STRING);
		assertEquals("How \"are", fv.value());
	}
	
	@Test
	public void testMediaType() throws ParseException {
		HttpField<MediaType> mt = new HttpField<>(
				"Test: text/html;charset=utf-8", Converters.MEDIA_TYPE);
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
		mt = new HttpField<>("Test: Text/HTML;Charset=\"utf-8\"",
				Converters.MEDIA_TYPE);
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
		mt = new HttpField<>("Test: text/html; charset=\"utf-8\"",
				Converters.MEDIA_TYPE);
		assertEquals("text/html; charset=utf-8", mt.asFieldValue());
	}
	
	@Test
	public void testParseDateType() throws ParseException {
		String dateTime = "Tue, 15 Nov 1994 08:12:31 GMT";
		HttpField<Instant> field = new HttpField<>(
				HttpField.DATE + ": " + dateTime, Converters.DATE_TIME);
		ZonedDateTime value = field.value().atZone(ZoneId.of("GMT"));
		assertEquals(15, value.getDayOfMonth());
		assertEquals(Month.NOVEMBER, value.getMonth());
		assertEquals(1994, value.getYear());
		assertEquals(8, value.getHour());
		assertEquals(12, value.getMinute());
		assertEquals(31, value.getSecond());
		HttpField<Instant> back = new HttpField<>(
				HttpField.DATE, value.toInstant(), Converters.DATE_TIME);
		assertEquals(dateTime, back.asFieldValue());
	}
	
	@Test
	public void testParseDateRfc850() throws ParseException {
		String dateTime = "Sunday, 06-Nov-94 08:49:37 GMT";
		HttpField<Instant> field = new HttpField<>(
				HttpField.DATE + ": " + dateTime, Converters.DATE_TIME);
		ZonedDateTime value = field.value().atZone(ZoneId.of("GMT"));
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
		HttpField<Instant> field = new HttpField<>(
				HttpField.DATE + ": " + dateTime, Converters.DATE_TIME);
		ZonedDateTime value = field.value().atZone(ZoneId.of("GMT"));
		assertEquals(6, value.getDayOfMonth());
		assertEquals(Month.NOVEMBER, value.getMonth());
		assertEquals(1994, value.getYear());
		assertEquals(8, value.getHour());
		assertEquals(49, value.getMinute());
		assertEquals(37, value.getSecond());
	}
	
	@Test
	public void testIntFromString() throws ParseException {
		HttpField<Long> field = new HttpField<>(
				"test: 42", Converters.LONG);
		assertEquals(42, field.value().longValue());
	}
	
	@Test
	public void testAcceptCharset() throws ParseException, URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(HttpField.ACCEPT_CHARSET, "iso-8859-5, unicode-1-1;q=0.8");
		List<ParameterizedValue<String>> value = hdr.getValue(
				HttpField.ACCEPT_CHARSET, Converters.WEIGHTED_STRINGS).get();
		Collections.sort(value, ParameterizedValue.WEIGHT_COMPARATOR);
		Iterator<ParameterizedValue<String>> itr = value.iterator();
		assertEquals("iso-8859-5", itr.next().toString());
		assertEquals("unicode-1-1; q=0.8", itr.next().toString());
	}

	@Test
	public void testAcceptLocale() throws ParseException, URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(HttpField.ACCEPT_LANGUAGE,
				"da, en-gb;q=0.8, *; q=0.1, en;q=0.7");
		HttpField<List<ParameterizedValue<Locale>>> field = hdr.getField(
				HttpField.ACCEPT_LANGUAGE, Converters.LANGUAGE_LIST).get();
		Collections.sort(field.value(), ParameterizedValue.WEIGHT_COMPARATOR);
		@SuppressWarnings("unchecked")
		Converter<ParameterizedValue<Locale>> itemConverter 
			= ((ListConverter<List<ParameterizedValue<Locale>>, 
					ParameterizedValue<Locale>>)field.converter()).getItemConverter();
		Iterator<ParameterizedValue<Locale>> itr = field.value().iterator();
		assertEquals("da", itemConverter.asFieldValue(itr.next()));
		assertEquals("en-GB; q=0.8", 
				itemConverter.asFieldValue(itr.next()));
		assertEquals("en; q=0.7", 
				itemConverter.asFieldValue(itr.next()));
		assertEquals("; q=0.1", 
				itemConverter.asFieldValue(itr.next()));
	}
	
	@Test
	public void testAllow() throws ParseException, URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(HttpField.ALLOW, "GET, HEAD, PUT");
		StringList field = hdr.getValue(
				HttpField.ALLOW, Converters.STRING_LIST).get();
		assertEquals("GET", field.get(0));
		assertEquals("HEAD", field.get(1));
		assertEquals("PUT", field.get(2));
	}
	
	@Test public void testUserAgent() throws ParseException {
		HttpField<List<CommentedValue<String>>> field 
			= new HttpField<>("User-Agent: CERN-LineMode/2.15 libwww/2.17b3",
					Converters.PRODUCT_DESCRIPTIONS);
		assertEquals("CERN-LineMode/2.15", field.value().get(0).getValue());
		assertEquals("libwww/2.17b3", field.value().get(1).getValue());
		
		field = new HttpField<>("User-Agent: Client",
				Converters.PRODUCT_DESCRIPTIONS);
		assertEquals("Client", field.value().get(0).getValue());
		
		field = new HttpField<>(
				"User-Agent: CERN-LineMode/2.15 (deprecated) "
				+ "(I think) libwww/2.17b3 (very old)",
				Converters.PRODUCT_DESCRIPTIONS);
		assertEquals("CERN-LineMode/2.15", field.value().get(0).getValue());
		assertEquals("deprecated", field.value().get(0).getComments()[0]);
		assertEquals("I think", field.value().get(0).getComments()[1]);
		assertEquals("libwww/2.17b3", field.value().get(1).getValue());
		assertEquals("very old", field.value().get(1).getComments()[0]);
	}
	
	@Test
	public void testContentLocation() throws ParseException, URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(HttpField.CONTENT_LOCATION, "test/index.html");
		HttpField<URI> field = hdr.getField(
				HttpField.CONTENT_LOCATION, Converters.URI_CONV).get();
		assertEquals("test/index.html", field.value().getPath());
	}
	
	@Test
	public void testMaxForwards() throws ParseException, URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpField<Long>(
				"Max-Forwards: 10", Converters.LONG));
		HttpField<Long> field = hdr.getField(
				HttpField.MAX_FORWARDS, Converters.LONG).get();
		assertEquals(10, field.value().intValue());
	}

	@Test
	public void testCacheControl() throws ParseException, URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpField<List<Directive>>(
				"Cache-Control: private, community=\"UCI\"", 
				Converters.DIRECTIVE_LIST));
		HttpField<List<Directive>> field = hdr.getField(
				HttpField.CACHE_CONTROL, Converters.DIRECTIVE_LIST).get();
		assertEquals("private", field.value().get(0).getName());
		assertFalse(field.value().get(0).getValue().isPresent());
		assertEquals("community", field.value().get(1).getName());
		assertTrue(field.value().get(1).getValue().isPresent());
		assertEquals("UCI", field.value().get(1).getValue().get());
	}

}
