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

package com.caucho.config.scope;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.caucho.inject.Module;

/**
 * Context for a named EL bean scope
 */
@Module
abstract public class ScopeContext implements Context {
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the scope annotation type.
   */
  abstract public Class<? extends Annotation> getScope();

  /**
   * Returns the current instance, if it exists.
   */
  public <T> T get(Contextual<T> bean)
  {
    ScopeMap scopeMap = getScopeMap();

    if (scopeMap != null) {
      return scopeMap.get(bean);
    }

    return null;
  }

  public <T> T get(Contextual<T> bean,
                   CreationalContext<T> creationalContext)
  {
    ScopeMap scopeMap = getScopeMap();

    T instance = null;

    if (scopeMap != null) {
      instance = scopeMap.get(bean);

      if (instance != null)
        return instance;
    }

    if (creationalContext == null)
      return null;

    // ConfigContext env = (ConfigContext) creationalContext;

    // env.setScope(this, bean);

    if (scopeMap == null)
      scopeMap = createScopeMap();

    instance = bean.create(creationalContext);

    scopeMap.put(bean, instance);
    addDestructor(bean, instance);

    return instance;
  }

  public <T> void put(Contextual<T> bean, T instance)
  {
    ScopeMap scopeMap = getScopeMap();

    scopeMap.put(bean, instance);
  }

  protected ScopeMap getScopeMap()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected ScopeMap createScopeMap()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if a value in the target scope can be safely be injected
   * into this scope
   */
  public boolean canInject(ScopeContext scope)
  {
    return (getClass().equals(scope.getClass())
            || scope instanceof ApplicationScope);
  }

  /**
   * Returns true if a value in the target scope can be safely be injected
   * into this scope
   */
  public boolean canInject(Class<?> scope)
  {
    /*
    return (getScope() == scope
            || scope == ApplicationScoped.class
            || scope == Dependent.class);
            */
    return (getScope() == scope
            || scope == ApplicationScoped.class);
  }

  public <T> void addDestructor(Contextual<T> bean, T value)
  {
  }
}
