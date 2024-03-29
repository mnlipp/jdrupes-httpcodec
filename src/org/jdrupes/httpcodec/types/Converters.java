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

import java.net.HttpCookie;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jdrupes.httpcodec.protocols.http.HttpConstants;
import org.jdrupes.httpcodec.types.CommentedValue.CommentedValueConverter;
import org.jdrupes.httpcodec.types.Directive.DirectiveConverter;
import org.jdrupes.httpcodec.types.Etag.EtagConverter;
import org.jdrupes.httpcodec.types.MediaBase.MediaTypePair;
import org.jdrupes.httpcodec.types.MediaBase.MediaTypePairConverter;
import org.jdrupes.httpcodec.types.MediaRange.MediaRangeConverter;
import org.jdrupes.httpcodec.types.MediaType.MediaTypeConverter;
import org.jdrupes.httpcodec.types.ParameterizedValue.ParamValueConverterBase;
import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * Utility methods and singletons for converters.
 */
public final class Converters {

    /**
     * Used to control the generation of "Set-Cookie" header fields.
     */
    public enum SameSiteAttribute {

        /**
         * Don't set the attribute.
         */
        UNSET("Unset"),

        /**
         * Always sent.
         */
        NONE("None"),

        /**
         * Sent with "same-site" requests, and with "cross-site" top-level navigations
         */
        LAX("Lax"),

        /**
         * Sent along with "same-site" requests
         */
        STRICT("Strict");

        private final String value;

        SameSiteAttribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /*
     * Note that the initialization sequence is important.
     * Converters used by others must be defined first.
     */

    /**
     * A noop converter, except that text is trimmed when converted to
     * a value.
     */
    public static final Converter<
            String> UNQUOTED_STRING = new Converter<String>() {

                @Override
                public String asFieldValue(String value) {
                    return value;
                }

                @Override
                public String fromFieldValue(String text)
                        throws ParseException {
                    return text.trim();
                }
            };

    /**
     * A noop converter, except that text is trimmed and unquoted
     * when converted to a value.
     */
    public static final Converter<String> UNQUOTE_ONLY
        = new Converter<String>() {

            @Override
            public String asFieldValue(String value) {
                return value;
            }

            @Override
            public String fromFieldValue(String text) throws ParseException {
                return unquoteString(text.trim());
            }
        };

    /**
     * A converter that quotes and unquoted strings as necessary.
     */
    public static final Converter<String> STRING = new Converter<String>() {

        @Override
        public String asFieldValue(String value) {
            return quoteIfNecessary(value);
        }

        @Override
        public String fromFieldValue(String text) throws ParseException {
            return unquoteString(text.trim());
        }
    };

    public static final Converter<StringList> STRING_LIST
        = new DefaultMultiValueConverter<>(StringList::new, STRING);

    /**
     * A converter that quotes strings.
     */
    public static final Converter<String> QUOTED_STRING
        = new Converter<String>() {

            @Override
            public String asFieldValue(String value) {
                return quoteString(value);
            }

            @Override
            public String fromFieldValue(String text) throws ParseException {
                return unquoteString(text.trim());
            }
        };

    public static final Converter<StringList> QUOTED_STRING_LIST
        = new DefaultMultiValueConverter<>(StringList::new, QUOTED_STRING);

    /**
     * An integer converter.
     */
    public static final Converter<Long> LONG = new Converter<Long>() {

        @Override
        public String asFieldValue(Long value) {
            return value.toString();
        }

        @Override
        public Long fromFieldValue(String text) throws ParseException {
            try {
                return Long.parseLong(unquoteString(text));
            } catch (NumberFormatException e) {
                throw new ParseException(text, 0);
            }
        }
    };

    /**
     * An integer list converter.
     */
    public static final MultiValueConverter<List<Long>, Long> LONG_LIST
        = new DefaultMultiValueConverter<>(ArrayList<Long>::new, LONG);

    /**
     * A date/time converter.
     */
    public static final Converter<Instant> DATE_TIME
        = new InstantConverter();

    /** 
     * Provide converters for the 
     */
    public static final Map<SameSiteAttribute,
            SetCookieStringConverter> SET_COOKIE_STRING = Map.of(
                SameSiteAttribute.UNSET, new SetCookieStringConverter(),
                SameSiteAttribute.NONE, new SetCookieStringConverter()
                    .setSameSiteAttribute(SameSiteAttribute.NONE),
                SameSiteAttribute.LAX, new SetCookieStringConverter()
                    .setSameSiteAttribute(SameSiteAttribute.LAX),
                SameSiteAttribute.STRICT, new SetCookieStringConverter()
                    .setSameSiteAttribute(SameSiteAttribute.STRICT));

