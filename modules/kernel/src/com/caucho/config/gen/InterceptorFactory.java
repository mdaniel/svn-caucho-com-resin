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
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Delegate;
import javax.ejb.Stateful;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.AnyLiteral;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents the interception
 */
@Module
public class InterceptorFactory<X>
  extends AbstractAspectFactory<X>
{
  private static final L10N L = new L10N(InterceptorFactory.class);
  
  private InjectManager _manager;
  
  private boolean _isInterceptorOrDecorator;
  
  private ArrayList<Class<?>> _classAroundInvokeInterceptors
    = new ArrayList<Class<?>>();
  private ArrayList<Class<?>> _classPostConstructInterceptors
    = new ArrayList<Class<?>>();
  private ArrayList<Class<?>> _classPreDestroyInterceptors
    = new ArrayList<Class<?>>();
  
  private HashMap<Class<?>, Annotation> _classInterceptorBindings;
  
  private HashSet<Class<?>> _decoratorClasses;
  
  private boolean _isPassivating;
  
  public InterceptorFactory(AspectBeanFactory<X> beanFactory,
                            AspectFactory<X> next,
                            InjectManager manager)
  {
    super(beanFactory, next);
    
    _manager = manager;
    
    introspectType();
  }
  
  public ArrayList<Class<?>> getClassAroundInvokeInterceptors()
  {
    return _classAroundInvokeInterceptors;
  }
  
  public HashMap<Class<?>, Annotation>
  getClassInterceptorBindings()
  {
    return _classInterceptorBindings;
  }
  
  public HashSet<Class<?>> getDecoratorClasses()
  {
    return _decoratorClasses;
  }
  
  public boolean isPassivating()
  {
    return _isPassivating;
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
    
    HashSet<Class<?>> methodInterceptors = null;

    Interceptors interceptors = method.getAnnotation(Interceptors.class);

    if (interceptors != null) {
      for (Class<?> iClass : interceptors.value()) {
        if (! hasAroundInvoke(iClass)) {
          continue;
        }
        
        if (methodInterceptors == null)
          methodInterceptors = new LinkedHashSet<Class<?>>();
        
        methodInterceptors.add(iClass);
      }
    }

    HashMap<Class<?>, Annotation> interceptorMap = null;
    
    interceptorMap = addInterceptorBindings(interceptorMap, 
                                            method.getAnnotations());
    
    HashSet<Class<?>> decoratorSet = introspectDecorators(method);
    
    if (method.isAnnotationPresent(Inject.class)
        || method.isAnnotationPresent(PostConstruct.class)) {
      if (methodInterceptors != null || interceptorMap != null) {
        throw new ConfigException(L.l("{0}.{1} is invalid because it's annotated with @Inject or @PostConstruct but also has interceptor bindings",
                                      method.getJavaMember().getDeclaringClass().getName(),
                                      method.getJavaMember().getName()));
      }
      // ioc/0c57
      return super.create(method, isEnhanced);
    }
    
    boolean isExcludeClassInterceptors
      = method.isAnnotationPresent(ExcludeClassInterceptors.class);

    if (isExcludeClassInterceptors || _classInterceptorBindings == null) {
    }
    else if (interceptorMap != null) {
      interceptorMap.putAll(_classInterceptorBindings);
    }
    else {
      interceptorMap = _classInterceptorBindings;
    }
    
    if (methodInterceptors != null
        || interceptorMap != null
        || decoratorSet != null
        || _classAroundInvokeInterceptors.size() > 0 && ! isExcludeClassInterceptors) {
      AspectGenerator<X> next = super.create(method, true);
      
      return new InterceptorGenerator<X>(this, method, next,
                                         methodInterceptors, 
                                         interceptorMap,
                                         decoratorSet,
                                         isExcludeClassInterceptors);
    }
    else
      return super.create(method, isEnhanced);
  }
  
  private boolean hasAroundInvoke(Class<?> cl)
  {
    for (Method m : cl.getDeclaredMethods()) {
      if (m.isAnnotationPresent(AroundInvoke.class))
        return true;
    }
    
    return false;
  }
  
  private boolean isPostConstruct(Class<?> cl)
  {
    return isLifecycle(cl, PostConstruct.class);
  }
  
  private boolean isPreDestroy(Class<?> cl)
  {
    return isLifecycle(cl, PreDestroy.class);
  }
  
  private boolean isLifecycle(Class<?> cl, 
                              Class<? extends Annotation> annType)
  {
    for (Method m : cl.getDeclaredMethods()) {
      if (m.isAnnotationPresent(annType)) {
        Class<?> []param = m.getParameterTypes();
        
        if (param.length == 1 && param[0].equals(InvocationContext.class))
          return true;
      }
    }
    
    return false;
  }
  
  @Override
  public void generateInject(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generateInject(out, map);
    
    if (_classPostConstructInterceptors.size() > 0) {
      InterceptorGenerator<X> gen 
        = new InterceptorGenerator<X>(this,
                                      _classPostConstructInterceptors,
                                      InterceptionType.POST_CONSTRUCT);
 
      gen.generateInject(out, map);
    }
    
    if (_classPreDestroyInterceptors.size() > 0) {
      InterceptorGenerator<X> gen 
        = new InterceptorGenerator<X>(this,
                                      _classPreDestroyInterceptors,
                                      InterceptionType.PRE_DESTROY);
 
      gen.generateInject(out, map);
    }
  }
  
  @Override
  public void generatePostConstruct(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generatePostConstruct(out, map);
    
    if (isEnhanced()) {
      InterceptorGenerator<X> gen =
        new InterceptorGenerator<X>(this, _classPostConstructInterceptors,
                                    InterceptionType.POST_CONSTRUCT);
 
      gen.generateClassPostConstruct(out, map);
    }
  }
  
  @Override
  public void generatePreDestroy(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generatePreDestroy(out, map);
    
    if (isEnhanced()) {
      InterceptorGenerator<X> gen =
        new InterceptorGenerator<X>(this, _classPreDestroyInterceptors,
                                    InterceptionType.PRE_DESTROY);
 
      gen.generateClassPreDestroy(out, map);
    }
  }
  
  @Override
  public void generateEpilogue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generateEpilogue(out, map);
    
    InterceptorGenerator<X> gen 
      = new InterceptorGenerator<X>(this, _classPostConstructInterceptors,
          InterceptionType.POST_CONSTRUCT);
 
    gen.generateEpilogue(out, map);
      
    gen = new InterceptorGenerator<X>(this, _classPreDestroyInterceptors,
                                      InterceptionType.PRE_DESTROY);
 
    gen.generateEpilogue(out, map);
      
    gen = new InterceptorGenerator<X>(this, _classAroundInvokeInterceptors,
                                      InterceptionType.AROUND_INVOKE);
 
    gen.generateEpilogue(out, map);
  }
  
  @Override
  public boolean isEnhanced()
  {
    if (_classPostConstructInterceptors.size() > 0)
      return true;
    else if (_classPreDestroyInterceptors.size() > 0)
      return true;
    else if (_classAroundInvokeInterceptors.size() > 0)
      return true;
    else if (_classInterceptorBindings != null)
      return true;
    else
      return super.isEnhanced();
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
    
    for (AnnotatedField<? super X> field : getBeanType().getFields()) {
      if (field.isAnnotationPresent(Delegate.class)) {
        _isInterceptorOrDecorator = true;
        return;
      }
    }
    
    for (AnnotatedMethod<? super X> method : getBeanType().getMethods()) {
      for (AnnotatedParameter<? super X> param : method.getParameters()) {
        if (param.isAnnotationPresent(Delegate.class)) {
          _isInterceptorOrDecorator = true;
          return;
        }
      }
    }
    
    for (Annotation ann : getBeanType().getAnnotations()) {
      if (_manager.isPassivatingScope(ann.annotationType())
          || Stateful.class.equals(ann.annotationType())) {
        _isPassivating = true;
      }
    }
    
    introspectClassInterceptors();
    introspectClassInterceptorBindings();
    introspectClassDecorators();
    
    if (_isPassivating)
      validatePassivating();
  }
  
  private void validatePassivating()
  {
    for (Class<?> cl : _classAroundInvokeInterceptors) {
      if (! Serializable.class.isAssignableFrom(cl))
        throw new ConfigException(L.l("{0} has an invalid interceptor {1} because it's not serializable.",
                                      getBeanType().getJavaClass().getName(),
                                      cl.getName()));        
    }
  }
  
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private void introspectClassInterceptors()
  {
    Interceptors interceptors = getBeanType().getAnnotation(Interceptors.class);

    if (interceptors == null)
      return;
    
    for (Class<?> iClass : interceptors.value()) {
      if (hasAroundInvoke(iClass)) {
        if (! _classAroundInvokeInterceptors.contains(iClass)) {
          _classAroundInvokeInterceptors.add(iClass);
        }
      }
        
      if (isPostConstruct(iClass)) {
        if (! _classPostConstructInterceptors.contains(iClass)) {
          _classPostConstructInterceptors.add(iClass);
        }
      }
      
      if (isPreDestroy(iClass)) {
        if (! _classPreDestroyInterceptors.contains(iClass)) {
          _classPreDestroyInterceptors.add(iClass);
        }
      }
    }
  }
  
  /**
   * Introspects the CDI interceptor bindings on the class
   */
  private void introspectClassInterceptorBindings()
  {
    /*
    for (Annotation ann : getBeanType().getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
        
      }
    }
    */
    
    _classInterceptorBindings
      = addInterceptorBindings(null, getBeanType().getAnnotations());
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
        
      Annotation oldAnn = interceptorMap.put(ann.annotationType(), ann);

      if (oldAnn != null && ! oldAnn.equals(ann))
        throw new ConfigException(L.l("duplicate @InterceptorBindings {0} and {1} are not allowed, because Resin can't tell which one to use.",
                                      ann, oldAnn));
    }

    if (annType.isAnnotationPresent(Stereotype.class)) {
      for (Annotation subAnn : annType.getAnnotations()) {
        interceptorMap = addInterceptorBindings(interceptorMap, subAnn);
      }
    }
    
    return interceptorMap;
  }

  public AnnotatedMethod<? super X> getAroundInvokeMethod()
  {
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
