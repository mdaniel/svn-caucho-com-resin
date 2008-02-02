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

package com.caucho.loader;

import com.caucho.jca.UserTransactionProxy;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.log.EnvironmentStream;
import com.caucho.loader.enhancer.ScanManager;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.management.j2ee.JTAResource;
import com.caucho.management.server.ClassLoaderMXBean;
import com.caucho.naming.Jndi;
import com.caucho.security.PolicyImpl;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.util.ResinThreadPoolExecutor;
import com.caucho.vfs.Vfs;

import javax.management.MBeanServerFactory;
import javax.naming.*;
import java.net.URL;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.*;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
public class EnvironmentClassLoader extends DynamicClassLoader
{
  private static Logger _log;

  private static boolean _isStaticInit;

  // listeners invoked at the start of any child environment
  private static EnvironmentLocal<ArrayList<EnvironmentListener>> _childListeners
    = new EnvironmentLocal<ArrayList<EnvironmentListener>>();

  // listeners invoked when a Loader is added
  private static EnvironmentLocal<ArrayList<AddLoaderListener>> _addLoaderListeners
    = new EnvironmentLocal<ArrayList<AddLoaderListener>>();

  // The owning bean
  private EnvironmentBean _owner;

  // Class loader specific attributes
  private Hashtable<String,Object> _attributes
    = new Hashtable<String,Object>(8);

  private ArrayList<ScanListener> _scanListeners;
  private ArrayList<URL> _pendingScanUrls = new ArrayList<URL>();

  // Array of listeners
  // XXX: this used to be a weak reference list, but that caused problems
  // server/306i  - can't be weak reference, instead create WeakStopListener
  private ArrayList<EnvironmentListener> _listeners;
  private WeakStopListener _stopListener;

  // The state of the environment
  private volatile Lifecycle _lifecycle = new Lifecycle();

  private Throwable _configException;

  /**
   * Creates a new environment class loader.
   */
  public EnvironmentClassLoader()
  {
    this(Thread.currentThread().getContextClassLoader(), null);
  }

  /**
   * Creates a new environment class loader.
   */
  public EnvironmentClassLoader(String id)
  {
    this(Thread.currentThread().getContextClassLoader(), id);
  }

  /**
   * Creates a new environment class loader.
   */
  public EnvironmentClassLoader(ClassLoader parent)
  {
    this(parent, null);
  }

  /**
   * Creates a new environment class loader.
   */
  public EnvironmentClassLoader(ClassLoader parent, String id)
  {
    super(parent);

    if (id != null)
      setId(id);

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
   * Sets the config exception.
   */
  public void setConfigException(Throwable e)
  {
    if (_configException == null)
      _configException = e;
  }

  /**
   * Gets the config exception.
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns true if the environment is active
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
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

    if (_lifecycle.isStarting()) {
      listener.environmentConfig(this);
    }

    if (_lifecycle.isActive()) {
      listener.environmentStart(this);
    }
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

        if (_stopListener == null)
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
   * Adds a child listener.
   */
  void addChildListener(EnvironmentListener listener)
  {
    synchronized (_childListeners) {
      ArrayList<EnvironmentListener> listeners
        = _childListeners.getLevel(this);

      if (listeners == null) {
        listeners = new ArrayList<EnvironmentListener>();

        _childListeners.set(listeners, this);
      }

      listeners.add(listener);
    }

    if (_lifecycle.isActive()) {
      listener.environmentStart(this);
    }
  }

  /**
   * Removes a child listener.
   */
  void removeChildListener(EnvironmentListener listener)
  {
    synchronized (_childListeners) {
      ArrayList<EnvironmentListener> listeners
        = _childListeners.getLevel(this);

      if (listeners != null)
        listeners.remove(listener);
    }
  }

  /**
   * Returns the listeners.
   */
  protected ArrayList<EnvironmentListener> getEnvironmentListeners()
  {
    ArrayList<EnvironmentListener> listeners;
    listeners = new ArrayList<EnvironmentListener>();

    // add the descendent listeners
    synchronized (_childListeners) {
      ClassLoader loader;

      for (loader = this; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          ArrayList<EnvironmentListener> childListeners;
          childListeners = _childListeners.getLevel(loader);

          if (childListeners != null)
            listeners.addAll(childListeners);
        }
      }
    }

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
   * Adds a child listener.
   */
  public void addLoaderListener(AddLoaderListener listener)
  {
    synchronized (_addLoaderListeners) {
      ArrayList<AddLoaderListener> listeners
        = _addLoaderListeners.getLevel(this);

      if (listeners == null) {
        listeners = new ArrayList<AddLoaderListener>();

        _addLoaderListeners.set(listeners, this);
      }

      listeners.add(listener);
    }

    listener.addLoader(this);
  }

  /**
   * Returns the listeners.
   */
  protected ArrayList<AddLoaderListener> getLoaderListeners()
  {
    ArrayList<AddLoaderListener> listeners;
    listeners = new ArrayList<AddLoaderListener>();

    // add the descendent listeners
    synchronized (_addLoaderListeners) {
      ClassLoader loader;

      for (loader = this; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          ArrayList<AddLoaderListener> childListeners;
          childListeners = _addLoaderListeners.getLevel(loader);

          if (childListeners != null)
            listeners.addAll(childListeners);
        }
      }
    }

    return listeners;
  }

