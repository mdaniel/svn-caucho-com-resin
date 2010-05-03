/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.gen;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.DependencyComponent;
import com.caucho.java.gen.GenClass;
import com.caucho.make.ClassDependency;
import com.caucho.vfs.PersistentDependency;

/**
 * Generates the skeleton for a bean.
 */
@Module
abstract public class BeanGenerator<X> extends GenClass
{
  private final AnnotatedType<X> _beanType;
  
  private final View<X> _view;

  private DependencyComponent _dependency = new DependencyComponent();

  protected BeanGenerator(String fullClassName,
                          AnnotatedType<X> beanType)
  {
    super(fullClassName);
    
    _beanType = beanType;

    addDependency(beanType.getJavaClass());
    
    _view = createView();
  }

  public AnnotatedType<X> getBeanType()
  {
    return _beanType;
  }

  protected void addDependency(PersistentDependency depend)
  {
    _dependency.addDependency(depend);
  }

  protected void addDependency(Class<?> cl)
  {
    _dependency.addDependency(new ClassDependency(cl));
  }

  /**
   * Returns the views.
   */
  public View<X> getView()
  {
    return _view;
  }

  /**
   * Generates the views for the bean
   */
  abstract protected View<X> createView();

  public void introspect()
  {
    
  }
  
  /**
   * Generates the view contents
   */
  public void generateViews(JavaWriter out)
    throws IOException
  {
    View<X> view = getView();
    
    out.println();

    view.generate(out);
  }

  /**
   * Generates the view contents
   */
  public void generateDestroyViews(JavaWriter out)
    throws IOException
  {
    View<X> view = getView();
    
    out.println();

    view.generateDestroy(out);
  }

  protected void generateDependency(JavaWriter out)
    throws IOException
  {
    _dependency.generate(out);
  }

  /**
   * Returns true if the method is implemented.
   */
  public boolean hasMethod(String methodName, Class<?> []paramTypes)
  {
    for (AnnotatedMethod<? super X> method : _beanType.getMethods()) {
      Method javaMethod = method.getJavaMember();
      
      if (! javaMethod.getName().equals(methodName))
        continue;
      
      if (! isMatch(javaMethod.getParameterTypes(), paramTypes))
        continue;
      
      return true;
    }
    
    return false;
  }
  
  private static boolean isMatch(Class<?> []typesA, Class<?> []typesB)
  {
    if (typesA.length != typesB.length)
      return false;
    
    for (int i = typesA.length - 1; i >= 0; i--) {
      if (! typesA[i].equals(typesB[i]))
        return false;
    }
    
    return true;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _beanType.getJavaClass().getSimpleName() + "]");
  }
}
