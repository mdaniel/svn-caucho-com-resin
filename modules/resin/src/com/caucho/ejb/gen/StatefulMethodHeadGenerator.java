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
import java.util.concurrent.TimeUnit;

import javax.ejb.Remove;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.MethodHeadGenerator;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a stateful local business method
 */
@Module
public class StatefulMethodHeadGenerator<X> extends MethodHeadGenerator<X> {
  private long _lockTimeout;
  private TimeUnit _lockTimeoutUnit;

  public StatefulMethodHeadGenerator(StatefulMethodHeadFactory<X> factory,
                                     AnnotatedMethod<? super X> method, 
                                     AspectGenerator<X> next)
  {
    super(factory, method, next);

    _lockTimeout = -1;
    _lockTimeoutUnit = null;
  }

  public StatefulMethodHeadGenerator(StatefulMethodHeadFactory<X> factory,
                                     AnnotatedMethod<? super X> method,
                                     AspectGenerator<X> next,
                                     long lockTimeout, 
                                     TimeUnit lockTimeoutUnit)
  {
    super(factory, method, next);

    if (lockTimeoutUnit != null) {
      _lockTimeout = lockTimeout;
      _lockTimeoutUnit = lockTimeoutUnit;
    } else {
      _lockTimeout = -1;
      _lockTimeoutUnit = null;
    }
  }

  protected TransactionAttributeType getDefaultTransactionType()
  {
    return TransactionAttributeType.REQUIRED;
  }

  /**
   * Generates the class prologue.
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, 
                                     HashMap<String, Object> map)
      throws IOException
  {
    if (map.get("caucho.ejb.semaphore") == null) {
      map.put("caucho.ejb.semaphore", "done");

      out.println();
      out.println("private transient final java.util.concurrent.Semaphore _semaphore = new java.util.concurrent.Semaphore(1);");
    }

    super.generateMethodPrologue(out, map);
  }

  @Override
  public void generatePreTry(JavaWriter out) throws IOException
  {
    super.generatePreTry(out);

    String beanClassName = getJavaClass().getName();

    out.println(beanClassName + " bean = _bean;");

    out.println("__caucho_validate();");
    /*
    out.println();
    out.println("if (bean == null)");
    out.println("  throw new javax.ejb.NoSuchEJBException(\"Stateful instance "
        + getJavaClass().getSimpleName() + " is no longer valid\");");
        */

    // Implicit semaphore on stateful beans.
    out.println();

    if (_lockTimeout != -1) {
      out.println("try {");
      out.pushDepth();
      out.println("if (!_semaphore.tryAcquire("
          + _lockTimeoutUnit.toMillis(_lockTimeout)
          + "L, java.util.concurrent.TimeUnit.MILLISECONDS))");
      out.pushDepth();
      out.println("throw new ConcurrentAccessTimeoutException(\"Timed out acquiring semaphore " + _lockTimeout + " ms.\");");
      out.popDepth();
      out.println("} catch (InterruptedException e) {");
      out.pushDepth();
      out.println("throw new ConcurrentAccessTimeoutException(\"Thread interruption acquiring semaphore: \" + e.getMessage());");
      out.popDepth();
      out.println("}");
    } else {
      out.println("try {");
      out.pushDepth();
      out.println("_semaphore.acquire();");
      out.popDepth();
      out.println("} catch (InterruptedException e) {");
      out.pushDepth();
      out.println("throw new ConcurrentAccessTimeoutException(\"Thread interruption acquiring semaphore: \" + e.getMessage());");
      out.popDepth();
      out.println("}");
    }

    out.println("boolean isValid = false;");

    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
  }

  @Override
  public void generatePreCall(JavaWriter out) throws IOException
  {
    out.println("thread.setContextClassLoader(_manager.getClassLoader());");
    out.println("_context.startLocal(this);");

    super.generatePreCall(out);
  }
  
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    super.generateCall(out);

    /*
    if (getMethod().isAnnotationPresent(Remove.class)) {
      out.println("__caucho_destroy(null);");
    }
    */
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  public void generatePostCall(JavaWriter out) throws IOException
  {
    out.println("isValid = true;");
    super.generatePostCall(out);
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  public void generateApplicationException(JavaWriter out, Class<?> exn)
      throws IOException
  {
    // ejb/5070
    super.generateApplicationException(out, exn);

    // ejb/5064
    if (getMethod().getAnnotation(Remove.class) == null) {
      out.println("isValid = true;");
    }
  }
  
  @Override
  public void generateFinally(JavaWriter out) throws IOException
  {
    // Implicit semaphore on stateful beans.
    out.println();
    out.println("_semaphore.release();");

    out.println("_context.endLocal(null);");
    out.println("boolean isValidFinally = false;");
    out.println("try {");
    out.pushDepth();
    super.generateFinally(out);
    out.println("isValidFinally = isValid;");
    out.popDepth();
    out.println("} finally {");
    out.pushDepth();
    
    Remove remove = getMethod().getAnnotation(Remove.class);

    if (remove != null) {
      boolean isRetainIfException= remove.retainIfException();
    
      if (isRetainIfException) {
        out.println("if (isValid)");
        out.println("  __caucho_destroy(null);");
      }
      else {
        out.println("  __caucho_destroy(null);");
      }
    }
    else {
      out.println("if (! isValidFinally) {");
      out.println("  __caucho_destroy(null);");
      out.println("}");
    }

    out.println("thread.setContextClassLoader(oldLoader);");
    out.popDepth();
    out.println("}");
  }
}
