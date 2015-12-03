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

package com.caucho.ejb.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.MethodHeadGenerator;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a stateless local business method
 */
@Module
public class StatelessMethodHeadGenerator<X> extends MethodHeadGenerator<X>
{
  public StatelessMethodHeadGenerator(StatelessMethodHeadFactory<X> factory,
                                      AnnotatedMethod<? super X> method,
                                      AspectGenerator<X> next)
  {
    super(factory, method, next);
  }

  @Override
  protected boolean isOverride()
  {
    return false;
  }

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    map.put("__caucho_interceptor_beans", true);
    
    super.generateMethodPrologue(out, map);
  }
 
  /**
   * Generates code before the "try" block
   * <code><pre>
   * retType myMethod(...)
   * {
   *   [pre-try]
   *   try {
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
//    out.println("Thread thread = Thread.currentThread();");
//    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
//
    super.generatePreTry(out);
//
//    out.println();
//    out.println("boolean isValid = false;");
//    
//    // bean allocation must be last because it needs to be
//    // freed or discarded in the finally block
//    out.println("StatelessPool.Item<" + getJavaClass().getName() + "> poolItem");
//    out.println("  = _statelessPool.allocate();");
//    out.println(getJavaClass().getName() + " bean = poolItem.getValue();");
  }

  /**
   * Generates code in the "try" block before the call
   * <code><pre>
   * retType myMethod(...)
   * {
   *   ...
   *   try {
   *     [pre-call]
   *     ret = super.myMethod(...)
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    //out.println("thread.setContextClassLoader(_manager.getClassLoader());");
    
    super.generatePreCall(out);
  }
  
  /**
   * Generates aspect code after the invocation.
   * <code><pre>
   * retType myMethod(...)
   * {
   *   ...
   *   try {
   *     ...
   *     ret = super.myMethod(...)
   *     [post-call]
   *     return ret;
   *   } finally {
   *     ...
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generatePostCall(JavaWriter out)
    throws IOException
  {
    super.generatePostCall(out);
//    
//    out.println();
//    out.println("isValid = true;");
  }
  
  /**
   * Generates code for an application (checked) exception.
   */
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
    out.println("isValid = true;");
    
    super.generateApplicationException(out, exn);
  }

  /**
   * Generates the code in the finally block
   * <code><pre>
   * myRet myMethod(...)
   * {
   *   try {
   *     ...
   *   } finally {
   *     [finally]
   *   }
   * </pre></code>
   */
  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    // free/discard must be first in case of exceptions
    // in the other finally methods
    // XXX: (possibly free semaphore first and allow bean at
    // end, since it's the semaphore that's critical
//    out.println("if (isValid)");
//    out.println("  _statelessPool.free(poolItem);");
//    out.println("else");
//    out.println("  _statelessPool.discard(poolItem);");
//     
    super.generateFinally(out);
//    
//    out.println();
//    out.println("thread.setContextClassLoader(oldLoader);");
  }
}
