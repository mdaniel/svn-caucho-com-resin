/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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

package com.caucho.v5.http.webapp;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.program.PropertyValueProgram;
import com.caucho.v5.config.types.DescriptionGroupConfig;
import com.caucho.v5.util.L10N;

import io.baratine.inject.InjectManager;

/**
 * Configuration for the listener
 */
@Configurable
public class ListenerConfig<T> extends DescriptionGroupConfig {
  static L10N L = new L10N(ListenerConfig.class);

  // The listener class
  private Class<T> _listenerClass;

  // The listener object
  private T _object;

  private ContainerProgram _init;

  private InjectManager _beanManager;

  //private Bean<T> _bean;

  //private InjectionTarget<T> _target;

  /**
   * Sets the listener class.
   */
  public void setListenerClass(Class<T> cl)
    throws ConfigException
  {
    ConfigContext.checkCanInstantiate(cl);

    if (ServletContextListener.class.isAssignableFrom(cl)) {
    }
    else if (ServletContextAttributeListener.class.isAssignableFrom(cl)) {
    }
    else if (ServletRequestListener.class.isAssignableFrom(cl)) {
    }
    else if (ServletRequestAttributeListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionAttributeListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionActivationListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionIdListener.class.isAssignableFrom(cl)) {
    }
    else
      throw new ConfigException(L.l("listener-class '{0}' does not implement any web-app listener interface.",
                                    cl.getName()));

    _listenerClass = cl;
  }

  /**
   * Gets the listener class.
   */
  public Class<?> getListenerClass()
  {
    return _listenerClass;
  }

  /**
   * Sets the init block
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init block
   */
  public ContainerProgram getInit()
  {
    return _init;
  }
  
  public void setProperty(String name, Object value)
  {
    if (_init == null) {
      _init = new ContainerProgram();
    }
    
    _init.addProgram(new PropertyValueProgram(name, value));
  }

  /**
   * Initialize.
   */
  public Object createListenerObject()
    throws Exception
  {
    if (_object != null)
      return _object;

    InjectManager cdiManager = getInjectManager();

    _object = cdiManager.lookup(_listenerClass);
    
    /*
    Bean<T> bean = getBean();

    if (bean == null) {
    */
      /*
      _target = cdiManager.discoverInjectionTarget(_listenerClass);

      CreationalContext<T> env = cdiManager.createCreationalContext(null);

      _object = _target.produce(env);
      _target.inject(_object, env);
      */
    /*
      bean = cdiManager.createTransientBean(_listenerClass);
      
    }
    */
    //else {
      // ioc/0p2b
    //  CreationalContext<T> env = cdiManager.createCreationalContext(null);

    //  _object = bean.create(env);
    //}
    /*
    _target = cdiManager.discoverInjectionTarget(_listenerClass);

    CreationalContext<T> env = cdiManager.createCreationalContext(null);

    _object = _target.produce(env);
    _target.inject(_object, env);
    */
    //XXX: Augment the Bean with the _init program (jsp/18n2).
    if (_init != null) {
      // jsp/18n2
      _init.configure(_object);

      _init.init(_object);
    }

    /*
    if (_target != null) {
      _target.postConstruct(_object);
    }
    */

    return _object;
  }

  public void destroy()
  {
    //Bean<T> bean = getBean();

    T listener = _object;
    _object = null;

    /*
    if (bean != null) {
      bean.destroy(listener, null);
    }
    */
    
    /*
    if (_target != null) {
      _target.preDestroy(listener);
    }
    */
  }

  private InjectManager getInjectManager()
  {
    if (_beanManager == null)
      _beanManager = InjectManager.current();

    return _beanManager;
  }

  /*
  private Bean<T> getBean()
  {
    if (_bean != null)
      return _bean;

    BeanManager manager = getBeanManager();

    Set beans = manager.getBeans(_listenerClass);

    _bean = (Bean<T>) manager.resolve(beans);

    return _bean;
  }
  */

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listenerClass + "]";
  }
}
