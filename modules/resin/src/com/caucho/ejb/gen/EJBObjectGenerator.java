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

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;
import com.caucho.ejb.*;

/**
 * Generator for stubs.
 */
class EJBObjectGenerator extends AbstractGenerator {
  protected Class _skelClass;
  protected Class _remoteClass;
  protected String prefix = "_ejb_lo_";
  
  /**
   * Creates an instance of the generator
   */
  public EJBObjectGenerator(Class remoteClass, Class skelClass)
  {
    _remoteClass = remoteClass;
    _skelClass = skelClass;

    setFullClassName("_ejb." + _remoteClass.getName() + "__Stub");
  }

  /**
   * Generates the Java source.
   *
   * @param methods the methods to generate
   */
  public void generateJava()
    throws IOException
  {
    printHeader();
    
    Method []methods = _remoteClass.getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      String methodName = method.getName();
      Class declaringClass = method.getDeclaringClass();

      if (declaringClass.getName().startsWith("javax.ejb."))
        continue;

      printMethod(method.getName(), method);
    }

    printFooter();
  }

  /**
   * Prints the header for a ObjectStub
   */
  protected void printHeader()
    throws IOException
  {
    if (getPackageName() != null)
      println("package " + getPackageName() + ";");

    println();
    println("import java.io.*;");
    println("import java.rmi.*;");
    println("import " + _skelClass.getName() + ";");
    println("import " + _remoteClass.getName() + ";");
    
    println();
    println("public class " + getClassName());
    println("  extends com.caucho.ejb.SessionObject");
    println("  implements " + _remoteClass.getName());
    
    println("{");
    pushDepth();

    println("private " + _skelClass.getName() + " _obj;");

    println();
    println("public javax.ejb.SessionBean _getObject()");
    println("{");
    println("  return _obj;");
    println("}");
    
    println();
    println("public void _setObject(javax.ejb.SessionBean bean)");
    println("{");
    println("  _obj = (" + _skelClass.getName() + ") bean;");
    println("}");
  }

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
    
    println("{");
    pushDepth();

    if (! ret.getName().equals("void")) {
      printClass(ret);
      println(" _ret;");
    }
    
    if (! ret.getName().equals("void"))
      print("_ret = ");

    print("_obj." + prefix + method.getName() + "(");
    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        print(", ");
      print("a" + i);
    }
    println(");");

    if (! ret.getName().equals("void"))
      println("return _ret;");
    
    popDepth();
    println("}");
  }

  /**
   * Prints the class footer for the generated stub.
   */
  void printFooter()
    throws IOException
  {
    popDepth();
    println("}");
  }
}
