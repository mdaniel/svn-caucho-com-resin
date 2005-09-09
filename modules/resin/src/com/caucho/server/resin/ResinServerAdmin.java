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
import com.caucho.util.L10N;
import com.caucho.jmx.IntrospectionAttributeDescriptor;
import com.caucho.jmx.AdminAttributeCategory;
import com.caucho.jmx.IntrospectionOperationDescriptor;
import com.caucho.jmx.IntrospectionMBeanDescriptor;

import javax.management.ObjectName;
import javax.management.MBeanOperationInfo;
import javax.management.MalformedObjectNameException;
import java.util.Date;
import java.util.ArrayList;

public class ResinServerAdmin
  implements ResinServerMBean
{
  private static final L10N L = new L10N(ResinServerAdmin.class);

  private static final ObjectName THREADPOOL_OBJECTNAME;

  static {
    try {
      THREADPOOL_OBJECTNAME = new ObjectName("resin:type=ThreadPool");
    }
    catch (MalformedObjectNameException e) {
      throw new AssertionError(e);
    }
  }

  private final ResinServer _resinServer;

  public ResinServerAdmin(ResinServer resinServer)
  {
    _resinServer = resinServer;
  }

  public void describe(IntrospectionMBeanDescriptor descriptor)
  {
    descriptor.setTitle(L.l("ResinServer"));
    descriptor.setDescription(L.l("An instance of Resin running in a JVM"));
  }

  public String getLocalHost()
  {
    return CauchoSystem.getLocalHost();
  }

  public void describeLocalHost(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setDescription(L.l("The ip address of the machine that is running this instance of Resin."));
    descriptor.setSortOrder(100);
  }

  public String getServerId()
  {
    return _resinServer.getServerId();
  }

  public void describeServerId(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setDescription(L.l("The server id used when starting this version of Resin, the value of `-server id'."));
    descriptor.setSortOrder(200);
  }

  public String getConfigFile()
  {
    return _resinServer.getConfigFile();
  }

  public void describeConfigFile(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setDescription(L.l("The configuration file used when starting this instance of Resin, the value of `-config file'."));
    descriptor.setSortOrder(300);
  }

  public String getResinHome()
  {
    return CauchoSystem.getResinHome().getNativePath();
  }

  public void describeResinHome(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setDescription(L.l("The Resin home directory used when starting this instance of Resin. This is the location of the Resin program files."));
    descriptor.setSortOrder(400);
  }

  public String getServerRoot()
  {
    return CauchoSystem.getServerRoot().getNativePath();
  }

  public void describeServerRoot(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CONFIGURATION);
    descriptor.setDescription(L.l("The server root directory used when starting this instance of Resin. This is the root directory of the web server files."));
    descriptor.setSortOrder(500);
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

  public void describeState(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);
    descriptor.setDescription(L.l("The current lifecycle state."));
    descriptor.setSortOrder(600);
  }

  public Date getInitialStartTime()
  {
    return _resinServer.getInitialStartTime();
  }

  public void describeInitialStartTime(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);
    descriptor.setDescription(L.l("The time that this instance was first started."));
    descriptor.setSortOrder(700);
  }

  public Date getStartTime()
  {
    return _resinServer.getStartTime();
  }

  public void describeStartTime(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.STATISTIC);
    descriptor.setDescription(L.l("The time that this instance was last started or restarted."));
    descriptor.setSortOrder(800);
  }

  public ObjectName getThreadPoolObjectName()
  {
    return THREADPOOL_OBJECTNAME;
  }

  public void describeThreadPoolObjectName(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CHILD);
    descriptor.setDescription(L.l("The ThreadPool manages all threads used by Resin."));
    descriptor.setSortOrder(900);
  }

  public ObjectName[] getServerObjectNames()
  {
    ArrayList<ServerController> servers = _resinServer.getServerList();

    ObjectName[] objectNames = new ObjectName[servers.size()];

    int i = 0;

    for (ServerController server : servers) {
      objectNames[i++] = server.getObjectName();
    }

    return objectNames;
  }

  public void describeServerObjectNames(IntrospectionAttributeDescriptor descriptor)
  {
    descriptor.setCategory(AdminAttributeCategory.CHILD);
    descriptor.setDescription(L.l("The servers that are active within this JVM."));
    descriptor.setSortOrder(1000);
  }

  public void restart()
  {
    _resinServer.destroy();
  }

  public void describeRestart(IntrospectionOperationDescriptor descriptor)
  {
    descriptor.setImpact(MBeanOperationInfo.ACTION);
    descriptor.setDescription(L.l("Exit this instance cleanly and allow the wrapper script to start a new JVM."));
    descriptor.setSortOrder(1100);
  }
}
