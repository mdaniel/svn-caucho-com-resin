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

package com.caucho.v5.ramp.embed;

import com.caucho.v5.baratine.ServiceServer;
import com.caucho.v5.cli.server.BootConfigParser;
import com.caucho.v5.server.config.ClusterConfigBoot;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.RootConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.server.container.ServerBuilderBaratine;
import com.caucho.v5.server.main.ArgsServerBaratine;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.VfsOld;

public class ServerBaratineEmbedBuilder implements ServiceServer.Builder
{
  private static final L10N L = new L10N(ServerBaratineEmbedBuilder.class);

  private ArgsServerBaratine _args;
  private int _port = -1;
  private RootConfigBoot _rootConfig;
  private boolean _isRemoveData;
  private boolean _isClient = true;
  private String _podName;

  public ServerBaratineEmbedBuilder()
  {
    _args = ArgsServerBaratine.defaultEmbed();
    //_rootConfig = new RootConfigBoot();
  }

  @Override
  public ServerBaratineEmbedBuilder root(String path)
  {
    _args.setRootDirectory(VfsOld.lookup(path));

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder cluster(String cluster)
  {
    _args.setClusterId(cluster);

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder client(boolean isClient)
  {
    _isClient = isClient;

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder data(String path)
  {
    _args.setDataDirectory(VfsOld.lookup(path));

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder seed(String address, int port)
  {
    _args.addSeedServer(address, port, "cluster");

    if (_port <= 0) {
      _port = 0;
    }

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder conf(String path)
  {
    _args.setConfigPath(VfsOld.lookup(path));

    // baratine/8805
    if (_port <= 0) {
      _port = 0;
    }

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder port(int port)
  {
    _port = port;
    
    _args.setServerPort(port);

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder removeData(boolean isRemove)
  {
    _isRemoveData = isRemove;

    return this;
  }

  @Override
  public ServerBaratineEmbedBuilder podName(String podName)
  {
    _podName = podName;

    return this;
  }

  @Override
  public ServiceServer build()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      SystemManager system = new SystemManager("baratine-embed");
      
      if (_isClient) {
        _args.setClient(true);
      }

      BootConfigParser parser = new BootConfigParser();

      ConfigBoot boot = parser.parseBoot(_args, system);

      _rootConfig = boot.getRoot();

      String clusterId = _args.getClusterId();

      if (clusterId == null) {
        clusterId = "cluster";
      }

      ClusterConfigBoot cluster = _rootConfig.findCluster(clusterId);

      if (cluster == null) {
        cluster = _rootConfig.createDynamicCluster(clusterId);

        if (cluster == null) {
          throw new IllegalStateException(L.l("No cluster '{0}' is available",
                                              clusterId));
        }
      }

      //ServerConfigBoot serverConfig = cluster.createDynamicServer(_port);
      /*
    if (_port != null) {
      serverConfig.setPort(_port);
    }
       */
      ServerConfigBoot serverConfig = null;
      
      if (serverConfig == null && _port >= 0) {
        serverConfig = boot.findServer(_args);
      }
      
      if (serverConfig == null) {
        serverConfig = cluster.createDynamicServer(_port);
      }

      serverConfig.setRemoveDataOnStart(_isRemoveData);

      ServerBuilderBaratine builder = new ServerBuilderBaratine(_args.config()); //, serverConfig);

      builder.setEmbedded(true);

      ServerBase server = null;//builder.build();

      //return new ServerBaratineEmbedImpl(server, _isClient, _podName);
      return null;
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
}
