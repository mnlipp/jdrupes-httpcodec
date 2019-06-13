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
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.types.StringList;
import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTests {

	private static class Server {
		public enum State { IDLE, CONNECT_REQUESTED, CONFIRMING_CONNECTION,
			CONNECTED, SENDING }
		
		private ServerEngine<?, ?> server = new ServerEngine<>(
				new HttpRequestDecoder(), new HttpResponseEncoder());
		public State state = State.IDLE;
		
		@SuppressWarnings("unchecked")
		public Decoder.Result<?> decodeHttp(
				ByteBuffer in, ByteBuffer out) throws ProtocolException {
			Decoder.Result<?> result = server.decode(in, out, false);
			if (result.isHeaderCompleted()) {
				if (((ServerEngine<HttpRequest, HttpResponse>)server)
						.currentRequest().get().fields()
						.containsKey("UPGRADE")) {
					state = State.CONNECT_REQUESTED;
				}
			}
			return result;
		}

		@SuppressWarnings("unchecked")
		public Codec.Result encodeConnectResponse(ByteBuffer out) {
			if (state == State.CONNECT_REQUESTED) {
				HttpResponse response = ((ServerEngine<HttpRequest, 
						HttpResponse>)server).currentRequest().get()
						.response().get();
				response.setStatus(HttpStatus.SWITCHING_PROTOCOLS);
				response.setField(HttpField.UPGRADE, new StringList("websocket"));
				((ServerEngine<HttpRequest, HttpResponse>)server)
					.encode(response);
				state = State.CONFIRMING_CONNECTION;
			}
			Codec.Result result = server.encode(out);
			if (!result.isOverflow()) {
				state = State.CONNECTED;
			}
			return result;
		}
		
		public Decoder.Result<?> decodeWebSocketText(ByteBuffer in, CharBuffer out) 
				throws ProtocolException {
			return server.decode(in, out, false);
		}
		
		@SuppressWarnings("unchecked")
		public Codec.Result encodeWebSocketText(CharBuffer in, ByteBuffer out) {
			if (state == State.CONNECTED) {
				((ServerEngine<WsMessageHeader, WsMessageHeader>)server)
					.encode(new WsMessageHeader(true, true));
				state = State.SENDING;
			}
			Codec.Result result 
				= ((ServerEngine<WsMessageHeader, WsMessageHeader>)server)
				.encode(in, out, true);
			if (!result.isOverflow()) {
				state = State.CONNECTED;
			}
			return result;
		}
	}
	
	private static class Client {
		
		public enum State { IDLE, CONNECTING, CONNECTED, SENDING }
		
		private ClientEngine<?,?> client 
			= new ClientEngine<>(new HttpRequestEncoder(), 
					new HttpResponseDecoder());
		public State state = State.IDLE;
		
		@SuppressWarnings("unchecked")
		public Codec.Result encodeConnectRequest(ByteBuffer out) {
			if (state == State.IDLE) {
				try {
					HttpRequest clntReq = new HttpRequest("GET", 
							new URI("http://localhost"),
							HttpProtocol.HTTP_1_1, false);
					clntReq.setField(HttpField.UPGRADE, new StringList("websocket"));
					((ClientEngine<HttpRequest, HttpResponse>)client)
						.encode(clntReq);
					state = State.CONNECTING;
				} catch (URISyntaxException e) {
					fail();
				}
			}
			return client.encode(out);
		}
		
		public Decoder.Result<?> decodeHttpResponse(
				ByteBuffer in, ByteBuffer out) throws ProtocolException {
			Decoder.Result<?> result = client.decode(in, out, false);
			client.switchedTo().ifPresent(s -> state = State.CONNECTED);
			return result;
		}
		
		@SuppressWarnings("unchecked")
		public Codec.Result encodeWebSocketText(CharBuffer in, ByteBuffer out) {
			if (state == State.CONNECTED) {
				((ClientEngine<WsMessageHeader, WsMessageHeader>)client)
					.encode(new WsMessageHeader(true, true));
				state = State.SENDING;
			}
			Codec.Result result 
				= ((ClientEngine<WsMessageHeader, WsMessageHeader>)client)
				.encode(in, out, true);
			if (!result.isOverflow()) {
				state = State.CONNECTED;
			}
			return result;
		}
		
		public Decoder.Result<?> decodeWebSocketText(ByteBuffer in, CharBuffer out) 
				throws ProtocolException {
			return client.decode(in, out, false);
		}
		
	}
	
	@Test
	public void testFragmented() throws URISyntaxException, ProtocolException {
		Client client = new Client();
		Server server = new Server();
		
		// Connect
		ByteBuffer msg = ByteBuffer.allocate(64);
		ByteBuffer byteBody = ByteBuffer.allocate(1024*1024);
		
		Decoder.Result<?> decodeResult;
		while (true) {
			Codec.Result encRes = client.encodeConnectRequest(msg);
			assertFalse(encRes.isUnderflow());
			msg.flip();
			decodeResult = server.decodeHttp(msg, byteBody);
			if (decodeResult.isHeaderCompleted()) {
				break;
			}
			msg.clear();
		}
		assertFalse(decodeResult.isUnderflow());
		assertFalse(decodeResult.isOverflow());
		
		// Confirm connect
		msg.clear();
		byteBody.clear();
		while (true) {
			Codec.Result encRes = server.encodeConnectResponse(msg);
			assertFalse(encRes.isUnderflow());
			msg.flip();
			decodeResult = client.decodeHttpResponse(msg, byteBody);
			assertFalse(decodeResult.isOverflow());
			if (decodeResult.isHeaderCompleted()) {
				assertFalse(decodeResult.isUnderflow());
				break;
			}
			assertTrue(decodeResult.isUnderflow());
			msg.clear();
		}
		assertTrue(client.state == Client.State.CONNECTED);
		
		// Send message from client to server
		CharBuffer outText = CharBuffer.allocate((int)(2.5*msg.capacity()));
		for (int i = 0; outText.hasRemaining(); i++) {
			String str = (i != 0 ? ", " : "") + i;
			if (outText.remaining() < str.length()) {
				str = str.substring(0, outText.remaining());
			}
			outText.put(str);
		}
		outText.flip();
		msg.clear();
		CharBuffer rcvdText = CharBuffer.allocate(1024*1024);
		while (true) {
			if (outText.hasRemaining()) {
				Codec.Result encRes = client.encodeWebSocketText(outText, msg);
				assertFalse(encRes.isUnderflow());
			}
			msg.flip();
			decodeResult = server.decodeWebSocketText(msg, rcvdText);
			if (!decodeResult.isOverflow() && !decodeResult.isUnderflow()) {
				assertFalse(outText.hasRemaining());
				break;
			}
			msg.clear();
		}
		outText.flip();
		rcvdText.flip();
		assertEquals(outText.toString(), rcvdText.toString());
		
		// Send message from server to client
		msg.clear();
		rcvdText.clear();
		while (true) {
			if (outText.hasRemaining()) {
				Codec.Result encRes = server.encodeWebSocketText(outText, msg);
				assertFalse(encRes.isUnderflow());
			}
			msg.flip();
			decodeResult = client.decodeWebSocketText(msg, rcvdText);
			if (!decodeResult.isOverflow() && !decodeResult.isUnderflow()) {
				assertFalse(outText.hasRemaining());
				break;
			}
			msg.clear();
		}
		outText.flip();
		rcvdText.flip();
		assertEquals(outText.toString(), rcvdText.toString());
		
	}

}
