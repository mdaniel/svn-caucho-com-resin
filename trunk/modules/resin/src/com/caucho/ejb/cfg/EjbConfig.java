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

import com.caucho.config.ConfigException;
import com.caucho.config.gen.ApplicationExceptionConfig;
import com.caucho.config.types.FileSetType;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.jms.JmsMessageListener;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.MessageDriven;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * Manages the EJB configuration files.
 */
public class EjbConfig {
  private static final L10N L = new L10N(EjbConfig.class);
  private static final Logger log
    = Logger.getLogger(EjbConfig.class.getName());

  private final EjbContainer _ejbContainer;

  private ArrayList<FileSetType> _fileSetList = new ArrayList<FileSetType>();

  private HashMap<String,EjbBean> _cfgBeans = new HashMap<String,EjbBean>();

  private ArrayList<EjbBean> _pendingBeans = new ArrayList<EjbBean>();
  private ArrayList<EjbBean> _deployingBeans = new ArrayList<EjbBean>();

  private ArrayList<EjbBeanConfigProxy> _proxyList
    = new ArrayList<EjbBeanConfigProxy>();

  private ArrayList<FunctionSignature> _functions
    = new ArrayList<FunctionSignature>();

  private String _booleanTrue = "1";
  private String _booleanFalse = "0";

  private boolean _isAllowPOJO;
  private HashMap<String, MessageDestination> _messageDestinations;

  private ArrayList<Interceptor> _cfgInterceptors
    = new ArrayList<Interceptor>();

  private ArrayList<InterceptorBinding> _cfgInterceptorBindings
    = new ArrayList<InterceptorBinding>();

  private ArrayList<ApplicationExceptionConfig> _cfgApplicationExceptions
    = new ArrayList<ApplicationExceptionConfig>();

  public EjbConfig(EjbContainer ejbContainer)
  {
    _ejbContainer = ejbContainer;
  }

  /**
   * Adds a path for an EJB config file to the config list.
   */
  public void addFileSet(FileSetType fileSet)
  {
    if (_fileSetList.contains(fileSet))
      return;

    _fileSetList.add(fileSet);
    
    for (Path path : fileSet.getPaths()) {
      addEjbPath(path);
    }
  }

  /**
   * Adds a path for an EJB config file to the config list.
   */
  public void addEjbPath(Path path)
    throws ConfigException
  {
    throw new UnsupportedOperationException();
  }

  public void addProxy(EjbBeanConfigProxy proxy)
  {
    _proxyList.add(proxy);
  }

  /**
   * Returns the schema name.
   */
  public String getSchema()
  {
    return "com/caucho/ejb/cfg/resin-ejb.rnc";
  }


  /**
   * Returns the EJB manager.
   */
  public EjbContainer getEjbContainer()
  {
    return _ejbContainer;
  }

  /**
   * Sets the boolean true literal.
   */
  public void setBooleanTrue(String trueLiteral)
  {
    _booleanTrue = trueLiteral;
  }

  /**
   * Gets the boolean true literal.
   */
  public String getBooleanTrue()
  {
    return _booleanTrue;
  }

  /**
   * Sets the boolean false literal.
   */
  public void setBooleanFalse(String falseLiteral)
  {
    _booleanFalse = falseLiteral;
  }

  /**
   * Gets the boolean false literal.
   */
  public String getBooleanFalse()
  {
    return _booleanFalse;
  }

  /**
   * Returns the cfg bean with the given name.
   */
  public EjbBean getBeanConfig(String name)
  {
    assert name != null;

    return _cfgBeans.get(name);
  }

  /**
   * Sets the cfg bean with the given name.
   */
  public void setBeanConfig(String name, EjbBean bean)
  {
    if (name == null || bean == null)
      throw new NullPointerException();

    EjbBean oldBean = _cfgBeans.get(name);

    if (oldBean == bean)
      return;
    else if (oldBean != null) {
      throw new IllegalStateException(L.l("{0}: duplicate bean '{1}' old ejb-class={2} new ejb-class={3}",
					  this, name,
					  oldBean.getEJBClass().getName(),
					  bean.getEJBClass().getName()));
    }

    _pendingBeans.add(bean);
    _cfgBeans.put(name, bean);
  }

  /**
   * Returns the interceptor with the given class name.
   */
  public Interceptor getInterceptor(String className)
  {
    assert className != null;

    for (Interceptor interceptor : _cfgInterceptors) {
      if (interceptor.getInterceptorClass().equals(className))
        return interceptor;
    }

    return null;
  }

  /**
   * Adds an interceptor.
   */
  public void addInterceptor(Interceptor interceptor)
  {
    if (interceptor == null)
      throw new NullPointerException();

    _cfgInterceptors.add(interceptor);
  }

