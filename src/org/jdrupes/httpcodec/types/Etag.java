/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2017 Michael N. Lipp
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

package org.jdrupes.httpcodec.types;

import java.text.ParseException;

/**
 * Represents an ETag.
 */
public class Etag {

	private String tag;
	private boolean weak;
	
	/**
	 * Create a new ETag.
	 * 
	 * @param tag the tag
	 * @param weak if the tag is weak
	 */
	public Etag(String tag, boolean weak) {
		this.tag = tag;
		this.weak = weak;
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @return the weak
	 */
	public boolean isWeak() {
		return weak;
	}

	public static class EtagConverter implements Converter<Etag> {

		@Override
		public String asFieldValue(Etag value) {
			if (value.isWeak()) {
				return "W/" + Converters.quoteString(value.getTag());
			}
			if ("*".equals(value)) {
				return Converters.WILDCARD;
			}
			return Converters.quoteString(value.getTag());
		}

		@Override
		public Etag fromFieldValue(String text) throws ParseException {
			if (!text.startsWith("W/")) {
				return new Etag(Converters.unquoteString(text), false);
			}
			return new Etag(Converters.unquoteString(text.substring(2)), true);
		}
		
	}
}
