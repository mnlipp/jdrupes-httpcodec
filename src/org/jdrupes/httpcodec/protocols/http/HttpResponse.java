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

package org.jdrupes.httpcodec.protocols.http;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Optional;

import javax.activation.MimetypesFileTypeMap;

import static org.jdrupes.httpcodec.protocols.http.HttpConstants.*;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;

/**
 * Represents an HTTP response header.
 */
public class HttpResponse extends HttpMessageHeader {

	private static MimetypesFileTypeMap typesMap = new MimetypesFileTypeMap();
	
	private int statusCode = -1;
	private String reasonPhrase;
	private HttpRequest request;
	
	public HttpResponse(HttpProtocol protocol, HttpStatus status, 
			boolean hasPayload) {
		super(protocol, hasPayload);
		setStatus(status);
	}
	
	public HttpResponse(HttpProtocol protocol, int statusCode, 
			String reasonPhrase, boolean hasPayload) {
		super(protocol, hasPayload);
		setStatusCode(statusCode);
		setReasonPhrase(reasonPhrase);
	}
	
	/* (non-Javadoc)
	 * @see HttpMessageHeader#setField(org.jdrupes.httpcodec.fields.HttpField)
	 */
	@Override
	public HttpResponse setField(HttpField<?> value) {
		super.setField(value);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see HttpMessageHeader#setField(java.lang.String, java.lang.Object)
	 */
	@Override
	public <T> HttpResponse setField(String name, T value) {
		super.setField(name, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.HttpMessageHeader#setHasPayload(boolean)
	 */
	@Override
	public HttpResponse setHasPayload(boolean hasPayload) {
		super.setHasPayload(hasPayload);
		return this;
	}

	/**
	 * @return the responseCode
	 */
	public int statusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode the responseCode to set
	 * @return the response for easy chaining
	 */
	public HttpResponse setStatusCode(int statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	/**
	 * @return the reason phrase
	 */
	public String reasonPhrase() {
		return reasonPhrase;
	}

	/**
	 * @param reasonPhrase the reason phrase to set
	 * @return the response for easy chaining
	 */
	public HttpResponse setReasonPhrase(String reasonPhrase) {
		this.reasonPhrase = reasonPhrase;
		return this;
	}

	/**
	 * Sets both status code and reason phrase from the given 
	 * http status value.
	 * 
	 * @param status the status value
	 * @return the response for easy chaining
	 */
	public HttpResponse setStatus(HttpStatus status) {
		statusCode = status.statusCode();
		reasonPhrase = status.reasonPhrase();
		return this;
	}
	
	/**
	 * A convenience method for setting the "Content-Type" header. Also sets 
	 * the "has payload" flag.
	 * 
	 * @param type the type
	 * @param subtype the subtype
	 * @return the response for easy chaining
	 * @throws ParseException if the values cannot be parsed
	 */
	public HttpResponse setContentType(String type, String subtype) 
			throws ParseException {
		setField(HttpField.CONTENT_TYPE, new MediaType(type, subtype));
		setHasPayload(true);
		return this;
	}

	/**
	 * A convenience method for setting the "Content-Type" header (usually
	 * of type "text") together with its charset parameter. Also sets 
	 * the "has payload" flag.
	 * 
	 * @param type the type
	 * @param subtype the subtype
	 * @param charset the charset
	 * @return the response for easy chaining
	 * @throws ParseException if the values cannot be parsed
	 */
	public HttpResponse setContentType(String type, String subtype,
			String charset) throws ParseException {
		setField(HttpField.CONTENT_TYPE,
				MediaType.builder().setType(type, subtype)
				.setParameter("charset", charset).build());
		setHasPayload(true);
		return this;
	}
	
	/**
	 * Convenience method for setting the "Content-Type" header using 
	 * the path information of the given request. Also sets 
	 * the "has payload" flag.
	 * 
	 * @param requestUri the requested resource
	 * @return the response for easy chaining
	 */
	public HttpResponse setContentType(URI requestUri) {
		// Get content type
		String mimeTypeName;
		try {
			// probeContentType is most advanced, but may fail if it tries
			// to look at the file's content (which doesn't exist).
			mimeTypeName = Files.probeContentType(Paths.get(
					requestUri.getPath()));
		} catch (IOException e) {
			mimeTypeName = null;
		}
		if (mimeTypeName == null) {
			mimeTypeName = typesMap.getContentType(requestUri.getPath());
		}
		MediaType mediaType = null;
		try {
			mediaType = Converters.MEDIA_TYPE.fromFieldValue(mimeTypeName);
		} catch (ParseException e) {
			// Cannot happen
		}
		
		// Send response 
		if ("text".equals(mediaType.topLevelType())) {
			mediaType = MediaType.builder().from(mediaType)
					.setParameter("charset", System.getProperty(
							"file.encoding", "UTF-8")).build();
		}
		setField(HttpField.CONTENT_TYPE, mediaType);
		setHasPayload(true);
		return this;
	}
	
	/**
	 * A convenience method for setting the "Content-Length" header.
	 * 
	 * @param length the length
	 * @return the response for easy chaining
  	 */
	public HttpResponse setContentLength(long length) {
		return setField(new HttpField<>(
				HttpField.CONTENT_LENGTH, length, Converters.LONG));
	}
	
	/**
	 * Associates the response with the request that it responds to. This method
	 * is invoked by the request decoder when it creates the prepared
	 * response for a request. The relationship with the request is required
	 * because information from the request headers may be needed when encoding
	 * the response. 
	 * 
	 * @param request
	 *            the request
	 * @return the response for easy chaining
	 * @see HttpRequest#setResponse(HttpResponse)
	 */
	public HttpResponse setRequest(HttpRequest request) {
		this.request = request;
		return this;
	}
	
	/**
	 * Returns the request that this response responds to.
	 * 
	 * @return the request
	 * @see #setRequest(HttpRequest)
	 */
	public Optional<HttpRequest> request() {
		return Optional.ofNullable(request);
	}

}
