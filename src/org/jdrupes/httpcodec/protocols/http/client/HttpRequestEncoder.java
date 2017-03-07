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

package org.jdrupes.httpcodec.protocols.http.client;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;

import org.jdrupes.httpcodec.protocols.http.HttpEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;

/**
 * An encoder for HTTP requests that accepts a header and optional
 * payload data end encodes it into a sequence of
 * {@link Buffer}s.
 * 
 * ![HttpRequestEncoder](httprequestencoder.svg)
 * 
 * @startuml httprequestencoder.svg
 * class HttpRequestEncoder {
 * 	+HttpRequestEncoder(Engine engine)
 * }
 * 
 * class HttpEncoder<T extends HttpMessageHeader> {
 * }
 * 
 * HttpEncoder <|-- HttpRequestEncoder : <<bind>> <T -> HttpRequest>
 *
 */
public class HttpRequestEncoder extends HttpEncoder<HttpRequest> {

	private static Result.Factory resultFactory = new Result.Factory() {
	};
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpEncoder#resultFactory()
	 */
	@Override
	protected Result.Factory resultFactory() {
		return resultFactory;
	}

	/* (non-Javadoc)
	 * @see Encoder#startMessage(MessageHeader, java.io.Writer)
	 */
	@Override
	protected void startMessage(HttpRequest messageHeader, Writer writer)
	        throws IOException {
		writer.write(messageHeader.getMethod());
		writer.write(" ");
		writer.write(messageHeader.getRequestUri().toString());
		writer.write(" ");
		writer.write(messageHeader.getProtocol().toString());
		writer.write("\r\n");
	}
	
	/**
	 * Results from {@link HttpRequestEncoder} add no additional
	 * information to 
	 * {@link org.jdrupes.httpcodec.protocols.http.HttpEncoder.Result}. This
	 * class just provides a factory for creating concrete results.
	 */
	public static class Result extends HttpEncoder.Result {

		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection) {
			super(overflow, underflow, closeConnection);
		}
	
		/**
		 * A concrete factory for creating new Results.
		 */
		protected static class Factory extends HttpEncoder.Result.Factory {
		}		
	}
}
