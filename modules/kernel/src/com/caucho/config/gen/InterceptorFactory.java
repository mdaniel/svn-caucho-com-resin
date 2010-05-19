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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Decorator;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.Interceptors;

import com.caucho.config.inject.AnyLiteral;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents the interception
 */
@Module
public class InterceptorFactory<X>
  extends AbstractAspectFactory<X>
{
  private InjectManager _manager;
  
  private boolean _isInterceptorOrDecorator;
  
  private ArrayList<Class<?>> _classInterceptors;
  private ArrayList<Annotation> _classInterceptorBinding;
  
  private HashSet<Class<?>> _decoratorClasses;
  
  public InterceptorFactory(AspectBeanFactory<X> beanFactory,
                            AspectFactory<X> next,
                            InjectManager manager)
  {
    super(beanFactory, next);
    
    _manager = manager;
    
    introspectType();
  }
  
  public ArrayList<Class<?>> getClassInterceptors()
  {
    return _classInterceptors;
  }
  
  public ArrayList<Annotation> getClassInterceptorBinding()
  {
    return _classInterceptorBinding;
  }
  
  public HashSet<Class<?>> getDecoratorClasses()
  {
    return _decoratorClasses;
  }
  
  /**
   * Creates an aspect for interception if the method should be intercepted.
   */
  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced)
  {
    if (_isInterceptorOrDecorator)
      return super.create(method, isEnhanced);
    
    if (method.isAnnotationPresent(Inject.class))
      return super.create(method, isEnhanced);

    // ioc/0c57
    if (method.isAnnotationPresent(PostConstruct.class))
      return super.create(method, isEnhanced);
    
    HashSet<Class<?>> methodInterceptors = null;

    Interceptors interceptors = method.getAnnotation(Interceptors.class);

    if (interceptors != null) {
      for (Class<?> iClass : interceptors.value()) {
        if (! hasAroundInvoke(iClass)) {
          continue;
        }
        
        if (methodInterceptors == null)
          methodInterceptors = new HashSet<Class<?>>();
        
        methodInterceptors.add(iClass);
      }
    }

    HashMap<Class<?>, Annotation> interceptorMap = null;

    interceptorMap = addInterceptorBindings(interceptorMap, 
                                            method.getAnnotations());

    /*
    if (interceptorTypes.size() > 0) {
      _interceptionType = InterceptionType.AROUND_INVOKE;
      _interceptorBinding.addAll(interceptorTypes.values());
    }
    */
    
    HashSet<Class<?>> decoratorSet = introspectDecorators(method);
    
    if (methodInterceptors != null
        || interceptorMap != null
        || decoratorSet != null
        || _classInterceptors != null
        || _classInterceptorBinding != null) {
      AspectGenerator<X> next = super.create(method, true);
      
      return new InterceptorGenerator<X>(this, method, next,
                                         methodInterceptors, 
                                         interceptorMap,
                                         decoratorSet);
    }
    else
      return super.create(method, isEnhanced);
  }
  
  private boolean hasAroundInvoke(Class<?> cl)
  {
    for (Method m : cl.getMethods()) {
      if (m.isAnnotationPresent(AroundInvoke.class))
        return true;
    }
    
    return false;
  }
  
  //
  // introspection
  //
  
  private void introspectType()
  {
    // interceptors aren't intercepted
    if (getBeanType().isAnnotationPresent(javax.interceptor.Interceptor.class)
        || getBeanType().isAnnotationPresent(javax.decorator.Decorator.class)) {
      _isInterceptorOrDecorator = true;
      return;
    }
    
    introspectClassInterceptors();
    introspectClassInterceptorBindings();
    introspectClassDecorators();
  }
  
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private void introspectClassInterceptors()
  {
    Interceptors interceptors = getBeanType().getAnnotation(Interceptors.class);

    if (interceptors != null) {
      _classInterceptors = new ArrayList<Class<?>>();
    
      for (Class<?> iClass : interceptors.value()) {
        if (! _classInterceptors.contains(iClass))
          _classInterceptors.add(iClass);
      }
    }
  }
  
  /**
   * Introspects the CDI interceptor bindings on the class
   */
  private void introspectClassInterceptorBindings()
  {
    for (Annotation ann : getBeanType().getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
        
      }
    }
    
    HashMap<Class<?>, Annotation> interceptorMap
      = addInterceptorBindings(null, getBeanType().getAnnotations());

    if (interceptorMap != null) {
      _classInterceptorBinding 
        = new ArrayList<Annotation>(interceptorMap.values());
    }
  }
  
  /**
   * Finds the matching decorators for the class
   */
  private void introspectClassDecorators()
  {
    Set<Type> types = getBeanType().getTypeClosure();
    
    HashSet<Annotation> decoratorBindingSet = new HashSet<Annotation>();
    
    boolean isQualifier = false;
    for (Annotation ann : getBeanType().getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        decoratorBindingSet.add(ann);

        if (! Named.class.equals(ann.annotationType())) {
          isQualifier = true;
        }
      }
    }
    
    if (! isQualifier)
      decoratorBindingSet.add(DefaultLiteral.DEFAULT);
    
    decoratorBindingSet.add(AnyLiteral.ANY);

    Annotation []decoratorBindings;

    if (decoratorBindingSet != null) {
      decoratorBindings = new Annotation[decoratorBindingSet.size()];
      decoratorBindingSet.toArray(decoratorBindings);
    }
    else
      decoratorBindings = new Annotation[0];

    List<Decorator<?>> decorators
      = _manager.resolveDecorators(types, decoratorBindings);
    
    if (decorators.size() == 0)
      return;
    
    HashSet<Type> closure = new HashSet<Type>();
    
    for (Decorator<?> decorator : decorators) {
      BaseType type = _manager.createTargetBaseType(decorator.getDelegateType());
      
      closure.addAll(type.getTypeClosure(_manager));
    }
    
    _decoratorClasses = new HashSet<Class<?>>();
    
    for (Type genericType : closure) {
      BaseType type = _manager.createTargetBaseType(genericType);
      
      Class<?> rawClass = type.getRawClass();

      if (Object.class.equals(rawClass))
        continue;
      
      _decoratorClasses.add(rawClass);
    }
  }

  private HashSet<Class<?>> 
  introspectDecorators(AnnotatedMethod<? super X> annMethod)
  {
    if (annMethod.getJavaMember().getDeclaringClass().equals(Object.class))
      return null;
    
    if (_decoratorClasses == null)
      return null;
    
    HashSet<Class<?>> decoratorSet = null;

    for (Class<?> decoratorClass : _decoratorClasses) {
      for (Method method : decoratorClass.getMethods()) {
        if (AnnotatedTypeUtil.isMatch(method, annMethod.getJavaMember())) {
          if (decoratorSet == null)
            decoratorSet = new HashSet<Class<?>>();
          
          decoratorSet.add(decoratorClass); 
        }
      }
    }
    
    return decoratorSet;
  }

  private HashMap<Class<?>,Annotation>
  addInterceptorBindings(HashMap<Class<?>,Annotation> interceptorMap,
                         Set<Annotation> annotations)
  {
    for (Annotation ann : annotations) {
      interceptorMap = addInterceptorBindings(interceptorMap, ann);
    }
    
    return interceptorMap;
  }

  private HashMap<Class<?>,Annotation>
  addInterceptorBindings(HashMap<Class<?>,Annotation> interceptorMap,
                         Annotation ann)
  {
    Class<?> annType = ann.annotationType();

    if (annType.isAnnotationPresent(InterceptorBinding.class)) {
      if (interceptorMap == null)
        interceptorMap = new HashMap<Class<?>,Annotation>();
        
      interceptorMap.put(ann.annotationType(), ann);
    }

    if (annType.isAnnotationPresent(Stereotype.class)) {
      for (Annotation subAnn : annType.getAnnotations()) {
        interceptorMap = addInterceptorBindings(interceptorMap, subAnn);
      }
    }
    
    return interceptorMap;
  }

  private boolean isMatch(Method methodA, Method methodB)
  {
    if (! methodA.getName().equals(methodB.getName()))
      return false;

    Class<?>[] paramA = methodA.getParameterTypes();
    Class<?>[] paramB = methodB.getParameterTypes();

    if (paramA.length != paramB.length)
      return false;

    for (int i = 0; i < paramA.length; i++) {
      if (! paramA[i].equals(paramB[i]))
        return false;
    }

    return true;
  }

  /**
   * @return
   */
  public AnnotatedMethod<? super X> getAroundInvokeMethod()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @param out
   */
  public void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("this");
  }

  /**
   * @param out
   */
  public void generateBeanInfo(JavaWriter out)
    throws IOException
  {
    out.print("bean");
    
  }
}
