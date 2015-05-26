/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.config.inject;

import java.lang.reflect.Method;

import javax.enterprise.inject.spi.AnnotatedMethod;

/**
 * InterceptorBean represents a Java interceptor
 */
public class InterceptorRuntimeBeanJavaee<X> extends InterceptorRuntimeBean<X>
{
  public InterceptorRuntimeBeanJavaee(InterceptorRuntimeBeanJavaee<X> child,
                                      Class<X> type)
  {
    super(child, type);
  }
  
  @Override
  protected InterceptorRuntimeBean<X> getChild()
  {
    return (InterceptorRuntimeBeanJavaee) super.getChild();
  }

  @Override
  protected void introspectMethod(Method method,
                                  AnnotatedMethod<?> annMethod,
                                  Class <?>childClass)
  {
    super.introspectMethod(method, annMethod, childClass);

    /*
    if (method.isAnnotationPresent(PrePassivate.class)
        && (getChild() == null
        || ! isMethodMatch(getChild().getPrePassivate(), method))) {
      setPrePassivate(method);
      method.setAccessible(true);
    }

    if (method.isAnnotationPresent(PostActivate.class)
        && (getChild() == null
        || ! isMethodMatch(getChild().getPostActivate(), method))) {
      setPostActivate(method);
      method.setAccessible(true);
    }
    */
  }
  
  protected boolean isMethodInterceptorJavaee(Method method)
  {
    if (super.isMethodInterceptor(method)) {
      return true;
    }
    /*
    else if (method.isAnnotationPresent(PrePassivate.class)) {
      return true;
    }
    else if (method.isAnnotationPresent(PostActivate.class)) {
      return true;
    }
    */
    else {
      return false;
    }
  }
}
