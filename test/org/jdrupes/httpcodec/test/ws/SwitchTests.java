package org.jdrupes.httpcodec.test.ws;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jdrupes.httpcodec.ClientEngine;
import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.client.HttpRequestEncoder;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsFrameHeader;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.types.StringList;
import static org.junit.Assert.*;
import org.junit.Test;

public class SwitchTests {

	@Test
	public void testRequest() throws URISyntaxException, ProtocolException {
		// First encode
		HttpRequest clntReq = new HttpRequest("GET", new URI("http://localhost"),
		        HttpProtocol.HTTP_1_1, false);
		clntReq.setField(HttpField.UPGRADE, new StringList("websocket"));
		ClientEngine<HttpRequest, HttpResponse> client = new ClientEngine<>(
				new HttpRequestEncoder(), new HttpResponseDecoder());
		client.encode(clntReq);
		ByteBuffer msg = ByteBuffer.allocate(1024*1024);
		Codec.Result codecRes = client.encode(msg);
		assertFalse(codecRes.isOverflow());
		assertFalse(codecRes.isUnderflow());
		String encoded = new String(msg.array(), 0, msg.position());
		assertTrue(encoded.contains("Connection: Upgrade"));
		assertTrue(encoded.contains("Sec-WebSocket-Key: "));
		assertTrue(encoded.contains("Sec-WebSocket-Version: 13"));
		
		// Now decode
		msg.flip();
		ServerEngine<HttpRequest, HttpResponse> server = new ServerEngine<>(
				new HttpRequestDecoder(), new HttpResponseEncoder());
		ByteBuffer byteBody = ByteBuffer.allocate(1024*1024);
		Decoder.Result<HttpResponse> srvDec = server.decode(msg, byteBody, false);
		assertTrue(srvDec.isHeaderCompleted());
		
		// Encode confirmation
		HttpResponse srvResp = server.currentRequest().get().response().get();
		srvResp.setStatus(HttpStatus.SWITCHING_PROTOCOLS);
		srvResp.setField(HttpField.UPGRADE, new StringList("websocket"));
		msg.clear();
		server.encode(srvResp);
		server.encode(msg);
		encoded = new String(msg.array(), 0, msg.position());
		assertTrue(encoded.contains("Connection: Upgrade"));
		assertTrue(encoded.contains("Sec-WebSocket-Accept: "));
		
		// Decode confirmation
		msg.flip();
		byteBody.clear();
		Decoder.Result<?> clntDec = client.decode(msg, byteBody, false);
		assertTrue(clntDec.isHeaderCompleted());
		assertEquals("websocket", server.switchedTo().get());
		assertEquals("websocket", client.switchedTo().get());
		
		// Now we should be able to send and receive WS messages.
		CharBuffer charBody = CharBuffer.allocate(1024*1024);
		charBody.put("Client2Server");
		charBody.flip();
		msg.clear();
		@SuppressWarnings("unchecked")
		ClientEngine<WsFrameHeader, WsFrameHeader> wsClient
			= (ClientEngine<WsFrameHeader, WsFrameHeader>)(Object)client;
		wsClient.encode(new WsMessageHeader(true, true));
		codecRes = wsClient.encode(charBody, msg, true);
		assertFalse(codecRes.isOverflow());
		assertFalse(codecRes.isUnderflow());
		msg.flip();
		charBody.clear();
		codecRes = server.decode(msg, charBody, true);
		assertFalse(codecRes.isOverflow());
		assertFalse(codecRes.isUnderflow());
		charBody.flip();
		assertEquals("Client2Server", charBody.toString());
		
		// Other direction
		charBody.clear();
		charBody.put("Server2Client");
		charBody.flip();
		msg.clear();
		@SuppressWarnings("unchecked")
		ServerEngine<WsFrameHeader, WsFrameHeader> wsServer
			= (ServerEngine<WsFrameHeader, WsFrameHeader>)(Object)server;
		wsServer.encode(new WsMessageHeader(true, true));
		codecRes = wsServer.encode(charBody, msg, true);
		assertFalse(codecRes.isOverflow());
		assertFalse(codecRes.isUnderflow());
		msg.flip();
		charBody.clear();
		codecRes = wsClient.decode(msg, charBody, true);
		assertFalse(codecRes.isOverflow());
		assertFalse(codecRes.isUnderflow());
		charBody.flip();
		assertEquals("Server2Client", charBody.toString());
	}

}
