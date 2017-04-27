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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * Represents a list of Cache-Control directives.
 * 
 * @see "[RFC 7234, 5.2](https://tools.ietf.org/html/rfc7234#section-5.2)"
 */
public class CacheControlDirectives implements Iterable<Directive> {

	private LinkedHashMap<String, Directive> directives;
	
	/**
	 * Creates a new empty cookie list.
	 */
	public CacheControlDirectives() {
		directives = new LinkedHashMap<>();
	}

	/**
	 * Creates a new list with items copied from the existing collection.
	 * 
	 * @param existing the existing collection
	 */
	public CacheControlDirectives(Collection<Directive> existing) {
		directives = new LinkedHashMap<>();
		for (Directive directive: existing) {
			directives.put(directive.name(), directive);
		}
	}

	/**
	 * Returns the value for the directive with the given name.
	 * 
	 * @param name the name
	 * @return the value if a directive with the given name exists
	 */
	public Optional<String> valueForName(String name) {
		return Optional.ofNullable(directives.get(name.toLowerCase()))
				.flatMap(Directive::value);
	}

	/**
	 * Adds a directive to the list. If a directive with the same name
	 * already exists, it is replaced (except for `no-cache`).
	 * 
	 * The `no-cache` directive is handled specially. If no such directive
	 * exists, it is added. Else, if the new directive has no
	 * arguments, it replaces the existing `no-cache` directive. If both
	 * the existing directive and the new directive specify fields,
	 * the fields are merged.
	 * 
	 * @param directive the directive
	 * @return the directives for easy chaining
	 */
	public CacheControlDirectives add(Directive directive) {
		if ("no-cache".equals(directive.name())) {
			Directive existing = directives.get(directive.name());
			if (existing != null) {
				if (!existing.value().isPresent()) {
					return this;
				}
				if (directive.value().isPresent()) {
					Set<String> values = new HashSet<>();
					new ListItemizer(existing.value().get(), ",")
						.forEachRemaining(values::add);
					new ListItemizer(directive.value().get(), ",")
						.forEachRemaining(values::add);
					directives.put(directive.name(), 
							new Directive(directive.name(),	values.stream()
									.collect(Collectors.joining(", "))));
					return this;
				}
			}
		}
		directives.remove(directive.name());
		directives.put(directive.name(), directive);
		return this;
	}

	/**
	 * Removes all directives from the list.
	 * 
	 * @return the directives for easy chaining
	 */
	public CacheControlDirectives clear() {
		directives.clear();
		return this;
	}

	public boolean isEmpty() {
		return directives.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.List#iterator()
	 */
	@Override
	public Iterator<Directive> iterator() {
		return directives.values().iterator();
	}

	public Stream<Directive> stream() {
		return directives.values().stream();
	}
	
	/**
	 * Remove the directive with the given name.
	 * 
	 * @param name
	 * @return the directives for easy chaining
	 */
	public boolean remove(String name) {
		return false;
	}

	public int size() {
		return directives.size();
	}

}
