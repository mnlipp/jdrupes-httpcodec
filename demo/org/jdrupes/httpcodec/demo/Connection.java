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

package org.jdrupes.httpcodec.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.stream.Collectors;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsFrameHeader;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.StringList;
import org.jdrupes.httpcodec.util.FormUrlDecoder;

/**
 * A connection to a client.
 */
public class Connection extends Thread {

	private SocketChannel channel;
	private ServerEngine<?, ?> engine;
	private ByteBuffer in;
	private ByteBuffer out;
	
	public Connection(SocketChannel channel) {
		this.channel = channel;
		// Create server with HTTP decoder and encoder
		ServerEngine<HttpRequest,HttpResponse> 
			serverEngine = new ServerEngine<>(
					new HttpRequestDecoder(), new HttpResponseEncoder());
		engine = serverEngine;
		// Allocate reusable buffers
		in = ByteBuffer.allocate(2048);
		out = ByteBuffer.allocate(2048);
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try (SocketChannel channel = this.channel) {
			channel.configureBlocking(true);
			while (channel.isOpen()) {
				processInput(channel);
			}
		} catch (IOException | ProtocolException e) {
			// Just a test
		}
	}

	private void processInput(SocketChannel channel)
	        throws ProtocolException, IOException {
		// Get data from client.
		in.clear();
		channel.read(in);
		in.flip();
		while (in.hasRemaining()) {
			// Decode. Don't expect output yet, just get header.
			// (Should there be a body, result will have overflow flag set.)
			Decoder.Result<?> decoderResult = engine.decode(in, null, false);
			if (decoderResult.response().isPresent()) {
				// Decoder wants us to send some urgent feedback
				sendResponseWithoutBody(decoderResult.response().get());
				// Sending the response may imply closing the connection 
				if (!channel.isOpen()) {
					break;
				}
			}
			if (decoderResult.isResponseOnly()
					|| !decoderResult.isHeaderCompleted()) {
				// This one is done or we need more decoding to get header.
				continue;
			}
			// Got message header! Handle as appropriate.
			MessageHeader hdr = engine.currentRequest().get();
			if (hdr instanceof HttpRequest) {
				handleHttpRequest((HttpRequest) hdr);
			}
			if (hdr instanceof WsFrameHeader) {
				handleWsFrame((WsFrameHeader) hdr);
			}
		}
	}

	private void handleHttpRequest(HttpRequest request) throws IOException {
		if (request.method().equalsIgnoreCase("GET")) {
			if (request.requestUri().getPath().equals("/form")) {
				handleGetForm(request);
				return;
			}
			if (request.requestUri().getPath().equals("/echo")
					|| request.requestUri().getPath().startsWith("/echo/")) {
				handleEcho(request);
				return;
			}
		}
		if (request.method().equalsIgnoreCase("POST")
				&& request.requestUri().getPath().equals("/form")) {
			handlePostForm(request);
			return;
		}
		// fall back
		HttpResponse response = request.response().get()
				.setStatus(HttpStatus.NOT_FOUND).setMessageHasBody(true);
		try {
			HttpField<MediaType> media = new HttpField<MediaType>(
					HttpField.CONTENT_TYPE + ": text/plain; charset=utf-8",
					Converters.MEDIA_TYPE);
			response.setField(media);
		} catch (ParseException e) {
			// Should work...
		}
		ByteBuffer body = ByteBuffer.wrap("Not Found".getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void handleGetForm(HttpRequest request) throws IOException {
		HttpResponse response = request.response().get()
				.setStatus(HttpStatus.OK).setMessageHasBody(true)
				.setField(HttpField.CONTENT_TYPE,
						MediaType.builder().setType("text", "html")
						.setParameter("charset", "utf-8").build());
		String form = "";
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
		        getClass().getResourceAsStream("form.html"), "utf-8"))) {
			form = in.lines().collect(Collectors.joining("\r\n"));
		}
		ByteBuffer body = ByteBuffer.wrap(form.getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	/**
	 * Handle Post request. This is an example of a request with body.
	 * 
	 * @param request
	 * @throws IOException
	 */
	private void handlePostForm(HttpRequest request) throws IOException {
		HttpResponse response = request.response().get();
		FormUrlDecoder fieldDecoder = new FormUrlDecoder();
		while (true) {
			// Header has been decoded already. Provide an out buffer
			// and continue decoding.
			out.clear();
			Decoder.Result<?> decoderResult = null;
			try {
				decoderResult = engine.decode(in, out, false);
			} catch (ProtocolException e) {
				return;
			}
			// Handle decoded body data.
			out.flip();
			fieldDecoder.addData(out);
			if (decoderResult.isOverflow()) {
				// More data left.
				continue;
			}
			if (decoderResult.isUnderflow()) {
				// More data from client expected.
				in.clear();
				channel.read(in);
				in.flip();
				continue;
			}
			break;
		}
		// Got all body data, provide response.
		response.setStatus(HttpStatus.OK).setMessageHasBody(true)
			.setField(HttpField.CONTENT_TYPE,
					MediaType.builder().setType("text", "plain")
					.setParameter("charset", "utf-8").build());
		String data = "First name: " + fieldDecoder.fields().get("firstname")
		        + "\r\n" + "Last name: "
		        + fieldDecoder.fields().get("lastname");
		ByteBuffer body = ByteBuffer.wrap(data.getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void handleEcho(HttpRequest request) throws IOException {
		// Upgrade request (with URL "/echo")?
		if (request.findField(
				HttpField.UPGRADE, Converters.STRING_LIST)
				.map(f -> f.value().containsIgnoreCase("websocket"))
				.orElse(false)) {
			// Do upgrade
			upgradeToWs(request);
			return;
		}
		// If it's not the upgrade request, page was requested.
		HttpResponse response = request.response().get()
				.setStatus(HttpStatus.OK).setMessageHasBody(true);
		response.setField(HttpField.CONTENT_TYPE,
				MediaType.builder().setType("text", "html")
				.setParameter("charset", "utf-8").build());
		String page = "";
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
		        getClass().getResourceAsStream("echo.html"), "utf-8"))) {
			page = in.lines().collect(Collectors.joining("\r\n"));
		}
		ByteBuffer body = ByteBuffer.wrap(page.getBytes("utf-8"));
		sendResponse(response, body, true);
	}

