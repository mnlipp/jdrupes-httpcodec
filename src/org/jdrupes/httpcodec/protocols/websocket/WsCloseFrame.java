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

package org.jdrupes.httpcodec.protocols.websocket;

import java.nio.CharBuffer;
import java.util.Optional;

/**
 * Represents a WebSocket close frame.
 * 
 * Note that status code and reason are modeled as part of the header in 
 * this API although they are handled like payload by the "wire protocol".
 */
public class WsCloseFrame extends WsFrameHeader {

	private Optional<Integer> statusCode;
	private Optional<String> reason;
	
	/**
	 * Creates a new close control frame.
	 * 
	 * @param statusCode the status code (if any)
	 * @param reason the reason (if any)
	 */
	public WsCloseFrame(Integer statusCode, CharBuffer reason) {
		super();
		this.statusCode = Optional.ofNullable(statusCode);
		this.reason = Optional.ofNullable(
				reason != null ? reason.toString() : null);
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.MessageHeader#hasPayload()
	 */
	@Override
	public boolean hasPayload() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.MessageHeader#isFinal()
	 */
	@Override
	public boolean isFinal() {
		return true;
	}

	/**
	 * @return the statusCode
	 */
	public Optional<Integer> statusCode() {
		return statusCode;
	}
	
	/**
	 * @return the reason
	 */
	public Optional<String> reason() {
		return reason;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WsCloseFrame [");
		if (statusCode != null) {
			builder.append("statusCode=");
			builder.append(statusCode);
			builder.append(", ");
		}
		if (reason != null) {
			builder.append("reason=");
			builder.append(reason.toString());
		}
		builder.append("]");
		return builder.toString();
	}
	
}
