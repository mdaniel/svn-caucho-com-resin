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

package com.caucho.ejb.hessian;

import java.io.*;
import java.util.*;
import java.beans.*;
import java.lang.reflect.*;

import com.caucho.vfs.*;
import com.caucho.util.*;

import com.caucho.make.PersistentDependency;
import com.caucho.make.ClassDependency;

import com.caucho.ejb.*;

/**
 * Skeleton generator code for both Home and Remote interfaces.
 */
class HessianSkeletonGenerator extends MarshalGenerator {
  Class _cl;
  String _objClass;
  String _fullName;
  String _pkg;
  String _className;

  protected int _unique;
  
  protected ArrayList<Class> _marshallClasses;
  protected ArrayList<Class> _unmarshallClasses;
  
  protected ArrayList<Class> _marshallArrays;
  protected ArrayList<Class> _unmarshallArrays;
  protected Class _interfaceClass;
  protected ArrayList<PersistentDependency> _dependList;

  /**
   * Creates a skeleton generator for a given bean configuration.
   */
  HessianSkeletonGenerator(Class interfaceClass)
  {
    _interfaceClass = interfaceClass;
    
    _dependList = new ArrayList<PersistentDependency>();

    _dependList.add(new ClassDependency(_interfaceClass));
  }

  /**
   * Creates a home interface.
   */
  static Class generate(Class interfaceClass, Path workDir)
    throws Exception
  {
    HessianSkeletonGenerator gen = new HessianSkeletonGenerator(interfaceClass);
    gen.setClassDir(workDir);

    gen.setFullClassName("_ejb." + interfaceClass.getName() + "__HessianSkel");

    Class cl = gen.preload();
    if (cl != null)
      return cl;

    gen.generate();
    return gen.compile();
  }

  /**
   * Generates the Java source code for the home skeleton.
   */
  public void generateJava()
    throws IOException
  {
    ArrayList<Method> methodList = new ArrayList<Method>();

    Method []methods = _interfaceClass.getMethods();
    for (int i = 0; i < methods.length; i++) {
      String className = methods[i].getDeclaringClass().getName();
      String methodName = methods[i].getName();
      
      methodList.add(methods[i]);
    }
    
    printHeader();
    
    IntMap methodMap = printDispatcher(methodList);

    printDependList(_dependList);
    
    printFooter(methodMap);
  }

  /**
   * Prints the header of the generated class.
   */
  void printHeader()
    throws IOException
  {
    if (getPackageName() != null)
      println("package " + getPackageName() + ";");

    println();
    println("import java.io.*;");
    println("import com.caucho.util.*;");
    println("import com.caucho.ejb.hessian.*;");
    println("import com.caucho.hessian.io.*;");
    println("import " + _interfaceClass.getName() + ";");

    println();
    print("public class " + getClassName() + " extends com.caucho.ejb.hessian.HessianSkeleton");
    println(" {");
    
    pushDepth();

    println("private " + _interfaceClass.getName() + " obj;");
    println();
    println("protected void _setObject(Object o)");
    println("{");
    println("  obj = (" + _interfaceClass.getName() + ") o;");
    println("}");
  }

  /**
   * Creates the dispatch code.
   *
   * @param methodMap a hash map from method names to methods.
   */
  protected IntMap printDispatcher(ArrayList<Method> methods)
    throws IOException
  {
    IntMap map = new IntMap();
    
    println();
    print("protected void _execute(CharBuffer method, HessianInput in, HessianOutput out, com.caucho.ejb.xa.TransactionContext xa)");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("switch (methods.get(method)) {");

    int i = 0;
    for (; i < methods.size(); i++) {
      Method method = methods.get(i);

      if (map.get(method.getName()) < 0)
        map.put(method.getName(), i);
      
      map.put(mangleMethodName(method.getName(), method, false), i);

      if (i > 0)
        println();
      println("case " + i + ":");
      pushDepth();
      println("{");
      pushDepth();
      
      printUnmarshal(method.getName(), method.getParameterTypes(),
                     method.getReturnType());

      println("break;");
      popDepth();
      println("}");
      popDepth();
    }

    /*
    printMetaMethod(map, i++, "getAttribute",
                    new Class[] { String.class },
                    String.class);
    */

    println("default:");
    println("  _executeUnknown(method, in, out);");
    println("  break;");
    println("}");
    popDepth();
    println("}");

    return map;
  }

  /**
   * Print the unmarshalling and marshalling code for a single method.
   *
   * @param method the reflected method to define code.
   */
  void printUnmarshal(String methodName, Class []param, Class retType)
    throws IOException
  {
    for (int i = 0; i < param.length; i++) {
      String var = "_arg" + i;
      printClass(param[i]);
      print(" " + var + " = ");
      printUnmarshalType(param[i]);
    }

    println("in.completeCall();");

    if (! retType.getName().equals("void")) {
      printClass(retType);
      print(" _ret = ");
    }

    printCall("obj", methodName, param);

    println("startReply(out, xa);");

    if (! retType.equals(void.class))
      printMarshalType(retType, "_ret");
    else
      println("out.writeNull();");
    
    println("out.completeReply();");
  }

  /**
   * Prints a call to the underlying objects.
   *
   * @param objName the underlying object name
   * @param method the underlying method
   */
  void printCall(String objName, String methodName, Class []param)
    throws IOException
  {
    print(objName);
    print(".");
    print(methodName);
    print("(");
    for (int i = 0; i < param.length; i++) {
      if (i != 0)
        print(", ");
      print("_arg" + i);
    }
    println(");");
  }

  /**
   * Print the unmarshalling and marshalling code for a meta method.
   *
   * @param method the reflected method to define code.
   */
  void printMetaMethod(IntMap map, int index,
                       String methodName, Class []param, Class retType)
    throws IOException
  {
    map.put("_hessian_" + methodName, index);
    map.put("_hessian_" + mangleMethodName(methodName, param, true), index);

    println("case " + index + ":");
    println("{");
    pushDepth();
    
    for (int i = 0; i < param.length; i++) {
      String var = "_arg" + i;
      printClass(param[i]);
      println(" " + var + " = ");
      printUnmarshalType(param[i]);
    }

    println("is.expectEndTag(\"hessian:call\");");
    
    if (! retType.getName().equals("void")) {
      printClass(retType);
      println(" _ret;");
    }

    if (! retType.getName().equals("void")) {
      print("_ret = ");
    }

    printCall("this", methodName, param);
    
    println("os.print(\"<hessian:reply><value>\");");

    if (! retType.equals(void.class))
      printMarshalType(retType, "_ret");
    else
      print("os.print(\"<null></null>\");");
    
    println("os.print(\"</value></hessian:reply>\");");

    println("break;");
    popDepth();
    println("}");
  }

  /**
   * Returns the externally callable method name.
   */
  String getMethodName(Method method)
  {
    return method.getName();
  }

  /**
   * Prints the code at the end of the class to define the static constants.
   */
  protected void printFooter(IntMap methodMap)
    throws IOException
  {
    println("static private IntMap methods = new IntMap();"); 
    println("static {");
    pushDepth();

    Iterator keys = methodMap.iterator();
    while (keys.hasNext()) {
      String name = (String) keys.next();
      int value = methodMap.get(name);
      
      println("methods.put(new CharBuffer(\"" + name + "\")," + value + ");");
    }
    popDepth();
    println("}");
    popDepth();
    println("}");
  }
}
