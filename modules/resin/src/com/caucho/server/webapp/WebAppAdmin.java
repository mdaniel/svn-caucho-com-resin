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

package com.caucho.server.webapp;

import com.caucho.server.deploy.DeployControllerAdmin;

import com.caucho.server.session.SessionManager;

import com.caucho.mbeans.server.WebAppMBean;
import com.caucho.mbeans.server.HostMBean;

import com.caucho.server.cluster.Store;

import com.caucho.server.host.Host;

import com.caucho.util.L10N;

/**
 * The admin implementation for a web-app.
 */
public class WebAppAdmin extends DeployControllerAdmin<WebAppController>
  implements WebAppMBean
{
  private static L10N L = new L10N(WebAppAdmin.class);

  public WebAppAdmin(WebAppController controller)
  {
    super(controller);
  }

  //
  // Hierarchy attributes
  //

  /**
   * Returns the owning host
   */
  public HostMBean getHost()
  {
    Host host = getController().getHost();

    if (host != null)
      return host.getAdmin();
    else
      return null;
  }

  /**
   * Returns the context path
   */
  public String getContextPath()
  {
    return getController().getContextPath();
  }

  /**
   * Returns the active sessions.
   */
  public int getActiveSessionCount()
  {
    Application app = getApplication();

    if (app == null)
      return 0;

    SessionManager manager = app.getSessionManager();
    if (manager == null)
      return 0;

    return manager.getActiveSessionCount();
  }

  /**
   * Returns the session timeout (in milliseconds)
   */
  public long getSessionTimeout()
  {
    SessionManager manager = getSessionManager();

    if (manager != null)
      return manager.getSessionTimeout();
    else
      return 0;
  }

  /**
   * Returns the active sessions.
   */
  public long getSessionActiveCount()
  {
    SessionManager manager = getSessionManager();

    if (manager != null)
      return manager.getSessionActiveCount();
    else
      return 0;
  }

  /**
   * Returns the session create count
   */
  public long getSessionCreateCount()
  {
    SessionManager manager = getSessionManager();

    if (manager != null)
      return manager.getSessionCreateCount();
    else
      return 0;
  }

  /**
   * Returns the session invalidate count
   */
  public long getSessionInvalidateCount()
  {
    SessionManager manager = getSessionManager();

    if (manager != null)
      return manager.getSessionInvalidateCount();
    else
      return 0;
  }

  /**
   * Returns the session timeout count
   */
  public long getSessionTimeoutCount()
  {
    SessionManager manager = getSessionManager();

    if (manager != null)
      return manager.getSessionTimeoutCount();
    else
      return 0;
  }

  /**
   * Returns the session store type
   */
  public String getSessionStoreType()
  {
    Store store = getSessionStore();

    if (store == null)
      return null;

    String className = store.getStoreManager().getClass().getName();

    int p = className.lastIndexOf('.');

    return className.substring(p + 1);
  }

  private Store getSessionStore()
  {
    SessionManager manager = getSessionManager();

    if (manager == null)
      return null;

    return manager.getSessionStore();
  }


  private SessionManager getSessionManager()
  {
    Application app = getApplication();

    if (app == null)
      return null;

    return app.getSessionManager();
  }

  public int getConnectionCount()
  {
    return getApplication().getRequestCount();
  }

  public long getLifetimeConnectionCount()
  {
    return getController().getLifetimeConnectionCount();
  }

  public long getLifetimeConnectionTime()
  {
    return getController().getLifetimeConnectionTime();
  }

  public long getLifetimeReadBytes()
  {
    return getController().getLifetimeReadBytes();
  }

  public long getLifetimeWriteBytes()
  {
    return getController().getLifetimeWriteBytes();
  }

  public long getLifetimeClientDisconnectCount()
  {
    return getController().getLifetimeClientDisconnectCount();
  }

  /**
   * Returns the active application.
   */
  protected Application getApplication()
  {
    return getController().getApplication();
  }

}
