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
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
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
import org.osgi.framework.Filter;
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

  private ClassLoader _parentLoader;

  private long _nextBundleId = 1;
  
  private ArrayList<OsgiBundle> _bundleList
    = new ArrayList<OsgiBundle>();

  private HashMap<String,ExportBundleClassLoader> _exportMap
    = new HashMap<String,ExportBundleClassLoader>();

  private HashMap<String,ServiceReference> _serviceReferenceMap
    = new HashMap<String,ServiceReference>();
  
  private ArrayList<ServiceListenerEntry> _serviceListenerList
    = new ArrayList<ServiceListenerEntry>();

  private OsgiManager()
  {
    _parentLoader = Thread.currentThread().getContextClassLoader().getParent();
  }

  public static OsgiManager getCurrent()
  {
    return _localOsgi.getLevel();
  }

  public static OsgiManager create()
  {
    synchronized (_localOsgi) {
      OsgiManager manager = _localOsgi.getLevel();

      if (manager == null) {
	manager = new OsgiManager();
	_localOsgi.set(manager);
      }

      return manager;
    }
  }

  public ClassLoader getParentLoader()
  {
    return _parentLoader;
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
  }

  /**
   * Adds the bundle for installation
   */
  public void install(OsgiBundle bundle)
  {
    bundle.activate();
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