  /**
   * Returns the interceptor bindings for a given ejb name.
   */
  public InterceptorBinding getInterceptorBinding(String ejbName,
                                                  boolean isExcludeDefault)
  {
    assert ejbName != null;

    for (InterceptorBinding binding : _cfgInterceptorBindings) {
      if (binding.getEjbName().equals(ejbName))
        return binding;
    }

    // ejb/0fbe vs ejb/0fbf
    for (InterceptorBinding binding : _cfgInterceptorBindings) {
      if (binding.getEjbName().equals("*")) {
        if (isExcludeDefault)
          continue;

        return binding;
      }
    }

    return null;
  }

  /**
   * Adds an application exception.
   */
  public void addApplicationException(ApplicationExceptionConfig applicationException)
  {
    _cfgApplicationExceptions.add(applicationException);
  }

  /**
   * Returns the application exceptions.
   */
  public ArrayList<ApplicationExceptionConfig> getApplicationExceptions()
  {
    return _cfgApplicationExceptions;
  }

  /**
   * Binds an interceptor to an ejb.
   */
  public void addInterceptorBinding(InterceptorBinding interceptorBinding)
  {
    _cfgInterceptorBindings.add(interceptorBinding);
  }

  /**
   * Adds the message destination mapping
   */
  public void addMessageDestination(MessageDestination messageDestination)
  {
    if (_messageDestinations == null)
      _messageDestinations = new HashMap<String, MessageDestination>();

    String name = messageDestination.getMessageDestinationName();

    _messageDestinations.put(name, messageDestination);
  }

  public MessageDestination getMessageDestination(String name)
  {
    if (_messageDestinations == null)
      return null;

    return _messageDestinations.get(name);
  }

  /**
   * Sets true if POJO are allowed.
   */
  public void setAllowPOJO(boolean allowPOJO)
  {
    _isAllowPOJO = allowPOJO;
  }

  /**
   * Return true if POJO are allowed.
   */
  public boolean isAllowPOJO()
  {
    return _isAllowPOJO;
  }

