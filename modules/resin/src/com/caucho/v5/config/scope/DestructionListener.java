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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.enterprise.context.spi.Contextual;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.ClassLoaderListener;
import com.caucho.v5.loader.DynamicClassLoader;

/**
 * Contains the objects which need destruction for a given scope.
 */
@SuppressWarnings("serial")
@Module
public class DestructionListener
  implements ScopeRemoveListener,
             HttpSessionBindingListener,
             ClassLoaderListener,
             Serializable {
  private transient ArrayList<Contextual<?>> _beanList
    = new ArrayList<Contextual<?>>();

  private transient ArrayList<WeakReference<Object>> _valueList
    = new ArrayList<WeakReference<Object>>();

  public void addValue(Contextual<?> bean, Object value)
  {
    _beanList.add(bean);
    _valueList.add(new WeakReference<Object>(value));
  }

  public void removeEvent(Object scope, String name)
  {
    close();
  }

  public void valueBound(HttpSessionBindingEvent event)
  {
  }

  public void valueUnbound(HttpSessionBindingEvent event)
  {
    close();
  }

  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    close();
  }

  public void close()
  {
    ArrayList<Contextual<?>> beanList = _beanList;
    _beanList = null;

    ArrayList<WeakReference<Object>> valueList = _valueList;
    _valueList = null;

    if (valueList == null || beanList == null)
      return;

    for (int i = beanList.size() - 1; i >= 0; i--) {
      Contextual bean = beanList.get(i);
      WeakReference<Object> ref = valueList.get(i);
      Object value = null;
      
      if (ref != null) {
        value = ref.get();

        if (value != null)
          bean.destroy(value, null);
      }
    }
  }
}