    /**
     * A converter for set cookies.
     */
    public static final Converter<CookieList> SET_COOKIE
        = new DefaultMultiValueConverter<CookieList, HttpCookie>(
            CookieList::new, CookieList::add,
            SET_COOKIE_STRING.get(SameSiteAttribute.UNSET), ",", true) {

            @Override
            public CookieList fromFieldValue(String text)
                    throws ParseException {
                try {
                    return new CookieList(HttpCookie.parse(text));
                } catch (IllegalArgumentException e) {
                    throw new ParseException(text, 0);
                }
            }

            /**
             * Not supported because {@link #separateValues()} is `true`.
             */
            @Override
            public String asFieldValue(CookieList value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Converter<HttpCookie> valueConverter(CookieList value) {
                return Converters.SET_COOKIE_STRING
                    .get(value.sameSiteAttribute());
            }
        };

    /**
     * A converter for a list of cookies. Supports the format used in
     * the "Cookie" header.
     * 
     * @see "[Cookie](https://www.rfc-editor.org/rfc/rfc6265#section-4.2)"
     */
    public static final MultiValueConverter<CookieList,
            HttpCookie> COOKIE_LIST
                = new DefaultMultiValueConverter<CookieList, HttpCookie>(
                    CookieList::new, CookieList::add,
                    new Converter<HttpCookie>() {

                        @Override
                        public String asFieldValue(HttpCookie value) {
                            return value.toString();
                        }

                        @Override
                        public HttpCookie fromFieldValue(String text)
                                throws ParseException {
                            try {
                                return HttpCookie.parse(text).get(0);
                            } catch (IllegalArgumentException e) {
                                throw new ParseException(text, 0);
                            }
                        }
                    }, ";", false);

    /**
     * A converter for a language or language range. 
     * Language range "`*`" is converted to a Locale with an empty language.
     */
    public static final Converter<Locale> LANGUAGE = new Converter<Locale>() {

        @Override
        public String asFieldValue(Locale value) {
            return value.getCountry().length() == 0 ? value.getLanguage()
                : (value.getLanguage() + "-" + value.getCountry());
        }

        @Override
        public Locale fromFieldValue(String text) throws ParseException {
            return Locale.forLanguageTag(text);
        }
    };

    /**
     * A converter for a weighted list of languages.
     */
    public static final MultiValueConverter<List<ParameterizedValue<Locale>>,
            ParameterizedValue<Locale>> LANGUAGE_LIST
                = new DefaultMultiValueConverter<
                        List<ParameterizedValue<Locale>>,
                        ParameterizedValue<Locale>>(
                            ArrayList::new,
                            new ParamValueConverterBase<
                                    ParameterizedValue<Locale>, Locale>(
                                        LANGUAGE,
                                        ParameterizedValue<Locale>::new) {
                            });

    /**
     * A converter for a weighted list of strings.
     */
    public static final MultiValueConverter<List<ParameterizedValue<String>>,
            ParameterizedValue<String>> WEIGHTED_STRINGS
                = new DefaultMultiValueConverter<
                        List<ParameterizedValue<String>>,
                        ParameterizedValue<String>>(
                            ArrayList::new,
                            new ParamValueConverterBase<
                                    ParameterizedValue<String>, String>(STRING,
                                        ParameterizedValue<String>::new) {
                            });

    /**
     * A converter for the media "topLevelType/Subtype" pair.
     */
    public static final Converter<MediaTypePair> MEDIA_TYPE_PAIR
        = new MediaTypePairConverter();

    /**
     * A converter for a media type pair with parameters.
     */
    public static final Converter<MediaRange> MEDIA_RANGE
        = new MediaRangeConverter();

    /**
     * A converter for a list of media ranges.
     */
    public static final MultiValueConverter<List<MediaRange>,
            MediaRange> MEDIA_RANGE_LIST
                = new DefaultMultiValueConverter<List<MediaRange>,
                        MediaRange>(ArrayList::new, MEDIA_RANGE);

    /**
     * A converter for a media type pair with parameters.
     */
    public static final Converter<MediaType> MEDIA_TYPE
        = new MediaTypeConverter();

    /**
     * A converter for a directive.
     */
    public static final DirectiveConverter DIRECTIVE
        = new DirectiveConverter();

    /**
     * A converter for a list of directives.
     */
    public static final MultiValueConverter<List<Directive>,
            Directive> DIRECTIVE_LIST
                = new DefaultMultiValueConverter<List<Directive>, Directive>(
                    ArrayList::new, DIRECTIVE);

    /**
     * A converter for cache control directives.
     */
    public static final MultiValueConverter<CacheControlDirectives,
            Directive> CACHE_CONTROL_LIST
                = new DefaultMultiValueConverter<CacheControlDirectives,
                        Directive>(CacheControlDirectives::new,
                            (left, right) -> {
                                left.add(right);
                            }, DIRECTIVE, ",", false);
    /**
     * A converter for a URI.
     */
    public static final Converter<URI> URI_CONV = new Converter<URI>() {

        @Override
        public String asFieldValue(URI value) {
            return value.toString();
        }

        @Override
        public URI fromFieldValue(String text) throws ParseException {
            try {
                return URI.create(text);
            } catch (IllegalArgumentException e) {
                throw new ParseException(e.getMessage(), 0);
            }
        }
    };

    /**
     * A converter for product descriptions as used in the `User-Agent`
     * and `Server` header fields.
     */
    public static final ProductDescriptionConverter PRODUCT_DESCRIPTIONS
        = new ProductDescriptionConverter();

    /**
     * Used by the {@link EtagConverter} to unambiguously denote
     * a decoded wildcard. If the result of `fromFieldValue` == 
     * `WILDCARD`, the field value was an unquoted asterisk.
     * If the result `equals("*")`, it may also have been
     * a quoted asterisk.
     */
    public static final String WILDCARD = "*";

    /**
     * A converter for an ETag header.
     */
    public static final EtagConverter ETAG = new EtagConverter();

    public static final Converter<List<Etag>> ETAG_LIST
        = new DefaultMultiValueConverter<>(ArrayList::new, ETAG);

    /**
     * A converter for a list of challenges.
     */
    public static final Converter<
            List<ParameterizedValue<String>>> CHALLENGE_LIST
                = new AuthInfoConverter();

    public static final Converter<ParameterizedValue<
            String>> CREDENTIALS = new Converter<ParameterizedValue<String>>() {

                @Override
                public String asFieldValue(ParameterizedValue<String> value) {
                    List<ParameterizedValue<String>> tmp = new ArrayList<>();
                    tmp.add(value);
                    return CHALLENGE_LIST.asFieldValue(tmp);
                }

                @Override
                public ParameterizedValue<String> fromFieldValue(String text)
                        throws ParseException {
                    return CHALLENGE_LIST.fromFieldValue(text).get(0);
                }

            };

    private Converters() {
    }

    /**
     * If the string contains a char with a backslash before it,
     * remove the backslash.
     * 
     * @param value the value to unquote
     * @return the unquoted value
     * @throws ParseException if the input violates the field format
     * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
     */
    public static String unquote(String value) {
        StringBuilder result = new StringBuilder();
        boolean pendingBackslash = false;
        for (char ch : value.toCharArray()) {
            switch (ch) {
            case '\\':
                if (pendingBackslash) {
                    result.append(ch);
                } else {
                    pendingBackslash = true;
                    continue;
                }
                break;

            default:
                result.append(ch);
                break;
            }
            pendingBackslash = false;
        }
        return result.toString();
    }

    /**
     * If the value is double quoted, remove the quotes and escape
     * characters.
     * 
     * @param value the value to unquote
     * @return the unquoted value
     * @throws ParseException if the input violates the field format
     * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
     */
    public static String unquoteString(String value) throws ParseException {
        if (value.length() == 0 || value.charAt(0) != '\"') {
            return value;
        }
        String unquoted = unquote(value);
        if (!unquoted.endsWith("\"")) {
            throw new ParseException(value, value.length() - 1);
        }
        return unquoted.substring(1, unquoted.length() - 1);
    }

    /**
     * Returns the given string as double quoted string if necessary.
     * 
     * @param value the value to quote if necessary
     * @return the result
     * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
     */
    public static String quoteIfNecessary(String value) {
        StringBuilder result = new StringBuilder();
        boolean needsQuoting = false;
        result.append('"');
        for (char ch : value.toCharArray()) {
            if (!needsQuoting && HttpConstants.TOKEN_CHARS.indexOf(ch) < 0) {
                needsQuoting = true;
            }
            switch (ch) {
            case '"':
                // fall through
            case '\\':
                result.append('\\');
                // fall through
            default:
                result.append(ch);
                break;
            }
        }
        result.append('\"');
        if (needsQuoting) {
            return result.toString();
        }
        return value;
    }

    /**
     * Returns the given string as double quoted string.
     * 
     * @param value the value to quote
     * @return the result
     */
    public static String quoteString(String value) {
        StringBuilder result = new StringBuilder();
        result.append('"');
        for (char ch : value.toCharArray()) {
            switch (ch) {
            case '"':
                // fall through
            case '\\':
                result.append('\\');
                // fall through
            default:
                result.append(ch);
                break;
            }
        }
        result.append('\"');
        return result.toString();
    }

    /**
     * Return a new string in which all characters from `toBeQuoted`
     * are prefixed with a backslash. 
     * 
     * @param value the string
     * @param toBeQuoted the characters to be quoted
     * @return the result
     * @see "[Field value components](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
     */
    public static String quote(String value, String toBeQuoted) {
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (toBeQuoted.indexOf(ch) >= 0) {
                result.append('\\');
            }
            result.append(ch);
        }
        return result.toString();
    }

