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

package com.caucho.config.gen;

import com.caucho.make.*;
import com.caucho.util.L10N;
import com.caucho.config.inject.InjectManager;
import com.caucho.java.*;
import com.caucho.java.gen.*;
import com.caucho.vfs.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.interceptor.*;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.AnnotatedMethod;

/**
 * Generates the skeleton for a bean.
 */
abstract public class BeanGenerator extends GenClass
{
  private static final L10N L = new L10N(BeanGenerator.class);

  protected final ApiClass _ejbClass;

  protected DependencyComponent _dependency = new DependencyComponent();
  
  private Method _aroundInvokeMethod;

  private ArrayList<Class> _defaultInterceptors
    = new ArrayList<Class>();

  private Set<Annotation> _bindings;

  private Set<Annotation> _interceptorBindings;

  private ArrayList<Type> _decorators
    = new ArrayList<Type>();

  private HashMap<Method,AnnotatedMethod> _methodAnnotations
    = new HashMap<Method,AnnotatedMethod>();

  protected BeanGenerator(String fullClassName, ApiClass ejbClass)
  {
    super(fullClassName);
    
    _ejbClass = ejbClass;

    addDependency(ejbClass.getJavaClass());
  }

  protected ApiClass getEjbClass()
  {
    return _ejbClass;
  }
  
  /**
   * Sets the remote name
   */
  public void setRemoteHome(ApiClass homeClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
 
  /**
   * Sets the local name
   */
  public void setLocalHome(ApiClass homeClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds a remote
   */
  public void addRemote(ApiClass remoteApi)
  {
  }

  /**
   * Adds a local
   */
  public void addLocal(ApiClass localApi)
  {
  }

  protected void addDependency(PersistentDependency depend)
  {
    _dependency.addDependency(depend);
  }

  protected void addDependency(Class cl)
  {
    _dependency.addDependency(new ClassDependency(cl));
  }

  /**
   * Gets the bindings for the decorators
   */
  public Set<Annotation> getBindings()
  {
    return _bindings;
  }

  /**
   * Sets the bindings for the decorators
   */
  public void setBindings(Set<Annotation> annotation)
  {
    _bindings = annotation;
  }


  /**
   * Gets the bindings for the interceptors
   */
  public Set<Annotation> getInterceptorBindings()
  {
    return _interceptorBindings;
  }

  /**
   * Sets the bindings for the interceptors
   */
  public void setInterceptorBindings(Set<Annotation> annotation)
  {
    _interceptorBindings = annotation;
  }

  /**
   * Adds the method annotations
   */
  public void setMethodAnnotations(Method method, AnnotatedMethod annMethod)
  {
    _methodAnnotations.put(method, annMethod);
  }

  /**
   * Adds the method annotations
   */
  public AnnotatedMethod getMethodAnnotations(Method method)
  {
    return _methodAnnotations.get(method);
  }

  /**
   * Introspects the bean.
   */
  public void introspect()
  {
    _aroundInvokeMethod = findAroundInvokeMethod(_ejbClass.getJavaClass());

    introspectDecorators(_ejbClass.getJavaClass());
  }

  public boolean isAnnotationPresent(Class cl)
  {
    return _ejbClass.getJavaClass().isAnnotationPresent(cl);
  }
  
  /**
   * Finds the matching decorators for the class
   */
  protected void introspectDecorators(Class cl)
  {
    if (cl.isAnnotationPresent(javax.decorator.Decorator.class))
      return;
    
    InjectManager webBeans = InjectManager.create();

    HashSet<Type> types = new HashSet<Type>();
    for (Class iface : cl.getInterfaces()) {
      fillTypes(types, iface);
    }

    if (_bindings == null)
      return;

    Annotation []bindings = new Annotation[_bindings.size()];
    _bindings.toArray(bindings);

    List<Decorator<?>> decorators
      = webBeans.resolveDecorators(types, bindings);
    
    for (Decorator decorator : decorators) {
      // XXX:
      fillTypes(_decorators, (Class) decorator.getDelegateType());
    }
  }

  protected void fillTypes(HashSet<Type> types, Class type)
  {
    if (type == null)
      return;
    
    types.add(type);

    fillTypes(types, type.getSuperclass());

    for (Class iface : type.getInterfaces()) {
      fillTypes(types, iface);
    }
  }

  protected void fillTypes(ArrayList<Type> types, Class type)
  {
    if (type == null || types.contains(type))
      return;

    types.add(type);

    fillTypes(types, type.getSuperclass());

    for (Class iface : type.getInterfaces()) {
      fillTypes(types, iface);
    }
  }

  /**
   * Returns the decorator classes
   */
  public ArrayList<Type> getDecoratorTypes()
  {
    return _decorators;
  }

  private static Method findAroundInvokeMethod(Class cl)
  {
    if (cl == null)
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class)
	  && method.getParameterTypes().length == 1
	  && method.getParameterTypes()[0].equals(InvocationContext.class)) {
	return method;
      }
    }

    return findAroundInvokeMethod(cl.getSuperclass());
  }

  /**
   * Returns the around-invoke method
   */
  public Method getAroundInvokeMethod()
  {
    return _aroundInvokeMethod;
  }

  /**
   * Sets the around-invoke method
   */
  public void setAroundInvokeMethod(Method method)
  {
    _aroundInvokeMethod = method;
  }

  /**
   * Adds a default interceptor
   */
  public void addInterceptor(Class cl)
  {
    if (_defaultInterceptors.indexOf(cl) < 0)
      _defaultInterceptors.add(cl);
  }

  /**
   * Gets the default interceptor
   */
  public ArrayList<Class> getDefaultInterceptors()
  {
    return _defaultInterceptors;
  }

  /**
   * Returns the views.
   */
  public ArrayList<View> getViews()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the views for the bean
   */
  public void createViews()
  {
  }

  /**
   * Generates the view contents
   */
  public void generateViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generate(out);
    }
  }

  /**
   * Generates the view contents
   */
  public void generateDestroyViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generateDestroy(out);
    }
  }

  protected void generateDependency(JavaWriter out)
    throws IOException
  {
    _dependency.generate(out);
  }

  /**
   * Returns true if the method is implemented.
   */
  protected boolean hasMethod(String methodName, Class []paramTypes)
  {
    return _ejbClass.hasMethod(methodName, paramTypes);
  }

  private String generateTypeCasting(String value, Class cl, boolean isEscapeString)
  {
    if (cl.equals(String.class)) {
      if (isEscapeString)
        value = "\"" + value + "\"";
    } else if (cl.equals(Character.class))
      value = "'" + value + "'";
    else if (cl.equals(Byte.class))
      value = "(byte) " + value;
    else if (cl.equals(Short.class))
      value = "(short) " + value;
    else if (cl.equals(Integer.class))
      value = "(int) " + value;
    else if (cl.equals(Long.class))
      value = "(long) " + value;
    else if (cl.equals(Float.class))
      value = "(float) " + value;
    else if (cl.equals(Double.class))
      value = "(double) " + value;

    return value;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ejbClass.getJavaClass().getSimpleName() + "]";
  }
}
