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

package com.caucho.v5.http.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.env.meter.MeterService;
import com.caucho.v5.env.meter.TotalMeter;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.dispatch.InvocationManager;
import com.caucho.v5.jmx.server.EnvironmentMXBean;
import com.caucho.v5.management.server.AbstractEmitterObject;
import com.caucho.v5.management.server.CacheItem;
import com.caucho.v5.management.server.ClusterMXBean;
import com.caucho.v5.management.server.ClusterServerMXBean;
import com.caucho.v5.management.server.PortMXBean;
import com.caucho.v5.management.server.ServerMXBean;
import com.caucho.v5.management.server.TcpConnectionInfo;
import com.caucho.v5.management.server.ThreadPoolMXBean;
import com.caucho.v5.network.listen.ConnectionTcp;
import com.caucho.v5.network.listen.PortTcp;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.util.CurrentTime;

public class HttpAdmin extends AbstractEmitterObject
  implements ServerMXBean
{
  private static final String BYTES_PROBE = "Caucho|Http|Request Bytes";
  private TotalMeter _httpBytesProbe;

  //private ServerBase _server;
  private HttpContainerServlet _httpContainer;
  private SystemManager _systemManager;

  public HttpAdmin(HttpContainerServlet httpContainer)
  {
    _systemManager = SystemManager.getCurrent();
    _httpContainer = httpContainer;

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
    return getSelfServerImpl().getId();
  }

  @Override
  public int getServerIndex()
  {
    return getSelfServerImpl().getServerIndex();
  }
  
  private ServerBartender getSelfServerImpl()
  {
    return BartenderSystem.getCurrentSelfServer();
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
    // XXX: return _server.getSelfServer().getAdmin();
    
    return null;
  }

  /**
   * Returns the cluster owning this server
   */
  @Override
  public ClusterMXBean getCluster()
  {
    // XXX: return _server.getCluster().getAdmin();
    return null;
  }

  @Override
  public EnvironmentMXBean getEnvironment()
  {
    // XXX: possible GC/classloader issues
    // return _server.getEnvironmentAdmin();
    return null;
  }

  /**
   * Returns the array of ports.
   */
  @Override
  public PortMXBean []getPorts()
  {
    Collection<PortTcp> portList = getNetworkListeners();

    PortMXBean []ports = new PortMXBean[portList.size()];

    int i = 0;
    for (PortTcp port : portList) {
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
    return ServerBase.getCurrent().getAdmin().getThreadPoolAdmin();
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
    return _httpContainer.isDevelopmentModeErrorPage();
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
    return _httpContainer.getServerHeader();
  }

  @Override
  public boolean isSelectManagerEnabled()
  {
    return false;
  }

  @Override
  public long getShutdownWaitMax()
  {
    return _httpContainer.getShutdownWaitMax();
  }

  @Override
  public String getStage()
  {
    return _httpContainer.getClusterName();
  }

  @Override
  public int getUrlLengthMax()
  {
    return _httpContainer.getUrlLengthMax();
  }

  @Override
  public int getHeaderSizeMax()
  {
    return _httpContainer.getHeaderSizeMax();
  }

  @Override
  public int getHeaderCountMax()
  {
    return _httpContainer.getHeaderCountMax();
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
    return _httpContainer.getState();
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
    return new Date(_httpContainer.getStartTime());
  }

  /**
   * Returns the time since the last start time.
   */
  @Override
  public long getUptime()
  {
    return CurrentTime.getCurrentTime() - _httpContainer.getStartTime();
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

    for (PortTcp port : getNetworkListeners()) {
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

    for (PortTcp port : getNetworkListeners()) {
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

    for (PortTcp port : getNetworkListeners())
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
   * {@link com.caucho.v5.vfs.ClientDisconnectException}.
   */
  @Override
  public long getClientDisconnectCountTotal()
  {
    long lifetimeClientDisconnectCount = 0;

    for (PortTcp port : getNetworkListeners())
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
    return _httpContainer.getInvocationManager().getInvocationCacheHitCount();
  }

  /**
   * Returns the invocation cache miss count.
   */
  @Override
  public long getInvocationCacheMissCountTotal()
  {
    return _httpContainer.getInvocationManager().getInvocationCacheMissCount();
  }

  /**
   * Returns the invocation cache miss count.
   */
  @Override
  public long getSendfileCountTotal()
  {
    return _httpContainer.getSendfileCount();
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
    return 0;
  }

  /**
   * Returns the cache stuff.
   */
  public ArrayList<CacheItem> getCacheStatistics()
  {
    InvocationManager server = _httpContainer.getInvocationManager();
    
    ArrayList<InvocationServlet> invocationList = server.getInvocations();

    if (invocationList == null)
      return null;

    HashMap<String,CacheItem> itemMap = new HashMap<String,CacheItem>();

    for (int i = 0; i < invocationList.size(); i++) {
      InvocationServlet inv = (InvocationServlet) invocationList.get(i);

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
    _httpContainer.destroy(ShutdownModeAmp.GRACEFUL);
  }

  /**
   * Finds the ConnectionMXBean for a given thread id
   */
  @Override
  public TcpConnectionInfo findConnectionByThreadId(long threadId)
  {
    ConnectionTcp conn = getListenService().findConnectionByThreadId(threadId);

    if (conn != null)
      return conn.getConnectionInfo();
    else
      return null;
  }
  
  private Collection<PortTcp> getNetworkListeners()
  {
    NetworkSystem listenService
      = _httpContainer.getSystemManager().getSystem(NetworkSystem.class);
  
    return listenService.getPorts();
  }
  
  private NetworkSystem getListenService()
  {
    return _httpContainer.getSystemManager().getSystem(NetworkSystem.class);
  }
}
