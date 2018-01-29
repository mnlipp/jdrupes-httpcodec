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

package org.jdrupes.httpcodec.test.ws;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.websocket.WsCloseFrame;
import org.jdrupes.httpcodec.protocols.websocket.WsDecoder;
import org.jdrupes.httpcodec.protocols.websocket.WsEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsFrameHeader;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.protocols.websocket.WsPingFrame;
import org.jdrupes.httpcodec.protocols.websocket.WsPongFrame;

import static org.junit.Assert.*;
import org.junit.Test;

public class MiscTests {

	@Test
	public void testDecodeUnmaskedEmptyPing() throws ProtocolException {
		byte[] msgBytes = new byte[] {(byte)0x89, 0x00};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().isPresent());
		assertEquals(WsPingFrame.class, decoder.header().get().getClass());
		WsPingFrame pingHdr = (WsPingFrame)decoder.header().get();
		assertFalse(pingHdr.applicationData().isPresent());
		assertTrue(result.response().isPresent());
		WsPongFrame pongHdr = (WsPongFrame)result.response().get();
		assertFalse(pongHdr.applicationData().isPresent());
	}
	
	@Test
	public void testEncodeUnmaskedEmptyPing() 
			throws ProtocolException, UnsupportedEncodingException {
		final byte[] pingBytes = new byte[] {(byte)0x89, 0x00};
		WsEncoder encoder = new WsEncoder(false);
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsPingFrame(null));
		Encoder.Result result = encoder.encode(null, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertEquals(pingBytes.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(pingBytes, msgBytes);
	}
	
	@Test
	public void testDecodeUnmaskedEmptyPong() throws ProtocolException {
		byte[] msgBytes = new byte[] {(byte)0x8a, 0x00};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().isPresent());
		assertEquals(WsPongFrame.class, decoder.header().get().getClass());
		WsPongFrame pongHdr = (WsPongFrame)decoder.header().get();
		assertFalse(pongHdr.applicationData().isPresent());
		assertFalse(result.response().isPresent());
	}
	
	@Test
	public void testEncodeUnmaskedEmptyPong() 
			throws ProtocolException, UnsupportedEncodingException {
		final byte[] pongBytes = new byte[] {(byte)0x8a, 0x00};
		WsEncoder encoder = new WsEncoder(false);
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsPongFrame(null));
		Encoder.Result result = encoder.encode(null, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertEquals(pongBytes.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(pongBytes, msgBytes);
	}
	
	@Test
	public void testDecodeInsertedPing() throws ProtocolException {
		// First frame
		byte[] msgBytes1 = new byte[] {0x01, 0x03, 0x48, 0x65, 0x6c};
		ByteBuffer msg = ByteBuffer.allocate(40);
		msg.put(msgBytes1);
		msg.flip();
		CharBuffer txt = CharBuffer.allocate(20);
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, txt, false);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().isPresent());
		assertTrue(decoder.header().get() instanceof WsMessageHeader);
		WsMessageHeader hdr = (WsMessageHeader)decoder.header().get();
		assertTrue(hdr.isTextMode());
		assertTrue(hdr.hasPayload());
		
		// Ping
		byte[] pingBytes = new byte[] 
				{(byte)0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		msg.clear();
		msg.put(pingBytes);
		msg.flip();
		result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(decoder.header().isPresent());
		assertTrue(decoder.header().get() instanceof WsPingFrame);
		WsPingFrame pingHdr = (WsPingFrame)decoder.header().get();
		CharBuffer pingData = Charset.forName("utf-8")
				.decode(pingHdr.applicationData().get());
		assertEquals("Hello", pingData.toString());
		assertTrue(result.response().isPresent());
		WsPongFrame pongHdr = (WsPongFrame)result.response().get();
		CharBuffer pongData = Charset.forName("utf-8")
				.decode(pongHdr.applicationData().get());
		assertEquals("Hello", pongData.toString());
		
		// Second frame
		msg.clear();
		byte[] msgBytes2 = new byte[] {(byte)0x80, 0x02, 0x6c, 0x6f};
		msg.put(msgBytes2);
		msg.flip();
		result = decoder.decode(msg, txt, false);
		txt.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertFalse(result.isHeaderCompleted());
		assertEquals("Hello", txt.toString());
	}

	@Test
	public void testEncodeInsertedPing() 
			throws ProtocolException, UnsupportedEncodingException {
		final byte[] msgBytes1 = new byte[] {0x01, 0x03, 0x48, 0x65, 0x6c};
		final byte[] msgBytes2 = new byte[] {(byte)0x80, 0x02, 0x6c, 0x6f};
		final byte[] pingBytes = new byte[] 
				{(byte)0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f};
		WsEncoder encoder = new WsEncoder(false);
		CharBuffer txt = CharBuffer.allocate(20);
		txt.put("Hel");
		txt.flip();
		ByteBuffer msg = ByteBuffer.allocate(100);
		encoder.encode(new WsMessageHeader(true, true));
		Encoder.Result result = encoder.encode(txt, msg, false);
		msg.flip();
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertEquals(msgBytes1.length, msg.remaining());
		byte[] msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);		
		assertArrayEquals(msgBytes1, msgBytes);

		// Ping
		ByteBuffer appData = ByteBuffer.allocate(20);
		appData.put("Hello".getBytes("utf-8"));
		appData.flip();
		msg.clear();
		encoder.encode(new WsPingFrame(appData));
		result = encoder.encode(null, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertTrue(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertEquals(pingBytes.length, msg.remaining());
		msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(pingBytes, msgBytes);
		
		// Part 2
		txt.clear();
		txt.put("lo");
		txt.flip();
		msg.clear();
		result = encoder.encode(txt, msg, true);
		msg.flip();
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());			
		assertEquals(msgBytes2.length, msg.remaining());
		msgBytes = new byte[msg.remaining()];
		msg.get(msgBytes);
		assertArrayEquals(msgBytes2, msgBytes);
	}
	
	@Test
	public void testDecodeMaskedSimpleClose() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x88, (byte)0x80, 0, 0, 0, 0 };
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertTrue(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(result.response().isPresent());
		assertTrue(result.response().get() instanceof WsCloseFrame);
		assertFalse(((WsCloseFrame)result.response().get()).statusCode().isPresent());
		assertTrue(decoder.header().isPresent());
		assertTrue(decoder.header().get() instanceof WsCloseFrame);
		// Subsequent encode must close
		WsEncoder encoder = new WsEncoder(false);
		encoder.encode((WsFrameHeader)result.response().get());
		msg = ByteBuffer.allocate(100);
		Encoder.Result encRes = encoder.encode(null, msg, true);
		assertTrue(encRes.closeConnection());
		assertTrue(msg.position() > 0);
	}

	@Test
	public void testDecodeMaskedStatusClose() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x88, (byte)0x82, 0, 0, 0, 0, 1, 2 };
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(result.response().isPresent());
		assertTrue(result.response().get() instanceof WsCloseFrame);
		assertTrue(((WsCloseFrame)result.response().get()).statusCode().isPresent());
		assertEquals(258, ((WsCloseFrame)result.response().get()).statusCode().get().intValue());
		assertTrue(decoder.header().isPresent());
		assertTrue(decoder.header().get() instanceof WsCloseFrame);
		WsCloseFrame hdr = (WsCloseFrame)decoder.header().get();
		assertEquals(258, hdr.statusCode().get().intValue());
		// Subsequent encode must close
		WsEncoder encoder = new WsEncoder(false);
		encoder.encode((WsFrameHeader)result.response().get());
		msg = ByteBuffer.allocate(100);
		Encoder.Result encRes = encoder.encode(null, msg, true);
		assertTrue(encRes.closeConnection());
		assertTrue(msg.position() > 0);
	}

	@Test
	public void testDecodeMaskedReasonClose() throws ProtocolException {
		byte[] msgBytes = new byte[] 
				{(byte)0x88, (byte)0x87, 0, 0, 0, 0, 1, 2, 
						(byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o'};
		ByteBuffer msg = ByteBuffer.allocate(msgBytes.length);
		msg.put(msgBytes);
		msg.flip();
		WsDecoder decoder = new WsDecoder();
		Decoder.Result<?> result = decoder.decode(msg, null, true);
		assertFalse(result.isOverflow());
		assertFalse(result.isUnderflow());
		assertFalse(result.closeConnection());
		assertTrue(result.isHeaderCompleted());
		assertTrue(result.response().isPresent());
		assertTrue(result.response().get() instanceof WsCloseFrame);
		assertTrue(((WsCloseFrame)result.response().get()).statusCode().isPresent());
		assertEquals(258, ((WsCloseFrame)result.response().get()).statusCode().get().intValue());
		assertTrue(decoder.header().isPresent());
		assertTrue(decoder.header().get() instanceof WsCloseFrame);
		WsCloseFrame hdr = (WsCloseFrame)decoder.header().get();
		assertEquals(258, hdr.statusCode().get().intValue());
		assertEquals("Hello", hdr.reason().get());
		// Subsequent encode must close
		WsEncoder encoder = new WsEncoder(false);
		encoder.encode((WsFrameHeader)result.response().get());
		msg = ByteBuffer.allocate(100);
		Encoder.Result encRes = encoder.encode(null, msg, true);
		assertTrue(encRes.closeConnection());
		assertTrue(msg.position() > 0);		
	}

	@Test
	public void testEncodeClose() throws ProtocolException {
		WsFrameHeader hdr = new WsCloseFrame(0, CharBuffer.wrap("Test"));
		WsEncoder encoder = new WsEncoder(false);
		encoder.encode(hdr);
		ByteBuffer encoded = ByteBuffer.allocate(100);
		Encoder.Result encRes = encoder.encode(null, encoded, true);
		assertFalse(encRes.closeConnection());
		WsDecoder decoder = new WsDecoder();
		ByteBuffer decoded = ByteBuffer.allocate(100);
		encoded.flip();
		Decoder.Result<?> decRes = decoder.decode(encoded, decoded, true);
		assertTrue(decRes.response().isPresent());
		encoder.encode((WsFrameHeader)decRes.response().get());
		encoded.clear();
		encRes = encoder.encode(null, encoded, true);
		assertTrue(encRes.closeConnection());
	}

}
