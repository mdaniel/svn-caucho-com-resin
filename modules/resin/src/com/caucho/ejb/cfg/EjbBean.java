/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.lang.ref.SoftReference;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.lang.reflect.*;

import javax.ejb.*;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassWrapper;
import com.caucho.bytecode.JClassLoader;
import com.caucho.bytecode.JClassLoaderWrapper;
import com.caucho.bytecode.JClassDependency;

import com.caucho.util.*;

import com.caucho.log.Log;

import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;

import com.caucho.vfs.Path;
import com.caucho.vfs.Depend;

import com.caucho.make.PersistentDependency;
import com.caucho.make.ClassDependency;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentBean;

import com.caucho.config.DependencyBean;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;

import com.caucho.config.types.Period;

import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.GenClass;

import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.AbstractServer;

import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.StatelessAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.ejb.gen.TransactionChain;
import com.caucho.ejb.gen.UserInRoleChain;

import com.caucho.ejb.amber.AmberConfig;

/**
 * Configuration for an ejb bean.
 */
public class EjbBean implements EnvironmentBean, DependencyBean {
  private static Logger log = Log.open(EjbBean.class);
  private static L10N L = new L10N(EjbBean.class);
  
  private static EnvironmentLocal<Map<JClass,SoftReference<JMethod[]>>> _methodCache
    = new EnvironmentLocal<Map<JClass,SoftReference<JMethod[]>>>();

  private EjbConfig _ejbConfig;

  private ClassLoader _loader;

  protected JClassLoader _jClassLoader;

  private String _ejbName;
  private String _jndiName;

  private String _location = "";

  // these classes are loaded with the parent (configuration) loader, not
  // the server loader
  private JClass _ejbClass;
  
  protected JClass _remoteHome;
  protected JClass _remote;
  protected JClass _localHome;
  protected JClass _local;

  protected EjbView _remoteHomeView;
  protected EjbView _remoteView;
  protected EjbView _localHomeView;
  protected EjbView _localView;

  private boolean _isAllowPOJO;

  private boolean _isContainerTransaction = true;
  
  ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();

  ArrayList<PersistentDependency> _configDependList =
    new ArrayList<PersistentDependency>();

  ArrayList<String> _beanDependList = new ArrayList<String>();

  ArrayList<EjbMethodPattern> _methodList = new ArrayList<EjbMethodPattern>();

  private HashMap<String,EjbBaseMethod> _methodMap =
    new HashMap<String,EjbBaseMethod>();

  private BuilderProgramContainer _initProgram;
  private BuilderProgramContainer _serverProgram;

  private long _transactionTimeout;

  /**
   * Creates a new entity bean configuration.
   */
  public EjbBean(EjbConfig ejbConfig)
  {
    _ejbConfig = ejbConfig;
    
    _loader = Thread.currentThread().getContextClassLoader();

    _jClassLoader = JClassLoaderWrapper.create(_loader);
  }

