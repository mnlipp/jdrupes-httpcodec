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

package org.jdrupes.httpcodec.plugin;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;

/**
 * The base class for a protocol that the HTTP connection
 * can be upgraded to.
 */
public abstract class UpgradeProvider {

	/**
	 * Checks if the plugin supports the given protocol.
	 * 
	 * @param protocol the protocol in question
	 * @return the result
	 */
	public abstract boolean supportsProtocol(String protocol);

	/**
	 * Add protocol specific information to a request with an `Upgrade`
	 * header field.
	 * 
	 * @param request the request
	 */
	public abstract void augmentInitialRequest(HttpRequest request);

	/**
	 * Check the `101 Switching Protocol` response for any problem
	 * indicators.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ProtocolException the protocol exception
	 */
	public abstract void checkSwitchingResponse(
			HttpRequest request, HttpResponse response)
		throws ProtocolException;
	
	/**
	 * Add any required information to the "switching protocols" response
	 * that is sent as the last package of the HTTP and starts the usage
	 * of the new protocol.
	 * 
	 * @param response the response
	 */
	public abstract void augmentInitialResponse(HttpResponse response);
	
	/**
	 * Creates a new request encoder for the protocol.
	 * 
	 * @param protocol the protocol, which must be supported by this plugin
	 * @return the request encoder
	 */
	public abstract Encoder<?, ?>	createRequestEncoder(String protocol);
	
	/**
	 * Creates a new request decoder for the protocol.
	 * 
	 * @param protocol the protocol, which must be supported by this plugin
	 * @return the request decoder
	 */
	public abstract Decoder<?, ?> createRequestDecoder(String protocol);
	
	/**
	 * Creates a new response encoder for the protocol.
	 * 
	 * @param protocol the protocol, which must be supported by this plugin
	 * @return the response encoder
	 */
	public abstract Encoder<?, ?> createResponseEncoder(String protocol);
	
	/**
	 * Creates a new response decoder for the protocol.
	 * 
	 * @param protocol the protocol, which must be supported by this plugin
	 * @return the response decoder
	 */
	public abstract Decoder<?, ?> createResponseDecoder(String protocol);
	
	
}
