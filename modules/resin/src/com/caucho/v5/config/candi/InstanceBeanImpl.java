/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.candi;

import javax.enterprise.inject.spi.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * Internal implementation for a Bean
 */
public class InstanceBeanImpl<T> implements Bean<T>
{
  private CandiManager _cdiManager;
  private Type _type;
  private Annotation []_bindings;

  private InstanceImpl _instance;

  InstanceBeanImpl(CandiManager cdiManager,
                   BeanManager beanManager,
                   Type type,
                   Annotation []bindings,
                   InjectionPoint injectionPoint)
  {
    _cdiManager = cdiManager;
    _type = type;
    _bindings = bindings;

    _instance = new InstanceImpl(cdiManager,
                                 beanManager,
                                 _type, _bindings,
                                 injectionPoint);
  }

  public Class getBeanClass()
  {
    return (Class) _type;
  }

  public T create(CreationalContext<T> env)
  {
    return (T) _instance;
  }

  public void destroy(T instance, CreationalContext<T> env)
  {
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding annotations.
   */
  public Set<Annotation> getQualifiers()
  {
    return null;
  }

  /**
   * Returns the bean's stereotype annotations.
   */
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return null;
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return new HashSet<InjectionPoint>();
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    return null;
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isAlternative()
  {
    return false;
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isPassivationCapable()
  {
    return true;
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _instance + "]";
  }
}
