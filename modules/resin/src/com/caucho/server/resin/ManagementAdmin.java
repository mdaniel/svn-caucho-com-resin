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

import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.LocalActorSender;
import com.caucho.boot.WatchdogStatusQuery;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.jmx.MXName;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.server.admin.HmuxClientFactory;
import com.caucho.server.admin.JmxDumpQuery;
import com.caucho.server.admin.JmxListQuery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class ManagementAdmin extends AbstractManagedObject
  implements ManagementMXBean
{
  private final Resin _resin;

  /**
   * Creates the admin object and registers with JMX.
   */
  public ManagementAdmin(Resin resin)
  {
    _resin = resin;

    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  /**
   * Test interface
   */
  @Override
  public String hello()
  {
    return "hello, world";
  }

  @Override
  public String listJmx(String serverId,
                        String pattern,
                        boolean isPrintAttributes,
                        boolean isPrintValues,
                        boolean isPrintOperations,
                        boolean isPrintAllBeans,
                        boolean isPrintPlatformBeans)
  {
    JmxListQuery query = new JmxListQuery(pattern,
                                          isPrintAttributes,
                                          isPrintValues,
                                          isPrintOperations,
                                          isPrintAllBeans,
                                          isPrintPlatformBeans);

    return (String) query(serverId, query);
  }

  @Override
  public String dumpJmx(@MXName("server") String serverId)
  {
    JmxDumpQuery query = new JmxDumpQuery();

    return (String) query(serverId, query);
  }

  @Override
  public String getStatus(@MXName("server") String serverId)
  {
    WatchdogStatusQuery query = new WatchdogStatusQuery();

    return (String) query(serverId, query);
  }

  private CloudServer getServer(String server)
  {
    CloudServer cloudServer;

    if (server == null)
      cloudServer = NetworkClusterSystem.getCurrent().getSelfServer();
    else cloudServer = NetworkClusterSystem.getCurrent()
                                           .getSelfServer()
                                           .getPod()
                                           .findServer(
                                             server);

    return cloudServer;
  }

  private Object query(String serverId, Serializable query)
  {
    final ActorSender sender;

    CloudServer server = getServer(serverId);

    if (server.isSelf()) {
      sender = new LocalActorSender(BamSystem.getCurrentBroker(), "");
    }
    else {
      String authKey = Resin.getCurrent().getResinSystemAuthKey();

      HmuxClientFactory hmuxFactory
        = new HmuxClientFactory(server.getAddress(),
                                server.getPort(),
                                "",
                                authKey);

      sender = hmuxFactory.create();
    }

    return sender.query("manager@resin.caucho", query);
  }

  public InputStream test(String value, InputStream is)
    throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    out.write(value.getBytes());
    out.write('\n');

    int ch;

    while ((ch = is.read()) >= 0) {
      out.write(ch);
    }

    return new ByteArrayInputStream(out.toByteArray());
  }
}
