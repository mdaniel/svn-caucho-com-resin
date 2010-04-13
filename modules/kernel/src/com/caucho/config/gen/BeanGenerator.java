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

import com.caucho.make.*;
import com.caucho.util.L10N;
import com.caucho.config.inject.AnyLiteral;
import com.caucho.config.inject.DefaultLiteral;
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
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.AnnotatedMethod;

/**
 * Generates the skeleton for a bean.
 */
abstract public class BeanGenerator extends GenClass
{
  private static final L10N L = new L10N(BeanGenerator.class);

  protected final ApiClass _beanClass;

  protected DependencyComponent _dependency = new DependencyComponent();
  
  private ApiMethod _aroundInvokeMethod;

  private Set<Annotation> _decoratorBindings;
  private Set<Annotation> _interceptorBindings;

  private ArrayList<Type> _decorators
    = new ArrayList<Type>();

  protected BeanGenerator(String fullClassName, ApiClass beanClass)
  {
    super(fullClassName);
    
    _beanClass = beanClass;

    addDependency(beanClass.getJavaClass());
  }

  public ApiClass getBeanClass()
  {
    return _beanClass;
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
   * Gets the bindings for the decorators
   */
  public Set<Annotation> getDecoratorBindings()
  {
    return _decoratorBindings;
  }

  /**
   * Gets the bindings for the interceptors
   */
  public Set<Annotation> getInterceptorBindings()
  {
    return _interceptorBindings;
  }

  /**
   * Adds the method annotations
   */
  /*
  public void setMethodAnnotations(Method method, AnnotatedMethod annMethod)
  {
    _methodAnnotations.put(method, annMethod);
  }
  */

  /**
   * Adds the method annotations
   */
  /*
  public AnnotatedMethod getMethodAnnotations(Method method)
  {
    return _methodAnnotations.get(method);
  }
  */

  /**
   * Introspects the bean.
   */
  public void introspect()
  {
    _aroundInvokeMethod = findAroundInvokeMethod(_beanClass);

    // introspectClass(_beanClass);
    introspectDecorators(_beanClass);
  }
  
  /**
   * Finds the matching decorators for the class
   */
  protected void introspectDecorators(ApiClass cl)
  {
    if (cl.isAnnotationPresent(javax.decorator.Decorator.class))
      return;
    
    InjectManager webBeans = InjectManager.create();

    HashSet<Type> types = new HashSet<Type>();
    boolean isExtends = false;
    
    isExtends = fillTypes(types, cl.getJavaClass().getSuperclass(), isExtends);
    
    for (ApiClass iface : cl.getInterfaces()) {
      isExtends = fillTypes(types, iface.getJavaClass(), isExtends);
    }

    _decoratorBindings = new HashSet<Annotation>();
    
    boolean isQualifier = false;
    for (Annotation ann : cl.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
	_decoratorBindings.add(ann);
	
	if (! Named.class.equals(ann.annotationType())) {
	  isQualifier = true;
	}
      }
    }
    
    if (! isQualifier)
      _decoratorBindings.add(DefaultLiteral.DEFAULT);
    
    _decoratorBindings.add(AnyLiteral.ANY);

    Annotation []decoratorBindings;

    if (_decoratorBindings != null) {
      decoratorBindings = new Annotation[_decoratorBindings.size()];
      _decoratorBindings.toArray(decoratorBindings);
    }
    else
      decoratorBindings = new Annotation[0];

    List<Decorator<?>> decorators
      = webBeans.resolveDecorators(types, decoratorBindings);
    
    isExtends = false;
    for (Decorator<?> decorator : decorators) {
      // XXX:
      isExtends = fillTypes(_decorators, 
                            (Class<?>) decorator.getDelegateType(),
                            isExtends);
    }
  }

  private boolean fillTypes(HashSet<Type> types, Class<?> type,
                            boolean isExtends)
  {
    if (type == null || Object.class.equals(type))
      return isExtends;
    
    if (! type.isInterface()) {
      if (! isExtends)
        types.add(type);
      
      isExtends = true;
    }
    else
      types.add(type);

    isExtends = fillTypes(types, type.getSuperclass(), isExtends);

    for (Class<?> iface : type.getInterfaces()) {
      isExtends = fillTypes(types, iface, isExtends);
    }
    
    return isExtends;
  }

  protected boolean fillTypes(ArrayList<Type> types, Class<?> type,
                              boolean isExtends)
  {
    if (type == null || types.contains(type) || Object.class.equals(type))
      return isExtends;

    if (! type.isInterface()) {
      if (! isExtends)
        types.add(type);
      
      isExtends = true;
    }
    else {
      types.add(type);
    }
    
    isExtends = fillTypes(types, type.getSuperclass(), isExtends);

    for (Class<?> iface : type.getInterfaces()) {
      isExtends = fillTypes(types, iface, isExtends);
    }
    
    return isExtends;
  }

  /**
   * Returns the decorator classes
   */
  public ArrayList<Type> getDecoratorTypes()
  {
    return _decorators;
  }

  private static ApiMethod findAroundInvokeMethod(ApiClass cl)
  {
    if (cl == null)
      return null;

    for (ApiMethod method : cl.getMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class)
	  && method.getParameterTypes().length == 1
	  && method.getParameterTypes()[0].equals(InvocationContext.class)) {
	return method;
      }
    }

    return null;
  }

  /**
   * Returns the around-invoke method
   */
  public ApiMethod getAroundInvokeMethod()
  {
    return _aroundInvokeMethod;
  }

  /**
   * Gets the default interceptor
   */
  /*
  public ArrayList<Class> getDefaultInterceptors()
  {
    return _defaultInterceptors;
  }
  */

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
  public boolean hasMethod(String methodName, Class []paramTypes)
  {
    return _beanClass.hasMethod(methodName, paramTypes);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass.getSimpleName() + "]";
  }
}
