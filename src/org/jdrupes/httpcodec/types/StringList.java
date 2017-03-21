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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Represents a list of strings with some additional methods.
 */
@SuppressWarnings("serial")
public class StringList extends ArrayList<String> {

	/**
	 * Creates a new empty list of strings.
	 */
	public StringList() {
	}

	/**
	 * Creates a new empty list of strings with an initial capacity.
	 * 
	 * @param initialCapacity the capacity
	 */
	public StringList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new list with items copied from the existing collection.
	 * 
	 * @param existing the existing collection
	 */
	public StringList(Collection<String> existing) {
		super(existing);
	}

	/**
	 * Creates a new list with given items.
	 * 
	 * @param item first item
	 * @param items more items
	 */
	public StringList(String item, String ... items) {
		super();
		add(item);
		addAll(Arrays.asList(items));
	}

	/**
	 * Returns whether the list contains the given value, ignoring
	 * differences in the cases of the letters.
	 * 
	 * @param value the value to compare with
	 * @return the result
	 */
	public boolean containsIgnoreCase(String value) {
		for (String s: this) {
			if (s.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes all strings equal to the given value, ignoring
	 * differences in the cases of the letters.
	 * 
	 * @param value the value to compare with
	 */
	public void removeIgnoreCase(String value) {
		removeIf(s -> s.equalsIgnoreCase(value));
	}
	
	/**
	 * Appends the value to the list of values if it is not already in the list.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public StringList appendIfNotContained(String value) {
		if (!contains(value)) {
			add(value);
		}
		return this;
	}
	
}
