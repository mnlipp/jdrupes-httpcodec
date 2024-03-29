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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.ProtocolException;
import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;
import org.jdrupes.httpcodec.protocols.http.HttpDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.StringList;

/**
 * A decoder for HTTP requests. It accepts data from a sequence of
 * {@link ByteBuffer}s and decodes them into {@link HttpRequest}s
 * and their (optional) payload.
 * 
 * ![HttpRequestDecoder](httprequestdecoder.svg)
 * 
 * @startuml httprequestdecoder.svg
 * class HttpRequestDecoder {
 * 	+Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
 * }
 * 
 * class HttpDecoder<T extends HttpMessageHeader, R extends HttpMessageHeader> {
 * }
 * 
 * HttpDecoder <|-- HttpRequestDecoder: <<bind>> <T -> HttpRequest, R -> HttpResponse>  
 * @enduml
 */
public class HttpRequestDecoder 
	extends HttpDecoder<HttpRequest, HttpResponse> {

	// RFC 7230 3.1.1
	private static final Pattern requestLinePatter = Pattern
	        .compile("^(" + TOKEN_REGEXP + ")" + SP + "([^ \\t]+)" + SP + "("
	                + HTTP_VERSION + ")$");
	private final Result.Factory resultFactory	= new Result.Factory(this);
	
	private boolean reportHeaderReceived = false;

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
	public Class<HttpRequest> decoding() {
		return HttpRequest.class;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#decode(java.nio.ByteBuffer)
	 */
	@Override
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput) {
		try {
			return (Result)super.decode(in, out, endOfInput);
		} catch (HttpProtocolException e) {
			HttpResponse response = new HttpResponse(e.httpVersion(), 
					e.statusCode(), e.reasonPhrase(), false);
			response.setField(new HttpField<>(HttpField.CONNECTION,
					new StringList("close"), Converters.STRING_LIST));
			return resultFactory().newResult(
					false, false, false, response, true);
		} catch (ProtocolException e) {
			HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1, 
					HttpStatus.INTERNAL_SERVER_ERROR.statusCode(),
					e.getMessage(), false);
			response.setField(new HttpField<>(HttpField.CONNECTION,
					new StringList("close"), Converters.STRING_LIST));
			return resultFactory().newResult(false, false, false, response, true);
		}
	}

	/**
	 * Checks whether the first line of a message is a valid request.
	 * If so, create a new request message object with basic information, else
	 * throw an exception.
	 * <P>
	 * Called by the base class when a first line is received.
	 * 
	 * @param startLine the first line
	 * @throws HttpProtocolException if the line is not a correct request line
	 */
	@Override
	protected HttpRequest newMessage(String startLine)
	        throws HttpProtocolException {
		Matcher requestMatcher = requestLinePatter.matcher(startLine);
		if (!requestMatcher.matches()) {
			// RFC 7230 3.1.1
			throw new HttpProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.statusCode(),
			        "Illegal request line");
		}
		String httpVersion = requestMatcher.group(3);
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
		String method = requestMatcher.group(1);
		String uriGroup = requestMatcher.group(2);
		URI uri = null;
		if ("*".equals(uriGroup)) {
			uri = HttpRequest.ASTERISK_REQUEST;
		} else {
			try {
				uri = new URI(uriGroup);
			} catch (URISyntaxException e) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.statusCode(), e.getMessage());
			}
		}
		HttpRequest request = new HttpRequest(
				method, uri, protocolVersion, false);
		HttpResponse response = (new HttpResponse(protocolVersion,
				HttpStatus.NOT_IMPLEMENTED, false)).setRequest(request); 
		return request.setResponse(response);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.HttpMessage)
	 */
	@Override
	protected BodyMode headerReceived(HttpRequest message) 
			throws HttpProtocolException {
		reportHeaderReceived = true;
		// Handle field of special interest
		Optional<HttpField<String>> host = message.findField(
				HttpField.HOST, Converters.STRING);
		if (host.isPresent()) {
			try {
				URI parsed = new URI("http://" + host.get().value());
				message.setHostAndPort(parsed.getHost(), parsed.getPort());
			} catch (URISyntaxException e) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.statusCode(),
				        "Invalid Host port.");
			}
		} else {
			// RFC 7230 5.4.
			if (message.protocol().compareTo(HttpProtocol.HTTP_1_1) >= 0) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.statusCode(),
				        "HTTP 1.1 request must have a Host field.");
			}
		}
		if (message.findField(HttpField.CONNECTION, 
				Converters.STRING_LIST).map(h -> h.value())
				.map(f -> f.containsIgnoreCase("close")).orElse(false)) {
			// RFC 7230 6.6.
			message.response().get().setField(new HttpField<>(
			        HttpField.CONNECTION, new StringList("close"), 
			        Converters.STRING_LIST));
		}

		// Find out about body
		Optional<HttpField<StringList>> transEncs = message.findField(
		        HttpField.TRANSFER_ENCODING, Converters.STRING_LIST);
		if (transEncs.isPresent()) {
			List<String> tecs = transEncs.get().value();
			// RFC 7230 3.3.1, currently only chunked is supported
			if (tecs.stream().anyMatch(s -> !s.equalsIgnoreCase(
							TransferCoding.CHUNKED.toString()))) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.NOT_IMPLEMENTED);
			}
			// RFC 7230 3.3.3 (3.)
			if (tecs.size() > 0	&& tecs.get(tecs.size() - 1)
					.equalsIgnoreCase(TransferCoding.CHUNKED.toString())) {
				return BodyMode.CHUNKED;
			} else {
				throw new HttpProtocolException(protocolVersion,
						HttpStatus.BAD_REQUEST);
			}
		}
		// RFC 7230 3.3.3 (5.)
		if (message.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			return BodyMode.LENGTH;
		}
		// RFC 7230 3.3.3 (6.)
		return BodyMode.NO_BODY;
	}

	/**
	 * Results from {@link HttpRequestDecoder} add no additional
	 * information to 
	 * {@link org.jdrupes.httpcodec.protocols.http.HttpDecoder.Result}. This
	 * class just provides a factory for creating concrete results.
	 * 
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 */
	public abstract static class Result 
		extends HttpDecoder.Result<HttpResponse> {

		/**
		 * Creates a new result.
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param headerCompleted
		 *            {@code true} if the header has completely been decoded
		 * @param response
		 *            a response to send due to an error
		 * @param responseOnly
		 *            if the result includes a response this flag indicates that
		 *            no further processing besides sending the response is
		 *            required
		 */
		public Result(boolean overflow, boolean underflow,
		        boolean headerCompleted, HttpResponse response, 
		        boolean responseOnly) {
			super(overflow, underflow, false, headerCompleted, response,
					responseOnly);
		}
		
		protected static class Factory 
			extends HttpDecoder.Result.Factory<HttpResponse> {
			
			private HttpRequestDecoder decoder;

			/**
			 * Creates a new factory for the given decoder. 
			 * 
			 * @param decoder the decoder
			 */
			protected Factory(HttpRequestDecoder decoder) {
				super();
				this.decoder = decoder;
			}
			
			/**
			 * Create a new result.
			 * 
			 * @param overflow
			 *            {@code true} if the data didn't fit in the out buffer
			 * @param underflow
			 *            {@code true} if more data is expected
			 * @param headerCompleted
			 *            {@code true} if the header has completely been decoded
			 * @param response
			 *            a response to send due to an error
			 * @param responseOnly
			 *            if the result includes a response this flag indicates
			 *            that no further processing besides sending the
			 *            response is required
			 * @return the result
			 */
			public Result newResult(boolean overflow, boolean underflow, 
					boolean headerCompleted, HttpResponse response, 
					boolean responseOnly) {
				return new Result(overflow, underflow, 
						headerCompleted, response, responseOnly) {
				};
			}

			/**
			 * Overrides the base interface's factory method in order to make
			 * it return the extended return type. As the {@link HttpRequestDecoder}
			 * does not know about a response, this implementation always
			 * returns a result without one. This may be a preliminary result
			 * and replaced in {@link HttpRequestDecoder#decode(ByteBuffer, Buffer, boolean)}.
			 * 
			 * @param overflow
			 *            {@code true} if the data didn't fit in the out buffer
			 * @param underflow
			 *            {@code true} if more data is expected
			 */
			@Override
			protected Result newResult(
			        boolean overflow, boolean underflow) {
				Result result = new Result(overflow, underflow, 
						decoder.reportHeaderReceived, null, false) {
				};
				decoder.reportHeaderReceived = false;
				return result;
			}
		}
	}
}
