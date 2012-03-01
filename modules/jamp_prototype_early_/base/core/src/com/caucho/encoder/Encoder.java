package com.caucho.encoder;

/** Encode object into on-wire stream format, i.e., JSON, Hessian, BSON. */
public interface Encoder {
	Object encodeObject(Object obj) throws Exception; 
}
