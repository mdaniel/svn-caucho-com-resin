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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;

/**
 * Represents the interception
 */
public class InterceptorCallChain implements EjbCallChain {
  private static final L10N L = new L10N(InterceptorCallChain.class);

  private BusinessMethodGenerator _next;

  private String _uniqueName;
  private Method _implMethod;
  private ArrayList<Class> _interceptors = new ArrayList<Class>();

  public InterceptorCallChain(BusinessMethodGenerator next)
  {
    _next = next;
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean isEnhanced()
  {
    return (_interceptors != null && _interceptors.size() > 0);
  }

  public ArrayList<Class> getInterceptors()
  {
    return _interceptors;
  }

  /**
   * Introspects the @Interceptors annotation on the method
   * and the class.
   */
  public void introspect(Method apiMethod, Method implMethod)
  {
    ArrayList<Class> interceptorList = new ArrayList<Class>();
    
    Interceptors interceptors
      = apiMethod.getAnnotation(Interceptors.class);

    if (interceptors != null) {
      for (Class interceptorClass : interceptors.value())
	interceptorList.add(interceptorClass);
    }
    
    if (interceptors == null) {
      Class apiClass = apiMethod.getDeclaringClass();
      
      interceptors
	= (Interceptors) apiClass.getAnnotation(Interceptors.class);

      if (interceptors != null) {
	for (Class interceptorClass : interceptors.value())
	  interceptorList.add(interceptorClass);
      }
    }
    
    if (interceptors == null) {
      interceptors = implMethod.getAnnotation(Interceptors.class);

      if (interceptors != null) {
	for (Class interceptorClass : interceptors.value())
	  interceptorList.add(interceptorClass);
      }
    }
    
    if (interceptors == null) {
      Class implClass = implMethod.getDeclaringClass();

      interceptors
	= (Interceptors) implClass.getAnnotation(Interceptors.class);

      if (interceptors != null) {
	for (Class interceptorClass : interceptors.value())
	  interceptorList.add(interceptorClass);
      }
    }

    _implMethod = implMethod;
    _interceptors = interceptorList;
  }

  @Override
  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    if (_interceptors.size() == 0) {
      _next.generatePrologue(out, map);
      return;
    }
    
    _uniqueName = "_v" + out.generateId();
    
    out.println();
    out.println("private static java.lang.reflect.Method " + _uniqueName + "_method;");
    out.println("private static java.lang.reflect.Method []" + _uniqueName + "_methodChain;");
    out.println("private static Object []" + _uniqueName + "_objectChain;");

    Class cl = _implMethod.getDeclaringClass();
    
    out.println();
    out.println("static {");
    out.pushDepth();
    
    out.println("try {");
    out.pushDepth();
    
    out.print(_uniqueName + "_method = ");
    generateGetMethod(out, _implMethod);
    out.println(";");
    out.print(_uniqueName + "_method.setAccessible(true);");

    generateMethodChain(out);
    
    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    out.popDepth();
    out.println("}");
    
    _next.generatePrologue(out, map);
  }

  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    if (_interceptors.size() == 0) {
      _next.generateCall(out);
      return;
    }

    out.println("try {");
    out.pushDepth();


    out.println("if (" + _uniqueName + "_objectChain == null) {");
    out.pushDepth();
    generateObjectChain(out);
    out.popDepth();
    out.println("}");

    if (! void.class.equals(_implMethod.getReturnType())) {
      out.printClass(_implMethod.getReturnType());
      out.println(" result;");
    }
      
    _next.generatePreCall(out);
    
    if (! void.class.equals(_implMethod.getReturnType())) {
      out.print("result = (");
      printCastClass(out, _implMethod.getReturnType());
      out.print(") ");
    }
    
    out.print("new com.caucho.ejb3.gen.InvocationContextImpl(");
    _next.generateThis(out);
    out.print(", ");
    out.print(_uniqueName + "_method, ");
    out.print(_uniqueName + "_methodChain, ");
    out.print(_uniqueName + "_objectChain, ");
    out.print("new Object[] { ");
    for (int i = 0; i < _implMethod.getParameterTypes().length; i++) {
      out.print("a" + i + ", ");
    }
    out.println("}).proceed();");
    
    _next.generatePostCall(out);
    
    if (! void.class.equals(_implMethod.getReturnType())) {
      out.println("return result;");
    }
    
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
  }

  protected Method findInterceptorMethod(Class cl)
  {
    if (cl == null)
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class))
	return method;
    }

    return findInterceptorMethod(cl.getSuperclass());
  }

  protected void generateMethodChain(JavaWriter out)
    throws IOException
  {
    out.println(_uniqueName + "_methodChain = new java.lang.reflect.Method[] {");
    out.pushDepth();

    for (Class iClass : _interceptors) {
      Method method = findInterceptorMethod(iClass);

      if (method == null)
	throw new IllegalStateException(L.l("Can't find @AroundInvoke in '{0}'",
					    iClass.getName()));
      
      generateGetMethod(out, method);
      out.println(", ");
    }
    out.popDepth();
    out.println("};");
  }

  protected void generateObjectChain(JavaWriter out)
    throws IOException
  {
    out.println(_uniqueName + "_objectChain = new Object[] {");
    out.pushDepth();

    for (Class iClass : _interceptors) {
      out.println("new " + iClass.getName() + "(),");
    }
    
    out.popDepth();
    out.println("};");
  }
  
  protected void generateGetMethod(JavaWriter out, Method method)
    throws IOException
  {
    Class cl = method.getDeclaringClass();

    out.print("com.caucho.ejb.util.EjbUtil.getMethod(");
    out.print(cl.getName() + ".class");
    out.print(", \"" + method.getName() + "\", new Class[] { ");
    
    for (Class type : method.getParameterTypes()) {
      out.printClass(type);
      out.print(".class, ");
    }
    out.print("})");
  }

  protected void printCastClass(JavaWriter out, Class type)
    throws IOException
  {
    if (! type.isPrimitive())
      out.printClass(type);
    else if (boolean.class.equals(type))
      out.print("Boolean");
    else if (char.class.equals(type))
      out.print("Character");
    else if (byte.class.equals(type))
      out.print("Byte");
    else if (short.class.equals(type))
      out.print("Short");
    else if (int.class.equals(type))
      out.print("Integer");
    else if (long.class.equals(type))
      out.print("Long");
    else if (float.class.equals(type))
      out.print("Float");
    else if (double.class.equals(type))
      out.print("Double");
    else
      throw new IllegalStateException(type.getName());
  }
}
