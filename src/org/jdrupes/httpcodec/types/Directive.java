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
import java.util.Optional;

/**
 * Represents a directive.
 */
public class Directive {

	private String name;
	private Optional<String> value;
	
	/**
	 * Creates a new directive.
	 * 
	 * @param name the name
	 * @param value the value
	 */
	public Directive(String name, String value) {
		super();
		this.name = name;
		this.value = Optional.of(value);
	}
	
	/**
	 * Creates a new directive.
	 * 
	 * @param name the name
	 */
	public Directive(String name) {
		super();
		this.name = name;
		this.value = Optional.empty();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the value
	 */
	public Optional<String> getValue() {
		return value;
	}
	
	public static class DirectiveConverter implements Converter<Directive> {

		@Override
		public String asFieldValue(Directive value) {
			if (!value.getValue().isPresent()) {
				return value.name;
			}
			return value.name + "=" 
				+ Converters.quoteIfNecessary(value.getValue().get());
		}

		@Override
		public Directive fromFieldValue(String text) throws ParseException {
			int pos = text.indexOf('=');
			if (pos < 0) {
				return new Directive(text);
			}
			return new Directive(text.substring(0, pos),
					Converters.unquoteString(text.substring(pos + 1)));
		}
		
	}
}
