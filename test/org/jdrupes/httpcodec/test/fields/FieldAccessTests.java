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
import java.util.List;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpMessageHeader;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.StringList;

import static org.junit.Assert.*;
import org.junit.Test;

public class FieldAccessTests {

	@Test
	public void testGetInt() throws URISyntaxException, ParseException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpField<>("Test: 42", Converters.STRING));
		Optional<HttpField<Long>> field 
			= hdr.getField("Test", Converters.LONG);
		assertTrue(field.isPresent());
		assertEquals(42, field.get().value().intValue());
	}

	@Test
	public void testGetStringList() throws URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpField<>("Test", "one, two", Converters.STRING));
		Optional<HttpField<StringList>> field = hdr
		        .getField("Test", Converters.STRING_LIST);
		assertTrue(field.isPresent());
		assertEquals(2, field.get().value().size());
		assertEquals("one", field.get().value().get(0));
		assertEquals("two", field.get().value().get(1));
	}

	@Test
	public void testGetIntList() throws URISyntaxException {
		HttpMessageHeader hdr = new HttpRequest("GET", new URI("/"),
		        HttpProtocol.HTTP_1_1, false);
		hdr.setField(new HttpField<>("Test", "1, 2, 3", Converters.STRING));
		Optional<HttpField<List<Long>>> field = hdr
		        .getField("Test", Converters.LONG_LIST);
		assertTrue(field.isPresent());
		assertEquals(3, field.get().value().size());
		assertEquals(1, field.get().value().get(0).longValue());
		assertEquals(2, field.get().value().get(1).longValue());
		assertEquals(3, field.get().value().get(2).longValue());
	}

}
