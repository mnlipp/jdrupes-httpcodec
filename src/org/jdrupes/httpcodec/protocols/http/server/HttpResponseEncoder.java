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

package org.jdrupes.httpcodec.protocols.http.server;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.plugin.ProtocolProvider;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;

import org.jdrupes.httpcodec.protocols.http.HttpEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;

/**
 * An encoder for HTTP responses that accepts a header and optional
 * payload data end encodes it into a sequence of
 * {@link Buffer}s.
 * 
 * ![HttpResponseEncoder](httpresponseencoder.svg)
 *
 * Headers
 * -------
 * 
 * The encoder automatically adds a `Date` header as specified
 * in [RFC 7231, Section 7.1.1.2](https://tools.ietf.org/html/rfc7231#section-7.1.1.2).
 * Any existing `Date` header will be overwritten. 
 * 
 * @startuml httpresponseencoder.svg
 * class HttpResponseEncoder {
 * 	+HttpResponseEncoder()
 * 	+void encode(HttpResponse messageHeader)
 * +Result encode(Buffer in, ByteBuffer out, boolean endOfInput)
 * }
 * 
 * class HttpEncoder<T extends HttpMessageHeader> {
 * }
 * 
 * HttpEncoder <|-- HttpResponseEncoder : <<bind>> <T -> HttpResponse>
 * 
 * @enduml
 */
public class HttpResponseEncoder extends HttpEncoder<HttpResponse> {

	private static ServiceLoader<ProtocolProvider> pluginLoader 
		= ServiceLoader.load(ProtocolProvider.class);
	private static Result.Factory resultFactory = new Result.Factory() {
	};
	
	private Map<String,ProtocolProvider> plugins = new HashMap<>();
	private String switchingTo;
	private ProtocolProvider protocolPlugin;

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpEncoder#resultFactory()
	 */
	@Override
	protected Result.Factory resultFactory() {
		return resultFactory;
	}

	/* (non-Javadoc)
	 * @see HttpEncoder#encode(HttpMessageHeader)
	 */
	@Override
	public void encode(HttpResponse messageHeader) {
		if (messageHeader.getStatusCode()
					== HttpStatus.SWITCHING_PROTOCOLS.getStatusCode()) {
			switchingTo = prepareSwitchProtocol(messageHeader);
		}
		
		// Make sure we have an up-to-date Date, RFC 7231 7.1.1.2
		messageHeader.setField(HttpField.DATE, Instant.now());
		
		checkContentLength(messageHeader);
		
		super.encode(messageHeader);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#encode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		Result result = (Result)super.encode(in, out, endOfInput);
		if (switchingTo != null && endOfInput 
				&& !result.isUnderflow() && !result.isOverflow()) {
			// Last invocation of encode
			return resultFactory().newResult(false, false, 
					result.getCloseConnection(), switchingTo, 
					protocolPlugin.createRequestDecoder(switchingTo), 
					protocolPlugin.createResponseEncoder(switchingTo));
		}
		return result;
	}

	private void checkContentLength(HttpResponse messageHeader) {
		// RFC 7230 3.3.2 
		boolean forbidden = messageHeader.fields()
				.containsKey(HttpField.TRANSFER_ENCODING)
				|| messageHeader.getStatusCode() % 100 == 1
				|| messageHeader.getStatusCode() 
						== HttpStatus.NO_CONTENT.getStatusCode()
				|| (messageHeader.getRequest().map(
						r -> r.getMethod().equalsIgnoreCase("CONNECT"))
						.orElse(false)
					&& messageHeader.getStatusCode() % 100 == 2);
		if (messageHeader.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			if (forbidden) {
				messageHeader.removeField(HttpField.CONTENT_LENGTH);
			}
			return;
		}
		// No content length header, maybe we should add one?
		if (forbidden || messageHeader.messageHasBody()) {
			// Not needed or data will determine header
			return;
		}
		// Don't add header if optional
		if (messageHeader.getRequest().map(
				r -> r.getMethod().equalsIgnoreCase("HEAD"))
				.orElse(false)
			|| messageHeader.getStatusCode() 
					== HttpStatus.NOT_MODIFIED.getStatusCode()) {
			return;
		}
		// Add 0 content length
		messageHeader.setField(new HttpField<>(
				HttpField.CONTENT_LENGTH, 0L, Converters.LONG));
	}

