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
import java.util.Collections;
import java.util.Map;

public abstract class MediaBase extends ParameterizedValue<MediaBase.MediaTypePair> {

	/**
	 * Create a new object with the given type and no parameters.
	 * 
	 * @param type the type
	 */
	protected MediaBase(MediaTypePair type) {
		super(type, Collections.emptyMap());
	}

	/**
	 * Create a new object with the given type and parameters.
	 * 
	 * @param type the type
	 * @param parameters the parameters
	 */
	protected MediaBase(MediaTypePair type, Map<String, String> parameters) {
		super(type, parameters);
	}

	/**
	 * Returns the top-level type.
	 * 
	 * @return the type
	 */
	public String getTopLevelType() {
		return getValue().topLevelType;
	}
	
	/**
	 * Returns the subtype
	 * 
	 * @return the subtype
	 */
	public String getSubtype() {
		return getValue().subtype;
	}

	/**
	 * Represents the tuple media top-level type and subtype, 
	 * including a media range (type or subtype
	 * equals "`*`").
	 */
	public static class MediaTypePair {
		
		public static final MediaTypePair ALL_MEDIA 
			= new MediaTypePair("*", "*");
		
		private String topLevelType;
		private String subtype;
		
		/**
		 * Create a new object with the given type and subtype.
		 * 
		 * @param type the type
		 * @param subtype the subtype
		 */
		public MediaTypePair(String type, String subtype) {
			super();
			this.topLevelType = type.toLowerCase();
			this.subtype = subtype.toLowerCase();
		}
		
		/**
		 * Create a main type from its textual representation. 
		 * 
		 * @param text the textual representation
		 * @return the result
		 * @throws ParseException if the text is ill-formed
		 */
		public static MediaTypePair fromString(String text) 
				throws ParseException {
			int sepPos = text.indexOf('/');
			if (sepPos <= 0 || sepPos > text.length() - 2) {
				throw new ParseException(
						"Format not \"type/subtype\": " + text, 0);
			}
			return new MediaTypePair(text.substring(0, sepPos),
					text.substring(sepPos + 1));
			
		}
		
		/**
		 * Returns the top-level type.
		 * 
		 * @return the type
		 */
		public String getTopLevelType() {
			return topLevelType;
		}
		
		/**
		 * Returns the subtype
		 * 
		 * @return the subtype
		 */
		public String getSubtype() {
			return subtype;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return topLevelType + "/" + subtype;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
			        + ((subtype == null) ? 0 : subtype.hashCode());
			result = prime * result + ((topLevelType == null) ? 0 : topLevelType.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MediaBase.MediaTypePair other = (MediaBase.MediaTypePair) obj;
			if (subtype == null) {
				if (other.subtype != null) {
					return false;
				}
			} else if (!subtype.equals(other.subtype)) {
				return false;
			}
			if (topLevelType == null) {
				if (other.topLevelType != null) {
					return false;
				}
			} else if (!topLevelType.equals(other.topLevelType)) {
				return false;
			}
			return true;
		}
	}
	
	public static class MediaTypePairConverter
		implements Converter<MediaTypePair> {

		@Override
		public String asFieldValue(MediaTypePair value) {
			return value.toString();
		}

		@Override
		public MediaTypePair fromFieldValue(String text)
				throws ParseException {
			return MediaTypePair.fromString(text);
		}
	}

}