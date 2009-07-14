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

package com.caucho.ejb.cfg;

import com.caucho.config.program.ContainerProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.types.EnvEntry;
import com.caucho.ejb.AbstractServer;
import com.caucho.config.gen.ApiClass;
import com.caucho.config.gen.ApiMethod;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.XaAnnotation;
import com.caucho.ejb.gen.SessionGenerator;
import com.caucho.ejb.gen.StatefulGenerator;
import com.caucho.ejb.gen.StatelessGenerator;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.session.StatefulServer;
import com.caucho.ejb.session.StatelessServer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.lang.reflect.*;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import static javax.ejb.TransactionAttributeType.*;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 * Configuration for an ejb entity bean.
 */
public class EjbSessionBean extends EjbBean {
  private static final L10N L = new L10N(EjbSessionBean.class);

  private boolean _isStateless;

  // Default is container managed transaction.
  private boolean _isContainerTransaction = true;

  private SessionGenerator _sessionBean;
  
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
			AnnotatedType annType,
			String ejbModuleName)
  {
    super(ejbConfig, annType, ejbModuleName);
  }

  /**
   * Returns the kind of bean.
   */
  public String getEJBKind()
  {
    return "session";
  }

  /**
   * Sets the ejb implementation class.
   */
  @Override
  public void setEJBClass(Class type)
    throws ConfigException
  {
    super.setEJBClass(type);

    ApiClass ejbClass = getEJBClassWrapper();

    if (ejbClass.isAbstract())
      throw error(L.l("'{0}' must not be abstract.  Session bean implementations must be fully implemented.", ejbClass.getName()));

    if (ejbClass.isAnnotationPresent(Stateless.class)) {
      Stateless stateless = ejbClass.getAnnotation(Stateless.class);

      if (getEJBName() == null && ! "".equals(stateless.name()))
	setEJBName(stateless.name());
      
      _isStateless = true;
    }
    else if (ejbClass.isAnnotationPresent(Stateful.class)) {
      Stateful stateful = ejbClass.getAnnotation(Stateful.class);

      if (getEJBName() == null && ! "".equals(stateful.name()))
	setEJBName(stateful.name());
      
      _isStateless = false;
    }

    if (getEJBName() == null)
      setEJBName(ejbClass.getSimpleName());
    
    /*
      if (! ejbClass.isAssignableTo(SessionBean.class)
          && ! ejbClass.isAnnotationPresent(Stateless.class)
          && ! ejbClass.isAnnotationPresent(Stateful.class))
        throw error(L.l("'{0}' must implement SessionBean or @Stateless or @Stateful.  Session beans must implement javax.ejb.SessionBean.", ejbClass.getName()));
    */

    // introspectSession();
  }

  /**
   * Returns true if it's a stateless session bean.
   */
  public boolean isStateless()
  {
    return _isStateless;
  }

  /**
   * Set true if it's a stateless session bean.
   */
  public void setSessionType(String type)
    throws ConfigException
  {
    if (type.equals("Stateful"))
      _isStateless = false;
    else if (type.equals("Stateless"))
      _isStateless = true;
    else
      throw new ConfigException(L.l("'{0}' is an unknown session-type.  session-type must be 'Stateless' or 'Stateful'.", type));
  }

  /**
   * Returns true if the container handles transactions.
   */
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
      throw new ConfigException(L.l("'{0}' is an unknown transaction-type.  transaction-type must be 'Container' or 'Bean'.", type));
  }

  /**
   * Configure initialization.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    super.init();

    try {
      if (getRemoteHome() != null) {
        validateHome(getRemoteHome(), getRemoteList().get(0));
      }

      if (getLocalHome() != null) {
        validateHome(getLocalHome(), getLocalList().get(0));
      }

      for (ApiClass remoteApi : getRemoteList())
        validateRemote(remoteApi);

      for (ApiClass localApi : getLocalList())
        validateRemote(localApi);

      if (getEJBClass() == null) {
        throw error(L.l("'{0}' does not have a defined ejb-class.  Session beans must have an ejb-class.",
                        getEJBName()));
      }

      if (! SessionSynchronization.class.isAssignableFrom(getEJBClassWrapper().getJavaClass())) {
      }
      else if (isStateless()) {
        throw error(L.l("'{0}' must not implement SessionSynchronization.  Stateless session beans must not implement SessionSynchronization.",
                        getEJBClass().getName()));
      }
      else if (! _isContainerTransaction) {
        throw error(L.l("'{0}' must not implement SessionSynchronization.  Session beans with Bean-managed transactions may not use SessionSynchronization.",
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
  protected BeanGenerator createBeanGenerator()
  {
    ApiClass ejbClass = getEJBClassWrapper();

    fillClassDefaults(ejbClass);
    
    if (_isStateless) {
      _sessionBean = new StatelessGenerator(getEJBName(),
					    ejbClass,
					    getLocalHome(),
					    getLocalList(),
					    getRemoteHome(),
					    getRemoteList());
    }
    else {
      _sessionBean = new StatefulGenerator(getEJBName(),
					   ejbClass,
					   getLocalHome(),
					   getLocalList(),
					   getRemoteHome(),
					   getRemoteList());
    }
    
    return _sessionBean;
  }

  private void fillClassDefaults(ApiClass ejbClass)
  {
    if (! _isContainerTransaction) {
      ejbClass.addAnnotation(XaAnnotation.createBeanManaged());
    }
    
    TransactionAttribute ann
      = ejbClass.getAnnotation(TransactionAttribute.class);

    if (ann == null) {
      // ejb/1100
      ejbClass.addAnnotation(XaAnnotation.create(REQUIRED));
    }
  }
  
  /**
   * Obtain and apply initialization from annotations.
   */
  public void initIntrospect()
    throws ConfigException
  {
    super.initIntrospect();

    ApiClass type = getEJBClassWrapper();

    // XXX: ejb/0f78
    if (type == null)
      return;

    // ejb/0j20
    if (! type.isAnnotationPresent(Stateful.class)
        && ! type.isAnnotationPresent(Stateless.class)
	&& ! isAllowPOJO())
      return;

    /* TCK: ejb/0f6d: bean with local and remote interfaces
    if (_localHome != null || _localList.size() != 0
        || _remoteHome != null || _remoteList.size() != 0)
      return;
    */

    ArrayList<ApiClass> interfaceList = new ArrayList<ApiClass>();

    for (ApiClass localApi : type.getInterfaces()) {
      Class javaApi = localApi.getJavaClass();
      
      Local local = (Local) javaApi.getAnnotation(Local.class);

      if (local != null) {
        setLocalWrapper(localApi);
        continue;
      }

      javax.ejb.Remote remote
	= (javax.ejb.Remote) javaApi.getAnnotation(javax.ejb.Remote.class);

      if (remote != null || java.rmi.Remote.class.isAssignableFrom(javaApi)) {
        setRemoteWrapper(localApi);
        continue;
      }

      if (javaApi.getName().equals("java.io.Serializable"))
        continue;

      if (javaApi.getName().equals("java.io.Externalizable"))
        continue;

      if (javaApi.getName().startsWith("javax.ejb"))
        continue;

      if (javaApi.getName().equals("java.rmi.Remote"))
        continue;

      if (! interfaceList.contains(localApi))
        interfaceList.add(localApi);
    }
    
    Local local = type.getAnnotation(Local.class);
    if (local != null && local.value() != null) {
      _localList.clear();
      
      for (Class api : local.value()) {
	// XXX: grab from type?
	_localList.add(new ApiClass(api));
      }
    }
    
    Remote remote = type.getAnnotation(Remote.class);
    if (remote != null && remote.value() != null) {
      _remoteList.clear();
      
      for (Class api : remote.value()) {
	// XXX: grab from type?
	_remoteList.add(new ApiClass(api));
      }
    }

    // if (getLocalList().size() != 0 || getRemoteList().size() != 0) {
    if (_localHome != null || _localList.size() != 0
        || _remoteHome != null || _remoteList.size() != 0) {
    }
    else if (interfaceList.size() == 0) {
      if (isStateless()) {
	throw new ConfigException(L.l("'{0}' has no interfaces.  Can't currently generate Stateless beans without interfaces.",
                                    type.getName()));
      }
    }
    else if (interfaceList.size() != 1)
      throw new ConfigException(L.l("'{0}' has multiple interfaces, but none are marked as @Local or @Remote.\n{1}",
                                    type.getName(),
                                    interfaceList.toString()));
    else {
      setLocalWrapper(interfaceList.get(0));
    }

    // XXX: Check ejb30/bb/session/stateless/migration/twothree/annotated
    // There is a conflict between 2.1 and 3.0 interfaces.

    // ejb/0f6f
    // The session bean might have @RemoteHome for EJB 2.1 and
    // the @Remote interface for EJB 3.0 (same with @LocalHome and @Local).
    // TCK: ejb30/bb/session/stateful/sessioncontext/annotated

    ApiClass ejbClass = getEJBClassWrapper();

    LocalHome localHomeAnn = ejbClass.getAnnotation(LocalHome.class);

    // ejb/0f6f
    if (localHomeAnn != null) {
      Class localHome = localHomeAnn.value();
      setLocalHome(localHome);
    }

    RemoteHome remoteHomeAnn = ejbClass.getAnnotation(RemoteHome.class);

    // ejb/0f6f
    if (remoteHomeAnn != null) {
      Class home = remoteHomeAnn.value();
      setHome(home);
    }
  }

  /**
   * Deploys the bean.
   */
  @Override
  public AbstractServer deployServer(EjbContainer ejbContainer,
                                     JavaClassGenerator javaGen)
    throws ClassNotFoundException, ConfigException
  {
    AbstractServer server;

    if (isStateless())
      server = new StatelessServer(ejbContainer, getAnnotatedType());
    else
      server = new StatefulServer(ejbContainer, getAnnotatedType());

    server.setModuleName(getEJBModuleName());
    server.setEJBName(getEJBName());
    server.setMappedName(getMappedName());
    server.setId(getEJBModuleName() + "#" + getEJBName());
    server.setContainerTransaction(_isContainerTransaction);
    
    server.setEjbClass(loadClass(getEJBClass().getName()));

    ApiClass remoteHome = getRemoteHome();
    if (remoteHome != null)
      server.setRemoteHomeClass(loadClass(remoteHome.getName()));

    ArrayList<ApiClass> remoteList = _sessionBean.getRemoteApi();
    if (remoteList.size() > 0) {
      ArrayList<Class> classList = new ArrayList<Class>();
      for (ApiClass apiClass : remoteList) {
	classList.add(loadClass(apiClass.getName()));
      }
      
      server.setRemoteApiList(classList);
    }

    /*
    if (getRemote21() != null)
      server.setRemote21(loadClass(getRemote21().getName()));
     */

    ApiClass localHome = getLocalHome();
    if (localHome != null)
      server.setLocalHomeClass(loadClass(localHome.getName()));

    ArrayList<ApiClass> localList = _sessionBean.getLocalApi();
    if (localList.size() > 0) {
      ArrayList<Class> classList = new ArrayList<Class>();
      for (ApiClass apiClass : localList) {
	classList.add(loadClass(apiClass.getName()));
      }
      
      server.setLocalApiList(classList);
    }

    /*
    if (getLocal21() != null)
      server.setLocal21(loadClass(getLocal21().getName()));
     */
    
    Class contextImplClass = javaGen.loadClass(getSkeletonName());

    server.setContextImplClass(contextImplClass);

    Class beanClass = javaGen.loadClass(getEJBClass().getName());

    Class[] classes = contextImplClass.getDeclaredClasses();
    
    for (Class aClass : classes) {
      if (getEJBClass().isAssignableFrom(aClass)) {
        server.setBeanImplClass(aClass);

        break;
      }
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(server.getClassLoader());

      server.setInjectionTarget(getInjectionTarget());
      
      server.setInitProgram(getInitProgram());

      try {
        if (getServerProgram() != null)
          getServerProgram().configure(server);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return server;
  }

  private void introspectSession()
    throws ConfigException
  {
    ApiClass ejbClass = getEJBClassWrapper();

    if (ejbClass.isAnnotationPresent(Stateless.class))
      introspectStateless(ejbClass);
    else if (ejbClass.isAnnotationPresent(Stateful.class))
      introspectStateful(ejbClass);
  }

  private void introspectStateless(ApiClass type)
    throws ConfigException
  {
    String className = type.getName();

    Stateless stateless = type.getAnnotation(Stateless.class);

    setAllowPOJO(true);

    setSessionType("Stateless");

    setTransactionType(type);

    String name;
    if (stateless != null)
      name = stateless.name();
    else
      name = className;

    introspectBean(type, name);
  }

  private void introspectStateful(ApiClass type)
    throws ConfigException
  {
    String className = type.getName();

    Stateful stateful = type.getAnnotation(Stateful.class);

    setAllowPOJO(true);

    setSessionType("Stateful");

    setTransactionType(type);

    String name;
    if (stateful != null)
      name = stateful.name();
    else
      name = className;

    introspectBean(type, name);
  }

  private void setTransactionType(ApiClass type)
  {
    TransactionManagement transaction
      = type.getAnnotation(TransactionManagement.class);
    
    if (transaction == null)
      setTransactionType("Container");
    else if (TransactionManagementType.BEAN.equals(transaction.value()))
      setTransactionType("Bean");
    else
      setTransactionType("Container");
  }

  private void validateMethods()
    throws ConfigException
  {
  }

  /**
   * Validates the home interface.
   */
  private void validateHome(ApiClass homeClass, ApiClass objectClass)
    throws ConfigException
  {
    ApiClass beanClass = getEJBClassWrapper();
    String beanName = beanClass.getName();

    if (homeClass == null)
      return;
    String homeName = homeClass.getName();
    String objectName = objectClass.getName();

    boolean hasFindByPrimaryKey = false;

    if (! homeClass.isPublic())
      throw error(L.l("'{0}' must be public", homeName));

    if (beanClass.isFinal())
      throw error(L.l("'{0}' must not be final", beanName));

    if (beanClass.isAbstract())
      throw error(L.l("'{0}' must not be abstract", beanName));

    if (! homeClass.isInterface())
      throw error(L.l("'{0}' must be an interface", homeName));

    boolean hasCreate = false;

    for (ApiMethod method : homeClass.getMethods()) {
      String name = method.getName();
      Class []param = method.getParameterTypes();
      Class retType = method.getReturnType();

      Method javaMethod = method.getJavaMember();

      if (javaMethod.getDeclaringClass().isAssignableFrom(EJBHome.class)
	  || javaMethod.getDeclaringClass().isAssignableFrom(EJBLocalHome.class))
        continue;

      if (EJBHome.class.isAssignableFrom(homeClass.getJavaClass()))
        validateException(method, java.rmi.RemoteException.class);

      if (name.startsWith("create")) {
        hasCreate = true;

        // TCK: ejb30/bb/session/stateless/migration/twothree/descriptor/callLocalSameTxContextTest
        // XXX TCK, needs QA: if a stateless session bean has EJB 3.0 and 2.1 interfaces,
        // it is not required to have a matching ejbCreate().
        // It may have a @PostConstruct method, if any.
        if (isStateless() && name.equals("create"))
          continue;

        if (isStateless() && (! name.equals("create")
			      || method.getParameterTypes().length != 0)) {
          throw error(L.l("{0}: '{1}' forbidden in stateless session home.  The create() method for a stateless session bean must have zero arguments.",
                          method.getFullName(),
                          homeName));
        }

        if (! isAllowPOJO())
          validateException(method, CreateException.class);

        /* XXX: ejb/0f6f

           The session bean might have @RemoteHome for EJB 2.1 and the @Remote interface for EJB 3.0
           TCK: ejb30/bb/session/stateful/sessioncontext/annotated

        if (! retType.equals(objectClass))
          throw error(L.l("{0}: '{1}' must return {2}.  Create methods must return the local or remote interface.",
                          homeName,
                          method.getFullName(),
                          objectClass.getName()));
        */

        String createName = "ejbC" + name.substring(1);
        ApiMethod implMethod =
          validateNonFinalMethod(createName, param,
                                 method, homeClass, isAllowPOJO());

        if (implMethod != null) {
          if (! implMethod.getReturnType().getName().equals("void"))
            throw error(L.l("'{0}' must return {1} in {2}",
                            getFullMethodName(createName, param),
                            "void",
                            beanName));

          validateExceptions(method, implMethod.getExceptionTypes());
        }
      }
      else if (name.startsWith("ejb") || name.startsWith("remove")) {
        throw error(L.l("'{0}' forbidden in {1}",
                        method.getFullName(),
                        homeClass.getName()));
      }
    }

    if (! hasCreate)
      throw error(L.l("'{0}' needs at least one create method.  Session beans need a create method.",
                      homeClass.getName()));
  }
}
