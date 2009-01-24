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

package com.caucho.config.scope;

import java.lang.annotation.Annotation;

import javax.webbeans.*;
import javax.context.Context;
import javax.inject.manager.Bean;

import com.caucho.config.inject.ComponentImpl;

/**
 * Context for a named EL bean scope
 */
abstract public class ScopeContext implements Context {
  /**
   * Returns true if the scope is currently active.
   */
  public boolean isActive()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the scope annotation type.
   */
  abstract public Class<? extends Annotation> getScopeType();

  /**
   * Returns a instance of a bean, creating if necessary
   */
  abstract public <T> T get(Bean<T> bean, boolean create);

  /**
   * Returns a instance of a bean, creating if necessary
   */
  public <T> void put(Bean<T> bean, T value)
  {
    // XXX: needs to be removed?
  }
  
  /**
   * Returns true if a value in the target scope can be safely be injected
   * into this scope
   */
  public boolean canInject(ScopeContext scope)
  {
    return (getClass().equals(scope.getClass())
	    || scope instanceof SingletonScope);
  }

  public void addDestructor(ComponentImpl comp, Object value)
  {
  }
}
