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

import com.caucho.loader.*;
import com.caucho.config.Service;
import com.caucho.webbeans.component.*;

import java.lang.annotation.Annotation;
import java.util.Hashtable;

import javax.inject.manager.Bean;

/**
 * The service scope manages load-on-startup services which also
 * publish to osgi.
 */
public class ServiceScope extends ScopeContext
{
  private Hashtable _map = new Hashtable();
  
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
    return Service.class;
  }
  
  public <T> T get(Bean<T> bean, boolean create)
  {
    Object v = _map.get(bean);
      
    if (v == null && create) {
      v = bean.create();
      // XXX: delete because of optimistic locking
      _map.put(bean, v);
    }
    
    return (T) v;
  }
  
  public <T> void put(Bean<T> bean, T value)
  {
    _map.put(bean, value);
  }
  
  public <T> void remove(Bean<T> bean)
  {
    _map.remove(bean);
  }
}
