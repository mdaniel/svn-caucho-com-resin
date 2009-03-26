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
 * @author Scott Ferguson
 */

package com.caucho.config.inject;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.context.Contextual;
import javax.context.CreationalContext;

import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.Manager;

/**
 * Configuration for the xml web bean component.
 */
public class InjectionPointBean<T> extends Bean<T>
{
  public InjectionPointBean(Manager manager)
  {
    super(manager);
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding annotations.
   */
  public Set<Annotation> getBindings()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's deployment type
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isSerializable()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScopeType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Class<?>> getTypes()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public T create(CreationalContext<T> creationalContext)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void destroy(T instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
