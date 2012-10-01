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

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;

/**
 * Aspect factory for generating @Lock aspects.
 */
@Module
public class LockFactory<X> extends AbstractAspectFactory<X> {
  private ConcurrencyManagementType _concurrencyManagement
    = ConcurrencyManagementType.CONTAINER;
  private LockType _classLockType;
  private AccessTimeout _classAccessTimeout;

  public LockFactory(AspectBeanFactory<X> beanFactory, AspectFactory<X> next) {
    super(beanFactory, next);

    AnnotatedType<X> beanType = beanFactory.getBeanType();

    ConcurrencyManagement concurrencyManagement = beanType
        .getAnnotation(ConcurrencyManagement.class);

    if (concurrencyManagement != null) {
      _concurrencyManagement = concurrencyManagement.value();
    }

    Lock lock = beanType.getAnnotation(Lock.class);

    if (lock != null) {
      _classLockType = lock.value();
    }

    _classAccessTimeout = beanType.getAnnotation(AccessTimeout.class);
  }

  /**
   * Creates an aspect for interception if the method should be intercepted.
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })  
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
      boolean isEnhanced)
  {
    // TODO The annotation resolution algorithm should probably be re-factored
    // in a superclass.

    AnnotatedType<?> declaringType = method.getDeclaringType();

    LockType lockType = null;

    Lock methodLevelLock = method.getAnnotation(Lock.class);

    Lock declaringClassLock = (declaringType != null) ? declaringType
        .getAnnotation(Lock.class) : null;

    // The method-level annotation takes precedence.
    if (methodLevelLock != null) {
      lockType = methodLevelLock.value();
    }
    // Then the class-level annotation at the declaring class takes precedence.
    else if (declaringClassLock != null) {
      lockType = declaringClassLock.value();
    }
    // Finally, the top level class annotation takes effect.
    else {
      lockType = _classLockType;
    }

    AccessTimeout accessTimeout = null;

    AccessTimeout methodLevelAccessTimeout = method
        .getAnnotation(AccessTimeout.class);

    AccessTimeout declaringClassAccessTimeout = (declaringType != null) ? declaringType
        .getAnnotation(AccessTimeout.class)
        : null;

    // The method-level annotation takes precedence.
    if (methodLevelAccessTimeout != null) {
      accessTimeout = methodLevelAccessTimeout;
    }
    // Then the class-level annotation at the declaring class takes precedence.
    else if (declaringClassAccessTimeout != null) {
      accessTimeout = declaringClassAccessTimeout;
    }
    // Finally, the top level class annotation takes effect.
    else {
      accessTimeout = _classAccessTimeout;
    }

    if ((lockType == null)
        || (_concurrencyManagement == ConcurrencyManagementType.BEAN))
      return super.create(method, isEnhanced);
    else {
      AspectGenerator<X> next = super.create(method, true);

      if (accessTimeout != null) {
        return new LockGenerator(this, method, next, lockType, accessTimeout
            .value(), accessTimeout.unit());
      } else {
        return new LockGenerator(this, method, next, lockType, -1, null);
      }
    }
  }
}
