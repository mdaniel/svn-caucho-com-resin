/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.bytecode.*;
import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.ConfigException;
import com.caucho.config.DependencyBean;
import com.caucho.config.LineConfigException;
import com.caucho.config.types.*;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.amber.AmberConfig;
import com.caucho.ejb.cfg.Interceptor;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.TransactionChain;
import com.caucho.ejb.gen.UserInRoleChain;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.log.Log;
import com.caucho.make.ClassDependency;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;
import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.*;

/**
 * Configuration for an ejb bean.
 */
public class EjbBean implements EnvironmentBean, DependencyBean {
  private static Logger log = Log.open(EjbBean.class);
  private static L10N L = new L10N(EjbBean.class);

  private static EnvironmentLocal<Map<Class,SoftReference<ApiMethod[]>>> _methodCache
    = new EnvironmentLocal<Map<Class,SoftReference<ApiMethod[]>>>();

  private final EjbConfig _ejbConfig;
  private final String _ejbModuleName;

  private ClassLoader _loader;

  protected ClassLoader _jClassLoader;

  private String _ejbName;

  // The published name as used by IIOP, Hessian, and
  // jndi-remote-prefix/jndi-local-prefix
  private String _mappedName;

  private String _location = "";
  private boolean _isInit; // used for error messsage line #

  // these classes are loaded with the parent (configuration) loader, not
  // the server loader
  private ApiClass _ejbClass;

  protected ApiClass _remoteHome21;
  protected ApiClass _remote21;
  
  protected ApiClass _remoteHome;
  protected ArrayList<ApiClass> _remoteList = new ArrayList<ApiClass>();

  protected ApiClass _localHome21;
  protected ApiClass _local21;
  
  protected ApiClass _localHome;
  protected ArrayList<ApiClass> _localList = new ArrayList<ApiClass>();
  protected ArrayList<ApiClass> _localApiList = new ArrayList<ApiClass>();


  protected EjbView _remoteHomeView;
  protected EjbView _remoteView21;
  protected EjbView _remoteView;
  protected EjbView _localHomeView;
  protected EjbView _localView21;
  protected EjbView _localView;

  // Can be both with multiple interfaces.
  protected boolean _isEJB21;
  protected boolean _isEJB30;

  private boolean _isAllowPOJO = true;

  private boolean _isContainerTransaction = true;

  ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();

  ArrayList<PersistentDependency> _configDependList =
    new ArrayList<PersistentDependency>();

  ArrayList<String> _beanDependList = new ArrayList<String>();

  ArrayList<EjbMethodPattern> _methodList = new ArrayList<EjbMethodPattern>();

  private HashMap<String,EjbBaseMethod> _methodMap
    = new HashMap<String,EjbBaseMethod>();

  private BuilderProgramContainer _initProgram;
  private ArrayList<BuilderProgram> _postConstructList
    = new ArrayList<BuilderProgram>();
  private BuilderProgramContainer _serverProgram;

  private ArrayList<Interceptor> _interceptors
    = new ArrayList<Interceptor>();

  private ArrayList<EjbLocalRef> _ejbLocalRefs
    = new ArrayList<EjbLocalRef>();

  private ArrayList<EnvEntry> _envEntries
    = new ArrayList<EnvEntry>();

  private ArrayList<ResourceEnvRef> _resourceEnvRefs
    = new ArrayList<ResourceEnvRef>();

  private ArrayList<ResourceRef> _resourceRefs
    = new ArrayList<ResourceRef>();

  private String _aroundInvokeMethodName;
  private String _timeoutMethodName;

  private long _transactionTimeout;

  private AroundInvokeConfig _aroundInvokeConfig;

  private ArrayList<RemoveMethod> _removeMethods
    = new ArrayList<RemoveMethod>();

  /**
   * Creates a new entity bean configuration.
   */
  public EjbBean(EjbConfig ejbConfig, String ejbModuleName)
  {
    _ejbConfig = ejbConfig;
    _ejbModuleName = ejbModuleName;

    _loader = ejbConfig.getEjbContainer().getClassLoader();

    // TCK ejb30/tx: ejb/0f14 vs ejb/02a0
    getEjbContainer().getTransactionManager().setEJB3(isEJB3());
  }

  public EjbConfig getConfig()
  {
    return _ejbConfig;
  }

  public EjbContainer getEjbContainer()
  {
    return _ejbConfig.getEjbContainer();
  }

  public String getAroundInvokeMethodName()
  {
    return _aroundInvokeMethodName;
  }

  public void setAroundInvokeMethodName(String aroundInvokeMethodName)
  {
    _aroundInvokeMethodName = aroundInvokeMethodName;
  }

  public void setAroundInvoke(AroundInvokeConfig aroundInvoke)
  {
    _aroundInvokeConfig = aroundInvoke;

    // ejb/0fbb
    _aroundInvokeMethodName = aroundInvoke.getMethodName();
  }

  /**
   * Returns the remove-method for the given method.
   */
  public RemoveMethod getRemoveMethod(Method method)
  {
    for (RemoveMethod removeMethod : _removeMethods) {
      if (removeMethod.isMatch(method))
        return removeMethod;
    }

    return null;
  }

  /**
   * Returns the remove-method list.
   */
  public ArrayList<RemoveMethod> getRemoveMethods()
  {
    return _removeMethods;
  }

  /**
   * Returns the timeout method name.
   */
  public String getTimeoutMethodName()
  {
    return _timeoutMethodName;
  }

  /**
   * Adds a new remove-method
   */
  public void addRemoveMethod(RemoveMethod removeMethod)
  {
    _removeMethods.add(removeMethod);
  }

  /**
   * Returns the env-entry list.
   */
  public ArrayList<EnvEntry> getEnvEntries()
  {
    return _envEntries;
  }

  /**
   * Adds a new env-entry
   */
  public void addEnvEntry(EnvEntry envEntry)
  {
    _envEntries.add(envEntry);
  }

  /**
   * Returns the resource-ref list.
   */
  public ArrayList<ResourceRef> getResourceRefs()
  {
    return _resourceRefs;
  }

  /**
   * Adds a new resource-ref.
   */
  public void addResourceRef(ResourceRef resourceRef)
  {
    _resourceRefs.add(resourceRef);
  }

  /**
   * Returns the interceptors.
   */
  public ArrayList<Interceptor> getInterceptors()
  {
    return _interceptors;
  }

  /**
   * Returns the interceptors.
   */
  public ArrayList<Interceptor> getInvokeInterceptors(String methodName)
  {
    ArrayList<Interceptor> matchList = null;

    for (Interceptor interceptor : _interceptors) {
      if (methodName.equals(interceptor.getAroundInvokeMethodName())) {
        if (matchList == null)
          matchList = new ArrayList<Interceptor>();

        matchList.add(interceptor);
      }
    }

    return matchList;
  }

  /**
   * Adds a new interceptor.
   */
  public void addInterceptor(Interceptor interceptor)
  {
    _interceptors.add(interceptor);
  }

  /**
   * Returns true if the interceptor is already configured.
   */
  public boolean containsInterceptor(String interceptorClassName)
  {
    return getInterceptor(interceptorClassName) != null;
  }

  /**
   * Returns the interceptor for a given class name.
   */
  public Interceptor getInterceptor(String interceptorClassName)
  {
    for (Interceptor interceptor : _interceptors) {
      String className = interceptor.getInterceptorClass();

      if (className.equals(interceptorClassName))
        return interceptor;
    }

    return null;
  }

