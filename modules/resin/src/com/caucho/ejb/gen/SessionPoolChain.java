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

import com.caucho.bytecode.JClass;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the bean instance for a method call.
 */
public class SessionPoolChain extends FilterCallChain {
  private static L10N L = new L10N(SessionPoolChain.class);

  private BaseMethod _method;

  public SessionPoolChain(CallChain next, BaseMethod method)
  {
    super(next);

    _method = method;
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

    generateCallInterceptors(out, args);

    super.generateCall(out, retVar, "ptr", args);

    out.popDepth();

    generateInterceptorExceptionHandling(out);

    out.println("} catch (RuntimeException e) {");
    out.pushDepth();
    out.println("ptr = null;");

    out.println("throw e;");

    out.popDepth();

    out.println("} finally {");
    out.pushDepth();

    out.println("_context._ejb_free(ptr);");

    out.popDepth();
    out.println("}");
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
    if (_method == null)
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


    JClass types[] = _method.getParameterTypes();

    String paramTypes = "";
    String parametersCasting = "";

    isFirst = true;

    for (int i = 0; i < args.length; i++) {
      String typeName = types[i].getPrintName();

      if (isFirst)
        isFirst = false;
      else
        paramTypes += ", ";

      parametersCasting += args[i] + " = ";

      String s = "parameters[" + i + "]";

      if (! types[i].isPrimitive()) {
        paramTypes += typeName + ".class";
        parametersCasting += "(" + typeName + ") " + s + ";";
      }
      else {
        Class primitiveType = (Class) types[i].getJavaClass();

        if (primitiveType == Boolean.TYPE) {
          paramTypes += "Boolean.TYPE";
          parametersCasting += "((Boolean) " + s + ").booleanValue();";
        }
        else if (primitiveType == Byte.TYPE) {
          paramTypes += "Byte.TYPE";
          parametersCasting += "((Byte) " + s + ").byteValue();";
        }
        else if (primitiveType == Character.TYPE) {
          paramTypes += "Character.TYPE";
          parametersCasting += "((Character) " + s + ").charValue();";
        }
        else if (primitiveType == Double.TYPE) {
          paramTypes += "Double.TYPE";
          parametersCasting += "((Double) " + s + ").doubleValue();";
        }
        else if (primitiveType == Float.TYPE) {
          paramTypes += "Float.TYPE";
          parametersCasting += "((Float) " + s + ").floatValue();";
        }
        else if (primitiveType == Integer.TYPE) {
          paramTypes += "Integer.TYPE";
          parametersCasting += "((Integer) " + s + ").intValue();";
        }
        else if (primitiveType == Long.TYPE) {
          paramTypes += "Long.TYPE";
          parametersCasting += "((Long) " + s + ").longValue();";
        }
        else if (primitiveType == Short.TYPE) {
          paramTypes += "Short.TYPE";
          parametersCasting += "((Short) " + s + ").shortValue();";
        }
      }
    }

    out.println("javax.interceptor.InvocationContext invocationContext;");
    out.println();

    out.print("invocationContext = ptr.__caucho_callInterceptors(this, ");
    out.print("new Object[] {" + argList + "}, ");
    out.println("\"" + _method.getMethodName() + "\", new Class[] {" + paramTypes + "});");
    out.println();

    out.println("Object parameters[] = invocationContext.getParameters();");
    out.println();

    if (! "".equals(parametersCasting)) {
      out.println(parametersCasting);
      out.println();
    }
  }

  protected void generateInterceptorExceptionHandling(JavaWriter out)
    throws IOException
  {
    out.popDepth();
    out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
    out.pushDepth();

    for (JClass cl : _method.getExceptionTypes()) {
      out.println("if (e.getCause() instanceof " + cl.getName() + ")");
      out.println("  throw (" + cl.getName() + ") e.getCause();");
    }

    out.println("throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
  }
}
