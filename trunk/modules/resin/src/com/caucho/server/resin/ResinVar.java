/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.resin;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.Config;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.Path;

public class ResinVar {
  private static final Logger log = Logger.getLogger(ResinVar.class.getName());
  
  private String _serverId;
  private Path _resinHome;
  private Path _resinRoot;
  private Path _resinLog;
  private Path _resinConf;
  private boolean _isProfessional;
  private CloudServer _selfServer;
  
  ResinVar(String serverId,
           Path resinHome,
           Path resinRoot,
           Path resinLog,
           Path resinConf,
           boolean isProfessional,
           CloudServer selfServer)
  {
    _serverId = serverId;
    _resinHome = resinHome;
    _resinRoot = resinRoot;
    _resinLog = resinLog;
    _resinConf = resinConf;
    _isProfessional = isProfessional;
    _selfServer = selfServer;
  }
  
  /**
   * Returns the -server id
   */
  public String getServerId()
  {
    String serverId = (String) Config.getProperty("rvar0");

    if (serverId != null) {
      return serverId;
    }
    else {
      return _serverId;
    }
  }

  /**
   * @deprecated use {@link #getServerId()}
   */
  public String getId()
  {
    return getServerId();
  }

  /**
   * Returns the local address
   *
   * @return IP address
   */
  public String getAddress()
  {
    try {
      if (_selfServer != null) {
        return _selfServer.getAddress();
      }
      else if (CurrentTime.isTest())
        return "127.0.0.1";
      else
        return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return "localhost";
    }
  }

  /**
   * Returns the resin config.
   */
  public Path getConf()
  {
    return _resinConf;
  }

  /**
   * Returns the resin home.
   */
  public Path getHome()
  {
    return _resinHome;
  }

  /**
   * Returns the root directory.
   *
   * @return the root directory
   */
  public Path getRoot()
  {
    return _resinRoot;
  }

  /**
   * @deprecated use {@link #getRoot()}
   */
  public Path getRootDir()
  {
    return getRoot();
  }

  /**
   * @deprecated use {@link #getRoot()}
   */
  public Path getRootDirectory()
  {
    return getRoot();
  }
  
  public Path getLogDirectory()
  {
    return _resinLog;
  }

  /**
   * Returns the user
   */
  public String getUserName()
  {
    return System.getProperty("user.name");
  }

  /**
   * Returns the port (backward compat)
   */
  public int getPort()
  {
    if (_selfServer != null) {
      return _selfServer.getPort();
    }
    else
      return 0;
  }

  /**
   * Returns the port (backward compat)
   */
  public String getHttpAddress()
  {
    return getAddress();
  }

  /**
   * Returns the port (backward compat)
   */
  public String getHttpsAddress()
  {
    return getAddress();
  }

  /**
   * Returns the port (backward compat)
   */
  public int getHttpPort()
  {
    return 0;
  }

  /**
   * Returns the port (backward compat)
   */
  public int getHttpsPort()
  {
    return 0;
  }

  /**
   * Returns the version
   *
   * @return version
   */
  public String getVersion()
  {
    if (CurrentTime.isTest())
      return "3.1.test";
    else
      return VersionFactory.getVersion();
  }

  /**
   * Returns the version date
   *
   * @return version
   */
  public String getVersionDate()
  {
    if (CurrentTime.isTest())
      return "19980508T0251";
    else
      return VersionFactory.getVersionDate();
  }

  /**
   * Returns the local hostname
   *
   * @return version
   */
  public String getHostName()
  {
    try {
      if (CurrentTime.isTest())
        return "localhost";
      else
        return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      Logger.getLogger(ResinVar.class.getName()).log(Level.FINE, e.toString(), e);

      return "localhost";
    }
  }

  /**
   * Returns true for Resin professional.
   */
  public boolean isProfessional()
  {
    return _isProfessional;
  }
}
