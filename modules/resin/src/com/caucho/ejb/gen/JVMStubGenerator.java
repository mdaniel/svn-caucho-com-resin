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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import java.io.*;
import java.util.*;
import java.beans.*;
import java.lang.reflect.*;
import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;

/**
 * Generator for stubs.
 */
abstract class JVMStubGenerator extends AbstractGenerator {
  protected static final L10N L = new L10N(JVMStubGenerator.class);
  
  // Classes which can be safely passed by reference because they're
  // immutable and part of the JDK
  static IntMap _immutableClasses;

  protected boolean _isProxy = false;
  protected ArrayList<Method> _proxyMethods = new ArrayList<Method>();
  protected Class _remoteClass;

  void setProxy(boolean isProxy)
  {
    _isProxy = isProxy;
  }

  /**
   * Prints the header for a HomeStub
   */
  abstract protected void printHeader()
    throws IOException;

  /**
   * Prints a direct call for use in the same JVM.
   *
   * @param method the method to generate
   */
  protected void printMethod(String name, Method method)
    throws IOException
  {
    Class ret = method.getReturnType();
    Class []params = method.getParameterTypes();
    
    printMethodHeader(name, method);
    
    boolean needsSerialization = printMethodHead(params, ret);

    printCall(method.getName(), params, ret);
    
    printMethodFooter(ret, needsSerialization);
  }

  protected boolean printMethodHead(Class []params, Class ret)
    throws IOException
  {
    println("{");
    pushDepth();
    println("Thread thread = Thread.currentThread();");
    println("ClassLoader oldLoader = thread.getContextClassLoader();");

    boolean needsSerialization = false;
    for (int i = 0; i < params.length; i++) {
      if (needsSerialization(params[i])) {
        if (! needsSerialization) {
          println("com.caucho.ejb.protocol.SelfSerializer ser;");
          println("ser = com.caucho.ejb.protocol.SelfSerializer.allocate();");
        }
        println("ser.write(a" + i + ");");
        needsSerialization = true;
      }
    }

    if (needsSerialization(ret) && ! needsSerialization) {
      println("com.caucho.ejb.protocol.SelfSerializer ser;");
      println("ser = com.caucho.ejb.protocol.SelfSerializer.allocate();");
      needsSerialization = true;
    }

    if (! ret.getName().equals("void")) {
      printClass(ret);
      println(" _ret;");
    }

    println();
    if (! _isProxy) {
      println(_remoteClass.getName() + " obj;");
      println("obj = ("  + _remoteClass.getName() + ") _caucho_getObject();");
    }
    else {
      println("Object obj = _caucho_getObject();");
    }
    println("thread.setContextClassLoader(_caucho_getClassLoader());");
    println("try {");
    pushDepth();

    return needsSerialization;
  }

  protected void printMethodFooter(Class ret, boolean needsSerialization)
    throws IOException
  {
    if (needsSerialization(ret)) {
      if (needsSerialization)
        println("ser.clear();");
      needsSerialization = true;
      println("ser.write(_ret);");
      println("thread.setContextClassLoader(oldLoader);");
      print("_ret = (");
      printClass(ret);
      println(") ser.read();");
    }

    popDepth();
    println("} finally {");
    pushDepth();
    println("thread.setContextClassLoader(oldLoader);");
    
    if (needsSerialization)
      println("ser.close();");

    popDepth();
    println("}");

    if (! ret.getName().equals("void"))
      println("return _ret;");
    
    popDepth();
    println("}");
  }

