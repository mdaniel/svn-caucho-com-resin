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

package com.caucho.loader;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.lang.ref.WeakReference;

import java.lang.reflect.Method;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import com.caucho.vfs.EnvironmentStream;
import com.caucho.vfs.SchemeMap;

import com.caucho.naming.Jndi;

import com.caucho.transaction.TransactionManagerImpl;

import com.caucho.jca.UserTransactionProxy;

import com.caucho.util.ThreadPool;

import com.caucho.jmx.Jmx;

import com.caucho.security.PolicyImpl;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
public class EnvironmentClassLoader extends DynamicClassLoader {
  private static boolean _isStaticInit;

  // The owning bean
  private EnvironmentBean _owner;
  
  // Class loader specific attributes
  private Hashtable<String,Object> _attributes =
    new Hashtable<String,Object>(8);
  
  // Array of listeners
  // XXX: this used to be a weak reference list, but that caused problems
  // server/306i  - can't be weak reference, instead create WeakStopListener
  private ArrayList<EnvironmentListener> _listeners;
  private WeakStopListener _stopListener;

  // Returns true once the environment has started
  private volatile boolean _isStarted;
  
  // Returns true if the environment is active
  private volatile boolean _isActive;
  
  // Returns true if the environment is stopped
  private volatile boolean _isStopped;
  
  /**
   * Creates a new environment class loader.
   */
  public EnvironmentClassLoader()
  {
    super(Thread.currentThread().getContextClassLoader());

    // initializeEnvironment();

    initListeners();
  }

  /**
   * Creates a new environment class loader.
   */
  public EnvironmentClassLoader(ClassLoader parent)
  {
    super(parent);
    
    // initializeEnvironment();
    
    initListeners();
  }

  /**
   * Returns the environment's owner.
   */
  public EnvironmentBean getOwner()
  {
    return _owner;
  }

  /**
   * Sets the environment's owner.
   */
  public void setOwner(EnvironmentBean owner)
  {
    _owner = owner;
  }

  /**
   * Returns true if the environment is active
   */
  public boolean isActive()
  {
    return _isActive;
  }

  /**
   * Initialize the environment.
   */
  public void init()
  {
    initializeEnvironment();

    super.init();
  }

  /**
   * Returns the named attributes
   */
  public Object getAttribute(String name)
  {
    if (_attributes != null)
      return _attributes.get(name);
    else
      return null;
  }

  /**
   * Sets the named attributes
   */
  public Object setAttribute(String name, Object obj)
  {
    if (obj == null) {
      if (_attributes == null)
        return null;
      else
        return _attributes.remove(name);
    }
    
    if (_attributes == null)
      _attributes = new Hashtable<String,Object>(8);
    
    return _attributes.put(name, obj);
  }

  /**
   * Removes the named attributes
   */
  public Object removeAttribute(String name)
  {
    if (_attributes == null)
      return null;
    else
      return _attributes.remove(name);
  }

  /**
   * Adds a listener to detect environment lifecycle changes.
   */
  public void addListener(EnvironmentListener listener)
  {
    synchronized (this) {
      if (_listeners == null) {
	_listeners = new ArrayList<EnvironmentListener>();

	initListeners();
      }
    }
    
    synchronized (_listeners) {
      for (int i = _listeners.size() - 1; i >= 0; i--) {
	EnvironmentListener oldListener = _listeners.get(i);

	if (listener == oldListener) {
	  return;
	}
	else if (oldListener == null)
	  _listeners.remove(i);
      }

      _listeners.add(listener);
    }

    if (_isStarted)
      listener.environmentStart(this);
  }

  /**
   * Adds self as a listener.
   */
  private void initListeners()
  {
    ClassLoader parent = getParent();
    
    for (; parent != null; parent = parent.getParent()) {
      if (parent instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader loader = (EnvironmentClassLoader) parent;

	_stopListener = new WeakStopListener(this);
        loader.addListener(_stopListener);

	return;
      }
    }
  }

  /**
   * Adds a listener to detect environment lifecycle changes.
   */
  public void removeListener(EnvironmentListener listener)
  {
    if (_listeners == null)
      return;
    
    synchronized (_listeners) {
      for (int i = _listeners.size() - 1; i >= 0; i--) {
	EnvironmentListener oldListener = _listeners.get(i);

	if (listener == oldListener) {
	  _listeners.remove(i);
	  return;
	}
	else if (oldListener == null)
	  _listeners.remove(i);
      }
    }
  }

  /**
   * Returns the listeners.
   */
  protected ArrayList<EnvironmentListener> getEnvironmentListeners()
  {
    ArrayList<EnvironmentListener> listeners;
    listeners = new ArrayList<EnvironmentListener>();

    if (_listeners == null)
      return listeners;
    
    synchronized (_listeners) {
      for (int i = 0; i < _listeners.size(); i++) {
	EnvironmentListener listener = _listeners.get(i);

	if (listener != null)
	  listeners.add(listener);
	else {
	  _listeners.remove(i);
	  i--;
	}
      }
    }

    return listeners;
  }

