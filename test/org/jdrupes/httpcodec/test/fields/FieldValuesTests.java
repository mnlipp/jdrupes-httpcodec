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

package org.jdrupes.httpcodec.test.fields;

import java.text.ParseException;
import java.util.Iterator;

import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.types.CommentedValue;
import org.jdrupes.httpcodec.types.CommentedValue.CommentedValueConverter;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.StringList;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 */
public class FieldValuesTests {

	@Test
	public void testStringList() throws ParseException {
		HttpField<StringList> fv = new HttpField<>(
				"Test: How, are,you,  \"out there\"", Converters.STRING_LIST);
		Iterator<String> iter = fv.value().iterator();
		assertEquals("How", iter.next());
		assertEquals("are", iter.next());
		assertEquals("you", iter.next());
		assertEquals("out there", iter.next());
		assertFalse(iter.hasNext());
		assertEquals("How, are, you, \"out there\"", fv.asFieldValue());
	}
	
	@Test
	public void testCommented() {
		CommentedValue<String> value = new CommentedValue<String>(
				"Hello", "World(!)");
		assertEquals("Hello (World\\(!\\))", 
				(new CommentedValueConverter<>(Converters.STRING))
				.asFieldValue(value));
	}
}
