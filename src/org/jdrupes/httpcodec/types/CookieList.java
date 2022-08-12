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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Stream;

import org.jdrupes.httpcodec.types.Converters.SameSiteAttribute;

/**
 * Represents a list of cookies. The additional property "same site attribute"
 * controls the generation of header fields.
 */
public class CookieList implements Iterable<HttpCookie> {

    private LinkedHashMap<String, HttpCookie> cookies;
    private final SameSiteAttribute sameSiteAttribute;

    /**
     * Creates a new empty cookie list with the specified same-site
     * attribute.
     */
    public CookieList(SameSiteAttribute sameSiteAttribute) {
        cookies = new LinkedHashMap<>();
        this.sameSiteAttribute = sameSiteAttribute;
    }

    /**
     * Creates a new empty cookie list.
     */
    public CookieList() {
        this(SameSiteAttribute.UNSET);
    }

    /**
     * Creates a new list with items copied from the existing collection.
     * 
     * @param existing the existing collection
     */
    public CookieList(Collection<HttpCookie> existing) {
        this();
        for (HttpCookie cookie : existing) {
            cookies.put(cookie.getName(), cookie);
        }
    }

    /**
     * Returns the same site attribute passed to the constructor.
     *
     * @return the same site attribute
     */
    public SameSiteAttribute sameSiteAttribute() {
        return sameSiteAttribute;
    }

    /**
     * Returns the value for the cookie with the given name.
     * 
     * @param name the name
     * @return the value if a cookie with the given name exists
     */
    public Optional<String> valueForName(String name) {
        return Optional.ofNullable(cookies.get(name))
            .map(HttpCookie::getValue);
    }

    /**
     * Adds a cookie to the list. If a cookie with the same name
     * already exists, it is replaced.
     * 
     * @param cookie the cookie
     * @return the cookie list for easy chaining
     */
    public CookieList add(HttpCookie cookie) {
        cookies.remove(cookie.getName());
        cookies.put(cookie.getName(), cookie);
        return this;
    }

    /**
     * Removes all cookies from the list.
     * 
     * @return the cookie list for easy chaining
     */
    public CookieList clear() {
        cookies.clear();
        return this;
    }

    public boolean isEmpty() {
        return cookies.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#iterator()
     */
    @Override
    public Iterator<HttpCookie> iterator() {
        return cookies.values().iterator();
    }

    public Stream<HttpCookie> stream() {
        return cookies.values().stream();
    }

    /**
     * Remove the cookie with the given name.
     * 
     * @param name
     * @return the cookie list for easy chaining
     */
    public boolean remove(String name) {
        return false;
    }

    public int size() {
        return cookies.size();
    }

}