    /**
     * Determines the length of a token in a header field
     * 
     * @param text the text to parse
     * @param startPos the start position
     * @return the length of the token
     * @see "[RFC 7230, Section 3.2.6](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
     */
    public static int tokenLength(String text, int startPos) {
        int pos = startPos;
        while (pos < text.length()
            && HttpConstants.TOKEN_CHARS.indexOf(text.charAt(pos)) >= 0) {
            pos += 1;
        }
        return pos - startPos;
    }

    /**
     * Determines the length of a token68 in a header field
     * 
     * @param text the text to parse
     * @param startPos the start position
     * @return the length of the token
     * @see "[RFC 7235, Section 2.1](https://tools.ietf.org/html/rfc7235#section-2.1)"
     */
    public static int token68Length(String text, int startPos) {
        int pos = startPos;
        while (pos < text.length()
            && HttpConstants.TOKEN68_CHARS.indexOf(text.charAt(pos)) >= 0) {
            pos += 1;
        }
        return pos - startPos;
    }

    /**
     * Determines the length of a white space sequence in a header field. 
     * 
     * @param text the test to parse 
     * @param startPos the start position
     * @return the length of the white space sequence
     * @see "[RFC 7230, Section 3.2.3](https://tools.ietf.org/html/rfc7230#section-3.2.3)"
     */
    public static int whiteSpaceLength(String text, int startPos) {
        int pos = startPos;
        while (pos < text.length()) {
            switch (text.charAt(pos)) {
            case ' ':
                // fall through
            case '\t':
                pos += 1;
                continue;

            default:
                break;
            }
            break;
        }
        return pos - startPos;
    }

