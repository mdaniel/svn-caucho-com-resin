/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.aop;

import java.io.IOException;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.BaseMethod;

/**
 * Basic method generation.
 */
public class AopMethod extends BaseMethod {
  private static final L10N L = new L10N(AopMethod.class);

  private String _fieldName;
  
  /**
   * Creates the base method
   */
  public AopMethod(JMethod apiMethod, JMethod implMethod)
  {
    super(apiMethod, implMethod);
  }
  
  /**
   * Generates the code for the class.
   *
   * @param out the writer to the output stream.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    _fieldName = "__caucho_aop_" + out.generateId();

    generateInterceptors(out);
    
    super.generate(out);
  }

  private void generateInterceptors(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private static class " + _fieldName + " extends com.caucho.aop.MethodInvocationImpl {");
    out.pushDepth();
    out.print("private static java.lang.reflect.Method _method = getMethod(");
    out.print(getMethod().getDeclaringClass().getName() + ".class");

    out.print(", \"" + getMethodName() + "\", new Class[] {");
    JClass []types = getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");
      out.print(types[i].getPrintName());
      out.print(".class");
    }
    out.println("});");
    out.print("private static java.lang.reflect.Method _superMethod = getMethod(");
    out.print(getMethod().getDeclaringClass().getName() + ".class");

    out.print(", \"" + getMethodName() + "__super\", new Class[] {");
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");
      out.print(types[i].getPrintName());
      out.print(".class");
    }
    out.println("});");
    out.println();
    out.println("static final org.aopalliance.intercept.MethodInterceptor _interceptor = com.caucho.aop.MethodInterceptorBuilder.create(_method);");
    
    out.println();
    out.print(_fieldName + "(");

    out.print("Object t");
    
    JClass []parameterTypes = getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      out.print(", ");
      
      out.print(parameterTypes[i].getPrintName());
      
      out.print(" a" + i);
    }
    
    out.println(")");
    out.println("{");
    out.pushDepth();

    out.print("super(t, new Object[] { ");
    
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i != 0)
	out.print(", ");
      
      JClass type = parameterTypes[i];

      if ("boolean".equals(type.getName()))
	out.print("new Boolean(a" + i + ")");
      else if ("byte".equals(type.getName()))
	out.print("new Byte(a" + i + ")");
      else if ("short".equals(type.getName()))
	out.print("new Short(a" + i + ")");
      else if ("int".equals(type.getName()))
	out.print("new Integer(a" + i + ")");
      else if ("long".equals(type.getName()))
	out.print("new Long(a" + i + ")");
      else if ("float".equals(type.getName()))
	out.print("new Float(a" + i + ")");
      else if ("double".equals(type.getName()))
	out.print("new Double(a" + i + ")");
      else if ("char".equals(type.getName()))
	out.print("new Character(a" + i + ")");
      else
	out.print("a" + i);
    }
    out.println("});");
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public java.lang.reflect.Method getMethod() { return _method; }");
    out.println("protected java.lang.reflect.Method getSuperMethod() { return _superMethod; }");
    
    out.popDepth();
    out.println("}");
  }
  
  /**
   * Generates the code for the call.
   *
   * @param out the writer to the output stream.
   * @param args the arguments
   */
  protected void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    super.generateCall(out, args);
  }
}
