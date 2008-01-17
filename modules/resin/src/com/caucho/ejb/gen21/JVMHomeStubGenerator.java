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

package com.caucho.ejb.gen21;

import com.caucho.ejb.gen.*;
import com.caucho.make.ClassDependency;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.PersistentDependency;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Generator for stubs.
 */
public class JVMHomeStubGenerator extends JVMStubGenerator {
  protected ArrayList<PersistentDependency> _dependList;

  /**
   * Creates an instance of the generator
   */
  public JVMHomeStubGenerator(Class remoteHomeClass, boolean isProxy)
  {
    _remoteClass = remoteHomeClass;

    _isProxy = isProxy;

    if (isProxy)
      setFullClassName(remoteHomeClass.getName() + "__JVMProxy");
    else
      setFullClassName(remoteHomeClass.getName() + "__JVMStub");
    
    MergePath mergePath = new MergePath();
    setSearchPath(mergePath);

    _dependList = new ArrayList<PersistentDependency>();

    _dependList.add(new ClassDependency(remoteHomeClass));
  }
  
  /**
   * Creates the home stub for the home interface
   */
  public Class generateStub()
    throws Exception
  {
    Class home = preload();
    if (home != null)
      return home;

    generate();
    
    return compile();
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

      Class []exns = method.getExceptionTypes();
      for (int j = 0; j < exns.length; j++) {
        if (exns[j].isAssignableFrom(java.rmi.RemoteException.class)) {
          printMethod(method.getName(), method);
          break;
        }
      }
    }
    
    printDependList(_dependList);

    printFooter();
  }

  /**
   * Prints the header for a HomeStub
   */
  protected void printHeader()
    throws IOException
  {
    if (getPackageName() != null)
      println("package " + getPackageName() + ";");

    println();
    println("import java.io.*;");
    println("import java.rmi.*;");
    println("import " + _remoteClass.getName() + ";");
    
    println();
    println("public class " + getClassName());
    println("  extends com.caucho.ejb.JVMHome");
    println("  implements " + _remoteClass.getName());
    
    println("{");
    pushDepth();
  }
}
