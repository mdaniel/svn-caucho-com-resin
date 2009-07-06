/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Reza Rahman
 */
package com.caucho.config.gen;

import static javax.ejb.ConcurrencyManagementType.CONTAINER;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.Lock;
import javax.ejb.LockType;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents EJB lock type specification interception. The specification gears
 * it towards EJB singletons, but it can be used for other bean types.
 */
public class LockCallChain extends AbstractCallChain {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(LockCallChain.class);

  private EjbCallChain _next;

  private boolean _isContainerManaged;
  private LockType _lockType;
  private long _lockTimeout;
  private TimeUnit _lockTimeoutUnit;

  public LockCallChain(BusinessMethodGenerator businessMethod, EjbCallChain next)
  {
    super(next);

    _next = next;

    _isContainerManaged = true;
    _lockType = LockType.WRITE;

    // TODO Should these be set from a configuration mechanism?
    _lockTimeout = 10000;
    _lockTimeoutUnit = TimeUnit.MILLISECONDS;
  }

  /**
   * Returns true if the business method has a lock annotation.
   */
  @Override
  public boolean isEnhanced()
  {
    return _isContainerManaged;
  }

  /**
   * Introspects the method for locking attributes.
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implementationMethod)
  {
    ApiClass apiClass = apiMethod.getDeclaringClass();

    ConcurrencyManagement concurrencyManagementAnnotation = apiClass
        .getAnnotation(ConcurrencyManagement.class);

    if ((concurrencyManagementAnnotation != null)
        && (concurrencyManagementAnnotation.value() != CONTAINER)) {
      _isContainerManaged = false;
      return;
    }

    ApiClass implementationClass = null;

    if (implementationMethod != null) {
      implementationClass = implementationMethod.getDeclaringClass();
    }

    Lock lockAttribute;

    lockAttribute = apiMethod.getAnnotation(Lock.class);

    if (lockAttribute == null) {
      lockAttribute = apiClass.getAnnotation(Lock.class);
    }

    if ((lockAttribute == null) && (implementationMethod != null)) {
      lockAttribute = implementationMethod.getAnnotation(Lock.class);
    }

    if ((lockAttribute == null) && (implementationClass != null)) {
      lockAttribute = implementationClass.getAnnotation(Lock.class);
    }

    if (lockAttribute != null) {
      _lockType = lockAttribute.value();
    }

    AccessTimeout accessTimeoutAttribute;

    accessTimeoutAttribute = apiMethod.getAnnotation(AccessTimeout.class);

    if (accessTimeoutAttribute == null) {
      accessTimeoutAttribute = apiClass.getAnnotation(AccessTimeout.class);
    }

    if ((accessTimeoutAttribute == null) && (implementationMethod != null)) {
      accessTimeoutAttribute = implementationMethod
          .getAnnotation(AccessTimeout.class);
    }

    if ((accessTimeoutAttribute == null) && (implementationClass != null)) {
      accessTimeoutAttribute = implementationClass
          .getAnnotation(AccessTimeout.class);
    }

    if (accessTimeoutAttribute != null) {
      _lockTimeout = accessTimeoutAttribute.timeout();
      _lockTimeoutUnit = accessTimeoutAttribute.unit();
    }
  }

  /**
   * Generates the class prologue.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void generatePrologue(JavaWriter out, HashMap map) throws IOException
  {
    if (_isContainerManaged && (map.get("caucho.ejb.lock") == null)) {
      // TODO Does this need be registered somewhere?
      map.put("caucho.ejb.lock", "done");

      out.println();
      out
          .println("private transient final java.util.concurrent.locks.ReadWriteLock _readWriteLock");
      out
          .println("  = new java.util.concurrent.locks.ReentrantReadWriteLock();");
      out.println();
    }

    _next.generatePrologue(out, map);
  }

  /**
   * Generates the method interception code.
   */
  @Override
  public void generateCall(JavaWriter out) throws IOException
  {
    // TODO Is this too much code to be in-lined?
    if (_isContainerManaged) {
      switch (_lockType) {
      case READ:
        out.println();
        out.println("try {");
        out.pushDepth(); // Increasing indentation depth.
        out.println("if (_readWriteLock.readLock().tryLock("
            + _lockTimeoutUnit.toMillis(_lockTimeout)
            + ", TimeUnit.MILLISECONDS)) {");
        out.pushDepth(); // Increasing indentation depth.
        out.println("try {");
        out.println();
        break;

      case WRITE:
        out.println();
        out.println("try {");
        out.pushDepth(); // Increasing indentation depth.
        out.println("if (_readWriteLock.writeLock().tryLock("
            + _lockTimeoutUnit.toMillis(_lockTimeout)
            + ", TimeUnit.MILLISECONDS)) {");
        out.pushDepth(); // Increasing indentation depth.
        out.println("try {");
        out.println();
        break;
      }
    }

    generateNext(out);

    if (_isContainerManaged) {
      switch (_lockType) {
      case READ:
        out.popDepth(); // Decrease indentation depth.
        out.println("} finally {");
        out.pushDepth(); // Increasing indentation depth.
        out.println("_readWriteLock.readLock().unlock();");
        out.popDepth(); // Decrease indentation depth.
        out.println("}");
        out.popDepth(); // Decrease indentation depth.
        out.println("} else {");
        out.pushDepth(); // Increasing indentation depth.
        out
            .println("throw new ConcurrentAccessTimeoutException(\"Timed out acquiring read lock.\");");
        out.popDepth(); // Decrease indentation depth.
        out.println("}");
        out.popDepth(); // Decrease indentation depth.
        out.println("} catch (InterruptedException interruptedException) {");
        out.pushDepth(); // Increasing indentation depth.
        out
            .println("throw new ConcurrentAccessTimeoutException(\"Thread interruption acquiring read lock: \" + interruptedException.getMessage());");
        out.popDepth(); // Decrease indentation depth.
        out.println("}");
        out.println();
        break;
      case WRITE:
        out.popDepth(); // Decrease indentation depth.
        out.println("} finally {");
        out.pushDepth(); // Increasing indentation depth.
        out.println("_readWriteLock.writeLock().unlock();");
        out.popDepth(); // Decrease indentation depth.
        out.println("}");
        out.popDepth(); // Decrease indentation depth.
        out.println("} else {");
        out.pushDepth(); // Increasing indentation depth.
        out
            .println("throw new ConcurrentAccessTimeoutException(\"Timed out acquiring write lock.\");");
        out.popDepth(); // Decrease indentation depth.
        out.println("}");
        out.popDepth(); // Decrease indentation depth.
        out.println("} catch (InterruptedException interruptedException) {");
        out.pushDepth(); // Increasing indentation depth.
        out
            .println("throw new ConcurrentAccessTimeoutException(\"Thread interruption acquiring write lock: \" + interruptedException.getMessage());");
        out.popDepth(); // Decrease indentation depth.
        out.println("}");
        out.println();
        break;
      }
    }
  }

  protected void generateNext(JavaWriter out) throws IOException
  {
    _next.generateCall(out);
  }
}