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
import java.util.HashMap;
import java.util.Map;

public class MediaType extends ParameterizedValue<MediaType.MediaMainType> {

	public static final MediaType ALL_MEDIA = new MediaType("*", "*");
	
	/**
	 * Create a new object with the given type and subtype.
	 * 
	 * @param type the top-level type
	 * @param subtype the subtype
	 */
	public MediaType(String type, String subtype) {
		super(new MediaMainType(type, subtype), Collections.emptyMap());
	}
	
	/**
	 * Create a new object with the given type, subtype and parameters.
	 * 
	 * @param type the top-level type
	 * @param subtype the subtype
	 * @param parameters the parameters
	 */
	public MediaType(String type, String subtype, 
			Map<String, String> parameters) {
		super(new MediaMainType(type, subtype), parameters);
	}

	/**
	 * Create a new object with the given type and parameters.
	 * 
	 * @param type the type
	 * @param parameters the parameters
	 */
	public MediaType(MediaMainType type, Map<String, String> parameters) {
		super(type, parameters);
	}

	/**
	 * For builder.
	 */
	private MediaType() {
		super(ALL_MEDIA.getValue(), Collections.emptyMap());
	}
	
	/**
	 * Creates a new instance with values obtained from parsing
	 * the given text.
	 * 
	 * @param text the text
	 * @return the mime type
	 * @throws ParseException if the text is not well-formed
	 */
	public static MediaType fromString(String text) 
			throws ParseException {
		return Converters.MEDIA_TYPE_CONVERTER.fromFieldValue(text);
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
	 * Creates a new builder for a media type.
	 * 
	 * @return the builder
	 */
	@SuppressWarnings("unchecked")
	public static Builder builder() {
		return new Builder();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Converters.MEDIA_TYPE_CONVERTER.asFieldValue(this);
	}


	/**
	 * A builder for the (immutable) parameterized type.
	 */
	public static class Builder 
		extends ParameterizedValue.Builder<MediaMainType> {

		private Builder() {
			super(new MediaType("*", "*", new HashMap<>()));
		}
		
		@Override
		public MediaType build() {
			return (MediaType)super.build();
		}

		public Builder from(MediaType existing) {
			return (Builder)super.from(existing);
		}

		@Override
		public Builder setValue(MediaMainType value) {
			return (Builder)super.setValue(value);
		}

		@Override
		public Builder setParameter(String name, String value) {
			return (Builder)super.setParameter(name, value);
		}

		@Override
		public Builder remove(String name) {
			return (Builder)super.remove(name);
		}

		/**
		 * Sets the media type.
		 * 
		 * @param topLevelType the top level type
		 * @param subtype the subtype
		 * @return the builder for easy chaining
		 */
		public Builder setType(String topLevelType, String subtype) {
			setValue(new MediaMainType(topLevelType, subtype));
			return this;
		}
	}
	
	/**
	 * Represents a media type, including a media range (type or subtype
	 * equals "`*`").
	 */
	public static class MediaMainType {
		
		private String topLevelType;
		private String subtype;
		
		/**
		 * Create a new object with the given type and subtype.
		 * 
		 * @param type the type
		 * @param subtype the subtype
		 */
		public MediaMainType(String type, String subtype) {
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
		public static MediaMainType fromString(String text) 
				throws ParseException {
			int sepPos = text.indexOf('/');
			if (sepPos <= 0 || sepPos > text.length() - 2) {
				throw new ParseException(
						"Format not \"type/subtype\": " + text, 0);
			}
			return new MediaMainType(text.substring(0, sepPos),
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
			MediaType.MediaMainType other = (MediaType.MediaMainType) obj;
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
}