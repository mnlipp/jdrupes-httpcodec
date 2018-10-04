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

package org.jdrupes.httpcodec;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.jdrupes.httpcodec.Codec.ProtocolSwitchResult;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;

/**
 * An engine that can be used as a client. It has an associated
 * request encoder and a response decoder. Using a {@link ClientEngine}
 * has two main advantages over using an encoder and decoder
 * directly. It invokes the 
 * {@link ResponseDecoder#decodeResponseTo(MessageHeader)} when
 * appropriate and it replaces the encoder and decoder if the
 * decoded result indicates a switch. The change takes place upon
 * the next `encode` or `decode` invocation. The "old" encoders
 * and decoders are therefore still available when the result of
 * a decode invocation indicates a switch.
 * 
 * @param <Q> the message header type handled be the encoder (the request)
 * @param <R> the message header type handled by the decoder (the response)
 */
public class ClientEngine<Q extends MessageHeader, 
	R extends MessageHeader> extends Engine {

	private Encoder<?> requestEncoder;
	private ResponseDecoder<?, ?> responseDecoder;
	private Encoder<?> newRequestEncoder;
	private ResponseDecoder<?, ?> newResponseDecoder;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param requestEncoder the encoder for the request
	 * @param responseDecoder the decoder for the response
	 */
	public ClientEngine(Encoder<Q> requestEncoder, 
			ResponseDecoder<R, Q> responseDecoder) {
		this.requestEncoder = requestEncoder;
		this.responseDecoder = responseDecoder;
	}
	
	/**
	 * @return the requestEncoder
	 */
	@SuppressWarnings("unchecked")
	public Encoder<Q> requestEncoder() {
		return (Encoder<Q>)requestEncoder;
	}
	
	/**
	 * @return the responseDecoder
	 */
	@SuppressWarnings("unchecked")
	public ResponseDecoder<R,Q> responseDecoder() {
		return (ResponseDecoder<R,Q>)responseDecoder;
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param out the buffer to use for the result
	 * @return the result
	 */
	public Codec.Result encode(ByteBuffer out) {
		return requestEncoder.encode(out);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param in the buffer with the data to encode
	 * @param out the buffer to use for the result
	 * @param endOfInput {@code true} if end of input
	 * @return the result
	 */
	public Codec.Result encode(Buffer in, ByteBuffer out, boolean endOfInput) {
		return requestEncoder.encode(in, out, endOfInput);
	}

	/**
	 * Convenience method to invoke the encoder's encode method.
	 * 
	 * @param messageHeader the message header
	 */
	@SuppressWarnings("unchecked")
	public void encode(Q messageHeader) {
		if (newRequestEncoder != null) {
			requestEncoder = newRequestEncoder;
			newRequestEncoder = null;
		}
		((Encoder<Q>)requestEncoder).encode(messageHeader);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @param request the request
	 */
	@SuppressWarnings("unchecked")
	public void decodeResponseTo(Q request) {
		((ResponseDecoder<R,Q>)responseDecoder).decodeResponseTo(request);
	}

	/**
	 * Convenience method to invoke the decoder's decode method.
	 * 
	 * @param in the buffer with the data to decode
	 * @param out the buffer to use for the result
	 * @param endOfInput {@code true} if end of input
	 * @return the result
	 * @throws ProtocolException if the input violates the protocol
	 * @see HttpResponseDecoder#decode(java.nio.ByteBuffer, java.nio.Buffer, boolean)
	 */
	@SuppressWarnings("unchecked")
	public Decoder.Result<Q> decode(
	        ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		if (newResponseDecoder != null) {
			responseDecoder = newResponseDecoder;
			newResponseDecoder = null;
		}
		if (responseDecoder.isAwaitingMessage()) {
			((Encoder<Q>)requestEncoder).header().ifPresent(request ->  
				((ResponseDecoder<R,Q>)responseDecoder).decodeResponseTo(request));
		}
		Decoder.Result<Q> result 
			= (Decoder.Result<Q>)responseDecoder.decode(in, out, endOfInput);
		if (result instanceof ProtocolSwitchResult) {
			ProtocolSwitchResult res = (ProtocolSwitchResult)result;
			if (res.newProtocol() != null) {
				setSwitchedTo(res.newProtocol());
				newResponseDecoder = (ResponseDecoder<?,?>)res.newDecoder();
				newRequestEncoder = res.newEncoder();
			}
		}
		return result;
	}
	
	/**
	 * Returns the last encoded request.
	 * 
	 * @return the request
	 */
	@SuppressWarnings("unchecked")
	public Optional<Q> currentRequest() {
		return (Optional<Q>)requestEncoder.header();
	}
	
}
