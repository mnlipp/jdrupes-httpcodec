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

package org.jdrupes.httpcodec.test;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * 
 */
public class Basic {

	@Test
	public void testBasicResult() {
		Codec.Result res1 = new Codec.Result(false, false, false) {
		};
		Codec.Result res2 = new Codec.Result(false, false, false) {
		};
		assertEquals(res1, res2);
		assertEquals(res1.hashCode(), res2.hashCode());
		Codec.Result res3 = new Codec.Result(true, false, false) {
		};
		assertNotEquals(res1, res3);
		res3 = new Codec.Result(false, true, false) {
		};
		assertNotEquals(res1, res3);
		res3 = new Codec.Result(false, false, true) {
		};
		assertNotEquals(res1, res3);
	}

	@Test
	public void testDecoderResult() {
		Decoder.Result<HttpRequest> res1 = new Decoder.Result<HttpRequest>(
				false, false, false, false, null, false) {
		};
		Decoder.Result<HttpRequest> res2 = new Decoder.Result<HttpRequest>(
				false, false, false, false, null, false) {
		};
		assertEquals(res1, res2);
		assertEquals(res1.hashCode(), res2.hashCode());
	}

}
