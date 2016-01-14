/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.pod;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.ConfigDeploy;
import com.caucho.v5.util.L10N;

/**
 * The configuration for a service in the resin.xml
 */
public class PodAppWeb extends ConfigDeploy
{
  private static final L10N L = new L10N(PodAppWeb.class);
  
  private String _contextPath;
  private String _path;
  
  /**
   * context-path: sets the web-app path
   */
  @ConfigArg(0)
  public void setContextPath(String contextPath)
  {
    if (! contextPath.startsWith("/")) {
      throw new ConfigException(L.l("Invalid context path '{0}'", contextPath));
    }
    
    _contextPath = contextPath;
  }

  public String getContextPath()
  {
    return _contextPath;
  }
  
  /**
   * path: sets the web-app location relative to the pod-app
   */
  @ConfigArg(1)
  public void setPath(String path)
  {
    if (path.startsWith("/") || path.indexOf(':') >= 0) {
      throw new ConfigException(L.l("Invalid web path '{0}'", path));
    }
    
    _path = path;
  }

  public String getPath()
  {
    return _path;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _contextPath + "]";
  }
}
