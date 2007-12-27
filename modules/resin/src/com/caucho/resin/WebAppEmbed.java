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

import com.caucho.server.cluster.*;

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
  private String _contextPath = "";
  private String _rootDirectory = ".";
  private String _archivePath;

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
}
