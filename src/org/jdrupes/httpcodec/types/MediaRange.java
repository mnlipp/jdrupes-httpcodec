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
import java.util.HashMap;
import java.util.Map;

public class MediaRange extends MediaBase implements Comparable<MediaRange> {

	public static final MediaRange ALL_MEDIA = new MediaRange("*", "*");

	/**
	 * Create a new object with the given type and subtype.
	 * 
	 * @param type the top-level type
	 * @param subtype the subtype
	 */
	public MediaRange(String type, String subtype) {
		super(new MediaTypePair(type, subtype));
	}
	
	/**
	 * Create a new object with the given type, subtype and parameters.
	 * 
	 * @param type the top-level type
	 * @param subtype the subtype
	 * @param parameters the parameters
	 */
	public MediaRange(String type, String subtype, 
			Map<String, String> parameters) {
		super(new MediaTypePair(type, subtype), parameters);
	}

	/**
	 * Create a new object with the given type and parameters.
	 * 
	 * @param type the type
	 * @param parameters the parameters
	 */
	public MediaRange(MediaTypePair type, Map<String, String> parameters) {
		super(type, parameters);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MediaRange other) {
		float myQuality = 1;
		String param = parameter("q");
		if (param != null) {
			myQuality = Float.parseFloat(param);
		}
		float otherQuality = 1;
		param = other.parameter("q");
		if (param != null) {
			otherQuality = Float.parseFloat(param);
		}
		if (myQuality != otherQuality) {
			return -(int)Math.signum(myQuality - otherQuality);
		}
		
		// Same or no quality, look for wildcards
		if (!subtype().equals("*") && other.subtype().equals("*")) {
			return -1;
		}
		if (subtype().equals("*") && !other.subtype().equals("*")) {
			return 1;
		}
		if (!topLevelType().equals("*") 
				&& other.topLevelType().equals("*")) {
			return -1;
		}
		if (topLevelType().equals("*")
				&& !other.topLevelType().equals("*")) {
			return 1;
		}
		
		// No wildcards or same type, look for number of parameters
		return countParameters(other)- countParameters(this);
	}

	private static int countParameters(ParameterizedValue<?> value) {
		return (int)value.parameters().keySet().stream()
				.filter(k -> !k.equals("q")).count();
	}
	
	/**
	 * Checks if the given media type falls within this range.
	 * 
	 * @param type the type to check
	 * @return the result
	 */
	public boolean matches(MediaType type) {
		if (!("*".equals(topLevelType()) 
				|| topLevelType().equals(type.topLevelType()))) {
			// Top level is neither * nor match
			return false;
		}
		if (!("*".equals(subtype())
				|| subtype().equals(type.subtype()))) {
			// Subtype is neither * nor match
			return false;
		}
		for (Map.Entry<String,String> e: parameters().entrySet()) {
			if ("q".equals(e.getKey())) {
				continue;
			}
			if (!type.parameters().containsKey(e.getKey())) {
				return false;
			}
			if (!type.parameter(e.getKey()).equals(e.getValue())) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Creates a new builder for a media type.
	 * 
	 * @return the builder
	 */
	@SuppressWarnings("unchecked")
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * A builder for the (immutable) parameterized type.
	 */
	public static class Builder 
		extends ParameterizedValue.Builder<MediaRange, MediaTypePair> {

		private Builder() {
			super(new MediaRange("*", "*", new HashMap<>()));
		}
		
		/**
		 * Sets the media range.
		 * 
		 * @param topLevelType the top level type
		 * @param subtype the subtype
		 * @return the builder for easy chaining
		 */
		public Builder setType(String topLevelType, String subtype) {
			setValue(new MediaTypePair(topLevelType, subtype));
			return this;
		}
	}
	
	public static class MediaRangeConverter
	        extends ParamValueConverterBase<MediaRange, MediaTypePair> {

		public MediaRangeConverter() {
			super(new MediaTypePairConverter() {
				
				/**
				 * Work around faulty clients, notably
				 * `HttpUrlConnection`.
				 */
				@Override
				public MediaTypePair fromFieldValue(String text)
				        throws ParseException {
					if ("*".equals(text)) {
						return MediaTypePair.ALL_MEDIA;
					}
					return super.fromFieldValue(text);
				}
			},
			Converters.UNQUOTE_ONLY, MediaRange::new);
		}
	}

}