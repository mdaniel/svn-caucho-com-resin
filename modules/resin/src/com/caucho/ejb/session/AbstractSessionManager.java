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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
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

  private Class<?> _proxyImplClass;
  private HashMap<Class<?>, AbstractSessionContext<X,?>> _contextMap
    = new HashMap<Class<?>, AbstractSessionContext<X,?>>();

  private InjectManager _injectManager;
  private Bean<X> _bean;
  
  private String[] _declaredRoles;

  public AbstractSessionManager(EjbManager manager, 
                                AnnotatedType<X> annotatedType,
                                Class<?> proxyImplClass)
  {
    super(manager, annotatedType);
    
    _proxyImplClass = proxyImplClass;
    
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
  
  public Class<?> getProxyImplClass()
  {
    return _proxyImplClass;
  }
  
  public InjectManager getInjectManager()
  {
    return _injectManager;
  }

  @SuppressWarnings("unchecked")
  protected <T> AbstractSessionContext<X,T> getSessionContext(Class<T> api)
  {
    return (AbstractSessionContext<X,T>) _contextMap.get(api);
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
      thread.setContextClassLoader(getClassLoader());

      super.init();
      
      _injectManager = InjectManager.create();

      for (Class<?> localApi : getLocalApiList()) {
        createContext(localApi);
      }
      
      Class<?> localBean = getLocalBean();
      if (localBean != null)
        createContext(localBean);

      for (Class<?> remoteApi : getRemoteApiList()) {
        createContext(remoteApi);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    registerWebBeans();

    log.fine(this + " initialized");
  }
  
  private <T> void createContext(Class<T> api)
  {
    if (_contextMap.get(api) != null)
      throw new IllegalStateException(String.valueOf(api));
    
    AbstractSessionContext<X,T> context = createSessionContext(api);
    
    InjectManager injectManager = context.getInjectManager();
    
    BeanBuilder<SessionContext> factory
      = injectManager.createBeanFactory(SessionContext.class);
  
    context.setDeclaredRoles(_declaredRoles);

    // XXX: separate additions?
    if (injectManager.getBeans(SessionContext.class).size() == 0)
      injectManager.addBean(factory.singleton(context));
   
    _contextMap.put(context.getApi(), context);
    
    /*
    if (_sessionContext == null) {
    }
    */
  }
  
  @Override
  public <T> T getLocalProxy(Class<T> api)
  {
    return getSessionContext(api).createProxy(null);
  }
  
  protected <T> AbstractSessionContext<X,T>
  createSessionContext(Class<T> api)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected <T> SessionProxyFactory<T>
  createProxyFactory(AbstractSessionContext<X,T> context)
  {
    try {
      Class<?> []param = new Class[] { getClass(), getContextClass() };
    
      Constructor<?> ctor = _proxyImplClass.getConstructor(param);
    
      return (SessionProxyFactory<T>) ctor.newInstance(this, context);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  
  protected Class<?> getContextClass()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private void registerWebBeans()
  {
    ArrayList<Class<?>> localApiList = getLocalApiList();
    ArrayList<Class<?>> remoteApiList = getRemoteApiList();
    
    AnnotatedType<X> beanType = getAnnotatedType();

    InjectManager moduleBeanManager = InjectManager.create();

    Named named = (Named) beanType.getAnnotation(Named.class);

    if (named != null) {
    }

    ManagedBeanImpl<X> mBean 
      = getInjectManager().createManagedBean(getAnnotatedType());
    
    Class<?> baseApi = beanType.getJavaClass();
      
    Set<Type> apiList = new LinkedHashSet<Type>();

    if (hasNoInterfaceView()) {
      baseApi = baseApi;
      
      BaseType sourceApi = moduleBeanManager.createSourceBaseType(baseApi);
        
      apiList.addAll(sourceApi.getTypeClosure(moduleBeanManager));
    }
      
    if (localApiList != null) {
      for (Class<?> api : localApiList) {
        baseApi = api;
          
        BaseType sourceApi = moduleBeanManager.createSourceBaseType(api);
          
        apiList.addAll(sourceApi.getTypeClosure(moduleBeanManager));
      }
    }
      
    apiList.add(Object.class);

    if (remoteApiList != null) {
      for (Class<?> api : remoteApiList) {
        baseApi = api;
      }
    }

    _bean = (Bean<X>) createBean(mBean, baseApi, apiList);
      
    ProcessSessionBeanImpl process
      = new ProcessSessionBeanImpl(moduleBeanManager,
                                   _bean,
                                   mBean.getAnnotatedType(),
                                   getEJBName(),
                                   getSessionBeanType());

    moduleBeanManager.addBean(_bean, process);

    for (Class<?> localApi : getLocalApiList()) {
      registerLocalSession(moduleBeanManager, localApi);
    }
  }
  
  private <T> void registerLocalSession(InjectManager beanManager, 
                                        Class<T> localApi)
  {
    BaseType localBeanType = beanManager.createSourceBaseType(localApi);
      
    AbstractSessionContext<X,T> context = getSessionContext(localApi);
      
    BeanName beanName = new BeanNameLiteral(getEJBName());

    SessionRegistrationBean<X,T> regBean
      = new SessionRegistrationBean<X,T>(beanManager, context, _bean, beanName);
      
    beanManager.addBean(regBean);
  }

  protected Bean<X> getBean()
  {
    return _bean;
  }

  abstract protected <T> Bean<T>
  createBean(ManagedBeanImpl<X> mBean,
             Class<T> api,
             Set<Type> apiList);
  
  protected SessionBeanType getSessionBeanType()
  {
    return SessionBeanType.STATELESS;
  }

  @Override
  public void destroy()
  {
    for (AbstractSessionContext<X,?> context : _contextMap.values()) {
      try {
        context.destroy();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
