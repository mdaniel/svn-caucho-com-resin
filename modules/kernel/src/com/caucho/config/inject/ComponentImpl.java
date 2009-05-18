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

import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Configuration for the xml web bean component.
 */
public class ComponentImpl<T> extends AbstractBean<T>
  implements ObjectProxy
{
  private static final L10N L = new L10N(ComponentImpl.class);

  private static final Object []NULL_ARGS = new Object[0];
  private static final ConfigProgram []NULL_INJECT = new ConfigProgram[0];

  private boolean _isFromClass;
  
  private SingletonHandle _handle;

  private String _scopeId;
  
  private ContainerProgram _init;

  public ComponentImpl(InjectManager webbeans)
  {
    super(webbeans);
  }

  /**
   * Sets the scope annotation.
   */
  public void setScope(ScopeContext scope)
  {
    _scope = scope;
  }

  /**
   * Gets the scope annotation.
   */
  public ScopeContext getScope()
  {
    return _scope;
  }

  public boolean isSingleton()
  {
    return (_scope instanceof ApplicationScope);
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Add to the init program.
   */
  public void addProgram(ConfigProgram program)
  {
    if (_init == null)
      _init = new ContainerProgram();
    
    _init.addProgram(program);
  }

  /**
   * True if the component was defined by class introspection.
   */
  public void setFromClass(boolean isFromClass)
  {
    _isFromClass = isFromClass;
  }

  /**
   * True if the component was defined by class introspection.
   */
  public boolean isFromClass()
  {
    return _isFromClass;
  }

  /**
   * Returns the serialization handle
   */
  public SingletonHandle getHandle()
  {
    if (_handle == null)
      _handle = new SingletonHandle(getTargetType(), getBindings());
    
    return _handle;
  }

  /**
   * Initialization.
   */
  public void init()
  {
    super.init();

    generateScopeId();

    if (isSingleton()) {
      _handle = new SingletonHandle(getTargetType(), getBindings());
    }
    
    if (getScopeType() != null) {
      _scope = _beanManager.getScopeContext(getScopeType());
    }
  }

  private void generateScopeId()
  {
    long crc64 = 17;

    crc64 = Crc64.generate(crc64, String.valueOf(getTargetType()));

    if (getName() != null)
      crc64 = Crc64.generate(crc64, getName());

    /*
    for (WbBinding binding : _bindingList) {
      crc64 = binding.generateCrc64(crc64);
    }
    */

    StringBuilder sb = new StringBuilder();
    Base64.encode(sb, crc64);

    _scopeId = sb.toString();
  }

  /**
   * Returns the component object if it already exists
   */
  public Object getIfExists()
  {
    return null;
    /*
    if (_scope != null)
      return _scope.get(this, false);
    else
      return get();
    */
  }

  /**
   * Returns the component object, creating if necessary
   */
  public Object get()
  {
    return _beanManager.getReference(this);
  }

  public T get(ConfigContext env)
  {
    return null;
    
    /* XXX:
    if (_scope != null) {
      T value = _scope.get(this, false);

      if (value != null)
	return value;
    }
    else {
      T value = (T) env.get(this);

      if (value != null)
	return value;
    }

    T value;

    if (_scope != null) {
      value = createNew(null);
      _scope.put(this, value);
      
      env = new ConfigContext(this, value, _scope);
    }
    else {
      value = createNew(env);
    }

    init(value, env);

    return value;
    */
  }

  /**
   * Creates a new instance of the component.
   */
  public T create(CreationalContext<T> context)
  {
    return create(context, null);
  }
  
  /**
   * Creates a new instance of the component.
   */
  public T create(CreationalContext<T> context,
		  InjectionPoint ij)
  {
    T object = createNew(context, ij);

    init(object, context);
    
    return object;
  }

  public T createNew()
  {
    return create(new ConfigContext());
  }

  /**
   * Creates a new instance of the component.
   */
  public T create()
  {
    /* XXX:
    try {
      ConfigContext env = new ConfigContext(_scope);
      
      T value = createNew(env);

      env.put(this, value);

      if (_scope != null)
	_scope.put(this, value);

      init(value, env);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    */
    return null;
  }

  /**
   * Creates a new instance of the component.
   */
  public T createNoInit()
  {
    try {
      T value = createNew(null, null);

      if (_injectProgram.length > 0) {
        ConfigContext env = new ConfigContext(this, value, null);

	for (ConfigProgram program : _injectProgram) {
	  program.inject(value, env);
	}
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new instance of the component
   * 
   * @param env the configuration environment
   * @return the new object
   */
  protected T createNew(CreationalContext context,
			InjectionPoint ij)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Initialize the created value
   */
  protected T init(T value, CreationalContext context)
  {
    ConfigContext env = (ConfigContext) context;

    if (_init != null)
      _init.inject(value, env);

    for (ConfigProgram inject : _injectProgram) {
      inject.inject(value, env);
    }

    for (ConfigProgram inject : _initProgram) {
      inject.inject(value, env);
    }

    if (_cauchoPostConstruct != null) {
      try {
	_cauchoPostConstruct.invoke(value);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }

    // server/13a3 - XXX: handled in scope
    /*
    if (_destroyProgram.length > 0) {
      env.addDestructor(this, value);
    }
    */

    return value;
  }

  /**
   * Returns true if there's a destroy program.
   */
  public boolean isDestroyPresent()
  {
    return _destroyProgram.length > 0;
  }

  /**
   * Destroys the value
   */
  public void destroy(Object value, ConfigContext env)
  {
    for (ConfigProgram inject : _destroyProgram) {
      inject.inject(value, env);
    }
  }

  /**
   * Destroys the value
   */
  public void destroy(T value)
  {
    destroy(value, null);
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
  }

  public Bean bindInjectionPoint(InjectionPoint ij)
  {
    return this;
  }

  public void createProgram(ArrayList<ConfigProgram> initList, Field field)
    throws ConfigException
  {
    // initList.add(new FieldComponentProgram(this, field));
  }

  public String getScopeId()
  {
    return _scopeId;
  }

  //
  // metadata for the bean
  //

  //
  // lifecycle
  //

  /**
   * Create a new instance of the bean.
   */
  /*
  public T create()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Destroys a bean instance
   */
  /*
  public void destroy(T instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  //
  // ObjectProxy
  //

  /**
   * Returns the new object for JNDI
   */
  public Object createObject(Hashtable env)
  {
    return get();
  }

  protected ConfigException error(Method method, String msg)
  {
    return new ConfigException(LineConfigException.loc(method) + msg);
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof ComponentImpl))
      return false;

    ComponentImpl comp = (ComponentImpl) obj;

    if (! getTargetType().equals(comp.getTargetType())) {
      return false;
    }

    /*
    int size = _bindingList.size();

    if (size != comp._bindingList.size()) {
      return false;
    }

    for (int i = size - 1; i >= 0; i--) {
      if (! comp._bindingList.contains(_bindingList.get(i))) {
	return false;
      }
    }
    */

    return true;
  }

  protected static String getSimpleName(Type type)
  {
    if (type instanceof Class)
      return ((Class) type).getSimpleName();
    else
      return String.valueOf(type);
  }
}
