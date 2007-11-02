/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package org.netbeans.modules.j2ee.resin;

import java.util.logging.Logger;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.status.ProgressObject;
import org.netbeans.modules.j2ee.deployment.plugins.api.ServerDebugInfo;
import org.netbeans.modules.j2ee.deployment.plugins.api.StartServer;

public class ResinStartServer extends StartServer
{
  private static final Logger log = Logger.getLogger(ResinStartServer.class.getName());

  public ProgressObject startDebugging(Target target)
  {
    return null;
  }
  
  public boolean isDebuggable(Target target)
  {
    return false;
  }
  
  public boolean isAlsoTargetServer(Target target)
  {
    return true;
  }
  
  public ServerDebugInfo getDebugInfo(Target target)
  {
    return null;
  }
  
  public boolean supportsStartDeploymentManager()
  {
    return false;
  }
  
  public ProgressObject stopDeploymentManager()
  {
    return null;
  }
  
  public ProgressObject startDeploymentManager()
  {
    return null;
  }
  
  public boolean needsStartForTargetList()
  {
    return false;
  }
  
  public boolean needsStartForConfigure()
  {
    return false;
  }
  
  public boolean needsStartForAdminConfig()
  {
    return false;
  }
  
  public boolean isRunning()
  {
    return false;
  }
}