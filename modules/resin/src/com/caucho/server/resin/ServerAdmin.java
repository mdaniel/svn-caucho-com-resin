/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import javax.management.ObjectName;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.jmx.AdminAttributeCategory;
import com.caucho.jmx.AdminInfo;

import com.caucho.server.deploy.DeployControllerAdmin;

import com.caucho.server.resin.mbean.ServletServerMBean;

public class ServerAdmin extends DeployControllerAdmin<ServerController>
  implements ServletServerMBean
{
  private static final L10N L = new L10N(ServerAdmin.class);

  ServerAdmin(ServerController controller)
  {
    super(controller);
  }

  public AdminInfo getAdminInfo()
  {
    AdminInfo descriptor = super.getAdminInfo();

    String title;

    String id = getId();

    if (id == null || id.length() == 0)
      title = L.l("Server");
    else
      title = L.l("Server {0}", id);

    descriptor.setTitle(title);

    descriptor.createAdminAttributeInfo("PortObjectNames")
      .setCategory(AdminAttributeCategory.CHILD);

    descriptor.createAdminAttributeInfo("ClusterObjectNames")
      .setCategory(AdminAttributeCategory.CHILD);

    descriptor.createAdminAttributeInfo("HostObjectNames")
      .setCategory(AdminAttributeCategory.CHILD);

    descriptor.createAdminAttributeInfo("InvocationCacheHitCount")
      .setCategory(AdminAttributeCategory.STATISTIC);

    descriptor.createAdminAttributeInfo("InvocationCacheMissCount")
      .setCategory(AdminAttributeCategory.STATISTIC);

    descriptor.createAdminAttributeInfo("ProxyCacheHitCount")
      .setIgnored(true);

    descriptor.createAdminAttributeInfo("ProxyCacheMissCount")
      .setIgnored(true);

    return descriptor;
  }

  /**
   * Returns the server directory.
   */
  public String getRootDirectory()
  {
    Path path = getController().getRootDirectory();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Returns the id
   */
  public String getId()
  {
    return getController().getId();
  }

  /**
   * Returns the array of ports.
   */
  public ObjectName []getPortObjectNames()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getPortObjectNames();
    else
      return new ObjectName[0];
  }

  /**
   * Returns the array of cluster.
   */
  public ObjectName []getClusterObjectNames()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getClusterObjectNames();
    else
      return new ObjectName[0];
  }

  /**
   * Returns the array of hosts
   */
  public ObjectName []getHostObjectNames()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getHostObjectNames();
    else
      return new ObjectName[0];
  }

  /**
   * Clears the cache.
   */
  public void clearCache()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      server.clearCache();
  }

  /**
   * Clears the cache by regexp patterns.
   *
   * @param hostRegexp the regexp to match the host.  Null matches all.
   * @param urlRegexp the regexp to match the url. Null matches all.
   */
  public void clearCacheByPattern(String hostRegexp, String urlRegexp)
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      server.clearCacheByPattern(hostRegexp, urlRegexp);
  }

  /**
   * Returns the invocation cache hit count.
   */
  public long getInvocationCacheHitCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getInvocationCacheHitCount();
    else
      return -1;
  }

  /**
   * Returns the invocation cache miss count.
   */
  public long getInvocationCacheMissCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getInvocationCacheMissCount();
    else
      return -1;
  }

  /**
   * Returns the proxy cache hit count.
   */
  public long getProxyCacheHitCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getProxyCacheHitCount();
    else
      return -1;
  }

  /**
   * Returns the proxy cache miss count.
   */
  public long getProxyCacheMissCount()
  {
    ServletServer server = getDeployInstance();

    if (server != null)
      return server.getProxyCacheMissCount();
    else
      return -1;
  }

  protected ServletServer getDeployInstance()
  {
    return getController().getDeployInstance();
  }
}
