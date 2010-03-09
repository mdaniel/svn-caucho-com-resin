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
import java.util.HashMap;
import java.util.Map;

import java.io.Serializable;

import javax.enterprise.context.*;
import javax.enterprise.context.spi.*;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.inject.HandleAware;
import com.caucho.config.inject.InjectManager;

/**
 * Context for a named EL bean scope
 */
public class ContextContainer implements Serializable, ScopeRemoveListener
{
  private transient InjectManager _beanManager = InjectManager.create();
  
  private ContextItem<?> _values;

  public Object get(String id)
  {
    for (ContextItem<?> ptr = _values; ptr != null; ptr = ptr.getNext()) {
      if (id.equals(ptr.getId()))
        return ptr.getObject();
    }
    
    return null;
  }

  public <T> T get(Contextual<T> bean)
  {
    for (ContextItem<?> ptr = _values; ptr != null; ptr = ptr.getNext()) {
      if (bean == ptr.getBean())
        return (T) ptr.getObject();
    }
    
    return null;
  }

  public <T> void put(Contextual<T> bean, 
                      String id, 
                      T value, 
                      CreationalContext<T> env)
  {
    _values = new ContextItem<T>(_values, bean, id, value, env);
  }

  public void removeEvent(Object scope, String name)
  {
    close();
  }
  
  public void close()
  {
    ContextItem<?> entry = _values;
    _values = null;
    
    for (; entry != null; entry = entry.getNext()) {
      Contextual bean = entry.getBean();
      String id = entry.getId();
      Object value = entry.getObject();

      if (bean == null)
        bean = _beanManager.getPassivationCapableBean(id);
      
      CreationalContext<?> env = entry.getEnv();

      bean.destroy(value, env);
    }
  }
  
  static class ContextItem<T> {
    private final ContextItem<?> _next;
    
    private final transient Contextual<T> _bean;
    private final String _id;
    private final T _object;
    private final transient CreationalContext<T> _env;
    
    ContextItem(ContextItem<?> next,
                Contextual<T> bean, 
                String id,
                T object, 
                CreationalContext<T> env)
    {
      _next = next;
      _bean = bean;
      _id = id;
      _object = object;
      _env = env;
    }
    
    ContextItem<?> getNext()
    {
      return _next;
    }
    
    Contextual<T> getBean()
    {
      return _bean;
    }
    
    String getId()
    {
      return _id;
    }
    
    T getObject()
    {
      return _object;
    }
    
    CreationalContext<T> getEnv()
    {
      return _env;
    }
  }
}
