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
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.StringList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 */
public class RequestDecoderTests {

    /**
     * Simple GET request.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testBasicGetRequestAtOnce()
            throws UnsupportedEncodingException {
        String reqText
            = "GET /test HTTP/1.1\r\n"
                + "Host: localhost:8888\r\n"
                + "Connection: keep-alive\r\n"
                + "User-Agent: JUnit\r\n"
                + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,"
                + "image/webp,*/*;q=0.8\r\n"
                + "Accept-Encoding: gzip, deflate, sdch\r\n"
                + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
                + "Cookie: _test.=yes; gsScrollPos=\r\n"
                + "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(decoder.header().get().hasPayload());
        assertEquals("GET", decoder.header().get().method());
        assertEquals("/test",
            decoder.header().get().requestUri().getPath());
        Optional<HttpField<CookieList>> field = decoder.header()
            .flatMap(h -> h.findField(
                HttpField.COOKIE, Converters.COOKIE_LIST));
        assertEquals(2, field.get().value().size());
        assertEquals("yes", field.get().value().valueForName("_test.").get());
        assertEquals("", field.get().value().valueForName("gsScrollPos").get());
    }

    /**
     * Simple GET request with query.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testBasicGetRequestWithQuery()
            throws UnsupportedEncodingException {
        String reqText
            = "GET /test?p1=Hello&p2=" + URLEncoder.encode("World!", "utf-8")
                + "&difficult=" + URLEncoder.encode("äöü", "utf-8")
                + "&difficult=" + URLEncoder.encode("ÄÖÜ", "utf-8")
                + " HTTP/1.1\r\n"
                + "Host: localhost:8888\r\n"
                + "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(decoder.header().get().hasPayload());
        assertEquals("GET", decoder.header().get().method());
        assertEquals("/test",
            decoder.header().get().requestUri().getPath());
        Map<String, List<String>> queryData
            = decoder.header().get().queryData();
        assertEquals("Hello", queryData.get("p1").get(0));
        assertEquals("World!", queryData.get("p2").get(0));
        assertEquals("äöü", queryData.get("difficult").get(0));
        assertEquals("ÄÖÜ", queryData.get("difficult").get(1));
    }

    /**
     * Request with header in two parts.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testRequestSplitHeader() throws UnsupportedEncodingException {
        // Partial header
        String reqText
            = "GET /test HTTP/1.1\r\n"
                + "Host: local";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
        assertFalse(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        // Continue header
        reqText
            = "host:8888\r\n"
                + "\r\n";
        buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        result = decoder.decode(buffer, null, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(decoder.header().get().hasPayload());
        assertEquals("GET", decoder.header().get().method());
        assertEquals("localhost", decoder.header().get().host());
        assertEquals(8888, decoder.header().get().port());
        assertEquals("/test",
            decoder.header().get().requestUri().getPath());
    }

    /**
     * POST with "expected" body (all in one call).
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testHeaderAndBodyAtOnce()
            throws UnsupportedEncodingException {
        String reqText = "POST /form HTTP/1.1\r\n"
            + "Host: localhost:8888\r\n"
            + "Connection: keep-alive\r\n"
            + "Content-Length: 28\r\n"
            + "Origin: http://localhost:8888\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Referer: http://localhost:8888/form\r\n"
            + "Accept-Encoding: gzip, deflate\r\n"
            + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
            + "\r\n"
            + "firstname=J.&lastname=Grapes";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        ByteBuffer body = ByteBuffer.allocate(1024);
        HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertTrue(decoder.header().get().hasPayload());
        assertEquals("POST", decoder.header().get().method());
        assertEquals("/form",
            decoder.header().get().requestUri().getPath());
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertFalse(buffer.hasRemaining());
        body.flip();
        String bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("firstname=J.&lastname=Grapes", bodyText);
    }

    /**
     * POST with "unexpected" body (no out buffer).
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testInterruptedBody()
            throws UnsupportedEncodingException {
        String reqText = "POST /form HTTP/1.1\r\n"
            + "Host: localhost:8888\r\n"
            + "Connection: keep-alive\r\n"
            + "Content-Length: 28\r\n"
            + "Origin: http://localhost:8888\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Referer: http://localhost:8888/form\r\n"
            + "Accept-Encoding: gzip, deflate\r\n"
            + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
            + "\r\n"
            + "firstname=J.&lastname=Grapes";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertTrue(decoder.header().get().hasPayload());
        assertEquals("POST", decoder.header().get().method());
        assertEquals("/form",
            decoder.header().get().requestUri().getPath());
        assertTrue(result.isOverflow());
        assertFalse(result.isUnderflow());
        // Get body
        ByteBuffer body = ByteBuffer.allocate(1024);
        result = decoder.decode(buffer, body, false);
        assertFalse(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertFalse(buffer.hasRemaining());
        body.flip();
        String bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("firstname=J.&lastname=Grapes", bodyText);
    }

    /**
     * POST with too small out buffer.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testPostSplitOut()
            throws UnsupportedEncodingException {
        String reqText = "POST /form HTTP/1.1\r\n"
            + "Host: localhost:8888\r\n"
            + "Connection: keep-alive\r\n"
            + "Content-Length: 28\r\n"
            + "Origin: http://localhost:8888\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Referer: http://localhost:8888/form\r\n"
            + "Accept-Encoding: gzip, deflate\r\n"
            + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
            + "\r\n"
            + "firstname=J.&lastname=Grapes";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        ByteBuffer body = ByteBuffer.allocate(20);
        HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertTrue(decoder.header().get().hasPayload());
        assertEquals("POST", decoder.header().get().method());
        assertEquals("/form",
            decoder.header().get().requestUri().getPath());
        assertTrue(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertTrue(buffer.hasRemaining());
        body.flip();
        String bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("firstname=J.&lastnam", bodyText);
        // Remaining
        body.clear();
        result = decoder.decode(buffer, body, false);
        assertFalse(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertFalse(buffer.hasRemaining());
        body.flip();
        bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("e=Grapes", bodyText);
    }

    /**
     * POST with input split in body.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testPostSplitIn()
            throws UnsupportedEncodingException {
        String reqText = "POST /form HTTP/1.1\r\n"
            + "Host: localhost:8888\r\n"
            + "Connection: keep-alive\r\n"
            + "Content-Length: 28\r\n"
            + "Origin: http://localhost:8888\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Referer: http://localhost:8888/form\r\n"
            + "Accept-Encoding: gzip, deflate\r\n"
            + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
            + "\r\n"
            + "firstname=J.&lastnam";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        ByteBuffer body = ByteBuffer.allocate(1024);
        HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertTrue(decoder.header().get().hasPayload());
        assertEquals("POST", decoder.header().get().method());
        assertEquals("/form",
            decoder.header().get().requestUri().getPath());
        assertFalse(result.isOverflow());
        assertTrue(result.isUnderflow());
        assertFalse(buffer.hasRemaining());
        // Rest
        buffer = ByteBuffer.wrap("e=Grapes".getBytes("ascii"));
        result = decoder.decode(buffer, body, false);
        assertFalse(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertFalse(buffer.hasRemaining());
        body.flip();
        String bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("firstname=J.&lastname=Grapes", bodyText);
    }

    /**
     * POST with chunked body.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testPostChunked()
            throws UnsupportedEncodingException {
        String reqText = "POST /form HTTP/1.1\r\n"
            + "Host: localhost:8888\r\n"
            + "Connection: keep-alive\r\n"
            + "Transfer-Encoding: chunked\r\n"
            + "Origin: http://localhost:8888\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Referer: http://localhost:8888/form\r\n"
            + "Accept-Encoding: gzip, deflate\r\n"
            + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
            + "\r\n"
            + "14;dummy=0\r\n"
            + "firstname=J.&lastnam\r\n"
            + "8\r\n"
            + "e=Grapes\r\n"
            + "0\r\n"
            + "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        ByteBuffer body = ByteBuffer.allocate(1024);
        HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertTrue(decoder.header().get().hasPayload());
        assertEquals("POST", decoder.header().get().method());
        assertEquals("/form",
            decoder.header().get().requestUri().getPath());
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertFalse(buffer.hasRemaining());
        body.flip();
        String bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("firstname=J.&lastname=Grapes", bodyText);
    }

    /**
     * POST with chunked body and trailer part.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testPostChunkedWithTrailer()
            throws UnsupportedEncodingException {
        String reqText = "POST /form HTTP/1.1\r\n"
            + "Host: localhost:8888\r\n"
            + "Connection: keep-alive\r\n"
            + "Transfer-Encoding: chunked\r\n"
            + "Origin: http://localhost:8888\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Referer: http://localhost:8888/form\r\n"
            + "Accept-Encoding: gzip, deflate\r\n"
            + "Accept-Language: de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4\r\n"
            + "\r\n"
            + "1c;dummy=0\r\n"
            + "firstname=J.&lastname=Grapes\r\n"
            + "0\r\n"
            + "X-Test-Field: Valid\r\n"
            + "X-Summary-Field: Good\r\n"
            + "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        ByteBuffer body = ByteBuffer.allocate(1024);
        HttpRequestDecoder.Result result = decoder.decode(buffer, body, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertTrue(decoder.header().get().hasPayload());
        assertEquals("POST", decoder.header().get().method());
        assertEquals("/form",
            decoder.header().get().requestUri().getPath());
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertTrue(!buffer.hasRemaining());
        body.flip();
        String bodyText = new String(body.array(), body.position(),
            body.limit());
        assertEquals("firstname=J.&lastname=Grapes", bodyText);
        // Trailer
        Optional<HttpField<StringList>> trailer = decoder.header()
            .flatMap(
                f -> f.findField(HttpField.TRAILER, Converters.STRING_LIST));
        assertEquals(2, trailer.get().value().size());
        trailer.get().value().containsIgnoreCase("X-Test-Field");
        trailer.get().value().containsIgnoreCase("X-Summary-Field");
        Optional<String> testValue = decoder.header()
            .flatMap(h -> h.findStringValue("X-Test-Field"));
        assertEquals("Valid", testValue.get());
        testValue = decoder.header().flatMap(
            h -> h.findStringValue("X-Summary-Field"));
        assertEquals("Good", testValue.get());
    }

    /**
     * Simple GET request.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testUrlDecoding()
            throws UnsupportedEncodingException {
        String reqText
            = "GET /test%20this HTTP/1.1\r\n"
                + "Host: localhost:8888\r\n"
                + "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
        assertTrue(result.isHeaderCompleted());
        assertFalse(result.response().isPresent());
        assertFalse(decoder.header().get().hasPayload());
        assertEquals("GET", decoder.header().get().method());
        assertEquals("/test%20this",
            decoder.header().get().requestUri().getRawPath());
        assertEquals("/test this",
            decoder.header().get().requestUri().getPath());
    }

    /**
     * Simple GET request.
     * 
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testMultipleStringListHeader()
            throws UnsupportedEncodingException {
        String reqText
            = "GET /test HTTP/1.1\r\n"
                + "Host: localhost:8888\r\n"
                + "Connection: keep-alive\r\n"
                + "Connection: Upgrade\r\n"
                + "\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(reqText.getBytes("ascii"));
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequestDecoder.Result result = decoder.decode(buffer, null, false);
        assertTrue(result.isHeaderCompleted());
        var connection = decoder.header().get()
            .findValue("Connection", Converters.STRING_LIST).get();
        assertTrue(connection.contains("keep-alive"));
        assertTrue(connection.contains("Upgrade"));
    }

}
