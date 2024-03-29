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

package org.jdrupes.httpcodec.protocols.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Iterator;
import java.util.Optional;
import java.util.Stack;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.StringList;
import org.jdrupes.httpcodec.util.ByteBufferOutputStream;
import org.jdrupes.httpcodec.util.ByteBufferUtils;

/**
 * Implements an encoder for HTTP. The class can be used as base class for both
 * a request and a response encoder.
 * 
 * @param <T> the type of the message header to be encoded
 * @param <D> the type of the message header decoded by the peer decoder
 */
public abstract class HttpEncoder<T extends HttpMessageHeader,
	D extends HttpMessageHeader> extends HttpCodec<T>
	implements Encoder<T, D> {

	private enum State {
		// Main states
		INITIAL, DONE, CLOSED,
		// Sub states
		HEADERS, CHUNK_BODY, STREAM_CHUNK, FINISH_CHUNK, FINISH_CHUNKED,
		START_COLLECT_BODY, COLLECT_BODY, STREAM_COLLECTED, 
		STREAM_BODY, FLUSH_ENCODER
	}

	private Stack<State> states = new Stack<>();
	private boolean closeAfterBody = false;
	private ByteBufferOutputStream outStream;
	private Writer writer;
	private Iterator<HttpField<?>> headerIter = null;
	private int pendingLimit = 1024 * 1024;
	private long leftToStream;
	private ByteBufferOutputStream collectedBodyData;
	private CharsetEncoder charEncoder = null;
	private Writer charWriter = null;
	private ByteBuffer chunkData;
	protected Decoder<D, T> peerDecoder;

	/**
	 * Creates a new encoder.
	 */
	public HttpEncoder() {
		outStream = new ByteBufferOutputStream();
		try {
			writer = new OutputStreamWriter(outStream, "ascii");
		} catch (UnsupportedEncodingException e) {
			// Cannot happen (specified to be supported)
		}
		states.push(State.INITIAL);
	}

	public Encoder<T, D> setPeerDecoder(Decoder<D, T> decoder) {
		peerDecoder = decoder;
		return this;
	}
	
	/**
	 * Returns the result factory for this codec.
	 * 
	 * @return the factory
	 */
	protected abstract Result.Factory resultFactory();
	
	/**
	 * Returns the limit for pending body bytes. If the protocol is HTTP/1.0 and
	 * the message has a body but no "Content-Length" header, the only
	 * (unreliable) way to indicate the end of the body is to close the
	 * connection after all body bytes have been sent.
	 * <P>
	 * The encoder tries to calculate the content length by buffering the body
	 * data up to the "pending" limit. If the body is smaller than the limit,
	 * the message is set with the calculated content length header, else the
	 * data is sent without such a header and the connection is closed.
	 * <P>
	 * If the response protocol is HTTP/1.1 and there is no "Content-Length"
	 * header, chunked transfer encoding is used.
	 *
	 * @return the limit
	 */
	public int pendingLimit() {
		return pendingLimit;
	}

	/**
	 * Sets the limit for the pending body bytes.
	 * 
	 * @param pendingLimit
	 *            the limit to set
	 */
	public void setPendingLimit(int pendingLimit) {
		this.pendingLimit = pendingLimit;
	}

	/**
	 * Returns {@code true} if the encoder does not accept further input because
	 * the processed data indicated that the connection has been or is to be
	 * closed.
	 * 
	 * @return the result
	 */
	public boolean isClosed() {
		return states.peek() == State.CLOSED;
	}

	/**
	 * Writes the first line of the message (including the terminating CRLF).
	 * Must be provided by the derived class because the first line depends on
	 * whether a request or response is encoded.
	 * 
	 * @param messageHeader
	 *            the message header to encode (see
	 *            {@link #encode(HttpMessageHeader)}
	 * @param writer
	 *            the Writer to use for writing
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void startMessage(T messageHeader, Writer writer) 
			throws IOException;

    /**
     * Force close after body. Used to override the header driven
     * behavior for HTTP/1.0 responses.
     *
     * @return true, if forced
     */
    protected boolean forceCloseAfterBody() {
        return false;
    }
    
	/**
	 * Set a new HTTP message that is to be encoded.
	 * 
	 * @param messageHeader
	 *            the response
	 */
	@Override
	public void encode(T messageHeader) {
		if (states.peek() != State.INITIAL) {
			throw new IllegalStateException();
		}
		this.messageHeader = messageHeader;
		charEncoder = null;
		charWriter = null;
		// Consistency checks
		if (messageHeader.fields().containsKey(HttpField.CONTENT_TYPE)) {
			messageHeader.setHasPayload(true);
		}
	}

	/**
	 * Encodes a HTTP message.
	 * 
	 * @param in
	 *            the body data
	 * @param out
	 *            the buffer to which data is written
	 * @param endOfInput
	 *            {@code true} if there is no input left beyond the data
	 *            currently in the {@code in} buffer (indicates end of body or
	 *            no body at all)
	 * @return the result
	 */
	public Encoder.Result encode(Buffer in, ByteBuffer out,
	        boolean endOfInput) {
		outStream.assignBuffer(out); // Prepare and copy over what was left.
		if (out.remaining() == 0) { // If a lot was left.
			outStream.assignBuffer(null);
			return resultFactory().newResult(true, false, false);
		}
		Encoder.Result result = resultFactory().newResult(false, false, false);
		while (true) {
			if (result.isOverflow() || result.isUnderflow()) {
				outStream.assignBuffer(null);
				return result;
			}
			switch (states.peek()) {
			case INITIAL:
				outStream.clear();
				outStream.assignBuffer(out);
				startEncoding();
				break;

			case HEADERS:
				// If headers remain (new request or buffer was full) write them
				if (writeHeaders()) {
					continue;
				}
				break;

			case START_COLLECT_BODY:
				// Start collecting
				if (in.remaining() == 0 && !endOfInput) {
					// Has probably been invoked with a dummy buffer,
					// cannot be used to create pending body buffer.
					outStream.assignBuffer(null);
					return resultFactory().newResult(false, true, false);
				}
				collectedBodyData = new ByteBufferOutputStream(
				        Math.min(pendingLimit, 
				        		Math.max(in.capacity(), 64 * 1024)));
				states.pop();
				states.push(State.COLLECT_BODY);
				// fall through (no write occurred yet)
			case COLLECT_BODY:
				result = collectBody(in, endOfInput);
				continue; // never writes to out

			case STREAM_COLLECTED:
				// Output collected body
				if (collectedBodyData.remaining() < 0) {
					// Copy over.
					collectedBodyData.assignBuffer(out);
					collectedBodyData.assignBuffer(null);
					break;
				}
				states.pop();
				continue;

			case STREAM_BODY:
				// Stream, i.e. simply copy from source to destination
				int initiallyRemaining = out.remaining();
				result = copyBodyData(in, out, endOfInput);
				leftToStream -= (initiallyRemaining - out.remaining());
				if (!result.isOverflow() && (leftToStream == 0 || endOfInput)) {
					// end of data
					states.pop();
					if (charEncoder != null) {
						states.push(State.FLUSH_ENCODER);
					}
					continue;
				}
				// Buffer written, waiting for space, more data or end of data
				outStream.assignBuffer(null);
				return result;

			case FLUSH_ENCODER:
				CoderResult flushResult = charEncoder.flush(out);
				if (flushResult.isOverflow()) {
					outStream.assignBuffer(null);
					return resultFactory().newResult(true, false, false);
				}
				states.pop();
				break;
				
			case CHUNK_BODY:
				// Send in data as chunk
				result = startChunk(in, endOfInput);
				continue; // remaining check already done 

			case STREAM_CHUNK:
				// Stream, i.e. simply copy from source to destination
				if (!ByteBufferUtils.putAsMuchAsPossible(out, chunkData)) {
					// Not enough space in out buffer
					outStream.assignBuffer(null);
					return resultFactory().newResult(true, false, false);
				}
				// everything from in written
				states.pop();
				continue;
	
			case FINISH_CHUNK:
				try {
					outStream.write("\r\n".getBytes("ascii"));
					states.pop();
				} catch (IOException e) {
					// Formally thrown by write
				}
				break;
				
			case FINISH_CHUNKED:
				try {
					outStream.write("0\r\n\r\n".getBytes("ascii"));
					states.pop();
				} catch (IOException e) {
					// Formally thrown by write
				}
				break;
				
			case DONE:
				// Everything is written
				outStream.assignBuffer(null);
				if (!endOfInput) {
					if (in.remaining() == 0) {
						return resultFactory()
								.newResult(false, true, false);
					}
					throw new IllegalStateException(
							"A message has been completely encoded but"
							+ " encode has not been invoked with endOfInput"
							+ " set to true.");
				}
				states.pop();
				if (closeAfterBody) {
					states.push(State.CLOSED);
				} else {
					states.push(State.INITIAL);
				}
				return resultFactory().newResult(false, false, isClosed());

			default:
				throw new IllegalStateException();
			}
			// Using "continue" above avoids this check. Use it only
			// if the state has changed and no additional output is expected. 
			if (out.remaining() == 0) {
				outStream.assignBuffer(null);
				return resultFactory().newResult(true, false, false);
			}
		}
	}

	@Override
	public Optional<T> header() {
		return Optional.ofNullable(messageHeader);
	}

	/**
	 * Called during the initial state. Writes the status line, determines the
	 * body-mode and puts the state machine in output-headers mode, unless the
	 * body-mode is "collect body".
	 */
	private void startEncoding() {
		// Complete content type
		Optional<HttpField<MediaType>> contentField = messageHeader
				.findField(HttpField.CONTENT_TYPE, Converters.MEDIA_TYPE);
		if (contentField.isPresent()) {
			MediaType type = contentField.get().value();
			if ("text".equals(type.topLevelType())
					&& type.parameter("charset") == null) {
				messageHeader.setField(HttpField.CONTENT_TYPE,
						MediaType.builder().from(type)
						.setParameter("charset", "utf-8").build());
			}
		}
		
		// Prepare encoder
		headerIter = null;

		// Write request or status line
		try {
			startMessage(messageHeader, writer);
		} catch (IOException e) {
			// Formally thrown by writer, cannot happen.
		}
		states.pop();
		// We'll eventually fall back to this state
		states.push(State.DONE);
		// Get a default for closeAfterBody from the header fields
		closeAfterBody = forceCloseAfterBody() || messageHeader
		        .findField(HttpField.CONNECTION, Converters.STRING_LIST)
		        .map(h -> h.value()).map(f -> f.containsIgnoreCase("close"))
		        .orElse(false);
		// If there's no body, start outputting header fields
		if (!messageHeader.hasPayload()) {
			states.push(State.HEADERS);
			return;
		}
		configureBodyHandling();
	}

	private void configureBodyHandling() {
		// Message has a body, find out how to handle it
		Optional<HttpField<Long>> cl = messageHeader
				.findField(HttpField.CONTENT_LENGTH, Converters.LONG);
		leftToStream = (!cl.isPresent() ? -1 : cl.get().value());
		if (leftToStream >= 0) {
			// Easiest: we have a content length, works always
			states.push(State.STREAM_BODY);
			states.push(State.HEADERS);
			return;
		} 
		if (messageHeader.protocol()
		        .compareTo(HttpProtocol.HTTP_1_0) > 0) {
			// At least 1.1, use chunked
			Optional<HttpField<StringList>> transEnc = messageHeader.findField(
			        HttpField.TRANSFER_ENCODING, Converters.STRING_LIST);
			if (!transEnc.isPresent()) {
				messageHeader.setField(HttpField.TRANSFER_ENCODING,
						new StringList(TransferCoding.CHUNKED.toString()));
			} else {
				transEnc.get().value().remove(TransferCoding.CHUNKED.toString());
				transEnc.get().value().add(TransferCoding.CHUNKED.toString());
			}
			states.push(State.CHUNK_BODY);
			states.push(State.HEADERS);
			return;
		}
		// Bad: 1.0 and no content length.
		if (pendingLimit > 0) {
			// Try to calculate length by collecting the data
			leftToStream = 0;
			states.push(State.START_COLLECT_BODY);
			return;
		}
		// May not buffer, use close
		states.push(State.STREAM_BODY);
		states.push(State.HEADERS);
		closeAfterBody = true;
	}

	/**
	 * Outputs as many headers as fit in the current out buffer. If all headers
	 * are output, pops a state (thus proceeding with the selected body mode).
	 * Unless very small out buffers are used (or very large headers occur),
	 * this is invoked only once. Therefore no attempt has been made to avoid
	 * the usage of temporary buffers in the header header stream (there may be
	 * a maximum overflow of one partial header).
	 * 
	 * @return {@code true} if all headers could be written to the out
	 * buffer (nothing pending)
	 */
	private boolean writeHeaders() {
		try {
			if (headerIter == null) {
				headerIter = messageHeader.fields().values().iterator();
			}
			while (true) {
				if (!headerIter.hasNext()) {
					writer.write("\r\n");
					writer.flush();
					states.pop();
					return outStream.remaining() >= 0;
				}
				HttpField<?> header = headerIter.next();
				writer.write(header.asHeaderField());
				writer.write("\r\n");
				writer.flush();
				if (outStream.remaining() <= 0) {
					break;
				}
			}
		} catch (IOException e) {
			// Formally thrown by writer, cannot happen.
		}
		return false;
	}

	/**
	 * Copy as much data as possible from in to out.
	 * 
	 * @param in
	 * @param out
	 * @param endOfInput
	 */
	private Encoder.Result copyBodyData(Buffer in, ByteBuffer out,
	        boolean endOfInput) {
		if (in instanceof CharBuffer) {
			// copy via encoder
			if (charEncoder == null) {
				charEncoder = Charset.forName(bodyCharset()).newEncoder();
			}
			CoderResult result = charEncoder.encode((CharBuffer) in, out,
			        endOfInput);
			return resultFactory().newResult(result.isOverflow(), 
					!endOfInput && result.isUnderflow(), false);
		}
		if (out.remaining() <= leftToStream) {
			ByteBufferUtils.putAsMuchAsPossible(out, (ByteBuffer) in);
		} else {
			ByteBufferUtils.putAsMuchAsPossible(out, (ByteBuffer) in,
			        (int) leftToStream);
		}
		return resultFactory().newResult(out.remaining() == 0, 
				!endOfInput && in.remaining() == 0, false);
	}

	/**
	 * Handle the input as appropriate for the collect-body mode.
	 * 
	 * @param in
	 * @param endOfInput
	 */
	private Encoder.Result collectBody(Buffer in, boolean endOfInput) {
		if (collectedBodyData.remaining() - in.remaining() < -pendingLimit) {
			// No space left, output headers, collected and rest (and then
			// close)
			states.pop();
			closeAfterBody = true;
			leftToStream = Long.MAX_VALUE;
			states.push(State.STREAM_BODY);
			states.push(State.STREAM_COLLECTED);
			states.push(State.HEADERS);
			return resultFactory().newResult(false, false, false);
		}
		// Space left, collect
		if (in instanceof ByteBuffer) {
			collectedBodyData.write((ByteBuffer)in);
		} else {
			if (charWriter == null) {
				try {
					charWriter = new OutputStreamWriter(collectedBodyData,
					        bodyCharset());
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException(e);
				}
			}
			try {
				if (in.hasArray()) {
					// more efficient than CharSequence
					charWriter.write(((CharBuffer) in).array(),
					        in.arrayOffset() + in.position(), in.remaining());
				} else {
					charWriter.append((CharBuffer)in);
				}
				in.position(in.limit());
				charWriter.flush();
			} catch (IOException e) {
				// Formally thrown, cannot happen
			}
		}
		if (endOfInput) {
			// End of body, found content length!
			messageHeader.setField(
					HttpField.CONTENT_LENGTH, collectedBodyData.bytesWritten());
			states.pop();
			states.push(State.STREAM_COLLECTED);
			states.push(State.HEADERS);
			return resultFactory().newResult(false, false, false);
		}
		// Get more input
		return resultFactory().newResult(false, true, false);
	}

	/**
	 * Handle the input as appropriate for the chunked-body mode.
	 * 
	 * @param in the data
	 * @return the result
	 */
	private Encoder.Result startChunk(Buffer in, boolean endOfInput) {
		if (endOfInput) {
			states.pop();
			states.push(State.FINISH_CHUNKED);
		}
		try {
			// Don't write zero sized chunks
			if (in.hasRemaining()) {
				if (in instanceof CharBuffer) {
					if (charEncoder == null) {
						charEncoder = Charset.forName(bodyCharset())
						        .newEncoder();
					}
					chunkData = charEncoder.encode((CharBuffer)in);
				} else {
					chunkData = (ByteBuffer)in;
				}
				outStream.write(
				        Long.toHexString(chunkData.remaining())
				                .getBytes("ascii"));
				outStream.write("\r\n".getBytes("ascii"));
				states.push(State.FINISH_CHUNK);
				states.push(State.STREAM_CHUNK);
				return resultFactory().newResult(
						outStream.remaining() <= 0, false, false);
			}
		} catch (IOException e) {
			// Formally thrown by outStream, cannot happen.
		}
		return resultFactory().newResult(outStream.remaining() < 0, 
				in.remaining() == 0 && !endOfInput, false);
	}
	
	/**
	 * Results from {@link HttpEncoder} provide no additional
	 * information compared to {@link org.jdrupes.httpcodec.Codec.Result}. 
	 * This class only provides a factory for creating concrete results.
	 */
	public static class Result extends Codec.Result {
	
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection) {
			super(overflow, underflow, closeConnection);
		}

		/**
		 * A factory for creating new Results.
		 */
		protected abstract static class Factory 
			extends Codec.Result.Factory {

			/**
			 * Create new result.
			 * 
			 * @param overflow
			 *            {@code true} if the data didn't fit in the out buffer
			 * @param underflow
			 *            {@code true} if more data is expected
			 * @param closeConnection
			 *            {@code true} if the connection should be closed
			 * @return the result
			 */
			public Result newResult(boolean overflow, boolean underflow,
			        boolean closeConnection) {
				return new Result(overflow, underflow, closeConnection) {
				};
			}
		}
	}
}
