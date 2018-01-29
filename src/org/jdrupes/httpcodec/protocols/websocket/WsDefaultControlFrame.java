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

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Control frames with binary application data.
 * 
 * Note that the application data is modeled as part of the header in the API
 * although it is handled like payload by the "wire protocol".
 */
public abstract class WsDefaultControlFrame extends WsFrameHeader {

	private Optional<ByteBuffer> applicationData;
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.MessageHeader#isFinal()
	 */
	@Override
	public boolean isFinal() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.MessageHeader#hasPayload()
	 */
	@Override
	public boolean hasPayload() {
		return false;
	}

	/**
	 * Creates a new frame.
	 * 
	 * @param applicationData the application data. May be {@code null}.
	 */
	public WsDefaultControlFrame(ByteBuffer applicationData) {
		this.applicationData = Optional.ofNullable(applicationData);
	}

	/**
	 * 
	 * @return the application data
	 */
	public Optional<ByteBuffer> applicationData() {
		return applicationData;
	}
}
