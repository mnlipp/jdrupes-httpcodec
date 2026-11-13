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

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Encoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpProtocol;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpOptions;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.types.CommentedValue;
import org.jdrupes.httpcodec.types.Converters;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests focusing on encoding the response header.
 */
public class ResponseEncoderTests {

    @Test
    public void testSimple10Response() {
        HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_0,
            HttpStatus.OK, false);
        HttpResponseEncoder encoder = new HttpResponseEncoder();
        encoder.encode(response);
        ByteBuffer out = ByteBuffer.allocate(1024 * 1024);
        Encoder.Result result = encoder.encode(out);
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertTrue(result.closeConnection());
        String encoded = new String(out.array(), 0, out.position());
        assertTrue(encoded.contains("HTTP/1.0 200 OK\r\n"));
        assertTrue(encoded.contains("Content-Length: 0\r\n"));
        assertTrue(encoded.endsWith("\r\n\r\n"));
    }

    @Test
    public void testSimpleResponse() {
        HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
            HttpStatus.OK, false);
        HttpResponseEncoder encoder = new HttpResponseEncoder();
        encoder.encode(response);
        ByteBuffer out = ByteBuffer.allocate(1024 * 1024);
        Encoder.Result result = encoder.encode(out);
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());
        assertFalse(result.closeConnection());
        String encoded = new String(out.array(), 0, out.position());
        assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
        assertTrue(encoded.contains("Content-Length: 0\r\n"));
        assertTrue(encoded.endsWith("\r\n\r\n"));
    }

    @Test
    public void testSimpleResponseTinyOut() {
        HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
            HttpStatus.OK, false);
        HttpResponseEncoder encoder = new HttpResponseEncoder();
        encoder.encode(response);
        ByteBuffer out = ByteBuffer.allocate(1024 * 1024);
        ByteBuffer tinyOut = ByteBuffer.allocate(1);
        while (true) {
            Encoder.Result result = encoder.encode(tinyOut);
            assertFalse(result.isUnderflow());
            assertFalse(result.closeConnection());
            tinyOut.flip();
            if (tinyOut.hasRemaining()) {
                out.put(tinyOut);
                tinyOut.compact();
            }
            if (!result.isOverflow()) {
                break;
            }
        }
        String encoded = new String(out.array(), out.arrayOffset(),
            out.position());
        assertTrue(encoded.contains("HTTP/1.1 200 OK\r\n"));
        assertTrue(encoded.endsWith("\r\n\r\n"));
    }

    /**
     * Test encoding a valid RFC-compliant SERVER header with three products,
     * two of which have comments.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">
     * RFC 7231, Section 7.4.2 (Server)</a>
     */
    @Test
    public void testEncodeServerHeaderWithThreeProducts() {
        HttpResponse response = new HttpResponse(HttpProtocol.HTTP_1_1,
            HttpStatus.OK, false);

        List<CommentedValue<String>> serverProducts = new ArrayList<>();
        serverProducts.add(new CommentedValue<>("MyServer/1.0",
            new String[] { "Custom Build" }));
        serverProducts.add(new CommentedValue<>("Helper/2.1",
            new String[] { "Library" }));
        serverProducts.add(new CommentedValue<>("Other"));

        response.setField(HttpField.SERVER, serverProducts);

        HttpResponseEncoder encoder = new HttpResponseEncoder();
        encoder.encode(response);
        ByteBuffer out = ByteBuffer.allocate(1024 * 1024);
        Encoder.Result result = encoder.encode(out);
        assertFalse(result.isOverflow());
        assertFalse(result.isUnderflow());

        String encoded = new String(out.array(), 0, out.position());
        assertTrue(encoded.contains(
            "Server: MyServer/1.0 (Custom Build) Helper/2.1 (Library) Other\r\n"));
    }

    /**
     * Test that parsing a non-RFC-compliant SERVER header like
     * "UPnP SDK, UPnP library" fails when laxServerHeaderParsing is disabled.
     * The comma-separated format is not RFC-compliant for the Server header.
     */
    @Test
    public void testParseNonCompliantServerHeaderFails() {
        String headerText = "HTTP/1.1 200 OK\r\n"
            + "Server: UPnP SDK, UPnP library\r\n"
            + "\r\n";
        ByteBuffer in = ByteBuffer
            .wrap(headerText
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        HttpResponseDecoder decoder = new HttpResponseDecoder();
        ByteBuffer body = ByteBuffer.allocate(1024);
        try {
            decoder.decode(in, body, false);
            fail("Expected ParseException");
        } catch (ProtocolException e) {
            // Expected: parsing fails with a protocol-level ParseException
            assertTrue(e.getCause() instanceof ParseException
                || e.getMessage().contains("Parsing"));
        }
    }

    /**
     * Test that parsing a non-RFC-compliant SERVER header like
     * "UPnP SDK, UPnP library" succeeds when laxServerHeaderParsing is enabled.
     * This option supports servers that send comma-separated product strings
     * instead of space-separated ones.
     *
     * @see <a href="https://github.com/gerbera/gerbera/issues/4060">
     * Gerbera issue #4060</a>
     */
    @Test
    public void testParseNonCompliantServerHeaderWithLaxOption()
            throws ProtocolException {
        String headerText = "HTTP/1.1 200 OK\r\n"
            + "Server: UPnP SDK, UPnP library\r\n"
            + "\r\n";
        ByteBuffer in = ByteBuffer
            .wrap(headerText
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        HttpResponseDecoder decoder = new HttpResponseDecoder();
        decoder.options(new HttpOptions().laxServerHeaderParsing(true));
        ByteBuffer body = ByteBuffer.allocate(1024);
        Decoder.Result<?> result = decoder.decode(in, body, false);
        assertTrue(result.isHeaderCompleted());

        Optional<HttpField<List<CommentedValue<String>>>> serverField
            = decoder.header().get()
                .findField(HttpField.SERVER,
                    Converters.PRODUCT_DESCRIPTIONS);
        assertTrue(serverField.isPresent());
        assertEquals(2, serverField.get().value().size());
        assertEquals("UPnP SDK", serverField.get().value().get(0).value());
        assertEquals("UPnP library", serverField.get().value().get(1).value());
    }

}
