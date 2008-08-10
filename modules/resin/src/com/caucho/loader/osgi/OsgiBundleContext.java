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
public class OsgiBundleContext implements BundleContext
{
  private static final L10N L = new L10N(OsgiBundleContext.class);
  private static final Logger log
    = Logger.getLogger(OsgiBundleContext.class.getName());

  private OsgiManager _manager;
  private OsgiBundle _bundle;

  OsgiBundleContext(OsgiManager manager, OsgiBundle bundle)
  {
    _manager = manager;
    _bundle = bundle;
  }

  //
  // framework methods
  //

  /**
   * Adds a listener for framework events
   */
  public void addFrameworkListener(FrameworkListener listener)
  {
    _manager.addFrameworkListener(_bundle, listener);
  }

  /**
   * Removes a listener for framework events
   */
  public void removeFrameworkListener(FrameworkListener listener)
  {
    _manager.removeFrameworkListener(_bundle, listener);
  }

  //
  // bundle methods
  //
  
  /**
   * Returns the bundle property.  Search the Framework properties,
   * then the system properties
   */
  public String getProperty(String key)
  {
    return null;
  }

  /**
   * Returns the Bundle for this context
   */
  public Bundle getBundle()
  {
    return _bundle;
  }

  /**
   * Installs a bundle from the location string
   */
  public Bundle installBundle(String location)
    throws BundleException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Installs a bundle from an input stream.
   */
  public Bundle installBundle(String location,
			      InputStream is)
    throws BundleException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bundle with the given id.
   */
  public Bundle getBundle(long id)
  {
    return _manager.getBundle(id);
  }

  /**
   * Returns all the installed bundles
   */
  public Bundle []getBundles()
  {
    return _manager.getBundles();
  }

  /**
   * Adds a listener for bundle events
   */
  public void addBundleListener(BundleListener listener)
  {
    _manager.addBundleListener(_bundle, listener);
  }

  /**
   * Removes a listener for bundle events
   */
  public void removeBundleListener(BundleListener listener)
  {
    _manager.removeBundleListener(_bundle, listener);
  }

  /**
   * Returns a data storage area for the bundle
   */
  public File getDataFile(String fileName)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // service methods
  //

  /**
   * Adds a listener for service events
   */
  public void addServiceListener(ServiceListener listener,
				 String filter)
    throws InvalidSyntaxException
  {
    _manager.addServiceListener(_bundle, listener, createFilter(filter));
  }

  /**
   * Adds a listener for service events
   */
  public void addServiceListener(ServiceListener listener)
  {
    _manager.addServiceListener(_bundle, listener, null);
  }

  /**
   * Removes a listener for service events
   */
  public void removeServiceListener(ServiceListener listener)
  {
    _manager.removeServiceListener(_bundle, listener);
  }

  /**
   * Registers a service
   */
  public ServiceRegistration registerService(String []classNames,
					     Object service,
					     Dictionary properties)
  {
    ServiceRegistration reg = _manager.registerService(_bundle,
						       classNames,
						       service,
						       properties);

    return reg;
  }

  /**
   * Registers a service
   */
  public ServiceRegistration registerService(String className,
					     Object service,
					     Dictionary properties)
  {
    return registerService(new String[] { className },
			   service,
			   properties);
  }

  /**
   * Returns matching services.
   */
  public ServiceReference []getServiceReferences(String className,
						 String filter)
    throws InvalidSyntaxException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns all matching services.
   */
  public ServiceReference []getAllServiceReferences(String className,
						    String filter)
    throws InvalidSyntaxException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a service reference
   */
  public ServiceReference getServiceReference(String className)
  {
    return _manager.getServiceReference(className);
  }

  /**
   * Returns the service object for the service
   */
  public Object getService(ServiceReference reference)
  {
    OsgiServiceReference ref = (OsgiServiceReference) reference;

    return ref.getService(_bundle);
  }

  /**
   * Release the service object for the service
   */
  public boolean ungetService(ServiceReference reference)
  {
    OsgiServiceReference ref = (OsgiServiceReference) reference;

    return ref.ungetService(_bundle);
  }

  /**
   * Creates a filter
   */
  public Filter createFilter(String filter)
    throws InvalidSyntaxException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bundle + "]";
  }
}