    /**
     * Determines the length of a comment in a header field.
     * 
     * @param text the text to parse
     * @param startPos the starting position (must be the position of the
     * opening brace)
     * @return the length of the comment
     * @see "[RFC 7230, Section 3.2.6](https://tools.ietf.org/html/rfc7230#section-3.2.6)"
     */
    public static int commentLength(String text, int startPos) {
        int pos = startPos + 1;
        while (pos < text.length()) {
            switch (text.charAt(pos)) {
            case ')':
                return pos - startPos + 1;

            case '(':
                pos += commentLength(text, pos);
                break;

            case '\\':
                pos = Math.min(pos + 2, text.length());
                break;

            default:
                pos += 1;
                break;
            }
        }
        return pos - startPos;
    }

    /**
     * Returns the length up to one of the match chars or end of string.
     * 
     * @param text the text
     * @param startPos the start position
     * @param matches the chars to match
     * @return the length
     */
    public int unmatchedLength(String text, int startPos, String matches) {
        int pos = startPos;
        while (pos < text.length()) {
            if (matches.indexOf(text.charAt(pos)) >= 0) {
                return pos - startPos;
            }
            pos += 1;
        }
        return pos - startPos;
    }

    private static class ProductDescriptionConverter
            extends DefaultMultiValueConverter<List<CommentedValue<String>>,
                    CommentedValue<String>> {

        public ProductDescriptionConverter() {
            super(ArrayList<CommentedValue<String>>::new,
                new CommentedValueConverter<>(Converters.STRING));
        }

        /*
         * (non-Javadoc)
         * 
         * @see Converter#fromFieldValue(java.lang.String)
         */
        @Override
        public List<CommentedValue<String>> fromFieldValue(String text)
                throws ParseException {
            List<CommentedValue<String>> result = new ArrayList<>();
            int pos = 0;
            while (pos < text.length()) {
                int length = Converters.tokenLength(text, pos);
                if (length == 0) {
                    throw new ParseException(
                        "Must start with token: " + text, pos);
                }
                String product = text.substring(pos, pos + length);
                pos += length;
                if (pos < text.length() && text.charAt(pos) == '/') {
                    pos += 1;
                    length = Converters.tokenLength(text, pos);
                    if (length == 0) {
                        throw new ParseException(
                            "Token expected: " + text, pos);
                    }
                    product = product + text.substring(pos - 1, pos + length);
                    pos += length;
                }
                List<String> comments = new ArrayList<>();
                while (pos < text.length()) {
                    length = Converters.whiteSpaceLength(text, pos);
                    if (length == 0) {
                        throw new ParseException(
                            "Whitespace expected: " + text, pos);
                    }
                    pos += length;
                    if (text.charAt(pos) != '(') {
                        break;
                    }
                    length = Converters.commentLength(text, pos);
                    if (text.charAt(pos + length - 1) != ')') {
                        throw new ParseException(
                            "Comment end expected: " + text,
                            pos + length - 1);
                    }
                    comments.add(text.substring(pos + 1, pos + length - 1));
                    pos += length;
                }
                result.add(new CommentedValue<String>(product,
                    comments.size() == 0 ? null
                        : comments
                            .toArray(new String[comments.size()])));
            }
            return result;
        }

    }

