/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016, 2017  Michael N. Lipp
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

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implemented by converters that convert header fields with a list of values.
 * 
 * Minimal restrictions are imposed on the type used as container for the 
 * values. It must be {@link Iterable} to provide read access. A supplier
 * and a function for appending values provide the required write access.
 * 
 * @param <T> the container for the values
 * @param <V> the type of the values
 */
public interface MultiValueConverter<T extends Iterable<V>, V>
        extends Converter<T> {

    /**
     * Returns the container supplier
     * 
     * @return the container supplier
     */
    Supplier<T> containerSupplier();

    /**
     * Returns the value adder
     * 
     * @return the value adder
     */
    BiConsumer<T, V> valueAdder();

    /**
     * Returns the value converter.
     * 
     * @return the value converter
     * @deprecated Use {@link #valueConverter(Iterable)} instead. 
     */
    @Deprecated
    Converter<V> valueConverter();

    /**
     * Returns the value converter. In most cases, the result will be
     * independent of the container type or instance. However, passing
     * it makes the selection more flexible.
     * 
     * @return the value converter
     */
    Converter<V> valueConverter(T value);

    /**
     * Return whether values should be converted to separate
     * header fields in {@link Converter#asFieldValue(Object)}.
     * 
     * @return the value
     */
    boolean separateValues();

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
        if (!separateValues()) {
            // Cannot call super here.
            return fieldName + ": " + asFieldValue(value);
        }
        // Convert list of items to separate fields
        var valueConverter = valueConverter(value);
        return StreamSupport.stream(value.spliterator(), false).map(
            item -> fieldName + ": " + valueConverter.asFieldValue(item))
            .collect(Collectors.joining("\r\n"));

    }

}