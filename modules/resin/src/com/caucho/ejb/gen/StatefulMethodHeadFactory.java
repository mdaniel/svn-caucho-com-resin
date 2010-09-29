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

import javax.ejb.AccessTimeout;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectFactory;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.MethodHeadFactory;
import com.caucho.inject.Module;

/**
 * Represents a stateful local business method
 */
@Module
public class StatefulMethodHeadFactory<X> extends MethodHeadFactory<X> {
  private AccessTimeout _classAccessTimeout;

  public StatefulMethodHeadFactory(StatefulAspectBeanFactory<X> beanFactory,
      AspectFactory<X> next)
  {
    super(beanFactory, next);

    AnnotatedType<X> beanType = beanFactory.getBeanType();

    _classAccessTimeout = beanType.getAnnotation(AccessTimeout.class);
  }

  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
      boolean isEnhanced)
  {
    // TODO The annotation resolution algorithm should probably be re-factored
    // in a superclass.

    AnnotatedType<?> declaringType = method.getDeclaringType();

    AccessTimeout accessTimeout = null;

    AccessTimeout methodLevelAccessTimeout = method
        .getAnnotation(AccessTimeout.class);

    AccessTimeout declaringClassAccessTimeout = (declaringType != null) ? declaringType
        .getAnnotation(AccessTimeout.class) : null;

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

    AspectGenerator<X> next = super.create(method, true);

    if (accessTimeout != null) {
      return new StatefulMethodHeadGenerator<X>(this, method, next,
          accessTimeout.value(), accessTimeout.unit());
    } else {
      return new StatefulMethodHeadGenerator<X>(this, method, next);
    }
  }
}