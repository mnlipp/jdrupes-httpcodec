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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ResponseDecoder;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;
import org.jdrupes.httpcodec.protocols.http.HttpDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.fields.HttpDateTimeField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpIntField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;

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
	implements ResponseDecoder<HttpResponse, HttpRequest> {

	// RFC 7230 3.1.2
	private static final Pattern responseLinePatter = Pattern
	        .compile("^(" + HTTP_VERSION + ")" + SP + "([1-9][0-9][0-9])"
	                + SP + "(.*)$");

	private final Result.Factory resultFactory	= new Result.Factory(this);
	private String requestMethod = "";
	private boolean reportHeaderReceived = false;
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpDecoder#resultFactory()
	 */
	@Override
	protected Result.Factory resultFactory() {
		return resultFactory;
	}

	/**
	 * Starts decoding a new response to a given request.
	 * Specifying the request is necessary because the existence of a body
	 * cannot be derived by looking at the header only. It depends on the kind
	 * of request made. Must be called before the response is decoded.
	 * 
	 * @param request
	 *            the request
	 */
	public void decodeResponseTo(HttpRequest request) {
		this.requestMethod = request.getMethod();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Decoder#decode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws HttpProtocolException {
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
			        HttpStatus.BAD_REQUEST.getStatusCode(),
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
	        throws HttpProtocolException {
		reportHeaderReceived = true;
		// Adjust Retry-After
		HttpField<?> retryAfter = message.fields().get(HttpField.RETRY_AFTER);
		if (retryAfter != null && (retryAfter instanceof HttpIntField)) {
			Instant base = message
				.getField(HttpDateTimeField.class, HttpField.DATE)
				.map(HttpDateTimeField::getValue).orElse(Instant.now());
			message.setField(new HttpDateTimeField(HttpField.RETRY_AFTER,
					base.plusSeconds(((HttpIntField)retryAfter).getValue())));
		}
		// RFC 7230 3.3.3 (1. & 2.)
		int statusCode = message.getStatusCode();
		if (requestMethod.equalsIgnoreCase("HEAD")
		        || (statusCode % 100) == 1
		        || statusCode == 204
		        || statusCode == 304
		        || (requestMethod.equalsIgnoreCase("CONNECT")
		                && (statusCode % 100 == 2))) {
			return BodyMode.NO_BODY;
		}
		HttpStringListField transEncs = message.getField(
		        HttpStringListField.class, HttpField.TRANSFER_ENCODING)
				.orElse(null);
		// RFC 7230 3.3.3 (3.)
		if (transEncs != null) {
			if (transEncs.get(transEncs.size() - 1)
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
		private ResponseDecoder<?, ?> newDecoder;
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
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 * @param newProtocol the name of the new protocol if a switch occurred
		 * @param newDecoder the new decoder if a switch occurred
		 * @param newEncoder the new decoder if a switch occurred
		 */
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection, boolean headerCompleted,
		        String newProtocol, ResponseDecoder<?, ?> newDecoder, 
		        Encoder<?> newEncoder) {
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
		public ResponseDecoder<?, ?> newDecoder() {
			return newDecoder;
		}
		
		@Override
		public Encoder<?> newEncoder() {
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
			builder.append("HttpResponseDecoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(getCloseConnection());
			builder.append(", headerCompleted=");
			builder.append(isHeaderCompleted());
			builder.append(", ");
			if (getResponse() != null) {
				builder.append("response=");
				builder.append(getResponse());
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

			/* (non-Javadoc)
			 * @see HttpDecoder.Result.Factory#newResult(boolean, boolean, boolean)
			 */
			@Override
			protected Result newResult(
			        boolean overflow, boolean underflow) {
				Result result = new Result(overflow, underflow, decoder.isClosed(),
						decoder.reportHeaderReceived, null, null, null) {
				};
				decoder.reportHeaderReceived = false;
				return result;
			}
		}
	}
	
}
