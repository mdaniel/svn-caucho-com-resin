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
import java.util.Enumeration;

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import javax.management.ServiceNotFoundException;

/**
 * Remote management interface of the MLet.
 */
public interface MLetMBean {
  /**
   * Loads ObjectNames from a URL for the MLet bean
   *
   * @param url the url for the MLET tags
   */
  public Set getMBeansFromURL(String url)
    throws ServiceNotFoundException;
  
  /**
   * Loads ObjectNames from a URL for the MLet bean
   *
   * @param url the url for the MLET tags
   */
  public Set getMBeansFromURL(URL url)
    throws ServiceNotFoundException;
  
  /**
   * Adds the URL to the list of URLs to search for classes and resources.
   *
   * @param url the url to add
   */
  public void addURL(URL url);
  
  /**
   * Adds the URL to the list of URLs to search for classes and resources.
   *
   * @param url the url to add
   */
  public void addURL(String url)
    throws ServiceNotFoundException;
  
  /**
   * Returns the search path of URLs for loading classes.
   *
   * @return the URLs to be searched
   */
  public URL []getURLs();
  
  /**
   * Returns the URL for the named research, searched in the path.
   *
   * @param name the name of the resource
   *
   * @return the URLs for the resource.
   */
  public URL getResource(String name);
  
  /**
   * Returns an input stream for the specified resource.
   *
   * @param name the name of the resource
   *
   * @return the input stream
   */
  public InputStream getResourceAsStream(String name);
  
  /**
   * Returns an enumeration of the resources with the given name
   *
   * @param name the name of the resource
   *
   * @return the input stream
   */
  public Enumeration getResources(String name)
    throws IOException;
  
  /**
   * Returns the current directory used by the library loader for
   * storing native libraries.
   *
   * @return the current library directory
   */
  public String getLibraryDirectory();
  
  /**
   * Set the directory used by the library loader for
   * storing native libraries.
   *
   * @param libdir the library directory
   */
  public void setLibraryDirectory(String libdir);
}

  
