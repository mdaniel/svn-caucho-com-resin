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

package com.caucho.server.host;

import java.util.ArrayList;

import javax.management.ObjectName;

import com.caucho.vfs.Path;

import com.caucho.server.deploy.DeployControllerAdmin;

import com.caucho.mbeans.server.HostMBean;
import com.caucho.mbeans.server.WebAppMBean;

import com.caucho.server.webapp.WebAppController;
import com.caucho.util.L10N;

/**
 * The admin implementation for a host.
 */
public class HostAdmin extends DeployControllerAdmin<HostController>
  implements HostMBean
{
  private static final L10N L = new L10N(HostAdmin.class);

  /**
   * Creates the admin.
   */
  public HostAdmin(HostController controller)
  {
    super(controller);
  }

  public String getName()
  {
    return getController().getName();
  }

  public String getHostName()
  {
    return getController().getHostName();
  }

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
      path = host.getDocumentDirectory();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Returns the host's war directory.
   */
  public String getWarDirectory()
  {
    Path path = null;

    Host host = getHost();

    if (host != null)
      path = host.getWarDir();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  public String getWarExpandDirectory()
  {
    Path path = null;

    Host host = getHost();

    if (host != null)
      path = host.getWarExpandDir();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Updates a .war deployment.
   */
  public void updateWebAppDeploy(String name)
    throws Throwable
  {
    Host host = getHost();

    if (host != null)
      host.updateWebAppDeploy(name);
  }

  /**
   * Updates a .ear deployment.
   */
  public void updateEarDeploy(String name)
    throws Throwable
  {
    Host host = getHost();

    if (host != null)
      host.updateEarDeploy(name);
  }

  /**
   * Expand a .ear deployment.
   */
  public void expandEarDeploy(String name)
  {
    Host host = getHost();

    if (host != null)
      host.expandEarDeploy(name);
  }

  /**
   * Start a .ear deployment.
   */
  public void startEarDeploy(String name)
  {
    Host host = getHost();

    if (host != null)
      host.startEarDeploy(name);
  }

  /**
   * Returns the web-app names.
   */
  public ObjectName []getWebAppObjectNames()
  {
    Host host = getHost();

    if (host == null)
      return new ObjectName[0];

    ArrayList<WebAppController> webappList = host.getApplicationList();

    int size = webappList.size();

    ArrayList<ObjectName> webappNameList = new ArrayList<ObjectName>(size);

    for (int i = 0; i < size; i++) {
      ObjectName name = webappList.get(i).getObjectName();

      if (name != null)
        webappNameList.add(name);
    }

    ObjectName[] webappNames = new ObjectName[webappNameList.size()];

    webappNames = webappNameList.toArray(webappNames);

    return webappNames;
  }

  final public ObjectName []getWebAppNames()
  {
    return getWebAppObjectNames();
  }

  /**
   * Returns the webapps.
   */
  public WebAppMBean []getWebApps()
  {
    Host host = getHost();

    if (host == null)
      return new WebAppMBean[0];

    ArrayList<WebAppController> webappList = host.getApplicationList();

    WebAppMBean []webapps = new WebAppMBean[webappList.size()];

    for (int i = 0; i < webapps.length; i++) {
      WebAppController controller = webappList.get(i);

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
  public String toString()
  {
    return "HostAdmin[" + getName() + "]";
  }
}
