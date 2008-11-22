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
 * An osgi-bundle
 */
public class OsgiServiceRegistration implements ServiceRegistration
{
  private static final L10N L = new L10N(ServiceRegistration.class);
  private static final Logger log
    = Logger.getLogger(OsgiServiceRegistration.class.getName());

  private final OsgiManager _manager;
  private final OsgiBundle _bundle;

  private final String []_classNames;
  
  private final HashMap<String,Object> _properties
    = new HashMap<String,Object>();

  private Object _service;

  private OsgiServiceReference _reference;

  private OsgiServiceAdmin _admin;

  OsgiServiceRegistration(OsgiManager manager,
			  OsgiBundle bundle,
			  String []classNames,
			  Object service,
			  Dictionary properties)
  {
    _manager = manager;
    _bundle = bundle;
    _classNames = classNames;
    _service = service;

    setProperties(properties);

    _properties.put("objectClass", classNames);

    _reference = new OsgiServiceReference(manager, bundle, classNames, this);

    _admin = new OsgiServiceAdmin(this);

    _admin.register();
  }
  
  /**
   * Returns reference to the service
   */
  public ServiceReference getReference()
  {
    return _reference;
  }

  /**
   * Sets a service's properties
   */
  public void setProperties(Dictionary properties)
  {
    _properties.clear();
    
    if (properties == null)
      return;

    Enumeration e = properties.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();

      _properties.put(key, properties.get(key));
    }
  }

  /**
   * Unregisters the service
   */
  public void unregister()
  {
    try {
      _reference.unregister();
      
      _manager.unregisterService(getReference());
      
      _admin.register();
    } finally {
      _service = null;
    }
  }

  //
  // Resin methods
  //

  Object getService()
  {
    return _service;
  }

  Object getProperty(String key)
  {
    Object value = _properties.get(key);

    if (value != null)
      return value;

    for (Map.Entry<String,Object> entry : _properties.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(key))
	return entry.getValue();
    }

    return null;
  }

  String []getPropertyKeys()
  {
    int size = _properties.size();
    
    String []keys = new String[size];

    _properties.keySet().toArray(keys);
      
    return keys;
  }

  OsgiBundle getBundle()
  {
    return _bundle;
  }
  
  OsgiServiceAdmin getAdmin()
  {
    return _admin;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _service + "]";
  }
}
