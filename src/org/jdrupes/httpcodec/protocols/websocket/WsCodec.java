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

import org.jdrupes.httpcodec.Codec;

/**
 * 
 */
public abstract class WsCodec implements Codec {

	public enum ClosingState { OPEN, CLOSE_RECEIVED, CLOSE_SENT, CLOSED }

	private ClosingStateHolder csHolder = new ClosingStateHolder();

	protected void linkClosingState(WsCodec other) {
		csHolder = other.csHolder;
	}
	
	protected void setClosingState(ClosingState state) {
		csHolder.closingState = state;
	}
	
	public ClosingState closingState() {
		return csHolder.closingState;
	}
	
	private class ClosingStateHolder {
		public ClosingState closingState = ClosingState.OPEN;
	}
}
