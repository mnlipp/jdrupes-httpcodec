/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016, 2017  Michael N. Lipp
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

package org.jdrupes.httpcodec.protocols.websocket;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.plugin.UpgradeProvider;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;

/**
 * A protocol provider for the WebSocket protocol.
 * 
 * ![WsProtocolProvider](WsProtocolProvider.svg)
 * 
 * 
 * 
 * @startuml WsProtocolProvider.svg
 * 
 * class ProtocolProvider
 * 
 * class WsProtocolProvider {
 * 	+boolean supportsProtocol(String protocol)
 * 	+void augmentInitialResponse(HttpResponse response)
 * 	+Encoder<?> createRequestEncoder(String protocol)
 * 	+Decoder<?,?> createRequestDecoder(String protocol)
 * 	+Encoder<?> createResponseEncoder(String protocol)
 * 	+ResponseDecoder<?,?> createResponseDecoder(String protocol)
 * }
 * 
 * ProtocolProvider <|-- WsProtocolProvider
 * 
 * @enduml
 * 
 */
public class WsProtocolProvider extends UpgradeProvider {

	/* (non-Javadoc)
	 * @see ProtocolProvider#supportsProtocol(java.lang.String)
	 */
	@Override
	public boolean supportsProtocol(String protocol) {
		return protocol.equalsIgnoreCase("websocket");
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.plugin.UpgradeProvider#augmentInitialRequest
	 */
	@Override
	public void augmentInitialRequest(HttpRequest request) {
		Optional<HttpField<Long>> version = request.findField(
				"Sec-WebSocket-Version", Converters.LONG);
		if (version.isPresent() && version.get().value() != 13) {
			// Sombody else's job...
			return;
		}
		request.setField(new HttpField<>(
				"Sec-WebSocket-Version", 13L, Converters.LONG));
		if (!request.findField("Sec-WebSocket-Key", Converters.UNQUOTED_STRING)
				.isPresent()) {
			byte[] randomBytes = new byte[16];
			new Random().nextBytes(randomBytes);
			request.setField(new HttpField<String>("Sec-WebSocket-Key",
					Base64.getEncoder().encodeToString(randomBytes), 
					Converters.UNQUOTED_STRING));
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.plugin.UpgradeProvider#checkSwitchingResponse
	 */
	@Override
	public void checkSwitchingResponse(HttpRequest request, 
			HttpResponse response) throws ProtocolException {
		Optional<String> accept = response.findStringValue(
				"Sec-WebSocket-Accept");
		if (!accept.isPresent()) {
			throw new ProtocolException(
					"Header field Sec-WebSocket-Accept is missing.");
		}
		String wsKey = request.findStringValue("Sec-WebSocket-Key").get();
		String magic = wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			byte[] sha1 = crypt.digest(magic.getBytes("ascii"));
			String expected = Base64.getEncoder().encodeToString(sha1);
			if (!accept.get().equals(expected)) {
				throw new ProtocolException(
						"Invalid value in Sec-WebSocket-Accept header field.");
			}
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			throw new ProtocolException(e);
		}
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#augmentInitialResponse
	 */
	@Override
	public void augmentInitialResponse(HttpResponse response) {
		Optional<String> wsKey = response.request()
			.flatMap(r -> r.findStringValue("Sec-WebSocket-Key"));
		if (!wsKey.isPresent()) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setHasPayload(false).clearHeaders();
			return;
		}
		// RFC 6455 4.1
		if(response.request().flatMap(r -> r.findField(
				"Sec-WebSocket-Version", Converters.LONG))
				.map(HttpField<Long>::value).orElse(-1L) != 13) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setHasPayload(false).clearHeaders();
			// RFC 6455 4.4
			response.setField(new HttpField<>(
					"Sec-WebSocket-Version", 13L, Converters.LONG));
			return;
			
		}
		String magic = wsKey.get() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			byte[] sha1 = crypt.digest(magic.getBytes("ascii"));
			String accept = Base64.getEncoder().encodeToString(sha1);
			response.setField(new HttpField<String>(
					"Sec-WebSocket-Accept", accept, Converters.UNQUOTED_STRING));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.setHasPayload(false).clearHeaders();
			return;
		}
 	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestEncoder()
	 */
	@Override
	public Encoder<?, ?> createRequestEncoder(String protocol) {
		return new WsEncoder(true);
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestDecoder()
	 */
	@Override
	public Decoder<?, ?> createRequestDecoder(String protocol) {
		return new WsDecoder();
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseEncoder()
	 */
	@Override
	public Encoder<?, ?> createResponseEncoder(String protocol) {
		return new WsEncoder(false);
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseDecoder()
	 */
	@Override
	public Decoder<?, ?> createResponseDecoder(String protocol) {
		return new WsDecoder();
	}

}
