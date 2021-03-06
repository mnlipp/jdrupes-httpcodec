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

package org.jdrupes.httpcodec.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * A decoder for URL-encoded form data.
 */
public class FormUrlDecoder {

	private Map<String,String> fields = new HashMap<>();
	private String rest = "";

	/**
	 * Add the data in the buffer to the form data. May be invoked
	 * several times if the form data is split across several buffers.
	 * 
	 * @param buf the buffer with the data
	 */
	public void addData(ByteBuffer buf) {
		try {
			String data;
			if (buf.hasArray()) {
				data = rest + new String(buf.array(),
						buf.arrayOffset() + buf.position(), buf.remaining(), "ascii");
			} else {
				byte[] bc = new byte[buf.remaining()];
				buf.get(bc);
				data = rest + new String(bc, "ascii");
			}
			buf.position(buf.limit()); // for consistency
			int oldPos = 0;
			while (true) {
				int newPos = data.indexOf('&', oldPos);
				if (newPos < 0) {
					rest = data.substring(oldPos);
					break;
				}
				split(data, oldPos, newPos);
				oldPos = newPos + 1;
			}
		} catch (UnsupportedEncodingException e) {
			// Using only built-in encodings
			e.printStackTrace();
		}
	}

	private void split(String pairString, int pairStart, int pairEnd) {
		int eqPos = pairString.indexOf('=', pairStart);
		if (eqPos < 0) {
			return;
		}
		try {
			fields.put(URLDecoder.decode(pairString.substring(pairStart, eqPos),
			        "utf-8"),
			        URLDecoder.decode(pairString.substring(eqPos + 1, pairEnd),
			                "utf-8"));
		} catch (UnsupportedEncodingException e) {
			// Using only built-in encodings
		}
	}

	/**
	 * Return the fields decoded from the data that has been added
	 * by {@link #addData(ByteBuffer)}. Invoking this method terminates
	 * the decoding, i.e. {@link #addData(ByteBuffer)} should not be
	 * called again after this method has been invoked.
	 * 
	 * @return the decoded fields
	 */
	public Map<String,String> fields() {
		split(rest, 0, rest.length());
		rest = "";
		return fields;
	}
}
