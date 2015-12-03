/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.StatefulTimeout;
import javax.ejb.Timeout;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.interceptor.AroundInvoke;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.DependencyBean;
import com.caucho.config.LineConfigException;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.inject.AnnotatedOverrideMap;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.reflect.AnnotatedMethodImpl;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.config.types.DataSourceRef;
import com.caucho.config.types.DescriptionGroupConfig;
import com.caucho.config.types.EjbLocalRef;
import com.caucho.config.types.EjbRef;
import com.caucho.config.types.EnvEntry;
import com.caucho.config.types.MessageDestinationRef;
import com.caucho.config.types.Period;
import com.caucho.config.types.ResourceEnvRef;
import com.caucho.config.types.ResourceGroupConfig;
import com.caucho.config.types.ResourceRef;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.loader.EnvironmentBean;
import com.caucho.make.ClassDependency;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.Vfs;

/**
 * Configuration for an ejb bean.
 */
public class EjbBean<X> extends DescriptionGroupConfig
  implements EnvironmentBean, DependencyBean
{
  private static final L10N L = new L10N(EjbBean.class);
  private final EjbConfig _ejbConfig;
  private final String _ejbModuleName;
  
  private EjbJar _jar;

  private ClassLoader _loader;

  protected ClassLoader _jClassLoader;

  private String _ejbName;

  private AnnotatedType<X> _rawAnnType;
  private AnnotatedTypeImpl<X> _ejbClass;

  // The published name as used by IIOP, Hessian, and
  // jndi-remote-prefix/jndi-local-prefix
  private String _mappedName;

  private String _location = "";
  private String _filename;
  private int _line;
  
  private boolean _isInit; // used for error messsage line #

  private InjectionTarget<X> _injectionTarget;

  protected ArrayList<AnnotatedType<? super X>> _remoteList
    = new ArrayList<AnnotatedType<? super X>>();

  protected ArrayList<AnnotatedType<? super X>> _localList
    = new ArrayList<AnnotatedType<? super X>>();
  
  private boolean _isLocalBean;
  private AnnotatedType<X> _localBean;

  // protected BeanGenerator<X> _bean;

  private boolean _isAllowPOJO = true;

  protected boolean _isContainerTransaction = true;
  
  private ConcurrencyManagementType _concurrencyManagementType;
  private StatefulTimeout _statefulTimeout;

  ArrayList<PersistentDependency> _dependList
    = new ArrayList<PersistentDependency>();

  ArrayList<PersistentDependency> _configDependList
    = new ArrayList<PersistentDependency>();

  ArrayList<String> _beanDependList = new ArrayList<String>();

  private ArrayList<EjbMethodPattern<X>> _methodList
    = new ArrayList<EjbMethodPattern<X>>();

  private ArrayList<EjbMethodPattern<X>> _beanMethodList
    = new ArrayList<EjbMethodPattern<X>>();

  private ContainerProgram _initProgram;
  
  private ArrayList<ConfigProgram> _postConstructList
    = new ArrayList<ConfigProgram>();
  
  private ContainerProgram _serverProgram;
  private ArrayList<ResourceGroupConfig> _resourceList
    = new ArrayList<ResourceGroupConfig>();


  private ArrayList<Interceptor> _defaultInterceptors
    = new ArrayList<Interceptor>();

  private ArrayList<Interceptor> _classInterceptors
    = new ArrayList<Interceptor>();

  private ArrayList<AroundInvokeConfig> _aroundInvoke
    = new ArrayList<AroundInvokeConfig>();

  private ArrayList<AsyncConfig> _asyncConfig
    = new ArrayList<AsyncConfig>();
  
  private String _timeoutMethodName;

  private long _transactionTimeout;

  /**
   * Creates a new entity bean configuration.
   */
  public EjbBean(EjbConfig ejbConfig, String ejbModuleName)
  {
    _ejbConfig = ejbConfig;
    _ejbModuleName = ejbModuleName;
    
    _loader = ejbConfig.getEjbContainer().getClassLoader();
  }

  /**
   * Creates a new entity bean configuration.
   */
  public EjbBean(EjbConfig ejbConfig,
                 AnnotatedType<X> rawAnnType,
                 AnnotatedType<X> annType,
                 String ejbModuleName)
  {
    _ejbConfig = ejbConfig;

    _rawAnnType = rawAnnType;
    _ejbClass = AnnotatedTypeImpl.create(annType);
    _ejbModuleName = ejbModuleName;
    
    setEJBClass(_ejbClass.getJavaClass());

    _loader = ejbConfig.getEjbContainer().getClassLoader();
  }

  public EjbConfig getConfig()
  {
    return _ejbConfig;
  }

  public EjbManager getEjbContainer()
  {
    return _ejbConfig.getEjbContainer();
  }
  
  public InjectManager getCdiManager()
  {
    return getEjbContainer().getInjectManager();
  }

  public String getModuleName()
  {
    return _ejbModuleName;
  }
  
  public EjbJar getJar()
  {
    return _jar;
  }
  
  public void setJar(EjbJar jar)
  {
    _jar = jar;
  }

  public void setAroundInvoke(AroundInvokeConfig aroundInvoke)
  {
    _aroundInvoke.add(aroundInvoke);
  }

  public void addAsyncMethod(AsyncConfig async)
  {
    _asyncConfig.add(async);
  }

  public void setInjectionTarget(InjectionTarget<X> injectTarget)
  {
    _injectionTarget = injectTarget;
  }

  public InjectionTarget<X> getInjectionTarget()
  {
    return _injectionTarget;
  }

  /**
   * Returns the remove-method for the given method.
   */
  /*
  public RemoveMethod getRemoveMethod(Method method)
  {
    for (RemoveMethod removeMethod : _removeMethods) {
      if (removeMethod.isMatch(method))
        return removeMethod;
    }

    return null;
  }
  */

  /**
   * Returns the remove-method list.
   */
  /*
  public ArrayList<RemoveMethod> getRemoveMethods()
  {
    return _removeMethods;
  }
  */

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
  @Configurable
  public void addRemoveMethod(RemoveMethod<X> removeMethod)
  {
    // _removeMethods.add(removeMethod);
    _beanMethodList.add(removeMethod);
  }

  /**
   * Adds a new concurrent-method
   */
  @Configurable
  public void addConcurrentMethod(ConcurrentMethod<X> concurrentMethod)
  {
    // _removeMethods.add(removeMethod);
    _beanMethodList.add(concurrentMethod);
  }
  
  public void addAfterBeginMethod(AfterBeginMethod<X> method)
  {
    _beanMethodList.add(method);
  }
  
  public void addBeforeCompletionMethod(BeforeCompletionMethod<X> method)
  {
    _beanMethodList.add(method);
  }

  /**
   * Adds a new interceptor.
   */
  public void addInterceptor(Interceptor interceptor, boolean isDefault)
  {
    if (isDefault)
      _defaultInterceptors.add(interceptor);
    else
      _classInterceptors.add(interceptor);
  }

  public String getEJBModuleName()
  {
    return _ejbModuleName;
  }

  /**
   * Returns the class loader.
   */
  @Override
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  protected Class<?> loadClass(String className)
  {
    try {
      return Class.forName(className, false, _loader);
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Sets the location
   */
  public void setConfigLocation(String filename, int line)
  {
    if (_filename == null) {
      _filename = filename;
      _line = line;
    }

    if (_location == null)
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
   * Gets the file name
   */
  public String getFilename()
  {
    return _filename;
  }

  /**
   * Gets the line
   */
  public int getLine()
  {
    return _line;
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
  public void setEJBClass(Class<X> ejbClass)
    throws ConfigException
  {
    if (_ejbClass != null)
      return;
    
    AnnotatedTypeImpl<X> annType;
    
    AnnotatedType<X> refType = ReflectionAnnotatedFactory.introspectType(ejbClass);
    
    annType = new AnnotatedTypeImpl<X>(refType);
    
    setEJBClassWrapper(annType);
  }

  /**
   * Sets the ejb implementation class.
   */
  public void setEJBClassWrapper(AnnotatedType<X> ejbClass)
    throws ConfigException
  {
    if (_ejbClass != null && ! _ejbClass.getJavaClass().getName().equals(ejbClass.getJavaClass().getName()))
      throw error(L.l("ejb-class '{0}' cannot be redefined.  Old value is '{1}'.",
                      _ejbClass.getJavaClass().getName(), 
                      ejbClass.getJavaClass().getName()));


    _ejbClass = AnnotatedTypeImpl.create(ejbClass);

    int modifiers = _ejbClass.getJavaClass().getModifiers();
    
    /*
    if (! _ejbClass.isPublic())
      throw error(L.l("'{0}' must be public.  Bean implementations must be public.", ejbClass.getName()));
      */
    if (Modifier.isPrivate(modifiers))
      throw error(L.l("'{0}' must be public.  Bean implementations must be public.", ejbClass.getJavaClass().getName()));

    if (Modifier.isFinal(modifiers))
      throw error(L.l("'{0}' must not be final.  Bean implementations must not be final.", ejbClass.getJavaClass().getName()));

    if (_ejbClass.getJavaClass().isInterface())
      throw error(L.l("'{0}' must not be an interface.  Bean implementations must be classes.", ejbClass.getJavaClass().getName()));

    AnnotatedMethod<? super X> method = getMethod("finalize", new Class[0]);

    if (method != null
        && ! method.getJavaMember().getDeclaringClass().equals(Object.class)) {
      throw error(L.l("'{0}' may not implement finalize().  Bean implementations may not implement finalize().", 
                      method.getJavaMember().getDeclaringClass().getName()));
    }
  }

  /**
   * Gets the ejb implementation class.
   */
  public Class<X> getEJBClass()
  {
    try {
      if (_ejbClass == null)
        return null;

      return (Class<X>) Class.forName(_ejbClass.getJavaClass().getName(), false, getClassLoader());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public AnnotatedType<X> getRawAnnotatedType()
  {
    return _rawAnnType;
  }

  public AnnotatedTypeImpl<X> getAnnotatedType()
  {
    return _ejbClass;
  }

  /**
   * Gets the ejb implementation class.
   */
  public String getEJBFullClassName()
  {
    return _ejbClass.getJavaClass().getName();
  }

  /**
   * Gets the ejb implementation class.
   */
  public String getEJBClassName()
  {
    String s = _ejbClass.getJavaClass().getName();
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
  
  public <T> void addRemote(Class<T> remote)
    throws ConfigException
  {
    BaseType type = getCdiManager().createTargetBaseType(remote);
    
    addRemoteType(type);
  }
  
  /**
   * Sets the ejb remote interface
   */
  public <T> void addRemoteType(BaseType remote)
    throws ConfigException
  {
    AnnotatedTypeImpl<X> annType;
    
    AnnotatedType<?> refType = ReflectionAnnotatedFactory.introspectType(remote);
    
    annType = new AnnotatedTypeImpl(refType);
    
    addRemoteWrapper(annType);
  }

  /**
   * Sets the remote interface class.
   */
  public void addRemoteWrapper(AnnotatedType<? super X> remote)
    throws ConfigException
  {
    Class<?> remoteClass = remote.getJavaClass();
    int modifiers = remoteClass.getModifiers();
    
    if (! Modifier.isPublic(modifiers))
      throw error(L.l("'{0}' must be public.  <remote> interfaces must be public.", remoteClass.getName()));

    if (! remoteClass.isInterface())
      throw error(L.l("'{0}' must be an interface. <remote> interfaces must be interfaces.", remoteClass.getName()));

    if (! _remoteList.contains(remote)) {
      _remoteList.add(remote);
    }
  }

  /**
   * Gets the remote interface class.
   */
  public ArrayList<AnnotatedType<? super X>> getRemoteList()
  {
    return _remoteList;
  }

  /**
   * Sets the ejb local interface
   */
  public void addLocal(Class<?> local)
    throws ConfigException
    {
      BaseType type = getCdiManager().createTargetBaseType(local);
      
      addLocalType(type);
    }
  
  protected void addLocalType(BaseType local)
  {
    AnnotatedTypeImpl<X> annType;
    
    AnnotatedType<?> refType = ReflectionAnnotatedFactory.introspectType(local);
    
    annType = new AnnotatedTypeImpl(refType);
    
    addLocalWrapper(annType);
  }

  /**
   * Sets the local interface class.
   */
  public void addLocalWrapper(AnnotatedType<? super X> local)
    throws ConfigException
  {
    Class<?> localClass = local.getJavaClass();
    /*
    int modifiers = localClass.getModifiers();
    
    if (! Modifier.isPublic(modifiers))
      throw error(L.l("'{0}' must be public.  <local> interfaces must be public.", localClass.getName()));
      */

    if (! localClass.isInterface())
      throw error(L.l("'{0}' must be an interface. <local> interfaces must be interfaces.", localClass.getName()));

    for (int i = _localList.size() - 1; i >= 0; i--) {
      AnnotatedType<? super X> oldLocal = _localList.get(i);
      
      Class<?> oldClass = oldLocal.getJavaClass();

      // ioc/1235 vs ejb/4040
      if (localClass.equals(oldClass))
        return;
      
      /*
      if (localClass.isAssignableFrom(oldClass))
        return;
      
      if (oldClass.isAssignableFrom(localClass)) {
        _localList.set(i, local);
        return;
      }
      else if (localClass.isAssignableFrom(oldClass))
        return;
        */
    }

    _localList.add(local);
  }

  /**
   * Gets the local interface class.
   */
  public ArrayList<AnnotatedType<? super X>> getLocalList()
  {
    return _localList;
  }
  
  public AnnotatedType<X> getLocalBean()
  {
    return _localBean;
  }

  /**
   * Returns true if the transaction type is container.
   */
  public boolean isContainerTransaction()
  {
    return _isContainerTransaction;
  }

  /**
   * Returns true if the transaction type is container.
   */
  public void setContainerTransaction(boolean isContainerTransaction)
  {
    _isContainerTransaction = isContainerTransaction;
  }
  
  public void setConcurrencyManagementType(String type)
  {
    if ("Container".equals(type))
      _concurrencyManagementType = ConcurrencyManagementType.CONTAINER;
    else if ("Bean".equals(type))
      _concurrencyManagementType = ConcurrencyManagementType.BEAN;
    else
      throw new ConfigException(L.l("'{0}' is an unknown concurrency-management-type",
                                    type));
  }
  
  public void setStatefulTimeout(EjbTimeout timeout)
  {
    _statefulTimeout = new StatefulTimeoutLiteral(timeout.getTimeoutValue());
  }

  /**
   * Adds a method.
   */
  public EjbMethodPattern<X> createMethod(MethodSignature sig)
  {
    for (int i = 0; i < _methodList.size(); i++) {
      EjbMethodPattern<X> method = _methodList.get(i);

      if (method.getSignature().equals(sig))
        return method;
    }

    EjbMethodPattern<X> method = new EjbMethodPattern<X>(this, sig);

    _methodList.add(method);

    return method;
  }

  /**
   * Adds a method.
   */
  public void addMethod(EjbMethodPattern<X> method)
  {
    _methodList.add(method);
  }
  
  public boolean isMatch(AnnotatedMethod<?> method)
  {
    if (_methodList.size() == 0)
      return true;
    
    for (EjbMethodPattern<?> ejbMethod : _methodList) {
      if (ejbMethod.isMatch(method))
        return true;
    }
    
    return false;
  }

  /**
   * Gets the best method.
   */
  public EjbMethodPattern<X> getMethodPattern(AnnotatedMethod<?> method, 
                                              String intf)
  {
    EjbMethodPattern<X> bestMethod = null;
    int bestCost = -1;

    for (int i = 0; i < _methodList.size(); i++) {
      EjbMethodPattern<X> ejbMethod = _methodList.get(i);
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
  public ArrayList<EjbMethodPattern<X>> getMethodList()
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
  
  public void addBusinessLocal(Class<?> localApi)
  {
    addLocal(localApi);
  }
  
  public void addBusinessRemote(Class<?> remoteApi)
  {
    addRemote(remoteApi);
  }
  
  public void setLocalBean(boolean isLocal)
  {
    _isLocalBean = true;
    
    if (_localBean == null)
      _localBean = getAnnotatedType();
  }

  /**
   * Gets the transaction timeout.
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  public MessageDestinationRef createMessageDestinationRef()
  {
    return new MessageDestinationRef(Vfs.lookup());
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
  public void addDependency(Class<?> cl)
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
  public void addInitProgram(ConfigProgram init)
  {
    if (_initProgram == null)
      _initProgram = new ContainerProgram();

    _initProgram.addProgram(init);
  }

  /**
   * Adds an undefined value, e.g. env-entry
   */
  public void addBuilderProgram(ConfigProgram init)
  {
    if (_serverProgram == null)
      _serverProgram = new ContainerProgram();

    _serverProgram.addProgram(init);
  }
  
  //
  // references and resources
  //
  
  public DataSourceRef createDataSource()
  {
    DataSourceRef def = new DataSourceRef();
    
    def.setProgram(true);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    def.setJndiClassLoader(loader);
    
    _resourceList.add(def);
    
    return def;
  }
  
  public EnvEntry createEnvEntry()
  {
    EnvEntry env = new EnvEntry();
    
    env.setProgram(true);
    // ejb/7038, ejb/8203, ejb/8220, tck

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    env.setJndiClassLoader(loader);
    
    _resourceList.add(env);
    
    return env;
  }
  
  public EjbRef createEjbRef()
  {
    EjbRef ref = new EjbRef();
    
    ref.setProgram(true);
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    ref.setJndiClassLoader(loader);
    
    _resourceList.add(ref);
    
    return ref;
  }
  
  public EjbLocalRef createEjbLocalRef()
  {
    EjbLocalRef ref = new EjbLocalRef();
    
    ref.setProgram(true);
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    ref.setJndiClassLoader(loader);
    
    _resourceList.add(ref);
    
    return ref;
  }
  
  public ResourceRef createResourceRef()
  {
    ResourceRef ref= new ResourceRef();
    
    ref.setProgram(true);
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    ref.setJndiClassLoader(loader);
    
    _resourceList.add(ref);
    
    return ref;
  }
  
  public ResourceEnvRef createResourceEnvRef()
  {
    ResourceEnvRef ref = new ResourceEnvRef();
    
    ref.setProgram(true);
    
    _resourceList.add(ref);
    
    return ref;
  }
  
  public ArrayList<ResourceGroupConfig> getResourceList()
  {
    return _resourceList;
  }

  public void setInit(ContainerProgram init)
  {
    if (_initProgram == null)
      _initProgram = new ContainerProgram();

    _initProgram.addProgram(init);
  }

  public void addPostConstruct(PostConstructType<X> postConstruct)
  {
    _beanMethodList.add(postConstruct);
    
    // _postConstructList.add(postConstruct.getProgram(getEJBClass()));
  }

  /**
   * Gets the init program.
   */
  public ContainerProgram getInitProgram()
  {
    /*
    if (_postConstructList != null) {
      if (_initProgram == null)
        _initProgram = new ContainerProgram();

      for (ConfigProgram program : _postConstructList)
        _initProgram.addProgram(program);

      _postConstructList = null;
    }
    */

    return _initProgram;
  }

  /**
   * Gets the server program.
   */
  public ContainerProgram getServerProgram()
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
      if (_isInit)
        return;
      _isInit = true;
      
      if (getAnnotatedType() == null)
        throw error(L.l("ejb-class is not defined for '{0}'",
                        getEJBName()));

      for (EjbMethodPattern<X> methodPattern : _methodList) {
        for (AnnotatedType<?> localList : _localList) {
          for (AnnotatedMethod<?> apiMethod : localList.getMethods()) {
            methodPattern.configure(apiMethod);
          }
        }

        for (AnnotatedType<?> remoteList : _remoteList) {
          for (AnnotatedMethod<?> apiMethod : remoteList.getMethods()) {
            methodPattern.configure(apiMethod);
          }
        }
      }

      // XXX: add local api

      introspect();
      
      initIntrospect();
      
      addInterceptors();
    } catch (ConfigException e) {
      throw ConfigException.createLine(_location, e);
    }
  }

  protected void introspect()
  {
    // _bean.introspect();
  }
  
  /**
   * Creates the BeanGenerator generator instance
   */
  protected BeanGenerator<X> createBeanGenerator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Configure initialization.
   */
  public void initIntrospect()
    throws ConfigException
  {
    // configureBeanMethods(getAnnotatedType());
    
    boolean isExcludeDefault = false;

    for (InterceptorBinding interceptor :
          _ejbConfig.getInterceptorBinding(getEJBName(), isExcludeDefault)) {
      introspectInterceptor(interceptor);
    }
    
    configureAroundInvoke(getAnnotatedType());
    configureAsync(getAnnotatedType());
    configureMethods(getAnnotatedType());
    
    if (_concurrencyManagementType != null)
      getAnnotatedType().addAnnotation(new ConcurrencyManagementLiteral(_concurrencyManagementType));
    
    if (_statefulTimeout != null)
      getAnnotatedType().addAnnotation(_statefulTimeout);
    
    if (_isLocalBean)
      _localBean = getAnnotatedType();
  }
  
  private void introspectInterceptor(InterceptorBinding binding)
  {
    ArrayList<String> interceptorClasses = new ArrayList<String>();

    if (binding.getMethodList().isEmpty()) {
      for (Class<?> iClass : binding.getInterceptors()) {
        interceptorClasses.add(iClass.getName());
      }

      if (interceptorClasses.isEmpty()) {
        InterceptorOrder interceptorOrder = binding.getInterceptorOrder();

        if (interceptorOrder != null) {
          for (Class<?> cl : interceptorOrder.getInterceptorClasses()) {
            interceptorClasses.add(cl.getName());
          }
        }
      }
      
      AnnotatedTypeImpl<?> typeImpl = (AnnotatedTypeImpl<?>) getAnnotatedType();

      if (binding.isExcludeDefaultInterceptors())
        typeImpl.addAnnotation(new ExcludeDefaultInterceptorsLiteral());
      
      if (binding.isExcludeClassInterceptors())
        typeImpl.addAnnotation(new ExcludeClassInterceptorsLiteral());
    }
    else {
      for (AnnotatedMethod<?> method : getAnnotatedType().getMethods()) {
        if (binding.isMatch(method)) {
          if (method instanceof AnnotatedMethodImpl<?>) {
            AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) method;
            
            if (binding.getAnnotation() != null)
              methodImpl.addAnnotation(binding.mergeAnnotation(methodImpl));
            
            if (binding.isExcludeClassInterceptors())
              methodImpl.addAnnotationIfAbsent(new ExcludeClassInterceptorsLiteral());
          }
        }
      }
    }

    for (String className : interceptorClasses) {
      /*
      Interceptor interceptor = getInterceptor(className);

      // ejb/0fb5 vs ejb/0fb6
      if (interceptor != null) {
        _interceptors.remove(interceptor);

        addInterceptor(interceptor, binding.isDefault());
      }
      else {
      */
      Interceptor interceptor = _ejbConfig.getInterceptor(className);

      if (interceptor == null) {
        interceptor = new Interceptor();
        interceptor.setInterceptorClass(className);
        
        _ejbConfig.addInterceptor(interceptor);
      }

      interceptor.init();

      addInterceptor(interceptor, binding.isDefault());
    }
  }

  private void addInterceptors()
  {
    if (_defaultInterceptors.size() > 0) {
      addDefaultInterceptors(createInterceptors(_defaultInterceptors));
    }
    
    if (_classInterceptors.size() > 0) {
      addClassInterceptors(createInterceptors(_classInterceptors));
    }
  }

  private Class<?> []createInterceptors(ArrayList<Interceptor> interceptorList)
  {
    Class<?> []interceptors = new Class<?>[interceptorList.size()];
    
    for (int i = 0; i < interceptorList.size(); i++) {
      String className = interceptorList.get(i).getInterceptorClass();
      Class<?> cl = null;
    
      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
        cl = Class.forName(className, false, loader);
        
        interceptors[i] = cl;
      } catch (ClassNotFoundException e) {
        throw ConfigException.create(e);
      }
    }
   
    return interceptors;
  }
  
  private void addClassInterceptors(Class<?> []cl)
  {
    _ejbClass.addAnnotation(new InterceptorsLiteral(cl));
  }
  
  private void addDefaultInterceptors(Class<?> []cl)
  {
    _ejbClass.addAnnotation(new InterceptorsDefaultLiteral(cl));
  }

  /**
   * Deploys the bean.
   */
  public AbstractEjbBeanManager<X> deployServer(EjbManager ejbContainer,
                                                EjbLazyGenerator<X> lazyGenerator)
    throws ClassNotFoundException, ConfigException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Validates the local interface.
   */
  protected <T> void validateLocal(AnnotatedType<T> objectType)
    throws ConfigException
  {
    validateRemote(objectType);
  }

  /**
   * Validates the remote interface.
   */
  protected <T> void validateRemote(AnnotatedType<T> objectType)
    throws ConfigException
  {
    Class<T> objectClass = objectType.getJavaClass();

    String objectName = objectClass.getName();

    if (! objectClass.isInterface())
      throw error(L.l("'{0}' must be an interface", objectName));

    for (AnnotatedMethod<? super T> method : objectType.getMethods()) {
      Method javaMethod = method.getJavaMember();
      
      String name = javaMethod.getName();
      Class<?> []param = javaMethod.getParameterTypes();

      if (name.startsWith("ejb")) {
        throw error(L.l("'{0}' forbidden in {1}.  Local or remote interfaces may not define ejbXXX methods.",
                        getFullMethodName(method),
                        objectName));
      }

      // ejb/11d6
      //Type returnType = javaMethod.getGenericReturnType();

      AnnotatedMethod<? super X> implMethod =
        validateRemoteImplMethod(javaMethod.getName(), param,
                                 method, objectType);

      /*
      InjectManager manager = InjectManager.create();
      BaseType target = manager.createTargetBaseType(returnType);
      BaseType source = manager.createSourceBaseType(returnType);

      if (! target.isAssignableFrom(source)) {
        throw error(L.l("{0}: '{1}' must return {2} to match {3}.{4}.  Business methods must return a type assignable to interface return type.",
                        javaMethod.getDeclaringClass().getName(),
                        getFullMethodName(method),
                        implMethod.getJavaMember().getReturnType().getName(),
                        implMethod.getJavaMember().getDeclaringClass().getSimpleName(),
                        getFullMethodName(implMethod)));
      }*/

      validateExceptions(method, implMethod.getJavaMember().getExceptionTypes());
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
  private <T> AnnotatedMethod<? super X>
  validateRemoteImplMethod(String methodName,
                           Class<?> []param,
                           AnnotatedMethod<? super T> sourceMethod,
                           AnnotatedType<T> sourceClass)
    throws ConfigException
  {
    AnnotatedMethod<? super X> method = null;
    AnnotatedType<X> beanClass = getAnnotatedType();

    // method = AnnotatedTypeUtil.findMethod(beanClass, methodName, param);
    method = AnnotatedTypeUtil.findMethod(beanClass, sourceMethod);

    if (method == null && sourceMethod != null) {
      throw error(L.l("{0}: '{1}' needed on the implementation class to match {2}.{3}",
                      beanClass.getJavaClass().getName(),
                      getFullMethodName(methodName, param),
                      sourceMethod.getJavaMember().getDeclaringClass().getSimpleName(),
                      getFullMethodName(sourceMethod)));
    }
    else if (method == null) {
      throw error(L.l("{0}: '{1}' expected",
                      beanClass.getJavaClass().getName(),
                      getFullMethodName(methodName, param)));
    }
    
    Method javaMethod = method.getJavaMember();
    int modifiers = javaMethod.getModifiers();

    if (! Modifier.isPublic(modifiers)) {
      throw error(L.l("{0}: '{1}' must be public",
                      beanClass.getJavaClass().getName(),
                      getFullMethodName(methodName, param)));
    }

    if (method.isStatic()) {
      throw error(L.l("{0}: '{1}' must not be static",
                      beanClass.getJavaClass().getName(),
                      getFullMethodName(methodName, param)));
    }

    if (Modifier.isFinal(modifiers)) {
      throw error(L.l("{0}: '{1}' must not be final.",
                      beanClass.getJavaClass().getName(),
                      getFullMethodName(methodName, param),
                      beanClass.getJavaClass().getName()));
    }

    return method;
  }

  public String getSkeletonName()
  {
     
    // XXX: needs to match generator
    
    StringBuilder sb = new StringBuilder();
    sb.append(getEJBClass().getName());
    sb.append("__");
    sb.append(getBeanType()).append("Proxy");

    return JavaClassGenerator.cleanClassName(sb.toString());
  }

  /**
   * @return Type of bean (Stateful, Stateless, etc.)
   */
  protected String getBeanType()
  {
    return "Bean";
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param method source method
   *
   * @return the matching method or null if non matches.
   */
  protected AnnotatedMethod<? super X> 
  getMethod(String methodName, Class<?> []paramTypes)
  {
    return AnnotatedTypeUtil.findMethod(getAnnotatedType(), methodName, paramTypes);
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param method source method
   *
   * @return the matching method or null if non matches.
   */
  public static <X,T> AnnotatedMethod<? super X>
  getMethod(AnnotatedType<X> cl, AnnotatedMethod<? extends T> sourceMethod)
  {
    Method method = sourceMethod.getJavaMember();
    
    return AnnotatedTypeUtil.findMethod(cl,
                                        method.getName(),
                                        method.getParameterTypes());
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
  public static <X> AnnotatedMethod<? super X>
  getMethod(AnnotatedType<X> cl, String name, Class<?> []param)
  {
    return AnnotatedTypeUtil.findMethod(cl, name, param);
  }

  public boolean isCMP()
  {
    return false;
  }

  public boolean isCMP1()
  {
    return false;
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
  public static <Y> AnnotatedMethod<? super Y> 
  findMethod(MethodSignature sig, AnnotatedType<Y> cl, String intf)
  {
    if (cl == null)
      return null;

    for (AnnotatedMethod<? super Y> method : cl.getMethods()) {
      if (sig.isMatch(method, intf))
        return method;
    }

    return null;
  }

  /**
   * Returns a printable version of a class.
   */
  public static String getClassName(Class<?> cl)
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
  public static String getShortClassName(Class<?> cl)
  {
    if (cl.isArray())
      return getShortClassName(cl.getComponentType()) + "[]";
    else
      return cl.getSimpleName();
  }

  /**
   * Tests is a method is declared in a class.
   */
  public boolean classHasMethod(AnnotatedType<?> cl, 
                                AnnotatedMethod<?> method)
  {
    return AnnotatedTypeUtil.findMethod(cl, method) != null;
  }

  public void validateException(AnnotatedMethod<?> method, Class<?> e)
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
  public void validateExceptions(AnnotatedMethod<?> method, Class<?> []exn)
    throws ConfigException
  {
    Method javaMethod = method.getJavaMember();
    
    Class<?> []methodExceptions = javaMethod.getExceptionTypes();

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
                                    javaMethod.getDeclaringClass().getName()));
    }
  }

  public void validateExceptions(AnnotatedMethod<?> caller, 
                                 AnnotatedMethod<? super X> callee)
    throws ConfigException
  {
    Method callerMethod = caller.getJavaMember();
    Method calleeMethod = callee.getJavaMember();
    
    Class<?> []exn = calleeMethod.getExceptionTypes();
    Class<?> missing = findMissingException(caller, exn);

    if (missing != null) {
      throw error(L.l("{0}: '{1}' must throw {2}.",
                      callerMethod.getDeclaringClass().getName(),
                      getFullMethodName(caller),
                      getShortClassName(missing),
                      callerMethod.getDeclaringClass().getName()) +
                  L.l(" {0} must throw all {1}.{2} exceptions.",
                      callerMethod.getName(),
                      calleeMethod.getDeclaringClass().getSimpleName(),
                      calleeMethod.getName()));
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
  Class<?> findMissingException(AnnotatedMethod<?> method, Class<?> []exn)
  {
    for (int i = 0; i < exn.length; i++) {
      if (! AnnotatedTypeUtil.hasException(method, exn[i])
          && ! RuntimeException.class.isAssignableFrom(exn[i]))
        return exn[i];
    }

    return null;
  }

  protected <T> AnnotatedMethod<? super T>
  findFirstCreateMethod(AnnotatedType<T> cl)
  {
    for (AnnotatedMethod<? super T> method : cl.getMethods()) {
      if (method.getJavaMember().getName().startsWith("create"))
        return method;
    }

    return null;
  }

  protected void introspectBean(AnnotatedType<X> type, String defaultName)
    throws ConfigException
  {
    try {
      setEJBClassWrapper(type);

      String name = getEJBName();

      if (name == null || name.equals(""))
        name = defaultName;

      if (name == null || name.equals("")) {
        name = type.getJavaClass().getSimpleName();
      }

      setEJBName(name);

      Local local = type.getAnnotation(Local.class);
      if (local != null) {
        for (Class<?> localClass : local.value()) {
          addLocal(localClass);
        }
      }
      
      if (type.isAnnotationPresent(LocalBean.class)) {
        _localBean = type;
      }
      
      if (_localList.size() == 0)
        _localBean = type;
      
      Remote remote = type.getAnnotation(Remote.class);
      if (remote != null) {
        for (Class<?> localClass : local.value()) {
          addRemote(localClass);
        }

        /*
        // ejb/0f08: single interface
        if (values.length == 0) {
          // XXX: getGenericInterfaces
          Class []ifs = type.getJavaClass().getInterfaces();

          if (ifs.length == 1)
            setRemoteWrapper(new ApiClass(ifs[0]));
        }
        */
      }

      TransactionAttribute xa = type.getAnnotation(TransactionAttribute.class);
      if (xa != null) {
        MethodSignature sig = new MethodSignature();
        sig.setMethodName("*");

        EjbMethodPattern<X> pattern = createMethod(sig);

        setPatternTransaction(pattern, xa);
      }

      configureMethods(getAnnotatedType());
      configureAroundInvoke(getAnnotatedType());
      configureAsync(getAnnotatedType());
      /*
        for (int i = 0; i < _initList.size(); i++)
        addInitProgram(_initList.get(i).getBuilderProgram());
      */
    } catch (ConfigException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.createLine(_location, e);
    }
  }

  private void configureAroundInvoke(AnnotatedType<X> type)
  {
    AnnotatedTypeImpl<X> typeImpl = (AnnotatedTypeImpl<X>) type;

    for (AroundInvokeConfig aroundInvoke : _aroundInvoke) {
      configureAroundInvoke(typeImpl, type.getJavaClass(), aroundInvoke);
    }
  }
  
  private void configureAroundInvoke(AnnotatedTypeImpl<X> type,
                                     Class<?> cl, 
                                     AroundInvokeConfig aroundInvoke)
  {
    if (cl == null)
      return;

    for (Method method : cl.getDeclaredMethods()) {
      if (aroundInvoke.isMatch(method)) {
        AnnotatedMethod<?> annMethod = AnnotatedTypeUtil.findMethod(type, method);
        
        if (annMethod == null) {
          annMethod = type.createMethod(method);
        }
          
        AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) annMethod; 
        
        methodImpl.addAnnotation(new AroundInvokeLiteral());

        AnnotatedOverrideMap.putMethod(method, methodImpl);
        
        return;
      }
    }
    
    configureAroundInvoke(type, cl.getSuperclass(), aroundInvoke);
  }

  private void configureAsync(AnnotatedType<X> type)
  {
    AnnotatedTypeImpl<X> typeImpl = (AnnotatedTypeImpl<X>) type;

    for (AsyncConfig async : _asyncConfig) {
      configureAsync(typeImpl, type.getJavaClass(), async);
    }
  }
  
  private void configureAsync(AnnotatedTypeImpl<X> type,
                              Class<?> cl, 
                              AsyncConfig async)
  {
    if (cl == null)
      return;

    for (Method method : cl.getDeclaredMethods()) {
      if (async.isMatch(method)) {
        AnnotatedMethod<?> annMethod = AnnotatedTypeUtil.findMethod(type, method);
        
        if (annMethod == null) {
          annMethod = type.createMethod(method);
        }
          
        AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) annMethod; 
        
        methodImpl.addAnnotation(new AsynchronousLiteral());

        AnnotatedOverrideMap.putMethod(method, methodImpl);
        
        return;
      }
    }
    
    configureAsync(type, cl.getSuperclass(), async);
  }
  
  private void configureMethods(AnnotatedTypeImpl<X> type)
  {
    if (_beanMethodList.size() == 0) {
      return;
    }
    
    for (AnnotatedMethod<?> method : type.getMethodsForUpdate()) {
      for (EjbMethodPattern<?> cfgMethod : _beanMethodList) {
        if (cfgMethod.isMatch(method)) {
          cfgMethod.configure(method);
        }
      }
    }
  }
    
  private void setPatternTransaction(EjbMethodPattern<X> pattern,
                                     TransactionAttribute xa)
    throws ConfigException
  {
    TransactionAttributeType xaType = xa.value();

    pattern.setTransaction(xaType);
  }

  private MethodSignature getSignature(AnnotatedMethod<?> annMethod)
    throws ConfigException
  {
    MethodSignature sig = new MethodSignature();
    
    Method method = annMethod.getJavaMember();

    sig.setMethodName(method.getName());

    Class<?> []paramTypes = method.getParameterTypes();

    for (int i = 0; i < paramTypes.length; i++) {
      sig.addParam(paramTypes[i].getName());
    }

    return sig;
  }

  /**
   * Returns a full method name with arguments.
   */
  public static String getFullMethodName(AnnotatedMethod<?> method)
  {
    Method javaMethod = method.getJavaMember();
    
    return getFullMethodName(javaMethod.getName(),
                             javaMethod.getParameterTypes());
  }

  /**
   * Returns a full method name with arguments.
   */
  public static String getFullMethodName(String methodName, Class<?> []params)
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
    if (_isInit && _filename != null)
      return new LineConfigException(_filename, _line, msg);
    else if (_isInit && ! "".equals(_location))
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }

  /**
   * Returns an error.
   */
  public RuntimeException error(Exception e)
  {
    if (_filename != null)
      return LineConfigException.create(_filename, _line, e);
    else if (_location != null)
      return ConfigException.createLine(_location, e);
    else
      return ConfigException.create(e);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ejbName + "]";
  }
}
