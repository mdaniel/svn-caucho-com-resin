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
import java.util.HashMap;

import com.caucho.java.JavaWriter;

/**
 * Represents a filter for invoking a method
 */
public class NullCallChain implements EjbCallChain {
  NullCallChain()
  {
  }
  
  //
  // introspection
  //

  /**
   * Returns true if this filter will generate code.
   */
  @Override
  public boolean isEnhanced()
  {
    return false;
  }

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
   * Generates the bean instance class prologue
   */
  @Override
  public void generateBeanPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates bean instance interception
   */
  @Override
  public void generateBeanConstructor(JavaWriter out,
                                      HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates bean post construct interception
   */
  @Override
  public void generatePostConstruct(JavaWriter out,
                                    HashMap<String,Object> map)
    throws IOException
  {
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
  }

  @Override
  public void generateAsync(JavaWriter out)
    throws IOException
  {
  }
  
  /**
   * Generates code before the try block
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
  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the method interception code
   */
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
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
  @Override
  public void generatePostCall(JavaWriter out)
    throws IOException
  {
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
  }

  /**
   * Generates finally code for the method
   */
  public void generateFinally(JavaWriter out)
    throws IOException
  {
  }
}