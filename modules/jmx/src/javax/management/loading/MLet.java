/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.management.loading;

import java.util.Set;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ServiceNotFoundException;

/**
 * ClassLoader for loading mlet beans.
 */
public class MLet extends URLClassLoader
  implements MLetMBean, MBeanRegistration {
  
  private String _libraryDirectory;
  private boolean delegateToCLR;
  
  /**
   * Creates a new classloader
   */
  public MLet()
  {
    super(new URL[0], Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates a new classloader based on a url set.
   */
  public MLet(URL []urls)
  {
    super(urls, Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates a new classloader based on a url set.
   */
  public MLet(URL []urls, ClassLoader parent)
  {
    super(urls, parent);
  }
  
  /**
   * Creates a new classloader based on a url set.
   */
  public MLet(URL []urls, ClassLoader parent, URLStreamHandlerFactory factory)
  {
    super(urls, parent, factory);
  }
  
  /**
   * Creates a new classloader based on a url set, delegating to the parent.
   *
   * @since JMX 1.2
   */
  public MLet(URL []urls, boolean delegateToCLR)
  {
    super(urls);

    this.delegateToCLR = delegateToCLR;
  }
  
  /**
   * Creates a new classloader based on a url set, delegating to the parent.
   *
   * @since JMX 1.2
   */
  public MLet(URL []urls, ClassLoader parent, boolean delegateToCLR)
  {
    super(urls, parent);

    this.delegateToCLR = delegateToCLR;
  }
  
  /**
   * Creates a new classloader based on a url set, delegating to the parent.
   *
   * @since JMX 1.2
   */
  public MLet(URL []urls, ClassLoader parent, URLStreamHandlerFactory factory,
	      boolean delegateToCLR)
  {
    super(urls, parent, factory);

    this.delegateToCLR = delegateToCLR;
  }

  /**
   * Adds a new URL to the search list.
   */
  public void addURL(URL url)
  {
    super.addURL(url);
  }

  /**
   * Adds a new URL to the search list.
   */
  public void addURL(String url)
    throws ServiceNotFoundException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the search URLs.
   */
  public Set getMBeansFromURL(URL url)
    throws ServiceNotFoundException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the search URLs.
   */
  public Set getMBeansFromURL(String url)
    throws ServiceNotFoundException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the library directory.
   */
  public String getLibraryDirectory()
  {
    return _libraryDirectory;
  }

  /**
   * Sets the library directory.
   */
  public void setLibraryDirectory(String libdir)
  {
    _libraryDirectory = libdir;
  }

  /**
   * Called before the mbean is registered.
   */
  public ObjectName preRegister(MBeanServer server, ObjectName name)
    throws Exception
  {
    return name;
  }

  /**
   * Called after the mbean is registered.
   */
  public void postRegister(Boolean registrationDone)
  {
  }

  /**
   * Called before deregistration
   */
  public void preDeregister()
    throws Exception
  {
  }

  /**
   * Called after deregistration
   */
  public void postDeregister()
  {
  }

  /**
   * Loads a class using the given repository.
   */
  public Class loadClass(String name, ClassLoaderRepository clr)
    throws ClassNotFoundException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Used for caching and versioning.
   *
   * @since JMX 1.2
   */
  public URL check(String version, URL codebase, String jarfile,
		   MLetMBean mlet)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
}

  
