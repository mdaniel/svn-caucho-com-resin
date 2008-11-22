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

package com.caucho.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
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

import org.osgi.framework.*;

/**
 * An osgi service reference
 */
public class OsgiServiceReference implements ServiceReference
{
  private static final L10N L = new L10N(ServiceReference.class);
  private static final Logger log
    = Logger.getLogger(OsgiServiceReference.class.getName());

  private final OsgiManager _manager;
  private final OsgiBundle _bundle;

  private final String []_classNames;

  private final OsgiServiceRegistration _registration;

  private ArrayList<OsgiBundle> _useList = new ArrayList<OsgiBundle>();

  OsgiServiceReference(OsgiManager manager,
		       OsgiBundle bundle,
		       String []classNames,
		       OsgiServiceRegistration registration)
  {
    _manager = manager;
    _bundle = bundle;
    _classNames = classNames;

    _registration = registration;
  }
  
  /**
   * Returns the service's property
   */
  public Object getProperty(String key)
  {
    return _registration.getProperty(key);
  }

  /**
   * Returns all the service's property keys
   */
  public String []getPropertyKeys()
  {
    return _registration.getPropertyKeys();
  }

  /**
   * Returns the bundle that registered the service
   */
  public Bundle getBundle()
  {
    return _bundle;
  }

  /**
   * Returns the bundles using the service
   */
  public Bundle []getUsingBundles()
  {
    return new Bundle[0];
  }

  /**
   * Checks if the bundled which registered the service uses this class name
   */
  public boolean isAssignableTo(Bundle bundle,
				String className)
  {
    if (_bundle != bundle)
      return false;

    for (String name : _classNames) {
      if (name.equals(className))
	return true;
    }
    
    return false;
  }

  /**
   * Compares to another reference for ordering
   */
  public int compareTo(Object reference)
  {
    return 0;
  }

  /**
   * Returns the service registration
   */
  OsgiServiceRegistration getRegistration()
  {
    return _registration;
  }
  
  /**
   * Returns the service object for the service
   */
  Object getService(OsgiBundle bundle)
  {
    if (! _useList.contains(bundle))
      _useList.add(bundle);
    
    return _registration.getService();
  }

  /**
   * Release the service object for the service
   */
  boolean ungetService(OsgiBundle bundle)
  {
    if (_useList.remove(bundle)) {
      bundle.ungetService(this);
      
      return true;
    }
    
    return false;
  }

  void unregister()
  {
    ArrayList<OsgiBundle> useList;

    synchronized (_useList) {
      useList = new ArrayList<OsgiBundle>(_useList);
      _useList.clear();
    }
      
    for (OsgiBundle bundle : useList) {
      bundle.ungetService(this);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _classNames[0] + "]";
  }
}