  void printCall(String name, Class []params, Class retType)
    throws IOException
  {
    if (_isProxy) {
      printProxyCall(name, params, retType);
      return;
    }

    if (! retType.equals(void.class))
      print("_ret = ");
    
    print("obj." + name + "(");
    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        print(", ");
      if (needsSerialization(params[i])) {
        print("(");
        printClass(params[i]);
        print(") ser.read()");
      }
      else
        print("a" + i);
    }
    println(");");
  }

  void printProxyCall(String name, Class []params, Class retType)
    throws IOException
  {
    Method method;

    try {
      method = _remoteClass.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      throw new IOExceptionWrapper(e);
    }
    _proxyMethods.add(method);

    if (_isProxy) {
      println("try {");
      pushDepth();
    }
    
    int index = _proxyMethods.size() - 1;

    boolean retNeedsSerialization = needsSerialization(retType);
    
    if (retType.equals(void.class)) {
    }
    else if (! retNeedsSerialization) {
      print("_ret = (");
      printClass(retType);
      print(")");
    }
    else {
      print("Object _oret = ");
    }

    if (retType.isPrimitive())
      print("com.caucho.ejb.JVMObject.to_" + retType.getName() + "(");

    print("method" + index + ".invoke(obj, new Object[] {");
    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        print(", ");
      if (needsSerialization(params[i])) {
        print("(");
        printClass(params[i]);
        print(") ser.read()");
      }
      else if (params[i].isPrimitive())
        printToObject("a" + i, params[i]);
      else
        print("a" + i);
    }
    if (retType.isPrimitive())
      println("}));");
    else
      println("});");
    
    if (retNeedsSerialization) {
      // println("System.out.println(_oret);");
      println("ser.clear();");
      println("ser.write(_oret);");
      println("thread.setContextClassLoader(oldLoader);");
      print("_ret = (");
      printClass(method.getReturnType());
      println(") ser.read();");
    }

    popDepth();
    println("} catch (java.lang.IllegalAccessException e) {");
    println("  throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e);");
    println("} catch (java.lang.reflect.InvocationTargetException e) {");
    println("  throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e);");
    println("}");
  }

  void printToObject(String name, Class cl)
    throws IOException
  {
    if (boolean.class.equals(cl))
      print("new Boolean(" + name + ")");
    else if (byte.class.equals(cl))
      print("new Byte(" + name + ")");
    else if (short.class.equals(cl))
      print("new Short(" + name + ")");
    else if (char.class.equals(cl))
      print("new Character(" + name + ")");
    else if (int.class.equals(cl))
      print("new Integer(" + name + ")");
    else if (long.class.equals(cl))
      print("new Long(" + name + ")");
    else if (float.class.equals(cl))
      print("new Float(" + name + ")");
    else if (double.class.equals(cl))
      print("new Double(" + name + ")");
    else
      throw new RuntimeException(L.l("can't create object for type {0}",
                                     cl.getName()));
  }

  /**
   * Prints the class footer for the generated stub.
   */
  void printFooter()
    throws IOException
  {
    for (int i = 0; i < _proxyMethods.size(); i++) {
      println("static java.lang.reflect.Method method" + i + ";");
    }
    if (_proxyMethods.size() > 0) {
      println("protected void _caucho_init_methods(Class cl)");
      println("{");
      pushDepth();
      println("try {");
      pushDepth();
      for (int i = 0; i < _proxyMethods.size(); i++) {
        Method method = _proxyMethods.get(i);
        
        print("method" + i + " = cl.getMethod(\"");
        print(method.getName());
        print("\", new Class[] {");
        Class []paramTypes = method.getParameterTypes();
        for (int j = 0; j < paramTypes.length; j++) {
          if (j != 0)
            print(", ");
          printClass(paramTypes[j]);
          print(".class");
        }
        println("});");
      }
      popDepth();
      println("} catch (Exception e) {");
      println("  com.caucho.ejb.EJBExceptionWrapper.createRuntime(e);");
      println("}");
      popDepth();
      println("}");
    }
    
    popDepth();
    println("}");
  }

  /**
   * Returns true if the class needs serialization.
   *
   * @param cl the class to test.
   *
   * @return true if the class needs serialization
   */
  boolean needsSerialization(Class cl)
  {
    if (cl.isPrimitive())
      return false;
    else if (! _isProxy && EJBObject.class.isAssignableFrom(cl))
      return false;
    else if (! _isProxy && EJBLocalObject.class.isAssignableFrom(cl))
      return false;
    else if (! _isProxy && EJBHome.class.isAssignableFrom(cl))
      return false;
    else if (! _isProxy && EJBLocalHome.class.isAssignableFrom(cl))
      return false;
    else
      return _immutableClasses.get(cl) < 0;
  }

  /**
   * Generates code for version changes.
   */
  protected void printVersionChange()
    throws IOException
  {
    println("if (com.caucho.ejb.Version.getVersionId() != " +
            com.caucho.ejb.Version.getVersionId() + ")");
    println("  return true;");
  }
  
  static {
    _immutableClasses = new IntMap();
    _immutableClasses.put(String.class, 1);
    _immutableClasses.put(Byte.class, 1);
    _immutableClasses.put(Character.class, 1);
    _immutableClasses.put(Short.class, 1);
    _immutableClasses.put(Integer.class, 1);
    _immutableClasses.put(Long.class, 1);
    _immutableClasses.put(Float.class, 1);
    _immutableClasses.put(Double.class, 1);
    _immutableClasses.put(Class.class, 1);
  }
}
