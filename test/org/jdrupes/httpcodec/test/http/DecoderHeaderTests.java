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

package org.jdrupes.httpcodec.test.http;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 */
public class DecoderHeaderTests {

	@Test
	public void testRequestAtOnce() throws UnsupportedEncodingException {
		// Partial header
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.response().isPresent());
		assertFalse(decoder.header().get().hasPayload());
		assertEquals("GET", decoder.header().get().method());
		assertEquals("localhost", decoder.header().get().host());
		assertEquals(8888, decoder.header().get().port());
		assertEquals("/test",
		        decoder.header().get().requestUri().getPath());
	}

	/**
	 * Request with header in two parts.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testRequestSplitHeader() throws UnsupportedEncodingException {
		// Partial header
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: local";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertFalse(result.isHeaderCompleted());
		assertFalse(result.response().isPresent());
		// Continue header
		reqText 
			= "host:8888\r\n"
			+ "\r\n";
		buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.response().isPresent());
		assertFalse(decoder.header().get().hasPayload());
		assertEquals("GET", decoder.header().get().method());
		assertEquals("localhost", decoder.header().get().host());
		assertEquals(8888, decoder.header().get().port());
		assertEquals("/test",
		        decoder.header().get().requestUri().getPath());
	}

	@Test
	public void testRequestTiny() throws UnsupportedEncodingException {
		// Partial header
		String reqText 
			= "GET /test HTTP/1.1\r\n"
			+ "Host: localhost:8888\r\n"
			+ "\r\n";
		ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpRequestDecoder decoder = new HttpRequestDecoder();
		HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
		assertTrue(result.isHeaderCompleted());
		assertFalse(result.response().isPresent());
		assertFalse(decoder.header().get().hasPayload());
		assertEquals("GET", decoder.header().get().method());
		assertEquals("localhost", decoder.header().get().host());
		assertEquals(8888, decoder.header().get().port());
		assertEquals("/test",
		        decoder.header().get().requestUri().getPath());
	}

}
