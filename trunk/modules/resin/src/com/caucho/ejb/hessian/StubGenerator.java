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

package com.caucho.ejb.hessian;

import com.caucho.config.ConfigException;
import com.caucho.java.WorkDir;
import com.caucho.make.ClassDependency;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.PersistentDependency;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Generator for stubs.
 */
class StubGenerator extends MarshalGenerator {
  private ArrayList<PersistentDependency> _dependList;
  
  StubGenerator()
  {
    setClassDir(WorkDir.getLocalWorkDir().lookup("ejb"));
  }
  
  Class createHomeStub(Class cl)
    throws ConfigException
  {
    return makeClient(cl, "__HessianStub", true);
  }
  
  Class createObjectStub(Class cl)
    throws ConfigException
  {
    return makeClient(cl, "__HessianStub", false);
  }
  
  Class createStub(Class cl)
    throws ConfigException
  {
    return makeClient(cl, "__HessianStub", true);
  }

  /**
   * Creates a client stub.
   *
   * @param cl the remote interface of the stub
   * @param genSuffix the suffix for the generated class
   */
  Class makeClient(Class cl, String genSuffix, boolean isHome)
    throws ConfigException
  {
    _cl = cl;

    try {
      setFullClassName("_ejb." + cl.getName() + genSuffix);

      if (cl.getClassLoader() != null)
        setParentLoader(cl.getClassLoader());
    
      MergePath mergePath = new MergePath();
      if (cl.getClassLoader() != null)
        mergePath.addClassPath(cl.getClassLoader());
      else
        mergePath.addClassPath(Thread.currentThread().getContextClassLoader());
      setSearchPath(mergePath);

      _dependList = new ArrayList<PersistentDependency>();

      _dependList.add(new ClassDependency(cl));

      Class stubClass = preload();
      if (stubClass != null)
        return stubClass;

      generate();
    
      return compile();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void generateJava()
    throws IOException
  {
    generateJava(_cl.getMethods());
  }


  /**
   * Generates the Java source.
   *
   * @param methods the methods to generate
   */
  private void generateJava(Method []methods)
    throws IOException
  {
    if (javax.ejb.EJBHome.class.isAssignableFrom(_cl))
      printHeader("HomeStub");
    else if (javax.ejb.EJBObject.class.isAssignableFrom(_cl))
      printHeader("ObjectStub");
    else
      printHeader("ObjectStub");

    println("public String getHessianType()");
    println("{");
    println("  return \"" + _cl.getName() + "\";");
    println("}");

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      Class declaringClass = method.getDeclaringClass();
      String prefix = "";

      if (declaringClass.getName().startsWith("javax.ejb."))
        prefix = "_ejb_";

      /*
      Class []exns = method.getExceptionTypes();
      for (int j = 0; j < exns.length; j++) {
        if (exns[j].isAssignableFrom(java.rmi.RemoteException.class)) {
          printMethod(prefix + method.getName(), method);
          break;
        }
      }
      */
      printMethod(prefix + method.getName(), method);
    }
    
    printDependList(_dependList);

    printFooter();
  }

  /**
   * Prints the header for a HomeStub
   */
  void printHeader(String stubClassName)
    throws IOException
  {
    if (getPackageName() != null)
      println("package " + getPackageName() + ";");

    println();
    println("import java.io.*;");
    println("import java.rmi.*;");
    println("import com.caucho.vfs.*;");
    println("import com.caucho.util.*;");
    println("import com.caucho.ejb.hessian.*;");
    println("import com.caucho.hessian.io.*;");
    println("import " + _cl.getName() + ";");
    
    println();
    println("public class " + getClassName() + " extends " + stubClassName);
    print("  implements " + _cl.getName());

    println(" {");
    pushDepth();
  }

  /**
   * Generates the code for a remote stub method.
   *
   * @param name the name of the remote
   * @param method the reflected object for the method
   */
  void printMethod(String name, Method method)
    throws IOException
  {
    Class ret = method.getReturnType();
    Class []params = method.getParameterTypes();

    printMethodDeclaration(name, method);
    
    println("{");
    pushDepth();

    println("HessianWriter out = _hessian_openWriter();");
    println("try {");
    pushDepth();

    String mangleName = mangleMethodName(method.getName(), method, false);
    
    println("out.startCall();");
    println("_hessian_writeHeaders(out);");
    println("out.writeMethod(\"" + mangleName + "\");");

    for (int i = 0; i < params.length; i++)
      printMarshalType(params[i], "_arg" + i);

    println("Hessian2Input in = out.doCall();");

    if (! void.class.equals(ret)) {
      printClass(ret);
      println(" _ret;");
      print("_ret = ");
      printUnmarshalType(ret);
    }
    else {
      println("in.readNull();");
    }

    println("in.completeReply();");

    println("_hessian_freeWriter(out);");
    println("out = null;");

    if (! void.class.equals(ret))
      println("return _ret;");
    
    popDepth();

    Class []exn = method.getExceptionTypes();

    boolean hasThrowable = false;
    boolean hasRuntimeException = false;
    
    loop:
    for (int i = 0; i < exn.length; i++) {
      for (int j = 0; j < i; j++) {
        if (exn[j].isAssignableFrom(exn[i]))
          continue loop;
      }
      
      if (! hasThrowable) {
        println("} catch (" + exn[i].getName() + " e) {");
        println("  throw e;");
      }
      
      if (exn[i].equals(Throwable.class)) {
        hasThrowable = true;
        hasRuntimeException = true;
      }
      
      if (exn[i].equals(Exception.class))
        hasRuntimeException = true;
      if (exn[i].equals(RuntimeException.class))
        hasRuntimeException = true;
    }

    if (! hasRuntimeException) {
      println("} catch (RuntimeException e) {");
      println("  throw e;");
    }
    
    if (! hasThrowable) {
      println("} catch (Throwable e) {");
      println("  throw new com.caucho.ejb.EJBExceptionWrapper(\"stub exception\", e);");
    }
    
    println("} finally {");
    println("  if (out != null) out.close();");
    println("}");

    popDepth();
    println("}");
  }

  /**
   * Prints the class footer for the generated stub.
   */
  void printFooter()
    throws IOException
  {
    println();
    println("public String toString()");
    println("{");
    pushDepth();
    println("return \"[HessianStub " + _cl.getName() + " \" + _urlPath + \"]\";");
    popDepth();
    println("}");
    
    popDepth();
    println("}");
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
}
