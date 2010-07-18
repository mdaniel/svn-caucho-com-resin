/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.util.Collection;
import java.util.Date;

import com.caucho.env.sample.ProbeManager;
import com.caucho.env.sample.TotalProbe;
import com.caucho.management.server.AbstractEmitterObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.management.server.TcpConnectionMXBean;
import com.caucho.management.server.ThreadPoolMXBean;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;

public class ServerAdmin extends AbstractEmitterObject
  implements ServerMXBean
{
  private static final String BYTES_PROBE = "Resin|Request|Http Request Bytes";
  private TotalProbe _httpBytesProbe;

  private Server _server;

  ServerAdmin(Server server)
  {
    _server = server;

    ProbeManager.createAverageProbe(BYTES_PROBE, "");

    String name = BYTES_PROBE;

    _httpBytesProbe = (TotalProbe) ProbeManager.getProbe(name);

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
    return _server.getCluster().getData(ClusterMXBean.class);
  }

  public EnvironmentMXBean getEnvironment()
  {
    // XXX: possible GC/classloader issues
    return _server.getEnvironmentAdmin();
  }

  /**
   * Returns the array of ports.
   */
  public PortMXBean []getPorts()
  {
    Collection<SocketLinkListener> portList = _server.getPorts();

    PortMXBean []ports = new PortMXBean[portList.size()];

    int i = 0;
    for (SocketLinkListener port : portList) {
      ports[i++] = port.getAdmin();
    }

    return ports;
  }

  /**
   * Returns the server's thread pool administration
   */
  public ThreadPoolMXBean getThreadPool()
  {
    return _server.getResin().getThreadPoolAdmin();
  }

  /**
   * Returns the cluster port
   */
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
    return _server.getListenService().isBindPortsAfterStart();
  }

  /**
   * Returns true if detailed statistics are being kept.
   */
  public boolean isDetailedStatistics()
  {
    return false;
  }

  public boolean isDevelopmentModeErrorPage()
  {
    return _server.isDevelopmentModeErrorPage();
  }

  public long getMemoryFreeMin()
  {
    return _server.getMemoryFreeMin();
  }

  public long getPermGenFreeMin()
  {
    return _server.getPermGenFreeMin();
  }

  public String getServerHeader()
  {
    return _server.getServerHeader();
  }

  public boolean isSelectManagerEnabled()
  {
    return _server.isSelectManagerEnabled();
  }

  public long getShutdownWaitMax()
  {
    return _server.getShutdownWaitMax();
  }

  public String getStage()
  {
    return _server.getStage();
  }

  public int getUrlLengthMax()
  {
    return _server.getUrlLengthMax();
  }

  //
  // state
  //

  /**
   * The current lifecycle state.
   */
  public String getState()
  {
    return _server.getState();
  }

  /**
   * Returns the current time according to the server.
   */
  public Date getCurrentTime()
  {
    return new Date(Alarm.getExactTime());
  }

  /**
   * Returns the last start time.
   */
  public Date getStartTime()
  {
    return new Date(_server.getStartTime());
  }

  /**
   * Returns the time since the last start time.
   */
  public long getUptime()
  {
    return Alarm.getExactTime() - _server.getStartTime();
  }

  //
  // statistics
  //

  /**
   * Returns the current number of threads that are servicing requests.
   */
  public int getThreadActiveCount()
  {
    int activeThreadCount = -1;

    for (SocketLinkListener port : _server.getPorts()) {
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
  public int getThreadKeepaliveCount()
  {
    int keepaliveThreadCount = -1;

    for (SocketLinkListener port : _server.getPorts()) {
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
  public int getSelectKeepaliveCount()
  {
    return _server.getKeepaliveSelectCount();
  }

  /**
   * Returns the total number of requests serviced by the server
   * since it started.
   */
  public long getRequestCountTotal()
  {
    long lifetimeRequestCount = 0;

    for (SocketLinkListener port : _server.getPorts())
      lifetimeRequestCount += port.getLifetimeRequestCount();

    return lifetimeRequestCount;
  }

  /**
   * Returns the number of requests that have ended up in the keepalive state
   * for this server in its lifetime.
   */
  public long getKeepaliveCountTotal()
  {
    return -1;
  }

  /**
   * The total number of connections that have terminated with
   * {@link com.caucho.vfs.ClientDisconnectException}.
   */
  public long getClientDisconnectCountTotal()
  {
    long lifetimeClientDisconnectCount = 0;

    for (SocketLinkListener port : _server.getPorts())
      lifetimeClientDisconnectCount += port.getLifetimeClientDisconnectCount();

    return lifetimeClientDisconnectCount;
  }

  /**
   * Returns the total duration in milliseconds that requests serviced by
   * this server have taken.
   */
  public long getRequestTimeTotal()
  {
    return -1;
  }

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have read.
   */
  public long getRequestReadBytesTotal()
  {
    return -1;
  }

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have written.
   */
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
  public long getInvocationCacheHitCountTotal()
  {
    return _server.getInvocationCacheHitCount();
  }

  /**
   * Returns the invocation cache miss count.
   */
  public long getInvocationCacheMissCountTotal()
  {
    return _server.getInvocationCacheMissCount();
  }

  /**
   * Returns the current total amount of memory available for the JVM, in bytes.
   */
  public long getRuntimeMemory()
  {
    if (Alarm.isTest())
      return 666;
    else
      return Runtime.getRuntime().totalMemory();
  }

  /**
   * Returns the current free amount of memory available for the JVM, in bytes.
   */
  public long getRuntimeMemoryFree()
  {
    if (Alarm.isTest())
      return 666;
    else
      return Runtime.getRuntime().freeMemory();
  }

  /**
   * Returns the CPU load average.
   */
  public double getCpuLoadAvg()
  {
    try {
      if (Alarm.isTest())
        return 0;
      else
        return CauchoSystem.getLoadAvg();
    } catch (Exception e) {
      return 0;
    }
  }

  //
  // Operations
  //

  /**
   * Restart this Resin server.
   */
  public void restart()
  {
    _server.destroy();
  }

  /**
   * Finds the ConnectionMXBean for a given thread id
   */
  public TcpConnectionMXBean findConnectionByThreadId(long threadId)
  {
    TcpSocketLink conn = _server.findConnectionByThreadId(threadId);

    if (conn != null)
      return conn.getAdmin();
    else
      return null;
  }
}
