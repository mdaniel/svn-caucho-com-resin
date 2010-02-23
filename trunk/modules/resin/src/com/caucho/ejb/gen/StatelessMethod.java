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

package com.caucho.ejb.gen;

import java.io.IOException;

import com.caucho.config.gen.ApiClass;
import com.caucho.config.gen.ApiMethod;
import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.java.JavaWriter;

/**
 * Represents a stateless local business method
 */
public class StatelessMethod extends BusinessMethodGenerator
{
  private String _beanClassName;
  
  public StatelessMethod(ApiClass ejbClass,
                              String beanClassName,
                              StatelessView view,
                              ApiMethod apiMethod,
                              ApiMethod implMethod,
                              int index)
  {
    super(view, apiMethod, implMethod, index);

    _beanClassName = beanClassName;
  }
  
  /*
  @Override
  protected EjbCallChain createTailCallChain()
  {
    return new MethodTailCallChain(this);
  }
  */

  /**
   * Session bean default is REQUIRED
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    // getXa().setTransactionType(TransactionAttributeType.REQUIRED);

    super.introspect(apiMethod, implMethod);
  }

  /**
   * Returns true if any interceptors enhance the business method
   */
  @Override
  public boolean isEnhanced()
  {
    return true;
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
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");

    super.generatePreTry(out);

    out.println();
    out.println("boolean isValid = false;");
    
    // bean allocation must be last because it needs to be
    // freed or discarded in the finally block
    out.println(_beanClassName + " bean = _statelessPool.allocate();");
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
    out.println("thread.setContextClassLoader(_context.getStatelessManager().getClassLoader());");
    
    super.generatePreCall(out);
  }
  
  /**
   * Generates code for the invocation itself.
   */
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    super.generateCall(out);
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
    
    out.println();
    out.println("isValid = true;");
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
    out.println("if (isValid)");
    out.println("  _statelessPool.free(bean);");
    out.println("else");
    out.println("  _statelessPool.discard(bean);");
     
    super.generateFinally(out);
    
    out.println();
    out.println("thread.setContextClassLoader(oldLoader);");
  }
  
  //
  // lifecycle override code
  //
   
  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("bean");
  }
  
  /**
   * Generates the underlying bean instance
   */
  @Override
  protected String getSuper()
  {
    return "bean";
  }
  
  /*
  // XXX: move to InterceptorCallChain
  @Override
  public void generateInterceptorTarget(JavaWriter out) throws IOException
  {
  }
  */
}
