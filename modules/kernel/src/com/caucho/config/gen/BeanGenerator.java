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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.caucho.config.inject.AnyLiteral;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
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
  protected final AnnotatedType<X> _beanClass;

  protected DependencyComponent _dependency = new DependencyComponent();
  
  private AnnotatedMethod<? super X> _aroundInvokeMethod;

  private Set<Annotation> _decoratorBindings;
  private Set<Annotation> _interceptorBindings;

  private ArrayList<Type> _decorators
    = new ArrayList<Type>();

  protected BeanGenerator(String fullClassName,
                          AnnotatedType<X> beanClass)
  {
    super(fullClassName);
    
    _beanClass = beanClass;

    addDependency(beanClass.getJavaClass());
  }

  public AnnotatedType<X> getBeanClass()
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
  protected void introspectDecorators(AnnotatedType<X> type)
  {
    if (type.isAnnotationPresent(javax.decorator.Decorator.class))
      return;
    
    InjectManager inject = InjectManager.create();

    Set<Type> types = type.getTypeClosure();
    
    _decoratorBindings = new HashSet<Annotation>();
    
    boolean isQualifier = false;
    for (Annotation ann : type.getAnnotations()) {
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
      = inject.resolveDecorators(types, decoratorBindings);
    
    boolean isExtends = false;
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

  private static <X> AnnotatedMethod<? super X> findAroundInvokeMethod(AnnotatedType<X> cl)
  {
    if (cl == null)
      return null;

    for (AnnotatedMethod<? super X> method : cl.getMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class)
	  && method.getJavaMember().getParameterTypes().length == 1
	  && method.getJavaMember().getParameterTypes()[0].equals(InvocationContext.class)) {
	return method;
      }
    }

    return null;
  }

  /**
   * Returns the around-invoke method
   */
  public AnnotatedMethod<? super X> getAroundInvokeMethod()
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
  public ArrayList<View<X,?>> getViews()
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
    for (View<X,?> view : getViews()) {
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
    for (View<X,?> view : getViews()) {
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
  public boolean hasMethod(String methodName, Class<?> []paramTypes)
  {
    for (AnnotatedMethod<? super X> method : _beanClass.getMethods()) {
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
            + "[" + _beanClass.getJavaClass().getSimpleName() + "]");
  }
}
