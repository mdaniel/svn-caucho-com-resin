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

package com.caucho.ejb.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.SessionContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Named;

import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.manager.EjbContainer;

/**
 * Server container for a session bean.
 */
abstract public class SessionServer extends AbstractServer {
  private final static Logger log = Logger.getLogger(SessionServer.class
      .getName());

  @SuppressWarnings("unchecked")
  private HashMap<Class, InjectionTarget> _componentMap = new HashMap<Class, InjectionTarget>();

  @SuppressWarnings("unchecked")
  private Bean _bean;

  @SuppressWarnings("unchecked")
  public SessionServer(EjbContainer manager, AnnotatedType annotatedType)
  {
    super(manager, annotatedType);
  }

  @Override
  protected String getType()
  {
    return "session:";
  }

  @SuppressWarnings("unchecked")
  @Override
  public Bean getDeployBean()
  {
    return _bean;
  }

  /**
   * Initialize the server
   */
  @SuppressWarnings("unchecked")
  @Override
  public void init() throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      super.init();

      InjectManager beanManager = InjectManager.create();

      BeanFactory factory
        = beanManager.createBeanFactory(SessionContext.class);

      _component = factory.singleton(getSessionContext());

      beanManager.addBean(_component);

      if (_localHomeClass != null)
        _localHome = (EJBLocalHome) getLocalObject(_localHomeClass);
      if (_remoteHomeClass != null)
        _remoteHome = (EJBHome) getRemoteObject(_remoteHomeClass);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    registerWebBeans();

    log.fine(this + " initialized");
  }

  @SuppressWarnings("unchecked")
  private void registerWebBeans()
  {
    Class beanClass = getBeanSkelClass();
    ArrayList<Class> localApiList = getLocalApiList();
    ArrayList<Class> remoteApiList = getRemoteApiList();

    if (beanClass != null && (localApiList != null || remoteApiList != null)) {
      InjectManager beanManager = InjectManager.create();

      String beanName = getEJBName();
      Named named = (Named) beanClass.getAnnotation(Named.class);

      if (named != null)
        beanName = named.value();

      ManagedBeanImpl mBean = beanManager.createManagedBean(getAnnotatedType());

      Class baseApi = beanClass;

      if (localApiList != null) {
        for (Class api : localApiList) {
          baseApi = api;
        }
      }

      if (remoteApiList != null) {
        for (Class api : remoteApiList) {
          baseApi = api;
        }
      }

      _bean = createBean(mBean, baseApi);

      beanManager.addBean(_bean);

      /*
       * if (remoteApiList != null) { for (Class api : remoteApiList) {
       * factory.type(api); } }
       */

      // XXX: component
      // beanManager.addBean(factory.bean());
    }
  }

  protected Bean getBean()
  {
    return _bean;
  }

  @SuppressWarnings("unchecked")
  protected Bean createBean(ManagedBeanImpl mBean, Class api)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected void bindInjection()
  {
    super.bindInjection();

    /*
     * for (ComponentImpl comp : _componentMap.values()) { comp.bind(); }
     */
  }

  @SuppressWarnings("unchecked")
  abstract protected InjectionTarget createSessionComponent(Class api,
      Class beanClass);

  @SuppressWarnings("unchecked")
  protected InjectionTarget getComponent(Class api)
  {
    return _componentMap.get(api);
  }

  /**
   * Returns the object key from a handle.
   */
  @SuppressWarnings("unchecked")
  @Override
  public Class getPrimaryKeyClass()
  {
    return null;
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  @Override
  public EJBLocalHome getEJBLocalHome()
  {
    return _localHome;
  }

  @Override
  public AbstractContext getContext()
  {
    return getSessionContext();
  }
}