  public String getEJBModuleName()
  {
    return _ejbModuleName;
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  protected Class loadClass(String className)
  {
    try {
      return Class.forName(className, false, _loader);
    } catch (ClassNotFoundException e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Sets the location
   */
  public void setConfigLocation(String filename, int line)
  {
    _location = filename + ":" + line + ": ";
  }

  /**
   * Sets the location
   */
  public void setLocation(String location)
  {
    _location = location;
  }

  /**
   * Gets the location
   */
  public String getLocation()
  {
    return _location;
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

  /**
   * Returns true if this EJB has a 2.1 interface.
   */
  public boolean isEJB21()
  {
    return _isEJB21;
  }

  /**
   * Returns true if this EJB has a 3.0 interface.
   */
  public boolean isEJB30()
  {
    return _isEJB30;
  }

  /**
   * Adds a description
   */
  public void addDescription(String description)
  {
  }

  /**
   * Adds a display name
   */
  public void addDisplayName(String displayName)
  {
  }

  /**
   * Adds a icon
   */
  public void addIcon(String icon)
  {
  }

  /**
   * Sets the ejbName
   */
  public void setEJBName(String ejbName)
  {
    _ejbName = ejbName;
  }

  /**
   * Gets the ejbName
   */
  public String getEJBName()
  {
    return _ejbName;
  }

  /**
   * The mapped-name is the remote published name
   * used by IIOP, Hessian, and jndi-remote-prefix, jndi-local-prefix.
   * The default is the EJBName.
   */
  public void setMappedName(String mappedName)
  {
    _mappedName = mappedName;
  }

  /**
   * The mapped-name is the published name
   * used by IIOP, Hessian, and jndi-remote-prefix, jndi-local-prefix.
   */
  public String getMappedName()
  {
    return _mappedName == null ? getEJBName() : _mappedName;
  }

  /**
   * Returns the kind of bean.
   */
  public String getEJBKind()
  {
    return "unknown";
  }

  /**
   * Sets the ejb implementation class.
   */
  public void setEJBClass(Class ejbClass)
    throws ConfigException
  {
    setEJBClassWrapper(new ApiClass(ejbClass));
  }

  /**
   * Sets the ejb implementation class.
   */
  public void setEJBClassWrapper(ApiClass ejbClass)
    throws ConfigException
  {
    if (_ejbClass != null && ! _ejbClass.getName().equals(ejbClass.getName()))
      throw error(L.l("ejb-class '{0}' cannot be redefined.  Old value is '{1}'.",
                      _ejbClass.getName(), ejbClass.getName()));


    _ejbClass = ejbClass;

    if (! _ejbClass.isPublic())
      throw error(L.l("'{0}' must be public.  Bean implementations must be public.", ejbClass.getName()));

    if (_ejbClass.isFinal())
      throw error(L.l("'{0}' must not be final.  Bean implementations must not be final.", ejbClass.getName()));

    if (_ejbClass.isInterface())
      throw error(L.l("'{0}' must not be an interface.  Bean implementations must be classes.", ejbClass.getName()));

    // ejb/02e5
    Constructor constructor = null;
    try {
      constructor = ejbClass.getConstructor(new Class[0]);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (constructor == null)
      throw error(L.l("'{0}' needs a public zero-arg constructor.  Bean implementations need a public zero-argument constructor.", ejbClass.getName()));

    for (Class exn : constructor.getExceptionTypes()) {
      if (! RuntimeException.class.isAssignableFrom(exn)) {
        throw error(L.l("{0}: constructor must not throw '{1}'.  Bean constructors must not throw checked exceptions.", ejbClass.getName(), exn.getName()));
      }
    }

    ApiMethod method = ejbClass.getMethod("finalize", new Class[0]);

    if (method != null && ! method.getDeclaringClass().equals(Object.class))
      throw error(L.l("'{0}' may not implement finalize().  Bean implementations may not implement finalize().", ejbClass.getName()));
  }

  /**
   * Gets the ejb implementation class.
   */
  public Class getEJBClass()
  {
    try {
      if (_ejbClass == null)
        return null;

      return Class.forName(_ejbClass.getName(), false, getClassLoader());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Gets the ejb implementation class.
   */
  public ApiClass getEJBClassWrapper()
  {
    return _ejbClass;
  }

  /**
   * Gets the ejb implementation class.
   */
  public String getEJBFullClassName()
  {
    return _ejbClass.getName();
  }

  /**
   * Gets the ejb implementation class.
   */
  public String getEJBClassName()
  {
    String s = _ejbClass.getName();
    int p = s.lastIndexOf('.');

    if (p > 0)
      return s.substring(p + 1);
    else
      return s;
  }

  /**
   * Gets the implementation class name.
   */
  public String getFullImplName()
  {
    return getEJBFullClassName();
  }

  /**
   * Sets the remote home interface class.
   */
  public void setHome(Class homeClass)
    throws ConfigException
  {
    ApiClass home = new ApiClass(homeClass);
    
    setHomeWrapper(home);

    /*
    // ejb/0ff0
    // Adds the 2.1 remote interface
    ApiMethod method = findFirstCreateMethod(home);

    if (method != null) {
      ApiClass remoteWrapper;

      remoteWrapper = new ApiClass(method.getReturnType());

      // Order is important.
      setRemote21(remoteWrapper);
      setRemoteWrapper(remoteWrapper);
    }
    */
  }

  /**
   * Sets the remote home interface class.
   */
  public void setHomeWrapper(ApiClass remoteHome)
    throws ConfigException
  {
    _remoteHome = remoteHome;

    if (! remoteHome.isPublic())
      throw error(L.l("'{0}' must be public.  <home> interfaces must be public.", remoteHome.getName()));

    if (! remoteHome.isInterface())
      throw error(L.l("'{0}' must be an interface. <home> interfaces must be interfaces.", remoteHome.getName()));

    if (EJBHome.class.isAssignableFrom(remoteHome.getJavaClass())) {
      _isEJB21 = true;
    }
    else if (! isAllowPOJO()) {
      // XXX: does descriptor still have this requirement?
      // i.e. annotations act differently
      throw new ConfigException(L.l("'{0}' must extend EJBHome.  <home> interfaces must extend javax.ejb.EJBHome.", remoteHome.getName()));
    }
  }

  /**
   * Gets the ejb implementation class.
   */
  public ApiClass getRemoteHome()
  {
    return _remoteHome;
  }

  /**
   * Gets the remote home class.
   */
  public Class getRemoteHomeClass()
  {
    if (_remoteHome == null)
      return null;

    try {
      return Class.forName(_remoteHome.getName(), false, getClassLoader());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the ejb remote interface
   */
  public void setRemote(Class remote)
    throws ConfigException
  {
    setRemoteWrapper(new ApiClass(remote));
  }

  /**
   * Adds a remote interface class
   */
  public void addBusinessRemote(Class remoteClass)
  {
    ApiClass remote = new ApiClass(remoteClass);
    
    if (! remote.isPublic())
      throw error(L.l("'{0}' must be public.  <business-remote> interfaces must be public.", remote.getName()));

    if (! remote.isInterface())
      throw error(L.l("'{0}' must be an interface. <business-remote> interfaces must be interfaces.", remote.getName()));

    if (! _remoteList.contains(remote)) {
      _remoteList.add(remote);

      if (remote == _remote21)
        _isEJB21 = true;
      else
        _isEJB30 = true;
    }
  }

  /**
   * Sets the remote interface class.
   */
  public void setRemoteWrapper(ApiClass remote)
    throws ConfigException
  {
    if (! remote.isPublic())
      throw error(L.l("'{0}' must be public.  <remote> interfaces must be public.", remote.getName()));

    if (! remote.isInterface())
      throw error(L.l("'{0}' must be an interface. <remote> interfaces must be interfaces.", remote.getName()));

    if (! EJBObject.class.isAssignableFrom(remote.getJavaClass())
	&& ! isAllowPOJO())
      throw new ConfigException(L.l("'{0}' must extend EJBObject.  <remote> interfaces must extend javax.ejb.EJBObject.", remote.getName()));

    if (! _remoteList.contains(remote)) {
      _remoteList.add(remote);

      if (remote == _remote21)
        _isEJB21 = true;
      else
        _isEJB30 = true;
    }
  }

  /**
   * Gets the 2.1 remote interface.
   */
  public ApiClass getRemote21()
  {
    return _remote21;
  }

  /**
   * Sets the 2.1 remote interface.
   */
  public void setRemote21(ApiClass remote21)
  {
    _isEJB21 = true;

    _remote21 = remote21;
  }

  /**
   * Gets the 2.1 local interface.
   */
  public ApiClass getLocal21()
  {
    return _local21;
  }

  /**
   * Sets the 2.1 local interface.
   */
  public void setLocal21(ApiClass local21)
  {
    _isEJB21 = true;

    _local21 = local21;
  }

  /**
   * Gets the remote interface class.
   */
  public ArrayList<ApiClass> getRemoteList()
  {
    return _remoteList;
  }

  /**
   * Gets the remote class.
   */
  public Class getRemoteClass()
  {
    if (_remoteList.size() < 1)
      return null;

    try {
      ApiClass remote = _remoteList.get(0);

      return Class.forName(remote.getName(), false, getClassLoader());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the ejb local home interface
   */
  public void setLocalHome(Class localHomeClass)
    throws ConfigException
  {
    ApiClass localHome = new ApiClass(localHomeClass);
    
    setLocalHomeWrapper(localHome);

    // ejb/0ff4
    // Adds the 2.1 local interface
    ApiMethod method = findFirstCreateMethod(localHome);

    ApiClass localWrapper = new ApiClass(method.getReturnType());

    // Order is important.
    setLocal21(localWrapper);
    setLocalWrapper(localWrapper);
  }

  /**
   * Sets the local home interface class.
   */
  public void setLocalHomeWrapper(ApiClass localHome)
    throws ConfigException
  {
    _localHome = localHome;

    if (! localHome.isPublic())
      throw error(L.l("'{0}' must be public.  <local-home> interfaces must be public.", localHome.getName()));

    if (! localHome.isInterface())
      throw error(L.l("'{0}' must be an interface. <local-home> interfaces must be interfaces.", localHome.getName()));

    if (! EJBLocalHome.class.isAssignableFrom(localHome.getJavaClass())
	&& ! isAllowPOJO())
      throw new ConfigException(L.l("'{0}' must extend EJBLocalHome.  <local-home> interfaces must extend javax.ejb.EJBLocalHome.", localHome.getName()));

  }

  /**
   * Gets the local home interface class.
   */
  public ApiClass getLocalHome()
  {
    return _localHome;
  }

  /**
   * Sets the ejb local interface
   */
  public void setLocal(Class local)
    throws ConfigException
  {
    setLocalWrapper(new ApiClass(local));
  }

  /**
   * Adds a local interface class
   */
  public void addBusinessLocal(Class localClass)
  {
    ApiClass local = new ApiClass(localClass);
    
    if (! local.isPublic())
      throw error(L.l("'{0}' must be public.  <local> interfaces must be public.", local.getName()));

    if (! local.isInterface())
      throw error(L.l("'{0}' must be an interface. <local> interfaces must be interfaces.", local.getName()));

    if (! _localList.contains(local)) {
      _localList.add(local);
      _localApiList.add(local);

      if (local == _local21)
        _isEJB21 = true;
      else
        _isEJB30 = true;
    }
  }

  /**
   * Sets the local interface class.
   */
  public void setLocalWrapper(ApiClass local)
    throws ConfigException
  {
    if (! local.isPublic())
      throw error(L.l("'{0}' must be public.  <local> interfaces must be public.", local.getName()));

    if (! local.isInterface())
      throw error(L.l("'{0}' must be an interface. <local> interfaces must be interfaces.", local.getName()));

    if (! EJBLocalObject.class.isAssignableFrom(local.getJavaClass())
	&& ! isAllowPOJO())
      throw new ConfigException(L.l("'{0}' must extend EJBLocalObject.  <local> interfaces must extend javax.ejb.EJBLocalObject.", local.getName()));

    if (! _localList.contains(local)) {
      _localList.add(local);
      _localApiList.add(new ApiClass(local.getJavaClass()));

      if (local == _local21)
        _isEJB21 = true;
      else
        _isEJB30 = true;
    }
  }

  /**
   * Gets the local interface class.
   */
  public ArrayList<ApiClass> getLocalList()
  {
    return _localList;
  }

  /**
   * Returns true if the transaction type is container.
   */
  public boolean isContainerTransaction()
  {
    return _isContainerTransaction;
  }

  /**
   * Adds a method.
   */
  public EjbMethodPattern createMethod(MethodSignature sig)
  {
    for (int i = 0; i < _methodList.size(); i++) {
      EjbMethodPattern method = _methodList.get(i);

      if (method.getSignature().equals(sig))
        return method;
    }

    EjbMethodPattern method = new EjbMethodPattern(this, sig);

    _methodList.add(method);

    return method;
  }

  /**
   * Adds a method.
   */
  public void addMethod(EjbMethodPattern method)
  {
    _methodList.add(method);
  }

  /**
   * Gets the best method.
   */
  public EjbMethodPattern getMethodPattern(ApiMethod method, String intf)
  {
    EjbMethodPattern bestMethod = null;
    int bestCost = -1;

    for (int i = 0; i < _methodList.size(); i++) {
      EjbMethodPattern ejbMethod = _methodList.get(i);
      MethodSignature sig = ejbMethod.getSignature();

      if (sig.isMatch(method, intf) && bestCost < sig.getCost()) {
        bestMethod = ejbMethod;
        bestCost = sig.getCost();
      }
    }

    return bestMethod;
  }

  /**
   * returns the method list.
   */
  public ArrayList<EjbMethodPattern> getMethodList()
  {
    return _methodList;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTransactionTimeout(Period timeout)
  {
    _transactionTimeout = timeout.getPeriod();
  }

  /**
   * Gets the transaction timeout.
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  public EjbRef createEjbRef()
  {
    return new EjbRef(Vfs.lookup(_ejbModuleName), getEJBName());
  }

  public ResourceEnvRef createResourceEnvRef()
  {
    ResourceEnvRef ref = new ResourceEnvRef(Vfs.lookup(_ejbModuleName), getEJBName());

    _resourceEnvRefs.add(ref);

    return ref;
  }

  /**
   * Returns the ejb-local-ref's from this bean
   */
  public ArrayList<EjbLocalRef> getEjbLocalRefs()
  {
    return _ejbLocalRefs;
  }

  /**
   * Returns the resource-env-ref's from this bean
   */
  public ArrayList<ResourceEnvRef> getResourceEnvRefs()
  {
    return _resourceEnvRefs;
  }

  public EjbLocalRef createEjbLocalRef()
  {
    EjbLocalRef ejbLocalRef = new EjbLocalRef(Vfs.lookup(_ejbModuleName), getEJBName());

    _ejbLocalRefs.add(ejbLocalRef);

    return ejbLocalRef;
  }

  public MessageDestinationRef createMessageDestinationRef()
  {
    return new MessageDestinationRef(Vfs.lookup(_ejbModuleName));
  }
  /**
   * Sets the security identity
   */
  public void setSecurityIdentity(EjbSecurityIdentity securityIdentity)
  {
  }

  /**
   * Adds a list of dependencies.
   */
  public void addDependencyList(ArrayList<PersistentDependency> dependList)
  {
    for (int i = 0; dependList != null && i < dependList.size(); i++) {
      addDependency(dependList.get(i));
    }
  }

  /**
   * Add a dependency.
   */
  public void addDepend(Path path)
  {
    addDependency(new Depend(path));
  }

  /**
   * Add a dependency.
   */
  public void addDependency(PersistentDependency depend)
  {
    if (! _dependList.contains(depend))
      _dependList.add(depend);
  }

  /**
   * Add a dependency.
   */
  public void addDependency(Class cl)
  {
    addDependency(new ClassDependency(cl));
  }

  /**
   * Gets the depend list.
   */
  public ArrayList<PersistentDependency> getDependList()
  {
    return _dependList;
  }

  /**
   * Add a bean dependency.
   */
  public void addBeanDependency(String ejbName)
  {
    if (! _beanDependList.contains(ejbName))
      _beanDependList.add(ejbName);
  }

  /**
   * Gets the bean depend list.
   */
  public ArrayList<String> getBeanDependList()
  {
    return _beanDependList;
  }

  /**
   * Adds an init program.
   */
  public void addInitProgram(BuilderProgram init)
  {
    if (_initProgram == null)
      _initProgram = new BuilderProgramContainer();

    _initProgram.addProgram(init);
  }

  /**
   * Adds an undefined value, e.g. env-entry
   */
  public void addBuilderProgram(BuilderProgram init)
  {
    if (_serverProgram == null)
      _serverProgram = new BuilderProgramContainer();

    _serverProgram.addProgram(init);
  }

  public void setInit(InitProgram init)
  {
    if (_initProgram == null)
      _initProgram = new BuilderProgramContainer();

    _initProgram.addProgram(init.getBuilderProgram());
  }

  public void addPostConstruct(PostConstructType postConstruct)
  {
    _postConstructList.add(postConstruct.getProgram(getEJBClass()));
  }

  /**
   * Gets the init program.
   */
  public BuilderProgramContainer getInitProgram()
  {
    if (_postConstructList != null) {
      if (_initProgram == null)
        _initProgram = new BuilderProgramContainer();

      for (BuilderProgram program : _postConstructList)
        _initProgram.addProgram(program);

      _postConstructList = null;
    }

    return _initProgram;
  }

  /**
   * Gets the server program.
   */
  public BuilderProgramContainer getServerProgram()
  {
    return _serverProgram;
  }

  /**
   * Configure initialization.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      _isInit = true;

      initIntrospect();

      assembleBeanMethods();

      createViews();

      /*
        if (_jndiName == null) {
        if (getRemoteHomeClass() != null)
        _jndiName = getConfig().getRemoteJndiName() + "/" + _ejbName;
        else
        _jndiName = getConfig().getLocalJndiName() + "/" + _ejbName;
        }
      */
    } catch (LineConfigException e) {
      throw e;
    } catch (ConfigException e) {
      throw new LineConfigException(_location + e.getMessage(), e);
    }
  }

  /**
   * Configure initialization.
   */
  public void initIntrospect()
    throws ConfigException
  {
    boolean isExcludeDefault = false;

    // XXX: ejb/0f78
    if (_ejbClass != null) {
      Interceptors interceptorsAnn = _ejbClass.getAnnotation(Interceptors.class);

      if (interceptorsAnn != null) {
        for (Class cl : interceptorsAnn.value()) {
          // XXX: ejb/0fb0
          if (! containsInterceptor(cl.getName())) {
            addInterceptor(configureInterceptor(cl));
          }
        }
      }

      // ejb/0fbj
      if (_ejbClass.isAnnotationPresent(ExcludeDefaultInterceptors.class))
        isExcludeDefault = true;
    }

    // ejb/0fb5
    InterceptorBinding binding =
      _ejbConfig.getInterceptorBinding(getEJBName(), isExcludeDefault);

    if (binding != null) {
      ArrayList<String> interceptorClasses = binding.getInterceptors();

      // ejb/0fb7
      if (interceptorClasses.isEmpty()) {
        InterceptorOrder interceptorOrder = binding.getInterceptorOrder();

        // ejb/0fbf
        if (interceptorOrder != null)
          interceptorClasses = interceptorOrder.getInterceptorClasses();
      }

      for (String className : interceptorClasses) {
        Interceptor interceptor = getInterceptor(className);

        // ejb/0fb5 vs ejb/0fb6
        if (interceptor != null) {
          _interceptors.remove(interceptor);

          addInterceptor(interceptor);
        }
        else {
          interceptor = _ejbConfig.getInterceptor(className);

          interceptor.init();

          addInterceptor(interceptor);
        }
      }
    }
  }

  private Interceptor configureInterceptor(Class type)
    throws ConfigException
  {
    try {
      Interceptor interceptor = new Interceptor();

      interceptor.setInterceptorClass(type.getName());

      interceptor.init();

      return interceptor;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Configure for amber.
   */
  public void configureAmber(AmberConfig config)
    throws ConfigException
  {
  }

  /**
   * Creates the views.
   */
  protected void createViews()
    throws ConfigException
  {
    if (_remoteHome != null) {
      _remoteHomeView = createHomeView(_remoteHome, "RemoteHome");
      _remoteHomeView.introspect();
    }

    if (_remote21 != null) {
      ArrayList<ApiClass> list = new ArrayList<ApiClass>();
      list.add(_remote21);

      _remoteView21 = createObjectView(list, "Remote", "21");
      _remoteView21.introspect();
    }

    else if (_remoteList.size() > 0) {
      ArrayList<ApiClass> list = new ArrayList<ApiClass>();
      list.addAll(_remoteList);
      list.remove(_remote21);

      if (list.size() > 0) {
        _remoteView = createObjectView(list, "Remote", "");
        _remoteView.introspect();
      }
    }

    if (_localHome != null) {
      _localHomeView = createHomeView(_localHome, "LocalHome");
      _localHomeView.introspect();
    }

    if (_local21 != null) {
      ArrayList<ApiClass> list = new ArrayList<ApiClass>();
      list.add(_local21);

      _localView21 = createObjectView(list, "Local", "21");
      _localView21.introspect();
    }

    if (_localList.size() > 0) {
      ArrayList<ApiClass> list = new ArrayList<ApiClass>();
      list.addAll(_localList);
      list.remove(_local21);

      if (list.size() > 0) {
        _localView = createObjectView(list, "Local", "");
        _localView.introspect();
      }
    }
  }

  /**
   * Creates a home view.
   */
  protected EjbHomeView createHomeView(ApiClass homeClass, String prefix)
    throws ConfigException
  {
    return new EjbHomeView(this, homeClass, prefix);
  }

  /**
   * Creates an object view.
   */
  protected EjbObjectView createObjectView(ArrayList<ApiClass> apiList,
                                           String prefix,
                                           String suffix)
    throws ConfigException
  {
    return new EjbObjectView(this, apiList, prefix, suffix);
  }

  /**
   * Generates the class.
   */
  public void generate(JavaClassGenerator javaGen, boolean isAutoCompile)
    throws Exception
  {
    String fullClassName = getSkeletonName();

    if (javaGen.preload(fullClassName) != null) {
    }
    else if (isAutoCompile) {
      GenClass genClass = assembleGenerator(fullClassName);

      if (genClass != null)
        javaGen.generate(genClass);
    }
  }

  /**
   * Deploys the bean.
   */
  public AbstractServer deployServer(EjbContainer ejbContainer,
                                     JavaClassGenerator javaGen)
    throws ClassNotFoundException, ConfigException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Validates the remote interface.
   */
  protected void validateRemote(ApiClass objectClass)
    throws ConfigException
  {
    ApiClass beanClass = getEJBClassWrapper();
    String beanName = beanClass.getName();

    String objectName = objectClass.getName();

    if (! objectClass.isPublic())
      throw error(L.l("'{0}' must be public", objectName));

    if (! objectClass.isInterface())
      throw error(L.l("'{0}' must be an interface", objectName));

    for (ApiMethod method : objectClass.getMethods()) {
      String name = method.getName();
      Class []param = method.getParameterTypes();
      Class retType = method.getReturnType();

      if (method.getDeclaringClass().isAssignableFrom(EJBObject.class))
        continue;
      if (method.getDeclaringClass().isAssignableFrom(EJBLocalObject.class))
        continue;

      if (EJBObject.class.isAssignableFrom(objectClass.getJavaClass()))
        validateException(method, java.rmi.RemoteException.class);

      if (name.startsWith("ejb")) {
        throw error(L.l("'{0}' forbidden in {1}.  Local or remote interfaces may not define ejbXXX methods.",
                        getFullMethodName(method),
                        objectName));
      }

      Class returnType = method.getReturnType();

      if (EJBObject.class.isAssignableFrom(objectClass.getJavaClass())
          && (EJBLocalObject.class.isAssignableFrom(returnType)
	      || EJBLocalHome.class.isAssignableFrom(returnType))) {
        throw error(L.l("'{0}' must not return '{1}' in {2}.  Remote methods must not return local interfaces.",
                        getFullMethodName(method),
                        getShortClassName(returnType),
                        objectClass.getName()));
      }

      ApiMethod implMethod =
        validateRemoteImplMethod(method.getName(), param,
                                 method, objectClass);

      if (! returnType.equals(implMethod.getReturnType())) {
        throw error(L.l("{0}: '{1}' must return {2} to match {3}.{4}.  Business methods must return the same type as the interface.",
                        method.getDeclaringClass().getName(),
                        getFullMethodName(method),
                        implMethod.getReturnType().getName(),
                        getShortClassName(implMethod.getDeclaringClass()),
                        getFullMethodName(implMethod)));
      }

      validateExceptions(method, implMethod.getExceptionTypes());
    }
  }

  /**
   * Check that a method exists, is public, not abstract.
   *
   * @param methodName the name of the method to check for
   * @param args the expected method parameters
   *
   * @return the matching method
   */
  private ApiMethod validateRemoteImplMethod(String methodName,
                                           Class []param,
                                           ApiMethod sourceMethod,
                                           ApiClass sourceClass)
    throws ConfigException
  {
    ApiMethod method = null;
    ApiClass beanClass = getEJBClassWrapper();

    method = getMethod(beanClass, methodName, param);

    if (method == null && sourceMethod != null) {
      throw error(L.l("{0}: '{1}' expected to match {2}.{3}",
                      beanClass.getName(),
                      getFullMethodName(methodName, param),
                      getShortClassName(sourceMethod.getDeclaringClass()),
                      getFullMethodName(sourceMethod)));
    }
    else if (method == null) {
      throw error(L.l("{0}: '{1}' expected",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    /*
      else if (Modifier.isAbstract(method.getModifiers()) &&
      getBeanManagedPersistence()) {
      throw error(L.l("{0}: '{1}' must not be abstract",
      beanClass.getName(),
      getFullMethodName(methodName, param)));
      }
    */
    else if (! method.isPublic()) {
      throw error(L.l("{0}: '{1}' must be public",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }

    if (method.isStatic()) {
      throw error(L.l("{0}: '{1}' must not be static",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }

    if (method.isFinal()) {
      throw error(L.l("{0}: '{1}' must not be final.",
                      beanClass.getName(),
                      getFullMethodName(methodName, param),
                      beanClass.getName()));
    }

    return method;
  }

  ApiMethod validateNonFinalMethod(String methodName, Class []param,
                                 boolean isOptional)
    throws ConfigException
  {
    if (isOptional && getMethod(_ejbClass, methodName, param) == null)
      return null;
    else
      return validateNonFinalMethod(methodName, param);
  }

  ApiMethod validateNonFinalMethod(String methodName, Class []param)
    throws ConfigException
  {
    return validateNonFinalMethod(methodName, param, null, null);
  }

  ApiMethod validateNonFinalMethod(String methodName, Class []param,
                                 ApiMethod sourceMethod, ApiClass sourceClass)
    throws ConfigException
  {
    return validateNonFinalMethod(methodName, param,
                                  sourceMethod, sourceClass, false);
  }

  /**
   * Check that a method exists, is public, not abstract, and not final.
   *
   * @param methodName the name of the method to check for
   * @param args the expected method parameters
   *
   * @return the matching method
   */
  ApiMethod validateNonFinalMethod(String methodName, Class []param,
				   ApiMethod sourceMethod,
				   ApiClass sourceClass,
				   boolean isOptional)
    throws ConfigException
  {
    ApiMethod method = validateMethod(methodName, param,
				      sourceMethod, sourceClass,
				      isOptional);

    if (method == null && isOptional)
      return null;

    if (method.isFinal())
      throw error(L.l("{0}: '{1}' must not be final",
                      _ejbClass.getName(),
                      getFullMethodName(method)));


    if (method.isStatic())
      throw error(L.l("{0}: '{1}' must not be static",
                      _ejbClass.getName(),
                      getFullMethodName(method)));

    return method;
  }

  ApiMethod validateMethod(String methodName, Class []param)
    throws ConfigException
  {
    return validateMethod(methodName, param, null, null);
  }

  /**
   * Check that a method exists, is public and is not abstract.
   *
   * @param methodName the name of the method to check for
   * @param args the expected method parameters
   *
   * @return the matching method
   */
  ApiMethod validateMethod(String methodName, Class []param,
                         ApiMethod sourceMethod, ApiClass sourceClass)
    throws ConfigException
  {
    return validateMethod(methodName, param, sourceMethod, sourceClass, false);
  }

  /**
   * Check that a method exists, is public and is not abstract.
   *
   * @param methodName the name of the method to check for
   * @param args the expected method parameters
   *
   * @return the matching method
   */
  ApiMethod validateMethod(String methodName, Class []param,
			   ApiMethod sourceMethod, ApiClass sourceClass,
			   boolean isOptional)
    throws ConfigException
  {
    ApiMethod method = null;

    method = getMethod(_ejbClass, methodName, param);

    if (method == null && isOptional)
      return null;

    if (method == null && sourceMethod != null) {
      throw error(L.l("{0}: missing '{1}' needed to match {2}.{3}",
                      _ejbClass.getName(),
                      getFullMethodName(methodName, param),
                      sourceClass.getSimpleName(),
                      getFullMethodName(sourceMethod)));
    }
    else if (method == null) {
      throw error(L.l("{0}: expected '{1}'",
                      _ejbClass.getName(),
                      getFullMethodName(methodName, param)));
    }

    Class declaringClass = method.getDeclaringClass();

    if (method.isAbstract()) {
      if (method.getDeclaringClass().getName().equals("javax.ejb.EntityBean"))
        throw error(L.l("{0}: '{1}' must not be abstract.  Entity beans must implement the methods in EntityBean.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param)));
      else if (method.getDeclaringClass().getName().equals("javax.ejb.SessionBean"))
        throw error(L.l("{0}: '{1}' must not be abstract.  Session beans must implement the methods in SessionBean.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param)));
      else if (sourceMethod != null)
        throw error(L.l("{0}: '{1}' must not be abstract.  All methods from '{2}' must be implemented in the bean.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param),
                        sourceClass.getName()));
      else
        throw error(L.l("{0}: '{1}' must not be abstract.  Business methods must be implemented.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param)));
    } else if (! method.isPublic()) {
      throw error(L.l("{0}: '{1}' must be public.  Business method implementations must be public.",
                      _ejbClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    if (method.isStatic()) {
      throw error(L.l("{0}: '{1}' must not be static.  Business method implementations must not be static.",
                      _ejbClass.getName(),
                      getFullMethodName(method)));
    }

    return method;
  }

  public String getSkeletonName()
  {
    String className = getEJBClass().getName();
    int p = className.lastIndexOf('.');

    if (p > 0)
      className = className.substring(p + 1);

    String ejbName = getEJBName();

    String fullClassName = "_ejb." + ejbName + "." + className + "__EJB";

    return JavaClassGenerator.cleanClassName(fullClassName);
  }

  /**
   * Assembles the generator.
   */
  protected GenClass assembleGenerator(String fullClassName)
    throws NoSuchMethodException, ConfigException
  {
    int p = fullClassName.lastIndexOf('.');
    String className = fullClassName;
    if (p > 0)
      className = fullClassName.substring(p + 1);

    BeanAssembler assembler = createAssembler(fullClassName);

    if (assembler == null)
      return null;

    addImports(assembler);

    assembler.addHeaderComponent(getEJBClassWrapper(),
                                 fullClassName,
                                 getFullImplName());

    assembleMethods(assembler, fullClassName);

    // getEJBClassName());

    if (_remoteHomeView != null)
      _remoteHomeView.assembleView(assembler, fullClassName);

    if (_remoteView21 != null)
      _remoteView21.assembleView(assembler, fullClassName);

    if (_remoteView != null)
      _remoteView.assembleView(assembler, fullClassName);

    if (_localHomeView != null)
      _localHomeView.assembleView(assembler, fullClassName);

    if (_localView21 != null)
      _localView21.assembleView(assembler, fullClassName);

    if (_localView != null)
      _localView.assembleView(assembler, fullClassName);

    for (PersistentDependency depend : _dependList) {
      assembler.addDependency(depend);
    }

    assembler.addDependency(new ClassDependency(_ejbClass.getJavaClass()));

    if (_remoteHome != null)
      assembler.addDependency(new ClassDependency(_remoteHome.getJavaClass()));

    for (ApiClass remote : _remoteList) {
      assembler.addDependency(new ClassDependency(remote.getJavaClass()));
    }

    if (_localHome != null)
      assembler.addDependency(new ClassDependency(_localHome.getJavaClass()));

    for (ApiClass local : _localList) {
      assembler.addDependency(new ClassDependency(local.getJavaClass()));
    }

    return assembler.getAssembledGenerator();
  }

  /**
   * Adds the assemblers.
   */
  protected void addImports(BeanAssembler assembler)
  {
    assembler.addImport("javax.ejb.*");
    assembler.addImport("com.caucho.vfs.*");

    assembler.addImport("com.caucho.ejb.xa.EjbTransactionManager");
    assembler.addImport("com.caucho.ejb.xa.TransactionContext");

    assembler.addImport("com.caucho.ejb.AbstractContext");
  }

  /**
   * Creates the assembler for the bean.
   */
  protected BeanAssembler createAssembler(String fullClassName)
  {
    return null;
  }

  /**
   * Introspects the bean's methods.
   */
  protected void assembleBeanMethods()
    throws ConfigException
  {
    // find API methods matching an implementation method
    for (ApiMethod method : getEJBClassWrapper().getMethods()) {
      EjbBaseMethod ejbMethod = null;

      String name = method.getName();

      if (name.startsWith("ejb")) {
        ejbMethod = introspectEJBMethod(method);

        if (ejbMethod != null)
          _methodMap.put(ejbMethod.getMethod().getFullName(), ejbMethod);
      }
      else
        validateImplMethod(method);
    }
  }

  /**
   * Assembles the generator methods.
   */
  protected void assembleMethods(BeanAssembler assembler,
                                 String fullClassName)
    throws ConfigException
  {
    for (EjbBaseMethod method : _methodMap.values()) {
      assembler.addMethod(method.assemble(assembler, fullClassName));
    }
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbBaseMethod introspectEJBMethod(ApiMethod method)
    throws ConfigException
  {
    return null;
  }

  /**
   * Validates an implementation method.
   */
  protected void validateImplMethod(ApiMethod method)
    throws ConfigException
  {
  }

  /**
   * Assembles methods.
   */
  protected void assembleMethods(BeanAssembler assembler,
                                 ViewClass view,
                                 String contextClassName,
                                 ArrayList<ApiMethod> methods,
                                 String prefix)
    throws NoSuchMethodException
  {
    for (ApiMethod method : methods) {
      String className = method.getDeclaringClass().getName();
      String methodName = method.getName();
      Class []args = method.getParameterTypes();

      if (className.startsWith("javax.ejb.")) {
      }
      /*
      else if (isOld(methods, method, i)) {
      }
      */
      else if (methodName.equals("equals") && args.length == 1
	       && args[0].equals(Object.class)) {
      }
      else if (methodName.equals("hashCode") && args.length == 0) {
      }
      else {
        ApiMethod beanMethod = null;

        ApiClass ejbClass = getEJBClassWrapper();

        beanMethod = ejbClass.getMethod(method.getName(),
                                        method.getParameterTypes());

        if (beanMethod == null)
          throw new NoSuchMethodException("Can't find public method " +
                                          method.getFullName());

        CallChain call = new MethodCallChain(beanMethod.getMethod());
        call = view.createPoolChain(call, null);
        call = getTransactionChain(call, beanMethod, method, prefix);
        call = getSecurityChain(call, beanMethod, prefix);

        view.addMethod(new BaseMethod(method.getMethod(), call));
      }
    }
  }

  protected void assembleHomeMethods(BeanAssembler assembler,
                                     BaseClass baseClass,
                                     String contextClassName,
                                     ApiClass homeClass,
                                     String prefix)
    throws NoSuchMethodException
  {
    for (ApiMethod method : homeClass.getMethods()) {
      String className = method.getDeclaringClass().getName();
      String methodName = method.getName();

      if (className.startsWith("javax.ejb.")) {
      }
      /*
      else if (isOld(methods, method, i)) {
      }
      */
      else if (methodName.startsWith("create")) {
        ApiMethod beanMethod = null;

        String name = ("ejbCreate" + Character.toUpperCase(methodName.charAt(0))
                       + methodName.substring(1));

	beanMethod
	  = getEJBClassWrapper().getMethod(name, method.getParameterTypes());

        /*
          baseClass.addMethod(assembler.createCreateMethod(methods[i],
          contextClassName,
          prefix));
        */

        /*
          if (isStateless()) {
          }
          else {
          CallChain call = new MethodCallChain(beanMethod);
          call = getTransactionChain(call, beanMethod, prefix);

          baseClass.addMethod(new BaseMethod(methods[i], call));
          }
        */

        //printCreate(methods[i], prefix);
      }
      else if (methodName.startsWith("find")) {
        //printFind(methods[i], prefix);
      }
      else {
        ApiMethod beanMethod = null;

        String name = ("ejbHome" + Character.toUpperCase(methodName.charAt(0))
                       + methodName.substring(1));

	beanMethod
	  = getEJBClassWrapper().getMethod(name, method.getParameterTypes());

        CallChain call = new MethodCallChain(beanMethod.getMethod());
        call = getTransactionChain(call, beanMethod, method, prefix);
        call = getSecurityChain(call, beanMethod, prefix);

        baseClass.addMethod(new BaseMethod(method.getMethod(), call));
      }
    }
  }

  protected CallChain getTransactionChain(CallChain next,
                                          ApiMethod apiMethod,
                                          ApiMethod implMethod,
                                          String prefix)
  {
    return TransactionChain.create(next,
				   getTransactionAttribute(implMethod, prefix),
                                   apiMethod, implMethod, isEJB3(),
                                   _ejbConfig.getApplicationExceptions());
  }

  protected CallChain getSecurityChain(CallChain next,
                                       ApiMethod method,
                                       String prefix)
  {
    EjbMethodPattern ejbMethod = getMethodPattern(method, prefix);

    ArrayList<String> roles = null;

    if (ejbMethod != null)
      roles = ejbMethod.getRoles();

    if (roles == null) {
      ejbMethod = getMethodPattern(null, prefix);
      if (ejbMethod != null)
        roles = ejbMethod.getRoles();
    }

    if (roles == null) {
      ejbMethod = getMethodPattern(method, null);
      if (ejbMethod != null)
        roles = ejbMethod.getRoles();
    }

    if (roles == null) {
      ejbMethod = getMethodPattern(null, null);
      if (ejbMethod != null)
        roles = ejbMethod.getRoles();
    }

    if (roles != null)
      return new UserInRoleChain(next, roles);
    else
      return next;
  }

  /**
   * Check that a method is public.
   *
   * @return the matching method
   */
  protected void validatePublicMethod(ApiMethod method)
    throws ConfigException
  {
    if (! method.isPublic()) {
      throw error(L.l("{0}: '{1}' must be public.",
                      _ejbClass.getName(),
                      getFullMethodName(method)));
    }
    else if (method.isStatic()) {
      throw error(L.l("{0}: '{1}' must not be static.",
                      _ejbClass.getName(),
                      getFullMethodName(method)));
    }
  }

  /**
   * True if we've already handled the method.
   */
  /*
  static boolean isOld(ApiMethod []methods, ApiMethod method, int index)
  {
    for (int i = 0; i < index; i++) {
      if (isEquiv(methods[i], method))
        return true;
    }

    return false;
  }
  */

  static boolean isEquiv(ApiMethod oldMethod, ApiMethod method)
  {
    if (! oldMethod.getName().equals(method.getName()))
      return false;

    Class []oldParam = oldMethod.getParameterTypes();
    Class []param = method.getParameterTypes();

    if (oldParam.length != param.length)
      return false;

    for (int j = 0; j < param.length; j++) {
      if (! param[j].equals(oldParam[j]))
        return false;
    }

    return true;
  }

  /**
   * Returns the matching transaction attribute.
   */
  public int getTransactionAttribute(ApiMethod method, String intf)
  {
    if (! isContainerTransaction())
      return EjbMethod.TRANS_BEAN;

    int transaction = EjbMethod.TRANS_REQUIRED;

    EjbMethodPattern ejbMethod = getMethodPattern(null, null);

    if (ejbMethod != null)
      transaction = ejbMethod.getTransactionType();

    ejbMethod = getMethodPattern(method, null);

    if (ejbMethod != null)
      transaction = ejbMethod.getTransactionType();

    ejbMethod = getMethodPattern(method, intf);

    if (ejbMethod != null)
      transaction = ejbMethod.getTransactionType();

    return transaction;
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param method source method
   *
   * @return the matching method or null if non matches.
   */
  ApiMethod getMethod(String methodName, Class []paramTypes)
  {
    return getMethod(getEJBClassWrapper(), methodName, paramTypes);
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param method source method
   *
   * @return the matching method or null if non matches.
   */
  public static ApiMethod getMethod(ApiClass cl, ApiMethod sourceMethod)
  {
    return getMethod(cl, sourceMethod.getName(),
                     sourceMethod.getParameterTypes());
  }

  /**
   * Finds the method in the class.
   *
   * @param apiList owning class
   * @param name method name to match
   * @param params method parameters to match
   *
   * @return the matching method or null if non matches.
   */
  public static ApiMethod getMethod(ArrayList<ApiClass> apiList,
				    String name,
				    Class []param)
  {
    
    for (int i = 0; i < apiList.size(); i++) {
      ApiMethod method = getMethod(apiList.get(i), name, param);

      if (method != null)
        return method;
    }

    return null;
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param name method name to match
   * @param params method parameters to match
   *
   * @return the matching method or null if non matches.
   */
  public static ApiMethod getMethod(ApiClass cl, String name, Class []param)
  {
    return cl.getMethod(name, param);
  }

  public boolean isCMP()
  {
    return false;
  }

  public boolean isCMP1()
  {
    return false;
  }

  public boolean isEJB3()
  {
    return ! (isCMP() || isCMP1());
  }

  static boolean isMatch(ApiMethod methodA, ApiMethod methodB)
  {
    if (methodA == methodB)
      return true;
    else if (methodA == null || methodB == null)
      return false;
    else
      return isMatch(methodA, methodB.getName(), methodB.getParameterTypes());
  }

  static boolean isMatch(ApiMethod method, String name, Class []param)
  {
    if (! method.getName().equals(name))
      return false;

    Class []mparam = method.getParameterTypes();

    if (mparam.length != param.length)
      return false;

    for (int j = 0; j < param.length; j++) {
      if (! mparam[j].equals(param[j]))
        return false;
    }

    return true;
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param name method name to match
   * @param params method parameters to match
   *
   * @return the matching method or null if non matches.
   */
  static ApiMethod findMethod(MethodSignature sig, ApiClass cl, String intf)
  {
    if (cl == null)
      return null;

    for (ApiMethod method : cl.getMethods()) {
      if (sig.isMatch(method, intf))
        return method;
    }

    return null;
  }

  /**
   * Returns all the method in the class.
   */
  static ArrayList<ApiMethod> getMethods(ArrayList<ApiClass> apiList)
  {
    ArrayList<ApiMethod> methodList = new ArrayList<ApiMethod>();

    for (ApiClass api : apiList) {
      for (ApiMethod method : api.getMethods()) {
        if (! methodList.contains(method))
          methodList.add(method);
      }
    }

    return methodList;
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param method source method
   *
   * @return the matching method or null if non matches.
   */
  static ApiMethod findMethod(ArrayList<ApiMethod> methods, ApiMethod method)
  {
    loop:
    for (int i = 0; i < methods.size(); i++) {
      ApiMethod oldMethod = methods.get(i);

      if (oldMethod.equals(method))
	return oldMethod;
    }

    return null;
  }

  /**
   * Returns a printable version of a class.
   */
  static String getClassName(Class cl)
  {
    if (cl == null)
      return "null";
    else if (cl.isArray())
      return getClassName(cl.getComponentType()) + "[]";
    else if (cl.getName().startsWith("java")) {
      int p = cl.getName().lastIndexOf('.');

      return cl.getName().substring(p + 1);
    }
    else
      return cl.getName();
  }

  /**
   * Returns a printable version of a class.
   */
  static String getShortClassName(Class cl)
  {
    if (cl.isArray())
      return getShortClassName(cl.getComponentType()) + "[]";
    else
      return cl.getSimpleName();
  }

  /**
   * Tests is a method is declared in a class.
   */
  boolean classHasMethod(ApiMethod method, ApiClass cl)
  {
    try {
      ApiMethod match = cl.getMethod(method.getName(),
				     method.getParameterTypes());
      return match != null;
    } catch (Exception e) {
      return false;
    }
  }

  void validateException(ApiMethod method, Class e)
    throws ConfigException
  {
    validateExceptions(method, new Class[] { e });
  }

  /**
   * Check that the method throws the expected exceptions.
   *
   * @param method the method to test
   * @param exn the expected exceptions
   */
  void validateExceptions(ApiMethod method, Class []exn)
    throws ConfigException
  {
    Class []methodExceptions = method.getExceptionTypes();

    loop:
    for (int i = 0; i < exn.length; i++) {
      if (RuntimeException.class.isAssignableFrom(exn[i]))
        continue;

      for (int j = 0; j < methodExceptions.length; j++) {
        if (methodExceptions[j].isAssignableFrom(exn[i]))
          continue loop;
      }

      throw new ConfigException(L.l("{2}: '{0}' must throw {1}.",
                                    getFullMethodName(method),
                                    exn[i].getName(),
                                    method.getDeclaringClass().getName()));
    }
  }

  void validateExceptions(ApiMethod caller, ApiMethod callee)
    throws ConfigException
  {
    Class []exn = callee.getExceptionTypes();
    Class missing = findMissingException(caller, exn);

    if (missing != null) {
      throw error(L.l("{0}: '{1}' must throw {2}.",
                      caller.getDeclaringClass().getName(),
                      getFullMethodName(caller),
                      getShortClassName(missing),
                      caller.getDeclaringClass().getName()) +
                  L.l(" {0} must throw all {1}.{2} exceptions.",
                      caller.getName(),
                      getShortClassName(callee.getDeclaringClass()),
                      callee.getName()));
    }
  }

  /**
   * Finds any exception in the exception array that the method isn't
   * throwing.
   *
   * @param method the method which should throw a superset of exceptions.
   * @param exn an array of exceptions the method should throw.
   *
   * @return the first missing exception
   */
  Class findMissingException(ApiMethod method, Class []exn)
    throws ConfigException
  {
    Class []methodExceptions = method.getExceptionTypes();

    for (int i = 0; i < exn.length; i++) {
      if (! hasException(method, exn[i])
	  && ! RuntimeException.class.isAssignableFrom(exn[i]))
        return exn[i];
    }

    return null;
  }

  boolean hasException(ApiMethod method, Class exn)
    throws ConfigException
  {
    Class []methodExceptions = method.getExceptionTypes();

    for (int j = 0; j < methodExceptions.length; j++) {
      if (methodExceptions[j].isAssignableFrom(exn))
        return true;
    }

    return false;
  }

  protected ApiMethod findFirstCreateMethod(ApiClass cl)
    throws ConfigException
  {
    for (ApiMethod method : cl.getMethods()) {
      if (method.getName().startsWith("create"))
	return method;
    }

    return null;
  }

  protected void introspectBean(ApiClass type, String defaultName)
    throws ConfigException
  {
    try {
      setEJBClassWrapper(type);

      String name = getEJBName();

      if (name == null || name.equals(""))
        name = defaultName;

      if (name == null || name.equals("")) {
        String className = type.getName();

        int p = className.lastIndexOf('.');

        if (p > 0)
          name = className.substring(p + 1);
        else
          name = className;
      }

      setEJBName(name);

      Local local = type.getAnnotation(Local.class);
      if (local != null) {
        Object []values = local.value();

        for (int i = 0; i < values.length; i++) {
          if (values[i] instanceof Class) {
            Class localClass = (Class) values[i];

            setLocalWrapper(new ApiClass(localClass));
          }
          else if (values[i] instanceof Class) {
            setLocal((Class) values[i]);
          }
        }
      }

      Remote remote = type.getAnnotation(Remote.class);
      if (remote != null) {
        Object []values = remote.value();

        for (int i = 0; i < values.length; i++) {
          if (values[i] instanceof Class) {
            Class remoteClass = (Class) values[i];

            setRemoteWrapper(new ApiClass(remoteClass));
          }
          else if (values[i] instanceof Class) {
            setRemote((Class) values[i]);
          }
        }

        // ejb/0f08: single interface
        if (values.length == 0) {
	  // XXX: getGenericInterfaces
          Class []ifs = type.getJavaClass().getInterfaces();

          if (ifs.length == 1)
            setRemoteWrapper(new ApiClass(ifs[0]));
        }
      }

      TransactionAttribute xa = type.getAnnotation(TransactionAttribute.class);
      if (xa != null) {
        MethodSignature sig = new MethodSignature();
        sig.setMethodName("*");

        EjbMethodPattern pattern = createMethod(sig);

        setPatternTransaction(pattern, xa);
      }

      configureMethods(type);

      /*
        for (int i = 0; i < _initList.size(); i++)
        addInitProgram(_initList.get(i).getBuilderProgram());
      */
    } catch (ConfigException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  private void configureMethods(ApiClass type)
    throws ConfigException
  {
    for (ApiMethod method : type.getMethods()) {
      TransactionAttribute xa
	= (TransactionAttribute) method.getAnnotation(TransactionAttribute.class);

      if (xa != null) {
        EjbMethodPattern pattern = createMethod(getSignature(method));

        setPatternTransaction(pattern, xa);
      }

      Annotation aroundInvoke = method.getAnnotation(AroundInvoke.class);

      // ejb/0fb8
      if (aroundInvoke != null) {
        _aroundInvokeMethodName = method.getName();
      }

      Annotation timeout = method.getAnnotation(Timeout.class);

      // ejb/0fj0
      if (timeout != null) {
        _timeoutMethodName = method.getName();
      }
    }
  }

  private void setPatternTransaction(EjbMethodPattern pattern,
                                     TransactionAttribute xa)
    throws ConfigException
  {
    TransactionAttributeType xaType = xa.value();

    switch (xaType) {
    case REQUIRED:
      pattern.setTransaction(EjbMethod.TRANS_REQUIRED);
      break;

    case REQUIRES_NEW:
      pattern.setTransaction(EjbMethod.TRANS_REQUIRES_NEW);
      break;

    case MANDATORY:
      pattern.setTransaction(EjbMethod.TRANS_MANDATORY);
      break;

    case SUPPORTS:
      pattern.setTransaction(EjbMethod.TRANS_SUPPORTS);
      break;

    case NOT_SUPPORTED:
      pattern.setTransaction(EjbMethod.TRANS_NOT_SUPPORTED);
      break;

    case NEVER:
      pattern.setTransaction(EjbMethod.TRANS_NEVER);
      break;

    default:
      throw new IllegalStateException();
    }
  }

  private MethodSignature getSignature(ApiMethod method)
    throws ConfigException
  {
    MethodSignature sig = new MethodSignature();

    sig.setMethodName(method.getName());

    Class []paramTypes = method.getParameterTypes();

    for (int i = 0; i < paramTypes.length; i++) {
      sig.addParam(paramTypes[i].getName());
    }

    return sig;
  }

  /**
   * Returns a full method name with arguments.
   */
  static String getFullMethodName(ApiMethod method)
  {
    return getFullMethodName(method.getName(), method.getParameterTypes());
  }

  /**
   * Returns a full method name with arguments.
   */
  static String getFullMethodName(String methodName, Class []params)
  {
    String name = methodName + "(";

    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        name += ", ";

      name += params[i].getSimpleName();
    }

    return name + ")";
  }

  /**
   * Returns an error.
   */
  public ConfigException error(String msg)
  {
    if (_isInit && ! "".equals(_location))
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }
}
