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

package com.caucho.loader.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.java.WorkDir;
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class OsgiManager
{
  private static final Logger log
    = Logger.getLogger(OsgiManager.class.getName());

  private static final EnvironmentLocal<OsgiManager> _localOsgi
    = new EnvironmentLocal<OsgiManager>();

  private ClassLoader _classLoader;
  private ClassLoader _parentLoader;

  private long _nextBundleId = 1;

  private OsgiBundle _systemBundle;

  private OsgiLoader _exportLoader;

  private Path _workRoot;
  
  private ArrayList<OsgiBundle> _bundleList
    = new ArrayList<OsgiBundle>();

  private HashMap<String,ExportBundleClassLoader> _exportMap
    = new HashMap<String,ExportBundleClassLoader>();

  private HashMap<String,ServiceReference> _serviceReferenceMap
    = new HashMap<String,ServiceReference>();
  
  private ArrayList<FrameworkListenerEntry> _frameworkListenerList
    = new ArrayList<FrameworkListenerEntry>();
  
  private ArrayList<BundleListenerEntry> _bundleListenerList
    = new ArrayList<BundleListenerEntry>();
  
  private ArrayList<ServiceListenerEntry> _serviceListenerList
    = new ArrayList<ServiceListenerEntry>();

  private ArrayList<OsgiBundle> _pendingStartupList
    = new ArrayList<OsgiBundle>();

  /**
   * Constructor should be called only from EnvironmentClassLoader
   */
  public OsgiManager(ClassLoader parentLoader)
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
    
    _parentLoader = _classLoader.getParent();

    _exportLoader = new OsgiLoader(this);

    if (_parentLoader instanceof DynamicClassLoader)
      ((DynamicClassLoader) _parentLoader).addLoader(_exportLoader);

    _systemBundle = new OsgiSystemBundle(this);
    _bundleList.add(_systemBundle);
  }

  public static OsgiManager getCurrent()
  {
    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();

    return loader.getOsgiManager();
  }

  public static OsgiManager create()
  {
    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();

    return loader.getOsgiManager();
  }

  public ClassLoader getParentLoader()
  {
    return _parentLoader;
  }

  Path getWorkRoot()
  {
    if (_workRoot == null)
      _workRoot = WorkDir.getLocalWorkDir(_parentLoader).lookup("_osgi");

    return _workRoot;
  }

  void addStartupBundle(Path path)
  {
    OsgiBundle bundle = addPath(path);

    synchronized (_pendingStartupList) {
      _pendingStartupList.add(bundle);
    }
  }

  /**
   * Adds a new jar
   */
  public OsgiBundle addPath(Path path)
  {
    JarPath jar = JarPath.create(path);

    OsgiBundle bundle = new OsgiBundle(nextBundleId(), this, jar);

    synchronized (_bundleList) {
      _bundleList.add(bundle);
    }

    sendBundleEvent(BundleEvent.INSTALLED, bundle);

    return bundle;
  }

  /**
   * Returns all bundles.
   */
  public Bundle []getBundles()
  {
    synchronized (_bundleList) {
      Bundle []bundles = new Bundle[_bundleList.size()];
      _bundleList.toArray(bundles);

      return bundles;
    }
  }

  /**
   * Returns all bundles.
   */
  public Bundle getBundle(long id)
  {
    synchronized (_bundleList) {
      for (int i = _bundleList.size() - 1; i >= 0; i--) {
	Bundle bundle = _bundleList.get(i);

	if (id == bundle.getBundleId())
	  return bundle;
      }
    }

    return null;
  }

  private long nextBundleId()
  {
    synchronized (this) {
      return _nextBundleId++;
    }
  }

  public ExportBundleClassLoader getExportLoader(String name)
  {
    return _exportMap.get(name);
  }

  public void putExportLoader(String name,
			      ExportBundleClassLoader loader)
  {
    _exportMap.put(name, loader);
    _exportMap.put(name.replace('.', '/'), loader);
  }

  /**
   * Returns any import class, e.g. from an osgi bundle
   */
  public Class findImportClass(String name)
  {
    int p = name.lastIndexOf('.');

    if (p < 0)
      return null;

    String packageName = name.substring(0, p);

    ExportBundleClassLoader loader;

    synchronized (_exportMap) {
      loader = _exportMap.get(packageName);

      try {
	return loader.findClassImpl(name);
      } catch (Exception e) {
      }
    }
    
    return null;
  }

  /**
   * Returns any import class, e.g. from an osgi bundle
   */
  public URL getImportResource(String name)
  {
    int p = name.lastIndexOf('/');

    if (p < 0)
      return null;

    String packageName = name.substring(0, p);

    ExportBundleClassLoader loader;

    synchronized (_exportMap) {
      loader = _exportMap.get(packageName);

      try {
	return loader.getResource(name);
      } catch (Exception e) {
      }
    }
    
    return null;
  }

  public void buildImportClassPath(StringBuilder head)
  {
    synchronized (_exportMap) {
      for (ExportBundleClassLoader exportLoader : _exportMap.values()) {
	for (Loader loader : exportLoader.getLoaders()) {
	  loader.buildClassPath(head);
	}
      }
    }
  }

  /**
   * Starts the manager
   */
  public void start()
  {
    ArrayList<OsgiBundle> startList = new ArrayList<OsgiBundle>();

    synchronized (_pendingStartupList) {
      startList.addAll(_pendingStartupList);
      _pendingStartupList.clear();
    }

    for (OsgiBundle bundle : startList) {
      _exportLoader.addBundle(bundle);

      try {
	bundle.start();
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
	
	BundleException exn = new BundleException(bundle + " " + e, e);

	sendFrameworkEvent(FrameworkEvent.ERROR, bundle, exn);
      }
    }

    sendFrameworkEvent(FrameworkEvent.STARTED, _systemBundle, null);
  }

  /**
   * Adds the bundle for installation
   */
  public void start(OsgiBundle bundle)
  {
    bundle.resolve();
    
    sendBundleEvent(BundleEvent.STARTING, bundle);

    try {
      bundle.startImpl();
    } finally {
      sendBundleEvent(BundleEvent.STARTED, bundle);
    }
  }

  //
  // services
  //

  /**
   * Registers a service
   */
  ServiceRegistration registerService(OsgiBundle bundle,
				      String []classNames,
				      Object service,
				      Dictionary properties)
  {
    OsgiServiceRegistration registration;
    
    registration = new OsgiServiceRegistration(this,
					       bundle,
					       classNames,
					       service,
					       properties);

    synchronized (_serviceReferenceMap) {
      for (String name : classNames) {
	_serviceReferenceMap.put(name, registration.getReference());
      }
    }

    sendServiceEvent(ServiceEvent.REGISTERED, registration.getReference());

    return registration;
  }
  
  /**
   * Returns the service reference for the given service
   */
  public ServiceReference getServiceReference(String className)
  {
    synchronized (_serviceReferenceMap) {
      return _serviceReferenceMap.get(className);
    }
  }
  
  /**
   * Returns the service reference for the given service
   */
  public ServiceReference []getServiceReferences(String className,
						 String filter)
    throws InvalidSyntaxException
  {
    ArrayList<ServiceReference> serviceList
      = new ArrayList<ServiceReference>();

    ServiceReference ref = getServiceReference(className);

    if (ref != null)
      serviceList.add(ref);

    ServiceReference []refArray = new ServiceReference[serviceList.size()];

    serviceList.toArray(refArray);

    return refArray;
  }
  
  /**
   * Returns the service reference for the given service
   */
  public ServiceReference []getAllServiceReferences(String className,
						    String filter)
    throws InvalidSyntaxException
  {
    return getServiceReferences(className, filter);
  }

  /**
   * Registers a service
   */
  void unregisterService(ServiceReference ref)
  {
    sendServiceEvent(ServiceEvent.UNREGISTERING, ref);
    
    synchronized (_serviceReferenceMap) {
      Iterator<Map.Entry<String,ServiceReference>> iter;
      iter = _serviceReferenceMap.entrySet().iterator();

      while (iter.hasNext()) {
	Map.Entry<String,ServiceReference> entry = iter.next();

	if (entry.getValue() == ref) {
	  iter.remove();
	}
      }
    }
  }

  //
  // listener management
  //

  /**
   * Adds a listener for framework events
   */
  void addFrameworkListener(OsgiBundle bundle,
			    FrameworkListener listener)
  {
    synchronized (_frameworkListenerList) {
      FrameworkListenerEntry entry;
      
      entry = new FrameworkListenerEntry(bundle, listener);

      for (int i = _frameworkListenerList.size() - 1; i >= 0; i--) {
	FrameworkListenerEntry oldEntry = _frameworkListenerList.get(i);

	if (oldEntry.getListener() == listener
	    && oldEntry.getBundle() == bundle) {
	  _frameworkListenerList.set(i, entry);
	  return;
	}
      }
	
      _frameworkListenerList.add(entry);
    }
  }

  /**
   * Removes a listener for framework events
   */
  void removeFrameworkListener(OsgiBundle bundle,
			       FrameworkListener listener)
  {
    synchronized (_frameworkListenerList) {
      for (int i = _frameworkListenerList.size() - 1; i >= 0; i--) {
	FrameworkListenerEntry entry = _frameworkListenerList.get(i);

	if (entry.getListener() == listener
	    && entry.getBundle() == bundle) {
	  _frameworkListenerList.remove(i);

	  return;
	}
      }
    }
  }

  private void sendFrameworkEvent(int type, Bundle bundle, Throwable exn)
  {
    ArrayList<FrameworkListenerEntry> listenerList = null;

    synchronized (_frameworkListenerList) {
      if (_frameworkListenerList.size() > 0) {
	listenerList = new ArrayList<FrameworkListenerEntry>();
	listenerList.addAll(_frameworkListenerList);
      }
    }

    if (listenerList != null) {
      FrameworkEvent event;

      event = new FrameworkEvent(type, bundle, exn);

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      for (FrameworkListenerEntry entry : listenerList) {
	FrameworkListener listener = entry.getListener();

	try {
	  thread.setContextClassLoader(entry.getBundle().getClassLoader());
	  
	  listener.frameworkEvent(event);
	} finally {
	  thread.setContextClassLoader(oldLoader);
	}
      }
    }
  }

  /**
   * Adds a listener for bundle events
   */
  void addBundleListener(OsgiBundle bundle,
			 BundleListener listener)
  {
    synchronized (_bundleListenerList) {
      BundleListenerEntry entry;
      
      entry = new BundleListenerEntry(bundle, listener);

      for (int i = _bundleListenerList.size() - 1; i >= 0; i--) {
	BundleListenerEntry oldEntry = _bundleListenerList.get(i);

	if (oldEntry.getListener() == listener
	    && oldEntry.getBundle() == bundle) {
	  _bundleListenerList.set(i, entry);
	  return;
	}
      }
	
      _bundleListenerList.add(entry);
    }
  }

  /**
   * Removes a listener for bundle events
   */
  void removeBundleListener(OsgiBundle bundle, BundleListener listener)
  {
    synchronized (_bundleListenerList) {
      for (int i = _bundleListenerList.size() - 1; i >= 0; i--) {
	BundleListenerEntry entry = _bundleListenerList.get(i);

	if (entry.getListener() == listener
	    && entry.getBundle() == bundle) {
	  _bundleListenerList.remove(i);

	  return;
	}
      }
    }
  }

  private void sendBundleEvent(int type, Bundle bundle)
  {
    ArrayList<BundleListenerEntry> listenerList = null;

    synchronized (_bundleListenerList) {
      if (_bundleListenerList.size() > 0) {
	listenerList = new ArrayList<BundleListenerEntry>();
	listenerList.addAll(_bundleListenerList);
      }
    }

    if (listenerList != null) {
      BundleEvent event = new BundleEvent(type, bundle);

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      
      for (BundleListenerEntry entry : listenerList) {
	BundleListener listener = entry.getListener();

	try {
	  thread.setContextClassLoader(entry.getBundle().getClassLoader());
	  
	  listener.bundleChanged(event);
	} finally {
	  thread.setContextClassLoader(oldLoader);
	}
      }
    }
  }

  /**
   * Adds a listener for service events
   */
  void addServiceListener(OsgiBundle bundle,
			  ServiceListener listener,
			  Filter filter)
  {
    synchronized (_serviceListenerList) {
      ServiceListenerEntry entry;
      
      entry = new ServiceListenerEntry(bundle, listener, filter);

      for (int i = _serviceListenerList.size() - 1; i >= 0; i--) {
	ServiceListenerEntry oldEntry = _serviceListenerList.get(i);

	if (oldEntry.getListener() == listener
	    && oldEntry.getBundle() == bundle) {
	  _serviceListenerList.set(i, entry);
	  return;
	}
      }
	
      _serviceListenerList.add(entry);
    }
  }

  /**
   * Removes a listener for service events
   */
  void removeServiceListener(OsgiBundle bundle, ServiceListener listener)
  {
    synchronized (_serviceListenerList) {
      for (int i = _serviceListenerList.size() - 1; i >= 0; i--) {
	ServiceListenerEntry entry = _serviceListenerList.get(i);

	if (entry.getListener() == listener
	    && entry.getBundle() == bundle) {
	  _serviceListenerList.remove(i);

	  return;
	}
      }
    }
  }

  private void sendServiceEvent(int type, ServiceReference ref)
  {
    ArrayList<ServiceListenerEntry> listenerList = null;

    synchronized (_serviceListenerList) {
      if (_serviceListenerList.size() > 0) {
	listenerList = new ArrayList<ServiceListenerEntry>();
	listenerList.addAll(_serviceListenerList);
      }
    }

    if (listenerList != null) {
      ServiceEvent event = new ServiceEvent(type, ref);

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      
      for (ServiceListenerEntry entry : listenerList) {
	ServiceListener listener = entry.getListener();

	// XXX: filter

	try {
	  thread.setContextClassLoader(entry.getBundle().getClassLoader());
	  
	  listener.serviceChanged(event);
	} finally {
	  thread.setContextClassLoader(oldLoader);
	}
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class FrameworkListenerEntry {
    private final OsgiBundle _bundle;
    private final FrameworkListener _listener;

    FrameworkListenerEntry(OsgiBundle bundle,
			   FrameworkListener listener)
    {
      _bundle = bundle;
      _listener = listener;
    }

    OsgiBundle getBundle()
    {
      return _bundle;
    }

    FrameworkListener getListener()
    {
      return _listener;
    }
  }

  static class BundleListenerEntry {
    private final OsgiBundle _bundle;
    private final BundleListener _listener;

    BundleListenerEntry(OsgiBundle bundle,
			BundleListener listener)
    {
      _bundle = bundle;
      _listener = listener;
    }

    OsgiBundle getBundle()
    {
      return _bundle;
    }

    BundleListener getListener()
    {
      return _listener;
    }
  }

  static class ServiceListenerEntry {
    private final OsgiBundle _bundle;
    private final ServiceListener _listener;
    private final Filter _filter;

    ServiceListenerEntry(OsgiBundle bundle,
			 ServiceListener listener,
			 Filter filter)
    {
      _bundle = bundle;
      _listener = listener;
      _filter = filter;
    }

    OsgiBundle getBundle()
    {
      return _bundle;
    }

    ServiceListener getListener()
    {
      return _listener;
    }
  }
}
