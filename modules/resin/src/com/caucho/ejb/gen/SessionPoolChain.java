/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the bean instance for a method call.
 */
public class SessionPoolChain extends FilterCallChain {
  private static L10N L = new L10N(SessionPoolChain.class);

  private BaseMethod _apiMethod;
  private JMethod _implMethod;

  public SessionPoolChain(CallChain next, BaseMethod apiMethod)
  {
    super(next);

    _apiMethod = apiMethod;

    CallChain callChain = apiMethod.getCall();

    MethodCallChain methodCallChain = (MethodCallChain) callChain;

    _implMethod = methodCallChain.getMethod();
  }

  /**
   * Prints a call within the same JVM
   *
   * @param out the java source stream
   * @param var the object with the method
   * @param args the call's arguments
   */
  public void generateCall(JavaWriter out, String retVar,
                           String var, String []args)
    throws IOException
  {
    out.println("Bean ptr = _context._ejb_begin(trans);");

    out.println("try {");
    out.pushDepth();

    // ejb/0fba
    if (! _implMethod.isAnnotationPresent(javax.ejb.Remove.class))
      generateCallInterceptors(out, args);
    else // The interceptor calls ctx.proceed() which invokes the business method.
      super.generateCall(out, retVar, "ptr", args);

    // ejb/0fba
    if (! _implMethod.isAnnotationPresent(javax.ejb.Remove.class)) {
      out.popDepth();

      generateInterceptorExceptionHandling(out);
    }

    out.println("} catch (com.caucho.ejb.EJBExceptionWrapper e) {");
    out.pushDepth();

    // Application exception: cannot set null since the finally block
    // needs to free up the bean first.
    // out.println("ptr = null;");

    out.println("throw e;");

    out.popDepth();

    out.println("} catch (javax.ejb.EJBException e) {");
    out.pushDepth();

    // XXX: ejb/02d1 vs TCK
    out.println("ptr = null;");

    out.println("throw e;");

    out.popDepth();

    out.println("} catch (RuntimeException e) {");
    out.pushDepth();

    // XXX TCK, needs QA out.println("ptr = null;");

    out.println("throw e;");

    out.popDepth();

    out.println("} finally {");
    out.pushDepth();

    out.println("_context._ejb_free(ptr);");
    out.println("ptr = null;");

    // ejb/0fba
    if (_implMethod.isAnnotationPresent(javax.ejb.Remove.class)) {
      out.println("_server.removeRemote((com.caucho.ejb.protocol.AbstractHandle) getHandle());");
    }

    out.popDepth();
    out.println("}");

    // ejb/0fba
    if (! _implMethod.isAnnotationPresent(javax.ejb.Remove.class)) {
      // generateExceptionHandling(out);
    }
    else {
      out.println("} catch (RuntimeException e) {");
      out.pushDepth();
    }
  }

  protected void generateFilterCall(JavaWriter out, String retVar,
                                    String var, String []args)
    throws IOException
  {
    super.generateCall(out, retVar, "ptr", args);
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


    JClass types[] = _apiMethod.getParameterTypes();

    StringBuilder paramTypes = new StringBuilder();

    isFirst = true;

    for (int i = 0; i < args.length; i++) {
      String typeName = types[i].getPrintName();

      if (isFirst)
        isFirst = false;
      else
        paramTypes.append(", ");

      String s = "parameters[" + i + "]";

      if (! types[i].isPrimitive())
        paramTypes.append(typeName + ".class");
      else {
        Class primitiveType = (Class) types[i].getJavaClass();

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

    if (_apiMethod.getReturnType().getPrintName().equals("void"))
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

  private String generateTypeCasting(String s, JClass type)
  {
    if (type.isPrimitive()) {
      Class primitiveType = (Class) type.getJavaClass();

      if (primitiveType == Boolean.TYPE)
        return "((Boolean) " + s + ").booleanValue()";
      else if (primitiveType == Byte.TYPE)
        return "((Byte) " + s + ").byteValue()";
      else if (primitiveType == Character.TYPE)
        return "((Character) " + s + ").charValue()";
      else if (primitiveType == Double.TYPE)
        return "((Double) " + s + ").doubleValue()";
      else if (primitiveType == Float.TYPE)
        return "((Float) " + s + ").floatValue()";
      else if (primitiveType == Integer.TYPE)
        return "((Integer) " + s + ").intValue()";
      else if (primitiveType == Long.TYPE)
        return "((Long) " + s + ").longValue()";
      else if (primitiveType == Short.TYPE)
        return "((Short) " + s + ").shortValue()";
    }

    return "(" + type.getPrintName() + ") " + s;
  }
}
