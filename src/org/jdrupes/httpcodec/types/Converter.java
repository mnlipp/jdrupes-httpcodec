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

import java.text.ParseException;

/**
 * Implemented by classes that convert between a value and its 
 * string representation in the HTTP header field.
 * 
 * @param <T> the type to be converted
 */
public interface Converter<T> {

    /**
     * Returns the representation of this value in a header field.
     * 
     * @param value the value to be converted
     * @return the representation
     */
    String asFieldValue(T value);

    /**
     * Returns the string representation of this header field as it 
     * appears in an HTTP message. Note that the returned string may 
     * span several lines (may contain CR/LF), if the converter is a
     * {@link MultiValueConverter} with separate values, but never 
     * has a trailing CR/LF.
     *
     * @param fieldName the field name
     * @param value the value
     * @return the field as it occurs in a header
     */
    default String asHeaderField(String fieldName, T value) {
        return fieldName + ": " + asFieldValue(value);
    }

    /**
     * Parses the given text and returns the parsed value.
     * 
     * @param text the value from the header field
     * @return the parsed value
     * @throws ParseException if the value cannot be parsed
     */
    T fromFieldValue(String text) throws ParseException;
}