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

package org.jdrupes.httpcodec.protocols.http.server;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.plugin.UpgradeProvider;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpEncoder;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;

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
 * ### Date ###
 * 
 * The encoder automatically adds a `Date` header as specified
 * in [RFC 7231, Section 7.1.1.2](https://tools.ietf.org/html/rfc7231#section-7.1.1.2).
 * Any existing `Date` header will be overwritten. 
 * 
 * ### Expires ###
 * 
 * If the protocol is HTTP 1.0 and the response includes a `Cache-Control`
 * header field with a `max-age` directive, an `Expires` header field
 * with the same information is generated (see
 * [RFC 7234, Section 5.3](https://tools.ietf.org/html/rfc7234#section-5.3)).
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
public class HttpResponseEncoder extends HttpEncoder<HttpResponse, HttpRequest>
	implements Encoder<HttpResponse, HttpRequest> {

	private static Result.Factory resultFactory = new Result.Factory() {
	};
	
	private String switchingTo;
	private UpgradeProvider protocolPlugin;

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpEncoder#resultFactory()
	 */
	@Override
	protected Result.Factory resultFactory() {
		return resultFactory;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Encoder#encoding()
	 */
	@Override
	public Class<HttpResponse> encoding() {
		return HttpResponse.class;
	}

	/* (non-Javadoc)
	 * @see HttpEncoder#encode(HttpMessageHeader)
	 */
	@Override
	public void encode(HttpResponse messageHeader) {
		if (messageHeader.statusCode()
					== HttpStatus.SWITCHING_PROTOCOLS.statusCode()) {
			switchingTo = prepareSwitchProtocol(messageHeader);
		}
		
		// Make sure we have an up-to-date Date, RFC 7231 7.1.1.2
		messageHeader.setField(HttpField.DATE, Instant.now());

		// ensure backward compatibility
		if (messageHeader.protocol().compareTo(HttpProtocol.HTTP_1_1) < 0) {
			// Create Expires
			Optional<HttpField<List<Directive>>> cacheCtrl = messageHeader
					.findField(HttpField.CACHE_CONTROL, Converters.DIRECTIVE_LIST);
			if (cacheCtrl.isPresent() 
					&& !messageHeader.fields().containsKey(HttpField.EXPIRES)) {
				Optional<Long> maxAge = cacheCtrl.get().value().stream()
						.filter(d -> "max-age".equalsIgnoreCase(d.name()))
						.map(d -> d.value()).map(v -> Long.parseLong(v.get()))
						.findFirst();
				if (maxAge.isPresent()) {
					messageHeader.setField(HttpField.EXPIRES, 
							Instant.now().plusSeconds(maxAge.get()));
				}
			}
		}

		// Check the content length rules
		checkContentLength(messageHeader);
		
		// Finally encode
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
					result.closeConnection(), switchingTo, 
					protocolPlugin.createRequestDecoder(switchingTo), 
					protocolPlugin.createResponseEncoder(switchingTo));
		}
		return result;
	}

	private void checkContentLength(HttpResponse messageHeader) {
		// RFC 7230 3.3.2 
		boolean forbidden = messageHeader.fields()
				.containsKey(HttpField.TRANSFER_ENCODING)
				|| messageHeader.statusCode() % 100 == 1
				|| messageHeader.statusCode() 
						== HttpStatus.NO_CONTENT.statusCode()
				|| (messageHeader.request().map(
						r -> r.method().equalsIgnoreCase("CONNECT"))
						.orElse(false)
					&& messageHeader.statusCode() % 100 == 2);
		if (messageHeader.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			if (forbidden) {
				messageHeader.removeField(HttpField.CONTENT_LENGTH);
			}
			return;
		}
		// No content length header, maybe we should add one?
		if (forbidden || messageHeader.hasPayload()) {
			// Not needed or data will determine header
			return;
		}
		// Don't add header if optional
		if (messageHeader.request().map(
				r -> r.method().equalsIgnoreCase("HEAD"))
				.orElse(false)
			|| messageHeader.statusCode() 
					== HttpStatus.NOT_MODIFIED.statusCode()) {
			return;
		}
		// Add 0 content length
		messageHeader.setField(new HttpField<>(
				HttpField.CONTENT_LENGTH, 0L, Converters.LONG));
	}

	private String prepareSwitchProtocol(HttpResponse response) {
		Optional<String> protocol = response
				.findField(HttpField.UPGRADE, Converters.STRING_LIST)
				.map(l -> l.value().get(0));
		if (!protocol.isPresent()) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setHasPayload(false).clearHeaders();
			return null;
		}
		// Load every time to support dynamic deployment of additional
		// services in an OSGi environment.
		protocolPlugin = StreamSupport.stream(
				ServiceLoader.load(UpgradeProvider.class)
				.spliterator(), false)
				.filter(p -> p.supportsProtocol(protocol.get()))
				.findFirst().orElse(null);
		if (protocolPlugin == null) {
			response.setStatus(HttpStatus.BAD_REQUEST)
				.setHasPayload(false).clearHeaders();
			return null;
		}
		protocolPlugin.augmentInitialResponse(response);
		if (response.statusCode() 
				!= HttpStatus.SWITCHING_PROTOCOLS.statusCode()) {
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
		writer.write(response.protocol().toString());
		writer.write(" ");
		writer.write(Integer.toString(response.statusCode()));
		writer.write(" ");
		writer.write(response.reasonPhrase());
		writer.write("\r\n");
	}

    @Override
    protected boolean forceCloseAfterBody() {
        return messageHeader.protocol()
                .compareTo(HttpProtocol.HTTP_1_0) <= 0;
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
		private Encoder<?, ?> newEncoder;
		
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
		        Decoder<?, ?> newDecoder, Encoder<?, ?> newEncoder) {
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
		public Encoder<?, ?> newEncoder() {
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
			if (!(obj instanceof Result)) {
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
			builder.append(closeConnection());
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
			        Decoder<?, ?> newDecoder, Encoder<?, ?> newEncoder) {
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
			public Result newResult(boolean overflow, boolean underflow,
			        boolean closeConnection) {
				return newResult(overflow, underflow, closeConnection,
						null, null, null);
			}
			
		}		

	}
}
