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

package com.caucho.server.resin;

import java.net.*;

import javax.annotation.*;

import com.caucho.util.*;

import com.caucho.server.cluster.*;
import com.caucho.vfs.*;
import com.caucho.server.vfs.*;

/**
 * Compatiblity configuration for Resin 3.0-style configuration.
 */
public class ClusterCompatConfig {
  private static final L10N L = new L10N(ClusterCompatConfig.class);

  private final Resin _resin;
  
  private String _id = "";
  private Cluster _cluster;

  /**
   * Creates a new cluster compat
   */
  public ClusterCompatConfig(Resin resin)
  {
    _resin = resin;
  }

  public void setId(String id)
  {
    _id = id;
  }

  Cluster getCluster()
  {
    if (_cluster != null)
      return _cluster;
    
    _cluster = _resin.findCluster(_id);

    if (_cluster == null) {
      try {
	_cluster = _resin.createCluster();
	_cluster.setId(_id);
	_resin.addCluster(_cluster);
      } catch (Throwable e) {
	throw new RuntimeException(e);
      }
    }

    return _cluster;
  }

  public SrunCompatConfig createSrun()
  {
    return new SrunCompatConfig();
  }

  public class SrunCompatConfig {
    private String _id = "";
    private ClusterServer _server;

    SrunCompatConfig()
    {
      Cluster cluster = getCluster();
    }

    public void setId(String id)
    {
      _id = id;
    }

    public void setServerId(String id)
    {
      setId(id);
    }

    public void setHost(String host)
      throws UnknownHostException
    {
      getClusterPort().setAddress(host);
    }

    public void setPort(int port)
    {
      getClusterPort().setPort(port);
    }

    public void setJsseSsl(JsseSSLFactory ssl)
    {
      getClusterPort().setJsseSsl(ssl);
    }

    public void setIndex(int index)
    {
      // getClusterServer().setIndex(index);
    }

    public void setBackup(boolean backup)
    {
      // getClusterServer().setBackup(index);
    }

    private ClusterPort getClusterPort()
    {
      return getClusterServer().getClusterPort();
    }

    private ClusterServer getClusterServer()
    {
      if (_server == null)
	_server = _cluster.findServer(_id);
      
      if (_server == null) {
	try {
	  _server = _cluster.createServer();
	  _server.setId(_id);
	  _cluster.addServer(_server);
	} catch (Throwable e) {
	  throw new RuntimeException(e);
	}
      }

      return _server;
    }

    @PostConstruct
    public void init()
      throws Exception
    {
      _server.init();
    }
  }
}
