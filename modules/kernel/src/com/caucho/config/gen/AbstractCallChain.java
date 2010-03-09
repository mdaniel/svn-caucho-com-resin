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
import java.util.HashMap;

import com.caucho.java.JavaWriter;

/**
 * Represents a filter for invoking a method
 */
abstract public class AbstractCallChain implements EjbCallChain {
  private EjbCallChain _next;

  AbstractCallChain(EjbCallChain next)
  {
    if (next == null)
      throw new NullPointerException();

    _next = next;
  }
  
  //
  // introspection methods
  //

  /**
   * Returns true if this filter will generate code.
   */
  @Override
  abstract public boolean isEnhanced();

  /**
   * Introspects the method for the default values
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
  }
  
  //
  // bean instance interception
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateBeanPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    _next.generateBeanPrologue(out, map);
  }

  /**
   * Generates initialization in the constructor
   */
  @Override
  public void generateBeanConstructor(JavaWriter out, 
                                      HashMap<String,Object> map)
    throws IOException
  {
    _next.generateBeanConstructor(out, map);
  }

  /**
   * Generates @PostConstruct code
   */
  @Override
  public void generatePostConstruct(JavaWriter out, 
                                    HashMap<String,Object> map)
    throws IOException
  {
    _next.generatePostConstruct(out, map);
  }
  
  //
  // business method interception
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    _next.generateMethodPrologue(out, map);
  }
  
  /**
   * Generates pre-async dispatch code.
   */
  public void generateAsync(JavaWriter out)
    throws IOException
  {
    _next.generateAsync(out);
  }  
  
  /**
   * Generates code before the try block
   */
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
    _next.generatePreTry(out);
  }  
  
  /**
   * Generates code before the call, in the try block.
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     [pre-call]
   *     value = bean.myMethod(...);
   *     ...
   * }
   * </pre></code>
   */
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    _next.generatePreCall(out);
  }

  /**
   * Generates the method interception code
   */
  public void generateCall(JavaWriter out) 
    throws IOException
  {
    _next.generateCall(out);
  }
  
  /**
   * Generates code after the call, before the return.
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     ...
   *     value = bean.myMethod(...);
   *     [post-call]
   *     return value;
   *   } finally {
   *     ...
   *   }
   * }
   * </pre></code>
   */
  public void generatePostCall(JavaWriter out)
    throws IOException
  {
    _next.generatePostCall(out);
  }
  
  /**
   * Generates application (checked) exception code for
   * the method.
   */
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
    _next.generateApplicationException(out, exn);
  }
  
  /**
   * Generates system (runtime) exception code for
   * the method.
   */
  @Override
  public void generateSystemException(JavaWriter out,
                                      Class<?> exn)
    throws IOException
  {
    _next.generateSystemException(out, exn);
  }
  
  /**
   * Generates finally code for the method
   */
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    _next.generateFinally(out);
  }

  protected <T extends Annotation> T getAnnotation(Class<T> annotationType,
                                                   ApiMethod apiMethod, 
                                                   ApiMethod implMethod)
  {
    Annotation annotation;

    annotation = apiMethod.getAnnotation(annotationType);

    if ((annotation == null) && (implMethod != null)) {
      annotation = implMethod.getAnnotation(annotationType);
    }

    return (T) annotation;
  }
  
  protected <T extends Annotation> T getAnnotation(Class<T> annotationType,
                                                   ApiClass apiClass,
                                                   ApiClass implClass)
  {
    Annotation annotation;

    annotation = apiClass.getAnnotation(annotationType);
  
    if ((annotation == null) && (implClass != null)) {
      annotation = implClass.getAnnotation(annotationType);
    }

    return (T) annotation;    
  }
  
  protected <T extends Annotation> T getAnnotation(Class<T> annotationType,
                                                   ApiMethod apiMethod, 
                                                   ApiClass apiClass,
                                                   ApiMethod implementationMethod, 
                                                   ApiClass implementationClass) 
  {
    Annotation annotation;

    annotation = apiMethod.getAnnotation(annotationType);

    if (annotation == null) {
      annotation = apiClass.getAnnotation(annotationType);
    }

    if ((annotation == null) && (implementationMethod != null)) {
      annotation = implementationMethod.getAnnotation(annotationType);
    }

    if ((annotation == null) && (implementationClass != null)) {
      annotation = implementationClass.getAnnotation(annotationType);
    }

    return (T) annotation;
  }
}