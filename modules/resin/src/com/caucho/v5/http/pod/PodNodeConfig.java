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

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.ConfigDeploy;
import com.caucho.v5.util.L10N;

/**
 * The configuration for a pod in the resin.xml
 */
public class PodNodeConfig extends ConfigDeploy
{
  private static final L10N L = new L10N(PodNodeConfig.class);

  // The context path
  private String _contextPath;
  
  // private ServiceConfig _prologue;

  public PodNodeConfig()
  {
  }

  /**
   * Gets the context path
   */
  public String getContextPath()
  {
    String cp = _contextPath;

    if (cp == null)
      cp = getId();

    if (cp == null)
      return null;

    if (cp.endsWith("/"))
      return cp.substring(0, cp.length() - 1);
    else
      return cp;
  }

  /**
   * Sets the context path
   */
  public void setContextPath(String path)
    throws ConfigException
  {
    if (! path.startsWith("/"))
      throw new ConfigException(L.l("context-path '{0}' must start with '/'.",
                                    path));
    
    _contextPath = path;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _contextPath + "]";
  }
}