  /**
   * Returns the owning config.
   */
  public EjbConfig getConfig()
  {
    return _ejbConfig;
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
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
   * Sets the jndiName
   */
  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  /**
   * Gets the jndiName
   */
  public String getJndiName()
  {
    return _jndiName;
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
    setEJBClassWrapper(new JClassWrapper(ejbClass, _jClassLoader));
  }

  /**
   * Sets the ejb implementation class.
   */
  public void setEJBClassWrapper(JClass ejbClass)
    throws ConfigException
  {
    if (_ejbClass != null && ! _ejbClass.getName().equals(ejbClass.getName()))
      throw error(L.l("ejb-class '{0}' cannot be redefined.  Old value is '{1}'.",
		      _ejbClass.getName(), ejbClass.getName()));

      
    _ejbClass = ejbClass;

    if (! _ejbClass.isPublic())
      throw error(L.l("`{0}' must be public.  Bean implementations must be public.", ejbClass.getName()));

    if (_ejbClass.isFinal())
      throw error(L.l("`{0}' must not be final.  Bean implementations must not be final.", ejbClass.getName()));

    if (_ejbClass.isInterface())
      throw error(L.l("`{0}' must not be an interface.  Bean implementations must be classes.", ejbClass.getName()));

    Constructor constructor = null;
    /*
    try {
      constructor = ejbClass.getConstructor(new Class[0]);
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (constructor == null)
      throw error(L.l("`{0}' needs a public zero-arg constructor.  Bean implementations need a public zero-argument constructor.", ejbClass.getName()));

    Class []exn = constructor.getExceptionTypes();
    for (int i = 0; i < exn.length; i++) {
      if (! RuntimeException.class.isAssignableFrom(exn[i])) {
        throw error(L.l("{0}: constructor must not throw `{1}'.  Bean constructors must not throw checked exceptions.", ejbClass.getName(), exn[i].getName()));
      }
    }
    */


    JMethod method = ejbClass.getMethod("finalize", new JClass[0]);

    if (method != null && ! method.getDeclaringClass().equals(JClass.OBJECT))
      throw error(L.l("`{0}' may not implement finalize().  Bean implementations may not implement finalize().", ejbClass.getName()));
  }

  /**
   * Gets the ejb implementation class.
   */
  public Class getEJBClass()
  {
    try {
      return Class.forName(_ejbClass.getName(), false, getClassLoader());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the ejb implementation class.
   */
  public JClass getEJBClassWrapper()
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
  public void setHome(Class home)
    throws ConfigException
  {
    setHomeWrapper(new JClassWrapper(home, _jClassLoader));
  }

  /**
   * Sets the remote home interface class.
   */
  public void setHomeWrapper(JClass remoteHome)
    throws ConfigException
  {
    _remoteHome = remoteHome;

    if (! remoteHome.isPublic())
      throw error(L.l("`{0}' must be public.  <home> interfaces must be public.", remoteHome.getName()));
    
    if (! remoteHome.isInterface())
      throw error(L.l("`{0}' must be an interface. <home> interfaces must be interfaces.", remoteHome.getName()));

    if (! remoteHome.isAssignableTo(EJBHome.class) && ! isAllowPOJO())
      throw new ConfigException(L.l("`{0}' must extend EJBHome.  <home> interfaces must extend javax.ejb.EJBHome.", remoteHome.getName()));
    
  }
  /**
   * Gets the ejb implementation class.
   */
  public JClass getRemoteHome()
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
    setRemoteWrapper(new JClassWrapper(remote, _jClassLoader));
  }

  /**
   * Sets the remote interface class.
   */
  public void setRemoteWrapper(JClass remote)
    throws ConfigException
  {
    _remote = remote;

    if (! _remote.isPublic())
      throw error(L.l("`{0}' must be public.  <remote> interfaces must be public.", remote.getName()));
    
    if (! _remote.isInterface())
      throw error(L.l("`{0}' must be an interface. <remote> interfaces must be interfaces.", remote.getName()));

    if (! remote.isAssignableTo(EJBObject.class) && ! isAllowPOJO())
      throw new ConfigException(L.l("`{0}' must extend EJBObject.  <remote> interfaces must extend javax.ejb.EJBObject.", remote.getName()));
    
  }

  /**
   * Gets the remote interface class.
   */
  public JClass getRemote()
  {
    return _remote;
  }

  /**
   * Gets the remote class.
   */
  public Class getRemoteClass()
  {
    if (_remote == null)
      return null;
    
    try {
      return Class.forName(_remote.getName(), false, getClassLoader());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the ejb local home interface
   */
  public void setLocalHome(Class localHome)
    throws ConfigException
  {
    setLocalHomeWrapper(new JClassWrapper(localHome, _jClassLoader));
  }

  /**
   * Sets the local home interface class.
   */
  public void setLocalHomeWrapper(JClass localHome)
    throws ConfigException
  {
    _localHome = localHome;

    if (! localHome.isPublic())
      throw error(L.l("`{0}' must be public.  <local-home> interfaces must be public.", localHome.getName()));
    
    if (! localHome.isInterface())
      throw error(L.l("`{0}' must be an interface. <local-home> interfaces must be interfaces.", localHome.getName()));

    if (! localHome.isAssignableTo(EJBLocalHome.class) && ! isAllowPOJO())
      throw new ConfigException(L.l("`{0}' must extend EJBLocalHome.  <local-home> interfaces must extend javax.ejb.EJBLocalHome.", localHome.getName()));

  }

  /**
   * Gets the local home interface class.
   */
  public JClass getLocalHome()
  {
    return _localHome;
  }

  /**
   * Sets the ejb local interface
   */
  public void setLocal(Class local)
    throws ConfigException
  {
    setLocalWrapper(new JClassWrapper(local, _jClassLoader));
  }

  /**
   * Sets the local interface class.
   */
  public void setLocalWrapper(JClass local)
    throws ConfigException
  {
    _local = local;

    if (! local.isPublic())
      throw error(L.l("`{0}' must be public.  <local> interfaces must be public.", local.getName()));
    
    if (! local.isInterface())
      throw error(L.l("`{0}' must be an interface. <local> interfaces must be interfaces.", local.getName()));

    if (! local.isAssignableTo(EJBLocalObject.class) && ! isAllowPOJO())
      throw new ConfigException(L.l("`{0}' must extend EJBLocalObject.  <local> interfaces must extend javax.ejb.EJBLocalObject.", local.getName()));
  }

  /**
   * Gets the local interface class.
   */
  public JClass getLocal()
  {
    return _local;
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
  public EjbMethodPattern getMethodPattern(JMethod method, String intf)
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

  /**
   * Gets the init program.
   */
  public BuilderProgramContainer getInitProgram()
  {
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
  public void init()
    throws ConfigException
  {
    try {
      introspect();
      
      assembleBeanMethods();
      
      createViews();
    } catch (LineConfigException e) {
      throw e;
    } catch (ConfigException e) {
      throw new LineConfigException(_location + e.getMessage(), e);
    }
  }

  /**
   * Configure initialization.
   */
  public void introspect()
    throws ConfigException
  {
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
    
    if (_remote != null) {
      _remoteView = createObjectView(_remote, "Remote");
      _remoteView.introspect();
    }

    if (_localHome != null) {
      _localHomeView = createHomeView(_localHome, "LocalHome");
      _localHomeView.introspect();
    }
    
    if (_local != null) {
      _localView = createObjectView(_local, "Local");
      _localView.introspect();
    }
  }

  /**
   * Creates a home view.
   */
  protected EjbHomeView createHomeView(JClass homeClass, String prefix)
    throws ConfigException
  {
    return new EjbHomeView(this, homeClass, prefix);
  }

  /**
   * Creates an object view.
   */
  protected EjbObjectView createObjectView(JClass apiClass, String prefix)
    throws ConfigException
  {
    return new EjbObjectView(this, apiClass, prefix);
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
  public AbstractServer deployServer(EjbServerManager ejbManager,
				     JavaClassGenerator javaGen)
    throws ClassNotFoundException, ConfigException
  {
    throw new UnsupportedOperationException();
  }
     
  /**
   * Validates the remote interface.
   */
  protected void validateRemote(JClass objectClass)
    throws ConfigException
  {
    JClass beanClass = getEJBClassWrapper();
    String beanName = beanClass.getName();
  
    String objectName = objectClass.getName();

    if (! objectClass.isPublic())
      throw error(L.l("`{0}' must be public", objectName));
  
    if (! objectClass.isInterface())
      throw error(L.l("`{0}' must be an interface", objectName));
    
    JMethod []methods = getMethods(objectClass);
    for (int i = 0; i < methods.length; i++) {
      JMethod method = methods[i];
      String name = method.getName();
      JClass []param = method.getParameterTypes();
      JClass retType = method.getReturnType();

      if (method.getDeclaringClass().isAssignableFrom(EJBObject.class))
        continue;
      if (method.getDeclaringClass().isAssignableFrom(EJBLocalObject.class))
        continue;

      if (objectClass.isAssignableTo(EJBObject.class))
        validateException(method, java.rmi.RemoteException.class);
      
      if (name.startsWith("ejb")) {
        throw error(L.l("`{0}' forbidden in {1}.  Local or remote interfaces may not define ejbXXX methods.",
                        getFullMethodName(method),
                        objectName));
      }

      JClass returnType = method.getReturnType();
        
      if (objectClass.isAssignableTo(EJBObject.class) &&
          (returnType.isAssignableTo(EJBLocalObject.class) ||
           returnType.isAssignableTo(EJBLocalHome.class)))
        throw error(L.l("`{0}' must not return `{1}' in {2}.  Remote methods must not return local interfaces.",
                        getFullMethodName(method),
                        getShortClassName(returnType),
                        objectClass.getName()));
        
      JMethod implMethod =
        validateRemoteImplMethod(method.getName(), param,
                                 method, objectClass);

      if (! returnType.equals(implMethod.getReturnType())) {
        throw error(L.l("{0}: `{1}' must return {2} to match {3}.{4}.  Business methods must return the same type as the interface.",
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
  private JMethod validateRemoteImplMethod(String methodName,
					   JClass []param,
					   JMethod sourceMethod,
					   JClass sourceClass)
    throws ConfigException
  {
    JMethod method = null;
    JClass beanClass = getEJBClassWrapper();

    method = getMethod(beanClass, methodName, param);

    if (method == null && sourceMethod != null) {
      throw error(L.l("{0}: `{1}' expected to match {2}.{3}",
                      beanClass.getName(),
                      getFullMethodName(methodName, param),
                      getShortClassName(sourceMethod.getDeclaringClass()),
                      getFullMethodName(sourceMethod)));
    }
    else if (method == null) {
      throw error(L.l("{0}: `{1}' expected",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    /*
    else if (Modifier.isAbstract(method.getModifiers()) &&
             getBeanManagedPersistence()) {
      throw error(L.l("{0}: `{1}' must not be abstract",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    */
    else if (! method.isPublic()) {
      throw error(L.l("{0}: `{1}' must be public",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    
    if (method.isStatic()) {
      throw error(L.l("{0}: `{1}' must not be static",
                      beanClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    
    if (method.isFinal()) {
      throw error(L.l("{0}: `{1}' must not be final.",
                      beanClass.getName(),
                      getFullMethodName(methodName, param),
                      beanClass.getName()));
    }

    return method;
  }

  JMethod validateNonFinalMethod(String methodName, JClass []param,
				boolean isOptional)
    throws ConfigException
  {
    if (isOptional && getMethod(_ejbClass, methodName, param) == null)
      return null;
    else
      return validateNonFinalMethod(methodName, param);
  }

  JMethod validateNonFinalMethod(String methodName, JClass []param)
    throws ConfigException
  {
    return validateNonFinalMethod(methodName, param, null, null);
  }
    
  JMethod validateNonFinalMethod(String methodName, JClass []param,
				 JMethod sourceMethod, JClass sourceClass)
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
  JMethod validateNonFinalMethod(String methodName, JClass []param,
				 JMethod sourceMethod, JClass sourceClass,
				 boolean isOptional)
    throws ConfigException
  {
    JMethod method = validateMethod(methodName, param,
				    sourceMethod, sourceClass,
				    isOptional);

    if (method == null && isOptional)
      return null;

    if (method.isFinal())
      throw error(L.l("{0}: `{1}' must not be final",
                      _ejbClass.getName(),
                      getFullMethodName(method)));


    if (method.isStatic())
      throw error(L.l("{0}: `{1}' must not be static",
                      _ejbClass.getName(),
                      getFullMethodName(method)));

    return method;
  }

  JMethod validateMethod(String methodName, JClass []param)
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
  JMethod validateMethod(String methodName, JClass []param,
			 JMethod sourceMethod, JClass sourceClass)
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
  JMethod validateMethod(String methodName, JClass []param,
                        JMethod sourceMethod, JClass sourceClass,
			boolean isOptional)
    throws ConfigException
  {
    JMethod method = null;
    
    method = getMethod(_ejbClass, methodName, param);

    if (method == null && isOptional)
      return null;

    if (method == null && sourceMethod != null) {
      throw error(L.l("{0}: missing `{1}' needed to match {2}.{3}",
                      _ejbClass.getName(),
                      getFullMethodName(methodName, param),
                      getShortClassName(sourceClass),
                      getFullMethodName(sourceMethod)));
    }
    else if (method == null) {
      throw error(L.l("{0}: expected `{1}'",
                      _ejbClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    
    JClass declaringClass = method.getDeclaringClass();

    if (method.isAbstract()) {
      if (method.getDeclaringClass().getName().equals("javax.ejb.EntityBean"))
        throw error(L.l("{0}: `{1}' must not be abstract.  Entity beans must implement the methods in EntityBean.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param)));
      else if (method.getDeclaringClass().getName().equals("javax.ejb.SessionBean"))
        throw error(L.l("{0}: `{1}' must not be abstract.  Session beans must implement the methods in SessionBean.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param)));
      else if (sourceMethod != null)
        throw error(L.l("{0}: `{1}' must not be abstract.  All methods from `{2}' must be implemented in the bean.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param),
                        sourceClass.getName()));
      else
        throw error(L.l("{0}: `{1}' must not be abstract.  Business methods must be implemented.",
                        _ejbClass.getName(),
                        getFullMethodName(methodName, param)));
    } else if (! method.isPublic()) {
      throw error(L.l("{0}: `{1}' must be public.  Business method implementations must be public.",
                      _ejbClass.getName(),
                      getFullMethodName(methodName, param)));
    }
    if (method.isStatic()) {
      throw error(L.l("{0}: `{1}' must not be static.  Business method implementations must not be static.",
                      _ejbClass.getName(),
                      getFullMethodName(method)));
    }

    return method;
  }

  protected String getSkeletonName()
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
    
    if (_remoteView != null)
      _remoteView.assembleView(assembler, fullClassName);

    if (_localHomeView != null)
      _localHomeView.assembleView(assembler, fullClassName);
    
    if (_localView != null)
      _localView.assembleView(assembler, fullClassName);

    for (PersistentDependency depend : _dependList) {
      assembler.addDependency(depend);
    }

    assembler.addDependency(new JClassDependency(_ejbClass));

    if (_remoteHome != null)
      assembler.addDependency(new JClassDependency(_remoteHome));

    if (_remote != null)
      assembler.addDependency(new JClassDependency(_remote));

    if (_localHome != null)
      assembler.addDependency(new JClassDependency(_localHome));

    if (_local != null)
      assembler.addDependency(new JClassDependency(_local));

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
    JMethod []implMethods = getMethods(getEJBClassWrapper());

    for (int i = 0; i < implMethods.length; i++) {
      JMethod method = implMethods[i];

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
  protected EjbBaseMethod introspectEJBMethod(JMethod method)
    throws ConfigException
  {
    return null;
  }
  
  /**
   * Validates an implementation method.
   */
  protected void validateImplMethod(JMethod method)
    throws ConfigException
  {
  }
  
  /**
   * Assembles methods.
   */
  protected void assembleMethods(BeanAssembler assembler,
				 ViewClass view,
				 String contextClassName,
				 JMethod []methods,
				 String prefix)
    throws NoSuchMethodException
  {
    for (int i = 0; i < methods.length; i++) {
      String className = methods[i].getDeclaringClass().getName();
      String methodName = methods[i].getName();
      JClass []args = methods[i].getParameterTypes();

      if (className.startsWith("javax.ejb.")) {
      }
      else if (isOld(methods, methods[i], i)) {
      }
      else if (methodName.equals("equals") && args.length == 1 &&
	       args[0].equals(JClass.OBJECT)) {
      }
      else if (methodName.equals("hashCode") && args.length == 0) {
      }
      else {
	JMethod beanMethod = null;

	JClass ejbClass = getEJBClassWrapper();
	  
	beanMethod = ejbClass.getMethod(methods[i].getName(),
					methods[i].getParameterTypes());

	if (beanMethod == null)
	  throw new NoSuchMethodException("Can't find public method " +
					  methods[i].getFullName());

	CallChain call = new MethodCallChain(beanMethod);
	call = view.createPoolChain(call);
	call = getTransactionChain(call, beanMethod, prefix);
	call = getSecurityChain(call, beanMethod, prefix);

	view.addMethod(new BaseMethod(methods[i], call));
      }
    }
  }

  protected void assembleHomeMethods(BeanAssembler assembler,
				     BaseClass baseClass,
				     String contextClassName,
				     JClass homeClass,
				     String prefix)
    throws NoSuchMethodException
  {
    JMethod []methods = getMethods(homeClass);

    for (int i = 0; i < methods.length; i++) {
      String className = methods[i].getDeclaringClass().getName();
      String methodName = methods[i].getName();

      if (className.startsWith("javax.ejb.")) {
      }
      else if (isOld(methods, methods[i], i)) {
      }
      else if (methodName.startsWith("create")) {
        JMethod beanMethod = null;

        String name = ("ejbCreate" + Character.toUpperCase(methodName.charAt(0))
                       + methodName.substring(1));

        try {
          beanMethod = getEJBClassWrapper().getMethod(name,
					       methods[i].getParameterTypes());
        } catch (Throwable e) {
        }

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
        JMethod beanMethod = null;

        String name = ("ejbHome" + Character.toUpperCase(methodName.charAt(0))
                       + methodName.substring(1));

        try {
          beanMethod = getEJBClassWrapper().getMethod(name,
					       methods[i].getParameterTypes());
        } catch (Exception e) {
          throw new NoSuchMethodException("can't find method " + name);
        }

	CallChain call = new MethodCallChain(beanMethod);
	call = getTransactionChain(call, beanMethod, prefix);
	call = getSecurityChain(call, beanMethod, prefix);

	baseClass.addMethod(new BaseMethod(methods[i], call));
      }
    }
  }

  protected CallChain getTransactionChain(CallChain next,
					  JMethod method,
					  String prefix)
  {
    return TransactionChain.create(next, getTransactionAttribute(method, prefix));
  }

  protected CallChain getSecurityChain(CallChain next,
				       JMethod method,
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
  protected void validatePublicMethod(JMethod method)
    throws ConfigException
  {
    if (! method.isPublic()) {
      throw error(L.l("{0}: '{1}' must be public.",
                      _ejbClass.getName(),
                      getFullMethodName(method)));
    }
    else if (method.isStatic()) {
      throw error(L.l("{0}: `{1}' must not be static.",
                      _ejbClass.getName(),
                      getFullMethodName(method)));
    }
  }

  /**
   * True if we've already handled the method.
   */
  static boolean isOld(JMethod []methods, JMethod method, int index)
  {
    for (int i = 0; i < index; i++) {
      if (isEquiv(methods[i], method))
        return true;
    }

    return false;
  }

  static boolean isEquiv(JMethod oldMethod, JMethod method)
  {
    if (! oldMethod.getName().equals(method.getName()))
      return false;

    JClass []oldParam = oldMethod.getParameterTypes();
    JClass []param = method.getParameterTypes();

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
  public int getTransactionAttribute(JMethod method, String intf)
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
  JMethod getMethod(String methodName, JClass []paramTypes)
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
  public static JMethod getMethod(JClass cl, JMethod sourceMethod)
  {
    return getMethod(cl, sourceMethod.getName(),
                     sourceMethod.getParameterTypes());
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
  public static JMethod getMethod(JClass cl, String name, JClass []param)
  {
    if (cl == null)
      return null;
    
    JMethod []methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      if (isMatch(methods[i], name, param))
	return methods[i];
    }
    
    JMethod method = getMethod(cl.getSuperClass(), name, param);
    if (method != null)
      return method;
    
    for (JClass iface : cl.getInterfaces()) {
      method = getMethod(iface, name, param);
      if (method != null)
	return method;
    }
    
    return null;
  }

  static boolean isMatch(JMethod methodA, JMethod methodB)
  {
    if (methodA == methodB)
      return true;
    else if (methodA == null || methodB == null)
      return false;
    else
      return isMatch(methodA, methodB.getName(), methodB.getParameterTypes());
  }

  static boolean isMatch(JMethod method, String name, JClass []param)
  {
    if (! method.getName().equals(name))
      return false;
    
    JClass []mparam = method.getParameterTypes();
      
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
  static JMethod findMethod(MethodSignature sig, JClass cl, String intf)
  {
    if (cl == null)
      return null;
    
    JMethod []methods = getMethods(cl);

    for (int i = 0; i < methods.length; i++) {
      if (sig.isMatch(methods[i], intf))
	return methods[i];
    }

    return null;
  }
  
  /**
   * Returns all the method in the class.
   */
  static JMethod []getMethods(JClass cl)
  {
    Map<JClass,SoftReference<JMethod[]>> methodMap = _methodCache.get();

    if (methodMap == null) {
      methodMap = new WeakHashMap<JClass,SoftReference<JMethod[]>>();
      _methodCache.set(methodMap);
    }

    SoftReference<JMethod[]> methodArrayRef = methodMap.get(cl);
    JMethod []methodArray = null;

    if (methodArrayRef != null) {
      methodArray = methodArrayRef.get();

      if (methodArray != null)
	return methodArray;
    }

    ArrayList<JMethod> methods = new ArrayList<JMethod>();

    getMethods(methods, cl);

    methodArray = methods.toArray(new JMethod[methods.size()]);

    methodMap.put(cl, new SoftReference<JMethod[]>(methodArray));
    
    return methodArray;
  }

  /**
   * Returns all the method in the class.
   */
  static void getMethods(ArrayList<JMethod> methods, JClass cl)
  {
    if (cl == null)
      return;
    
    JMethod []subMethods = cl.getDeclaredMethods();

    for (int i = 0; i < subMethods.length; i++) {
      if (findMethod(methods, subMethods[i]) == null) {
        methods.add(subMethods[i]);
      }
    }

    getMethods(methods, cl.getSuperClass());

    JClass []interfaces = cl.getInterfaces();
    for (int i = 0; interfaces != null && i < interfaces.length; i++) {
      getMethods(methods, interfaces[i]);
    }
  }

  /**
   * Finds the method in the class.
   *
   * @param cl owning class
   * @param method source method
   *
   * @return the matching method or null if non matches.
   */
  static JMethod findMethod(ArrayList<JMethod> methods, JMethod method)
  {
    loop:
    for (int i = 0; i < methods.size(); i++) {
      JMethod oldMethod = methods.get(i);

      if (! method.getName().equals(oldMethod.getName()))
	continue loop;

      JClass []aParamTypes = oldMethod.getParameterTypes();
      JClass []bParamTypes = method.getParameterTypes();

      if (aParamTypes.length != bParamTypes.length)
	continue loop;
      
      for (int j = 0; j < aParamTypes.length; j++) {
	if (! aParamTypes[j].equals(bParamTypes[j]))
	  continue loop;
      }

      return oldMethod;
    }

    return null;
  }

  /**
   * Returns a full method name with arguments.
   */
  static String getFullMethodName(JMethod method)
  {
    return getFullMethodName(method.getName(), method.getParameterTypes());
  }
  
  /**
   * Returns a full method name with arguments.
   */
  static String getFullMethodName(String methodName, JClass []params)
  {
    String name = methodName + "(";

    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        name += ", ";

      name += getShortClassName(params[i]);
    }

    return name + ")";
  }

  /**
   * Returns a printable version of a class.
   */
  static String getClassName(JClass cl)
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
  static String getShortClassName(JClass cl)
  {
    if (cl.isArray())
      return getShortClassName(cl.getComponentType()) + "[]";
    else {
      int p = cl.getName().lastIndexOf('.');

      return cl.getName().substring(p + 1);
    }
  }

  /**
   * Tests is a method is declared in a class.
   */
  boolean classHasMethod(JMethod method, JClass cl)
  {
    try {
      JMethod match = cl.getMethod(method.getName(),
                                  method.getParameterTypes());
      return match != null;
    } catch (Exception e) {
      return false;
    }
  }
  
  void validateException(JMethod method, Class e)
    throws ConfigException
  {
    validateException(method, new JClassWrapper(e, _jClassLoader));
  }
  
  void validateException(JMethod method, JClass e)
    throws ConfigException
  {
    validateExceptions(method, new JClass[] { e });
  }
  
  /**
   * Check that the method throws the expected exceptions.
   *
   * @param method the method to test
   * @param exn the expected exceptions
   */
  void validateExceptions(JMethod method, JClass []exn)
    throws ConfigException
  {
    JClass []methodExceptions = method.getExceptionTypes();

    loop:
    for (int i = 0; i < exn.length; i++) {
      if (exn[i].isAssignableTo(RuntimeException.class))
        continue;

      for (int j = 0; j < methodExceptions.length; j++) {
        if (methodExceptions[j].isAssignableFrom(exn[i]))
          continue loop;
      }

      throw new ConfigException(L.l("{2}: `{0}' must throw {1}.",
                                    getFullMethodName(method),
                                    exn[i].getName(),
                                    method.getDeclaringClass().getName()));
    }
  }

  void validateExceptions(JMethod caller, JMethod callee)
    throws ConfigException
  {
    JClass []exn = callee.getExceptionTypes();
    JClass missing = findMissingException(caller, exn);
    
    if (missing != null) {
      throw error(L.l("{0}: `{1}' must throw {2}.",
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
  JClass findMissingException(JMethod method, JClass []exn)
    throws ConfigException
  {
    JClass []methodExceptions = method.getExceptionTypes();

    for (int i = 0; i < exn.length; i++) {
      if (! hasException(method, exn[i]) &&
          ! exn[i].isAssignableTo(RuntimeException.class))
        return exn[i];
    }

    return null;
  }
  
  boolean hasException(JMethod method, JClass exn)
    throws ConfigException
  {
    JClass []methodExceptions = method.getExceptionTypes();

    for (int j = 0; j < methodExceptions.length; j++) {
      if (methodExceptions[j].isAssignableFrom(exn))
        return true;
    }

    return false;
  }
  
  boolean hasException(JMethod method, Class exn)
    throws ConfigException
  {
    JClass []methodExceptions = method.getExceptionTypes();

    for (int j = 0; j < methodExceptions.length; j++) {
      if (methodExceptions[j].isAssignableFrom(exn))
        return true;
    }

    return false;
  }

  /**
   * Returns an error.
   */
  public ConfigException error(String msg)
  {
    return new ConfigException(msg);
  }
}