	private String prepareSwitchProtocol(HttpResponse response) {
		Optional<String> protocol = response
				.getField(HttpField.UPGRADE, Converters.STRING_LIST)
				.map(l -> l.value().get(0));
		if (!protocol.isPresent()) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			return null;
		}
		synchronized (pluginLoader) {
			if (plugins.containsKey(protocol.get())) {
				protocolPlugin = plugins.get(protocol.get());
			} else {
				protocolPlugin = StreamSupport
						.stream(pluginLoader.spliterator(), false)
						.filter(p -> p.supportsProtocol(protocol.get()))
						.findFirst().get();
				plugins.put(protocol.get(), protocolPlugin);
			}
		}
		if (protocolPlugin == null) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setMessageHasBody(false).clearHeaders();
			return null;
		}
		protocolPlugin.augmentInitialResponse(response);
		if (response.getStatusCode() 
				!= HttpStatus.SWITCHING_PROTOCOLS.getStatusCode()) {
			// Not switching after all
			return null;
		}
		return protocol.get();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Encoder#startMessage(java.io.Writer)
	 */
	@Override
	protected void startMessage(HttpResponse response, Writer writer)
	        throws IOException {
		writer.write(response.getProtocol().toString());
		writer.write(" ");
		writer.write(Integer.toString(response.getStatusCode()));
		writer.write(" ");
		writer.write(response.getReasonPhrase());
		writer.write("\r\n");
	}

	/**
	 * The result from encoding a response. In addition to the usual
	 * codec result, a response encoder may signal to the invoker that the
	 * connection to the requester must be closed and that the protocol has
	 * been switched.
	 */
	public abstract static class Result extends HttpEncoder.Result
		implements Codec.ProtocolSwitchResult {

		private String newProtocol;
		private Decoder<?, ?> newDecoder;
		private Encoder<?> newEncoder;
		
		/**
		 * Returns a new result.
		 * 
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 * @param newProtocol the name of the new protocol if a switch occurred
		 * @param newDecoder the new decoder if a switch occurred
		 * @param newEncoder the new decoder if a switch occurred
		 */
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection, String newProtocol,
		        Decoder<?, ?> newDecoder, Encoder<?> newEncoder) {
			super(overflow, underflow, closeConnection);
			this.newProtocol = newProtocol;
			this.newEncoder = newEncoder;
			this.newDecoder = newDecoder;
		}

		@Override
		public String newProtocol() {
			return newProtocol;
		}
		
		@Override
		public Encoder<?> newEncoder() {
			return newEncoder;
		}
		
		@Override
		public Decoder<?, ?> newDecoder() {
			return newDecoder;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
			        + ((newDecoder == null) ? 0 : newDecoder.hashCode());
			result = prime * result
			        + ((newEncoder == null) ? 0 : newEncoder.hashCode());
			result = prime * result
			        + ((newProtocol == null) ? 0 : newProtocol.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Result other = (Result) obj;
			if (newDecoder == null) {
				if (other.newDecoder != null) {
					return false;
				}
			} else if (!newDecoder.equals(other.newDecoder)) {
				return false;
			}
			if (newEncoder == null) {
				if (other.newEncoder != null) {
					return false;
				}
			} else if (!newEncoder.equals(other.newEncoder)) {
				return false;
			}
			if (newProtocol == null) {
				if (other.newProtocol != null) {
					return false;
				}
			} else if (!newProtocol.equals(other.newProtocol)) {
				return false;
			}
			return true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("HttpResponseEncoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(getCloseConnection());
			builder.append(", ");
			if (newProtocol != null) {
				builder.append("newProtocol=");
				builder.append(newProtocol);
				builder.append(", ");
			}
			if (newDecoder != null) {
				builder.append("newDecoder=");
				builder.append(newDecoder);
				builder.append(", ");
			}
			if (newEncoder != null) {
				builder.append("newEncoder=");
				builder.append(newEncoder);
			}
			builder.append("]");
			return builder.toString();
		}

		/**
		 * A factory for creating new Results.
		 */
		protected static class Factory extends HttpEncoder.Result.Factory {
			
			/**
			 * Create a new result.
			 * 
			 * @param overflow
			 *            {@code true} if the data didn't fit in the out buffer
			 * @param underflow
			 *            {@code true} if more data is expected
			 * @param closeConnection
			 *            {@code true} if the connection should be closed
			 * @param newProtocol the name of the new protocol if a switch occurred
			 * @param newDecoder the new decoder if a switch occurred
			 * @param newEncoder the new decoder if a switch occurred
			 * @return the result
			 */
			public Result newResult(boolean overflow, boolean underflow,
			        boolean closeConnection, String newProtocol,
			        Decoder<?, ?> newDecoder, Encoder<?> newEncoder) {
				return new Result(overflow, underflow, closeConnection,
						newProtocol, newDecoder, newEncoder) {
				};
			}

			/**
			 * Create a new (preliminary) result. This is invoked by the
			 * base class. We cannot supply the missing information yet.
			 * If necessary the result will be modified in 
			 * {@link HttpResponseEncoder#encode(Buffer, ByteBuffer, boolean)}.
			 * 
			 * @param overflow
			 *            {@code true} if the data didn't fit in the out buffer
			 * @param underflow
			 *            {@code true} if more data is expected
			 * @param closeConnection
			 *            {@code true} if the connection should be closed
			 * @return the result
			 */
			@Override
			public Result newResult(
			        boolean overflow, boolean underflow,
			        boolean closeConnection) {
				return newResult(overflow, underflow, closeConnection,
						null, null, null);
			}
			
		}		

	}
}
