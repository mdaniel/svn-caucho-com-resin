/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.logging.Logger;

/**
 * Configuration for management.
 */
public class Management
{
  private static L10N L = new L10N(Management.class);
  private static Logger log = Logger.getLogger(Management.class.getName());

  private Resin _resin;
  private Path _path;
  private String _remoteEnableCookie;

  void setResin(Resin resin)
  {
    _resin = resin;
  }

  public Resin getResin()
  {
    return _resin;
  }
  
  public void setPath(Path path)
  {
    _path = path;
  }
  
  public Path getPath()
  {
    return _path;
  }
    
  public void setRemoteEnableCookie(String cookie)
  {
    if ("false".equalsIgnoreCase(cookie)
	|| "no".equalsIgnoreCase(cookie)
        || "off".equalsIgnoreCase(cookie)
	|| "".equals(cookie)) {
      _remoteEnableCookie = null;
    }
    else
      _remoteEnableCookie = cookie;
  }
    
  public String getRemoteEnableCookie()
  {
    return _remoteEnableCookie;
  }
    
  public void setRemoteEnable(boolean isRemote)
  {
    log.config(L.l("remote-enable is deprecated.  Please use remote-enable-cookie instead."));
    
    if (isRemote)
      _remoteEnableCookie = "true";
    else
      _remoteEnableCookie = null;
  }
    
  public boolean isRemoteEnable()
  {
    return _remoteEnableCookie != null;
  }

  public void start()
  {
    if (getPath() != null) {
      try {
	getPath().mkdirs();
      } catch (Exception e) {
	throw new ConfigException(e);
      }
    }
  }
}
