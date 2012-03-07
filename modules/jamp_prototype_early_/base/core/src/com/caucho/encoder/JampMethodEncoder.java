package com.caucho.encoder;

import java.lang.reflect.Method;


/** Convert method into JAMP method. */
public class JampMethodEncoder implements MethodEncoder<String>{
	
    /* If we support Jackson we have to make this pluggable. */
	private Encoder<String, Object> encoder = new JSONEncoder();

	@Override
	public String encodeMethodForSend(final Method method, final Object[] method_params, final String toURL, final String fromURL) throws Exception {
		StringBuilder builder = new StringBuilder(255);
		
		builder.append(String.format("[\"send\",\"%s\",\"%s\",\"%s\"",toURL, fromURL, method.getName()));
		
		
		final int length = method_params.length;
		if (length!=0) {
			builder.append(",[");
		}
		
		for (int index=0; index < length; index++) {
			Object param = method_params[index];
			builder.append((String)encoder.encodeObject(param));
			if (index+1!=length) {
				builder.append(',');
			} else {
				builder.append(']');
			}
		}
		builder.append(']');
		return builder.toString();
	}

}
