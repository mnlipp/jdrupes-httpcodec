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

package org.jdrupes.httpcodec.test.http;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Optional;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.types.CacheControlDirectives;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests focusing on the body, applicable to both requests and responses,
 * using collected content.
 */
public class EncoderFixTests {
	
	@Test
	public void testExpires()
	        throws UnsupportedEncodingException, ParseException {
		// Prepare response
		HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
		        HttpStatus.OK, false);
		CacheControlDirectives directives = new CacheControlDirectives();
		directives.add(new Directive("max-age", 100));
		response.setField(HttpField.CACHE_CONTROL, directives);
		
		// Encode header (apply fixes)
		HttpResponseEncoder encoder = new HttpResponseEncoder();
		Instant now = Instant.now();
		encoder.encode(response);
		
		// Check
		Optional<Instant> expires = response.findValue(
				HttpField.EXPIRES, Converters.DATE_TIME);
		assertTrue(expires.isPresent());
		assertTrue(now.plusSeconds(95).isBefore(expires.get())
				&& expires.get().isBefore(now.plusSeconds(105)));
	}

}