  public void addIntrospectableClass(String className)
  {
    try {
      ClassLoader tempLoader = _ejbContainer.getIntrospectionClassLoader();
      ClassLoader loader = _ejbContainer.getClassLoader();

      // ejb/0f20
      Class type = Class.forName(className, false, loader);

      if (findBeanByType(type) != null)
	return;

      if (type.isAnnotationPresent(javax.ejb.Stateless.class)) {
	EjbStatelessBean bean = new EjbStatelessBean(this, "resin-ejb");
	bean.setEJBClass(type);
	bean.setAllowPOJO(true);

	setBeanConfig(bean.getEJBName(), bean);
      }
      else if (type.isAnnotationPresent(javax.ejb.Stateful.class)) {
	EjbStatefulBean bean = new EjbStatefulBean(this, "resin-ejb");
	bean.setAllowPOJO(true);
	bean.setEJBClass(type);
		
	setBeanConfig(bean.getEJBName(), bean);
      }
      else if (type.isAnnotationPresent(javax.ejb.MessageDriven.class)) {
	EjbMessageBean bean = new EjbMessageBean(this, "resin-ejb");
	bean.setAllowPOJO(true);
	bean.setEJBClass(type);
	
	setBeanConfig(bean.getEJBName(), bean);
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void addAnnotatedType(AnnotatedType annType,
			       InjectionTarget injectTarget)
  {
    try {
      ClassLoader loader = _ejbContainer.getIntrospectionClassLoader();

      Class type = annType.getJavaClass();

      if (findBeanByType(type) != null)
	return;

      if (annType.isAnnotationPresent(Stateless.class)) {
	Stateless stateless = annType.getAnnotation(Stateless.class);
	
	EjbStatelessBean bean = new EjbStatelessBean(this, annType, stateless);
	bean.setInjectionTarget(injectTarget);

	setBeanConfig(bean.getEJBName(), bean);
      }
      else if (annType.isAnnotationPresent(Stateful.class)) {
	Stateful stateful = annType.getAnnotation(Stateful.class);
	
	EjbStatefulBean bean = new EjbStatefulBean(this, annType, stateful);
	bean.setInjectionTarget(injectTarget);

	setBeanConfig(bean.getEJBName(), bean);
      }
      else if (annType.isAnnotationPresent(Singleton.class)) {
        Singleton singleton = annType.getAnnotation(Singleton.class);
        
        EjbSingletonBean bean = new EjbSingletonBean(this, annType, singleton);
        bean.setInjectionTarget(injectTarget);

        setBeanConfig(bean.getEJBName(), bean);
      }      
      else if (annType.isAnnotationPresent(MessageDriven.class)) {
	MessageDriven message = annType.getAnnotation(MessageDriven.class);
	EjbMessageBean bean = new EjbMessageBean(this, annType, message);
	bean.setInjectionTarget(injectTarget);

	setBeanConfig(bean.getEJBName(), bean);
      }
      else if (annType.isAnnotationPresent(JmsMessageListener.class)) {
	JmsMessageListener listener
	  = annType.getAnnotation(JmsMessageListener.class);
	
	EjbMessageBean bean = new EjbMessageBean(this, annType,
						 listener.destination());
	bean.setInjectionTarget(injectTarget);

	setBeanConfig(bean.getEJBName(), bean);
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Finds an entity bean by its abstract schema.
   */
  public EjbBean findBeanByType(Class type)
  {
    for (EjbBean bean : _cfgBeans.values()) {
      // ejb/0j03
      if (type.getName().equals(bean.getEJBClass().getName()))
	return bean;
    }

    return null;
  }

  /**
   * Adds a function.
   */
  public void addFunction(FunctionSignature sig, String sql)
  {
    _functions.add(sig);
  }

  /**
   * Gets the function list.
   */
  public ArrayList<FunctionSignature> getFunctions()
  {
    return _functions;
  }

  /**
   * Configures the pending beans.
   */
  public void configure()
    throws ConfigException
  {
    findConfigurationFiles();

    try {
      ArrayList<EjbBean> beanConfig = new ArrayList<EjbBean>(_pendingBeans);
      _pendingBeans.clear();

      _deployingBeans.addAll(beanConfig);

      EnvironmentClassLoader parentLoader = _ejbContainer.getClassLoader();

      Path workDir = _ejbContainer.getWorkDir();

      JavaClassGenerator javaGen = new JavaClassGenerator();
      // need to be compatible with enhancement
      javaGen.setWorkDir(workDir);
      javaGen.setParentLoader(parentLoader);

      configureRelations();

      for (EjbBeanConfigProxy proxy : _proxyList) {
        EjbBean bean = _cfgBeans.get(proxy.getEJBName());

        if (bean != null)
          proxy.getBuilderProgram().configure(bean);
      }

      for (EjbBean bean : beanConfig) {
        bean.init();
      }

      // Collections.sort(beanConfig, new BeanComparator());

      for (EjbBean bean : beanConfig) {
        bean.generate(javaGen, _ejbContainer.isAutoCompile());
      }

      javaGen.compilePendingJava();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }


  /**
   * Configures the pending beans.
   */
  private void findConfigurationFiles()
    throws ConfigException
  {
    for (FileSetType fileSet : _fileSetList) {
      for (Path path : fileSet.getPaths()) {
        addEjbPath(path);
      }
    }
  }

  /**
   * Configures the pending beans.
   */
  public void deploy()
    throws ConfigException
  {
    try {
      ClassLoader parentLoader = _ejbContainer.getClassLoader();

      Path workDir = _ejbContainer.getWorkDir();

      JavaClassGenerator javaGen = new JavaClassGenerator();
      javaGen.setWorkDir(workDir);
      javaGen.setParentLoader(parentLoader);

      ArrayList<EjbBean> deployingBeans
	= new ArrayList<EjbBean>(_deployingBeans);
      _deployingBeans.clear();

      deployBeans(deployingBeans, javaGen);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Configures the pending beans.
   */
  public void deployBeans(ArrayList<EjbBean> beanConfig,
                          JavaClassGenerator javaGen)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_ejbContainer.getClassLoader());

      // ejb/0g1c, ejb/0f68, ejb/0f69
      ArrayList<EjbBean> beanList = new ArrayList<EjbBean>();

      for (EjbBean bean : beanConfig) {
        if (beanList.contains(bean))
          continue;

        AbstractServer server = initBean(bean, javaGen);
        ArrayList<String> dependList = bean.getBeanDependList();

        for (String depend : dependList) {
          for (EjbBean b : beanConfig) {
            if (bean == b)
              continue;

            if (depend.equals(b.getEJBName())) {
              beanList.add(b);

              AbstractServer dependServer = initBean(b, javaGen);

              initResources(b, dependServer);

              thread.setContextClassLoader(server.getClassLoader());
            }
          }
        }

        initResources(bean, server);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private AbstractServer initBean(EjbBean bean, JavaClassGenerator javaGen)
    throws Exception
  {
    AbstractServer server = bean.deployServer(_ejbContainer, javaGen);

    server.init();

    return server;
  }

  private void initResources(EjbBean bean, AbstractServer server)
    throws Exception
  {
    /*
    for (ResourceEnvRef ref : bean.getResourceEnvRefs())
      ref.initBinding(server);

    // XXX TCK, needs QA probably ejb/0gc4 ejb/0gc5
    for (EjbLocalRef ref : bean.getEjbLocalRefs())
      ref.initBinding(server);
      */
    _ejbContainer.addServer(server);
  }

  /**
   * Match up the relations.
   */
  protected void configureRelations()
    throws ConfigException
  {
  }

  public String toString()
  {
    String id = _ejbContainer.getClassLoader().getId();
    
    return getClass().getSimpleName() + "[" + id + "]";
  }
}
