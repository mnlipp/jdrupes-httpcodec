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

package org.jdrupes.httpcodec.protocols.websocket;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
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
public class WsProtocolProvider extends ProtocolProvider {

	/* (non-Javadoc)
	 * @see ProtocolProvider#supportsProtocol(java.lang.String)
	 */
	@Override
	public boolean supportsProtocol(String protocol) {
		return protocol.equalsIgnoreCase("websocket");
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#augmentInitialResponse
	 */
	@Override
	public void augmentInitialResponse(HttpResponse response) {
		Optional<String> wsKey = response.getRequest()
			.flatMap(r -> r.getStringValue("Sec-WebSocket-Key"));
		if (!wsKey.isPresent()) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			return;
		}
		// RFC 6455 4.1
		if(response.getRequest().flatMap(r -> r.getField(
				"Sec-WebSocket-Version", Converters.LONG))
				.map(HttpField<Long>::value).orElse(-1L) != 13) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
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
				.setMessageHasBody(false).clearHeaders();
			return;
		}
 	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createRequestEncoder()
	 */
	@Override
	public Encoder<?> createRequestEncoder(String protocol) {
		return null;
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
	public Encoder<?> createResponseEncoder(String protocol) {
		return new WsEncoder(false);
	}

	/* (non-Javadoc)
	 * @see ProtocolProvider#createResponseDecoder()
	 */
	@Override
	public ResponseDecoder<?, ?> createResponseDecoder(String protocol) {
		return null;
	}

	
	
}
