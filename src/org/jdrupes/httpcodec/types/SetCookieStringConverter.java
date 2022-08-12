/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2022 Michael N. Lipp
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
import java.text.ParseException;

import org.jdrupes.httpcodec.types.Converters.SameSiteAttribute;

/**
 * Converts a {@link HttpCookie} to the representation used in
 * a "Set-Cookie" header.
 * 
 * @see "[Set-Cookie](https://www.rfc-editor.org/rfc/rfc6265#section-4.1)"
 */
public class SetCookieStringConverter implements Converter<HttpCookie> {

    private SameSiteAttribute sameSiteAttribute = SameSiteAttribute.UNSET;

    /**
     * Returns the same site attribute.
     *
     * @return the same site attribute
     */
    public SameSiteAttribute sameSiteAttribute() {
        return sameSiteAttribute;
    }

    /**
     * Controls if and which same site attribute is added to the string.
     *
     * @param sameSiteAttribute the new same site attribute
     */
    public SetCookieStringConverter
            setSameSiteAttribute(SameSiteAttribute sameSiteAttribute) {
        this.sameSiteAttribute = sameSiteAttribute;
        return this;
    }

    @Override
    public String asFieldValue(HttpCookie cookie) {
        StringBuilder result = new StringBuilder();
        result.append(cookie.getName()).append('=');
        if (cookie.getValue() != null) {
            result.append(cookie.getValue());
        }
        if (cookie.getMaxAge() > -1) {
            result.append("; Max-Age=");
            result.append(Long.toString(cookie.getMaxAge()));
        }
        if (cookie.getDomain() != null) {
            result.append("; Domain=");
            result.append(cookie.getDomain());
        }
        if (cookie.getPath() != null) {
            result.append("; Path=");
            result.append(cookie.getPath());
        }
        if (cookie.getSecure()) {
            result.append("; Secure");
        }
        if (cookie.isHttpOnly()) {
            result.append("; HttpOnly");
        }
        if (sameSiteAttribute != SameSiteAttribute.UNSET) {
            result.append("; SameSite=").append(sameSiteAttribute.value());
        }
        return result.toString();
    }

    @Override
    public HttpCookie fromFieldValue(String text)
            throws ParseException {
        throw new UnsupportedOperationException();
    }

}
