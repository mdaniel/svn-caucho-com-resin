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

package com.caucho.ejb.session;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.SessionBeanType;
import javax.inject.Named;

import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.j2ee.BeanName;
import com.caucho.config.j2ee.BeanNameLiteral;
import com.caucho.config.reflect.BaseType;
import com.caucho.ejb.SessionPool;
import com.caucho.ejb.inject.ProcessSessionBeanImpl;
import com.caucho.ejb.inject.SessionRegistrationBean;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.ejb.server.AbstractEjbBeanManager;

/**
 * Server container for a session bean.
 */
abstract public class AbstractSessionManager<X> extends AbstractEjbBeanManager<X> {
  private final static Logger log
     = Logger.getLogger(AbstractSessionManager.class.getName());

  private HashMap<Class<?>, InjectionTarget<?>> _componentMap
    = new HashMap<Class<?>, InjectionTarget<?>>();
  
  private SessionContext _sessionContext;

  private Bean<X> _bean;
  
  private int _sessionIdleMax = 16;
  private int _sessionConcurrentMax = -1;
  private long _sessionConcurrentTimeout = -1;
  
  private String[] _declaredRoles;

  public AbstractSessionManager(EjbManager manager, 
                                AnnotatedType<X> annotatedType)
  {
    super(manager, annotatedType);
    
    DeclareRoles declareRoles 
      = annotatedType.getJavaClass().getAnnotation(DeclareRoles.class);

    RolesAllowed rolesAllowed 
      = annotatedType.getJavaClass().getAnnotation(RolesAllowed.class); 
    
    if (declareRoles != null && rolesAllowed != null) {
      _declaredRoles = new String[declareRoles.value().length +
                                  rolesAllowed.value().length];

      System.arraycopy(declareRoles.value(), 0, 
                       _declaredRoles, 0, 
                       declareRoles.value().length);

      System.arraycopy(rolesAllowed.value(), 0, 
                       _declaredRoles, declareRoles.value().length, 
                       rolesAllowed.value().length);
    }
    else if (declareRoles != null) {
      _declaredRoles = declareRoles.value();
    }
    else if (rolesAllowed != null) {
      _declaredRoles = rolesAllowed.value();
    }
  }

  @Override
  protected String getType()
  {
    return "session:";
  }

  @Override
  public Bean<X> getDeployBean()
  {
    return _bean;
  }
  
  public int getSessionIdleMax()
  {
    return _sessionIdleMax;
  }
  
  public int getSessionConcurrentMax()
  {
    return _sessionConcurrentMax;
  }
  
  public long getSessionConcurrentTimeout()
  {
    return _sessionConcurrentTimeout;
  }

  /**
   * Initialize the server
   */
  @Override
  public void init() throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      super.init();

      InjectManager beanManager = InjectManager.create(_loader);
      
      AnnotatedType<?> annType = getAnnotatedType();
      SessionPool sessionPool = annType.getAnnotation(SessionPool.class);
      
      if (sessionPool != null) {
        if (sessionPool.maxIdle() >= 0)
          _sessionIdleMax = sessionPool.maxIdle();
        
        if (sessionPool.maxConcurrent() >= 0)
          _sessionConcurrentMax = sessionPool.maxConcurrent();
        
        if (sessionPool.maxConcurrentTimeout() >= 0)
          _sessionConcurrentTimeout = sessionPool.maxConcurrentTimeout();
        
      }

      if (_sessionContext == null) {
        AbstractContext context = getContext();
        _sessionContext = (SessionContext) context;
        
        BeanBuilder<SessionContext> factory
        = beanManager.createBeanFactory(SessionContext.class);
      
        context.setDeclaredRoles(_declaredRoles);

        beanManager.addBean(factory.singleton(context));
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    registerWebBeans();

    log.fine(this + " initialized");
  }

  private void registerWebBeans()
  {
    Class<?> beanClass = getBeanSkelClass();
    ArrayList<Class<?>> localApiList = getLocalApiList();
    ArrayList<Class<?>> remoteApiList = getRemoteApiList();

    if (beanClass != null 
        && (isNoInterfaceView() 
            || localApiList != null 
            || remoteApiList != null)) {
      InjectManager beanManager = InjectManager.create();

      Named named = (Named) beanClass.getAnnotation(Named.class);

      if (named != null) {
      }

      ManagedBeanImpl<X> mBean = beanManager.createManagedBean(getAnnotatedType());

      Class<?> baseApi = beanClass;
      
      Set<Type> apiList = new LinkedHashSet<Type>();

      if (isNoInterfaceView()) {
        baseApi = _ejbClass;
        
        BaseType sourceApi = beanManager.createSourceBaseType(_ejbClass);
        
        apiList.addAll(sourceApi.getTypeClosure(beanManager));
      }
      
      if (localApiList != null) {
        for (Class<?> api : localApiList) {
          baseApi = api;
          
          BaseType sourceApi = beanManager.createSourceBaseType(api);
          
          apiList.addAll(sourceApi.getTypeClosure(beanManager));
        }
      }
      
      apiList.add(Object.class);

      if (remoteApiList != null) {
        for (Class<?> api : remoteApiList) {
          baseApi = api;
        }
      }

      _bean = createBean(mBean, baseApi, apiList);
      
      ProcessSessionBeanImpl process
        = new ProcessSessionBeanImpl(beanManager,
                                     _bean,
                                     mBean.getAnnotatedType(),
                                     getEJBName(),
                                     getSessionBeanType());

      beanManager.addBean(_bean, process);
      
      BeanName beanName = new BeanNameLiteral(getEJBName());

      SessionRegistrationBean regBean
        = new SessionRegistrationBean(beanManager, _bean, beanName);
      
      beanManager.addBean(regBean);
      
      /*
       * if (remoteApiList != null) { for (Class api : remoteApiList) {
       * factory.type(api); } }
       */

      // XXX: component
      // beanManager.addBean(factory.bean());
    }
  }

  protected Bean<X> getBean()
  {
    return _bean;
  }

  abstract protected Bean<X> createBean(ManagedBeanImpl<X> mBean,
                                        Class<?> api,
                                        Set<Type> apiList);
  
  protected SessionBeanType getSessionBeanType()
  {
    return SessionBeanType.STATELESS;
  }
  
  abstract protected <T> InjectionTarget<T> createSessionComponent(Class<T> api,
                                                                   Class<X> beanClass);

  protected <T> InjectionTarget<T> getComponent(Class<T> api)
  {
    return (InjectionTarget<T>) _componentMap.get(api);
  }

  @Override
  public AbstractSessionContext getContext()
  {
    return null;
  }
}
