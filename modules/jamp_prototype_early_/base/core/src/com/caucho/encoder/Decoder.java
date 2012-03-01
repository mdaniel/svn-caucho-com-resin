package com.caucho.encoder;

/** Decode object from on-wire stream format to Java object. */
public interface Decoder {
	Object decodeObject(Object obj) throws Exception; 
}
