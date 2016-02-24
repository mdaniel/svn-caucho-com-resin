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

package com.caucho.v5.http.webapp;

import java.util.Date;
import java.util.Map;

import com.caucho.v5.deploy.DeployControllerAdmin;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.jmx.server.ConfigMXBean;
import com.caucho.v5.management.server.HostMXBean;
import com.caucho.v5.management.server.SessionManagerMXBean;
import com.caucho.v5.management.server.WebAppMXBean;

/**
 * The admin implementation for a web-app.
 */
public class WebAppAdmin extends DeployControllerAdmin<WebAppResinBase,WebAppController>
  implements WebAppMXBean
{
  public WebAppAdmin(DeployHandle<WebAppResinBase> handle)
  {
    super(handle);
  }

  //
  // Hierarchy attributes
  //

  /**
   * Returns the owning host
   */
  @Override
  public HostMXBean getHost()
  {
    Host host = getController().getHost();

    if (host != null)
      return host.getAdmin();
    else
      return null;
  }

  @Override
  public SessionManagerMXBean getSessionManager()
  {
    WebAppResinBase app = getWebApp();

    if (app == null)
      return null;

    return app.getSessionManager().getAdmin();
  }
  
  @Override
  public ConfigMXBean []getConfigs()
  {
    WebAppResinBase app = getWebApp();

    if (app == null) {
      return null;
    }
    
    /*
    Collection<ConfigMXBean> beans = ConfigAdmin.getMBeans(app.getClassLoader());
    ConfigMXBean[] array = new ConfigMXBean[beans.size()];
    beans.toArray(array);
    
    return array;
    */
    
    return null;
  }

  //
  // Configuration attribute
  // 

  /**
   * Returns the context path
   */
  @Override
  public String getContextPath()
  {
    return getController().getContextPath();
  }

  /**
   * Returns the web-app version number
   */
  @Override
  public String getVersion()
  {
    return getController().getVersion();
  }

  /**
   * Returns the manifest attributes
   */
  @Override
  public Map<String,String> getManifestAttributes()
  {
    return getController().getManifestAttributes();
  }
  
  //
  // lifecycle statistics
  //
  
  @Override
  public boolean isEnabled()
  {
    WebAppResinBase webApp = getWebApp();
    
    return webApp != null && webApp.isEnabled();
  }

  //
  // error statistics
  //

  @Override
  public long getStatus500CountTotal()
  {
    WebAppResinBase webApp = getWebApp();
    
    if (webApp != null)
      return webApp.getStatus500CountTotal();
    else
      return 0;
  }

  @Override
  public Date getStatus500LastTime()
  {
    WebAppResinBase webApp = getWebApp();
    
    if (webApp == null)
      return null;
    
    long lastTime = webApp.getStatus500LastTime();

    if (lastTime > 0)
      return new Date(lastTime);
    else
      return null;
  }
  
  //
  // statistics
  //
  
  @Override
  public int getRequestCount()
  {
    WebAppResinBase webApp = getWebApp();
    
    if (webApp != null)
      return webApp.getRequestCount();
    else
      return 0;
  }

  @Override
  public long getRequestCountTotal()
  {
    return getController().getLifetimeConnectionCount();
  }

  @Override
  public long getRequestTimeTotal()
  {
    return getController().getLifetimeConnectionTime();
  }

  @Override
  public long getRequestReadBytesTotal()
  {
    return getController().getLifetimeReadBytes();
  }

  @Override
  public long getRequestWriteBytesTotal()
  {
    return getController().getLifetimeWriteBytes();
  }

  @Override
  public long getClientDisconnectCountTotal()
  {
    return getController().getLifetimeClientDisconnectCount();
  }

  /**
   * Returns the active webApp.
   */
  protected WebAppResinBase getWebApp()
  {
    return getHandle().getDeployInstance();
  }
}
