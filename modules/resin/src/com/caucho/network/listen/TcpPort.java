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

package com.caucho.network.listen;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Period;
import com.caucho.env.meter.ActiveMeter;
import com.caucho.env.meter.CountMeter;
import com.caucho.env.meter.MeterService;
import com.caucho.env.thread.ThreadPool;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.TcpConnectionInfo;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeRingDual;
import com.caucho.util.Friend;
import com.caucho.util.L10N;
import com.caucho.vfs.JsseSSLFactory;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SSLFactory;
import com.caucho.vfs.net.NetworkSystem;

/**
 * Represents a protocol connection.
 */
@Configurable
public class TcpPort
{
  private static final L10N L = new L10N(TcpPort.class);

  private static final Logger log
    = Logger.getLogger(TcpPort.class.getName());

  private static final int ACCEPT_IDLE_MIN = 4;
  private static final int ACCEPT_IDLE_MAX = 64;

  private static final int ACCEPT_THROTTLE_LIMIT = 1024;
  private static final long ACCEPT_THROTTLE_SLEEP_TIME = 0;

  private static final int KEEPALIVE_MAX = 65536;

  private static final CountMeter _throttleDisconnectMeter
    = MeterService.createCountMeter("Resin|Port|Throttle Disconnect Count");

  private static final CountMeter _keepaliveMeter
    = MeterService.createCountMeter("Resin|Port|Keepalive Count");

  private static final ActiveMeter _keepaliveThreadMeter
    = MeterService.createActiveMeter("Resin|Port|Keepalive Thread");

  private static final ActiveMeter _suspendMeter
    = MeterService.createActiveMeter("Resin|Port|Request Suspend");

  private final AtomicInteger _connectionCount = new AtomicInteger();

  // started at 128, but that seems wasteful since the active threads
  // themselves are buffering the free connections
  private FreeRingDual<TcpSocketLink> _idleConn
    = new FreeRingDual<TcpSocketLink>(256, 2 * 1024);

  // The owning server
  // private ProtocolDispatchServer _server;

  private ThreadPool _threadPool = ThreadPool.getThreadPool();

  private SocketLinkThreadLauncher _launcher;

  private ClassLoader _classLoader
    = Thread.currentThread().getContextClassLoader();

  // The id
  private String _serverId = "";

  // The address
  private String _address;
  // The port
  private int _port = -1;

  // URL for debugging
  private String _url;

  // The protocol
  private Protocol _protocol;

  // The SSL factory, if any
  private SSLFactory _sslFactory;

  // Secure override for load-balancers/proxies
  private boolean _isSecure;

  private InetAddress _socketAddress;

  private int _acceptListenBacklog = 4000;

  private int _connectionMax = 1024 * 1024;

  private int _keepaliveMax = -1;

  private long _keepaliveTimeMax = 10 * 60 * 1000L;
  private long _keepaliveTimeout = 120 * 1000L;

  private boolean _isKeepaliveAsyncEnable = true;
  private long _keepaliveSelectThreadTimeout = 1000;

  // default timeout
  private long _socketTimeout = 120 * 1000L;

  private long _suspendReaperTimeout = 60000L;
  private long _suspendTimeMax = 600 * 1000L;
  // after for 120s start checking for EOF on comet requests
  private long _suspendCloseTimeMax = 120 * 1000L;

  private long _requestTimeout = -1;

  private boolean _isTcpNoDelay = true;
  private boolean _isTcpKeepalive;
  private boolean _isTcpCork;

  private boolean _isEnableJni = true;

  // The virtual host name
  private String _virtualHost;

  private final TcpPortAdmin _admin = new TcpPortAdmin(this);

  // the server socket
  private QServerSocket _serverSocket;

  // the throttle
  private Throttle _throttle;

  // the selection manager
  private AbstractSelectManager _selectManager;

  // active set of all connections
  private ConcurrentHashMap<TcpSocketLink,TcpSocketLink> _activeConnectionSet
    = new ConcurrentHashMap<TcpSocketLink,TcpSocketLink>();

  private final AtomicInteger _activeConnectionCount = new AtomicInteger();

  // server push (comet) suspend set
  private Set<TcpSocketLink> _suspendConnectionSet
    = Collections.synchronizedSet(new HashSet<TcpSocketLink>());

  // active requests that are closing after the request like an access-log
  // but should not trigger a new thread launch.
  private final AtomicInteger _shutdownRequestCount = new AtomicInteger();

  // reaper alarm for timed out comet requests
  private Alarm _suspendAlarm;

  // statistics

  private final AtomicLong _lifetimeRequestCount = new AtomicLong();
  private final AtomicLong _lifetimeKeepaliveCount = new AtomicLong();
  private final AtomicLong _lifetimeKeepaliveSelectCount = new AtomicLong();
  private final AtomicLong _lifetimeClientDisconnectCount = new AtomicLong();
  private final AtomicLong _lifetimeRequestTime = new AtomicLong();
  private final AtomicLong _lifetimeReadBytes = new AtomicLong();
  private final AtomicLong _lifetimeWriteBytes = new AtomicLong();
  private final AtomicLong _lifetimeThrottleDisconnectCount = new AtomicLong();

  // total keepalive
  private AtomicInteger _keepaliveAllocateCount = new AtomicInteger();
  // thread-based
  private AtomicInteger _keepaliveThreadCount = new AtomicInteger();
  // True if the port has been bound
  private final AtomicBoolean _isBind = new AtomicBoolean();
  private final AtomicBoolean _isPostBind = new AtomicBoolean();

