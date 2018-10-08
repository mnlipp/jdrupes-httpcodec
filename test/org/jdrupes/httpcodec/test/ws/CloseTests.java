package org.jdrupes.httpcodec.test.ws;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.ClientEngine;
import org.jdrupes.httpcodec.Codec.Result;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.websocket.WsCloseFrame;
import org.jdrupes.httpcodec.protocols.websocket.WsDecoder;
import org.jdrupes.httpcodec.protocols.websocket.WsEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsFrameHeader;
import static org.junit.Assert.*;
import org.junit.Test;

public class CloseTests {

	@Test
	public void testServerClose() throws URISyntaxException, ProtocolException {
		ServerEngine<WsFrameHeader, WsFrameHeader> server = new ServerEngine<>(
				new WsDecoder(), new WsEncoder(false));
		ByteBuffer msg = ByteBuffer.allocate(1024*1024);
		// Encode close on server
		WsCloseFrame closeMsg 
			= new WsCloseFrame(42, CharBuffer.wrap("Testing..."));
		server.encode(closeMsg);
		Result encResult = server.encode(msg);
		assertFalse(encResult.isOverflow());
		assertFalse(encResult.isUnderflow());
		assertFalse(encResult.closeConnection());

		// Decode close on client
		ClientEngine<WsFrameHeader, WsFrameHeader> client = new ClientEngine<>(
				new WsEncoder(true), new WsDecoder());
		msg.flip();
		CharBuffer payload = CharBuffer.allocate(1024*1024);
		Decoder.Result<WsFrameHeader> decResult = client.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertFalse(decResult.closeConnection());
		WsFrameHeader recvd = client.responseDecoder().header().get();
		assertTrue(recvd instanceof WsCloseFrame);
		assertEquals(42,  ((WsCloseFrame)recvd).statusCode().get().intValue());
		assertEquals("Testing...", ((WsCloseFrame)recvd).reason().get());
		assertTrue(decResult.response().isPresent());
		
		// Feed back prepared response
		msg.clear();
		client.encode(decResult.response().get());
		client.encode(msg);
		msg.flip();
		payload.clear();
		decResult = server.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertTrue(decResult.closeConnection());
		assertTrue(server.requestDecoder().header().get()
				instanceof WsCloseFrame);
		assertEquals(42, ((WsCloseFrame)server.requestDecoder().header().get())
				.statusCode().get().intValue());
	}


	@Test
	public void testClientClose() throws URISyntaxException, ProtocolException {
		ClientEngine<WsFrameHeader, WsFrameHeader> client = new ClientEngine<>(
				new WsEncoder(true), new WsDecoder());
		// Encode close on client
		WsCloseFrame closeMsg 
			= new WsCloseFrame(42, CharBuffer.wrap("Testing..."));
		client.encode(closeMsg);
		ByteBuffer msg = ByteBuffer.allocate(1024*1024);
		Result encResult = client.encode(msg);
		assertFalse(encResult.isOverflow());
		assertFalse(encResult.isUnderflow());
		assertFalse(encResult.closeConnection());
		
		// Decode close on server
		msg.flip();
		ServerEngine<WsFrameHeader, WsFrameHeader> server = new ServerEngine<>(
				new WsDecoder(), new WsEncoder(false));
		CharBuffer payload = CharBuffer.allocate(1024*1024);
		Decoder.Result<WsFrameHeader> decResult = server.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertFalse(decResult.closeConnection());
		WsFrameHeader recvd = server.requestDecoder().header().get();
		assertTrue(recvd instanceof WsCloseFrame);
		assertEquals(42,  ((WsCloseFrame)recvd).statusCode().get().intValue());
		assertEquals("Testing...", ((WsCloseFrame)recvd).reason().get());
		assertTrue(decResult.response().isPresent());
		
		// Feed back prepared response, encode on server...
		msg.clear();
		server.encode(decResult.response().get());
		encResult = server.encode(msg);
		assertFalse(encResult.isOverflow());
		assertFalse(encResult.isUnderflow());
		assertTrue(encResult.closeConnection());
		msg.flip();
		
		// ... decode on client.
		payload.clear();
		decResult = client.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertFalse(decResult.closeConnection());
		assertTrue(client.responseDecoder().header().get()
				instanceof WsCloseFrame);
		assertEquals(42, ((WsCloseFrame)client.responseDecoder().header().get())
				.statusCode().get().intValue());
	}
	