  /**
   * Adds a listener to detect class loader changes.
   */
  @Override
  protected void sendAddLoaderEventImpl()
  {
    ArrayList<AddLoaderListener> listeners = getLoaderListeners();
    
    for (int i = 0;
	 listeners != null && i < listeners.size();
	 i++) {
      listeners.get(i).addLoader(this);
    }
  }

  /**
   * Adds the URL to the URLClassLoader.
   */
  @Override
  public void addURL(URL url)
  {
    super.addURL(url);

    _pendingScanUrls.add(url);
  }

  /**
   * Adds a scan listener.
   */
  public void addScanListener(ScanListener listener)
  {
    if (_scanListeners == null)
      _scanListeners = new ArrayList<ScanListener>();

    _scanListeners.add(listener);
    
    ArrayList<URL> urlList = new ArrayList<URL>();
    for (URL url : getURLs()) {
      if (! _pendingScanUrls.contains(url))
	urlList.add(url);
    }

    if (urlList.size() > 0) {
      try {
	make();
      } catch (Exception e) {
	log().log(Level.WARNING, e.toString(), e);
	
	if (_configException == null)
	  _configException = e;
      }
      
      ArrayList<ScanListener> selfList = new ArrayList<ScanListener>();
      selfList.add(listener);
      ScanManager scanManager = new ScanManager(selfList);

      for (URL url : urlList) {
	scanManager.scan(this, url);
      }
    }
  }

  /**
   * Called when the <class-loader> completes.
   */
  public void validate()
  {
    super.validate();
  }

  @Override
  public void scan()
  {
    ArrayList<URL> urlList = new ArrayList<URL>(_pendingScanUrls);
    _pendingScanUrls.clear();

    if (_scanListeners != null && urlList.size() > 0) {
      try {
	make();
      } catch (Exception e) {
	log().log(Level.WARNING, e.toString(), e);
	
	if (_configException == null)
	  _configException = e;
      }
      
      ScanManager scanManager = new ScanManager(_scanListeners);
      
      for (URL url : urlList) {
	scanManager.scan(this, url);
      }
    }

    sendAddLoaderEventImpl();
  }

  /**
   * Starts the config phase of the environment.
   */
  private void config()
  {
    sendAddLoaderEvent();
      
    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    int size = listeners.size();
    for (int i = 0; listeners != null && i < size; i++) {
      EnvironmentListener listener = listeners.get(i);

      listener.environmentConfig(this);
    }
  }

  /**
   * Marks the environment of the class loader as started.  The
   * class loader itself doesn't use this, but a callback might.
   */
  public void start()
  {
    if (! _lifecycle.toStarting())
      return;

    sendAddLoaderEvent();
    
    config();
      
    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    int size = listeners.size();
    for (int i = 0; listeners != null && i < size; i++) {
      EnvironmentListener listener = listeners.get(i);

      listener.environmentStart(this);
    }

    _lifecycle.toActive();
  }