  /**
   * Marks the environment of the class loader as started.  The
   * class loader itself doesn't use this, but a callback might.
   */
  public void start()
  {
    synchronized (this) {
      if (_isStarted)
        return;
      
      _isStarted = true;
    }

    try {
      ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

      int size = listeners.size();
      for (int i = 0; listeners != null && i < size; i++) {
	EnvironmentListener listener = listeners.get(i);

	listener.environmentStart(this);
      }
    } catch (Throwable e) {
      log().log(Level.WARNING, e.toString(), e);
    }
    
    _isActive = true;
  }

  /**
   * Stops the environment, closing down any resources.
   *
   * The resources are closed in the reverse order that they're started
   */
  public void stop()
  {
    synchronized (this) {
      if (_isStopped)
        return;

      _isStopped = true;
      _isActive = false;
    }
    
    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    thread.setContextClassLoader(this);

    try {
      // closing down in reverse
      if (listeners != null) {
	for (int i = listeners.size() - 1; i >= 0; i--) {
	  EnvironmentListener listener = listeners.get(i);

	  try {
	    listener.environmentStop(this);
	  } catch (Throwable e) {
	    e.printStackTrace();
	    log().log(Level.WARNING, e.toString(), e);
	  }
	}
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Copies the loader.
   */
  protected void replace(EnvironmentClassLoader source)
  {
    super.replace(source);

    // XXX: might need separate class
    _owner = source._owner;
    _attributes = source._attributes;
    if (source._listeners != null) {
      if (_listeners == null)
	_listeners = new ArrayList<EnvironmentListener>();
      _listeners.addAll(source._listeners);
      source._listeners.clear();
    }

    _isStarted = source._isStarted;
    _isActive = source._isActive;
    _isStopped = source._isStopped;
  }
  
  /**
   * Destroys the class loader.
   */
  public void destroy()
  {
    try {
      // make sure it's stopped first
      stop();
    
      super.destroy();

      ClassLoader parent = getParent();
      for (; parent != null; parent = parent.getParent()) {
	if (parent instanceof EnvironmentClassLoader) {
	  EnvironmentClassLoader loader = (EnvironmentClassLoader) parent;

	  loader.removeListener(_stopListener);
	}
      }
    } finally {
      _owner = null;
      _attributes = null;
      _listeners = null;

      ThreadPool.reset(); // drain the thread pool for GC
    }
  }

  public String toString()
  {
    if (getId() != null)
      return "EnvironmentClassLoader$" + System.identityHashCode(this) + "[" + getId() + "]";
    else
      return "EnvironmentClassLoader$" + System.identityHashCode(this) + getLoaders();
  }
  
  /**
   * Initializes the environment
   */
  public static synchronized void initializeEnvironment()
  {
    if (_isStaticInit)
      return;

    _isStaticInit = true;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

      PolicyImpl.init();
      
      EnvironmentStream.setStdout(System.out);
      EnvironmentStream.setStderr(System.err);

      try {
	SchemeMap.initJNI();
      } catch (Throwable e) {
      }

      if (System.getProperty("org.xml.sax.driver") == null)
        System.setProperty("org.xml.sax.driver", "com.caucho.xml.Xml");

      Properties props = System.getProperties();

      if (props.get("java.util.logging.manager") == null) {
        props.put("java.util.logging.manager",
                  "com.caucho.log.LogManagerImpl");
      }

      if (props.get("java.naming.factory.initial") == null) {
        props.put("java.naming.factory.initial",
                  "com.caucho.naming.InitialContextFactoryImpl");
      }

      props.put("java.naming.factory.url.pkgs", "com.caucho.naming");

      EnvironmentProperties contextProps = new EnvironmentProperties(props);
      System.setProperties(contextProps);

      TransactionManagerImpl tm = TransactionManagerImpl.getInstance();
      // TransactionManagerImpl.setLocal(tm);
      Jndi.bindDeep("java:comp/TransactionManager", tm);

      UserTransactionProxy ut = UserTransactionProxy.getInstance();
      Jndi.bindDeep("java:comp/UserTransaction", ut);

      String oldBuilder = props.getProperty("javax.management.builder.initial");
      if (oldBuilder == null)
	oldBuilder = "com.caucho.jmx.MBeanServerBuilderImpl";

      /*
      props.put("javax.management.builder.initial",
		"com.caucho.jmx.EnvironmentMBeanServerBuilder");
      */

      props.put("javax.management.builder.initial", oldBuilder);

      if (MBeanServerFactory.findMBeanServer(null).size() == 0)
	MBeanServerFactory.createMBeanServer("Resin");

      try {
	Class cl = Class.forName("java.lang.management.ManagementFactory");
	Method method = cl.getMethod("getPlatformMBeanServer", new Class[0]);
	method.invoke(null, new Object[0]);
      } catch (Throwable e) {
      }
      
      Jndi.bindDeep("java:comp/env/jmx/MBeanServer",
		    Jmx.getContextMBeanServer());
      Jndi.bindDeep("java:comp/env/jmx/GlobalMBeanServer",
		    Jmx.getGlobalMBeanServer());
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