  // The port lifecycle
  private final Lifecycle _lifecycle = new Lifecycle();

  public TcpPort()
  {
    if (CauchoSystem.is64Bit()) {
      // on 64-bit machines we can use more threads before parking in nio
      _keepaliveSelectThreadTimeout = 60000;
    }

    _launcher = new SocketLinkThreadLauncher(this);

    if (CurrentTime.isTest()) {
      _launcher.setIdleMin(2);
      _launcher.setIdleMax(ACCEPT_IDLE_MAX);
    }
    else {
      _launcher.setIdleMin(ACCEPT_IDLE_MIN);
      _launcher.setIdleMax(ACCEPT_IDLE_MAX);
    }

    _launcher.setThrottleLimit(ACCEPT_THROTTLE_LIMIT);
    _launcher.setThrottleSleepTime(ACCEPT_THROTTLE_SLEEP_TIME);
  }

  /**
   * Sets the id.
   */
  // exists only for QA regressions
  @Deprecated
  public void setId(String id)
  {
  }

  public String getDebugId()
  {
    return getUrl();
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public PortMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Set protocol.
   */
  public void setProtocol(Protocol protocol)
    throws ConfigException
  {
    _protocol = protocol;
  }

  /**
   * Returns the protocol handler responsible for generating protocol-specific
   * ProtocolConnections.
   */
  public Protocol getProtocol()
  {
    return _protocol;
  }

  /**
   * Gets the protocol name.
   */
  public String getProtocolName()
  {
    if (_protocol != null)
      return _protocol.getProtocolName();
    else
      return null;
  }

  /**
   * Sets the address
   */
  @Configurable
  public void setAddress(String address)
    throws UnknownHostException
  {
    if ("*".equals(address))
      address = null;

    _address = address;

    if (address != null)
      _socketAddress = InetAddress.getByName(address);
  }

  /**
   * Gets the IP address
   */
  public String getAddress()
  {
    return _address;
  }

  /**
   * @deprecated
   */
  public void setHost(String address)
    throws UnknownHostException
  {
    setAddress(address);
  }

  /**
   * Sets the port.
   */
  @Configurable
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * Gets the port.
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * Gets the local port (for ephemeral ports)
   */
  public int getLocalPort()
  {
    if (_serverSocket != null)
      return _serverSocket.getLocalPort();
    else
      return _port;
  }

  /**
   * Sets the virtual host for IP-based virtual host.
   */
  @Configurable
  public void setVirtualHost(String host)
  {
    _virtualHost = host;
  }

  /**
   * Gets the virtual host for IP-based virtual host.
   */
  public String getVirtualHost()
  {
    return _virtualHost;
  }

  /**
   * Sets the SSL factory
   */
  public void setSSL(SSLFactory factory)
  {
    _sslFactory = factory;
  }

  /**
   * Sets the SSL factory
   */
  @Configurable
  public SSLFactory createOpenssl()
    throws ConfigException
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName("com.caucho.vfs.OpenSSLFactory", false, loader);

      _sslFactory = (SSLFactory) cl.newInstance();

      return _sslFactory;
    } catch (Throwable e) {
      e.printStackTrace();

      log.log(Level.FINER, e.toString(), e);

      throw new ConfigException(L.l("<openssl> requires Resin Professional. See http://www.caucho.com for more information."),
                                e);
    }
  }

  /**
   * Sets the SSL factory
   */
  public JsseSSLFactory createJsse()
  {
    // should probably check that openssl exists
    return new JsseSSLFactory();
  }

  /**
   * Sets the SSL factory
   */
  public void setJsseSsl(JsseSSLFactory factory)
  {
    _sslFactory = factory;
  }

  /**
   * Gets the SSL factory.
   */
  public SSLFactory getSSL()
  {
    return _sslFactory;
  }

  /**
   * Returns true for ssl.
   */
  public boolean isSSL()
  {
    return _sslFactory != null;
  }

  /**
   * Sets true for secure
   */
  @Configurable
  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  /**
   * Return true for secure
   */
  public boolean isSecure()
  {
    return _isSecure || _sslFactory != null;
  }

  /**
   * Sets the server socket.
   */
  public void setServerSocket(QServerSocket socket)
  {
    _serverSocket = socket;
  }

  //
  // Configuration/Tuning
  //

  /**
   * Sets the minimum spare listen.
   */
  @Configurable
  public void setAcceptThreadMin(int minSpare)
    throws ConfigException
  {
    /*
    if (minSpare < 1)
      throw new ConfigException(L.l("accept-thread-min must be at least 1."));
      */
    if (minSpare < 1)
      return;

    _launcher.setIdleMin(minSpare);
  }

  /**
   * The minimum spare threads.
   */
  public int getAcceptThreadMin()
  {
    return _launcher.getIdleMin();
  }

  /**
   * Sets the minimum spare listen.
   */
  @Configurable
  public void setAcceptThreadMax(int maxSpare)
    throws ConfigException
  {
    /*
    if (maxSpare < 1)
      throw new ConfigException(L.l("accept-thread-max must be at least 1."));
      */
    if (maxSpare < 1)
      return;

    _launcher.setIdleMax(maxSpare);
  }

  /**
   * The maximum spare threads.
   */
  public int getAcceptThreadMax()
  {
    return _launcher.getIdleMax();
  }

  @Configurable
  public void setPortThreadMax(int max)
  {
    int threadMax = ThreadPool.getThreadPool().getThreadMax();

    if (threadMax < max) {
      log.warning(L.l("<port-thread-max> value '{0}' should be less than <thread-max> value '{1}'",
                      max, threadMax));
    }

    _launcher.setThreadMax(max);
  }

  public int getPortThreadMax()
  {
    return _launcher.getThreadMax();
  }

    /**
   * Sets the minimum spare idle timeout.
   */
  @Configurable
  public void setAcceptThreadIdleTimeout(Period timeout)
    throws ConfigException
  {
    _launcher.setIdleTimeout(timeout.getPeriod());
  }

  /**
   * Sets the minimum spare idle timeout.
   */
  public long getAcceptThreadIdleTimeout()
    throws ConfigException
  {
    return _launcher.getIdleTimeout();
  }


  //
  // launcher throttle configuration
  //


  /**
   * Sets the throttle period.
   */
  public void setThrottlePeriod(long period)
  {
    _launcher.setThrottlePeriod(period);
  }

  /**
   * Sets the throttle limit.
   */
  public void setThrottleLimit(int limit)
  {
    _launcher.setThrottleLimit(limit);
  }

  /**
   * Sets the throttle sleep time.
   */
  public void setThrottleSleepTime(long period)
  {
    _launcher.setThrottleSleepTime(period);
  }


  /**
   * Sets the operating system listen backlog
   */
  @Configurable
  public void setAcceptListenBacklog(int listen)
    throws ConfigException
  {
    if (listen < 1)
      throw new ConfigException(L.l("accept-listen-backlog must be at least 1."));

    _acceptListenBacklog = listen;
  }

  /**
   * The operating system listen backlog
   */
  public int getAcceptListenBacklog()
  {
    return _acceptListenBacklog;
  }

  /**
   * Sets the connection max.
   */
  @Configurable
  public void setConnectionMax(int max)
  {
    _connectionMax = max;
  }

  /**
   * Gets the connection max.
   */
  public int getConnectionMax()
  {
    return _connectionMax;
  }

  /**
   * Sets the read/write timeout for the accepted sockets.
   */
  @Configurable
  public void setSocketTimeout(Period period)
  {
    _socketTimeout = period.getPeriod();
  }

  /**
   * Sets the read/write timeout for the accepted sockets.
   */
  public void setSocketTimeoutMillis(long timeout)
  {
    _socketTimeout = timeout;
  }

  /**
   * Gets the read timeout for the accepted sockets.
   */
  public long getSocketTimeout()
  {
    return _socketTimeout;
  }

  public void setRequestTimeout(Period period)
  {
    _requestTimeout = period.getPeriod();
  }

  /**
   * Returns the max time for a request.
   */
  public long getRequestTimeout()
  {
    return _requestTimeout;
  }

  /**
   * Gets the tcp-no-delay property
   */
  public boolean isTcpNoDelay()
  {
    return _isTcpNoDelay;
  }

  /**
   * Sets the tcp-no-delay property
   */
  @Configurable
  public void setTcpNoDelay(boolean tcpNoDelay)
  {
    _isTcpNoDelay = tcpNoDelay;
  }

  /**
   * Sets the tcp-keepalive property
   */
  @Configurable
  public void setTcpKeepalive(boolean tcpKeepalive)
  {
    _isTcpKeepalive = tcpKeepalive;
  }

  public boolean isTcpKeepalive()
  {
    return _isTcpKeepalive;
  }

  /**
   * Gets the tcp-cork property
   */
  public boolean isTcpCork()
  {
    return _isTcpNoDelay;
  }

  /**
   * Sets the tcp-no-delay property
   */
  @Configurable
  public void setTcpCork(boolean tcpCork)
  {
    _isTcpCork = tcpCork;
  }

  /**
   * Configures the throttle.
   */
  @Configurable
  public void setThrottleConcurrentMax(int max)
  {
    Throttle throttle = createThrottle();

    if (throttle != null)
      throttle.setMaxConcurrentRequests(max);
  }

  /**
   * Configures the throttle.
   */
  public long getThrottleConcurrentMax()
  {
    if (_throttle != null)
      return _throttle.getMaxConcurrentRequests();
    else
      return -1;
  }

  public void setEnableJni(boolean isEnableJni)
  {
    _isEnableJni = isEnableJni;
  }

  public boolean isJniEnabled()
  {
    if (_serverSocket != null) {
      return _serverSocket.isJni();
    }
    else {
      return false;
    }
  }

  private Throttle createThrottle()
  {
    if (_throttle == null) {
      _throttle = Throttle.createPro();

      if (_throttle == null
          && ServletService.getCurrent() != null
          && ! ServletService.getCurrent().isWatchdog())
        throw new ConfigException(L.l("throttle configuration requires Resin Professional"));
    }

    return _throttle;
  }

  //
  // compat config
  //

  /**
   * Sets the keepalive max.
   */
  public void setKeepaliveMax(int max)
  {
    _keepaliveMax = max;
  }

  /**
   * Gets the keepalive max.
   */
  public int getKeepaliveMax()
  {
    return _keepaliveMax;
  }

  /**
   * Sets the keepalive max.
   */
  public void setKeepaliveConnectionTimeMax(Period period)
  {
    _keepaliveTimeMax = period.getPeriod();
  }

  /**
   * Gets the keepalive max.
   */
  public long getKeepaliveConnectionTimeMax()
  {
    return _keepaliveTimeMax;
  }

  public void setKeepaliveConnectionTimeMaxMillis(long timeout)
  {
    _keepaliveTimeMax = timeout;
  }

  /**
   * Gets the suspend max.
   */
  public long getSuspendTimeMax()
  {
    return _suspendTimeMax;
  }

  public void setSuspendTimeMax(Period period)
  {
    _suspendTimeMax = period.getPeriod();
  }

  public void setSuspendReaperTimeout(Period period)
  {
    _suspendReaperTimeout = period.getPeriod();
  }

  public void setKeepaliveTimeout(Period period)
  {
    setKeepaliveTimeoutMillis(period.getPeriod());
  }

  public long getKeepaliveTimeout()
  {
    return _keepaliveTimeout;
  }

  public void setKeepaliveTimeoutMillis(long timeout)
  {
    _keepaliveTimeout = timeout;
  }

  public boolean isKeepaliveAsyncEnabled()
  {
    return _isKeepaliveAsyncEnable;
  }

  public void setKeepaliveSelectEnabled(boolean isKeepaliveSelect)
  {
    _isKeepaliveAsyncEnable = isKeepaliveSelect;
  }

  public void setKeepaliveSelectEnable(boolean isKeepaliveSelect)
  {
    setKeepaliveSelectEnabled(isKeepaliveSelect);
  }

  public void setKeepaliveSelectMax(int max)
  {
  }

  public long getKeepaliveSelectThreadTimeout()
  {
    return _keepaliveSelectThreadTimeout;
  }

  public long getKeepaliveThreadTimeout()
  {
    return _keepaliveSelectThreadTimeout;
  }

  public void setKeepaliveSelectThreadTimeout(Period period)
  {
    setKeepaliveSelectThreadTimeoutMillis(period.getPeriod());
  }

  public void setKeepaliveThreadTimeout(Period period)
  {
    setKeepaliveSelectThreadTimeoutMillis(period.getPeriod());
  }

  public void setKeepaliveSelectThreadTimeoutMillis(long timeout)
  {
    _keepaliveSelectThreadTimeout = timeout;
  }

  public long getBlockingTimeoutForSelect()
  {
    long timeout = _keepaliveSelectThreadTimeout;

    if (timeout <= 10)
      return timeout;
    else if (_threadPool.getFreeThreadCount() < 64)
      return 10;
    else
      return timeout;
  }

  public int getKeepaliveSelectMax()
  {
    if (getSelectManager() != null)
      return getSelectManager().getSelectMax();
    else
      return -1;
  }

  /**
   * Ignore unknown tags.
   *
   * server/0940
   * network/02b0
   */
  @Configurable
  public void addContentProgram(ConfigProgram program)
  {
  }

  /**
   * Returns the thread launcher for the link.
   */
  SocketLinkThreadLauncher getLauncher()
  {
    return _launcher;
  }

  ThreadPool getThreadPool()
  {
    return _threadPool;
  }

  //
  // statistics
  //

  /**
   * Returns the thread count.
   */
  public int getThreadCount()
  {
    return _launcher.getThreadCount();
  }

  /**
   * Returns the active thread count.
   */
  public int getActiveThreadCount()
  {
    return _launcher.getThreadCount() - _launcher.getIdleCount();
  }

  /**
   * Returns the count of idle threads.
   */
  public int getIdleThreadCount()
  {
    return _launcher.getIdleCount();
  }

  /**
   * Returns the count of start threads.
   */
  public int getStartThreadCount()
  {
    return _launcher.getStartingCount();
  }

  /**
   * Returns the number of keepalive connections
   */
  public int getKeepaliveCount()
  {
    return _keepaliveAllocateCount.get();
  }

  public Lifecycle getLifecycleState()
  {
    return _lifecycle;
  }

  public boolean isAfterBind()
  {
    return _isBind.get();
  }
  /**
   * Returns true if the port is active.
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns the active connections.
   */
  public int getActiveConnectionCount()
  {
    return getActiveThreadCount();
  }

  /**
   * Returns the keepalive connections.
   */
  public int getKeepaliveConnectionCount()
  {
    return getKeepaliveCount();
  }

  /**
   * Returns the number of keepalive connections
   */
  public int getKeepaliveThreadCount()
  {
    return _keepaliveThreadCount.get();
  }

  /**
   * Returns the number of connections in the select.
   */
  public int getSelectConnectionCount()
  {
    if (_selectManager != null)
      return _selectManager.getSelectCount();
    else
      return -1;
  }

  /**
   * Returns the server socket class name for debugging.
   */
  public String getServerSocketClassName()
  {
    QServerSocket ss = _serverSocket;

    if (ss != null)
      return ss.getClass().getName();
    else
      return null;
  }

  /**
   * Initializes the port.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (! _lifecycle.toInit())
      return;

    _launcher.init();
  }

  public String getUrl()
  {
    if (_url == null) {
      StringBuilder url = new StringBuilder();

      if (_protocol != null)
        url.append(_protocol.getProtocolName());
      else
        url.append("unknown");
      url.append("://");

      if (getAddress() != null)
        url.append(getAddress());
      else
        url.append("*");
      url.append(":");
      url.append(getPort());

      if (_serverId != null && ! "".equals(_serverId)) {
        url.append("(");
        url.append(_serverId);
        url.append(")");
      }

      _url = url.toString();
    }

    return _url;
  }

  /**
   * Starts the port listening.
   */
  public void bind()
    throws Exception
  {
    if (_isBind.getAndSet(true))
      return;

    if (_protocol == null)
      throw new IllegalStateException(L.l("'{0}' must have a configured protocol before starting.", this));

    // server 1e07
    if (_port < 0)
      return;

    NetworkSystem system = NetworkSystem.getCurrent();

    if (_throttle == null)
      _throttle = new Throttle();

    boolean isEnableJni = _isEnableJni && ! CauchoSystem.isWindows();
    
    if (_serverSocket != null) {
      if (_address != null)
        log.info("listening to " + _address + ":" + _serverSocket.getLocalPort());
      else
        log.info("listening to " + _serverSocket.getLocalPort());
    }
    else if (_sslFactory != null && _socketAddress != null) {
      _serverSocket = _sslFactory.create(_socketAddress, _port);

      log.info(_protocol.getProtocolName() + "s listening to " + _socketAddress.getHostName() + ":" + _port);
    }
    else if (_sslFactory != null) {
      if (_address == null) {
        _serverSocket = _sslFactory.create(null, _port);
        log.info(_protocol.getProtocolName() + "s listening to *:" + _port);
      }
      else {
        InetAddress addr = InetAddress.getByName(_address);

        _serverSocket = _sslFactory.create(addr, _port);

        log.info(_protocol.getProtocolName() + "s listening to " + _address + ":" + _port);
      }
    }
    else if (_socketAddress != null) {
      _serverSocket = system.openServerSocket(_socketAddress, _port,
                                              _acceptListenBacklog,
                                              isEnableJni);

      log.info(_protocol.getProtocolName() + " listening to " + _socketAddress.getHostName() + ":" + _serverSocket.getLocalPort());
    }
    else {
      _serverSocket = system.openServerSocket(null, _port, _acceptListenBacklog,
                                              isEnableJni);

      log.info(_protocol.getProtocolName() + " listening to *:"
               + _serverSocket.getLocalPort());
    }

    assert(_serverSocket != null);

    postBind();
  }

  /**
   * Starts the port listening.
   */
  public void bind(QServerSocket ss)
    throws Exception
  {
    if (ss == null)
      throw new NullPointerException();

    _isBind.set(true);

    if (_protocol == null)
      throw new IllegalStateException(L.l("'{0}' must have a configured protocol before starting.", this));

    if (_throttle == null)
      _throttle = new Throttle();

    _serverSocket = ss;

    String scheme = _protocol.getProtocolName();

    if (_address != null)
      log.info(scheme + " listening to " + _address + ":" + _port);
    else
      log.info(scheme + " listening to *:" + _port);

    if (_sslFactory != null)
      _serverSocket = _sslFactory.bind(_serverSocket);
  }

  public void postBind()
  {
    if (_isPostBind.getAndSet(true))
      return;

    if (_serverSocket == null)
      return;

    _serverSocket.setTcpNoDelay(_isTcpNoDelay);
    _serverSocket.setTcpKeepalive(_isTcpKeepalive);
    _serverSocket.setTcpCork(_isTcpCork);

    _serverSocket.setConnectionSocketTimeout((int) getSocketTimeout());

    if (_serverSocket.isJni()) {
      SocketPollService pollService = SocketPollService.getCurrent();

      if (pollService != null && isKeepaliveAsyncEnabled()) {
        _selectManager = pollService.getSelectManager();
      }
    }

    if (_keepaliveMax < 0 && _selectManager != null)
      _keepaliveMax = _selectManager.getSelectMax();

    if (_keepaliveMax < 0)
      _keepaliveMax = KEEPALIVE_MAX;

    _admin.register();
  }

  /**
   * binds for the watchdog.
   */
  public QServerSocket bindForWatchdog()
    throws java.io.IOException
  {
    QServerSocket ss;

    // use same method for ports for testability reasons
    /*
    if (_port >= 1024)
      return null;
    else
    */

    if (_sslFactory instanceof JsseSSLFactory) {
      if (_port < 1024) {
        log.warning(this + " cannot bind jsse in watchdog");
      }

      return null;
    }

    if (_socketAddress != null) {
      ss = QJniServerSocket.createJNI(_socketAddress, _port);

      if (ss == null)
        return null;

      log.fine(this + " watchdog binding to " + _socketAddress.getHostName() + ":" + _port);
    }
    else {
      ss = QJniServerSocket.createJNI(null, _port);

      if (ss == null)
        return null;

      log.fine(this + " watchdog binding to *:" + _port);
    }

    if (! ss.isJni()) {
      ss.close();

      return ss;
    }

    ss.setTcpNoDelay(_isTcpNoDelay);
    ss.setTcpKeepalive(_isTcpKeepalive);
    ss.setTcpCork(_isTcpCork);

    ss.setConnectionSocketTimeout((int) getSocketTimeout());

    return ss;
  }

  /**
   * Starts the port listening.
   */
  public void start()
    throws Exception
  {
    if (_port < 0)
      return;

    if (! _lifecycle.toStarting())
      return;

    boolean isValid = false;
    try {
      bind();
      postBind();

      enable();

      _launcher.start();

      _suspendAlarm = new Alarm(new SuspendReaper());
      _suspendAlarm.queue(_suspendReaperTimeout);

      isValid = true;
    } finally {
      if (! isValid)
        close();
    }
  }

  public boolean isEnabled()
  {
    return _lifecycle.isActive();
  }

  /**
   * Starts the port listening for new connections.
   */
  public void enable()
  {
    if (_lifecycle.toActive()) {
      if (_serverSocket != null) {
        _serverSocket.listen(_acceptListenBacklog);
      }
    }
  }

  /**
   * Stops the port from listening for new connections.
   */
  public void disable()
  {
    if (_lifecycle.toStop()) {
      if (_serverSocket != null)
        _serverSocket.listen(0);

      if (_port < 0) {
      }
      else if (_address != null)
        log.info(_protocol.getProtocolName() + " disabled "
                 + _address + ":" + getLocalPort());
      else
        log.info(_protocol.getProtocolName() + " disabled *:" + getLocalPort());
    }
  }

  /**
   * returns the connection info for jmx
   */
  TcpConnectionInfo []getActiveConnections()
  {
    List<TcpConnectionInfo> infoList = new ArrayList<TcpConnectionInfo>();

    TcpSocketLink[] connections = new TcpSocketLink[_activeConnectionSet.size()];
    _activeConnectionSet.keySet().toArray(connections);

    for (int i = 0 ; i < connections.length; i++) {
      TcpConnectionInfo connInfo = connections[i].getConnectionInfo();
      if (connInfo != null)
        infoList.add(connInfo);
    }

    TcpConnectionInfo []infoArray = new TcpConnectionInfo[infoList.size()];
    infoList.toArray(infoArray);

    return infoArray;
  }

  /**
   * returns the select manager.
   */
  public AbstractSelectManager getSelectManager()
  {
    return _selectManager;
  }

  /**
   * Accepts a new connection.
   */
  @Friend(TcpSocketLink.class)
  boolean accept(QSocket socket)
  {
    try {
      SocketLinkThreadLauncher launcher = getLauncher();

      while (! isClosed()) {
        Thread.interrupted();

        if (_serverSocket.accept(socket)) {
          //System.out.println("REMOTE: " + socket.getRemotePort());
          if (launcher.isThreadMax()
              && ! isKeepaliveAsyncEnabled()
              && launcher.getIdleCount() <= 1) {
            // System.out.println("CLOSED:");
            _throttleDisconnectMeter.start();
            _lifetimeThrottleDisconnectCount.incrementAndGet();
            socket.close();
          }
          else if (_throttle.accept(socket)) {
            return true;
          }
          else {
            _throttleDisconnectMeter.start();
            _lifetimeThrottleDisconnectCount.incrementAndGet();
            socket.close();
          }
        }
      }
    } catch (Throwable e) {
      if (_lifecycle.isActive() && log.isLoggable(Level.FINER))
        log.log(Level.FINER, e.toString(), e);
    }

    return false;
  }

  /**
   * Notification when a socket closes.
   */
  void closeSocket(QSocket socket)
  {
    if (_throttle != null)
      _throttle.close(socket);
  }

  /**
   * request threads in a shutdown, but not yet idle.
   */
  void requestShutdownBegin()
  {
    _shutdownRequestCount.incrementAndGet();
  }

  /**
   * request threads in a shutdown, but not yet idle.
   */
  void requestShutdownEnd()
  {
    _shutdownRequestCount.decrementAndGet();
  }

  /**
   * Allocates a keepalive for the connection.
   *
   * @param connectionStartTime - when the connection's accept occurred.
   */
  boolean isKeepaliveAllowed(long connectionStartTime)
  {
    if (! _lifecycle.isActive())
      return false;
    else if (connectionStartTime + _keepaliveTimeMax < CurrentTime.getCurrentTime())
      return false;
    else if (_keepaliveMax <= _keepaliveAllocateCount.get())
      return false;
    else if (_launcher.isThreadMax()
             && _launcher.isIdleLow()
             && ! isKeepaliveAsyncEnabled()) {
      return false;
    }
    else
      return true;
  }

  /**
   * When true, use the async manager to wait for reads rather than
   * blocking.
   */
  public boolean isAsyncThrottle()
  {
    return isKeepaliveAsyncEnabled() && _launcher.isThreadHigh();
  }


  /**
   * Marks the keepalive allocation as starting.
   * Only called from ConnectionState.
   */
  void keepaliveAllocate()
  {
    _keepaliveAllocateCount.incrementAndGet();
  }

  /**
   * Marks the keepalive allocation as ending.
   * Only called from ConnectionState.
   */
  void keepaliveFree()
  {
    int value = _keepaliveAllocateCount.decrementAndGet();

    if (value < 0 && isActive()) {
      System.out.println("FAILED keep-alive; " + value);
      Thread.dumpStack();
    }
  }

  /**
   * Reads data from a keepalive connection
   */
  int keepaliveThreadRead(ReadStream is)
    throws IOException
  {
    if (isClosed()) {
      return -1;
    }

    int available = is.getBufferAvailable();

    if (available > 0) {
      return available;
    }

    long timeout = Math.min(getKeepaliveTimeout(), getSocketTimeout());

    // server/2l02
    int keepaliveThreadCount = _keepaliveThreadCount.incrementAndGet();

    // boolean isSelectManager = getServer().isSelectManagerEnabled();

    try {
      int result;

      if (isKeepaliveAsyncEnabled() && _selectManager != null) {
        timeout = Math.min(timeout, getBlockingTimeoutForSelect());

        if (keepaliveThreadCount > 32) {
          // throttle the thread keepalive when heavily loaded to save threads
          if (isAsyncThrottle()) {
            // when async throttle is active move the thread to async
            // immediately
            return 0;
          }
          else {
            timeout = Math.min(timeout, 100);
          }
        }
      }

      /*
      if (timeout < 0)
        timeout = 0;
        */

      if (timeout <= 0) {
        return 0;
      }

      _keepaliveThreadMeter.start();

      try {
        if (false && _keepaliveThreadCount.get() < 32) {
          // benchmark perf with memcache
          result = is.fillWithTimeout(-1);
        }
        else {
          result = is.fillWithTimeout(timeout);
        }
      } finally {
        _keepaliveThreadMeter.end();
      }

      if (isClosed()) {
        return -1;
      }

      return result;
    } catch (IOException e) {
      if (isClosed()) {
        log.log(Level.FINEST, e.toString(), e);

        return -1;
      }

      throw e;
    } finally {
      _keepaliveThreadCount.decrementAndGet();
    }
  }

  /**
   * Suspends the controller (for comet-style ajax)
   *
   * @return true if the connection was added to the suspend list
   */
  @Friend(SocketLinkState.class)
  void cometSuspend(TcpSocketLink conn)
  {
    _suspendMeter.start();

    _suspendConnectionSet.add(conn);
  }

  /**
   * Remove from suspend list.
   */
  @Friend(SocketLinkState.class)
  boolean cometDetach(TcpSocketLink conn)
  {
    _suspendMeter.end();

    return _suspendConnectionSet.remove(conn);
  }

  void duplexKeepaliveBegin()
  {
  }

  void duplexKeepaliveEnd()
  {
  }

  /**
   * Returns true if the port is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.getState().isDestroyed();
  }

  //
  // statistics
  //

  /**
   * Returns the number of connections
   */
  public int getConnectionCount()
  {
    return _activeConnectionCount.get();
  }

  /**
   * Returns the number of comet connections.
   */
  public int getCometIdleCount()
  {
    return _suspendConnectionSet.size();
  }

  /**
   * Returns the number of duplex connections.
   */
  public int getDuplexCount()
  {
    return 0;
  }

  void addLifetimeRequestCount()
  {
    _lifetimeRequestCount.incrementAndGet();
  }

  public long getLifetimeRequestCount()
  {
    return _lifetimeRequestCount.get();
  }

  void addLifetimeKeepaliveCount()
  {
    _keepaliveMeter.start();
    _lifetimeKeepaliveCount.incrementAndGet();
  }

  public long getLifetimeKeepaliveCount()
  {
    return _lifetimeKeepaliveCount.get();
  }

  void addLifetimeKeepaliveSelectCount()
  {
    _lifetimeKeepaliveSelectCount.incrementAndGet();
  }

  public long getLifetimeKeepaliveSelectCount()
  {
    return _lifetimeKeepaliveSelectCount.get();
  }

  void addLifetimeClientDisconnectCount()
  {
    _lifetimeClientDisconnectCount.incrementAndGet();
  }

  public long getLifetimeClientDisconnectCount()
  {
    return _lifetimeClientDisconnectCount.get();
  }

  void addLifetimeRequestTime(long time)
  {
    _lifetimeRequestTime.addAndGet(time);
  }

  public long getLifetimeRequestTime()
  {
    return _lifetimeRequestTime.get();
  }

  void addLifetimeReadBytes(long bytes)
  {
    _lifetimeReadBytes.addAndGet(bytes);
  }

  public long getLifetimeReadBytes()
  {
    return _lifetimeReadBytes.get();
  }

  void addLifetimeWriteBytes(long bytes)
  {
    _lifetimeWriteBytes.addAndGet(bytes);
  }

  public long getLifetimeWriteBytes()
  {
    return _lifetimeWriteBytes.get();
  }

  long getLifetimeThrottleDisconnectCount()
  {
    return _lifetimeThrottleDisconnectCount.get();
  }

  /**
   * Find the TcpConnection based on the thread id (for admin)
   */
  public TcpSocketLink findConnectionByThreadId(long threadId)
  {
    ArrayList<TcpSocketLink> connList
      = new ArrayList<TcpSocketLink>(_activeConnectionSet.keySet());

    for (TcpSocketLink conn : connList) {
      if (conn.getThreadId() == threadId)
        return conn;
    }

    return null;
  }

  TcpSocketLink allocateConnection()
    throws IOException
  {
    TcpSocketLink startConn = _idleConn.allocate();

    if (startConn == null) {
      int connId = _connectionCount.incrementAndGet();
      QSocket socket = _serverSocket.createSocket();

      startConn = new TcpSocketLink(connId, this, socket);
    }

    _activeConnectionSet.put(startConn,startConn);
    _activeConnectionCount.incrementAndGet();

    return startConn;
  }

  /**
   * Closes the stats for the connection.
   */
  @Friend(TcpSocketLink.class)
  void closeConnection(TcpSocketLink conn)
  {
    if (_activeConnectionSet.remove(conn) != null) {
      _activeConnectionCount.decrementAndGet();
    }
    else if (! isClosed()){
      Thread.dumpStack();
    }

    _launcher.wake();
  }

  /**
   * Frees the connection to the idle pool.
   *
   * only called from ConnectionState
   */
  @Friend(TcpSocketLink.class)
  void free(TcpSocketLink conn)
  {
    _idleConn.free(conn);
  }

  /**
   * Shuts the Port down.  The server gives connections 30
   * seconds to complete.
   */
  public void close()
  {
    if (! _lifecycle.toDestroy())
      return;

    if (log.isLoggable(Level.FINE))
      log.fine(this + " closing");

    _launcher.close();

    Alarm suspendAlarm = _suspendAlarm;
    _suspendAlarm = null;

    if (suspendAlarm != null)
      suspendAlarm.dequeue();

    QServerSocket serverSocket = _serverSocket;
    _serverSocket = null;

    InetAddress localAddress = null;
    int localPort = 0;
    if (serverSocket != null) {
      localAddress = serverSocket.getLocalAddress();
      localPort = serverSocket.getLocalPort();
    }

    // close the server socket
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (Throwable e) {
      }

      try {
        synchronized (serverSocket) {
          serverSocket.notifyAll();
        }
      } catch (Throwable e) {
      }
    }

    /*
    if (selectManager != null) {
      try {
        selectManager.onPortClose(this);
      } catch (Throwable e) {
      }
    }
    */

    Set<TcpSocketLink> activeSet;

    synchronized (_activeConnectionSet) {
      activeSet = new HashSet<TcpSocketLink>(_activeConnectionSet.keySet());
    }

    for (TcpSocketLink conn : activeSet) {
      try {
        conn.requestDestroy();
      }
      catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    // wake the start thread
    _launcher.wake();

    // Close the socket server socket and send some request to make
    // sure the Port accept thread is woken and dies.
    // The ping is before the server socket closes to avoid
    // confusing the threads

    // ping the accept port to wake the listening threads
    if (localPort > 0) {
      int idleCount = getIdleThreadCount() + getStartThreadCount();

      for (int i = 0; i < idleCount + 10; i++) {
        InetSocketAddress addr;

        if (getIdleThreadCount() == 0)
          break;

        if (localAddress == null ||
            localAddress.getHostAddress().startsWith("0.")) {
          addr = new InetSocketAddress("127.0.0.1", localPort);
          connectAndClose(addr);

          addr = new InetSocketAddress("[::1]", localPort);
          connectAndClose(addr);
        }
        else {
          addr = new InetSocketAddress(localAddress, localPort);
          connectAndClose(addr);
        }

        try {
          Thread.sleep(10);
        } catch (Exception e) {
        }
      }
    }

    TcpSocketLink conn;
    while ((conn = _idleConn.allocate()) != null) {
      conn.requestDestroy();
    }

    // cloud/0550
    /*
    // clearning the select manager must be after the conn.requestDestroy
    AbstractSelectManager selectManager = _selectManager;
    _selectManager = null;
    */

    log.finest(this + " closed");
  }

  private void connectAndClose(InetSocketAddress addr)
  {
    try {
      Socket socket = new Socket();

      socket.connect(addr, 100);

      socket.close();
    } catch (ConnectException e) {
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }

  }

  public String toURL()
  {
    return getUrl();
  }

  /*
  @Override
  protected String getThreadName()
  {
    return "resin-port-" + getAddress() + ":" + getPort();
  }
  */

  @Override
  public String toString()
  {
    if (_url != null)
      return getClass().getSimpleName() + "[" + _url + "]";
    else
      return getClass().getSimpleName() + "[" + getAddress() + ":" + getPort() + "]";
  }

  public class SuspendReaper implements AlarmListener {
    private ArrayList<TcpSocketLink> _suspendSet
      = new ArrayList<TcpSocketLink>();

    private ArrayList<TcpSocketLink> _timeoutSet
      = new ArrayList<TcpSocketLink>();

    private ArrayList<TcpSocketLink> _completeSet
      = new ArrayList<TcpSocketLink>();

    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        _suspendSet.clear();
        _timeoutSet.clear();
        _completeSet.clear();

        long now = CurrentTime.getCurrentTime();

        // wake the launcher in case of freeze
        _launcher.wake();

        _suspendSet.addAll(_suspendConnectionSet);
        for (int i = _suspendSet.size() - 1; i >= 0; i--) {
          TcpSocketLink conn = _suspendSet.get(i);

          if (conn.getIdleExpireTime() < now) {
            _timeoutSet.add(conn);
            continue;
          }

          long idleStartTime = conn.getIdleStartTime();

          // check periodically for end of file
          if (idleStartTime + _suspendCloseTimeMax < now
              && conn.isReadEof()) {
            _completeSet.add(conn);
          }
        }

        for (int i = _timeoutSet.size() - 1; i >= 0; i--) {
          TcpSocketLink conn = _timeoutSet.get(i);

          if (log.isLoggable(Level.FINE))
            log.fine(this + " suspend idle timeout " + conn);

          try {
            conn.requestCometTimeout();
          } catch (Exception e) {
            log.log(Level.WARNING, conn + ": " + e.getMessage(), e);
          }
        }

        for (int i = _completeSet.size() - 1; i >= 0; i--) {
          TcpSocketLink conn = _completeSet.get(i);

          if (log.isLoggable(Level.FINE))
            log.fine(this + " async end-of-file " + conn);

          try {
            conn.requestCometComplete();
          } catch (Exception e) {
            log.log(Level.WARNING, conn + ": " + e.getMessage(), e);
          }
          /*
          AsyncController async = conn.getAsyncController();

          if (async != null)
            async.complete();
            */

          // server/1lc2
          // conn.wake();
          // conn.destroy();
        }
      } catch (Throwable e) {
        e.printStackTrace();
      } finally {
        if (! isClosed()) {
          alarm.queue(_suspendReaperTimeout);
        }
      }
    }
  }
}