    private static class AuthInfoConverter extends
            DefaultMultiValueConverter<List<ParameterizedValue<String>>,
                    ParameterizedValue<String>> {

        public AuthInfoConverter() {
            super(ArrayList<ParameterizedValue<String>>::new,
                new Converter<ParameterizedValue<String>>() {

                    @Override
                    public String
                            asFieldValue(ParameterizedValue<String> value) {
                        StringBuilder result = new StringBuilder();
                        result.append(value.value());
                        boolean first = true;
                        for (Map.Entry<String, String> e : value.parameters()
                            .entrySet()) {
                            if (first) {
                                first = false;
                            } else {
                                result.append(',');
                            }
                            result.append(' ');
                            if (e.getKey() == null) {
                                result.append(e.getValue());
                            } else {
                                result.append(e.getKey());
                                result.append("=");
                                result.append(quoteIfNecessary(e.getValue()));
                            }
                        }
                        return result.toString();
                    }

                    @Override
                    public ParameterizedValue<String> fromFieldValue(
                            String text) throws ParseException {
                        throw new UnsupportedOperationException();
                    }
                }, ",");
        }

        @Override
        public List<ParameterizedValue<String>> fromFieldValue(String text)
                throws ParseException {
            List<ParameterizedValue<String>> result = new ArrayList<>();
            ListItemizer itemizer = new ListItemizer(text, ",");
            ParameterizedValue.Builder<ParameterizedValue<String>,
                    String> builder = null;
            String itemRepr = null;
            while (true) {
                // New auth scheme may have left over the parameter part as
                // itemRepr
                if (itemRepr == null) {
                    if (!itemizer.hasNext()) {
                        if (builder != null) {
                            result.add(builder.build());
                        }
                        break;
                    }
                    itemRepr = itemizer.next();
                }
                if (builder != null) {
                    // itemRepr may be new auth scheme or parameter
                    ListItemizer paramItemizer
                        = new ListItemizer(itemRepr, "=");
                    String name = paramItemizer.next();
                    if (paramItemizer.hasNext() && name.indexOf(" ") < 0) {
                        // Really parameter
                        builder.setParameter(name, unquoteString(
                            paramItemizer.next()));
                        itemRepr = null;
                        continue;
                    }
                    // new challenge or credentials
                    result.add(builder.build());
                    builder = null;
                    // fall through
                }
                // New challenge or credentials, space used as separator
                ListItemizer schemeItemizer = new ListItemizer(itemRepr, " ");
                String authScheme = schemeItemizer.next();
                if (authScheme == null) {
                    throw new ParseException(itemRepr, 0);
                }
                builder = ParameterizedValue.builder();
                builder.setValue(authScheme);
                itemRepr = schemeItemizer.next();
                if (itemRepr == null
                    || (token68Length(itemRepr, 0) == itemRepr.length())) {
                    if (itemRepr != null) {
                        builder.setParameter(null, itemRepr);
                    }
                    result.add(builder.build());
                    builder = null;
                    // Fully processed
                    itemRepr = null;
                    continue;
                }
            }
            return result;
        }

    }

}
