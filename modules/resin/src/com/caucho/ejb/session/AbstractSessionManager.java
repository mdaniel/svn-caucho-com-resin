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
import java.lang.reflect.Modifier;
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
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.SessionBeanType;
import javax.inject.Named;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.j2ee.BeanName;
import com.caucho.config.j2ee.BeanNameLiteral;
import com.caucho.config.reflect.BaseType;
import com.caucho.ejb.SessionPool;
import com.caucho.ejb.cfg.EjbLazyGenerator;
import com.caucho.ejb.gen.SingletonGenerator;
import com.caucho.ejb.gen.StatefulGenerator;
import com.caucho.ejb.gen.StatelessGenerator;
import com.caucho.ejb.inject.ProcessSessionBeanImpl;
import com.caucho.ejb.inject.SessionRegistrationBean;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.java.gen.JavaClassGenerator;

/**
 * Server container for a session bean.
 */
abstract public class AbstractSessionManager<X> extends AbstractEjbBeanManager<X> {
  private final static Logger log
     = Logger.getLogger(AbstractSessionManager.class.getName());

  private EjbLazyGenerator<X> _lazyGenerator;
  
  private Class<?> _proxyImplClass;
  private HashMap<Class<?>, AbstractSessionContext<X,?>> _contextMap
    = new HashMap<Class<?>, AbstractSessionContext<X,?>>();

  private InjectManager _injectManager;
  private Bean<X> _bean;
  
  private String[] _declaredRoles;

  public AbstractSessionManager(EjbManager manager, 
                                AnnotatedType<X> annotatedType,
                                EjbLazyGenerator<X> lazyGenerator)
  {
    super(manager, annotatedType);
    
    _lazyGenerator = lazyGenerator;
    
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
  
  @Override
  public InjectManager getInjectManager()
  {
    return _injectManager;
  }
  
  protected EjbLazyGenerator<X> getLazyGenerator()
  {
    return _lazyGenerator;
  }
  
  @Override
  public ArrayList<AnnotatedType<? super X>> getLocalApi()
  {
    return _lazyGenerator.getLocalApi();
  }

  @SuppressWarnings("unchecked")
  protected <T> AbstractSessionContext<X,T> getSessionContext(Class<T> api)
  {
    return (AbstractSessionContext<X,T>) _contextMap.get(api);
  }

  /**
   * Initialize the server during the config phase.
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

      for (AnnotatedType<? super X> localApi : _lazyGenerator.getLocalApi()) {
        createContext(localApi.getJavaClass());
      }
      
      AnnotatedType<X> localBean = _lazyGenerator.getLocalBean();
      if (localBean != null)
        createContext(localBean.getJavaClass());
      
      for (AnnotatedType<? super X> remoteApi : _lazyGenerator.getRemoteApi()) {
        createContext(remoteApi.getJavaClass());
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    registerCdiBeans();

    log.fine(this + " initialized");
  }
  
  public void bind()
  {
    try {
      boolean isAutoCompile = true;
    
      BeanGenerator<X> beanGen = createBeanGenerator();

      beanGen.introspect();
    
      JavaClassGenerator javaGen = getLazyGenerator().getJavaClassGenerator();
    
      String fullClassName = beanGen.getFullClassName();

      if (javaGen.preload(fullClassName) != null) {
      }
      else if (isAutoCompile) {
        javaGen.generate(beanGen);

        /*
        GenClass genClass = assembleGenerator(fullClassName);

        if (genClass != null)
          javaGen.generate(genClass);
        */
      }
      
      javaGen.compilePendingJava();
      
      _proxyImplClass = generateProxyClass(fullClassName, javaGen);
     
      for (AbstractSessionContext<X,?> cxt : _contextMap.values()) {
        cxt.bind();
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Creates the bean generator for the session bean.
   */
  protected BeanGenerator<X> createBeanGenerator()
  {
    throw new UnsupportedOperationException();
  }
  /*
    AnnotatedType<X> ejbClass = getAnnotatedType();

    // fillClassDefaults(ejbClass);

    if (Stateless.class.equals(getSessionType())) {
      _sessionBean = new StatelessGenerator<X>(getEJBName(), ejbClass,
                                               getLocalList(), getRemoteList());
    } else if (Stateful.class.equals(getSessionType())) {
      _sessionBean = new StatefulGenerator<X>(getEJBName(), ejbClass,
                                              getLocalList(), getRemoteList());
    } else if (Singleton.class.equals(getSessionType())){
      _sessionBean = new SingletonGenerator<X>(getEJBName(), ejbClass,
                                               getLocalList(), getRemoteList());
    }

    return _sessionBean;
  }
  */
  
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

  private Class<?> generateProxyClass(String skeletonName,
                                      JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    Class<?> proxyImplClass;
    
    Class<?> ejbClass = getAnnotatedType().getJavaClass();
  
    if (Modifier.isPublic(ejbClass.getModifiers())) {
      proxyImplClass = javaGen.loadClass(skeletonName);
    }
    else {
      // ejb/1103
      proxyImplClass = javaGen.loadClassParentLoader(skeletonName, ejbClass);
    }
    // contextImplClass.getDeclaredConstructors();
    
    return proxyImplClass;
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
      if (_proxyImplClass == null)
        bind();
      
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

  private void registerCdiBeans()
  {
    ArrayList<AnnotatedType<? super X>> localApiList = getLocalApi();
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
      for (AnnotatedType<? super X> api : localApiList) {
        baseApi = api.getJavaClass();
          
        BaseType sourceApi = moduleBeanManager.createSourceBaseType(api.getJavaClass());
          
        apiList.addAll(sourceApi.getTypeClosure(moduleBeanManager));
      }
    }
      
    apiList.add(Object.class);

    if (remoteApiList != null) {
      for (Class<?> api : remoteApiList) {
        baseApi = api;
      }
    }
    
    if (baseApi == null)
      throw new NullPointerException();

    _bean = (Bean<X>) createBean(mBean, baseApi, apiList);
      
    ProcessSessionBeanImpl process
      = new ProcessSessionBeanImpl(moduleBeanManager,
                                   _bean,
                                   mBean.getAnnotatedType(),
                                   getEJBName(),
                                   getSessionBeanType());

    moduleBeanManager.addBean(_bean, process);

    for (AnnotatedType<?> localApi : getLocalApi()) {
      registerLocalSession(moduleBeanManager, localApi.getJavaClass());
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
