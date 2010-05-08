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

package com.caucho.ejb.cfg;

import static javax.ejb.TransactionAttributeType.REQUIRED;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.SessionSynchronization;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.XaAnnotation;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.ejb.gen.SessionGenerator;
import com.caucho.ejb.gen.SingletonGenerator;
import com.caucho.ejb.gen.StatefulGenerator;
import com.caucho.ejb.gen.StatelessGenerator;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.ejb.server.EjbInjectionTarget;
import com.caucho.ejb.session.AbstractSessionManager;
import com.caucho.ejb.session.SingletonManager;
import com.caucho.ejb.session.StatefulManager;
import com.caucho.ejb.session.StatelessManager;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;

/**
 * Configuration for an ejb entity bean.
 */
public class EjbSessionBean<X> extends EjbBean<X> {
  private static final L10N L = new L10N(EjbSessionBean.class);

  // Default is container managed transaction.
  private boolean _isContainerTransaction = true;

  private SessionGenerator<X> _sessionBean;

  private Class<? extends Annotation> _sessionType;

  /**
   * Creates a new session bean configuration.
   */
  public EjbSessionBean(EjbConfig ejbConfig, String ejbModuleName)
  {
    super(ejbConfig, ejbModuleName);
  }

  /**
   * Creates a new session bean configuration.
   */
  public EjbSessionBean(EjbConfig ejbConfig, 
                        AnnotatedType<X> annType,
                        String ejbModuleName)
  {
    super(ejbConfig, annType, ejbModuleName);
  }

  /**
   * Returns the kind of bean.
   */
  @Override
  public String getEJBKind()
  {
    return "session";
  }

  /**
   * Sets the ejb implementation class.
   */
  @Override
  public void setEJBClass(Class<X> type) throws ConfigException
  {
    super.setEJBClass(type);

    AnnotatedType<X> ejbClass = getAnnotatedType();
    Class<X> ejbJavaClass = ejbClass.getJavaClass();
    
    if (Modifier.isAbstract(ejbJavaClass.getModifiers()))
      throw error(L.l(
             "'{0}' must not be abstract.  Session bean implementations must be fully implemented.",
             ejbJavaClass.getName()));

    if (ejbClass.isAnnotationPresent(Stateless.class)) {
      Stateless stateless = ejbClass.getAnnotation(Stateless.class);

      if (getEJBName() == null && !"".equals(stateless.name()))
        setEJBName(stateless.name());

      _sessionType = Stateless.class;
    } else if (ejbClass.isAnnotationPresent(Stateful.class)) {
      Stateful stateful = ejbClass.getAnnotation(Stateful.class);

      if (getEJBName() == null && !"".equals(stateful.name()))
        setEJBName(stateful.name());

      _sessionType = Stateful.class;
    }

    if (getEJBName() == null)
      setEJBName(ejbJavaClass.getSimpleName());

    /*
     * if (! ejbClass.isAssignableTo(SessionBean.class) && !
     * ejbClass.isAnnotationPresent(Stateless.class) && !
     * ejbClass.isAnnotationPresent(Stateful.class)) throwerror(L.l(
     * "'{0}' must implement SessionBean or @Stateless or @Stateful.  Session beans must implement javax.ejb.SessionBean."
     * , ejbClass.getName()));
     */

    // introspectSession();
  }

  /**
   * Gets the session bean type.
   */
  public Class<? extends Annotation> getSessionType()
  {
    return _sessionType;
  }

  /**
   * Returns true if the container handles transactions.
   */
  @Override
  public boolean isContainerTransaction()
  {
    return _isContainerTransaction;
  }

  /**
   * Set true if the container handles transactions.
   */
  public void setTransactionType(String type)
    throws ConfigException
  {
    if (type.equals("Container"))
      _isContainerTransaction = true;
    else if (type.equals("Bean"))
      _isContainerTransaction = false;
    else
      throw new ConfigException(
          L.l("'{0}' is an unknown transaction-type.  transaction-type must be 'Container' or 'Bean'.",
              type));
  }

