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

package org.jdrupes.httpcodec.test.fields;

import java.net.HttpCookie;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.types.CacheControlDirectives;
import org.jdrupes.httpcodec.types.CommentedValue;
import org.jdrupes.httpcodec.types.Converter;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.Directive;
import org.jdrupes.httpcodec.types.Etag;
import org.jdrupes.httpcodec.types.MediaBase;
import org.jdrupes.httpcodec.types.MediaRange;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.ParameterizedValue;
import org.jdrupes.httpcodec.types.StringList;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Converter round-trip tests.
 * 
 * Each converter type defined in {@link Converters} is tested for compliance
 * with its corresponding RFC:
 * <ul>
 *   <li>String to typed value (fromFieldValue)</li>
 *   <li>Typed value back to string (asFieldValue) produces the original
 *       string</li>
 * </ul>
 */
public class ConverterTests {

    /*
     * ============================================================
     * UNQUOTED_STRING - RFC 7230, Section 3.2.6 (token)
     * Used by: Host (RFC 7230, 5.4)
     * ============================================================
     */

    /**
     * Test UNQUOTED_STRING converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">
     * RFC 7230, Section 3.2.6</a>
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.4">
     * RFC 7230, Section 5.4 (Host)</a>
     */
    @Test
    public void testUnquotedStringRoundTrip() throws ParseException {
        Converter<String> conv = Converters.UNQUOTED_STRING;

        // Simple host
        String input = "www.example.com";
        assertEquals(input, conv.fromFieldValue(input));
        assertEquals(input, conv.asFieldValue(input));

        // Host with port
        input = "www.example.com:8080";
        String parsed = conv.fromFieldValue("  " + input + "  ");
        assertEquals(input, parsed);
        assertEquals(input, conv.asFieldValue(parsed));

        // IPv6 host
        input = "[2001:db8::1]";
        parsed = conv.fromFieldValue(input);
        assertEquals(input, parsed);
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * STRING - RFC 7230, Section 3.2.6 (field value)
     * Used by: Warning (RFC 7234, 5.5), unknown fields
     * ============================================================
     */

    /**
     * Test STRING converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">
     * RFC 7230, Section 3.2.6</a>
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.5">
     * RFC 7234, Section 5.5 (Warning)</a>
     */
    @Test
    public void testStringRoundTrip() throws ParseException {
        Converter<String> conv = Converters.STRING;

        // Simple token string
        String input = "HelloWorld";
        String parsed = conv.fromFieldValue(input);
        assertEquals(input, parsed);
        assertEquals(input, conv.asFieldValue(parsed));

        // String with spaces (needs quoting on round-trip)
        input = "Hello World";
        parsed = conv.fromFieldValue("\"" + input + "\"");
        assertEquals(input, parsed);
        assertEquals("\"Hello World\"", conv.asFieldValue(parsed));

        // String with escaped quote - backslashes are preserved in unquote
        parsed = conv.fromFieldValue("\"He said \\\\\"hi\\\\\"\"");
        // The unquote method preserves trailing backslash escapes
        assertEquals("He said \\\"hi\\\"", parsed);
    }

    /*
     * ============================================================
     * STRING_LIST - RFC 7230, Section 3.2.6 / RFC 7231, 7.4.1
     * Used by: Allow (RFC 7231, 7.4.1), Connection (RFC 7230, 6.1),
     * TE (RFC 7230, 4.3), Trailer (RFC 7230, 4.4),
     * Transfer-Encoding (RFC 7230, 3.3.1),
     * Upgrade (RFC 7230, 6.7), Via (RFC 7230, 5.7.1)
     * ============================================================
     */

    /**
     * Test STRING_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">
     * RFC 7231, Section 7.4.1 (Allow)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.1">
     * RFC 7230, Section 6.1 (Connection)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">
     * RFC 7230, Section 3.3.1 (Transfer-Encoding)</a>
     */
    @Test
    public void testStringListRoundTrip() throws ParseException {
        Converter<StringList> conv = Converters.STRING_LIST;

        // Allow header - RFC 7231, 7.4.1
        String input = "GET, HEAD, PUT";
        StringList parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        assertEquals("GET", parsed.get(0));
        assertEquals("HEAD", parsed.get(1));
        assertEquals("PUT", parsed.get(2));
        assertEquals(input, conv.asFieldValue(parsed));

        // Transfer-Encoding - RFC 7230, 3.3.1
        input = "chunked";
        parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals(input, conv.asFieldValue(parsed));

        // With whitespace variations
        input = "gzip,  deflate, chunked";
        parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        assertEquals("gzip", parsed.get(0));
        assertEquals("deflate", parsed.get(1));
        assertEquals("chunked", parsed.get(2));
        assertEquals("gzip, deflate, chunked", conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * LONG - RFC 7230, 3.3.2 / RFC 7234, 5.1
     * Used by: Content-Length (RFC 7230, 3.3.2),
     * Age (RFC 7234, 5.1),
     * Max-Forwards (RFC 7231, 5.1.2)
     * ============================================================
     */

    /**
     * Test LONG converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">
     * RFC 7230, Section 3.3.2 (Content-Length)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.1">
     * RFC 7234, Section 5.1 (Age)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.2">
     * RFC 7231, Section 5.1.2 (Max-Forwards)</a>
     */
    @Test
    public void testLongRoundTrip() throws ParseException {
        Converter<Long> conv = Converters.LONG;

        // Content-Length - RFC 7230, 3.3.2
        String input = "42";
        Long parsed = conv.fromFieldValue(input);
        assertEquals(42L, parsed.longValue());
        assertEquals(input, conv.asFieldValue(parsed));

        // Age - RFC 7234, 5.1
        input = "120";
        parsed = conv.fromFieldValue(input);
        assertEquals(120L, parsed.longValue());
        assertEquals(input, conv.asFieldValue(parsed));

        // Large value
        input = "1073741824";
        parsed = conv.fromFieldValue(input);
        assertEquals(1073741824L, parsed.longValue());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * DATE_TIME - RFC 7231, 7.1.1.2
     * Used by: Date (RFC 7231, 7.1.1.2), Expires (RFC 7234, 5.3),
     * If-Modified-Since (RFC 7232, 3.3),
     * If-Unmodified-Since (RFC 7232, 3.4),
     * Last-Modified (RFC 7232, 2.2)
     * ============================================================
     */

    /**
     * Test DATE_TIME converter with RFC 1123 format (preferred).
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">
     * RFC 7231, Section 7.1.1.2 (Date)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.3">
     * RFC 7234, Section 5.3 (Expires)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.2">
     * RFC 7232, Section 2.2 (Last-Modified)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.3">
     * RFC 7232, Section 3.3 (If-Modified-Since)</a>
     */
    @Test
    public void testDateTimeRoundTrip() throws ParseException {
        Converter<Instant> conv = Converters.DATE_TIME;

        // RFC 1123 format (preferred, RFC 7231, 7.1.1.2)
        String input = "Sun, 06 Nov 1994 08:49:37 GMT";
        Instant parsed = conv.fromFieldValue(input);
        assertNotNull(parsed);
        // Output is always RFC 1123 (single-digit day not zero-padded)
        assertEquals("Sun, 6 Nov 1994 08:49:37 GMT", conv.asFieldValue(parsed));

        // Another RFC 1123 date
        input = "Tue, 15 Nov 1994 08:12:31 GMT";
        parsed = conv.fromFieldValue(input);
        assertEquals("Tue, 15 Nov 1994 08:12:31 GMT",
            conv.asFieldValue(parsed));
    }

    /**
     * Test DATE_TIME converter with obsolete RFC 850 format.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">
     * RFC 7231, Section 7.1.1.2 (Date)</a>
     */
    @Test
    public void testDateTimeRfc850() throws ParseException {
        Converter<Instant> conv = Converters.DATE_TIME;

        // RFC 850 format (obsolete, but accepted)
        String input = "Sunday, 06-Nov-94 08:49:37 GMT";
        Instant parsed = conv.fromFieldValue(input);
        assertNotNull(parsed);
        // Output is always RFC 1123 (single-digit day not zero-padded)
        assertEquals("Sun, 6 Nov 1994 08:49:37 GMT", conv.asFieldValue(parsed));
    }

    /**
     * Test DATE_TIME converter with ANSI C asctime() format.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">
     * RFC 7231, Section 7.1.1.2 (Date)</a>
     */
    @Test
    public void testDateTimeAnsi() throws ParseException {
        Converter<Instant> conv = Converters.DATE_TIME;

        // ANSI C asctime() format (obsolete, but accepted)
        String input = "Sun Nov  6 08:49:37 1994";
        Instant parsed = conv.fromFieldValue(input);
        assertNotNull(parsed);
        // Output is always RFC 1123 (single-digit day not zero-padded)
        assertEquals("Sun, 6 Nov 1994 08:49:37 GMT", conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * SECONDS_OR_DATE_TIME - RFC 7231, 7.1.3
     * Used by: Retry-After (RFC 7231, 7.1.3)
     * ============================================================
     */

    /**
     * Test SECONDS_OR_DATE_TIME converter with seconds (relative).
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">
     * RFC 7231, Section 7.1.3 (Retry-After)</a>
     */
    @Test
    public void testSecondsOrDateTimeSeconds() throws ParseException {
        Converter<Instant> conv = Converters.SECONDS_OR_DATE_TIME;

        // HTTP-date or delta-seconds (RFC 7231, 7.1.3)
        String input = "120";
        Instant parsed = conv.fromFieldValue(input);
        assertNotNull(parsed);
        // Output is an RFC 1123 date (relative converted to absolute)
        String output = conv.asFieldValue(parsed);
        assertNotNull(output);
        assertTrue(output.contains("GMT"));
    }

    /**
     * Test SECONDS_OR_DATE_TIME converter with HTTP-date.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">
     * RFC 7231, Section 7.1.3 (Retry-After)</a>
     */
    @Test
    public void testSecondsOrDateTimeHttpDate() throws ParseException {
        Converter<Instant> conv = Converters.SECONDS_OR_DATE_TIME;

        String input = "Mon, 01 Dec 2025 16:00:00 GMT";
        Instant parsed = conv.fromFieldValue(input);
        assertNotNull(parsed);
        // Dec 1, 2025 is a Monday, single-digit day
        assertEquals("Mon, 1 Dec 2025 16:00:00 GMT", conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * MEDIA_TYPE_PAIR - RFC 7231, 3.1.1.1
     * Used internally by MediaType and MediaRange
     * ============================================================
     */

    /**
     * Test MEDIA_TYPE_PAIR converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1">
     * RFC 7231, Section 3.1.1.1 (Media Type)</a>
     */
    @Test
    public void testMediaTypePairRoundTrip() throws ParseException {
        Converter<MediaBase.MediaTypePair> conv = Converters.MEDIA_TYPE_PAIR;

        String input = "text/html";
        MediaBase.MediaTypePair parsed = conv.fromFieldValue(input);
        assertEquals("text", parsed.topLevelType());
        assertEquals("html", parsed.subtype());
        assertEquals(input, conv.asFieldValue(parsed));

        // Application type
        input = "application/json";
        parsed = conv.fromFieldValue(input);
        assertEquals("application", parsed.topLevelType());
        assertEquals("json", parsed.subtype());
        assertEquals(input, conv.asFieldValue(parsed));

        // Case insensitivity (RFC 7231, 3.1.1.1: tokens are case-insensitive)
        parsed = conv.fromFieldValue("Text/HTML");
        assertEquals("text", parsed.topLevelType());
        assertEquals("html", parsed.subtype());
    }

    /*
     * ============================================================
     * MEDIA_TYPE - RFC 7231, 3.1.1.5
     * Used by: Content-Type (RFC 7231, 3.1.1.5)
     * ============================================================
     */

    /**
     * Test MEDIA_TYPE converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">
     * RFC 7231, Section 3.1.1.5 (Content-Type)</a>
     */
    @Test
    public void testMediaTypeRoundTrip() throws ParseException {
        Converter<MediaType> conv = Converters.MEDIA_TYPE;

        // Basic media type
        String input = "text/html";
        MediaType parsed = conv.fromFieldValue(input);
        assertEquals("text", parsed.topLevelType());
        assertEquals("html", parsed.subtype());
        assertEquals(input, conv.asFieldValue(parsed));

        // With parameter
        input = "text/html; charset=utf-8";
        parsed = conv.fromFieldValue(input);
        assertEquals("text", parsed.topLevelType());
        assertEquals("html", parsed.subtype());
        assertEquals("utf-8", parsed.parameter("charset"));
        assertEquals(input, conv.asFieldValue(parsed));

        // Multiple parameters
        parsed = conv.fromFieldValue("multipart/form-data; boundary=foo987654");
        assertEquals("multipart", parsed.topLevelType());
        assertEquals("form-data", parsed.subtype());
        assertEquals("foo987654", parsed.parameter("boundary"));
    }

    /*
     * ============================================================
     * MEDIA_RANGE - RFC 7231, 5.3.2
     * Used by: Accept (RFC 7231, 5.3.2)
     * ============================================================
     */

    /**
     * Test MEDIA_RANGE converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">
     * RFC 7231, Section 5.3.2 (Accept)</a>
     */
    @Test
    public void testMediaRangeRoundTrip() throws ParseException {
        Converter<MediaRange> conv = Converters.MEDIA_RANGE;

        // Specific range
        String input = "text/html";
        MediaRange parsed = conv.fromFieldValue(input);
        assertEquals("text", parsed.topLevelType());
        assertEquals("html", parsed.subtype());
        assertEquals(input, conv.asFieldValue(parsed));

        // Range with quality
        input = "text/html; q=0.8";
        parsed = conv.fromFieldValue(input);
        assertEquals("text", parsed.topLevelType());
        assertEquals("html", parsed.subtype());
        assertEquals("0.8", parsed.parameter("q"));
        assertEquals(input, conv.asFieldValue(parsed));

        // Wildcard range
        input = "*/*";
        parsed = conv.fromFieldValue(input);
        assertEquals("*", parsed.topLevelType());
        assertEquals("*", parsed.subtype());
        assertEquals(input, conv.asFieldValue(parsed));

        // Type wildcard
        input = "text/*";
        parsed = conv.fromFieldValue(input);
        assertEquals("text", parsed.topLevelType());
        assertEquals("*", parsed.subtype());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /**
     * Test MEDIA_RANGE_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">
     * RFC 7231, Section 5.3.2 (Accept)</a>
     */
    @Test
    public void testMediaRangeListRoundTrip() throws ParseException {
        Converter<List<MediaRange>> conv = Converters.MEDIA_RANGE_LIST;

        String input = "text/html, application/json; q=0.5, */*";
        List<MediaRange> parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        assertEquals("text/html", parsed.get(0).toString());
        assertEquals("application/json; q=0.5", parsed.get(1).toString());
        assertEquals("*/*", parsed.get(2).toString());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * WEIGHTED_STRINGS - RFC 7231, 5.3.3
     * Used by: Accept-Charset (RFC 7231, 5.3.3)
     * ============================================================
     */

    /**
     * Test WEIGHTED_STRINGS converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.3">
     * RFC 7231, Section 5.3.3 (Accept-Charset)</a>
     */
    @Test
    public void testWeightedStringsRoundTrip() throws ParseException {
        Converter<List<ParameterizedValue<String>>> conv
            = Converters.WEIGHTED_STRINGS;

        // Accept-Charset example from RFC 7231, 5.3.3
        String input = "iso-8859-1, unicode-1-1; q=0.8";
        List<ParameterizedValue<String>> parsed = conv.fromFieldValue(input);
        assertEquals(2, parsed.size());
        assertEquals("iso-8859-1", parsed.get(0).value());
        assertNull(parsed.get(0).parameter("q"));
        assertEquals("unicode-1-1", parsed.get(1).value());
        assertEquals("0.8", parsed.get(1).parameter("q"));
        assertEquals(input, conv.asFieldValue(parsed));

        // Single charset
        input = "utf-8";
        parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("utf-8", parsed.get(0).value());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * LANGUAGE / LANGUAGE_LIST - RFC 7231, 5.3.5
     * Used by: Accept-Language (RFC 7231, 5.3.5)
     * ============================================================
     */

    /**
     * Test LANGUAGE converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">
     * RFC 7231, Section 5.3.5 (Accept-Language)</a>
     * @see <a href="https://tools.ietf.org/html/rfc5646">RFC 5646
     * (Language Tags)</a>
     */
    @Test
    public void testLanguageRoundTrip() throws ParseException {
        Converter<Locale> conv = Converters.LANGUAGE;

        // Simple language
        String input = "da";
        Locale parsed = conv.fromFieldValue(input);
        assertEquals("da", parsed.getLanguage());
        assertEquals(input, conv.asFieldValue(parsed));

        // Language with region - Locale normalizes country to uppercase
        input = "en-gb";
        parsed = conv.fromFieldValue(input);
        assertEquals("en", parsed.getLanguage());
        assertEquals("GB", parsed.getCountry());
        // Output uses uppercase country code
        assertEquals("en-GB", conv.asFieldValue(parsed));
    }

    /**
     * Test LANGUAGE_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">
     * RFC 7231, Section 5.3.5 (Accept-Language)</a>
     */
    @Test
    public void testLanguageListRoundTrip() throws ParseException {
        Converter<List<ParameterizedValue<Locale>>> conv
            = Converters.LANGUAGE_LIST;

        String input = "da, en-gb; q=0.8, en; q=0.7";
        List<ParameterizedValue<Locale>> parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        assertEquals("da", parsed.get(0).value().getLanguage());
        assertEquals("en", parsed.get(1).value().getLanguage());
        assertEquals("GB", parsed.get(1).value().getCountry());
        assertEquals("0.8", parsed.get(1).parameter("q"));
        assertEquals("en", parsed.get(2).value().getLanguage());
        assertEquals("0.7", parsed.get(2).parameter("q"));
    }

    /*
     * ============================================================
     * ETAG - RFC 7232, 2.3
     * Used by: ETag (RFC 7232, 2.3)
     * ============================================================
     */

    /**
     * Test ETAG converter with strong validator.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">
     * RFC 7232, Section 2.3 (ETag)</a>
     */
    @Test
    public void testEtagStrongRoundTrip() throws ParseException {
        Converter<Etag> conv = Converters.ETAG;

        String input = "\"xyzzy\"";
        Etag parsed = conv.fromFieldValue(input);
        assertFalse(parsed.isWeak());
        assertEquals("xyzzy", parsed.tag());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /**
     * Test ETAG converter with weak validator.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">
     * RFC 7232, Section 2.3 (ETag)</a>
     */
    @Test
    public void testEtagWeakRoundTrip() throws ParseException {
        Converter<Etag> conv = Converters.ETAG;

        String input = "W/\"xyzzy\"";
        Etag parsed = conv.fromFieldValue(input);
        assertTrue(parsed.isWeak());
        assertEquals("xyzzy", parsed.tag());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /**
     * Test ETAG converter with wildcard.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">
     * RFC 7232, Section 3.1 (If-Match)</a>
     */
    @Test
    public void testEtagWildcardRoundTrip() throws ParseException {
        Converter<Etag> conv = Converters.ETAG;

        String input = "*";
        Etag parsed = conv.fromFieldValue(input);
        assertFalse(parsed.isWeak());
        assertEquals(Converters.WILDCARD, parsed.tag());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * ETAG_LIST - RFC 7232, 3.1 / 3.2
     * Used by: If-Match (RFC 7232, 3.1), If-None-Match (RFC 7232, 3.2)
     * ============================================================
     */

    /**
     * Test ETAG_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">
     * RFC 7232, Section 3.1 (If-Match)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.2">
     * RFC 7232, Section 3.2 (If-None-Match)</a>
     */
    @Test
    public void testEtagListRoundTrip() throws ParseException {
        Converter<List<Etag>> conv = Converters.ETAG_LIST;

        // If-Match example from RFC 7232, 3.1
        String input = "\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\"";
        List<Etag> parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        assertEquals("xyzzy", parsed.get(0).tag());
        assertEquals("r2d2xxxx", parsed.get(1).tag());
        assertEquals("c3piozzzz", parsed.get(2).tag());
        for (Etag e : parsed) {
            assertFalse(e.isWeak());
        }
        assertEquals(input, conv.asFieldValue(parsed));

        // If-None-Match with weak validators
        input = "W/\"xyzzy\", W/\"r2d2xxxx\"";
        parsed = conv.fromFieldValue(input);
        assertEquals(2, parsed.size());
        assertTrue(parsed.get(0).isWeak());
        assertTrue(parsed.get(1).isWeak());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * URI_CONV - RFC 7231, 3.1.4.2 / 7.1.2
     * Used by: Content-Location (RFC 7231, 3.1.4.2),
     * Location (RFC 7231, 7.1.2)
     * ============================================================
     */

    /**
     * Test URI converter with absolute URI.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">
     * RFC 7231, Section 7.1.2 (Location)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">
     * RFC 7231, Section 3.1.4.2 (Content-Location)</a>
     */
    @Test
    public void testUriRoundTrip() throws ParseException {
        Converter<URI> conv = Converters.URI_CONV;

        // Absolute URI (Location, RFC 7231, 7.1.2)
        String input = "http://www.example.org/~a/t.html";
        URI parsed = conv.fromFieldValue(input);
        assertEquals("http", parsed.getScheme());
        assertEquals("www.example.org", parsed.getHost());
        assertEquals("/~a/t.html", parsed.getPath());
        assertEquals(input, conv.asFieldValue(parsed));

        // Relative URI (Content-Location, RFC 7231, 3.1.4.2)
        input = "test/index.html";
        parsed = conv.fromFieldValue(input);
        assertEquals("test/index.html", parsed.getPath());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * PRODUCT_DESCRIPTIONS - RFC 7231, 5.5.3 / 7.4.2
     * Used by: User-Agent (RFC 7231, 5.5.3), Server (RFC 7231, 7.4.2)
     * ============================================================
     */

    /**
     * Test PRODUCT_DESCRIPTIONS converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.3">
     * RFC 7231, Section 5.5.3 (User-Agent)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">
     * RFC 7231, Section 7.4.2 (Server)</a>
     */
    @Test
    public void testProductDescriptionsRoundTrip() throws ParseException {
        Converter<List<CommentedValue<String>>> conv
            = Converters.PRODUCT_DESCRIPTIONS;

        // User-Agent from RFC 7231, 5.5.3
        String input = "CERN-LineMode/2.15 libwww/2.17b3";
        List<CommentedValue<String>> parsed = conv.fromFieldValue(input);
        assertEquals(2, parsed.size());
        assertEquals("CERN-LineMode/2.15", parsed.get(0).value());
        assertEquals("libwww/2.17b3", parsed.get(1).value());
        // Output quotes product tokens containing /
        assertEquals("\"CERN-LineMode/2.15\"  \"libwww/2.17b3\"",
            conv.asFieldValue(parsed));

        // Simple product token (no slash, no quoting needed)
        input = "Client";
        parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("Client", parsed.get(0).value());
        assertEquals(input, conv.asFieldValue(parsed));

        // Server with comment (RFC 7231, 7.4.2)
        input = "Apache/2.4.41 (Ubuntu)";
        parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("Apache/2.4.41", parsed.get(0).value());
        assertEquals("Ubuntu", parsed.get(0).comments()[0]);
    }

    /*
     * ============================================================
     * DIRECTIVE / DIRECTIVE_LIST - RFC 7234, 5.4
     * Used by: Pragma (RFC 7234, 5.4)
     * ============================================================
     */

    /**
     * Test DIRECTIVE converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">
     * RFC 7234, Section 5.4 (Pragma)</a>
     */
    @Test
    public void testDirectiveRoundTrip() throws ParseException {
        Converter<Directive> conv = Converters.DIRECTIVE;

        // Directive without value
        String input = "no-cache";
        Directive parsed = conv.fromFieldValue(input);
        assertEquals("no-cache", parsed.name());
        assertFalse(parsed.value().isPresent());
        assertEquals(input, conv.asFieldValue(parsed));

        // Directive with value
        input = "max-age=3600";
        parsed = conv.fromFieldValue(input);
        assertEquals("max-age", parsed.name());
        assertTrue(parsed.value().isPresent());
        assertEquals("3600", parsed.value().get());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /**
     * Test DIRECTIVE_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">
     * RFC 7234, Section 5.4 (Pragma)</a>
     */
    @Test
    public void testDirectiveListRoundTrip() throws ParseException {
        Converter<List<Directive>> conv = Converters.DIRECTIVE_LIST;

        // Pragma header (RFC 7234, 5.4)
        String input = "no-cache";
        List<Directive> parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("no-cache", parsed.get(0).name());
        assertEquals(input, conv.asFieldValue(parsed));
    }

    /*
     * ============================================================
     * CACHE_CONTROL_LIST - RFC 7234, 5.2
     * Used by: Cache-Control (RFC 7234, 5.2)
     * ============================================================
     */

    /**
     * Test CACHE_CONTROL_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">
     * RFC 7234, Section 5.2 (Cache-Control)</a>
     */
    @Test
    public void testCacheControlListRoundTrip() throws ParseException {
        Converter<CacheControlDirectives> conv = Converters.CACHE_CONTROL_LIST;

        // Simple directive
        String input = "no-cache";
        CacheControlDirectives parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals(input, conv.asFieldValue(parsed));

        // Multiple directives
        input = "private, max-age=600, no-transform";
        parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        assertEquals(input, conv.asFieldValue(parsed));

        // With quoted value
        input = "no-cache=\"Set-Cookie\"";
        parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("Set-Cookie", parsed.valueForName("no-cache").get());
    }

    /*
     * ============================================================
     * CHALLENGE_LIST - RFC 7235, 4.1 / 4.3
     * Used by: WWW-Authenticate (RFC 7235, 4.1),
     * Proxy-Authenticate (RFC 7235, 4.3)
     * ============================================================
     */

    /**
     * Test CHALLENGE_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.1">
     * RFC 7235, Section 4.1 (WWW-Authenticate)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.3">
     * RFC 7235, Section 4.3 (Proxy-Authenticate)</a>
     */
    @Test
    public void testChallengeListRoundTrip() throws ParseException {
        Converter<List<ParameterizedValue<String>>> conv
            = Converters.CHALLENGE_LIST;

        // Basic challenge from RFC 7235, 4.1
        String input = "Basic realm=\"example\"";
        List<ParameterizedValue<String>> parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("Basic", parsed.get(0).value());
        assertEquals("example", parsed.get(0).parameter("realm"));
        // Output omits quotes when not needed
        assertEquals("Basic realm=example", conv.asFieldValue(parsed));

        // Multiple challenges
        input = "Basic realm=\"simple\", Digest realm=\"api\"";
        parsed = conv.fromFieldValue(input);
        assertEquals(2, parsed.size());
        assertEquals("Basic", parsed.get(0).value());
        assertEquals("Digest", parsed.get(1).value());
    }

    /*
     * ============================================================
     * CREDENTIALS - RFC 7235, 4.2 / 4.4
     * Used by: Authorization (RFC 7235, 4.2),
     * Proxy-Authorization (RFC 7235, 4.4)
     * ============================================================
     */

    /**
     * Test CREDENTIALS converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">
     * RFC 7235, Section 4.2 (Authorization)</a>
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.4">
     * RFC 7235, Section 4.4 (Proxy-Authorization)</a>
     */
    @Test
    public void testCredentialsRoundTrip() throws ParseException {
        Converter<ParameterizedValue<String>> conv = Converters.CREDENTIALS;

        // Basic credentials from RFC 7235, 4.2
        String input = "Basic aHR0cHdhdGNoOmY=";
        ParameterizedValue<String> parsed = conv.fromFieldValue(input);
        assertEquals("Basic", parsed.value());
        assertEquals("aHR0cHdhdGNoOmY=", parsed.parameter(null));
        assertEquals(input, conv.asFieldValue(parsed));

        // Digest credentials
        input = "Digest username=\"test\", realm=\"example\", nonce=\"abc\","
            + " uri=\"/\", response=\"xyz\"";
        parsed = conv.fromFieldValue(input);
        assertEquals("Digest", parsed.value());
        assertEquals("test", parsed.parameter("username"));
    }

    /*
     * ============================================================
     * SET_COOKIE - RFC 6265, 4.1.1
     * Used by: Set-Cookie (RFC 6265, 4.1.1)
     * ============================================================
     */

    /**
     * Test SET_COOKIE converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc6265#section-4.1.1">
     * RFC 6265, Section 4.1.1 (Set-Cookie)</a>
     */
    @Test
    public void testSetCookieRoundTrip() throws ParseException {
        Converter<CookieList> conv = Converters.SET_COOKIE;

        // Simple cookie
        String input = "SID=31d4d96e407aad42";
        CookieList parsed = conv.fromFieldValue(input);
        assertEquals(1, parsed.size());
        assertEquals("SID", parsed.iterator().next().getName());
        assertEquals("31d4d96e407aad42", parsed.iterator().next().getValue());
    }

    /**
     * Test individual Set-Cookie string via SET_COOKIE_STRING converter.
     * This converter is write-only (asFieldValue only).
     * 
     * @see <a href="https://tools.ietf.org/html/rfc6265#section-4.1.1">
     * RFC 6265, Section 4.1.1 (Set-Cookie)</a>
     */
    @Test
    public void testSetCookieStringAsFieldValue() {
        Converter<HttpCookie> conv = Converters.SET_COOKIE_STRING
            .get(Converters.SameSiteAttribute.UNSET);

        HttpCookie cookie = new HttpCookie("SID", "31d4d96e407aad42");
        cookie.setDomain(".example.com");
        cookie.setPath("/");
        cookie.setSecure(true);
        String output = conv.asFieldValue(cookie);
        assertNotNull(output);
        assertTrue(output.contains("SID=31d4d96e407aad42"));
        assertTrue(output.contains(".example.com"));
        assertTrue(output.contains("/"));
        assertTrue(output.contains("Secure"));

        // fromFieldValue is not supported
        try {
            conv.fromFieldValue("SID=31d4d96e407aad42");
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException | ParseException e) {
            // expected
        }
    }

    /*
     * ============================================================
     * COOKIE_LIST - RFC 6265, 5.4
     * Used by: Cookie (RFC 6265, 5.4)
     * ============================================================
     */

    /**
     * Test COOKIE_LIST converter.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc6265#section-5.4">
     * RFC 6265, Section 5.4 (Cookie header)</a>
     */
    @Test
    public void testCookieListRoundTrip() throws ParseException {
        Converter<CookieList> conv = Converters.COOKIE_LIST;

        // Cookie header from RFC 6265, 5.4
        String input = "name1=value1; name2=value2; name3=value3";
        CookieList parsed = conv.fromFieldValue(input);
        assertEquals(3, parsed.size());
        Iterator<HttpCookie> it = parsed.iterator();
        HttpCookie c1 = it.next();
        HttpCookie c2 = it.next();
        HttpCookie c3 = it.next();
        assertEquals("name1", c1.getName());
        assertEquals("value1", c1.getValue());
        assertEquals("name2", c2.getName());
        assertEquals("value2", c2.getValue());
        assertEquals("name3", c3.getName());
        assertEquals("value3", c3.getValue());
        // Round-trip
        String output = conv.asFieldValue(parsed);
        assertNotNull(output);
        assertTrue(output.contains("name1=value1"));
    }

    /*
     * ============================================================
     * Integration tests via HttpField
     * ============================================================
     */

    /**
     * Test HttpField round-trip for Content-Type.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">
     * RFC 7231, Section 3.1.1.5 (Content-Type)</a>
     */
    @Test
    public void testHttpFieldContentTypeRoundTrip() throws ParseException {
        String header = "Content-Type: text/html; charset=utf-8";
        HttpField<MediaType> field
            = new HttpField<>(header, Converters.MEDIA_TYPE);
        assertEquals(HttpField.CONTENT_TYPE, field.name());
        assertEquals("text", field.value().topLevelType());
        assertEquals("html", field.value().subtype());
        assertEquals("utf-8", field.value().parameter("charset"));
        assertEquals("text/html; charset=utf-8", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for ETag.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">
     * RFC 7232, Section 2.3 (ETag)</a>
     */
    @Test
    public void testHttpFieldEtagRoundTrip() throws ParseException {
        String header = "ETag: W/\"wxyzzy\"";
        HttpField<Etag> field = new HttpField<>(header, Converters.ETAG);
        assertEquals(HttpField.ETAG, field.name());
        assertTrue(field.value().isWeak());
        assertEquals("wxyzzy", field.value().tag());
        assertEquals("W/\"wxyzzy\"", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Date.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">
     * RFC 7231, Section 7.1.1.2 (Date)</a>
     */
    @Test
    public void testHttpFieldDateRoundTrip() throws ParseException {
        String header = "Date: Sun, 06 Nov 1994 08:49:37 GMT";
        HttpField<Instant> field
            = new HttpField<>(header, Converters.DATE_TIME);
        assertEquals(HttpField.DATE, field.name());
        assertNotNull(field.value());
        // Single-digit day in output
        assertEquals("Sun, 6 Nov 1994 08:49:37 GMT", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Location.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">
     * RFC 7231, Section 7.1.2 (Location)</a>
     */
    @Test
    public void testHttpFieldLocationRoundTrip() throws ParseException {
        String header = "Location: http://www.example.org/new.html";
        HttpField<URI> field = new HttpField<>(header, Converters.URI_CONV);
        assertEquals(HttpField.LOCATION, field.name());
        assertEquals("http", field.value().getScheme());
        assertEquals("www.example.org", field.value().getHost());
        assertEquals("/new.html", field.value().getPath());
        assertEquals("http://www.example.org/new.html", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Cache-Control.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">
     * RFC 7234, Section 5.2 (Cache-Control)</a>
     */
    @Test
    public void testHttpFieldCacheControlRoundTrip() throws ParseException {
        String header = "Cache-Control: max-age=3600, public";
        HttpField<CacheControlDirectives> field = new HttpField<>(header,
            Converters.CACHE_CONTROL_LIST);
        assertEquals(HttpField.CACHE_CONTROL, field.name());
        assertEquals(2, field.value().size());
        assertEquals("3600", field.value().valueForName("max-age").get());
    }

    /**
     * Test HttpField round-trip for Accept.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">
     * RFC 7231, Section 5.3.2 (Accept)</a>
     */
    @Test
    public void testHttpFieldAcceptRoundTrip() throws ParseException {
        String header = "Accept: text/html, application/json; q=0.5";
        HttpField<List<MediaRange>> field = new HttpField<>(header,
            Converters.MEDIA_RANGE_LIST);
        assertEquals(HttpField.ACCEPT, field.name());
        assertEquals(2, field.value().size());
        assertEquals("text/html", field.value().get(0).toString());
        assertEquals("application/json; q=0.5",
            field.value().get(1).toString());
    }

    /**
     * Test HttpField round-trip for Accept-Language.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">
     * RFC 7231, Section 5.3.5 (Accept-Language)</a>
     */
    @Test
    public void testHttpFieldAcceptLanguageRoundTrip() throws ParseException {
        String header = "Accept-Language: da, en-gb; q=0.8";
        HttpField<List<ParameterizedValue<Locale>>> field
            = new HttpField<>(header,
                Converters.LANGUAGE_LIST);
        assertEquals(HttpField.ACCEPT_LANGUAGE, field.name());
        assertEquals(2, field.value().size());
        assertEquals("da", field.value().get(0).value().getLanguage());
    }

    /**
     * Test HttpField round-trip for Content-Length.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">
     * RFC 7230, Section 3.3.2 (Content-Length)</a>
     */
    @Test
    public void testHttpFieldContentLengthRoundTrip() throws ParseException {
        String header = "Content-Length: 4096";
        HttpField<Long> field = new HttpField<>(header, Converters.LONG);
        assertEquals(HttpField.CONTENT_LENGTH, field.name());
        assertEquals(4096L, field.value().longValue());
        assertEquals("4096", field.asFieldValue());
        assertEquals(header, field.asHeaderField());
    }

    /**
     * Test HttpField round-trip for WWW-Authenticate.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.1">
     * RFC 7235, Section 4.1 (WWW-Authenticate)</a>
     */
    @Test
    public void testHttpFieldWwwAuthenticateRoundTrip() throws ParseException {
        String header = "WWW-Authenticate: Basic realm=\"example\"";
        HttpField<List<ParameterizedValue<String>>> field
            = new HttpField<>(header,
                Converters.CHALLENGE_LIST);
        assertEquals(HttpField.WWW_AUTHENTICATE, field.name());
        assertEquals(1, field.value().size());
        assertEquals("Basic", field.value().get(0).value());
        assertEquals("example", field.value().get(0).parameter("realm"));
    }

    /**
     * Test HttpField round-trip for Authorization.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">
     * RFC 7235, Section 4.2 (Authorization)</a>
     */
    @Test
    public void testHttpFieldAuthorizationRoundTrip() throws ParseException {
        String header = "Authorization: Basic aHR0cHdhdGNoOmY=";
        HttpField<ParameterizedValue<String>> field = new HttpField<>(header,
            Converters.CREDENTIALS);
        assertEquals(HttpField.AUTHORIZATION, field.name());
        assertEquals("Basic", field.value().value());
        assertEquals("aHR0cHdhdGNoOmY=", field.value().parameter(null));
        assertEquals("Basic aHR0cHdhdGNoOmY=", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Retry-After with seconds.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">
     * RFC 7231, Section 7.1.3 (Retry-After)</a>
     */
    @Test
    public void testHttpFieldRetryAfterSeconds() throws ParseException {
        String header = "Retry-After: 300";
        HttpField<Instant> field = new HttpField<>(header,
            Converters.SECONDS_OR_DATE_TIME);
        assertEquals(HttpField.RETRY_AFTER, field.name());
        assertNotNull(field.value());
        // Output is an absolute date
        String output = field.asFieldValue();
        assertNotNull(output);
        assertTrue(output.contains("GMT"));
    }

    /**
     * Test HttpField round-trip for Retry-After with HTTP-date.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">
     * RFC 7231, Section 7.1.3 (Retry-After)</a>
     */
    @Test
    public void testHttpFieldRetryAfterDate() throws ParseException {
        String header = "Retry-After: Mon, 01 Dec 2025 16:00:00 GMT";
        HttpField<Instant> field = new HttpField<>(header,
            Converters.SECONDS_OR_DATE_TIME);
        assertEquals(HttpField.RETRY_AFTER, field.name());
        assertEquals("Mon, 1 Dec 2025 16:00:00 GMT", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for User-Agent.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.3">
     * RFC 7231, Section 5.5.3 (User-Agent)</a>
     */
    @Test
    public void testHttpFieldUserAgentRoundTrip() throws ParseException {
        String header = "User-Agent: CERN-LineMode/2.15 libwww/2.17b3";
        HttpField<List<CommentedValue<String>>> field = new HttpField<>(header,
            Converters.PRODUCT_DESCRIPTIONS);
        assertEquals(HttpField.USER_AGENT, field.name());
        assertEquals(2, field.value().size());
        assertEquals("CERN-LineMode/2.15", field.value().get(0).value());
        assertEquals("libwww/2.17b3", field.value().get(1).value());
        // Output quotes product tokens containing /
        assertEquals("\"CERN-LineMode/2.15\"  \"libwww/2.17b3\"",
            field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Allow.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">
     * RFC 7231, Section 7.4.1 (Allow)</a>
     */
    @Test
    public void testHttpFieldAllowRoundTrip() throws ParseException {
        String header = "Allow: GET, HEAD, PUT, DELETE";
        HttpField<StringList> field
            = new HttpField<>(header, Converters.STRING_LIST);
        assertEquals(HttpField.ALLOW, field.name());
        assertEquals(4, field.value().size());
        assertEquals("GET", field.value().get(0));
        assertEquals("HEAD", field.value().get(1));
        assertEquals("PUT", field.value().get(2));
        assertEquals("DELETE", field.value().get(3));
        assertEquals("GET, HEAD, PUT, DELETE", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for If-Match.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">
     * RFC 7232, Section 3.1 (If-Match)</a>
     */
    @Test
    public void testHttpFieldIfMatchRoundTrip() throws ParseException {
        String header = "If-Match: \"xyzzy\", \"r2d2xxxx\"";
        HttpField<List<Etag>> field
            = new HttpField<>(header, Converters.ETAG_LIST);
        assertEquals(HttpField.IF_MATCH, field.name());
        assertEquals(2, field.value().size());
        assertEquals("xyzzy", field.value().get(0).tag());
        assertEquals("r2d2xxxx", field.value().get(1).tag());
    }

    /**
     * Test HttpField round-trip for If-None-Match with wildcard.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.2">
     * RFC 7232, Section 3.2 (If-None-Match)</a>
     */
    @Test
    public void testHttpFieldIfNoneMatchWildcard() throws ParseException {
        String header = "If-None-Match: *";
        HttpField<List<Etag>> field
            = new HttpField<>(header, Converters.ETAG_LIST);
        assertEquals(HttpField.IF_NONE_MATCH, field.name());
        assertEquals(1, field.value().size());
        assertEquals(Converters.WILDCARD, field.value().get(0).tag());
        assertEquals("*", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Accept-Charset.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.3">
     * RFC 7231, Section 5.3.3 (Accept-Charset)</a>
     */
    @Test
    public void testHttpFieldAcceptCharsetRoundTrip() throws ParseException {
        String header = "Accept-Charset: utf-8, iso-8859-1; q=0.5";
        HttpField<List<ParameterizedValue<String>>> field
            = new HttpField<>(header,
                Converters.WEIGHTED_STRINGS);
        assertEquals(HttpField.ACCEPT_CHARSET, field.name());
        assertEquals(2, field.value().size());
        assertEquals("utf-8", field.value().get(0).value());
        assertEquals("iso-8859-1", field.value().get(1).value());
        assertEquals("0.5", field.value().get(1).parameter("q"));
    }

    /**
     * Test HttpField round-trip for Pragma.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">
     * RFC 7234, Section 5.4 (Pragma)</a>
     */
    @Test
    public void testHttpFieldPragmaRoundTrip() throws ParseException {
        String header = "Pragma: no-cache";
        HttpField<List<Directive>> field = new HttpField<>(header,
            Converters.DIRECTIVE_LIST);
        assertEquals(HttpField.PRAGMA, field.name());
        assertEquals(1, field.value().size());
        assertEquals("no-cache", field.value().get(0).name());
        assertEquals("no-cache", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Connection.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.1">
     * RFC 7230, Section 6.1 (Connection)</a>
     */
    @Test
    public void testHttpFieldConnectionRoundTrip() throws ParseException {
        String header = "Connection: keep-alive, Upgrade";
        HttpField<StringList> field = new HttpField<>(header,
            Converters.STRING_LIST);
        assertEquals(HttpField.CONNECTION, field.name());
        assertEquals(2, field.value().size());
        assertEquals("keep-alive", field.value().get(0));
        assertEquals("Upgrade", field.value().get(1));
        assertEquals("keep-alive, Upgrade", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Transfer-Encoding.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">
     * RFC 7230, Section 3.3.1 (Transfer-Encoding)</a>
     */
    @Test
    public void testHttpFieldTransferEncodingRoundTrip() throws ParseException {
        String header = "Transfer-Encoding: chunked";
        HttpField<StringList> field = new HttpField<>(header,
            Converters.STRING_LIST);
        assertEquals(HttpField.TRANSFER_ENCODING, field.name());
        assertEquals(1, field.value().size());
        assertEquals("chunked", field.value().get(0));
        assertEquals("chunked", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Host.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.4">
     * RFC 7230, Section 5.4 (Host)</a>
     */
    @Test
    public void testHttpFieldHostRoundTrip() throws ParseException {
        String header = "Host: www.example.org";
        HttpField<String> field
            = new HttpField<>(header, Converters.UNQUOTED_STRING);
        assertEquals(HttpField.HOST, field.name());
        assertEquals("www.example.org", field.value());
        assertEquals("www.example.org", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Max-Forwards.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.2">
     * RFC 7231, Section 5.1.2 (Max-Forwards)</a>
     */
    @Test
    public void testHttpFieldMaxForwardsRoundTrip() throws ParseException {
        String header = "Max-Forwards: 10";
        HttpField<Long> field = new HttpField<>(header, Converters.LONG);
        assertEquals(HttpField.MAX_FORWARDS, field.name());
        assertEquals(10L, field.value().longValue());
        assertEquals("10", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Expires.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.3">
     * RFC 7234, Section 5.3 (Expires)</a>
     */
    @Test
    public void testHttpFieldExpiresRoundTrip() throws ParseException {
        String header = "Expires: Mon, 01 Dec 2025 16:00:00 GMT";
        HttpField<Instant> field
            = new HttpField<>(header, Converters.DATE_TIME);
        assertEquals(HttpField.EXPIRES, field.name());
        assertEquals("Mon, 1 Dec 2025 16:00:00 GMT", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Age.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.1">
     * RFC 7234, Section 5.1 (Age)</a>
     */
    @Test
    public void testHttpFieldAgeRoundTrip() throws ParseException {
        String header = "Age: 12345";
        HttpField<Long> field = new HttpField<>(header, Converters.LONG);
        assertEquals(HttpField.AGE, field.name());
        assertEquals(12345L, field.value().longValue());
        assertEquals("12345", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Server.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">
     * RFC 7231, Section 7.4.2 (Server)</a>
     */
    @Test
    public void testHttpFieldServerRoundTrip() throws ParseException {
        String header = "Server: Apache/2.4.41";
        HttpField<List<CommentedValue<String>>> field = new HttpField<>(header,
            Converters.PRODUCT_DESCRIPTIONS);
        assertEquals(HttpField.SERVER, field.name());
        assertEquals(1, field.value().size());
        assertEquals("Apache/2.4.41", field.value().get(0).value());
        // Output quotes product tokens containing /
        assertEquals("\"Apache/2.4.41\"", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Upgrade.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.7">
     * RFC 7230, Section 6.7 (Upgrade)</a>
     */
    @Test
    public void testHttpFieldUpgradeRoundTrip() throws ParseException {
        String header = "Upgrade: h2c, SHTTP/1.3, IRC/6.9, RTA/x11";
        HttpField<StringList> field = new HttpField<>(header,
            Converters.STRING_LIST);
        assertEquals(HttpField.UPGRADE, field.name());
        assertEquals(4, field.value().size());
        assertEquals("h2c", field.value().get(0));
        assertEquals("SHTTP/1.3", field.value().get(1));
    }

    /**
     * Test HttpField round-trip for Via.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.7.1">
     * RFC 7230, Section 5.7.1 (Via)</a>
     */
    @Test
    public void testHttpFieldViaRoundTrip() throws ParseException {
        String header = "Via: 1.0 fred, 1.1 example.com";
        HttpField<StringList> field = new HttpField<>(header,
            Converters.STRING_LIST);
        assertEquals(HttpField.VIA, field.name());
        assertEquals(2, field.value().size());
        assertEquals("1.0 fred", field.value().get(0));
        assertEquals("1.1 example.com", field.value().get(1));
    }

    /**
     * Test HttpField round-trip for Warning.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.5">
     * RFC 7234, Section 5.5 (Warning)</a>
     */
    @Test
    public void testHttpFieldWarningRoundTrip() throws ParseException {
        String header = "Warning: 110 - \"Response is stale\"";
        HttpField<String> field = new HttpField<>(header, Converters.STRING);
        assertEquals(HttpField.WARNING, field.name());
        assertEquals("110 - \"Response is stale\"", field.value());
    }

    /**
     * Test HttpField round-trip for Content-Location.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">
     * RFC 7231, Section 3.1.4.2 (Content-Location)</a>
     */
    @Test
    public void testHttpFieldContentLocationRoundTrip() throws ParseException {
        String header = "Content-Location: /images/logo.jpg";
        HttpField<URI> field = new HttpField<>(header, Converters.URI_CONV);
        assertEquals(HttpField.CONTENT_LOCATION, field.name());
        assertEquals("/images/logo.jpg", field.value().getPath());
        assertEquals("/images/logo.jpg", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Last-Modified.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.2">
     * RFC 7232, Section 2.2 (Last-Modified)</a>
     */
    @Test
    public void testHttpFieldLastModifiedRoundTrip() throws ParseException {
        String header = "Last-Modified: Mon, 01 Dec 2025 16:00:00 GMT";
        HttpField<Instant> field
            = new HttpField<>(header, Converters.DATE_TIME);
        assertEquals(HttpField.LAST_MODIFIED, field.name());
        assertEquals("Mon, 1 Dec 2025 16:00:00 GMT", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for If-Modified-Since.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.3">
     * RFC 7232, Section 3.3 (If-Modified-Since)</a>
     */
    @Test
    public void testHttpFieldIfModifiedSinceRoundTrip() throws ParseException {
        String header = "If-Modified-Since: Wed, 29 Oct 2025 19:43:31 GMT";
        HttpField<Instant> field
            = new HttpField<>(header, Converters.DATE_TIME);
        assertEquals(HttpField.IF_MODIFIED_SINCE, field.name());
        assertEquals("Wed, 29 Oct 2025 19:43:31 GMT", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for If-Unmodified-Since.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.4">
     * RFC 7232, Section 3.4 (If-Unmodified-Since)</a>
     */
    @Test
    public void testHttpFieldIfUnmodifiedSinceRoundTrip()
            throws ParseException {
        String header = "If-Unmodified-Since: Wed, 29 Nov 2023 22:44:06 GMT";
        HttpField<Instant> field
            = new HttpField<>(header, Converters.DATE_TIME);
        assertEquals(HttpField.IF_UNMODIFIED_SINCE, field.name());
        assertEquals("Wed, 29 Nov 2023 22:44:06 GMT", field.asFieldValue());
    }

    /**
     * Test HttpField round-trip for Proxy-Authorization.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.4">
     * RFC 7235, Section 4.4 (Proxy-Authorization)</a>
     */
    @Test
    public void testHttpFieldProxyAuthorizationRoundTrip()
            throws ParseException {
        String header = "Proxy-Authorization: Basic YWJjOjEyMw==";
        HttpField<ParameterizedValue<String>> field = new HttpField<>(header,
            Converters.CREDENTIALS);
        assertEquals(HttpField.PROXY_AUTHORIZATION, field.name());
        assertEquals("Basic", field.value().value());
        assertEquals("YWJjOjEyMw==", field.value().parameter(null));
    }

    /**
     * Test HttpField round-trip for Proxy-Authenticate.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.3">
     * RFC 7235, Section 4.3 (Proxy-Authenticate)</a>
     */
    @Test
    public void testHttpFieldProxyAuthenticateRoundTrip()
            throws ParseException {
        String header
            = "Proxy-Authenticate: Digest realm=\"proxy\", nonce=\"abc\"";
        HttpField<List<ParameterizedValue<String>>> field
            = new HttpField<>(header,
                Converters.CHALLENGE_LIST);
        assertEquals(HttpField.PROXY_AUTHENTICATE, field.name());
        assertEquals(1, field.value().size());
        assertEquals("Digest", field.value().get(0).value());
        assertEquals("proxy", field.value().get(0).parameter("realm"));
    }

    /**
     * Test HttpField round-trip for Trailer.
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.4">
     * RFC 7230, Section 4.4 (Trailer)</a>
     */
    @Test
    public void testHttpFieldTrailerRoundTrip() throws ParseException {
        String header = "Trailer: MAC-Key, SHA-256";
        HttpField<StringList> field = new HttpField<>(header,
            Converters.STRING_LIST);
        assertEquals(HttpField.TRAILER, field.name());
        assertEquals(2, field.value().size());
        assertEquals("MAC-Key", field.value().get(0));
        assertEquals("SHA-256", field.value().get(1));
        assertEquals("MAC-Key, SHA-256", field.asFieldValue());
    }
}
