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

package com.caucho.resin;

import com.caucho.config.*;
import com.caucho.server.cluster.*;
import com.caucho.server.dispatch.*;
import com.caucho.server.webapp.*;

import java.util.*;

/**
 * Embeddable version of a Resin web-app.
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class WebAppEmbed
{
  private String _contextPath = "/";
  private String _rootDirectory = ".";
  private String _archivePath;

  private final ArrayList<ServletEmbed> _servletList
    = new ArrayList<ServletEmbed>();

  private final ArrayList<ServletMappingEmbed> _servletMappingList
    = new ArrayList<ServletMappingEmbed>();

  /**
   * Creates a new embedded webapp
   */
  public WebAppEmbed()
  {
  }

  /**
   * Creates a new embedded webapp
   *
   * @param contextPath the URL prefix of the web-app
   */
  public WebAppEmbed(String contextPath, String rootDirectory)
  {
    setContextPath(contextPath);
    setRootDirectory(rootDirectory);
  }

  /**
   * The context-path
   */
  public void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  /**
   * The context-path
   */
  public String getContextPath()
  {
    return _contextPath;
  }

  /**
   * The root directory of the expanded web-app
   */
  public void setRootDirectory(String rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * The root directory of the expanded web-app
   */
  public String getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * The path to the archive war file
   */
  public void setArchivePath(String archivePath)
  {
    _archivePath = archivePath;
  }

  /**
   * The path to the archive war file
   */
  public String getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Adds a servlet definition
   */
  public void addServlet(ServletEmbed servlet)
  {
    if (servlet == null)
      throw new NullPointerException();
    
    _servletList.add(servlet);
  }

  /**
   * Adds a servlet-mapping definition
   */
  public void addServletMapping(ServletMappingEmbed servletMapping)
  {
    if (servletMapping == null)
      throw new NullPointerException();
    
    _servletMappingList.add(servletMapping);
  }

  /**
   * Configures the web-app (for internal use)
   */
  protected void configure(WebApp webApp)
  {
    try {
      for (ServletEmbed servletEmbed : _servletList) {
	ServletConfigImpl servlet = webApp.createServlet();

	servletEmbed.configure(servlet);

	webApp.addServlet(servlet);
      }
    
      for (ServletMappingEmbed servletMappingEmbed : _servletMappingList) {
	ServletMapping servletMapping = webApp.createServletMapping();

	servletMappingEmbed.configure(servletMapping);

	webApp.addServletMapping(servletMapping);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
