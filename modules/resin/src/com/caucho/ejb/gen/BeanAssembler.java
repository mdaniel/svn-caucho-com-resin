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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.ejb.cfg.EjbBean;

import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.ClassComponent;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.DependencyComponent;

import com.caucho.util.L10N;
import com.caucho.vfs.*;

/**
 * Assembles the generator structure.
 */
abstract public class BeanAssembler {
  private static final L10N L = new L10N(BeanAssembler.class);

  private EjbBean _bean;
  
  protected GenClass _genClass;
  protected GenClass _beanClass;

  protected DependencyComponent _dependency;

  public BeanAssembler(EjbBean bean,
		       String fullClassName)
  {
    _bean = bean;
    _genClass = new GenClass(fullClassName);
    _dependency = new DependencyComponent();
    _genClass.addComponent(_dependency);
  }

  /**
   * Returns the short classname.
   */
  public String getShortClassName()
  {
    return _genClass.getClassName();
  }

  /**
   * Returns assembled class.
   */
  public GenClass getAssembledGenerator()
  {
    return _genClass;
  }

  /**
   * Adds an import.
   */
  public void addImport(String importName)
  {
    _genClass.addImport(importName);
  }

  /**
   * Adds the header component.
   */
  public void addHeaderComponent(JClass beanClass,
				 String contextClassName,
				 String implClassName)
  {
  }

  /**
   * Adds the bean method
   */
  public void addMethod(BaseMethod method)
  {
    addComponent(method);
  }

  /**
   * Adds the bean method
   */
  public void addComponent(ClassComponent component)
  {
  }

  /**
   * Adds a dependency
   */
  public void addDependency(PersistentDependency depend)
  {
    _dependency.addDependency(depend);
  }

  /**
   * Creates the home view.
   */
  abstract public ViewClass createHomeView(JClass homeClass,
					   String fullClassName,
					   String viewPrefix);

  /**
   * Creates the home view.
   */
  abstract public ViewClass createView(JClass homeClass,
				       String fullClassName,
				       String viewPrefix);

  /**
   * Checks for the existence of a method.
   */
  public static boolean hasMethod(JClass cl, String methodName, JClass []args)
  {
    return cl.getMethod(methodName, args) != null;
  }
}
