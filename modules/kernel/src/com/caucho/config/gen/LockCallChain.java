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

import java.io.IOException;
import java.util.HashMap;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import static javax.ejb.ConcurrencyManagementType.CONTAINER;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.transaction.Synchronization;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents EJB lock type specification interception. The specification gears
 * it towards EJB singletons, but it can be used for other bean types.
 */
// TODO What bean types (e.g. JCDI scopes) do we foresee a use for?
public class LockCallChain extends AbstractCallChain {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(LockCallChain.class);

  private EjbCallChain _next;

  private boolean _isContainerManaged;
  private LockType _lockType;
  private boolean _isSynchronization;

  public LockCallChain(BusinessMethodGenerator businessMethod,
		       EjbCallChain next)
  {
    super(next);

    _next = next;

    // TODO What would be the synchronization counter-part? Is this just for
    // defaulting? Will a default of "true" suffice?
    _isContainerManaged = businessMethod.isXaContainerManaged();

    // TODO Should the lock type be defaulted to "WRITE"? This would be true for
    // EJB 3.1 singletons...
  }

  /**
   * Returns true if the business method has a lock annotation.
   */
  @Override
  public boolean isEnhanced()
  {
    return (_isContainerManaged && _lockType != null);
  }

  /**
   * Introspects the method for locking attributes.
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implementationMethod)
  {
    ApiClass apiClass = apiMethod.getDeclaringClass();

    ConcurrencyManagement concurrencyManagementAnnotation
      = apiClass.getAnnotation(ConcurrencyManagement.class);

    if (concurrencyManagementAnnotation != null
        && concurrencyManagementAnnotation.value() != CONTAINER) {
      _isContainerManaged = false;
      return;
    }

    ApiClass implementationClass = null;

    if (implementationMethod != null) {
      implementationClass = implementationMethod.getDeclaringClass();
    }

    // TODO What is this checking for? Whether the implementing class is a
    // subclass of Synchronization? Is this pertinent to the locking
    // implementation?
    if (implementationClass != null
        && Synchronization.class.isAssignableFrom(implementationClass
            .getJavaClass())) {
      _isSynchronization = true;
    }

    Lock lockAttribute;

    lockAttribute = apiMethod.getAnnotation(Lock.class);

    if (lockAttribute == null) {
      lockAttribute = apiClass.getAnnotation(Lock.class);
    }

    if (lockAttribute == null && implementationMethod != null) {
      lockAttribute = implementationMethod.getAnnotation(Lock.class);
    }

    if (lockAttribute == null && implementationClass != null) {
      lockAttribute = implementationClass.getAnnotation(Lock.class);
    }

    if (lockAttribute != null) {
      _lockType = lockAttribute.value();
    }
  }

  /**
   * Generates the class prologue.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void generatePrologue(JavaWriter out, HashMap map) throws IOException
  {
    if (_isContainerManaged && map.get("caucho.ejb.lock") == null) {
      // TODO Does this need be registered somewhere?
      map.put("caucho.ejb.lock", "done");

      out.println();
      // TODO Should this be static? Should locks be placed at the instance or
      // class level? The class level is fine for EJB singletons, but not
      // necessarily for other bean types.
      out.println("private final com.caucho.ejb3.lock.LockManager _lockManager");
      out.println("  = new com.caucho.ejb3.lock.LockManager();");
    }

    _next.generatePrologue(out, map);
  }

  /**
   * Generates the method interception code.
   */
  @Override
  public void generateCall(JavaWriter out) throws IOException
  {
    if (_isContainerManaged && _lockType != null) {
      switch (_lockType) {
      case READ:
        out.println();
        out.println("try {");
        // Increasing indentation depth.
        out.pushDepth();
        out.println("_lockManager.acquireReadLock();");
        out.println();
        break;

      case WRITE:
        out.println();
        out.println("try {");
        // Increasing indentation depth.
        out.pushDepth();
        out.println("_lockManager.acquireWriteLock();");
        out.println();
        break;
      }
    }

    // TODO Is an equivalent needed here?
    if (_isSynchronization) {
      out.println("_xa.registerSynchronization(_bean);");
    }

    generateNext(out);

    if (_isContainerManaged && _lockType != null) {
      // Decrease indentation depth.
      out.popDepth();

      switch (_lockType) {
      case READ:
        out.println("} finally {");
        out.println("  _lockManager.releaseReadLock();");
        out.println("}");
        out.println();
        break;
      case WRITE:
        out.println("} finally {");
        out.println("  _lockManager.releaseWriteLock();");
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