  /**
   * Stops the environment, closing down any resources.
   *
   * The resources are closed in the reverse order that they're started
   */
  public void stop()
  {
    if (! _lifecycle.toDestroy())
      return;

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
            log().log(Level.WARNING, e.toString(), e);
          }
        }
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      // drain the thread pool for GC
      ResinThreadPoolExecutor.getThreadPool().stopEnvironment(this); 
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

    /*
    _isStarted = source._isStarted;
    _isActive = source._isActive;
    _isStopped = source._isStopped;
    */
  }

  /**
   * Destroys the class loader.
   */
  public void destroy()
  {
    try {
      WeakStopListener stopListener = _stopListener;
      _stopListener = null;

      // make sure it's stopped first
      stop();

      super.destroy();

      ClassLoader parent = getParent();
      for (; parent != null; parent = parent.getParent()) {
        if (parent instanceof EnvironmentClassLoader) {
          EnvironmentClassLoader loader = (EnvironmentClassLoader) parent;

          loader.removeListener(stopListener);
        }
      }
    } finally {
      _owner = null;
      _attributes = null;
      _listeners = null;
      _scanListeners = null;
    }
  }

  public String toString()
  {
    String hashCode = "";
    if (! com.caucho.util.Alarm.isTest())
      hashCode = "$" + System.identityHashCode(this);
    
    if (getId() != null)
      return getClass().getSimpleName() + hashCode + "[" + getId() + "]";
    else {
      return getClass().getSimpleName() + hashCode + "[]";
    }
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

      // #2281
      // PolicyImpl.init();

      EnvironmentStream.setStdout(System.out);
      EnvironmentStream.setStderr(System.err);

      try {
        Vfs.initJNI();
      } catch (Throwable e) {
      }

      if (System.getProperty("org.xml.sax.driver") == null)
        System.setProperty("org.xml.sax.driver", "com.caucho.xml.Xml");

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
	Class cl = Class.forName("com.caucho.naming.InitialContextFactoryImpl",
				 false,
				 systemLoader);

	isGlobalLoadable = (cl != null);
      } catch (Exception e) {
	log().log(Level.FINER, e.toString(), e);
      }
	
      if (isGlobalLoadable) {
	// These properties require Resin to be at the system loader
	
	if (props.get("java.naming.factory.initial") == null) {
	  props.put("java.naming.factory.initial",
		    "com.caucho.naming.InitialContextFactoryImpl");
	}

	props.put("java.naming.factory.url.pkgs", "com.caucho.naming");

	EnvironmentProperties.enableEnvironmentSystemProperties(true);

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
      }

      TransactionManagerImpl tm = TransactionManagerImpl.getInstance();
      // TransactionManagerImpl.setLocal(tm);
      //Jndi.bindDeep("java:comp/TransactionManager", tm);

      UserTransactionProxy ut = UserTransactionProxy.getInstance();

      Jndi.bindDeep("java:comp/env/jmx/MBeanServer",
                    Jmx.getContextMBeanServer());
      Jndi.bindDeep("java:comp/env/jmx/GlobalMBeanServer",
                    Jmx.getGlobalMBeanServer());
      
      Jndi.bindDeep("java:comp/UserTransaction", ut);

      // server/16g0
      // Applications are incorrectly using TransactionManager
      // as an extended UserTransaction
      Jndi.bindDeep("java:comp/TransactionManager", tm);
      Jndi.bindDeep("java:/TransactionManager", tm);
      
      Jndi.bindDeep("java:comp/ThreadPool",
		    ResinThreadPoolExecutor.getThreadPool());

      try {
        Jndi.rebindDeep("java:comp/ORB",
                        new com.caucho.iiop.orb.ORBImpl());
      } catch (Exception e) {
        e.printStackTrace();
      }

      J2EEManagedObject.register(new JTAResource(tm));
    } catch (NamingException e) {
      log().log(Level.FINE, e.toString(), e);
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private static final Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(EnvironmentClassLoader.class.getName());

    return _log;
  }
}
