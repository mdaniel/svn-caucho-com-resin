/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import org.aopalliance.intercept.MethodInterceptor;

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JMethod;

import com.caucho.config.MapBuilder;

import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.BaseMethod;

import com.caucho.loader.enhancer.MethodEnhancer;

/**
 * Configuration for a method-enhancer builder.
 */
public class AopMethodEnhancer implements MethodEnhancer {
  private static final Logger log =
    Logger.getLogger(AopMethodEnhancer.class.getName());
  
  private Class _annotation;
  private Class _type;

  private boolean _isStatic = true;

  /**
   * Sets the annotation.
   */
  public void setAnnotation(Class ann)
  {
    _annotation = ann;
  }

  /**
   * Gets the annotation.
   */
  public Class getAnnotation()
  {
    return _annotation;
  }
  
  /**
   * Sets the type of the method enhancer.
   */
  public void setType(Class type)
  {
    _type = type;
  }
  
  /**
   * Sets true if the interceptor is static.
   */
  public void setStatic(boolean isStatic)
  {
    _isStatic = isStatic;
  }

  /**
   * Enhances the method.
   *
   * @param genClass the generated class
   * @param jMethod the method to be enhanced
   * @param jAnn the annotation to be enhanced
   */
  public void enhance(GenClass genClass,
		      JMethod jMethod,
		      JAnnotation jAnn)
  {
    AopEnhancer aopEnhancer = AopEnhancer.create();

    aopEnhancer.addInterceptor(jMethod.getName(), this);
	  
    // XXX: hardwired
    AopVarComponent aopVar = new AopVarComponent(jMethod, jMethod);

    aopVar.setStatic(_isStatic);
    
    genClass.addComponent(aopVar);
    
    BaseMethod genMethod = genClass.createMethod(jMethod);

    genMethod.setCall(new AopCallChain(genMethod.getCall(), aopVar));
  }

  /**
   * Returns the invocation builder.
   */
  MethodInterceptorBuilder createBuilder(Method method)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    MethodInterceptor interceptor = null;

    try {
      if (_isStatic)
	thread.setContextClassLoader(method.getDeclaringClass().getClassLoader());
      
      interceptor = (MethodInterceptor) _type.newInstance();

      Annotation ann = method.getAnnotation(_annotation);

      if (ann != null) {
	HashMap<String,Object> props = new HashMap<String,Object>();

	for (Method annMethod : _annotation.getMethods()) {
	  if (annMethod.getDeclaringClass().equals(Object.class))
	    continue;
	  else if (annMethod.getDeclaringClass().equals(Annotation.class))
	    continue;
	  else if (annMethod.getParameterTypes().length != 0)
	    continue;

	  Object value = annMethod.invoke(ann);

	  if (value != null)
	    props.put(annMethod.getName(), value);
	}
	
	MapBuilder.configure(interceptor, props);
      }

      return new MethodInterceptorBuilder(interceptor);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
