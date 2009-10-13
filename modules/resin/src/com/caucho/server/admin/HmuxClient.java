/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.server.admin;

import com.caucho.config.ConfigException;
import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterPort;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.ClusterStream;
import com.caucho.server.cluster.ServerPool;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;

public class HmuxClient
{
  private static final L10N L = new L10N(HmuxClient.class);

  private final ServerPool _pool;

  private ExtSerializerFactory _extFactory;

  public HmuxClient(String serverId)
  {
    _pool = findClient(serverId);

    if (_pool == null)
      throw new ConfigException(L.l("'{0}' is an unknown server.",
                                    serverId));
  }

  public HmuxClient(String host,
		    int port)
  {
    _pool = createClient(host, port);
  }

  //
  // ServerPool creation utilities
  //

  /**
   * Finds the ClusterServer in the current Resin instances by its
   * server-id and returns its ServerPool.
   */
  private ServerPool findClient(String serverId)
  {
    Resin resin = Resin.getCurrent();

    if (resin == null)
      throw new ConfigException(L.l("No active Resin is available."));

    ClusterServer server = resin.findClusterServer(serverId);

    if (server == null)
      throw new ConfigException(L.l("'{0}' is an unknown server.",
                                    serverId));

    return server.getServerPool();
  }

  /**
   * Creates a new ServerPool to a Resin server by the address and port.
   */
  private ServerPool createClient(String address, int port)
  {
    try {
      ServerPool conn = new ServerPool("hmux",
                                       address + ":" + port,
                                       address + ":" + port,
				       address, port, false);

      conn.init();

      return conn;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _pool + "]";
  }
}

