/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.hessian.io;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Serializing an object for known object types.
 */
public class JavaSerializer extends AbstractSerializer {
  private Field []_fields;
  private Method _writeReplace;
  
  public JavaSerializer(Class cl)
  {
    _writeReplace = getWriteReplace(cl);
    if (_writeReplace != null)
      _writeReplace.setAccessible(true);

    ArrayList primitiveFields = new ArrayList();
    ArrayList compoundFields = new ArrayList();
    
    for (; cl != null; cl = cl.getSuperclass()) {
      Field []fields = cl.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
	Field field = fields[i];

	if (Modifier.isTransient(field.getModifiers()) ||
	    Modifier.isStatic(field.getModifiers()))
	  continue;

	// XXX: could parameterize the handler to only deal with public
	field.setAccessible(true);

	if (field.getType().isPrimitive() ||
	    field.getType().getName().startsWith("java.lang.") &&
	    ! field.getType().equals(Object.class))
	  primitiveFields.add(field);
	else
	  compoundFields.add(field);
      }
    }

    ArrayList fields = new ArrayList();
    fields.addAll(primitiveFields);
    fields.addAll(compoundFields);

    _fields = new Field[fields.size()];
    fields.toArray(_fields);
  }

  /**
   * Returns the writeReplace method
   */
  protected Method getWriteReplace(Class cl)
  {
    for (; cl != null; cl = cl.getSuperclass()) {
      Method []methods = cl.getDeclaredMethods();
      
      for (int i = 0; i < methods.length; i++) {
	Method method = methods[i];

	if (method.getName().equals("writeReplace") &&
	    method.getParameterTypes().length == 0)
	  return method;
      }
    }

    return null;
  }
  
  public void writeObject(Object obj, AbstractHessianOutput out)
    throws IOException
  {
    if (out.addRef(obj))
      return;
    
    Class cl = obj.getClass();
    
    try {
      if (_writeReplace != null) {
	Object repl = _writeReplace.invoke(obj, new Object[0]);

	out.removeRef(obj);

	out.writeObject(repl);

	out.replaceRef(repl, obj);

	return;
      }
    } catch (Exception e) {
    }

    try {
      out.writeMapBegin(cl.getName());

      for (int i = 0; i < _fields.length; i++) {
	Field field = _fields[i];
      
	out.writeString(field.getName());
	out.writeObject(field.get(obj));
      }
      out.writeMapEnd();
    } catch (IllegalAccessException e) {
      throw new IOException(String.valueOf(e));
    }
  }
}
