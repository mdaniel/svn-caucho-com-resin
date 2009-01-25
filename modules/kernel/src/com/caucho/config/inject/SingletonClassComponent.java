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

package com.caucho.config.inject;

import com.caucho.config.ConfigContext;
import com.caucho.config.scope.SingletonScope;
import com.caucho.jmx.*;

import java.util.logging.*;
import java.io.Closeable;

/**
 * Configuration for a singleton component.
 */
public class SingletonClassComponent extends SimpleBean
  implements Closeable
{
  private static final Logger log
    = Logger.getLogger(SingletonClassComponent.class.getName());
  
  private Object _value;

  public SingletonClassComponent(Class type)
  {
    super(type);
    
    super.setScope(new SingletonScope());
  }
  
  public SingletonClassComponent(InjectManager webBeans)
  {
    super(webBeans);
    
    super.setScope(new SingletonScope());
  }

  /**
   * Returns the singleton object
   */
  @Override
  public Object getIfExists()
  {
    return get();
  }

  /**
   * Returns the singleton object
   */
  @Override
  public Object get()
  {
    if (_value == null) {
      return get(null);
    }

    return _value;
  }

  /**
   * Returns the singleton object
   */
  @Override
  public Object get(ConfigContext env)
  {
    if (_value == null) {
      _value = createNew(null);

      init(_value, new ConfigContext(this, _value, new SingletonScope()));

      if (_value instanceof HandleAware)
	((HandleAware) _value).setSerializationHandle(getHandle());
    }

    return _value;
  }

  /**
   * The singleton instance is created in its original class loader context
   */
  @Override
  protected Object createNew(ConfigContext env)
  {
    ClassLoader loader = getWebBeans().getClassLoader();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      return super.createNew(env);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * The singleton instance is initialized in its original class loader context
   */
  @Override
  protected Object init(Object value, ConfigContext env)
  {
    ClassLoader loader = getWebBeans().getClassLoader();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      bind();

      value = super.init(value, env);

      try {
	if (getMBeanInterface() != null)
	  Jmx.register(value, getMBeanName());
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      return value;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Frees the singleton on environment shutdown
   */
  public void close()
  {
    if (_value != null)
      destroy(_value);
  }
}
