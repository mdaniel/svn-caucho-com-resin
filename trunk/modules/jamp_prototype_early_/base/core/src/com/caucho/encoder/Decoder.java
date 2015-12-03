package com.caucho.encoder;

/** Decode object from on-wire stream format to Java object. */
public interface Decoder <TO, FROM>{
	TO decodeObject(FROM value) throws Exception; 
}
