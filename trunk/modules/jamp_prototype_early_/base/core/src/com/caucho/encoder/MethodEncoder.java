package com.caucho.encoder;

import java.lang.reflect.Method;

/** Encodes a Java method into an AMP message. */
public interface MethodEncoder <BUFFER>{
    BUFFER encodeMethodForSend(Method method, Object[] method_params, String toURL, String fromURL) throws Exception;
}