	@Test
	public void testServerCloseNoReason()
			throws URISyntaxException, ProtocolException {
		ServerEngine<WsFrameHeader, WsFrameHeader> server = new ServerEngine<>(
				new WsDecoder(), new WsEncoder(false));
		ByteBuffer msg = ByteBuffer.allocate(1024*1024);
		// Encode close on server
		WsCloseFrame closeMsg = new WsCloseFrame(null, null);
		server.encode(closeMsg);
		Result encResult = server.encode(msg);
		assertFalse(encResult.isOverflow());
		assertFalse(encResult.isUnderflow());
		assertFalse(encResult.closeConnection());

		// Decode close on client
		ClientEngine<WsFrameHeader, WsFrameHeader> client = new ClientEngine<>(
				new WsEncoder(true), new WsDecoder());
		msg.flip();
		CharBuffer payload = CharBuffer.allocate(1024*1024);
		Decoder.Result<WsFrameHeader> decResult = client.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertFalse(decResult.closeConnection());
		WsFrameHeader recvd = client.responseDecoder().header().get();
		assertTrue(recvd instanceof WsCloseFrame);
		assertFalse(((WsCloseFrame)recvd).statusCode().isPresent());
		assertFalse(((WsCloseFrame)recvd).reason().isPresent());
		assertTrue(decResult.response().isPresent());
		
		// Feed back prepared response
		msg.clear();
		client.encode(decResult.response().get());
		client.encode(msg);
		msg.flip();
		payload.clear();
		decResult = server.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertTrue(decResult.closeConnection());
		assertTrue(server.requestDecoder().header().get()
				instanceof WsCloseFrame);
		assertFalse(((WsCloseFrame)server.requestDecoder().header().get())
				.statusCode().isPresent());
	}


	@Test
	public void testClientCloseNoReason()
			throws URISyntaxException, ProtocolException {
		ClientEngine<WsFrameHeader, WsFrameHeader> client = new ClientEngine<>(
				new WsEncoder(true), new WsDecoder());
		// Encode close on client
		WsCloseFrame closeMsg = new WsCloseFrame(null, null);
		client.encode(closeMsg);
		ByteBuffer msg = ByteBuffer.allocate(1024*1024);
		Result encResult = client.encode(msg);
		assertFalse(encResult.isOverflow());
		assertFalse(encResult.isUnderflow());
		assertFalse(encResult.closeConnection());
		
		// Decode close on server
		msg.flip();
		ServerEngine<WsFrameHeader, WsFrameHeader> server = new ServerEngine<>(
				new WsDecoder(), new WsEncoder(false));
		CharBuffer payload = CharBuffer.allocate(1024*1024);
		Decoder.Result<WsFrameHeader> decResult = server.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertFalse(decResult.closeConnection());
		WsFrameHeader recvd = server.requestDecoder().header().get();
		assertTrue(recvd instanceof WsCloseFrame);
		assertFalse(((WsCloseFrame)recvd).statusCode().isPresent());
		assertFalse(((WsCloseFrame)recvd).reason().isPresent());
		assertTrue(decResult.response().isPresent());
		
		// Feed back prepared response, encode on server...
		msg.clear();
		server.encode(decResult.response().get());
		encResult = server.encode(msg);
		assertFalse(encResult.isOverflow());
		assertFalse(encResult.isUnderflow());
		assertTrue(encResult.closeConnection());
		msg.flip();
		
		// ... decode on client.
		payload.clear();
		decResult = client.decode(msg, payload, true);
		assertFalse(decResult.isOverflow());
		assertFalse(decResult.isUnderflow());
		assertFalse(decResult.closeConnection());
		assertTrue(client.responseDecoder().header().get()
				instanceof WsCloseFrame);
		assertFalse(((WsCloseFrame)client.responseDecoder().header().get())
				.statusCode().isPresent());
	}
}
