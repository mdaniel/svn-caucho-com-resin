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

package com.caucho.server.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.caucho.cloud.network.NetworkListenSystem;
import com.caucho.env.meter.MeterService;
import com.caucho.env.meter.TotalMeter;
import com.caucho.env.service.ResinSystem;
import com.caucho.management.server.*;
import com.caucho.network.listen.TcpPort;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationServer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

public class ServerAdmin extends AbstractEmitterObject
  implements ServerMXBean
{
  private static final String BYTES_PROBE = "Resin|Http|Request Bytes";
  private TotalMeter _httpBytesProbe;

  private ServletService _server;

  ServerAdmin(ServletService server)
  {
    _server = server;

    MeterService.createAverageMeter(BYTES_PROBE, "");

    String name = BYTES_PROBE;

    _httpBytesProbe = (TotalMeter) MeterService.getMeter(name);

    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public String getType()
  {
    return "Server";
  }

  @Override
  public String getId()
  {
    return _server.getServerId();
  }

  @Override
  public int getServerIndex()
  {
    return _server.getServerIndex();
  }

  //
  // Hierarchy
  //

  /**
   * Returns the cluster server owning this server
   */
  @Override
  public ClusterServerMXBean getSelfServer()
  {
    return _server.getSelfServer().getAdmin();
  }

  /**
   * Returns the cluster owning this server
   */
  @Override
  public ClusterMXBean getCluster()
  {
    return _server.getCluster().getAdmin();
  }

  @Override
  public EnvironmentMXBean getEnvironment()
  {
    // XXX: possible GC/classloader issues
    return _server.getEnvironmentAdmin();
  }

  /**
   * Returns the array of ports.
   */
  @Override
  public PortMXBean []getPorts()
  {
    Collection<TcpPort> portList = getNetworkListeners();

    PortMXBean []ports = new PortMXBean[portList.size()];

    int i = 0;
    for (TcpPort port : portList) {
      ports[i++] = port.getAdmin();
    }

    return ports;
  }

  /**
   * Returns the server's thread pool administration
   */
  @Override
  public ThreadPoolMXBean getThreadPool()
  {
    return _server.getResin().getAdmin().getThreadPoolAdmin();
  }

  /**
   * Returns the cluster port
   */
  @Override
  public PortMXBean getClusterPort()
  {
    return null;
  }

  //
  // Configuration attributes
  //

  @Override
  public boolean isBindPortsAfterStart()
  {
    
    ResinSystem resinSystem = _server.getResinSystem();
    NetworkListenSystem listenService 
      = resinSystem.getService(NetworkListenSystem.class);
    
    return listenService.isBindPortsAfterStart();
  }

  /**
   * Returns true if detailed statistics are being kept.
   */
  @Override
  public boolean isDetailedStatistics()
  {
    return false;
  }

  @Override
  public boolean isDevelopmentModeErrorPage()
  {
    return _server.isDevelopmentModeErrorPage();
  }

  @Override
  public long getMemoryFreeMin()
  {
    return 0;
  }

  @Override
  public long getPermGenFreeMin()
  {
    return 0;
  }

  @Override
  public String getServerHeader()
  {
    return _server.getServerHeader();
  }

  @Override
  public boolean isSelectManagerEnabled()
  {
    return false;
  }

  @Override
  public long getShutdownWaitMax()
  {
    return _server.getShutdownWaitMax();
  }

  @Override
  public String getStage()
  {
    return _server.getStage();
  }

  @Override
  public int getUrlLengthMax()
  {
    return _server.getUrlLengthMax();
  }

  @Override
  public int getHeaderSizeMax()
  {
    return _server.getHeaderSizeMax();
  }

  @Override
  public int getHeaderCountMax()
  {
    return _server.getHeaderCountMax();
  }

  //
  // state
  //

  /**
   * The current lifecycle state.
   */
  @Override
  public String getState()
  {
    return _server.getState();
  }

  /**
   * Returns the current time according to the server.
   */
  @Override
  public Date getCurrentTime()
  {
    return new Date(CurrentTime.getCurrentTime());
  }

  /**
   * Returns the last start time.
   */
  @Override
  public Date getStartTime()
  {
    return new Date(_server.getStartTime());
  }

  /**
   * Returns the time since the last start time.
   */
  @Override
  public long getUptime()
  {
    return CurrentTime.getCurrentTime() - _server.getStartTime();
  }

  //
  // statistics
  //

  /**
   * Returns the current number of threads that are servicing requests.
   */
  @Override
  public int getThreadActiveCount()
  {
    int activeThreadCount = -1;

    for (TcpPort port : getNetworkListeners()) {
      if (port.getActiveThreadCount() >= 0) {
        if (activeThreadCount == -1)
          activeThreadCount = 0;

        activeThreadCount += port.getActiveThreadCount();
      }
    }

    return activeThreadCount;
  }

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using a thread to maintain the connection.
   */
  @Override
  public int getThreadKeepaliveCount()
  {
    int keepaliveThreadCount = -1;

    for (TcpPort port : getNetworkListeners()) {
      if (port.getKeepaliveConnectionCount() >= 0) {
        if (keepaliveThreadCount == -1)
          keepaliveThreadCount = 0;

        keepaliveThreadCount += port.getKeepaliveConnectionCount();
      }
    }

    return keepaliveThreadCount;
  }

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using select to maintain the connection.
   */
  @Override
  public int getSelectKeepaliveCount()
  {
    return 0;
  }

  /**
   * Returns the total number of requests serviced by the server
   * since it started.
   */
  @Override
  public long getRequestCountTotal()
  {
    long lifetimeRequestCount = 0;

    for (TcpPort port : getNetworkListeners())
      lifetimeRequestCount += port.getLifetimeRequestCount();

    return lifetimeRequestCount;
  }

  /**
   * Returns the number of requests that have ended up in the keepalive state
   * for this server in its lifetime.
   */
  @Override
  public long getKeepaliveCountTotal()
  {
    return -1;
  }

  /**
   * The total number of connections that have terminated with
   * {@link com.caucho.vfs.ClientDisconnectException}.
   */
  @Override
  public long getClientDisconnectCountTotal()
  {
    long lifetimeClientDisconnectCount = 0;

    for (TcpPort port : getNetworkListeners())
      lifetimeClientDisconnectCount += port.getLifetimeClientDisconnectCount();

    return lifetimeClientDisconnectCount;
  }

  /**
   * Returns the total duration in milliseconds that requests serviced by
   * this server have taken.
   */
  @Override
  public long getRequestTimeTotal()
  {
    return -1;
  }

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have read.
   */
  @Override
  public long getRequestReadBytesTotal()
  {
    return -1;
  }

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have written.
   */
  @Override
  public long getRequestWriteBytesTotal()
  {
    if (_httpBytesProbe != null)
      return (long) _httpBytesProbe.getTotal();
    else
      return 0;
  }

  /**
   * Returns the invocation cache hit count.
   */
  @Override
  public long getInvocationCacheHitCountTotal()
  {
    return _server.getInvocationServer().getInvocationCacheHitCount();
  }

  /**
   * Returns the invocation cache miss count.
   */
  @Override
  public long getInvocationCacheMissCountTotal()
  {
    return _server.getInvocationServer().getInvocationCacheMissCount();
  }

  /**
   * Returns the invocation cache miss count.
   */
  @Override
  public long getSendfileCountTotal()
  {
    return _server.getSendfileCount();
  }

  /**
   * Returns the current total amount of memory available for the JVM, in bytes.
   */
  @Override
  public long getRuntimeMemory()
  {
    if (CurrentTime.isTest())
      return 666;
    else
      return Runtime.getRuntime().totalMemory();
  }

  /**
   * Returns the current free amount of memory available for the JVM, in bytes.
   */
  @Override
  public long getRuntimeMemoryFree()
  {
    if (CurrentTime.isTest())
      return 666;
    else
      return Runtime.getRuntime().freeMemory();
  }

  /**
   * Returns the CPU load average.
   */
  @Override
  public double getCpuLoadAvg()
  {
    try {
      if (CurrentTime.isTest())
        return 0;
      else
        return CauchoSystem.getLoadAvg();
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Returns the cache stuff.
   */
  public ArrayList<CacheItem> getCacheStatistics()
  {
    InvocationServer server = _server.getInvocationServer();
    
    ArrayList<Invocation> invocationList = server.getInvocations();

    if (invocationList == null)
      return null;

    HashMap<String,CacheItem> itemMap = new HashMap<String,CacheItem>();

    for (int i = 0; i < invocationList.size(); i++) {
      Invocation inv = (Invocation) invocationList.get(i);

      String uri = inv.getURI();
      int p = uri.indexOf('?');
      if (p >= 0)
        uri = uri.substring(0, p);

      CacheItem item = itemMap.get(uri);

      if (item == null) {
        item = new CacheItem();
        item.setUrl(uri);

        itemMap.put(uri, item);
      }
    }

    return null;
  }

  //
  // Operations
  //

  /**
   * Restart this Resin server.
   */
  @Override
  public void restart()
  {
    _server.destroy();
  }

  /**
   * Finds the ConnectionMXBean for a given thread id
   */
  @Override
  public TcpConnectionInfo findConnectionByThreadId(long threadId)
  {
    TcpSocketLink conn = getListenService().findConnectionByThreadId(threadId);

    if (conn != null)
      return conn.getConnectionInfo();
    else
      return null;
  }
  
  private Collection<TcpPort> getNetworkListeners()
  {
    NetworkListenSystem listenService
      = _server.getResinSystem().getService(NetworkListenSystem.class);
  
    return listenService.getListeners();
  }
  
  private NetworkListenSystem getListenService()
  {
    return _server.getResinSystem().getService(NetworkListenSystem.class);
  }
}
