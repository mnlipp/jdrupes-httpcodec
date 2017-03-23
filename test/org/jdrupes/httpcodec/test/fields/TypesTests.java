package org.jdrupes.httpcodec.test.fields;

import java.text.ParseException;

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;

import static org.junit.Assert.*;
import org.junit.Test;

public class TypesTests {

	@Test
	public void testMediaTypeCreation() throws ParseException {
		MediaType media = MediaType.builder().setType("text", "html")
				.setParameter("charset", "utf-8").build();
		assertEquals("text", media.getTopLevelType());
		assertEquals("html", media.getSubtype());
		assertEquals("utf-8", media.getParameter("charset"));
		// from string
		media = Converters.MEDIA_TYPE.fromFieldValue("text/html; charset=utf-8");
		assertEquals("text", media.getTopLevelType());
		assertEquals("html", media.getSubtype());
		assertEquals("utf-8", media.getParameter("charset"));
	}

}
