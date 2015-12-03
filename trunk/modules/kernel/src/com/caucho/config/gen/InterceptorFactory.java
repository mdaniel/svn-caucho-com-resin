/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import static javax.enterprise.inject.spi.InterceptionType.AROUND_INVOKE;
import static javax.enterprise.inject.spi.InterceptionType.POST_CONSTRUCT;
import static javax.enterprise.inject.spi.InterceptionType.PRE_DESTROY;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.AnnotatedOverrideMap;
import com.caucho.config.inject.AnyLiteral;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeImpl;
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
  
  /*
  private ArrayList<Class<?>> _classInterceptors
    = new ArrayList<Class<?>>();
  
  private ArrayList<Class<?>> _defaultInterceptors
    = new ArrayList<Class<?>>();
  
  private ArrayList<Class<?>> _selfInterceptors
    = new ArrayList<Class<?>>();
    */
  
  private HashMap<AnnotatedType<?>, ClassInterceptors> _interceptorMap
    = new HashMap<AnnotatedType<?>, ClassInterceptors>();
  
  private ClassInterceptors _selfInterceptors;
  
  private HashMap<Class<?>, Annotation> _classInterceptorBindings;
  
  private HashSet<Class<?>> _decoratorClasses;
  
  private boolean _isExcludeClassInterceptors;
  private boolean _isExcludeDefaultInterceptors;
  
  private boolean _isPassivating;
  private boolean _isStateful;
  
  public InterceptorFactory(AspectBeanFactory<X> beanFactory,
                            AspectFactory<X> next,
                            InjectManager manager)
  {
    super(beanFactory, next);
    
    // ejb/0i5c
    if (manager.isChildManager())
      manager = manager.getParent();
    
    _manager = manager;

    introspectType();
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
  
  public boolean isStateful()
  {
    return _isStateful || getBeanType().isAnnotationPresent(Stateful.class);
  }
  
  public boolean isSelfInterceptor()
  {
    return _selfInterceptors.getSelfInterceptors() != null;
    // return _selfInterceptors.size() > 0;
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
    
    boolean isPrivate
      = Modifier.isPrivate(method.getJavaMember().getModifiers());
    
    boolean isLifecycle
      = method.isAnnotationPresent(PostConstruct.class);

    AnnotatedType<?> beanType;

    // XXX: interceptor/singleton/business/annotated
    // packaging/war/mbean/interceptor/lifecycle
    /*
    if (isPrivate)
      beanType = method.getDeclaringType();
    else
      beanType = getBeanType();
      */
    
    /*
    if (isLifecycle)
      beanType = method.getDeclaringType();
    else
      beanType = getBeanType();
      */
    
    beanType = getBeanType();

    // beanType = method.getDeclaringType();
    // beanType = getBeanType();
    
    boolean isExcludeClassInterceptors = false;
    
    if (method.isAnnotationPresent(ExcludeClassInterceptors.class)
        || beanType.isAnnotationPresent(ExcludeClassInterceptors.class)) {
      isExcludeClassInterceptors = true;
    }
    
    boolean isExcludeDefaultInterceptors = false;
    
    if (method.isAnnotationPresent(ExcludeDefaultInterceptors.class)
        || beanType.isAnnotationPresent(ExcludeDefaultInterceptors.class)) {
      isExcludeDefaultInterceptors = true;
    }
    
    HashSet<Class<?>> methodInterceptors = null;
    
    InterceptionType type = InterceptionType.AROUND_INVOKE;
    Class<? extends Annotation> annType = AroundInvoke.class;
    

    if (method.isAnnotationPresent(PostConstruct.class)){
      type = InterceptionType.POST_CONSTRUCT;
      annType = PostConstruct.class;
    }
    else if (method.isAnnotationPresent(PreDestroy.class)){
      type = InterceptionType.PRE_DESTROY;
      annType = PreDestroy.class;
    }
    
    // ejb/0cb3, ejb/1060
    ClassInterceptors typeInterceptors = getTypeInterceptors(beanType);
    //ClassInterceptors typeInterceptors = getTypeInterceptors(method.getDeclaringType());
    
    if (! isExcludeDefaultInterceptors) {
      methodInterceptors = addInterceptors(methodInterceptors,
                                           typeInterceptors.getDefaultInterceptors(),
                                           annType);
    }

    if (! isExcludeClassInterceptors) {
      methodInterceptors = addInterceptors(methodInterceptors,
                                           typeInterceptors.getClassInterceptors(),
                                           annType);
    }
    
    Interceptors interceptorsAnn = method.getAnnotation(Interceptors.class);

    boolean isMethodInterceptor = false;
    
    if (interceptorsAnn != null) {
      for (Class<?> iClass : interceptorsAnn.value()) {
        if (! hasAroundInvoke(iClass)) {
          continue;
        }
        
        isMethodInterceptor = true;
        
        if (methodInterceptors == null)
          methodInterceptors = new LinkedHashSet<Class<?>>();
        
        methodInterceptors.add(iClass);
      }
    }

    methodInterceptors = addInterceptors(methodInterceptors,
                                         typeInterceptors.getSelfInterceptors(),
                                         annType);

    HashMap<Class<?>, Annotation> interceptorMap = null;
    
    interceptorMap = addInterceptorBindings(interceptorMap, 
                                            method.getAnnotations());
    
    HashSet<Class<?>> decoratorSet = introspectDecorators(method);
    
    if (method.isAnnotationPresent(Inject.class)
        || method.isAnnotationPresent(PostConstruct.class)) {
      if (isMethodInterceptor || interceptorMap != null) {
        throw new ConfigException(L.l("{0}.{1} is invalid because it's annotated with @Inject or @PostConstruct but also has interceptor bindings",
                                      method.getJavaMember().getDeclaringClass().getName(),
                                      method.getJavaMember().getName()));
      }
    }
    
    if (method.isAnnotationPresent(Inject.class)) {
      // ioc/0c57, ejb/60b0
      return super.create(method, isEnhanced);
    }

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
        // || _selfInterceptors.size() > 0
        || decoratorSet != null) {
      AspectGenerator<X> next = super.create(method, true);

      return new InterceptorGenerator<X>(this, method, next,
                                         type,
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
    if (cl == null)
      return false;
    
    for (Method m : cl.getDeclaredMethods()) {
      if (m.isAnnotationPresent(AroundInvoke.class))
        return true;
    }
    
    return hasAroundInvoke(cl.getSuperclass());
  }
  
  @Override
  public void generateInject(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generateInject(out, map);
    
    if (_isInterceptorOrDecorator)
      return;
    
    if (isEnhanced()) {
      generateInject(out, map, POST_CONSTRUCT, PostConstruct.class);
      generateInject(out, map, AROUND_INVOKE, AroundInvoke.class);
      generateInject(out, map, PRE_DESTROY, PreDestroy.class);
    }
  }
  
  private void generateInject(JavaWriter out, 
                              HashMap<String,Object> map,
                              InterceptionType type,
                              Class<? extends Annotation> annType)
    throws IOException
  {
    HashSet<Class<?>> interceptors = null;
    
    interceptors = addInterceptors(interceptors,
                                   _selfInterceptors.getClassInterceptors(), 
                                   annType);
    interceptors = addInterceptors(interceptors,
                                   _selfInterceptors.getDefaultInterceptors(), 
                                   annType);
    interceptors = addInterceptors(interceptors, 
                                   _selfInterceptors.getSelfInterceptors(),
                                   annType);

    if (interceptors != null) {
      InterceptorGenerator<X> gen 
        = new InterceptorGenerator<X>(this, interceptors, type);
 
      gen.generateInject(out, map);
    }
  }
  
  @Override
  public void generatePostConstruct(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    if (! _isInterceptorOrDecorator && isEnhanced()) {
      HashSet<Class<?>> set = null;

      /*
      set = addInterceptors(set,
                            _selfInterceptors.getClassInterceptors(),
                            PostConstruct.class);
      set = addInterceptors(set,
                            _selfInterceptors.getDefaultInterceptors(),
                            PostConstruct.class);
                            */
      set = addInterceptors(set,
                            _selfInterceptors.getClassInterceptors(),
                            AroundInvoke.class);
      set = addInterceptors(set,
                            _selfInterceptors.getDefaultInterceptors(),
                            AroundInvoke.class);

      if (set != null) {
        /*
        InterceptorGenerator<X> gen
          = new InterceptorGenerator<X>(this,
                                        set,
                                        InterceptionType.POST_CONSTRUCT);
                                        */
        InterceptorGenerator<X> gen
        = new InterceptorGenerator<X>(this,
                                      set,
                                      InterceptionType.AROUND_INVOKE);
        
        gen.generateClassPostConstruct(out, map);
        
        return;
      }
    }
    
    super.generatePostConstruct(out, map);
  }
  
  @Override
  public void generatePreDestroy(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generatePreDestroy(out, map);
    
    if (_isInterceptorOrDecorator)
      return;
    
    if (isEnhanced()) {
      HashSet<Class<?>> set = null;
      
      set = addInterceptors(set,
                            _selfInterceptors.getClassInterceptors(),
                            PreDestroy.class);

      // ioc/055b
      if (set != null || _classInterceptorBindings != null) {
        InterceptorGenerator<X> gen
          = new InterceptorGenerator<X>(this,
                                        set,
                                        InterceptionType.PRE_DESTROY);
        
        gen.generateClassPreDestroy(out, map);
      }
    }
  }
  
  @Override
  public void generateEpilogue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generateEpilogue(out, map);
    
    if (_isInterceptorOrDecorator)
      return;
    
    if (isEnhanced()) {
      //generateEpilogue(out, map, POST_CONSTRUCT, PostConstruct.class);
      generateEpilogue(out, map, AROUND_INVOKE, AroundInvoke.class);
      generateEpilogue(out, map, PRE_DESTROY, PreDestroy.class);
    }
  }
  
  private void generateEpilogue(JavaWriter out, 
                                HashMap<String,Object> map,
                                InterceptionType type,
                                Class<? extends Annotation> annType)
    throws IOException
  {
    HashSet<Class<?>> interceptors = new LinkedHashSet<Class<?>>();
    
    interceptors = addInterceptors(interceptors, 
                                   _selfInterceptors.getClassInterceptors(), 
                                   annType);
    interceptors = addInterceptors(interceptors, 
                                   _selfInterceptors.getDefaultInterceptors(), 
                                   annType);
    interceptors = addInterceptors(interceptors,
                                   _selfInterceptors.getSelfInterceptors(),
                                   annType);
    
    InterceptorGenerator<X> gen 
      = new InterceptorGenerator<X>(this, interceptors, type);
 
    gen.generateEpilogue(out, map);
  }
  
  @Override
  public boolean isEnhanced()
  {
    if (_isInterceptorOrDecorator)
      return false;
    else if (_selfInterceptors.getClassInterceptors() != null)
      return true;
    else if (_selfInterceptors.getSelfInterceptors() != null)
      return true;
    else if (_classInterceptorBindings != null)
      return true;
    else
      return super.isEnhanced();
  }
  
  private boolean isInterceptorPresentRec(Class<?> cl)
  {
    if (cl == null)
      return false;
    else if (isInterceptorPresent(cl))
      return true;
    else
      return isInterceptorPresentRec(cl.getSuperclass());
  }
  
  private boolean isInterceptorPresent(Class<?> cl)
  {
    for (Method m : cl.getDeclaredMethods()) {
      Class<?> []param = m.getParameterTypes();
      
      if (param.length == 1 && param[0].equals(InvocationContext.class))
        return true;
      
      // if (m.isAnnotationPresent(annType))
    }
    
    return false;
  }
  
  private HashSet<Class<?>> 
  addInterceptors(HashSet<Class<?>> set,
                  ArrayList<Class<?>> sourceList,
                 Class<? extends Annotation> annType)
  {
    if (sourceList == null)
      return set;
    
    for (Class<?> cl : sourceList) {
      set = addInterceptor(set, cl, cl, annType);
    }
    
    return set;
  }
  
  private HashSet<Class<?>> 
  addInterceptor(HashSet<Class<?>> set,
                 Class<?> cl,
                 Class<?> subClass,
                 Class<? extends Annotation> annType)
  {
    if (subClass == null || subClass == Object.class)
      return set;
    
    for (Method m : subClass.getDeclaredMethods()) {
      if (Modifier.isAbstract(m.getModifiers()))
        continue;
      
      Class<?> []param = m.getParameterTypes();
      
      if (param.length != 1 || ! param[0].equals(InvocationContext.class))
        continue;
        
      if (! isAnnotationPresent(m, annType))
        continue;
        
      if (set == null)
        set = new LinkedHashSet<Class<?>>();
          
      set.add(cl);
        
      return set;
    }
    
    return addInterceptor(set, cl, subClass.getSuperclass(), annType);
  }
  
  private boolean isAnnotationPresent(Method m, 
                                      Class<? extends Annotation> annType)
  {
    if (m.isAnnotationPresent(annType))
      return true;
    
    AnnotatedMethod<?> annMethod = AnnotatedOverrideMap.getMethod(m);
    
    if (annMethod == null)
      return false;
    else
      return annMethod.isAnnotationPresent(annType);
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
      if (_manager.isPassivatingScope(ann.annotationType())) {
        _isPassivating = true;
      }
      else if (Stateful.class.equals(ann.annotationType())) {
        _isStateful = true;
        // ioc/05as, ejb/5019
        // _isPassivating = true;
      }
    }
    
    _selfInterceptors = getTypeInterceptors(getBeanType());

    introspectClassInterceptorBindings();
    introspectClassDecorators();
    
    if (isPassivating())
      validatePassivating();
  }
  
  private void validatePassivating()
  {
    /*
    for (Class<?> cl : _classInterceptors) {
      if (! Serializable.class.isAssignableFrom(cl) && false) {
        throw new ConfigException(L.l("{0} has an invalid interceptor {1} because it's not serializable.",
                                      getBeanType().getJavaClass().getName(),
                                      cl.getName()));
      }
    }
    */
  }
  
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private ClassInterceptors
  getTypeInterceptors(AnnotatedType<?> beanType)
  {
    ClassInterceptors interceptors = _interceptorMap.get(beanType);
    
    if (interceptors == null) {
      interceptors = introspectTypeInterceptors(beanType);
      
      _interceptorMap.put(beanType, interceptors);
    }
    
    return interceptors;
  }
    
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private ClassInterceptors
  introspectTypeInterceptors(AnnotatedType<?> beanType)
  {
    ArrayList<Class<?>> selfInterceptors
      = introspectSelfInterceptors(beanType);
    
    ArrayList<Class<?>> classInterceptors
      = introspectClassInterceptors(beanType);
    
    ArrayList<Class<?>> defaultInterceptors
      = introspectDefaultInterceptors(beanType);
    
    ClassInterceptors interceptors
      = new ClassInterceptors(classInterceptors,
                              defaultInterceptors,
                              selfInterceptors);
    
    return interceptors;
  }
  
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private ArrayList<Class<?>> 
  introspectClassInterceptors(AnnotatedType<?> beanType)
  {
    if (beanType == null)
      return null;
    
    ArrayList<Class<?>> classInterceptors = null;
    
    if (beanType.isAnnotationPresent(ExcludeClassInterceptors.class)) {
      // _isExcludeClassInterceptors = true;
      return classInterceptors;
    }
    
    Interceptors interceptors = beanType.getAnnotation(Interceptors.class);

    if (interceptors != null) {
      classInterceptors = new ArrayList<Class<?>>();
      for (Class<?> iClass : interceptors.value()) {
        if (! classInterceptors.contains(iClass))
          classInterceptors.add(iClass);
      }
    }
    
    return classInterceptors;
  }
  
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private ArrayList<Class<?>> 
  introspectSelfInterceptors(AnnotatedType<?> beanType)
  {
    ArrayList<Class<?>> selfInterceptors = null;
    
    if (isInterceptorPresentRec(beanType.getJavaClass())) {
      selfInterceptors = new ArrayList<Class<?>>();
      
      selfInterceptors.add(beanType.getJavaClass());
    }
    
    return selfInterceptors;
  }
  
  /**
   * Introspects the @Interceptors annotation on the class
   */
  private ArrayList<Class<?>>
  introspectDefaultInterceptors(AnnotatedType<?> beanType)
  {
    ArrayList<Class<?>> defaultInterceptors = null;
    
    if (beanType.isAnnotationPresent(ExcludeDefaultInterceptors.class)) {
      // _isExcludeDefaultInterceptors = true;
      return defaultInterceptors;
    }
    
    DefaultInterceptors interceptors 
      = beanType.getAnnotation(DefaultInterceptors.class);
    if (interceptors != null) {
      defaultInterceptors = new ArrayList<Class<?>>();
      for (Class<?> iClass : interceptors.value()) {
        if (! defaultInterceptors.contains(iClass)) {
          defaultInterceptors.add(iClass);
        }
      }
    }
    
    return defaultInterceptors;
  }
  
  private void introspectClassInterceptors(ArrayList<Class<?>> list,
                                           Class<?> iClass)
  {
    if (isInterceptorPresent(iClass)) {
      if (! list.contains(iClass)) {
        list.add(iClass);
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
    
    if (isPassivating() || isStateful()) {
      // ioc/0i5e
      CandiUtil.validatePassivatingDecorators(getBeanType().getJavaClass(), decorators);
    }
    
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
  
  static class ClassInterceptors {
    private ArrayList<Class<?>> _classInterceptors
      = new ArrayList<Class<?>>();
    
    private ArrayList<Class<?>> _defaultInterceptors
      = new ArrayList<Class<?>>();
    
    private ArrayList<Class<?>> _selfInterceptors
      = new ArrayList<Class<?>>();
    
    ClassInterceptors(ArrayList<Class<?>> classInterceptors,
                      ArrayList<Class<?>> defaultInterceptors,
                      ArrayList<Class<?>> selfInterceptors)
    {
      _classInterceptors = classInterceptors;
      _defaultInterceptors = defaultInterceptors;
      _selfInterceptors = selfInterceptors;
    }
    
    public ArrayList<Class<?>> getClassInterceptors()
    {
      return _classInterceptors;
    }
    
    public ArrayList<Class<?>> getDefaultInterceptors()
    {
      return _defaultInterceptors;
    }
    
    public ArrayList<Class<?>> getSelfInterceptors()
    {
      return _selfInterceptors;
    }
  }
}
