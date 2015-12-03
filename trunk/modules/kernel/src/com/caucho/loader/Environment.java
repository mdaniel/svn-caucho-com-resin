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

package com.caucho.loader;

import java.lang.management.ManagementFactory;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.log.EnvironmentStream;
import com.caucho.log.LogManagerImpl;
import com.caucho.naming.Jndi;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Static utility classes.
 */
public class Environment {
  private static boolean _isStaticInit;

  private static Logger _log;

  private static ArrayList<EnvironmentListener> _globalEnvironmentListeners
    = new ArrayList<EnvironmentListener>();

  private static ArrayList<ClassLoaderListener> _globalLoaderListeners
    = new ArrayList<ClassLoaderListener>();

  private static boolean _isInitComplete;
  
  private static ClassLoader _systemClassLoader;

  /**
   * Returns the local environment.
   */
  public static EnvironmentClassLoader getEnvironmentClassLoader()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader)
        return (EnvironmentClassLoader) loader;
    }
    
    loader = getSystemClassLoader();
    
    if (loader instanceof EnvironmentClassLoader)
      return (EnvironmentClassLoader) loader;

    return null;
  }

  /**
   * Returns the local environment.
   */
  public static EnvironmentClassLoader
    getEnvironmentClassLoader(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader)
        return (EnvironmentClassLoader) loader;
    }

    return null;
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment start/stop
   */
  public static void addEnvironmentListener(EnvironmentListener listener)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    addEnvironmentListener(listener, loader);
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addEnvironmentListener(EnvironmentListener listener,
                                            ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).addListener(listener);
        return;
      }
    }
    
    /*
    if (_envSystemClassLoader != null) {
      _envSystemClassLoader.addNotificationListener(listener);
      return;
    }
    */

    _globalEnvironmentListeners.add(listener);
  }

  /**
   * Remove listener.
   *
   * @param listener object to listen for environment start/stop
   */
  public static void removeEnvironmentListener(EnvironmentListener listener)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    removeEnvironmentListener(listener, loader);
  }

  /**
   * Remove listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void removeEnvironmentListener(EnvironmentListener listener,
                                            ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).removeListener(listener);
        return;
      }
    }

    _globalEnvironmentListeners.remove(listener);
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addChildEnvironmentListener(EnvironmentListener listener)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    addChildEnvironmentListener(listener, loader);
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addChildEnvironmentListener(EnvironmentListener listener,
                                                 ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).addChildListener(listener);
        return;
      }
    }
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addChildLoaderListener(AddLoaderListener listener)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    addChildLoaderListener(listener, loader);
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addChildLoaderListener(AddLoaderListener listener,
                                            ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).addLoaderListener(listener);
        return;
      }
    }
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   */
  public static void addClassLoaderListener(ClassLoaderListener listener)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    addClassLoaderListener(listener, loader);
  }

  /**
   * Add listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addClassLoaderListener(ClassLoaderListener listener,
                                            ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).addListener(listener);
        return;
      }
    }
    
    /*
    if (_envSystemClassLoader != null) {
      _envSystemClassLoader.addNotificationListener(listener);
      return;
    }
    */

    _globalLoaderListeners.add(listener);
  }

  /**
   * Add start listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addStartListener(Object obj)
  {
    addEnvironmentListener(new StartListener(obj));
  }

  /**
   * Add close listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addCloseListener(Object obj)
  {
    addClassLoaderListener(new CloseListener(obj));
  }

  /**
   * Add close listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addCloseListener(Object obj, ClassLoader loader)
  {
    addClassLoaderListener(new CloseListener(obj), loader);
  }

  /**
   * Add close listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addWeakCloseListener(Object obj)
  {
    addClassLoaderListener(new WeakCloseListener(obj));
  }

  /**
   * Add close listener.
   *
   * @param listener object to listen for environment create/destroy
   * @param loader the context class loader
   */
  public static void addWeakCloseListener(Object obj, ClassLoader loader)
  {
    addClassLoaderListener(new WeakCloseListener(obj), loader);
  }

  /**
   * Starts the current environment.
   */
  public static void init()
  {
    initializeEnvironment();

    init(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Starts the current environment.
   */
  public static void init(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).init();
        return;
      }
    }

    for (int i = 0; i < _globalLoaderListeners.size(); i++) {
      ClassLoaderListener listener = _globalLoaderListeners.get(i);

      listener.classLoaderInit(null);
    }
  }

  /**
   * Starts the current environment.
   */
  public static void start()
    throws StartLifecycleException
  {
    start(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Starts the current environment.
   */
  public static void start(ClassLoader loader)
    throws StartLifecycleException
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).start();
        return;
      }
    }

    init(loader);

    for (int i = 0; i < _globalEnvironmentListeners.size(); i++) {
      EnvironmentListener listener = _globalEnvironmentListeners.get(i);

      listener.environmentStart(null);
    }
  }

  /**
   * Starts the current environment.
   */
  public static void stop()
  {
    stop(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Starts the current environment.
   */
  public static void stop(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).stop();
        return;
      }
    }

    ArrayList<EnvironmentListener> listeners;
    listeners = new ArrayList<EnvironmentListener>();
    listeners.addAll(_globalEnvironmentListeners);
    _globalEnvironmentListeners.clear();

    for (int i = 0; i < listeners.size(); i++) {
      EnvironmentListener listener = listeners.get(i);

      listener.environmentStop(null);
    }
  }

  /**
   * Adds a dependency to the current environment.
   *
   * @param depend the dependency to add
   */
  public static void addDependency(Dependency depend)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    addDependency(depend, loader);
  }

  /**
   * Adds a dependency to the current environment.
   *
   * @param depend the dependency to add
   * @param loader the context loader
   */
  public static void addDependency(Dependency depend,
                                   ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        ((EnvironmentClassLoader) loader).addDependency(depend);
        return;
      }
    }
  }

  /**
   * Returns the topmost dynamic class loader.
   */
  public static DynamicClassLoader getDynamicClassLoader()
  {
    Thread thread = Thread.currentThread();

    return getDynamicClassLoader(thread.getContextClassLoader());
  }

  /**
   * Returns the topmost dynamic class loader.
   *
   * @param loader the context loader
   */
  public static DynamicClassLoader getDynamicClassLoader(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof DynamicClassLoader) {
        return (DynamicClassLoader) loader;
      }
    }

    return null;
  }

  /**
   * Adds a dependency to the current environment.
   *
   * @param depend the dependency to add
   */
  public static void addDependency(Path path)
  {
    addDependency(new Depend(path));
  }

  /**
   * Adds a dependency to the current environment.
   *
   * @param path the dependency to add
   * @param loader the context loader
   */
  public static void addDependency(Path path, ClassLoader loader)
  {
    addDependency(new Depend(path), loader);
  }

  /**
   * Gets a local variable for the current environment.
   *
   * @param name the attribute name
   *
   * @return the attribute value
   */
  public static Object getAttribute(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return getAttribute(name, loader);
  }

  /**
   * Returns the current dependency check interval.
   */
  public static long getDependencyCheckInterval()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof DynamicClassLoader)
        return ((DynamicClassLoader) loader).getDependencyCheckInterval();
    }

    return DynamicClassLoader.getGlobalDependencyCheckInterval();
  }

  /**
   * Returns the current dependency check interval.
   */
  public static long getDependencyCheckInterval(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof DynamicClassLoader)
        return ((DynamicClassLoader) loader).getDependencyCheckInterval();
    }

    return DynamicClassLoader.getGlobalDependencyCheckInterval();
  }

  /**
   * Gets a local variable for the current environment.
   *
   * @param name the attribute name
   * @param loader the context loader
   *
   * @return the attribute value
   */
  public static Object getAttribute(String name, ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        Object value = ((EnvironmentClassLoader) loader).getAttribute(name);

        if (value != null)
          return value;
      }
    }
    
    /*
    if (_envSystemClassLoader != null)
      return _envSystemClassLoader.getAttribute(name);
      */

    return null;
  }

  /**
   * Gets a local variable for the current environment.
   *
   * @param name the attribute name
   *
   * @return the attribute value
   */
  public static Object getLevelAttribute(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return getLevelAttribute(name, loader);
  }

  /**
   * Gets a local variable for the current environment.
   *
   * @param name the attribute name
   * @param loader the context loader
   *
   * @return the attribute value
   */
  public static Object getLevelAttribute(String name, ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        return ((EnvironmentClassLoader) loader).getAttribute(name);
      }
    }
    
    /*
    if (_envSystemClassLoader != null)
      return _envSystemClassLoader.getAttribute(name);
      */

    return null;
  }

  /**
   * Sets a local variable for the current environment.
   *
   * @param name the attribute name
   * @param value the new attribute value
   *
   * @return the old attribute value
   */
  public static Object setAttribute(String name, Object value)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return setAttribute(name, value, loader);
  }

  /**
   * Sets a local variable for the current environment.
   *
   * @param name the attribute name
   * @param value the new attribute value
   * @param loader the context loader
   *
   * @return the old attribute value
   */
  public static Object setAttribute(String name,
                                    Object value,
                                    ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        Object oldValue = envLoader.getAttribute(name);

        envLoader.setAttribute(name, value);

        return oldValue;
      }
    }
    
    /*
    if (_envSystemClassLoader != null) {
      Object oldValue = _envSystemClassLoader.getAttribute(name);

      _envSystemClassLoader.setAttribute(name, value);

      return oldValue;
    }
    */

    return null;
  }

  /**
   * Adds a permission to the current environment.
   *
   * @param perm the permission to add.
   *
   * @return the old attribute value
   */
  public static void addPermission(Permission perm)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    addPermission(perm, loader);
  }

  /**
   * Adds a permission to the current environment.
   *
   * @param perm the permission to add.
   *
   * @return the old attribute value
   */
  public static void addPermission(Permission perm, ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        envLoader.addPermission(perm);
      }
    }
  }

  /**
   * Gets the class loader owner.
   */
  public static Object getOwner()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return getOwner(loader);
  }

  /**
   * Gets the class loader owner.
   */
  public static Object getOwner(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        Object owner = envLoader.getOwner();

        if (owner != null)
          return owner;
      }
    }

    return null;
  }

  /**
   * Sets a configuration exception.
   */
  public static void setConfigException(Throwable e)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        envLoader.setConfigException(e);

        return;
      }
    }
  }

  /**
   * Returns any configuration exception.
   */
  public static Throwable getConfigException()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        if (envLoader.getConfigException() != null)
          return envLoader.getConfigException();
      }
    }

    return null;
  }

  /**
   * Returns the environment name.
   */
  public static String getEnvironmentName()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return getEnvironmentName(loader);
  }

  /**
   * Returns the environment name.
   */
  public static String getEnvironmentName(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        String name = ((EnvironmentClassLoader) loader).getId();

        if (name != null)
          return name;
        else
          return "";
      }
    }
    
    /*
    if (_envSystemClassLoader != null) {
      String name = _envSystemClassLoader.getId();
      
      if (name != null)
        return name;
      else
        return "";
    }
    */

    return Thread.currentThread().getContextClassLoader().toString();
  }

  /**
   * Apply the action to visible classloaders
   */
  public static void applyVisibleModules(EnvironmentApply apply)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;
        envLoader.applyVisibleModules(apply);

        return;
      }
    }
    
    /*
    if (_envSystemClassLoader != null) {
      _envSystemClassLoader.applyVisibleModules(apply);
    }
    */
  }

  /**
   * Returns the classpath for the environment level.
   */
  public static String getLocalClassPath()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return getLocalClassPath(loader);
  }

  /**
   * Returns the classpath for the environment level.
   */
  public static String getLocalClassPath(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        return ((EnvironmentClassLoader) loader).getLocalClassPath();
      }
    }
    
    return CauchoSystem.getClassPath();
  }

  /**
   * destroys the current environment.
   */
  public static void closeGlobal()
  {
    ArrayList<ClassLoaderListener> listeners;
    listeners = new ArrayList<ClassLoaderListener>();
    listeners.addAll(_globalLoaderListeners);
    _globalLoaderListeners.clear();

    for (int i = 0; i < listeners.size(); i++) {
      ClassLoaderListener listener = listeners.get(i);

      listener.classLoaderDestroy(null);
    }
  }

  /**
   * @return
   */
  public static boolean isLoggingInitialized()
  {
    String logManager = System.getProperty("java.util.logging.manager");
    
    return _isInitComplete && LogManagerImpl.class.getName().equals(logManager);
  }

  /**
   * Initializes the environment
   */
  public static synchronized void initializeEnvironment()
  {
    if (_isStaticInit)
      return;

    _isStaticInit = true;

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(systemLoader);
      
      if ("1.6.".compareTo(System.getProperty("java.runtime.version")) > 0)
        throw new ConfigException("Resin requires JDK 1.6 or later");

      // #2281
      // PolicyImpl.init();

      EnvironmentStream.setStdout(System.out);
      EnvironmentStream.setStderr(System.err);

      try {
        Vfs.initJNI();
      } catch (Throwable e) {
      }

      Properties props = System.getProperties();

      /*
      if (props.get("java.util.logging.manager") == null) {
        props.put("java.util.logging.manager",
                  "com.caucho.log.LogManagerImpl");
      }
      */

      ClassLoader envClassLoader
        = EnvironmentClassLoader.class.getClassLoader();

      boolean isGlobalLoadable = false;
      try {
        Class<?> cl = Class.forName("com.caucho.naming.InitialContextFactoryImpl",
                                    false,
                                    systemLoader);

        isGlobalLoadable = (cl != null);
      } catch (Exception e) {
        log().log(Level.FINER, e.toString(), e);
      }

      // #3486
      String namingPkgs = (String) props.get("java.naming.factory.url.pkgs");
      if (namingPkgs == null)
        namingPkgs = "com.caucho.naming";
      else
        namingPkgs = namingPkgs + ":" + "com.caucho.naming";
      props.put("java.naming.factory.url.pkgs", namingPkgs);

      if (isGlobalLoadable) {
        // These properties require Resin to be at the system loader

        if (props.get("java.naming.factory.initial") == null) {
          props.put("java.naming.factory.initial",
                    "com.caucho.naming.InitialContextFactoryImpl");
        }

        // props.put("java.naming.factory.url.pkgs", "com.caucho.naming");

        EnvironmentProperties.enableEnvironmentSystemProperties(true);

        String oldBuilder = props.getProperty("javax.management.builder.initial");
        if (oldBuilder == null) {
          oldBuilder = "com.caucho.jmx.MBeanServerBuilderImpl";
          props.put("javax.management.builder.initial", oldBuilder);
        }

        /*
          props.put("javax.management.builder.initial",
          "com.caucho.jmx.EnvironmentMBeanServerBuilder");
        */

        // server/21ab
        /*
        if (MBeanServerFactory.findMBeanServer(null).size() == 0)
          MBeanServerFactory.createMBeanServer("Resin");
        */

        ManagementFactory.getPlatformMBeanServer();
      }

      Jndi.bindDeep("java:comp/env/jmx/MBeanServer",
                    Jmx.getGlobalMBeanServer());
      Jndi.bindDeep("java:comp/env/jmx/GlobalMBeanServer",
                    Jmx.getGlobalMBeanServer());

      try {
        Class<?> cl = Class.forName("com.caucho.server.resin.EnvInit",
                                    false,
                                    systemLoader);

        cl.newInstance();
      } catch (Exception e) {
        log().log(Level.FINE, e.toString(), e);
      }

      /*
      try {
        Jndi.rebindDeep("java:comp/ORB",
                        new com.caucho.iiop.orb.ORBImpl());
      } catch (Exception e) {
        e.printStackTrace();
      }
      */
    } catch (NamingException e) {
      log().log(Level.FINE, e.toString(), e);
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      thread.setContextClassLoader(oldLoader);
      
      _isInitComplete = true;
    }
  }

  private static final Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(Environment.class.getName());

    return _log;
  }
  
  private static ClassLoader getSystemClassLoader()
  {
    try {
      if (_systemClassLoader == null) {
        if (_systemClassLoader == null) {
          _systemClassLoader = ClassLoader.getSystemClassLoader();
          
          if (_systemClassLoader != null)
            _systemClassLoader = RootDynamicClassLoader.getSystemRootClassLoader();
        }
      }
    } catch (Exception e) {
      // can't log this early in startup
      _systemClassLoader = Environment.class.getClassLoader();
    }
    
    return _systemClassLoader;
  }
}
