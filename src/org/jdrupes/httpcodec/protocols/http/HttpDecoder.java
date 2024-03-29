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

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.text.ParseException;
import java.util.Optional;
import java.util.Stack;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.ProtocolException;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.MultiValueConverter;
import org.jdrupes.httpcodec.types.StringList;
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
public abstract class HttpDecoder<T extends HttpMessageHeader,
        R extends HttpMessageHeader>
        extends HttpCodec<T> implements Decoder<T, R> {

    protected static final String SP = "[ \\t]+";
    protected static final String HTTP_VERSION = "HTTP/\\d+\\.\\d";

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
    protected Encoder<R, T> peerEncoder;

    /**
     * Creates a new decoder.
     */
    public HttpDecoder() {
        states.push(State.AWAIT_MESSAGE_START);
        states.push(State.RECEIVE_LINE);
    }

    public Decoder<T, R> setPeerEncoder(Encoder<R, T> encoder) {
        peerEncoder = encoder;
        return this;
    }

    public boolean isAwaitingMessage() {
        return states.size() > 0
            && states.get(0) == State.AWAIT_MESSAGE_START;
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
    public long maxHeaderLength() {
        return maxHeaderLength;
    }

    /**
     * Returns the message (header) if one exists.
     * 
     * @return the result
     */
    public Optional<T> header() {
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
            throws ProtocolException;

    /**
     * Informs the derived class that the header has been received completely.
     * 
     * @param message the message
     * @return indication how the body will be transferred
     * @throws HttpProtocolException if the input violates the HTTP
     */
    protected abstract BodyMode headerReceived(T message)
            throws ProtocolException;

    /**
     * Informs the derived class that a complete message has been received
     * and the given result will be returned. The derived class may take
     * additional actions and even modify the result. The default
     * implementation simply returns the given result.
     */
    protected Decoder.Result<R> messageComplete(Decoder.Result<R> result) {
        return result;
    }

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
     * @throws ProtocolException
     *             if the message violates the Protocol
     */
    public Decoder.Result<R> decode(ByteBuffer in, Buffer out,
            boolean endOfInput) throws ProtocolException {
        try {
            try {
                return uncheckedDecode(in, out, endOfInput);
            } catch (ParseException | NumberFormatException e) {
                throw new HttpProtocolException(protocolVersion,
                    HttpStatus.BAD_REQUEST.statusCode(), e.getMessage());
            }
        } catch (HttpProtocolException e) {
            states.clear();
            states.push(State.CLOSED);
            throw e;
        }
    }

    private Decoder.Result<R> uncheckedDecode(
            ByteBuffer in, Buffer out, boolean endOfInput)
            throws ProtocolException, ParseException {
        while (true) {
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
                        HttpStatus.BAD_REQUEST.statusCode(),
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
                    HttpStatus.BAD_REQUEST.statusCode(),
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
                    if (!messageHeader.hasPayload()) {
                        return adjustToEndOfMessage();
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
                if (out instanceof CharBuffer && charDecoder != null) {
                    states.push(State.FINISH_CHARDECODER);
                    break;
                }
                states.pop();
                return adjustToEndOfMessage();

            case CHUNK_START_RECEIVED:
                // We "drop" to this state when a line has been read
                String sizeText = receivedLine.split(";")[0];
                long chunkSize = Long.parseLong(sizeText, 16);
                if (chunkSize == 0) {
                    states.pop();
                    states.push(State.CHUNK_TRAILER_LINE_RECEIVED);
                    states.push(State.RECEIVE_LINE);
                    if (out instanceof CharBuffer && charDecoder != null) {
                        states.push(State.FINISH_CHARDECODER);
                    }
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
                        HttpStatus.BAD_REQUEST.statusCode(),
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
                return adjustToEndOfMessage();

            case COPY_SPECIFIED:
                // If we get here, leftToRead is greater zero.
                int initiallyRemaining = in.remaining();
                if (out == null) {
                    return resultFactory().newResult(true,
                        initiallyRemaining <= 0);
                }
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
                if (charDecoder.flush((CharBuffer) out).isOverflow()) {
                    return resultFactory().newResult(true, false);
                }
                // No longer needed (and no longer usable btw)
                charDecoder = null;
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
        HttpField<?> field;
        try {
            field = new HttpField<>(headerLine, Converters.STRING);
        } catch (ParseException e) {
            throw new HttpProtocolException(protocolVersion,
                HttpStatus.BAD_REQUEST.statusCode(), "Invalid header");
        }
        if (field.name().equalsIgnoreCase(HttpField.SET_COOKIE)) {
            field
                = new HttpField<CookieList>(headerLine, Converters.SET_COOKIE);
        }
        switch (field.name()) {
        case HttpField.CONTENT_LENGTH:
            // RFC 7230 3.3.3 (3.)
            if (building.fields()
                .containsKey(HttpField.TRANSFER_ENCODING)) {
                field = null;
                break;
            }
            // RFC 7230 3.3.3 (4.)
            Optional<HttpField<Long>> existing = building.findField(
                HttpField.CONTENT_LENGTH, Converters.LONG);
            if (existing.isPresent()) {
                @SuppressWarnings("unchecked")
                HttpField<Long> newLength = (HttpField<Long>) field;
                if (!existing.get().value().equals(newLength.value())) {
                    throw new HttpProtocolException(protocolVersion,
                        HttpStatus.BAD_REQUEST);
                }
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
        HttpField<?> field;
        try {
            field = new HttpField<>(headerLine, Converters.STRING);
        } catch (ParseException e) {
            throw new HttpProtocolException(protocolVersion,
                HttpStatus.BAD_REQUEST.statusCode(), "Invalid header");
        }
        // RFC 7230 4.4
        HttpField<StringList> trailerField = messageHeader
            .computeIfAbsent(HttpField.TRAILER, Converters.STRING_LIST,
                StringList::new);
        if (!trailerField.value().containsIgnoreCase(field.name())) {
            trailerField.value().add(field.name());
        }
        addHeaderField(messageHeader, field);
    }

    private void addHeaderField(T header, HttpField<?> field)
            throws HttpProtocolException, ParseException {
        // RFC 7230 3.2.2
        var exists = header.findField(field.name(),
            HttpField.lookupConverter(field.name()));
        if (exists.isPresent()) {
            var existing = exists.get();
            // Duplicate field name is only allowed for value lists
            if (!(existing.converter() instanceof MultiValueConverter)) {
                throw new HttpProtocolException(protocolVersion,
                    HttpStatus.BAD_REQUEST.statusCode(),
                    "Multiple occurences of single value field "
                        + field.name());
            }
            @SuppressWarnings("unchecked")
            var converter = (MultiValueConverter<Iterable<Object>,
                    Object>) existing.converter();
            HttpField<?> srcField = field;
            if (field.converter().equals(Converters.STRING)) {
                // Still default (String), use real converter (same as existing)
                var converted = new HttpField<>(field.name(),
                    converter.fromFieldValue((String) field.asFieldValue()),
                    converter);
                srcField = converted;
            }
            var adder = converter.valueAdder();
            @SuppressWarnings("unchecked")
            Iterable<Object> source = (Iterable<Object>) srcField.value();
            @SuppressWarnings("unchecked")
            Iterable<Object> target = (Iterable<Object>) existing.value();
            source.forEach(item -> adder.accept(target, item));
        } else {
            header.setField(field);
        }
    }

    private void adjustToBodyMode(BodyMode bm) {
        states.pop();
        switch (bm) {
        case UNTIL_CLOSE:
            states.push(State.COPY_UNTIL_CLOSED);
            building.setHasPayload(true);
            break;
        case CHUNKED:
            states.push(State.CHUNK_START_RECEIVED);
            states.push(State.RECEIVE_LINE);
            building.setHasPayload(true);
            break;
        case LENGTH:
            HttpField<Long> clf = building.findField(
                HttpField.CONTENT_LENGTH, Converters.LONG).get();
            leftToRead = clf.value();
            if (leftToRead > 0) {
                states.push(State.LENGTH_RECEIVED);
                states.push(State.COPY_SPECIFIED);
                building.setHasPayload(true);
                break;
            }
            // Length == 0 means no body, fall through
        case NO_BODY:
            building.setHasPayload(false);
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
                return charDecoder.decode(in, (CharBuffer) out, endOfInput);
            } finally {
                in.limit(oldLimit);
            }
        } else {
            throw new IllegalArgumentException(
                "Only Byte- or CharBuffer are allowed.");
        }
    }

    private Decoder.Result<R> adjustToEndOfMessage() {
        // RFC 7230 6.3
        Optional<HttpField<StringList>> connection = messageHeader
            .findField(HttpField.CONNECTION, Converters.STRING_LIST);
        if (connection.isPresent() && connection.get().value()
            .stream().anyMatch(s -> s.equalsIgnoreCase("close"))) {
            states.push(State.CLOSED);
            return messageComplete(resultFactory().newResult(false, false));
        }
        if (messageHeader.protocol().compareTo(HttpProtocol.HTTP_1_1) >= 0) {
            states.push(State.AWAIT_MESSAGE_START);
            states.push(State.RECEIVE_LINE);
            return messageComplete(resultFactory().newResult(false, false));
        }
        states.push(State.CLOSED);
        return messageComplete(resultFactory().newResult(false, false));
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
            super(overflow, underflow, closeConnection, headerCompleted,
                response,
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
             * {@link org.jdrupes.httpcodec.Codec.Result#closeConnection()}
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
