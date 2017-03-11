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

package org.jdrupes.httpcodec.protocols.http.fields;

import java.io.IOException;
import java.io.ObjectInput;
import java.text.ParseException;

import javax.activation.MimeType;
import javax.activation.MimeTypeParameterList;
import javax.activation.MimeTypeParseException;

/**
 * Represents a media type header field and provides methods for interpreting
 * its value.
 */
public class HttpMediaTypeField extends HttpField<MimeType>
	implements Cloneable {

	public static final Converter<MimeType> CONVERTER 
		= new Converter<MimeType>() {

		@Override
		public String asFieldValue(MimeType value) {
			return value.toString();
		}

		@Override
		public MimeType fromFieldValue(String text) throws ParseException {
			try {
				return new RestrictedMimeType(text);
			} catch (MimeTypeParseException e) {
				throw new ParseException(text, 0);
			}
		}
	};

	/**
	 * Creates new header field object with the given type and subtype and no
	 * parameters.
	 * 
	 * @param name
	 *            the field name
	 * @param type
	 *            the type
	 * @param subtype
	 *            the sub type
	 * @throws ParseException if the input violates the field format
	 */
	public HttpMediaTypeField(String name, String type, String subtype) 
			throws ParseException {
		super(name, createMimeType(type, subtype), CONVERTER);
	}

	private static RestrictedMimeType createMimeType(
			String type, String subtype)
		throws ParseException {
		try {
			return new RestrictedMimeType(type, subtype);
		} catch (MimeTypeParseException e) {
			throw new ParseException(e.getMessage(), 0);
		}
		
	}
	
	/**
	 * Creates new header field object with the name and type.
	 * 
	 * @param name
	 *            the field name
	 * @param type
	 *            the type
	 */
	public HttpMediaTypeField(String name, MimeType type) {
		super(name, type, CONVERTER);
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpField#clone()
	 */
	@Override
	public HttpMediaTypeField clone() {
		return (HttpMediaTypeField)super.clone();
	}

	/**
	 * Creates a new representation of a media type field value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpMediaTypeField fromString(String name, String value)
	        throws ParseException {
		return new HttpMediaTypeField(name, CONVERTER.fromFieldValue(value));
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpField#cloneValue()
	 */
	@Override
	protected MimeType cloneValue() {
		try {
			return new MimeType(getValue().toString());
		} catch (MimeTypeParseException e) {
			// Would be strange, indeed.
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * @return the result
	 * @see javax.activation.MimeType#getBaseType()
	 */
	public String getBaseType() {
		return getValue().getBaseType();
	}

	/**
	 * @param name the name
	 * @return the result
	 * @see javax.activation.MimeType#getParameter(java.lang.String)
	 */
	public String getParameter(String name) {
		return getValue().getParameter(name);
	}

	/**
	 * @return the result
	 * @see javax.activation.MimeType#getParameters()
	 */
	public MimeTypeParameterList getParameters() {
		return getValue().getParameters();
	}

	/**
	 * @return the result
	 * @see javax.activation.MimeType#getPrimaryType()
	 */
	public String getPrimaryType() {
		return getValue().getPrimaryType();
	}

	/**
	 * @return the result
	 * @see javax.activation.MimeType#getSubType()
	 */
	public String getSubType() {
		return getValue().getSubType();
	}

	/**
	 * @param type the type
	 * @return the result
	 * @see javax.activation.MimeType#match(javax.activation.MimeType)
	 */
	public boolean match(MimeType type) {
		return getValue().match(type);
	}

	/**
	 * @param rawdata the data
	 * @return the result
	 * @throws MimeTypeParseException if an error occurred
	 * @see javax.activation.MimeType#match(java.lang.String)
	 */
	public boolean match(String rawdata) throws MimeTypeParseException {
		return getValue().match(rawdata);
	}

	/**
	 * @param name the name
	 * @see javax.activation.MimeType#removeParameter(java.lang.String)
	 */
	public void removeParameter(String name) {
		getValue().removeParameter(name);
	}

	/**
	 * @param name the name
	 * @param value the value
	 * @see javax.activation.MimeType#setParameter(java.lang.String, java.lang.String)
	 */
	public void setParameter(String name, String value) {
		getValue().setParameter(name, value);
	}

	private static class RestrictedMimeType extends MimeType {
		
		public RestrictedMimeType(String primary, String sub)
		        throws MimeTypeParseException {
			super(primary, sub);
		}

		public RestrictedMimeType(String rawdata)
		        throws MimeTypeParseException {
			super(rawdata);
		}

		@Override
		public void readExternal(ObjectInput in)
		        throws IOException, ClassNotFoundException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPrimaryType(String primary)
		        throws MimeTypeParseException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setSubType(String sub)
		        throws MimeTypeParseException {
			throw new UnsupportedOperationException();
		}
	}
	
}
