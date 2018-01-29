/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2018 Michael N. Lipp
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

/**
 * A special close frame created by the decoder. It is passed to the invoker as
 * part of the result. The invoker forwards it to the encoder, which knows
 * -- because of the type -- that this is a close confirmation (not a close
 * initiated by this endpoint).
 */
public class WsCloseResponse extends WsCloseFrame {

	WsCloseResponse(Integer statusCode) {
		super(statusCode, null);
	}

}
