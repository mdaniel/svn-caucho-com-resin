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

package com.caucho.aop;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;

import java.util.HashMap;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.GenClass;

import com.caucho.loader.EnvironmentLocal;

/**
 * Enhancing a method objects.
 */
public class AopEnhancer {
  private static final L10N L = new L10N(AopEnhancer.class);
  private static final Logger log = Logger.getLogger(AopEnhancer.class.getName());

  private static EnvironmentLocal<AopEnhancer> _localEnhancer =
    new EnvironmentLocal<AopEnhancer>();
  
  private HashMap<String,Class> _methodInterceptorMap =
    new HashMap<String,Class>();
  
  private HashMap<String,AopMethodEnhancer> _methodMap =
    new HashMap<String,AopMethodEnhancer>();

  private AopEnhancer()
  {
  }

  public static AopEnhancer create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }
    
  public static AopEnhancer create(ClassLoader loader)
  {
    AopEnhancer enhancer = _localEnhancer.getLevel(loader);

    if (enhancer == null) {
      enhancer = new AopEnhancer();
      _localEnhancer.set(enhancer, loader);
    }

    return enhancer;
  }
  
  public void addMethod(MethodItem item)
  {
    _methodInterceptorMap.put(item.getAnnotation().getName(),
			      item.getType());
  }
  
  /**
   * Enhances a method.
   */
  public void enhance(Method method, GenClass genClass)
  {
    /*
    if (shouldEnhance(method)) {
      AopMethod aopMethod = (AopMethod) javaClass.findMethod(method);
      if (aopMethod == null) {
	aopMethod = new AopMethod(method, method);
	javaClass.addMethod(aopMethod);
      }
      
      for (Annotation ann :  method.getDeclaredAnnotations()) {
	String annName = ann.annotationType().getName();

	Class interceptorCl = _methodInterceptorMap.get(annName);

	if (interceptorCl != null) {
	  try {
	    MethodInterceptor interceptor = (MethodInterceptor) interceptorCl.newInstance();
	    _methodMap.put(method.getName(), new MethodInterceptorBuilder(interceptor));
	  } catch (Throwable e) {
	    log.log(Level.WARNING, e.toString(), e);
	  }
	}
      }

      aopMethod.setCall(enhance(method, aopMethod.getCall()));
    }
    */
  }

  static MethodInterceptorBuilder getBuilder(Method method)
  {
    AopEnhancer aopEnhancer = _localEnhancer.get();

    AopMethodEnhancer methodEnhancer;
    methodEnhancer = aopEnhancer._methodMap.get(method.getName());

    if (methodEnhancer == null)
      return new MethodInterceptorBuilder(new IdentityMethodInterceptor());
    
    return methodEnhancer.createBuilder(method);
  }

  /*
  static void addBuilder(String methodName, MethodInterceptorBuilder builder)
  {
    AopEnhancer aopEnhancer = _localEnhancer.get();

    aopEnhancer._methodMap.put(methodName, builder);
  }
  */

  public static void addInterceptor(String methodName,
				    AopMethodEnhancer enhancer)
  {
    AopEnhancer aopEnhancer = _localEnhancer.get();

    aopEnhancer._methodMap.put(methodName, enhancer);
  }

  /**
   * Returns true if the transaction should be enhanced.
   */
  public boolean shouldEnhance(Method method)
  {
    for (Annotation ann :  method.getDeclaredAnnotations()) {
      String annName = ann.annotationType().getName();

      Class interceptor = _methodInterceptorMap.get(annName);

      if (interceptor != null) {
	return true;
      }
    }
    
    return false;
  }
  
  /**
   * Enhances a method.
   */
  public CallChain enhance(Method method, CallChain call)
  {
    // return new AopCallChain(call);
    return null;
  }

  public static class MethodItem {
    private Class _annotation;
    private Class _type;

    /**
     * Sets the annotation class.
     */
    public void setAnnotation(Class annotation)
    {
      _annotation = annotation;
    }

    /**
     * Gets the annotation class.
     */
    public Class getAnnotation()
    {
      return _annotation;
    }

    /**
     * Returns the type.
     */
    public void setType(Class type)
    {
      _type = type;
    }

    /**
     * Returns the type.
     */
    public Class getType()
    {
      return _type;
    }
  }
}
