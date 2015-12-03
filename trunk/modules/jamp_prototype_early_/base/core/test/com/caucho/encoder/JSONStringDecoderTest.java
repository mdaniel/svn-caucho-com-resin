package com.caucho.encoder;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class JSONStringDecoderTest {
	
	@Test
	public void test() throws Exception{
		JSONStringDecoder decode = new JSONStringDecoder();
		String decodedString = (String) decode.decodeObject("\\\"Hello how are you?\\\"\\nGood and you?\\u20ac\\/\\\\");
		String testString = "\"Hello how are you?\"\nGood and you?Û/\\";
		assertTrue(decodedString.equals(testString));
	}

}
