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

package org.jdrupes.httpcodec.util;

import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Splits a list of items. Delimiters are escaped if they are in
 * double quotes.
 */
public class ListItemizer implements Iterator<String> {

	private String unparsedValue;
	private int position = 0;
	private String delimiters;
	private String pendingItem = null;
	private ParseException pendingException = null;
	
	/**
	 * Generates a new itemizer.
	 * 
	 * @param list the list to be itemized
	 * @param delimiters the set of delimiter characters
	 */
	public ListItemizer(String list, String delimiters) {
		unparsedValue = list;
		this.delimiters = delimiters;
		while (true) { // Skip optional white space
			char ch = unparsedValue.charAt(position);
			if (ch != ' ' && ch != '\t') {
				break;
			}
			position += 1;
		}
		try {
			pendingItem = nextItem();
		} catch (ParseException e) {
			pendingException = e;
		}
	}

	/**
	 * Returns the next item from the unparsed value.
	 * 
	 * @return the next item or {@code null} if no items remain
	 * @throws ParseException if the input violates the field format
	 */
	private String nextItem() throws ParseException {
		// RFC 7230 3.2.6
		boolean inDquote = false;
		int startPosition = position;
		try {
			while (true) {
				if (inDquote) {
					char ch = unparsedValue.charAt(position);
					switch (ch) {
					 case '\\':
						 position += 2;
						 continue;
					 case '\"':
						 inDquote = false;
						 // fall through
					 default:
						 position += 1;
						 continue;
					}
				}
				if (position == unparsedValue.length()) {
					if (position == startPosition) {
						return null;
					}
					return unparsedValue.substring(startPosition, position);
				}
				char ch = unparsedValue.charAt(position);
				if (delimiters.indexOf(ch) >= 0) {
					String result = unparsedValue
					        .substring(startPosition, position).trim();
					position += 1; // Skip delimiter
					while (true) { // Skip optional white space
						ch = unparsedValue.charAt(position);
						if (ch != ' ' && ch != '\t') {
							break;
						}
						position += 1;
					}
					return result;
				}
				switch (ch) {
				case '\"':
					inDquote = true;
					// fall through
				default:
					position += 1;
					continue;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(unparsedValue, position);
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return pendingItem != null;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public String next() {
		if (pendingException != null) {
			NoSuchElementException exc = new NoSuchElementException();
			exc.initCause(pendingException);
			throw exc;
		}
		if (pendingItem == null) {
			throw new NoSuchElementException("No elements left.");
		}
		String result = pendingItem;
		pendingItem = null;
		try {
			pendingItem = nextItem();
		} catch (ParseException e) {
			pendingException = e;
			NoSuchElementException exc = new NoSuchElementException();
			exc.initCause(pendingException);
			throw exc;
		}
		return result;
	}
	
}
