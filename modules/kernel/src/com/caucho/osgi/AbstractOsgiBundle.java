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
import com.caucho.config.inject.InjectManager;
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
 * An osgi-bundle for exporting web-beans events
 */
public class AbstractOsgiBundle extends OsgiBundle
{
  protected AbstractOsgiBundle(OsgiManager manager)
  {
    this(manager, null);
  }
  
  protected AbstractOsgiBundle(OsgiManager manager, String name)
  {
    super(manager.nextBundleId(), manager, null, null, false);
    
    manager.addBundle(this);

    if (name == null)
      name = getClass().getName();
    
    setSymbolicName(name);

    install();
  }

  protected ClassLoader getClassLoader()
  {
    return getManager().getParentLoader();
  }

  //
  // Bundle API
  //

  /**
   * Returns the location
   */
  public String getLocation()
  {
    return getClass().getName();
  }

  /**
   * Start the bundle
   */
  public void start(int options)
    throws BundleException
  {
    start();
  }

  /**
   * Start the bundle
   */
  public void start()
    throws BundleException
  {
  }

  /**
   * Stop the bundle
   */
  public void stop(int options)
    throws BundleException
  {
  }

  /**
   * Start the bundle
   */
  public void stop()
    throws BundleException
  {
  }

  /**
   * Updates the bundle
   */
  public void update()
    throws BundleException
  {
  }

  /**
   * Updates the bundle from an input stream
   */
  public void update(InputStream is)
    throws BundleException
  {
  }

  /**
   * Uninstall the bundle
   */
  public void uninstall()
    throws BundleException
  {
  }

  /**
   * Returns the Manifest headers
   */
  public Dictionary getHeaders()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bundle's registered services
   */
  public ServiceReference []getRegisteredServices()
  {
    return new ServiceReference[0];
  }

  /**
   * Returns the services the bundle is using
   */
  public ServiceReference []getServicesInUse()
  {
    return new ServiceReference[0];
  }

  /**
   * Returns true if the bundle has the specified permission
   */
  public boolean hasPermission(Object permission)
  {
    return true;
  }

  /**
   * Returns the specified resource from the bundle
   */
  public URL getResource(String name)
  {
    return getClassLoader().getResource(name);
  }

  /**
   * Returns the localized view of the manifest
   */
  public Dictionary getHeaders(String locale)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Loads a class using the bundle's classloader
   */
  public Class loadClass(String name)
    throws ClassNotFoundException
  {
    return Class.forName(name, false, getClassLoader());
  }

  /**
   * Returns the resources for the bundle
   */
  public Enumeration getResources(String name)
    throws IOException
  {
    return getClassLoader().getResources(name);
  }

  /**
   * Returns the paths to entries in the bundle
   */
  public Enumeration getEntryPaths(String path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a URL to the named entry
   */
  public URL getEntry(String path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the last modified time of the bundle.
   */
  public long getLastModified()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns entries matching a pattern.
   */
  public Enumeration findEntries(String path,
				 String filePattern,
				 boolean recurse)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
