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

package com.caucho.v5.http.host;

import java.util.ArrayList;

import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.vfs.PathImpl;

/**
 * A configuration entry for a host
 */
public class HostVar
{
  // DeployHandle<Host> _hostHandle;
  HostController _hostController;
  
  HostVar(HostController controller)
  {
    _hostController = controller;
  }

  public String getName()
  {
    return _hostController.getId();
  }

  public String getHostName()
  {
    return _hostController.getHostName();
    /*
    Host host = _hostController.getHostName(); // .getDeployInstance();
    
    if (host != null) {
      return host.getHostName();
    }
    else {
      return getName();
    }
    */
  }

  public String getUrl()
  {
    /*
    Host host = _hostController.getDeployInstance();

    if (host != null)
      return host.getURL();
      */
    
    String hostName = getHostName();
    
    if (hostName.equals(""))
      return "";
    else if (hostName.startsWith("http:")
        || hostName.startsWith("https:"))
      return hostName;
    else
      return "http://" + hostName;
  }

  public ArrayList<String> getRegexp()
  {
    // return (ArrayList<String>) _hostHandle.getVariableMap().get("regexp");
    return new ArrayList<>();
  }

  public PathImpl getRoot()
  {
    return _hostController.getRootDirectory();
    /*
    Host host = _hostController.getDeployInstance();

    if (host != null) {
      return host.getWebAppContainer().getRootDirectory();
    }
    else {
      // return _hostHandle.getRootDirectory();
      throw new IllegalStateException();
    }
    */
  }

  /**
   * @deprecated
   */
  /*
  public Path getRootDir()
  {
    return getRoot();
  }
  */

  /**
   * @deprecated
   */
  public PathImpl getRootDirectory()
  {
    return getRoot();
  }
  
  /*
  public Path getDocumentDirectory()
  {
    return getRoot();
  }
  */

  public PathImpl getWarDirectory()
  {
    /*
    Host host = _hostController.getDeployInstance();

    if (host != null)
      return host.getWebAppContainer().getWarDir();
    else
      return null;
      */
    return getWarDir();
  }

  public PathImpl getWarDir()
  {
    return _hostController.getWarExpandDirectory();
    //return getWarDirectory();
  }

  /*
  public Path getWarExpandDirectory()
  {
    Host host = _hostController.getDeployInstance();

    if (host != null)
      return host.getWebAppContainer().getWarExpandDir();
    else
      return null;
  }
  */

/*
  public Path getWarExpandDir()
  {
    return getWarExpandDirectory();
  }
  */

  @Override
  public String toString()
  {
    return "Host[" + _hostController.getId() + "]";
  }
}