	private void upgradeToWs(HttpRequest request) throws IOException {
		HttpResponse response = request.response().get()
			.setStatus(HttpStatus.SWITCHING_PROTOCOLS)
			.setField(HttpField.UPGRADE, new StringList("websocket"));
		sendResponse(response, null, true);
	}
	
	/**
	 * Send a response without a body.
	 * 
	 * @param response
	 * @throws IOException
	 */
	private void sendResponseWithoutBody(MessageHeader response)
	        throws IOException {
		// This works for both HTTP and WebSocket
		@SuppressWarnings("unchecked")
		ServerEngine<MessageHeader, MessageHeader> genericServer
			= (ServerEngine<MessageHeader, MessageHeader>)engine; 
		// Set header to be encoded (produces no output yet)
		genericServer.encode(response);
		out.clear();
		while (true) {
			// Encode into out buffer.
			Encoder.Result encoderResult = genericServer.encode(out);
			// Send to client.
			out.flip();
			if (out.hasRemaining()) {
				channel.write(out);
				out.clear();
			}
			// More data to be encoded?
			if (encoderResult.isOverflow()) {
				continue;
			}
			// Protocol demands closing the connection?
			if (encoderResult.closeConnection()) {
				channel.close();
			}
			break;
		}
	}

	/**
	 * Send response with body data in buffer "in".
	 * 
	 * @param response
	 * @param in
	 * @param endOfInput
	 * @throws IOException
	 */
	private void sendResponse(MessageHeader response, Buffer in,
	        boolean endOfInput) throws IOException {
		// This works for both HTTP and WebSocket
		@SuppressWarnings("unchecked")
		ServerEngine<MessageHeader, MessageHeader> genericEngine
			= (ServerEngine<MessageHeader, MessageHeader>)engine;
		// Set header to be encoded (produces no output yet)
		genericEngine.encode(response);
		out.clear();
		while (true) {
			// Encode header into out buffer and append body (if sufficient
			// space available).
			Encoder.Result encoderResult = engine.encode(in, out, endOfInput);
			// Send to client.
			out.flip();
			if (out.hasRemaining()) {
				channel.write(out);
				out.clear();
			}
			// More data to be encoded?
			if (encoderResult.isOverflow()) {
				continue;
			}
			// Protocol demands closing the connection?
			if (encoderResult.closeConnection()) {
				channel.close();
			}
			break;
		}
	}

	/**
	 * Handle received WebSocket header.
	 * 
	 * @param header
	 * @throws IOException
	 */
	private void handleWsFrame(WsFrameHeader header) throws IOException {
		if (!(header instanceof WsMessageHeader)) {
			return;
		}
		WsMessageHeader hdr = (WsMessageHeader)header;
		if (!hdr.hasPayload()) {
			return;
		}
		// We expect (short) message to be sent
		CharBuffer out = CharBuffer.allocate(100);
		while (true) {
			// Decode payload into "out" buffer
			Decoder.Result<?> decoderResult = null;
			try {
				decoderResult = engine.decode(in, out, false);
			} catch (ProtocolException e) {
				return;
			}
			// If message is longer than 100 chars, discard beginning.
			if (decoderResult.isOverflow()) {
				out.clear();
				continue;
			}
			// If more data from client is expected, get it.
			if (decoderResult.isUnderflow() && channel.isOpen()) {
				in.clear();
				channel.read(in);
				in.flip();
				continue;
			}
			out.flip();
			break;
		}
		sendResponse(new WsMessageHeader(true, true), out, true);
	}
	
}