  /**
   * Configure initialization.
   */
  @PostConstruct
  public void init() throws ConfigException
  {
    super.init();

    try {
      for (AnnotatedType<?> remoteApi : getRemoteList())
        validateRemote(remoteApi);

      for (AnnotatedType<?> localApi : getLocalList())
        validateLocal(localApi);

      if (getEJBClass() == null) {
        throw error(L.l(
               "'{0}' does not have a defined ejb-class.  Session beans must have an ejb-class.",
               getEJBName()));
      }
      
      Class<X> ejbClass = getAnnotatedType().getJavaClass();

      if (! SessionSynchronization.class.isAssignableFrom(ejbClass)) {
      } else if (! Stateful.class.equals(getSessionType())) {
        throw error(L.l(
               "'{0}' must not implement SessionSynchronization.  Stateless session beans must not implement SessionSynchronization.",
               getEJBClass().getName()));
      } else if (! _isContainerTransaction) {
        throw error(L.l(
               "'{0}' must not implement SessionSynchronization.  Session beans with Bean-managed transactions may not use SessionSynchronization.",
               getEJBClass().getName()));
      }
    } catch (LineConfigException e) {
      throw e;
    } catch (ConfigException e) {
      throw new LineConfigException(getLocation() + e.getMessage(), e);
    }
  }

