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

import java.util.Arrays;

/**
 * Represents a value with a optional comments
 * such as `value (comment)`.
 * 
 * @param <U> the type of the uncommented value
 */
public class CommentedValue<U> {

	private static final String[] NO_COMMENTS = new String[0];
	
	private U value;
	private String[] comments;
	
	/**
	 * Creates a new object with the given value and no comment. 
	 * 
	 * @param value the value
	 */
	public CommentedValue(U value) {
		this (value, (String[])null);
	}

	/**
	 * Creates a new object with the given value and comment. 
	 * 
	 * @param value the value
	 * @param comment the comment (without parenthesis)
	 */
	public CommentedValue(U value, String comment) {
		this (value, new String[] { comment });
	}

	/**
	 * Creates a new object with the given value and comments. 
	 * 
	 * @param value the value
	 * @param comments the comments (without parenthesis)
	 */
	public CommentedValue(U value, String[] comments) {
		this.value = value;
		this.comments = comments;
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public U getValue() {
		return value;
	}

	/**
	 * Returns the comments (without parenthesis).
	 * 
	 * @return the comments 
	 */
	public String[] getComments() {
		if (comments == null) {
			return NO_COMMENTS;
		}
		return Arrays.copyOf(comments, comments.length);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CommentedValue [");
		if (value != null) {
			builder.append("value=");
			builder.append(value);
			builder.append(", ");
		}
		if (comments != null) {
			builder.append("comments=");
			builder.append(Arrays.toString(comments));
		}
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(comments);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		@SuppressWarnings("rawtypes")
		CommentedValue other = (CommentedValue) obj;
		if (!Arrays.equals(comments, other.comments)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	public static class CommentedValueConverter<U> 
		implements Converter<CommentedValue<U>> {

		private Converter<U> valueConverter;		
		
		/**
		 * Creates a new converter with the given value converter.
		 * 
		 * @param valueConverter the value converter
		 */
		public CommentedValueConverter(Converter<U> valueConverter) {
			super();
			this.valueConverter = valueConverter;
		}

		@Override
		public String asFieldValue(CommentedValue<U> value) {
			StringBuilder result = new StringBuilder();
			result.append(valueConverter.asFieldValue(value.getValue()));
			for (String comment: value.getComments()) {
				result.append(" (");
				result.append(Converters.quote(comment, "()\\"));
				result.append(')');
			}
			return result.toString();
		}

		/* (non-Javadoc)
		 * @see org.jdrupes.httpcodec.types.Converter#fromFieldValue(java.lang.String)
		 */
		@Override
		public CommentedValue<U> fromFieldValue(String text) {
			throw new UnsupportedOperationException();
		}
		
	}
}