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

import com.caucho.config.inject.AbstractBean;
import com.caucho.inject.Module;
import com.caucho.loader.Environment;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;


/**
 * The application scope value
 */
@Module
public class DependentScope {
  private static final ThreadLocal<DependentScope> _threadScope
    = new ThreadLocal<DependentScope>();

  private AbstractBean<?> _owner;
  private Object _value;

  private IdentityHashMap<AbstractBean<?>,Object> _map;

  public DependentScope()
  {
  }
  
  /**
   * Returns the current dependent scope.
   */
  public static DependentScope getCurrent()
  {
    return _threadScope.get();
  }
  
  /**
   * Returns the object with the given name.
   */
  public Object get(AbstractBean<?> comp)
  {
    if (comp == _owner)
      return _value;
    else if (_map != null)
      return _map.get(comp);
    else
      return null;
  }

  public Object findByName(String name)
  {
    if (_owner != null && name.equals(_owner.getName()))
      return _value;
    else if (_map != null && _map.size() > 0) {
      for (Map.Entry<AbstractBean<?>,Object> entry : _map.entrySet()) {
	AbstractBean<?> comp = entry.getKey();

	if (name.equals(comp.getName()))
	  return entry.getValue();
      }

      return null;
    }
    else
      return null;
  }

  /**
   * Sets the object with the given name.
   */
  public void put(AbstractBean<?> comp, Object value)
  {
    if (_map == null)
      _map = new IdentityHashMap<AbstractBean<?>,Object>(8);
    
    _map.put(comp, value);
  }

  /**
   * Sets the object with the given name.
   */
  public void remove(AbstractBean<?> comp)
  {
    if (_map != null)
      _map.remove(comp);
  }

  /**
   * Adds a @PreDestroy destructor
   */
  public <T> void addDestructor(AbstractBean<T> comp, T value)
  {
    /*
    if (_scope != null)
      _scope.addDestructor(comp, value);
    else {
      // add to env?
      Environment.addCloseListener(new ComponentDestructor(comp, value));
    }
    */
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
