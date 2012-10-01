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
 * @author Scott Ferguson
 */

package com.caucho.server.host;

import com.caucho.env.deploy.DeployControllerAdmin;
import com.caucho.env.deploy.DeployException;
import com.caucho.management.server.HostMXBean;
import com.caucho.management.server.WebAppMXBean;
import com.caucho.server.webapp.WebAppController;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;

/**
 * The admin implementation for a host.
 */
@SuppressWarnings("serial")
public class HostAdmin extends DeployControllerAdmin<HostController>
  implements HostMXBean
{
  /**
   * Creates the admin.
   */
  public HostAdmin(HostController controller)
  {
    super(controller);
  }

  @Override
  public String getName()
  {
    String name = getController().getName();

    if (name == null || name.equals(""))
      return "default";
    else
      return name;
  }

  @Override
  public String getHostName()
  {
    return getController().getHostName();
  }

  @Override
  public String getURL()
  {
    Host host = getHost();

    if (host != null)
      return host.getURL();
    else
      return null;
  }

  /**
   * Returns the host's document directory.
   */
  @Override
  public String getRootDirectory()
  {
    Path path = null;

    Host host = getHost();

    if (host != null)
      path = host.getRootDirectory();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Returns the host's document directory.
   */
  public String getDocumentDirectory()
  {
    Path path = null;

    Host host = getHost();

    if (host != null)
      path = host.getWebAppContainer().getDocumentDirectory();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Returns the host's war directory.
   */
  @Override
  public String getWarDirectory()
  {
    Path path = null;

    Host host = getHost();

    if (host != null)
      path = host.getWebAppContainer().getWarDir();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  @Override
  public String getWarExpandDirectory()
  {
    Path path = null;

    Host host = getHost();

    if (host != null)
      path = host.getWebAppContainer().getWarExpandDir();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Updates a .war deployment.
   */
  public void updateWebAppDeploy(String name)
    throws DeployException
  {
    Host host = getHost();

    try {
      if (host != null)
        host.getWebAppContainer().updateWebAppDeploy(name);
    } catch (Throwable e) {
      throw new DeployException(e);
    }
  }

  /**
   * Updates a .ear deployment.
   */
  @Override
  public void updateEarDeploy(String name)
    throws DeployException
  {
    Host host = getHost();

    try {
      if (host != null)
        host.getWebAppContainer().updateEarDeploy(name);
    } catch (Throwable e) {
      throw new DeployException(e);
    }
  }

  /**
   * Expand a .ear deployment.
   */
  public void expandEarDeploy(String name)
  {
    Host host = getHost();

    if (host != null)
      host.getWebAppContainer().expandEarDeploy(name);
  }

  /**
   * Start a .ear deployment.
   */
  @Override
  public void startEarDeploy(String name)
  {
    Host host = getHost();

    if (host != null)
      host.getWebAppContainer().startEarDeploy(name);
  }

  /**
   * Returns the webapps.
   */
  @Override
  public WebAppMXBean []getWebApps()
  {
    Host host = getHost();

    if (host == null)
      return new WebAppMXBean[0];

    WebAppController []webappList = host.getWebAppContainer().getWebAppList();

    WebAppMXBean []webapps = new WebAppMXBean[webappList.length];

    for (int i = 0; i < webapps.length; i++) {
      WebAppController controller = webappList[i];

      webapps[i] = controller.getAdmin();
    }

    return webapps;
  }

  /**
   * Returns the host.
   */
  protected Host getHost()
  {
    return getController().getDeployInstance();
  }

  /**
   * Returns a string view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
