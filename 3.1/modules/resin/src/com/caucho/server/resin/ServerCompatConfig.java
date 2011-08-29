/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.SchemaBean;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.port.Port;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Compatiblity configuration for Resin 3.0-style configuration.
 */
public class ServerCompatConfig implements SchemaBean {
  private static final L10N L = new L10N(ServerCompatConfig.class);
  private static final Logger log
    = Logger.getLogger(ServerCompatConfig.class.getName());

  private final Resin _resin;

  private ArrayList<HttpCompatConfig> _httpList
    = new ArrayList<HttpCompatConfig>();

  private ContainerProgram _program
    = new ContainerProgram();

  /**
   * Creates a new resin server.
   */
  public ServerCompatConfig(Resin resin)
  {
    if (resin == null)
      throw new NullPointerException();
    
    _resin = resin;
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/resin/server.rnc";
  }

  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Creates a http compat.
   */

  public HttpCompatConfig createHttp()
  {
    HttpCompatConfig http = new HttpCompatConfig();

    return http;
  }

  /**
   * Creates a cluster compat.
   */
  public ClusterCompatConfig createCluster()
  {
    return new ClusterCompatConfig(_resin);
  }

  @PostConstruct
  public void init()
  {
    try {
      String serverId = _resin.getServerId();
    
      ClusterServer clusterServer = _resin.findClusterServer(serverId);

      if (clusterServer != null) {
      }
      else {
	if (_resin.getClusterList().size() > 0 || ! "".equals(serverId)) {
	  log.warning(L.l("-server '{0}' does not match any defined servers",
			  serverId));
	}

	Cluster cluster = _resin.createCluster();
	_resin.addCluster(cluster);

	clusterServer = cluster.createServer();
	
	clusterServer.setPort(0);

	cluster.addServer(clusterServer);
      }

      _program.configure(clusterServer.getCluster());
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public class HttpCompatConfig {
    private String _id = "";

    private ContainerProgram _program
      = new ContainerProgram();

    HttpCompatConfig()
    {
    }

    public void setId(String id)
    {
      _id = id;
    }

    public void setServerId(String id)
    {
      setId(id);
    }

    public void addBuilderProgram(ConfigProgram program)
    {
      _program.addProgram(program);
    }

    @PostConstruct
    public void init()
      throws Throwable
    {
      ClusterServer server = _resin.findClusterServer(_id);

      if (server == null) {
	Cluster cluster = _resin.findCluster("");

	if (cluster == null) {
	  cluster = _resin.createCluster();
	  _resin.addCluster(cluster);
	}

	server = cluster.createServer();
	server.setId(_id);
	server.getClusterPort().setPort(0);
	cluster.addServer(server);
      }

      Port http = server.createHttp();
      _program.configure(http);
    }
  }
}
