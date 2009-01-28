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
import com.caucho.config.inject.HandleAware;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.ApplicationScope;

import java.io.Closeable;
import java.lang.annotation.*;
import java.lang.reflect.Type;
import javax.context.CreationalContext;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
public class SingletonBean extends SimpleBean
  implements Closeable
{
  private static final Object []NULL_ARGS = new Object[0];

  private Object _value;

  /**
   * Creates a WebBeans Bean for the value by introspecting the
   * value's annotations and methods.
   *
   * @param value the singleton value
   */
  public SingletonBean(Object value)
  {
    this(value, null, null, null);
  }


  /**
   * Creates a WebBeans Bean for the value by introspecting the
   * value's annotations and methods.
   *
   * @param value the singleton value
   * @param name the webbean name
   */
  public SingletonBean(Object value, String name)
  {
    this(value, name, null, null, null);
  }
  
  public SingletonBean(Object value,
		       String name,
		       Type ...api)
  {
    this(value, null, name, null, api);
  }
  
  public SingletonBean(Object value,
		       String name,
		       Annotation []binding)
  {
    this(value, null, name, binding);
  }
  
  public SingletonBean(Object value,
		       Class<? extends Annotation> deploymentType,
		       String name,
		       Type ...api)
  {
    this(value, deploymentType, name, null, api);
  }
  
  public SingletonBean(Object value,
		       Class<? extends Annotation> deploymentType,
		       String name,
		       Annotation []binding,
		       Type ...api)
  {
    super(InjectManager.create());
    
    _value = value;
    setTargetType(value.getClass());
    
    super.setScope(InjectManager.create().getApplicationScope());
    
    setName(name);

    // for null API, use void.class
    if (api != null && api.length > 0) {
      for (Type type : api) {
	addType(type);
      }
    }

    if (deploymentType != null)
      setDeploymentType(deploymentType);

    if (binding != null) {
      for (Annotation ann : binding)
	addBinding(ann);
    }

    init();
  }

  /**
   * Special constructor for internal use
   */
  public SingletonBean(InjectManager webBeans, Object value)
  {
    super(webBeans);
    
    _value = value;

    setTargetType(value.getClass());
    
    super.setScope(webBeans.getApplicationScope());
  }

  @Override
  public void bind()
  {
  }
  
  @Override
  public void introspectConstructor()
  {
  }

  /**
   * Complete initialization
   */
  @Override
  public void init()
  {
    super.init();

    if (_value instanceof HandleAware)
      ((HandleAware) _value).setSerializationHandle(getHandle());
  }

  @Override
  public void setScope(ScopeContext scope)
  {
  }

  @Override
  public Object get()
  {
    return _value;
  }

  @Override
  public Object get(ConfigContext env)
  {
    return _value;
  }

  @Override
  public Object create()
  {
    return _value;
  }

  @Override
  public Object create(CreationalContext env)
  {
    return _value;
  }

  @Override
  protected Object createNew(CreationalContext env)
  {
    throw new IllegalStateException();
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
