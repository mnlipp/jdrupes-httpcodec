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

package org.jdrupes.httpcodec.protocols.websocket;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.EmptyStackException;
import java.util.Optional;
import java.util.Stack;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.protocols.http.HttpEncoder;
import org.jdrupes.httpcodec.util.ByteBufferOutputStream;
import org.jdrupes.httpcodec.util.ByteBufferUtils;

/**
 * The Websocket encoder.
 */
public class WsEncoder extends WsCodec 
	implements Encoder<WsFrameHeader, WsFrameHeader> {

	private static enum State { STARTING_FRAME, WRITING_HEADER,  
		WRITING_LENGTH, WRITING_MASK, WRITING_PAYLOAD }
	
	private static float bytesPerCharUtf8		
		= Charset.forName("utf-8").newEncoder().averageBytesPerChar();
	private static final Result.Factory resultFactory = new Result.Factory();
	
	private SecureRandom randoms = new SecureRandom();
	private State state = State.STARTING_FRAME;
	private boolean continuationFrame;
	private Stack<WsFrameHeader> messageHeaders = new Stack<>();
	private int headerHead;
	private long bytesToSend;
	private long payloadSize;
	private int payloadBytes;
	private boolean doMask = false;
	private byte[] maskingKey = new byte[4];
	private int maskIndex;
	private ByteBufferOutputStream convData = new ByteBufferOutputStream();

	/**
	 * Creates new encoder.
	 * 
	 * @param mask set if the data is to be masked (client)
	 */
	public WsEncoder(boolean mask) {
		super();
		this.doMask = mask;
	}

	public Encoder<WsFrameHeader, WsFrameHeader> setPeerDecoder(
			Decoder<WsFrameHeader, WsFrameHeader> decoder) {
		linkClosingState((WsCodec)decoder);
		return this;
	}
	
	/**
	 * Returns the result factory for this codec.
	 * 
	 * @return the factory
	 */
	protected Result.Factory resultFactory() {
		return resultFactory;
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Encoder#encoding()
	 */
	@Override
	public Class<WsFrameHeader> encoding() {
		return WsFrameHeader.class;
	}

	private Result frameFinished(boolean endOfInput) {
		// If we have encoded a close, adapt
		boolean close = false;
		if (messageHeaders.peek() instanceof WsCloseFrame) {
			switch (closingState()) {
			case OPEN:
				setClosingState(ClosingState.CLOSE_SENT);
				break;
			case CLOSE_RECEIVED:
				setClosingState(ClosingState.CLOSED);
				// fall through
			case CLOSED:
				if (!doMask) {
					// Server side encoder
					close = true;
				}
				break;
			case CLOSE_SENT:
				// Shouldn't happen
				break;
			}
		}
		// Fix statck
		if (!(messageHeaders.peek() instanceof WsMessageHeader) 
				|| endOfInput) {
			messageHeaders.pop();
		}
		state = State.STARTING_FRAME;
		bytesToSend = 2;
		return resultFactory().newResult(false, 
				!endOfInput || !messageHeaders.isEmpty(), close);
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.ResponseEncoder#encode(org.jdrupes.httpcodec.MessageHeader)
	 */
	@Override
	public void encode(WsFrameHeader messageHeader) {
		if (state != State.STARTING_FRAME) {
			throw new IllegalStateException(
					"Trying to start new frame while previous "
						+ "has not completely been sent");
		}
		if (messageHeader instanceof WsMessageHeader) {
			messageHeaders.clear();
			messageHeaders.push(messageHeader);
			if (((WsMessageHeader) messageHeader).isTextMode()) {
				headerHead = (1 << 8);
			} else {
				headerHead = (2 << 8);
			}
			continuationFrame = false;
		} else {
			messageHeaders.push(messageHeader);
			if (messageHeader instanceof WsCloseFrame) {
				headerHead = (8 << 8);
			} else if (messageHeader instanceof WsPingFrame) {
				headerHead = (9 << 8);
			} else if (messageHeader instanceof WsPongFrame) {
				headerHead = (10 << 8);
			} else {
				throw new IllegalArgumentException(
				        "Invalid hessage header type");
			}
		}
		state = State.STARTING_FRAME;
		bytesToSend = 2;
	}

	@Override
	public Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		if (closingState() == ClosingState.CLOSED) {
			// Must no longer send anything. 
			// If server (!doMask) close connection.
			return resultFactory().newResult(false, false, !doMask);
		}
		Result result = null;
		while (out.remaining() > 0) {
			switch(state) {
			case STARTING_FRAME:
				prepareHeaderHead(in, endOfInput);
				// If called again without new message header...
				continuationFrame = true;
				state = State.WRITING_HEADER;
				// fall through
			case WRITING_HEADER:
				out.put((byte)(headerHead >> 8 * --bytesToSend));
				if (bytesToSend > 0) {
					continue;
				}
				if (payloadBytes > 0) {
					state = State.WRITING_LENGTH;
					bytesToSend = payloadBytes;
					continue;
				}
				// Length written
				result = nextAfterLength(endOfInput);
				break;
			case WRITING_LENGTH:
				out.put((byte)(payloadSize >> 8 * --bytesToSend));
				if (bytesToSend > 0) {
					continue;
				}
				result = nextAfterLength(endOfInput);
				break;
			case WRITING_MASK:
				out.put(maskingKey[4 - (int)bytesToSend]);
				if (--bytesToSend > 0) {
					continue;
				}
				result = nextAfterMask(endOfInput);
				break;
			case WRITING_PAYLOAD:
				int posBefore = out.position();
				outputPayload(in, out);
				bytesToSend -= (out.position() - posBefore);
				if (bytesToSend == 0) {
					convData.clear();
					return frameFinished(endOfInput);
				}
				return resultFactory().newResult(!out.hasRemaining(),
						(messageHeaders.peek() instanceof WsMessageHeader) 
							&& !in.hasRemaining(), false);
			}
			if (result != null) {
				return result;
			}
		}
		return resultFactory().newResult(true, false, false);
	}

	/**
	 * Prepares the start (head) of the header. As a side effect, if
	 * "in" holds textual data (or if the data is obtained from the
	 * to be encoded message header (close frame)) it is written into
	 * convData because this is the only way to "calculate" the payload 
	 * size. 
	 * 
	 * @param in input data
	 * @param endOfInput set if end of input
	 */
	private void prepareHeaderHead(Buffer in, boolean endOfInput) {
		WsFrameHeader hdr = messageHeaders.peek();
		if (hdr instanceof WsMessageHeader) {
			if (continuationFrame) {
				headerHead = 0;
			}
			if (endOfInput) {
				headerHead |= 0x8000;
			}
			// Prepare payload
			if (in instanceof CharBuffer) {
				convData.clear();
				payloadSize = convTextData(in);
			} else {
				payloadSize = in.remaining();
			}
		} else {
			// Control frame
			headerHead |= 0x8000;
			// Prepare payload
			if (hdr instanceof WsCloseFrame) {
				payloadSize = 0;
				((WsCloseFrame)hdr).statusCode().ifPresent(code -> {
					convData.clear();
					try {
						convData.write(code >> 8);
						convData.write(code & 0xff);
						payloadSize = 2;
					} catch (IOException e) {
						// Formally thrown, cannot happen
					}
				});
				((WsCloseFrame)hdr).reason().ifPresent(reason -> {
					payloadSize = convTextData(CharBuffer.wrap(reason));
				});
			} else if (hdr instanceof WsDefaultControlFrame) {
				payloadSize = ((WsDefaultControlFrame)hdr)
						.applicationData().map(ByteBuffer::remaining).orElse(0);
			}
		}
		
		// Finally add mask bit
		if (doMask) {
			headerHead |= 0x80;
			randoms.nextBytes(maskingKey);
		}

		// Code payload size
		if (payloadSize <= 125) {
			headerHead |= payloadSize;
			payloadBytes = 0;
		} else if (payloadSize < 0x10000) {
			headerHead |= 126;
			payloadBytes = 2;
		} else {
			headerHead |= 127;
			payloadBytes = 8;
		}
	}

	private long convTextData(Buffer in) {
		convData.setOverflowBufferSize(
				(int) (in.remaining() * bytesPerCharUtf8));
		try {
			OutputStreamWriter charWriter = new OutputStreamWriter(
			        convData, "utf-8");
			if (in.hasArray()) {
				// more efficient than CharSequence
				charWriter.write(((CharBuffer) in).array(),
				        in.arrayOffset() + in.position(),
				        in.remaining());
			} else {
				charWriter.append((CharBuffer) in);
			}
			// "in" is consumed, but don't move the position
			// until all data has been processed (from convData).
			charWriter.flush();
			return convData.bytesWritten();
		} catch (IOException e) {
			// Formally thrown, cannot happen
			return 0;
		}
	}
	
	private Result nextAfterLength(boolean endOfInput) {
		if (doMask) {
			bytesToSend = 4;
			state = State.WRITING_MASK;
			return null;
		}
		return nextAfterMask(endOfInput);
	}
	
	private Result nextAfterMask(boolean endOfInput) {
		if (payloadSize == 0) {
			return frameFinished(endOfInput);
		}
		maskIndex = 0;
		bytesToSend = payloadSize;
		state = State.WRITING_PAYLOAD;
		return null;
	}
	
	/**
	 * Copy payload to "out". Note that if we have textual data
	 * or a close frame, data has already been written into
	 * convData (see {@link #prepareHeaderHead(Buffer, boolean)}.
	 *
	 * @param in the input data, unless already wriiten to convData 
	 * @param out the out
	 */
	private void outputPayload(Buffer in, ByteBuffer out) {
		// Default is to use data directly from in buffer.
		Buffer src = in;
		WsFrameHeader hdr = messageHeaders.peek();
		boolean textPayload = (hdr instanceof WsMessageHeader) 
				&& ((WsMessageHeader)hdr).isTextMode();
		if (textPayload || (hdr instanceof WsCloseFrame)) {
			// Data has been put into convData
			if (!doMask) {
				// Moves data from temporary buffers to "out"
				convData.assignBuffer(out);
			} else {
				// Retrieve into src as much as fits in 
				// out buffer for masking.
				src = ByteBuffer.allocate(out.remaining());
				convData.assignBuffer((ByteBuffer)src);
				src.flip();
			}
			if (convData.remaining() >= 0 && textPayload) {
				// Make full consumption visible "outside",
				// see convTextData.
				in.position(in.limit());
			}
			if (!doMask) {
				return;
			}
		} else {
			if (hdr instanceof WsDefaultControlFrame) {
				// Data is taken from control frame.
				src = ((WsDefaultControlFrame)hdr)
						.applicationData().orElse(Codec.EMPTY_IN);
			}
			if (!doMask) {
				ByteBufferUtils.putAsMuchAsPossible(out, (ByteBuffer) src);
				return;
			}
		}
		// Mask while writing
		while (bytesToSend > 0
		        && src.hasRemaining() && out.hasRemaining()) {
			out.put((byte) (((ByteBuffer) src)
			        .get() ^ maskingKey[maskIndex]));
			maskIndex = (maskIndex + 1) % 4;
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.Decoder#getHeader()
	 */
	@Override
	public Optional<WsFrameHeader> header() {
		try {
			return Optional.of(messageHeaders.peek());
		} catch (EmptyStackException e) {
			return Optional.empty();
		}
	}

	/**
	 * Results from {@link HttpEncoder} provide no additional
	 * information compared to {@link org.jdrupes.httpcodec.Codec.Result}. This
	 * class only provides a factory for creating concrete results.
	 */
	public static class Result extends Codec.Result {
	
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection) {
			super(overflow, underflow, closeConnection);
		}

		/**
		 * A factory for creating new Results.
		 */
		protected static class Factory extends Codec.Result.Factory {

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
