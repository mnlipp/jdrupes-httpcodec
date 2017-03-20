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

package org.jdrupes.httpcodec.protocols.http;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.text.ParseException;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.MessageHeader;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;
import org.jdrupes.httpcodec.protocols.http.fields.HttpContentLengthField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpSetCookieListField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;
import org.jdrupes.httpcodec.util.ByteBufferUtils;
import org.jdrupes.httpcodec.util.DynamicByteArray;
import org.jdrupes.httpcodec.util.OptimizedCharsetDecoder;


/**
 * Implements a decoder for HTTP. The class can be used as base class for both
 * a request and a response decoder.
 * 
 * @param <T> the type of the message header to be decoded
 * @param <R> the type of the response message header
 */
public abstract class 	HttpDecoder<T extends HttpMessageHeader,
	R extends HttpMessageHeader> 
	extends HttpCodec<T> implements Decoder<T, R> {

	protected static final String TOKEN = "[" + Pattern.quote(TOKEN_CHARS)
	        + "]+";
	protected static final String SP = "[ \\t]+";
	protected static final String HTTP_VERSION = "HTTP/\\d+\\.\\d";

	// RFC 7230 3.2, 3.2.4
	protected static final Pattern headerLinePatter = Pattern
	        .compile("^(" + TOKEN + "):(.*)$");

	private enum State {
	    // Main states
		AWAIT_MESSAGE_START, HEADER_LINE_RECEIVED, COPY_UNTIL_CLOSED, 
		LENGTH_RECEIVED, CHUNK_START_RECEIVED, CHUNK_END_RECEIVED, 
		CHUNK_TRAILER_LINE_RECEIVED, CLOSED,
		// Sub states
		RECEIVE_LINE, AWAIT_LINE_END, COPY_SPECIFIED, FINISH_CHARDECODER, 
		FLUSH_CHARDECODER
	}

	protected enum BodyMode {
		NO_BODY, CHUNKED, LENGTH, UNTIL_CLOSE
	}

	private long maxHeaderLength = 4194304;
	private Stack<State> states = new Stack<>();
	private DynamicByteArray lineBuilder = new DynamicByteArray(8192);
	private String receivedLine;
	private String headerLine = null;
	protected HttpProtocol protocolVersion = HttpProtocol.HTTP_1_0;
	private long headerLength = 0;
	private T building;
	private long leftToRead = 0;
	private OptimizedCharsetDecoder charDecoder = null;

	/**
	 * Creates a new decoder.
	 */
	public HttpDecoder() {
		states.push(State.AWAIT_MESSAGE_START);
		states.push(State.RECEIVE_LINE);
	}

	/**
	 * Returns the result factory for this codec.
	 * 
	 * @return the factory
	 */
	protected abstract Result.Factory<R> resultFactory();
	
	/**
	 * Sets the maximum size for the complete header. If the size is exceeded, a
	 * {@link HttpProtocolException} will be thrown. The default size is 4MB
	 * (4194304 Byte).
	 * 
	 * @param maxHeaderLength
	 *            the maxHeaderLength to set
	 */
	public void setMaxHeaderLength(long maxHeaderLength) {
		this.maxHeaderLength = maxHeaderLength;
	}

	/**
	 * Returns the maximum header length.
	 * 
	 * @return the maxHeaderLength
	 */
	public long getMaxHeaderLength() {
		return maxHeaderLength;
	}

	/**
	 * Returns the message (header) if one exists.
	 * 
	 * @return the result
	 */
	public Optional<T> getHeader() {
		return Optional.ofNullable(messageHeader);
	}

	/**
	 * Returns {@code true} if the decoder does not accept further input because
	 * the processed data indicated that the connection has been or is to be
	 * closed.
	 * 
	 * @return the result
	 */
	public boolean isClosed() {
		return states.peek() == State.CLOSED;
	}

	/**
	 * Informs the derived class about the start of a new message.
	 * 
	 * @param startLine
	 *            the start line (first line) of the message
	 * @return the new HttpMessage object that is to hold the decoded data
	 * @throws HttpProtocolException if the input violates the HTTP
	 */
	protected abstract T newMessage(String startLine)
	        throws HttpProtocolException;

	/**
	 * Informs the derived class that the header has been received completely.
	 * 
	 * @param message the message
	 * @return indication how the body will be transferred
	 * @throws HttpProtocolException if the input violates the HTTP
	 */
	protected abstract BodyMode headerReceived(T message) 
			throws HttpProtocolException;

	/**
	 * Decodes the next chunk of data.
	 * 
	 * @param in
	 *            holds the data to be decoded
	 * @param out
	 *            gets the body data (if any) written to it
	 * @param endOfInput
	 *            {@code true} if there is no input left beyond the data
	 *            currently in the {@code in} buffer (indicates end of body or
	 *            no body at all)
	 * @return the result
	 * @throws HttpProtocolException
	 *             if the message violates the HTTP
	 */
	public Decoder.Result<R> decode( 
		ByteBuffer in, Buffer out, boolean endOfInput)
	        throws HttpProtocolException {
		try {
			try {
				return uncheckedDecode(in, out, endOfInput);
			} catch (ParseException | NumberFormatException e) {
				throw new HttpProtocolException(protocolVersion,
					HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
			}
		} catch (HttpProtocolException e) {
			states.clear();
			states.push(State.CLOSED);
			throw e;
		}
	}

	private Decoder.Result<R> uncheckedDecode(
		ByteBuffer in, Buffer out, boolean endOfInput)
			throws HttpProtocolException, ParseException {
		while(true) {
			switch (states.peek()) {
			// Waiting for CR (start of end of line)
			case RECEIVE_LINE: {
				if (!in.hasRemaining()) {
					return resultFactory().newResult(false, true);
				}
				byte ch = in.get();
				if (ch == '\r') {
					states.pop();
					states.push(State.AWAIT_LINE_END);
					break;
				}
				lineBuilder.append(ch);
				// RFC 7230 3.2.5
				if (headerLength + lineBuilder.position() > maxHeaderLength) {
					throw new HttpProtocolException(protocolVersion,
					        HttpStatus.BAD_REQUEST.getStatusCode(),
					        "Maximum header size exceeded");
				}
				break;
			}
			// Waiting for LF (confirmation of end of line)
			case AWAIT_LINE_END: {
				if (!in.hasRemaining()) {
					return resultFactory().newResult(false, true);
				}
				char ch = (char) in.get();
				if (ch == '\n') {
					try {
						// RFC 7230 3.2.4
						receivedLine = new String(lineBuilder.array(), 0,
						        lineBuilder.position(), "iso-8859-1");
					} catch (UnsupportedEncodingException e) {
						// iso-8859-1 is guaranteed to be supported
					}
					lineBuilder.clear();
					states.pop();
					break;
				}
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "CR not followed by LF");
			}
			// Waiting for the initial request line
			case AWAIT_MESSAGE_START:
				if (receivedLine.isEmpty()) {
					// Ignore as recommended by RFC2616/RFC7230
					states.push(State.RECEIVE_LINE);
					break;
				}
				building = newMessage(receivedLine);
				messageHeader = null;
				charDecoder = null;
				states.pop();
				headerLine = null;
				states.push(State.HEADER_LINE_RECEIVED);
				states.push(State.RECEIVE_LINE);
				break;

			case HEADER_LINE_RECEIVED:
				if (headerLine != null) {
					// RFC 7230 3.2.4
					if (!receivedLine.isEmpty()
					        && (receivedLine.charAt(0) == ' '
					                || receivedLine.charAt(0) == '\t')) {
						headerLine += (" " + receivedLine.substring(1));
						states.push(State.RECEIVE_LINE);
						break;
					}
					// Header line complete, evaluate
					newHeaderLine();
				}
				if (receivedLine.isEmpty()) {
					// Body starts
					BodyMode bm = headerReceived(building);
					adjustToBodyMode(bm);
					messageHeader = building;
					building = null;
					if (bm == BodyMode.NO_BODY) {
						adjustToEndOfMessage();
						return resultFactory().newResult(false, false);
					}
					if (out == null) {
						return resultFactory().newResult(true, false);
					}
					break;
				}
				headerLine = receivedLine;
				states.push(State.RECEIVE_LINE);
				break;

			case LENGTH_RECEIVED:
				// We "drop" to this state after COPY_SPECIFIED
				// if we had a content length field
				states.pop();
				adjustToEndOfMessage();
				return resultFactory().newResult(false, false);

			case CHUNK_START_RECEIVED:
				// We "drop" to this state when a line has been read
				String sizeText = receivedLine.split(";")[0];
				long chunkSize = Long.parseLong(sizeText, 16);
				if (chunkSize == 0) {
					states.pop();
					states.push(State.CHUNK_TRAILER_LINE_RECEIVED);
					states.push(State.RECEIVE_LINE);
					break;
				}
				leftToRead = chunkSize;
				// We expect the chunk data and the trailing CRLF (empty line)
				// (which must be skipped). In reverse order:
				states.push(State.CHUNK_END_RECEIVED);
				states.push(State.RECEIVE_LINE);
				states.push(State.COPY_SPECIFIED);
				break;

			case CHUNK_END_RECEIVED:
				// We "drop" to this state when the CR/LF after chunk data
				// has been read. There's nothing to do except to wait for
				// next chunk
				if (receivedLine.length() != 0) {
					throw new HttpProtocolException(protocolVersion,
					        HttpStatus.BAD_REQUEST.getStatusCode(),
					        "No CRLF after chunk data.");
				}
				states.pop();
				states.push(State.CHUNK_START_RECEIVED);
				states.push(State.RECEIVE_LINE);
				break;

			case CHUNK_TRAILER_LINE_RECEIVED:
				// We "drop" to this state when a line has been read
				if (!receivedLine.isEmpty()) {
					headerLine = receivedLine;
					newTrailerLine();
					states.push(State.RECEIVE_LINE);
					break;
				}
				// All chunked data received
				adjustToEndOfMessage();
				return resultFactory().newResult(false, false);

			case COPY_SPECIFIED:
				if (out == null) {
					return resultFactory().newResult(true, false);
				}
				int initiallyRemaining = in.remaining();
				CoderResult decRes;
				if (in.remaining() <= leftToRead) {
					decRes = copyBodyData(out, in, in.remaining(), endOfInput);
				} else {
					decRes = copyBodyData(
							out, in, (int) leftToRead, endOfInput);
				}
				leftToRead -= (initiallyRemaining - in.remaining());
				if (leftToRead == 0) {
					// Everything written (except, maybe, final bytes 
					// from decoder)
					states.pop();
					if (out instanceof CharBuffer && charDecoder != null) {
						states.push(State.FINISH_CHARDECODER);
					}
					break;
				}
				return resultFactory().newResult(
						(!out.hasRemaining() && in.hasRemaining())
						|| (decRes != null && decRes.isOverflow()),
				        !in.hasRemaining() 
				        || (decRes != null && decRes.isUnderflow()));

			case FINISH_CHARDECODER:
				if (charDecoder.decode(EMPTY_IN, (CharBuffer) out, true)
				        .isOverflow()) {
					return resultFactory().newResult(true, false);
				}
				states.pop();
				states.push(State.FLUSH_CHARDECODER);
				break;
				
			case FLUSH_CHARDECODER:
				if (charDecoder.flush((CharBuffer)out).isOverflow()) {
					return resultFactory().newResult(true, false);
				}
				states.pop();
				break;

			case COPY_UNTIL_CLOSED:
				if (out == null) {
					return resultFactory().newResult(true, false);
				}
				decRes = copyBodyData(out, in, in.remaining(), endOfInput);
				boolean overflow = (!out.hasRemaining() && in.hasRemaining())
						|| (decRes != null && decRes.isOverflow());
				if (overflow) {
					return resultFactory().newResult(true, false);
				}
				if (!endOfInput) {
					return resultFactory().newResult(false, true);
				}
				// Final input successfully processed.
				states.pop();
				states.push(State.CLOSED);
				if (out instanceof CharBuffer && charDecoder != null) {
					// Final flush needed
					states.push(State.FINISH_CHARDECODER);
				}
				break;

			case CLOSED:
				in.position(in.limit());
				return resultFactory().newResult(false, false);
			}
		}
	}

	private void newHeaderLine() throws HttpProtocolException, ParseException {
		headerLength += headerLine.length() + 2;
		// RFC 7230 3.2
		Matcher hlp = headerLinePatter.matcher(headerLine);
		if (!hlp.matches()) {
			throw new HttpProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.getStatusCode(), "Invalid header");
		}
		String fieldName = hlp.group(1);
		// RFC 7230 3.2.4
		String fieldValue = hlp.group(2).trim();
		HttpField<?> field;
		if (fieldName.equalsIgnoreCase(HttpField.SET_COOKIE)) {
			field = HttpSetCookieListField.fromString(fieldValue);
		} else {
			field = HttpStringField.fromString(fieldName, fieldValue);
		}
		switch (field.getName()) {
		case HttpField.CONTENT_LENGTH:
			// RFC 7230 3.3.3 (3.)
			if (building.fields()
			        .containsKey(HttpField.TRANSFER_ENCODING)) {
				field = null;
				break;
			}
			// RFC 7230 3.3.3 (4.)
			HttpContentLengthField existing = building.getField(
			        HttpContentLengthField.class, HttpField.CONTENT_LENGTH)
					.orElse(null);
			if (existing != null && !existing.getValue()
			        .equals(((HttpContentLengthField) field).getValue())) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST);
			}
			break;
		case HttpField.TRANSFER_ENCODING:
			// RFC 7230 3.3.3 (3.)
			building.removeField(HttpField.CONTENT_LENGTH);
			break;
		}
		if (field == null) {
			return;
		}
		addHeaderField(building, field);
	}

	private void newTrailerLine() throws HttpProtocolException, ParseException {
		headerLength += headerLine.length() + 2;
		// RFC 7230 3.2
		Matcher hlp = headerLinePatter.matcher(headerLine);
		if (!hlp.matches()) {
			throw new HttpProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.getStatusCode(), "Invalid header");
		}
		String fieldName = hlp.group(1);
		// RFC 7230 3.2.4
		String fieldValue = hlp.group(2).trim();
		HttpField<?> field = HttpStringField.fromString(fieldName, fieldValue);
		// RFC 7230 4.4
		HttpStringListField trailerField = messageHeader
		        .computeIfAbsent(HttpStringListField.class, HttpField.TRAILER,
		        		n -> new HttpStringListField(n));
		if (!trailerField.containsIgnoreCase(field.getName())) {
			trailerField.add(field.getName());
		}
		addHeaderField(messageHeader, field);
	}

	private void addHeaderField(T header, HttpField<?> field)
	        throws HttpProtocolException, ParseException {
		// RFC 7230 3.2.2
		HttpField<?> existing = header.fields().get(field.getName());
		if (existing != null) {
			if (!(existing instanceof HttpListField<?>)
			        || !(field instanceof HttpListField<?>)
			        || !(existing.getClass().equals(field.getClass()))) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "Multiple occurences of field " + field.getName());
			}
			((HttpListField<?>) existing).combine((HttpListField<?>) field);
		} else {
			header.setField(field);
		}
	}

	private void adjustToBodyMode(BodyMode bm) {
		building.setMessageHasBody(bm != BodyMode.NO_BODY);
		states.pop();
		switch (bm) {
		case UNTIL_CLOSE:
			states.push(State.COPY_UNTIL_CLOSED);
			break;
		case CHUNKED:
			states.push(State.CHUNK_START_RECEIVED);
			states.push(State.RECEIVE_LINE);
			break;
		case LENGTH:
			HttpContentLengthField clf = building.getField(
			        HttpContentLengthField.class,
			        HttpField.CONTENT_LENGTH).get();
			leftToRead = clf.getValue();
			if (leftToRead > 0) {
				states.push(State.LENGTH_RECEIVED);
				states.push(State.COPY_SPECIFIED);
				break;
			}
			// Length == 0 means no body, fall through
		case NO_BODY:
			break;
		}
	}

	private CoderResult copyBodyData(
			Buffer out, ByteBuffer in, int limit, boolean endOfInput) {
		if (out instanceof ByteBuffer) {
			ByteBufferUtils.putAsMuchAsPossible((ByteBuffer) out, in, limit);
			return null;
		} else if (out instanceof CharBuffer) {
			if (charDecoder == null) {
				charDecoder = new OptimizedCharsetDecoder(
				        Charset.forName(bodyCharset()).newDecoder());
			}
			int oldLimit = in.limit();
			try {
				if (in.remaining() > limit) {
					in.limit(in.position() + limit);
				}
				return charDecoder.decode(in, (CharBuffer)out, endOfInput);
			} finally {
				in.limit(oldLimit);
			}
		} else {
			throw new IllegalArgumentException(
			        "Only Byte- or CharBuffer are allowed.");
		}
	}

	private void adjustToEndOfMessage() {
		// RFC 7230 6.3
		Optional<HttpStringListField> connection = messageHeader
		        .getField(HttpStringListField.class, HttpField.CONNECTION);
		if (connection.isPresent() 
				&& connection.get().containsIgnoreCase("close")) {
			states.push(State.CLOSED);
			return;
		}
		if (messageHeader.getProtocol().compareTo(HttpProtocol.HTTP_1_1) >= 0) {
			states.push(State.AWAIT_MESSAGE_START);
			states.push(State.RECEIVE_LINE);
			return;
		}
		states.push(State.CLOSED);
	}
	
	/**
	 * Results from {@link HttpDecoder} add no additional
	 * information to {@link org.jdrupes.httpcodec.Decoder.Result}. This
	 * class provides only a factory for creating 
	 * the results as required by {@link HttpDecoder}.
	 * 
	 * @param <R> the type of the response message header
	 */
	public static class Result<R extends MessageHeader>
		extends Decoder.Result<R> {

		public Result(boolean overflow, boolean underflow,
		        boolean closeConnection, boolean headerCompleted, R response,
		        boolean responseOnly) {
			super(overflow, underflow, closeConnection, headerCompleted, response,
			        responseOnly);
		}

		/**
		 * A factory for creating new Results.
		 */
		protected abstract static class Factory<R extends MessageHeader> 
			extends Decoder.Result.Factory<R> {
			
			/**
			 * Create a new result. Implementing classes can
			 * obtain the value for 
			 * {@link org.jdrupes.httpcodec.Codec.Result#getCloseConnection()}
			 * from {@link HttpDecoder#isClosed()}.
			 * 
			 * @param overflow
			 *            {@code true} if the data didn't fit in the out buffer
			 * @param underflow
			 *            {@code true} if more data is expected
			 * @return the result
			 */
			protected abstract Result<R> newResult(
				boolean overflow, boolean underflow);
		}		
	}
}
