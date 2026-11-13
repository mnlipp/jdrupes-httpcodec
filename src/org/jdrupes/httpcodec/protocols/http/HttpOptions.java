/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2026  Michael N. Lipp
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

import org.jdrupes.httpcodec.Options;

/**
 * Common HTTP specific options for the codec.
 */
public class HttpOptions extends Options {

    private boolean laxServerHeaderParsing = false;

    /**
     * Initializes the options.
     */
    public HttpOptions() {
        // Make javadoc happy
    }

    /**
     * Enable or disable lax server header parsing (default disabled).
     * Some HTTP servers (notably
     * [Gerbera](https://github.com/gerbera/gerbera/issues/4060)) send
     * illegal server headers. Setting this flags supports headers in the
     * format
     * <code>
     * Server = product-name *( "," [SP] string )
     * product-name = 1*( %x00-2B / %x2D-3A / %x3C-FF )
     * </code>
     *
     * @param enabled the enabled
     * @return the http options
     */
    public HttpOptions laxServerHeaderParsing(boolean enabled) {
        laxServerHeaderParsing = enabled;
        return this;
    }

    /**
     * Return the state of lax server header parsing.
     *
     * @return true, if enabled
     * @see #laxServerHeaderParsing(boolean)
     */
    public boolean laxServerHeaderParsing() {
        return laxServerHeaderParsing;
    }
}
