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
  
  private HashMap<String,Object> _valueMap = new HashMap<String,Object>();

  public Object get(String id)
  {
    return _valueMap.get(id);
  }

  public void put(String id, Object value)
  {
    _valueMap.put(id, value);
  }

  public void removeEvent(Object scope, String name)
  {
    close();
  }
  
  public void close()
  {
    for (Map.Entry<String,Object> entry : _valueMap.entrySet()) {
      String id = entry.getKey();
      Object value = entry.getValue();

      Bean<Object> bean = _beanManager.getPassivationCapableBean(id);
      CreationalContext<Object> env = null;

      bean.destroy(value, env);
    }
  }
}
