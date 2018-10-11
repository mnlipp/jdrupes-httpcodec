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

package org.jdrupes.httpcodec.protocols.http.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.plugin.UpgradeProvider;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.StringList;

/**
 * A decoder for HTTP reponses that accepts data from a sequence of
 * {@link ByteBuffer}s and decodes them into {@link HttpResponse}s
 * and their (optional) payload.
 * 
 * ![HttpResponseDecoder](httpresponsedecoder.svg)
 * 
 * Headers
 * -------
 * 
 * The decoder converts a `Retry-After` header with a delay value to
 * a header with a date by adding the span to the time in the `Date` header.
 *  
 * 
 * @see "[RFC 7231, Section 7.1.3](https://tools.ietf.org/html/rfc7231#section-7.1.3)"
 * 
 * @startuml httpresponsedecoder.svg
 * class HttpResponseDecoder {
 * 	+HttpRequestDecoder()
 * 	+Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
 * }
 * 
 * class HttpDecoder<T extends HttpMessageHeader, R extends HttpMessageHeader> {
 * }
 * 
 * HttpDecoder <|-- HttpResponseDecoder: <<bind>> <T -> HttpResponse, R -> HttpRequest>  
 * @enduml
 */
public class HttpResponseDecoder 
	extends HttpDecoder<HttpResponse, HttpRequest>
	implements Decoder<HttpResponse, HttpRequest> {

	// RFC 7230 3.1.2
	private static final Pattern responseLinePatter = Pattern
	        .compile("^(" + HTTP_VERSION + ")" + SP + "([1-9][0-9][0-9])"
	                + SP + "(.*)$");

	private final Result.Factory resultFactory	= new Result.Factory(this);
	private boolean reportHeaderReceived = false;
	private String switchingTo;
	private UpgradeProvider protocolPlugin;
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpDecoder#resultFactory()
	 */
	@Override
	protected Result.Factory resultFactory() {
		return resultFactory;
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Decoder#decoding()
	 */
	@Override
	public Class<HttpResponse> decoding() {
		return HttpResponse.class;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Decoder#decode
	 */
	@Override
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		return (Result)super.decode(in, out, endOfInput);
	}

	/**
	 * Checks whether the first line of a message is a valid response.
	 * If so, create a new response message object with basic information, else
	 * throw an exception.
	 * <P>
	 * Called by the base class when a first line is received.
	 * 
	 * @param startLine the first line
	 * @throws HttpProtocolException if the line is not a correct request line
	 */
	@Override
	protected HttpResponse newMessage(String startLine)
	        throws HttpProtocolException {
		Matcher responseMatcher = responseLinePatter.matcher(startLine);
		if (!responseMatcher.matches()) {
			throw new HttpProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.statusCode(),
			        "Illegal request line");
		}
		String httpVersion = responseMatcher.group(1);
		int statusCode = Integer.parseInt(responseMatcher.group(2));
		String reasonPhrase = responseMatcher.group(3);
		boolean found = false;
		for (HttpProtocol v : HttpProtocol.values()) {
			if (v.toString().equals(httpVersion)) {
				protocolVersion = v;
				found = true;
			}
		}
		if (!found) {
			throw new HttpProtocolException(HttpProtocol.HTTP_1_1,
			        HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		}
		return new HttpResponse(protocolVersion, statusCode, reasonPhrase,
		        false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.
	 * HttpMessage)
	 */
	@Override
	protected BodyMode headerReceived(HttpResponse message)
	        throws ProtocolException {
		reportHeaderReceived = true;
		// Adjust Retry-After
		HttpField<?> hdr = message.fields().get(HttpField.RETRY_AFTER);
		if (hdr != null && String.class
				.isAssignableFrom(hdr.value().getClass())) {
			String value = (String)hdr.value();
			if (Character.isDigit(value.charAt(0))) {
				Instant base = message.findField(
						HttpField.DATE, Converters.DATE_TIME)
						.map(HttpField<Instant>::value).orElse(Instant.now());
				message.setField(new HttpField<>(HttpField.RETRY_AFTER,
						base.plusSeconds(Long.parseLong(value)),
						Converters.DATE_TIME));
			}
		}
		// Prepare protocol switch
		if (message.statusCode()
				== HttpStatus.SWITCHING_PROTOCOLS.statusCode()) {
			Optional<String> protocol = message.findField(
					HttpField.UPGRADE, Converters.STRING_LIST)
					.map(l -> l.value().get(0));
			if (!protocol.isPresent()) {
				throw new ProtocolException(
						"Upgrade header field missing in response");
			}
			switchingTo = protocol.get();
			// Load every time to support dynamic deployment of additional
			// services in an OSGi environment.
			protocolPlugin = StreamSupport.stream(
					ServiceLoader.load(UpgradeProvider.class)
					.spliterator(), false)
					.filter(p -> p.supportsProtocol(protocol.get()))
					.findFirst().orElseThrow(() -> new ProtocolException(
							"Upgrade to protocol " + protocol.get() 
							+ " not supported."));
			switchingTo = protocol.get();
			if (peerEncoder != null && peerEncoder.header().isPresent()) {
				protocolPlugin.checkSwitchingResponse(
						peerEncoder.header().get(), message);
			}
		}
		// RFC 7230 3.3.3 (1. & 2.)
		int statusCode = message.statusCode();
		HttpRequest request = Optional.ofNullable(peerEncoder)
				.flatMap(Encoder::header).orElse(null);
		if (request != null && request.method().equalsIgnoreCase("HEAD")
		        || (statusCode % 100) == 1
		        || statusCode == 204
		        || statusCode == 304
		        || (request != null 
		        	&& request.method().equalsIgnoreCase("CONNECT")
		            && (statusCode % 100 == 2))) {
			return BodyMode.NO_BODY;
		}
		Optional<HttpField<StringList>> transEncs = message.findField(
		        HttpField.TRANSFER_ENCODING, Converters.STRING_LIST);
		// RFC 7230 3.3.3 (3.)
		if (transEncs.isPresent()) {
			StringList values = transEncs.get().value(); 
			if (values.get(values.size() - 1)
			        .equalsIgnoreCase(TransferCoding.CHUNKED.toString())) {
				return BodyMode.CHUNKED;
			} else {
				return BodyMode.UNTIL_CLOSE;
			}
		}
		// RFC 7230 3.3.3 (5.)
		if (message.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			return BodyMode.LENGTH;
		}
		// RFC 7230 3.3.3 (7.)
		return BodyMode.UNTIL_CLOSE;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpDecoder#messageComplete
	 */
	@Override
	protected Decoder.Result<HttpRequest> messageComplete(
			Decoder.Result<HttpRequest> result) {
		if (switchingTo != null) {
			return resultFactory().newResult(false, false, 
					result.closeConnection(), result.isHeaderCompleted(),
					switchingTo, 
					protocolPlugin.createRequestEncoder(switchingTo), 
					protocolPlugin.createResponseDecoder(switchingTo));
		}
		return super.messageComplete(result);
	}

	/**
	 * The result from encoding a response. In addition to the usual
	 * codec result, a result decoder may signal to the invoker that the
	 * connection to the responder must be closed.
	 * 
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 */
	public abstract static class Result extends HttpDecoder.Result<HttpRequest>
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
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 * @param newProtocol the name of the new protocol if a switch occurred
		 * @param newEncoder the new decoder if a switch occurred
		 * @param newDecoder the new decoder if a switch occurred
		 */
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection, boolean headerCompleted,
		        String newProtocol, Encoder<?, ?> newEncoder, 
		        Decoder<?, ?> newDecoder) {
			super(overflow, underflow, closeConnection, headerCompleted,
					null, false);
			this.newProtocol = newProtocol;
			this.newDecoder = newDecoder;
			this.newEncoder = newEncoder;
		}

		@Override
		public String newProtocol() {
			return newProtocol;
		}
		
		@Override
		public Decoder<?, ?> newDecoder() {
			return newDecoder;
		}
		
		@Override
		public Encoder<?, ?> newEncoder() {
			return newEncoder;
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
			builder.append("HttpResponseDecoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(closeConnection());
			builder.append(", headerCompleted=");
			builder.append(isHeaderCompleted());
			builder.append(", ");
			if (response() != null) {
				builder.append("response=");
				builder.append(response());
				builder.append(", ");
			}
			builder.append("responseOnly=");
			builder.append(isResponseOnly());
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
		 * The Factory for (extended) results.
		 */
		protected static class Factory
			extends HttpDecoder.Result.Factory<HttpRequest> {

			private HttpResponseDecoder decoder;

			/**
			 * Creates a new factory for the given decoder. 
			 * 
			 * @param decoder the decoder
			 */
			protected Factory(HttpResponseDecoder decoder) {
				super();
				this.decoder = decoder;
			}

			public Result newResult(boolean overflow, boolean underflow,
			        boolean closeConnection, boolean headerCompleted,
			        String newProtocol, Encoder<?, ?> newEncoder, 
			        Decoder<?, ?> newDecoder) {
				return new Result(overflow, underflow, closeConnection,
						headerCompleted, newProtocol, newEncoder, newDecoder) {
				};
			}
			
			/**
			 * Create a new (preliminary) result. This is invoked by the
			 * base class. We cannot supply the missing information yet.
			 * If necessary the result will be modified in 
			 * {@link HttpResponseDecoder#decode(ByteBuffer, Buffer, boolean)}.
			 **/
			@Override
			protected Result newResult(
			        boolean overflow, boolean underflow) {
				Result result = newResult(overflow, underflow, decoder.isClosed(),
						decoder.reportHeaderReceived, null, null, null);
				decoder.reportHeaderReceived = false;
				return result;
			}
		}
	}
	
}
