/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.util.L10N;

import javax.ejb.Remove;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.*;

/**
 * Generates the bean instance for a method call.
 */
abstract public class SessionPoolChain extends FilterCallChain {
  private static L10N L = new L10N(SessionPoolChain.class);

  protected BaseMethod _apiMethod;
  protected boolean _isRemote;
  
  protected SessionPoolChain(CallChain next,
			     BaseMethod apiMethod,
			     boolean isRemote)
  {
    super(next);

    _apiMethod = apiMethod;
    _isRemote = isRemote;
  }

  protected void generateFilterCall(JavaWriter out, String retVar,
                                    String var, String []args)
    throws IOException
  {
    super.generateCall(out, retVar, "ptr", args);
  }

  protected void generateMethodCall(JavaWriter out, String []args)
    throws IOException
  {
  }

  protected boolean isRemote()
  {
    return _isRemote;
  }

  protected void generateCallInterceptors(JavaWriter out, String []args)
    throws IOException
  {
    if (_apiMethod == null)
      return;

    String argList = "";

    boolean isFirst = true;

    for (String arg : args) {
      if (isFirst)
        isFirst = false;
      else
        argList += ", ";

      argList += arg;
    }

    Class types[] = _apiMethod.getParameterTypes();

    StringBuilder paramTypes = new StringBuilder();

    isFirst = true;

    for (int i = 0; i < args.length; i++) {
      String typeName = javaClassName(types[i]);

      if (isFirst)
        isFirst = false;
      else
        paramTypes.append(", ");

      String s = "parameters[" + i + "]";

      if (! types[i].isPrimitive())
        paramTypes.append(typeName + ".class");
      else {
        Class primitiveType = types[i];

        if (primitiveType == Boolean.TYPE)
          paramTypes.append("Boolean.TYPE");
        else if (primitiveType == Byte.TYPE)
          paramTypes.append("Byte.TYPE");
        else if (primitiveType == Character.TYPE)
          paramTypes.append("Character.TYPE");
        else if (primitiveType == Double.TYPE)
          paramTypes.append("Double.TYPE");
        else if (primitiveType == Float.TYPE)
          paramTypes.append("Float.TYPE");
        else if (primitiveType == Integer.TYPE)
          paramTypes.append("Integer.TYPE");
        else if (primitiveType == Long.TYPE)
          paramTypes.append("Long.TYPE");
        else if (primitiveType == Short.TYPE)
          paramTypes.append("Short.TYPE");
      }
    }

    String s = "ptr.__caucho_callInterceptors(this, new Object[] {" + argList + "}, ";
    s += "\"" + _apiMethod.getMethodName() + "\", new Class[] {" + paramTypes + "})";

    if (void.class.equals(_apiMethod.getReturnType()))
      out.print(s);
    else {
      out.print("return " + generateTypeCasting(s, _apiMethod.getReturnType()));
    }

    out.println(";");
  }

  protected void generateInterceptorExceptionHandling(JavaWriter out)
    throws IOException
  {
    out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
    out.pushDepth();
    out.println("throw com.caucho.ejb.EJBExceptionWrapper.create(e.getCause());");
    out.popDepth();
  }

  protected String generateTypeCasting(String s, Class type)
  {
    if (type.isPrimitive()) {
      if (type == Boolean.TYPE)
        return "((Boolean) " + s + ").booleanValue()";
      else if (type == Byte.TYPE)
        return "((Byte) " + s + ").byteValue()";
      else if (type == Character.TYPE)
        return "((Character) " + s + ").charValue()";
      else if (type == Double.TYPE)
        return "((Double) " + s + ").doubleValue()";
      else if (type == Float.TYPE)
        return "((Float) " + s + ").floatValue()";
      else if (type == Integer.TYPE)
        return "((Integer) " + s + ").intValue()";
      else if (type == Long.TYPE)
        return "((Long) " + s + ").longValue()";
      else if (type == Short.TYPE)
        return "((Short) " + s + ").shortValue()";
    }

    // XXX: arrays
    return "(" + javaClassName(type) + ") " + s;
  }

  protected String javaClassName(Class cl)
  {
    if (cl.isArray())
      return javaClassName(cl.getComponentType()) + "[]";
    else
      return cl.getName();
  }
}
