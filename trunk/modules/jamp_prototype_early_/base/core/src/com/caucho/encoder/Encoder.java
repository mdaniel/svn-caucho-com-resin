package com.caucho.encoder;

/** Encode object into on-wire stream format, i.e., JSON, Hessian, BSON. 
 * This is typically converting a Java object to a binary or text buffer.
 * */
public interface Encoder <BUFFER, FROM> {
	BUFFER encodeObject(FROM obj) throws Exception; 
}
