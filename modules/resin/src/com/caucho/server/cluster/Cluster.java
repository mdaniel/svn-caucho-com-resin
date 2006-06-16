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

package com.caucho.server.cluster;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.ObjectName;

import com.caucho.util.*;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.config.types.Period;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;

import com.caucho.jmx.Jmx;

import com.caucho.mbeans.server.ClusterMBean;

/**
 * Defines a set of clustered servers.
 */
public class Cluster implements EnvironmentListener {
  static protected final L10N L = new L10N(ClusterGroup.class);
  static protected final Logger log = Log.open(Cluster.class);

  static protected final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  static protected final EnvironmentLocal<Cluster> _clusterLocal
    = new EnvironmentLocal<Cluster>("caucho.cluster");

  private String _id = "";

  private String _serverId = "";

  private ObjectName _objectName;

  private ArrayList<ClusterServer> _serverList
    = new ArrayList<ClusterServer>();

  private ClusterServer[] _serverArray = new ClusterServer[0];

  private ClusterGroup _group;

  private StoreManager _clusterStore;

  private long _clientMaxIdleTime = 30000L;
  private long _clientFailRecoverTime = 15000L;
  private long _clientSlowStartTime = 60000L;
  
  private long _clientReadTimeout = 60000L;
  private long _clientWriteTimeout = 60000L;
  private long _clientConnectTimeout = 5000L;

  private String _ref;

  private volatile boolean _isClosed;

  public Cluster()
  {
    Environment.addEnvironmentListener(this);

    _group = ClusterGroup.createClusterGroup();
    _group.addCluster(this);
  }

  /**
   * Returns the currently active local cluster.
   */
  public static Cluster getLocal()
  {
    Cluster cluster = _clusterLocal.get();

    return cluster;
  }

  /**
   * Returns the currently active local cluster.
   */
  public static Cluster getCluster(ClassLoader loader)
  {
    Cluster cluster = _clusterLocal.get(loader);

    return cluster;
  }