  /**
   * Creates the bean generator for the session bean.
   */
  @Override
  protected BeanGenerator<X> createBeanGenerator()
  {
    AnnotatedType<X> ejbClass = getAnnotatedType();

    fillClassDefaults(ejbClass);

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

  private void fillClassDefaults(AnnotatedType<X> ejbClass)
  {
    AnnotatedTypeImpl<X> ejbClassImpl = AnnotatedTypeImpl.create(ejbClass);
    
    if (!_isContainerTransaction) {
      ejbClassImpl.addAnnotation(XaAnnotation.createBeanManaged());
    }

    TransactionAttribute ann = ejbClass
        .getAnnotation(TransactionAttribute.class);

    if (ann == null) {
      // ejb/1100
      ejbClassImpl.addAnnotation(XaAnnotation.create(REQUIRED));
    }
  }

  /**
   * Obtain and apply initialization from annotations.
   */
  @Override
  public void initIntrospect() throws ConfigException
  {
    super.initIntrospect();

    AnnotatedType<X> type = getAnnotatedType();

    // XXX: ejb/0f78
    if (type == null)
      return;

    // ejb/0j20
    if (!type.isAnnotationPresent(Stateful.class)
        && !type.isAnnotationPresent(Stateless.class) 
        && !type.isAnnotationPresent(Singleton.class) 
        && !isAllowPOJO())
      return;

    /*
     * TCK: ejb/0f6d: bean with local and remote interfaces if (_localHome !=
     * null || _localList.size() != 0 || _remoteHome != null ||
     * _remoteList.size() != 0) return;
     */
    
    Class<?> ejbClass = type.getJavaClass();

    ArrayList<Class<?>> interfaceList = new ArrayList<Class<?>>();

    for (Class<?> localApi : ejbClass.getInterfaces()) {
      Local local = localApi.getAnnotation(Local.class);

      if (local != null) {
        addLocal((Class) localApi);
        continue;
      }

      javax.ejb.Remote remote = localApi.getAnnotation(javax.ejb.Remote.class);

      if (remote != null || java.rmi.Remote.class.isAssignableFrom(localApi)) {
        addRemote(localApi);
        continue;
      }

      if (localApi.getName().equals("java.io.Serializable"))
        continue;

      if (localApi.getName().equals("java.io.Externalizable"))
        continue;

      if (localApi.getName().startsWith("javax.ejb"))
        continue;

      if (localApi.getName().equals("java.rmi.Remote"))
        continue;

      if (!interfaceList.contains(localApi))
        interfaceList.add(localApi);
    }

    Local local = type.getAnnotation(Local.class);
    if (local != null && local.value() != null) {
      _localList.clear();

      for (Class<?> api : local.value()) {
        // XXX: grab from type?
        addLocal((Class) api);
      }
    }

    Remote remote = type.getAnnotation(Remote.class);
    if (remote != null && remote.value() != null) {
      _remoteList.clear();

      for (Class<?> api : remote.value()) {
        // XXX: grab from type?
        addRemote(api);
      }
    }

    // if (getLocalList().size() != 0 || getRemoteList().size() != 0) {
    if (_localList.size() != 0 || _remoteList.size() != 0) {
    } else if (interfaceList.size() == 0) {
      // Session bean no-interface view.
    } else if (interfaceList.size() != 1)
      throw new ConfigException(
          L.l(
                 "'{0}' has multiple interfaces, but none are marked as @Local or @Remote.\n{1}",
                 type.getJavaClass().getName(), interfaceList.toString()));
    else {
      addLocal((Class) interfaceList.get(0));
    }
  }

  /**
   * Deploys the bean.
   */
  @Override
  public AbstractSessionManager<X> deployServer(EjbManager ejbContainer,
                                                JavaClassGenerator javaGen)
      throws ClassNotFoundException, ConfigException
  {
    Class<?> proxyImplClass = generateProxyClass(javaGen);
    
    AbstractSessionManager<X> manager;

    if (Stateless.class.equals(getSessionType())) {
      manager = new StatelessManager<X>(ejbContainer, 
                                        getAnnotatedType(),
                                        proxyImplClass);
    }
    else if (Stateful.class.equals(getSessionType())) {
      manager = new StatefulManager<X>(ejbContainer,
                                       getAnnotatedType(),
                                       proxyImplClass);
    }
    else if (Singleton.class.equals(getSessionType())) {
      manager = new SingletonManager<X>(ejbContainer, 
                                        getAnnotatedType(),
                                        proxyImplClass);
    }
    else
      throw new IllegalStateException(String.valueOf(getSessionType()));

    manager.setModuleName(getEJBModuleName());
    manager.setEJBName(getEJBName());
    manager.setMappedName(getMappedName());
    manager.setId(getEJBModuleName() + "#" + getEJBName());
    manager.setContainerTransaction(_isContainerTransaction);

    ArrayList<AnnotatedType<? super X>> remoteList = _sessionBean.getRemoteApi();
    if (remoteList.size() > 0) {
      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
      for (AnnotatedType<?> apiClass : remoteList) {
        classList.add(loadClass(apiClass.getJavaClass().getName()));
      }

      manager.setRemoteApiList(classList);
    }

    /*
     * if (getRemote21() != null)
     * server.setRemote21(loadClass(getRemote21().getName()));
     */
    
    manager.setIsNoInterfaceView(_sessionBean.hasNoInterfaceView());

    ArrayList<AnnotatedType<? super X>> localList = _sessionBean.getLocalApi();
    if (localList.size() > 0) {
      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
      for (AnnotatedType<?> apiClass : localList) {
        classList.add(loadClass(apiClass.getJavaClass().getName()));
      }

      manager.setLocalApiList(classList);
    }
    
    if (_sessionBean.hasNoInterfaceView())
      manager.setLocalBean(getEJBClass());

    /*
     * if (getLocal21() != null)
     * server.setLocal21(loadClass(getLocal21().getName()));
     */

    // Class<?> contextImplClass = javaGen.loadClassParentLoader(getSkeletonName());

    /*
    Class<?>[] classes = contextImplClass.getDeclaredClasses();

    for (Class<?> aClass : classes) {
      if (getEJBClass().isAssignableFrom(aClass)) {
        manager.setBeanImplClass((Class<X>) aClass);

        break;
      }
    }
    */

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(manager.getClassLoader());

      manager.setInjectionTarget(getInjectionTarget());

      manager.setInitProgram(getInitProgram());

      try {
        if (getServerProgram() != null)
          getServerProgram().configure(manager);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return manager;
  }

  private Class<?> generateProxyClass(JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    Class<?> proxyImplClass;
  
    if (Modifier.isPublic(getEJBClass().getModifiers())) {
      proxyImplClass = javaGen.loadClass(getSkeletonName());
    }
    else {
      // ejb/1103
      proxyImplClass = javaGen.loadClassParentLoader(getSkeletonName(), getEJBClass());
    }
    // contextImplClass.getDeclaredConstructors();
    
    return proxyImplClass;
  }

  /**
   * @return Type of bean (Stateful, Stateless, etc.)
   */
  @Override
  protected String getBeanType()
  {
    return getSessionType().getSimpleName();
  }
}
