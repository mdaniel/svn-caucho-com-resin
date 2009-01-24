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

import com.caucho.config.inject.ComponentImpl;
import com.caucho.loader.*;
import com.caucho.webbeans.Singleton;

import java.lang.annotation.Annotation;
import javax.inject.manager.Bean;

/**
 * The singleton scope value
 */
public class SingletonScope extends ScopeContext {
  private final static EnvironmentLocal<ScopeMap> _localScopeMap
    = new EnvironmentLocal<ScopeMap>();

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
  public Class<? extends Annotation> getScopeType()
  {
    return Singleton.class;
  }
  
  public <T> T get(Bean<T> bean, boolean create)
  {
    ScopeMap map = _localScopeMap.get();

    if (map != null) {
      return (T) map.get(bean);
    }
    else
      return null;
  }
  
  public <T> void put(Bean<T> bean, T value)
  {
    ScopeMap map;
    
    synchronized (this) {
      map = _localScopeMap.getLevel();

      if (map == null) {
	map = new ScopeMap();
	
	_localScopeMap.set(map);
      }
    }

    map.put(bean, value);
  }
  
  public <T> void remove(Bean<T> component)
  {
    ScopeMap map = _localScopeMap.getLevel();

    if (map != null) {
      synchronized (map) {
	map.remove(component);
      }
    }
  }

  public void addDestructor(ComponentImpl comp, Object value)
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