  /**
   * Sets the cluster id.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Gets the cluster id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the cluster ref.
   */
  public void setClusterRef(String ref)
  {
    _ref = ref;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String id)
  {
    for (int i = _serverList.size() - 1; i >= 0; i--) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getId().equals(id))
        return server;
    }

    return null;
  }

  /**
   * Adds a new server to the cluster.
   */
  void addServer(ClusterServer server)
    throws ConfigException
  {
    ClusterServer oldServer = findServer(server.getId());

    if (oldServer != null)
      log.warning(L.l("duplicate <srun> with server-id='{0}'",
                      server.getId()));

    _serverList.add(server);
    _serverArray = new ClusterServer[_serverList.size()];
    _serverList.toArray(_serverArray);
  }
  /**
   * Adds a srun server.
   */
  public void addPort(ClusterPort port)
    throws Exception
  {
    ClusterServer server = new ClusterServer();
    server.setCluster(this);
    server.setPort(port);

    if (port.getIndex() >= 0 &&
	port.getIndex() != _serverList.size() + 1) {
      log.config(L.l("srun index '{0}' for port 'id={1}' does not match expected cluster index '{2}'",
		     port.getIndex(),
		     port.getServerId(),
		     _serverList.size() + 1));
    }

    port.setIndex(_serverList.size() + 1);
    
    server.init();
    
    addServer(server);
  }

  /**
   * Adds a srun server.
   */
  public void addSrun(ClusterPort port)
    throws Exception
  {
    addPort(port);
  }

  /**
   * Adds a srun server.
   */
  public ClusterClient findClient(String host, int port)
  {
    for (int i = _serverList.size() - 1; i >= 0; i--) {
      ClusterServer server = _serverList.get(i);
      ClusterPort clusterPort = server.getClusterPort();

      if (host.equals(clusterPort.getHost()) &&
	  port == clusterPort.getPort())
	return server.getClient();
    }

    return null;
  }

  /**
   * Returns the cluster store.
   */
  public StoreManager getStore()
  {
    return _clusterStore;
  }

  /**
   * Sets the cluster store.
   */
  void setStore(StoreManager store)
  {
    _clusterStore = store;
  }

  /**
   * Sets the max-idle time.
   */
  public void setClientMaxIdleTime(Period period)
  {
    _clientMaxIdleTime = period.getPeriod();
  }

  /**
   * Gets the live time.
   */
  public long getClientMaxIdleTime()
  {
    return _clientMaxIdleTime;
  }

  /**
   * Sets the live time.
   *
   * @deprecated
   */
  public void setClientLiveTime(Period period)
  {
    setClientMaxIdleTime(period);
  }

  /**
   * Sets the client connection fail-recover time.
   */
  public void setClientFailRecoverTime(Period period)
  {
    _clientFailRecoverTime = period.getPeriod();
  }

  /**
   * Gets the client fail-recover time.
   */
  public long getClientFailRecoverTime()
  {
    return _clientFailRecoverTime;
  }

  /**
   * Sets the dead time.
   *
   * @deprecated
   */
  public void setClientDeadTime(Period period)
  {
    setClientFailRecoverTime(period);
  }

  /**
   * Sets the client slow-start time.
   */
  public void setClientSlowStartTime(Period period)
  {
    _clientSlowStartTime = period.getPeriod();
  }

  /**
   * Gets the client slow-start time.
   */
  public long getClientSlowStartTime()
  {
    return _clientSlowStartTime;
  }

  /**
   * Sets the connect timeout.
   */
  public void setClientConnectTimeout(Period period)
  {
    _clientConnectTimeout = period.getPeriod();
  }

  /**
   * Gets the connect timeout.
   */
  public long getClientConnectTimeout()
  {
    return _clientConnectTimeout;
  }

  /**
   * Sets the read timeout.
   */
  public void setClientReadTimeout(Period period)
  {
    _clientReadTimeout = period.getPeriod();
  }

  /**
   * Gets the read timeout.
   */
  public long getClientReadTimeout()
  {
    return _clientReadTimeout;
  }

  /**
   * Sets the write timeout.
   */
  public void setClientWriteTimeout(Period period)
  {
    _clientWriteTimeout = period.getPeriod();
  }

  /**
   * Gets the write timeout.
   */
  public long getClientWriteTimeout()
  {
    return _clientWriteTimeout;
  }

  public StoreManager createJdbcStore()
    throws ConfigException
  {
    if (getStore() != null)
      throw new ConfigException(L.l("multiple jdbc stores are not allowed in a cluster."));

    StoreManager store = null;

    try {
      Class cl = Class.forName("com.caucho.server.cluster.JdbcStore");

      store = (StoreManager) cl.newInstance();

      store.setCluster(this);

      setStore(store);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (store == null)
      throw new ConfigException(L.l("'jdbc' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));

    return store;
  }

  public StoreManager createPrivateFileStore()
    throws ConfigException
  {
    StoreManager store = createFileStore();

    setStore(null);

    return store;
  }

  public StoreManager createFileStore()
    throws ConfigException
  {
    if (getStore() != null)
      throw new ConfigException(L.l("multiple file stores are not allowed in a cluster."));

    StoreManager store = null;

    try {
      Class cl = Class.forName("com.caucho.server.cluster.FileStore");

      store = (StoreManager) cl.newInstance();

      store.setCluster(this);

      setStore(store);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (store == null)
      throw new ConfigException(L.l("'file' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));

    return store;
  }

  public StoreManager createClusterStore()
    throws ConfigException
  {
    if (getStore() != null)
      throw new ConfigException(L.l("multiple cluster stores are not allowed in a cluster."));

    StoreManager store = null;

    try {
      Class cl = Class.forName("com.caucho.server.cluster.ClusterStore");

      store = (StoreManager) cl.newInstance();

      store.setCluster(this);

      setStore(store);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (store == null)
      throw new ConfigException(L.l("'cluster' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));

    return store;
  }

  /**
   * Initializes the cluster.
   */
  public void init()
    throws ConfigException
  {
    ClusterContainer container = ClusterContainer.create();

    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server == null)
        continue;

      ClusterPort port = server.getClusterPort();

      if (port.getReadTimeout() < getClientMaxIdleTime()) {
        throw new ConfigException(L.l("client-max-idle-time '{0}s' must be less than the read-timeout '{1}s'.",
                                      getClientMaxIdleTime() / 1000L,
                                      port.getReadTimeout() / 1000L));
      }
    }

    String serverId = _serverIdLocal.get();

    if (serverId == null)
      serverId = "";

    if (_ref != null) {
      Cluster cluster = container.findCluster(_ref);

      if (cluster == null)
        throw new ConfigException(L.l("'{0}' is an unknown cluster-ref.",
                                      _ref));

      _clusterLocal.set(cluster);
    }
    else {
      container.addCluster(this);

      ClusterServer self = findServer(serverId);

      if (self != null)
        _clusterLocal.set(this);
      else if (_clusterLocal.get() == null && _serverList.size() == 0) {
        // if it's the empty cluster, add it
        _clusterLocal.set(this);
      }
    }

    try {
      String name = _id;

      if (name == null || name.equals(""))
        name = "default";

      ObjectName objectName = Jmx.getObjectName("type=Cluster,name=" + name);

      ClusterAdmin admin = new ClusterAdmin(this);

      Jmx.register(admin, objectName);

      _objectName = objectName;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns the server id.
   */
  public static String getServerId()
  {
    return _serverIdLocal.get();
  }

  /**
   * Returns the JMX object name.
   */
  public String getObjectName()
  {
    return _objectName == null ? null : _objectName.toString();
  }

  /**
   * Returns the server corresponding to the current server-id.
   */
  public ClusterServer getSelfServer()
  {
    _serverId = _serverIdLocal.get();

    return getServer(_serverId);
  }

  /**
   * Returns the server list.
   */
  public ClusterServer []getServerList()
  {
    return _serverArray;
  }

  /**
   * Returns the client name list.
   */
  public String []getClientObjectNames()
  {
    String []objectNames = new String[_serverList.size()];

    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer client = _serverList.get(i);

      objectNames[i] = client.getObjectName();
    }

    return objectNames;
  }

  /**
   * Returns the server in the cluster with the given server-id.
   */
  public ClusterServer getServer(String serverId)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getId().equals(serverId))
        return server;
    }

    return null;
  }

  /**
   * Returns the server with the matching index.
   */
  public ClusterServer getServer(int index)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getIndex() == index)
        return server;
    }

    return null;
  }

  /**
   * Returns the matching ports.
   */
  public ArrayList<ClusterPort> getServerPorts(String serverId)
  {
    ArrayList<ClusterPort> ports = new ArrayList<ClusterPort>();

    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null) {
        ClusterPort port = server.getClusterPort();

        if (port.getServerId().equals(serverId))
          ports.add(port);
      }
    }

    return ports;
  }

  /**
   * Handles the case where a class loader has completed initialization
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      if (_clusterStore != null)
        _clusterStore.start();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    try {
      close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Closes the cluster.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
        return;

      _isClosed = true;
    }

    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      try {
        if (server != null)
          server.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    return "Cluster[" + _id + "]";
  }
}
