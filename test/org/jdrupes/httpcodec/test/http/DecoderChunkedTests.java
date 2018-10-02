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
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.test.Common;

import static org.junit.Assert.*;
import org.junit.Test;

public class DecoderChunkedTests {

	/**
	 * Response with body determined by length.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyLengthAtOnce()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Transfer-Encoding: chunked\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "7\r\n"
				+ "Hello W\r\n"
				+ "5\r\n"
				+ "orld!\r\n"
				+ "0\r\n"
				+ "\r\n";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		Decoder.Result<?> result = decoder.decode(in, body, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().get().hasPayload());
		assertFalse(result.closeConnection());
		assertEquals(HttpStatus.OK.statusCode(),
		        decoder.header().get().statusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body determined by length (first header then body).
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodySeparatePhases()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Transfer-Encoding: chunked\r\n"
				+ "Content-Length: 12\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "7\r\n"
				+ "Hello W\r\n"
				+ "5\r\n"
				+ "orld!\r\n"
				+ "0\r\n"
				+ "\r\n";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		Decoder.Result<?> result = decoder.decode(in, null, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().get().hasPayload());
		assertFalse(result.closeConnection());
		assertEquals(HttpStatus.OK.statusCode(),
		        decoder.header().get().statusCode());
		assertTrue(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(in.hasRemaining());
		ByteBuffer body = ByteBuffer.allocate(1024);
		// Decode body
		result = decoder.decode(in, body, false);
		assertFalse(result.isHeaderCompleted());
		assertTrue(decoder.header().get().hasPayload());
		assertFalse(result.closeConnection());
		assertEquals(HttpStatus.OK.statusCode(),
		        decoder.header().get().statusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		// Check result
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body determined by length, using tiny out buffer.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyLengthTiny()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Transfer-Encoding: chunked\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "7\r\n"
				+ "Hello W\r\n"
				+ "5\r\n"
				+ "orld!\r\n"
				+ "0\r\n"
				+ "\r\n";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		ByteBuffer body = ByteBuffer.allocate(1024);
		Decoder.Result<?> result = Common.tinyDecodeLoop(decoder, in, body);
		assertTrue(decoder.header().get().hasPayload());
		assertEquals(HttpStatus.OK.statusCode(), 
				decoder.header().get().statusCode());
		assertFalse(result.closeConnection());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}

	/**
	 * Response with body determined by length, decoding to charset.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws ProtocolException 
	 */
	@Test
	public void testWithBodyLengthAtOnceUtf8()
	        throws UnsupportedEncodingException, ProtocolException {
		String reqText = "HTTP/1.1 200 OK\r\n"
				+ "Date: Sat, 23 Jul 2016 16:54:54 GMT\r\n"
				+ "Last-Modified: Fri, 11 Apr 2014 15:15:17 GMT\r\n"
				+ "Transfer-Encoding: chunked\r\n"
				+ "Content-Type: text/plain; charset=UTF-8\r\n"
				+ "\r\n"
				+ "7\r\n"
				+ "Hello W\r\n"
				+ "5\r\n"
				+ "orld!\r\n"
				+ "0\r\n"
				+ "\r\n";
		ByteBuffer in = ByteBuffer.wrap(reqText.getBytes("ascii"));
		HttpResponseDecoder decoder = new HttpResponseDecoder();
		CharBuffer body = CharBuffer.allocate(1024);
		Decoder.Result<?> result = decoder.decode(in, body, false);
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().get().hasPayload());
		assertFalse(result.closeConnection());
		assertEquals(HttpStatus.OK.statusCode(),
		        decoder.header().get().statusCode());
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(in.hasRemaining());
		body.flip();
		String bodyText = new String(body.array(), body.position(),
		        body.limit());
		assertEquals("Hello World!", bodyText);
	}


}
