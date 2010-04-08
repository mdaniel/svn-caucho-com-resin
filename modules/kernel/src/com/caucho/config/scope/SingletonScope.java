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

import javax.enterprise.context.spi.Contextual;
import javax.inject.Singleton;

import com.caucho.inject.Module;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;

/**
 * The application scope value
 */
@Module
public class SingletonScope extends ScopeContext {
  private ScopeMap _scopeMap = new ScopeMap();

  /**
   * Returns the current application scope
   */
  public SingletonScope()
  {
  }

  /**
   * Returns true if the scope is currently active.
   */
  public boolean isActive()
  {
    return true;
   }

  /**
   * Returns the scope annotation type.
   */
  public Class<? extends Annotation> getScope()
  {
    return Singleton.class;
  }

  @Override
  protected ScopeMap getScopeMap()
  {
    return _scopeMap;
  }

  @Override
  protected ScopeMap createScopeMap()
  {
    return _scopeMap;
  }

  @Override
  public boolean canInject(ScopeContext scope)
  {
    return (scope instanceof ApplicationScope);
  }

  public <T> void addDestructor(Contextual<T> comp, T value)
  {
    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();

    if (loader != null) {
      DestructionListener listener
        = (DestructionListener) loader.getAttribute("caucho.destroy");

      if (listener == null) {
        listener = new DestructionListener();
        loader.setAttribute("caucho.destroy", listener);
        loader.addListener(listener);
      }

      listener.addValue(comp, value);
    }
  }
}
