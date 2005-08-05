/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.resin;

import com.caucho.server.resin.mbean.ResinServerMBean;
import com.caucho.util.CauchoSystem;

import java.util.Date;

public class ResinServerAdmin
  implements ResinServerMBean
{
  private final ResinServer _resinServer;

  public ResinServerAdmin(ResinServer resinServer)
  {
    _resinServer = resinServer;
  }

  public String getLocalHost()
  {
    return CauchoSystem.getLocalHost();
  }

  public String getResinHome()
  {
    return CauchoSystem.getResinHome().getNativePath();
  }

  public String getServerRoot()
  {
    return CauchoSystem.getServerRoot().getNativePath();
  }

  public String getServerId()
  {
    return _resinServer.getServerId();
  }

  public String getConfigFile()
  {
    return _resinServer.getConfigFile();
  }

  public String getState()
  {
    // XXX: s/b _resinServer.getLifecycle()....
    
    if (_resinServer.isClosed())
      return "destroyed";

    if (_resinServer.isClosing())
      return "destroying";

    return "active";
  }

  public Date getInitialStartTime()
  {
    return _resinServer.getInitialStartTime();
  }

  public Date getStartTime()
  {
    return _resinServer.getStartTime();
  }

  public void restart()
  {
    _resinServer.destroy();
  }
}
