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

package com.caucho.v5.config.scope;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

import com.caucho.v5.inject.Module;
import com.caucho.v5.util.L10N;

/**
 * Context for a named EL bean scope
 */
@Module
abstract public class ScopeContextBase implements Context, AlterableContext {
  public static final L10N L = new L10N(ScopeContextBase.class);
  
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  abstract public boolean isActive();

  /**
   * Returns the scope annotation type.
   */
  @Override
  abstract public Class<? extends Annotation> getScope();

  /**
   * Returns the current instance, if it exists.
   */
  @Override
  public <T> T get(Contextual<T> bean)
  {
    if (! isActive())
      throw new ContextNotActiveException(L.l("{0} cannot be used because it's not currently active",
                                              getClass().getName()));
    
    ContextContainer context = getContextContainer();
    
    if (context == null)
      return null;
    
    Object key = bean;

    if (bean instanceof PassivationCapable)
      key = ((PassivationCapable) bean).getId();

    return (T) context.get(key);
  }

  @Override
  public <T> T get(Contextual<T> bean,
                   CreationalContext<T> creationalContext)
  {
    if (! isActive())
      throw new ContextNotActiveException(L.l("{0} cannot be used because it's not currently active",
                                              getClass().getName()));
    
    ContextContainer context = createContextContainer();

    if (context == null)
      return null;

    Object key = bean;
    
    if (bean instanceof PassivationCapable) {
      key = ((PassivationCapable) bean).getId();
    }

    T result = (T) context.get(key);
    
    if (result != null || creationalContext == null) {
      return result;
    }

    result = create(bean, creationalContext);
    
    context.put(bean, key, result, creationalContext);

    return result;
  }
  
  protected <T> T create(Contextual<T> bean, CreationalContext<T> env)
  {
    return bean.create(env);
  }
  
  @Override
  public void destroy(Contextual<?> bean)
    throws ContextNotActiveException
  {
    ContextContainer env = getContextContainer();
    
    if (env == null) {
      return;
    }
    
    env.destroy(bean);
  }

  abstract protected ContextContainer getContextContainer();

  abstract protected ContextContainer createContextContainer();
  
  public void closeContext()
  {
    ContextContainer context = getContextContainer();
    
    if (context != null)
      context.close();
  }

  public String toString()
  {
    return this.getClass().getName() + "[]";
  }
}
