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

package com.caucho.ejb3.gen;

import com.caucho.ejb.gen.BusinessMethodGenerator;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.ejb.*;

/**
 * Generates the skeleton for a session bean.
 */
public class PojoBean extends GenClass {
  private static final L10N L = new L10N(PojoBean.class);

  private Class _beanClass;

  private ArrayList<BusinessMethodGenerator> _businessMethods
    = new ArrayList<BusinessMethodGenerator>();

  private boolean _isEnhanced;
  private boolean _hasXA;
  
  public PojoBean(Class beanClass)
  {
    super(beanClass.getName() + "__Resin");

    addImport("javax.ejb.*");
    addImport("javax.transaction.*");

    setSuperClassName(beanClass.getName());    
    
    _beanClass = beanClass;

    introspect();
  }

  protected void introspect()
  {
    for (Method method : _beanClass.getMethods()) {
      if (Object.class.equals(method.getDeclaringClass()))
	continue;
      
      int modifiers = method.getModifiers();

      if (! Modifier.isPublic(modifiers) && ! Modifier.isProtected(modifiers))
	continue;
      if (Modifier.isStatic(modifiers))
	continue;
      if (Modifier.isFinal(modifiers))
	continue;

      int index = _businessMethods.size();
      BusinessMethodGenerator bizMethod
	= new BusinessMethodGenerator(method, method, index);

      bizMethod.introspect(method, method);

      if (bizMethod.isEnhanced()) {
	_isEnhanced = true;
	_businessMethods.add(bizMethod);
      }
    }
  }

  public Class generateClass()
  {
    if (! isEnhanced())
      return _beanClass;
    
    try {
      JavaClassGenerator gen = new JavaClassGenerator();

      Class cl = gen.preload(getFullClassName());

      if (cl != null)
	return cl;

      gen.generate(this);

      gen.compilePendingJava();

      return gen.loadClass(getFullClassName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean isEnhanced()
  {
    return _isEnhanced;
  }

  @Override
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    generateHeader(out);

    for (Constructor ctor : _beanClass.getDeclaredConstructors()) {
      if (Modifier.isPublic(ctor.getModifiers()))
	generateConstructor(out, ctor);
    }

    HashMap map = new HashMap();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generate(out, map);
    }

    super.generateClassContent(out);
  }

  /**
   * Generates header and prologue data.
   */
  protected void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println("private static final java.util.logging.Logger __log");
    out.println("  = java.util.logging.Logger.getLogger(\"" + getFullClassName() + "\");");
    out.println("private static final boolean __isFiner");
    out.println("  = __log.isLoggable(java.util.logging.Level.FINER);");

    if (_hasXA) {
      out.println();
      out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
      out.println("  = new com.caucho.ejb3.xa.XAManager();");
    }
  }
  
  protected void generateConstructor(JavaWriter out, Constructor ctor)
    throws IOException
  {
    Class []paramTypes = ctor.getParameterTypes();
    
    out.print("public " + getClassName() + "(");

    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
	out.print(", ");

      out.printClass(paramTypes[i]);
      out.print(" a" + i);
    }
    
    out.println(")");

    generateThrows(out, ctor.getExceptionTypes());
    
    out.println("{");
    out.pushDepth();

    out.print("super(");

    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print("a" + i);
    }
    out.println(");");
    
    out.popDepth();
    out.println("}");
  }

  protected void generateThrows(JavaWriter out, Class []exnCls)
    throws IOException
  {
    if (exnCls.length == 0)
      return;

    out.print(" throws ");
    
    for (int i = 0; i < exnCls.length; i++) {
      if (i != 0)
	out.print(", ");

      out.printClass(exnCls[i]);
    }
  }
}
