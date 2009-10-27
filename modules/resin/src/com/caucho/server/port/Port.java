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

package com.caucho.server.port;

import com.caucho.config.ConfigException;
import com.caucho.config.program.*;
import com.caucho.config.types.*;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.TcpConnectionInfo;
import com.caucho.server.connection.ConnectionController;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.Server;
import com.caucho.util.*;
import com.caucho.vfs.JsseSSLFactory;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SSLFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a protocol connection.
 */
public class Port extends TaskWorker
  implements EnvironmentListener, Runnable
{
  private static final L10N L = new L10N(Port.class);

  private static final Logger log
    = Logger.getLogger(Port.class.getName());

  private static final int DEFAULT = -0xcafe;

  // started at 128, but that seems wasteful since the active threads
  // themselves are buffering the free connections
  private FreeList<TcpConnection> _freeConn
    = new FreeList<TcpConnection>(32);

  // The owning server
  private ProtocolDispatchServer _server;

  private ThreadPool _threadPool = ThreadPool.getThreadPool();

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

  private int _idleThreadMin = DEFAULT;
  private int _idleThreadMax = DEFAULT;

  private int _acceptListenBacklog = DEFAULT;

  private int _connectionMax = DEFAULT;

  private int _keepaliveMax = DEFAULT;

  private long _keepaliveTimeMax = DEFAULT;
  private long _keepaliveTimeout = DEFAULT;
  private long _keepaliveSelectThreadTimeout = DEFAULT;
  private int _minSpareConnection = 16;

  // default timeout
  private long _socketTimeout = DEFAULT;

  private long _suspendReaperTimeout = 60000L;
  private long _suspendTimeMax = DEFAULT;

  private boolean _tcpNoDelay = true;

  // The virtual host name
  private String _virtualHost;

  private final PortAdmin _admin = new PortAdmin(this);

  // the server socket
  private QServerSocket _serverSocket;

  // the throttle
  private Throttle _throttle;

  // the selection manager
  private AbstractSelectManager _selectManager;

  // active set of all connections
  private Set<TcpConnection> _activeConnectionSet
    = Collections.synchronizedSet(new HashSet<TcpConnection>());

  private final AtomicInteger _activeConnectionCount = new AtomicInteger();

  // server push (comet) suspend set
  private Set<TcpConnection> _suspendConnectionSet
    = Collections.synchronizedSet(new HashSet<TcpConnection>());

  private final AtomicInteger _idleThreadCount = new AtomicInteger();
  private final AtomicInteger _startThreadCount = new AtomicInteger();

  // timeout to limit the thread close rate
  private long _idleCloseTimeout = 15000L;
  private volatile long _idleCloseExpire;

  // reaper alarm for timed out comet requests
  private Alarm _suspendAlarm;

  // statistics

  private final AtomicInteger _threadCount = new AtomicInteger();

  private volatile long _lifetimeRequestCount;
  private volatile long _lifetimeKeepaliveCount;
  private volatile long _lifetimeClientDisconnectCount;
  private volatile long _lifetimeRequestTime;
  private volatile long _lifetimeReadBytes;
  private volatile long _lifetimeWriteBytes;

  // total keepalive
  private AtomicInteger _keepaliveCount = new AtomicInteger();
  // thread-based
  private AtomicInteger _keepaliveThreadCount = new AtomicInteger();
  private final Object _keepaliveCountLock = new Object();

  // True if the port has been bound
  private final AtomicBoolean _isBind = new AtomicBoolean();
  private final AtomicBoolean _isPostBind = new AtomicBoolean();

  // The port lifecycle
  private final Lifecycle _lifecycle = new Lifecycle();

  public Port()
  {
  }

  public Port(ClusterServer server)
  {
  }

  /**
   * Sets the containing server.
   */
  public void setParent(ProtocolDispatchServer parent)
  {
    setServer(parent);
  }

  /**
   * Sets the server.
   */
  public void setServer(ProtocolDispatchServer protocolServer)
  {
    _server = protocolServer;

    if (_protocol != null)
      _protocol.setServer(protocolServer);

    if (protocolServer instanceof Server) {
      Server server = (Server) protocolServer;

      if (_idleThreadMax == DEFAULT)
        _idleThreadMax = server.getAcceptThreadMax();

      if (_idleThreadMin == DEFAULT)
        _idleThreadMin = server.getAcceptThreadMin();

      if (_acceptListenBacklog == DEFAULT)
        _acceptListenBacklog = server.getAcceptListenBacklog();

      if (_connectionMax == DEFAULT)
        _connectionMax = server.getConnectionMax();

      if (_keepaliveMax == DEFAULT)
        _keepaliveMax = server.getKeepaliveMax();

      if (_keepaliveTimeMax == DEFAULT)
        _keepaliveTimeMax = server.getKeepaliveConnectionTimeMax();

      if (_keepaliveTimeout == DEFAULT)
        _keepaliveTimeout = server.getKeepaliveTimeout();

      if (_keepaliveSelectThreadTimeout == DEFAULT) {
        _keepaliveSelectThreadTimeout
          = server.getKeepaliveSelectThreadTimeout();
      }

      if (_suspendTimeMax == DEFAULT)
        _suspendTimeMax = server.getSuspendTimeMax();

      if (_socketTimeout == DEFAULT)
        _socketTimeout = server.getSocketTimeout();
    }
  }

  /**
   * Gets the server.
   */
  public ProtocolDispatchServer getServer()
  {
    return _server;
  }

  /**
   * Sets the id.
   */
  public void setId(String id)
  {
    _serverId = id;
  }

  /**
   * Sets the server id.
   */
  public void setServerId(String id)
  {
    _serverId = id;
  }

  /**
   * Gets the server id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  public void setType(Class cl)
  {
    setClass(cl);
  }

  public void setClass(Class cl)
  {
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
    /* server/0170
    if (_server == null)
      throw new IllegalStateException(L.l("Server is not set."));
    */

    _protocol = protocol;
    _protocol.setServer(_server);
  }

  /**
   * Set protocol.
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
   * @deprecated
   */
  public void setHost(String address)
    throws UnknownHostException
  {
    setAddress(address);
  }

  /**
   * Gets the IP address
   */
  public String getAddress()
  {
    return _address;
  }

  /**
   * Sets the port.
   */
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
  public SSLFactory createOpenssl()
    throws ConfigException
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class cl = Class.forName("com.caucho.vfs.OpenSSLFactory", false, loader);

      _sslFactory = (SSLFactory) cl.newInstance();

      return _sslFactory;
    } catch (Throwable e) {
      e.printStackTrace();

      log.log(Level.FINER, e.toString(), e);

      throw new ConfigException(L.l("<openssl> requires Resin Professional.  See http://www.caucho.com for more information."),
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
  public void setAcceptThreadMin(int minSpare)
    throws ConfigException
  {
    if (minSpare < 1)
      throw new ConfigException(L.l("accept-thread-min must be at least 1."));

    _idleThreadMin = minSpare;
  }

  /**
   * The minimum spare threads.
   */
  public int getAcceptThreadMin()
  {
    return _idleThreadMin;
  }

  /**
   * Sets the minimum spare listen.
   */
  public void setAcceptThreadMax(int maxSpare)
    throws ConfigException
  {
    if (maxSpare < 1)
      throw new ConfigException(L.l("accept-thread-max must be at least 1."));

    _idleThreadMax = maxSpare;
  }

  /**
   * The maximum spare threads.
   */
  public int getAcceptThreadMax()
  {
    return _idleThreadMax;
  }

  /**
   * Sets the operating system listen backlog
   */
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
   * Returns true for ignore-client-disconnect.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return _server.isIgnoreClientDisconnect();
  }

  /**
   * Sets the read/write timeout for the accepted sockets.
   */
  public void setSocketTimeout(Period period)
  {
    _socketTimeout = period.getPeriod();
  }

  /**
   * Sets the read timeout for the accepted sockets.
   *
   * @deprecated
   */
  public void setReadTimeout(Period period)
  {
    setSocketTimeout(period);
  }

  /**
   * Gets the read timeout for the accepted sockets.
   */
  public long getSocketTimeout()
  {
    return _socketTimeout;
  }

  /**
   * Gets the tcp-no-delay property
   */
  public boolean getTcpNoDelay()
  {
    return _tcpNoDelay;
  }

  /**
   * Sets the tcp-no-delay property
   */
  public void setTcpNoDelay(boolean tcpNoDelay)
  {
    _tcpNoDelay = tcpNoDelay;
  }

  /**
   * Configures the throttle.
   */
  public void setThrottleConcurrentMax(int max)
  {
    Throttle throttle = createThrottle();

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

  /**
   * Sets the write timeout for the accepted sockets.
   *
   * @deprecated
   */
  public void setWriteTimeout(Period period)
  {
  }

  private Throttle createThrottle()
  {
    if (_throttle == null) {
      _throttle = Throttle.createPro();

      if (_throttle == null)
        throw new ConfigException(L.l("throttle configuration requires Resin Professional"));
    }

    return _throttle;
  }

  //
  // compat config
  //

  /**
   * Sets the minimum spare listen.
   */
  public void setMinSpareListen(int minSpare)
    throws ConfigException
  {
    setAcceptThreadMin(minSpare);
  }

  /**
   * Sets the maximum spare listen.
   */
  public void setMaxSpareListen(int maxSpare)
    throws ConfigException
  {
    setAcceptThreadMax(maxSpare);
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
    _lifetimeRequestCount++;
  }

  public long getLifetimeRequestCount()
  {
    return _lifetimeRequestCount;
  }

  void addLifetimeKeepaliveCount()
  {
    _lifetimeKeepaliveCount++;
  }

  public long getLifetimeKeepaliveCount()
  {
    return _lifetimeKeepaliveCount;
  }

  void addLifetimeClientDisconnectCount()
  {
    _lifetimeClientDisconnectCount++;
  }

  public long getLifetimeClientDisconnectCount()
  {
    return _lifetimeClientDisconnectCount;
  }

  void addLifetimeRequestTime(long time)
  {
    _lifetimeRequestTime += time;
  }

  public long getLifetimeRequestTime()
  {
    return _lifetimeRequestTime;
  }

  void addLifetimeReadBytes(long time)
  {
    _lifetimeReadBytes += time;
  }

  public long getLifetimeReadBytes()
  {
    return _lifetimeReadBytes;
  }

  void addLifetimeWriteBytes(long time)
  {
    _lifetimeWriteBytes += time;
  }

  public long getLifetimeWriteBytes()
  {
    return _lifetimeWriteBytes;
  }

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

  public void setKeepaliveTimeout(Period period)
  {
    _keepaliveTimeout = period.getPeriod();
  }

  public long getKeepaliveTimeout()
  {
    return _keepaliveTimeout;
  }

  public long getKeepaliveSelectThreadTimeout()
  {
    return _keepaliveSelectThreadTimeout;
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

  //
  // statistics
  //

  /**
   * Returns the thread count.
   */
  public int getThreadCount()
  {
    return _threadCount.get();
  }

  /**
   * Returns the active thread count.
   */
  public int getActiveThreadCount()
  {
    return _threadCount.get() - _idleThreadCount.get();
  }

  /**
   * Returns the count of idle threads.
   */
  public int getIdleThreadCount()
  {
    return _idleThreadCount.get();
  }

  /**
   * Returns the number of keepalive connections
   */
  public int getKeepaliveCount()
  {
    return _keepaliveCount.get();
  }

  public Lifecycle getLifecycleState()
  {
    return _lifecycle;
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
    return _threadCount.get() - _idleThreadCount.get();
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
   * Returns true if the port should start a new thread because there are
   * less than _idleThreadMin accepting threads.
   */
  private boolean isStartThreadRequired()
  {
    return (_startThreadCount.get() + _idleThreadCount.get() < _idleThreadMin);
  }

  /**
   * Returns the accept pool.
   */
  public int getFreeKeepalive()
  {
    int freeKeepalive = _keepaliveMax - _keepaliveCount.get();
    int freeConnections = (_connectionMax - _activeConnectionCount.get()
                           - _minSpareConnection);
    int freeSelect = _server.getFreeSelectKeepalive();

    if (freeKeepalive < freeConnections)
      return freeSelect < freeKeepalive ? freeSelect : freeKeepalive;
    else
      return freeSelect < freeConnections ? freeSelect : freeConnections;
  }

  /**
   * Returns true if the port matches the server id.
   */
  public boolean matchesServerId(String serverId)
  {
    return getServerId().equals("*") || getServerId().equals(serverId);
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

    if (_throttle == null)
      _throttle = new Throttle();

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
      _serverSocket = QJniServerSocket.create(_socketAddress, _port,
                                              _acceptListenBacklog);

      log.info(_protocol.getProtocolName() + " listening to " + _socketAddress.getHostName() + ":" + _serverSocket.getLocalPort());
    }
    else {
      _serverSocket = QJniServerSocket.create(_port, _acceptListenBacklog);

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
    
    if (_tcpNoDelay)
      _serverSocket.setTcpNoDelay(_tcpNoDelay);

    _serverSocket.setConnectionSocketTimeout((int) getSocketTimeout());

    if (_serverSocket.isJNI() && _server.isSelectManagerEnabled()) {
      _selectManager = _server.getSelectManager();

      if (_selectManager == null) {
        throw new IllegalStateException(L.l("Cannot load select manager"));
      }
    }

    if (_keepaliveMax < 0)
      _keepaliveMax = _server.getKeepaliveMax();

    if (_keepaliveMax < 0 && _selectManager != null)
      _keepaliveMax = _selectManager.getSelectMax();

    if (_keepaliveMax < 0)
      _keepaliveMax = 256;

    _admin.register();
  }

  /**
   * binds for the watchdog.
   */
  public QServerSocket bindForWatchdog()
    throws java.io.IOException
  {
    QServerSocket ss;

    if (_port >= 1024)
      return null;
    else if (_sslFactory instanceof JsseSSLFactory) {
      log.warning(this + " cannot bind jsse in watchdog");
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

    if (! ss.isJNI()) {
      ss.close();

      return ss;
    }

    if (_tcpNoDelay)
      ss.setTcpNoDelay(_tcpNoDelay);

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

      assert(_server != null);

      if (_server instanceof EnvironmentBean)
        Environment.addEnvironmentListener(this, ((EnvironmentBean) _server).getClassLoader());
      
      bind();
      postBind();

      enable();

      wake();

      _suspendAlarm = new Alarm(new SuspendReaper());
      _suspendAlarm.queue(_suspendReaperTimeout);

      isValid = true;
    } finally {
      if (! isValid)
        close();
    }
  }

  /**
   * Starts the port listening for new connections.
   */
  void enable()
  {
    if (_lifecycle.toActive()) {
      if (_serverSocket != null)
        _serverSocket.listen(_acceptListenBacklog);
    }
  }

  /**
   * Stops the port from listening for new connections.
   */
  void disable()
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
  TcpConnectionInfo []connectionInfo()
  {
    TcpConnection []connections;

    connections = new TcpConnection[_activeConnectionSet.size()];
    _activeConnectionSet.toArray(connections);

    long now = Alarm.getExactTime();
    TcpConnectionInfo []infoList = new TcpConnectionInfo[connections.length];

    for (int i = 0 ; i < connections.length; i++) {
      TcpConnection conn = connections[i];

      long requestTime = -1;
      long startTime = conn.getRequestStartTime();

      if (conn.isRequestActive() && startTime > 0)
        requestTime = now - startTime;

      TcpConnectionInfo info
        = new TcpConnectionInfo(conn.getId(),
                                conn.getThreadId(),
                                getAddress() + ":" + getPort(),
                                conn.getState().toString(),
                                requestTime);

      infoList[i] = info;
    }

    return infoList;
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
   *
   * @param isStart boolean to mark the first request on the thread for
   *   bookkeeping.
   */
  public boolean accept(QSocket socket)
  {
    boolean isDecrementIdle = true;

    try {
      int idleThreadCount = _idleThreadCount.incrementAndGet();

      while (_lifecycle.isActive()) {
        long now = Alarm.getCurrentTime();

        idleThreadCount = _idleThreadCount.get();

        if (_idleThreadMax < idleThreadCount
            && _idleThreadCount.compareAndSet(idleThreadCount,
                                              idleThreadCount - 1)) {
          isDecrementIdle = false;
          _idleCloseExpire = now + _idleCloseTimeout;

          return false;
        }

        Thread.interrupted();
        if (_serverSocket.accept(socket)) {
          if (_throttle.accept(socket))
            return true;
          else
            socket.close();
        }
      }
    } catch (Throwable e) {
      if (_lifecycle.isActive() && log.isLoggable(Level.FINER))
        log.log(Level.FINER, e.toString(), e);
    } finally {
      if (isDecrementIdle)
        _idleThreadCount.decrementAndGet();

      if (isStartThreadRequired()) {
        // if there are not enough idle threads, wake the manager to
        // create a new one
        wake();
      }
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
   * Registers the new connection as started
   */
  void startConnection(TcpConnection conn)
  {
    _startThreadCount.decrementAndGet();
  }

  /**
   * Marks a new thread as running.
   */
  void threadBegin(TcpConnection conn)
  {
    _threadCount.incrementAndGet();
  }

  /**
   * Marks a new thread as stopped.
   */
  void threadEnd(TcpConnection conn)
  {
    _threadCount.decrementAndGet();
  }

  /**
   * Returns true if the keepalive is allowed
   */
  public boolean allowKeepalive(long acceptStartTime)
  {
    if (! _lifecycle.isActive())
      return false;
    else if (acceptStartTime + _keepaliveTimeMax < Alarm.getCurrentTime())
      return false;
    else if (_keepaliveMax <= _keepaliveCount.get())
      return false;
    else if (_connectionMax
             <= _activeConnectionCount.get() + _minSpareConnection)
      return false;
    else
      return true;
  }

  /**
   * Marks a keepalive as starting running.  Called only from TcpConnection.
   */
  boolean keepaliveBegin(TcpConnection conn, long acceptStartTime)
  {
    if (! _lifecycle.isActive())
      return false;
    else if (_connectionMax
             <= _activeConnectionCount.get() + _minSpareConnection) {
      log.warning(conn + " failed keepalive, active-connections="
                  + _activeConnectionCount.get());

      return false;
    }
    else if (false &&
             acceptStartTime + _keepaliveTimeMax < Alarm.getCurrentTime()) {
      // #2262 - skip this check to avoid confusing the load balancer
      // the keepalive check is in allowKeepalive
      log.warning(conn + " failed keepalive, delay=" + (Alarm.getCurrentTime() - acceptStartTime));

      return false;
    }
    else if (false && _keepaliveMax <= _keepaliveCount.get()) {
      // #2262 - skip this check to avoid confusing the load balancer
      // the keepalive check is in allowKeepalive
      log.warning(conn + " failed keepalive, keepalive-max " + _keepaliveCount);

      return false;
    }

    _keepaliveCount.incrementAndGet();

    return true;
  }

  /**
   * Marks the keepalive as ending. Called only from TcpConnection.
   */
  void keepaliveEnd(TcpConnection conn)
  {
    long count = _keepaliveCount.decrementAndGet();

    if (count < 0) {
      log.warning(conn + " internal error: negative keepalive count " + count);
      Thread.dumpStack();
    }
  }

  /**
   * Starts a keepalive thread.
   */
  void keepaliveThreadBegin()
  {
    _keepaliveThreadCount.incrementAndGet();
  }

  /**
   * Ends a keepalive thread.
   */
  void keepaliveThreadEnd()
  {
    _keepaliveThreadCount.decrementAndGet();
  }

  /**
   * Reads data from a keepalive connection
   */
  boolean keepaliveThreadRead(ReadStream is)
    throws IOException
  {
    if (isClosed())
      return false;

    if (is.getBufferAvailable() > 0) {
      System.out.println("DATA:");
      return true;
    }

    long timeout = getKeepaliveTimeout();

    boolean isSelectManager = getServer().isSelectManagerEnabled();

    if (isSelectManager) {
      timeout = getBlockingTimeoutForSelect();
    }

    if (getSocketTimeout() < timeout)
      timeout = getSocketTimeout();

    if (timeout < 0)
      timeout = 0;

    // server/2l02

    keepaliveThreadBegin();

    try {
      boolean result = is.fillWithTimeout(timeout);

      return result;
    } finally {
      keepaliveThreadEnd();
    }
  }

  /**
   * Suspends the controller (for comet-style ajax)
   *
   * @return true if the connection was added to the suspend list
   */
  void suspend(TcpConnection conn)
  {
    boolean isResume = false;

    if (conn.isWake()) {
      conn.toResume();

      _threadPool.schedule(conn.getResumeTask());
    }
    else if (conn.isComet()) {
      conn.toSuspend();

      _suspendConnectionSet.add(conn);
    }
    else {
      throw new IllegalStateException(L.l("{0} suspend is not allowed because the connection is not an asynchronous connection",
                                          conn));
    }
  }

  /**
   * Remove from suspend list.
   */
  boolean detach(TcpConnection conn)
  {
    return _suspendConnectionSet.remove(conn);
  }

  /**
   * Resumes the controller (for comet-style ajax)
   */
  boolean resume(TcpConnection conn)
  {
    if (_suspendConnectionSet.remove(conn)) {
      conn.toResume();

      _threadPool.schedule(conn.getResumeTask());

      return true;
    }
    else
      return false;
  }

  /**
   * Returns true if the port is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isAfterActive();
  }

  /**
   * Find the TcpConnection based on the thread id (for admin)
   */
  public TcpConnection findConnectionByThreadId(long threadId)
  {
    ArrayList<TcpConnection> connList
      = new ArrayList<TcpConnection>(_activeConnectionSet);

    for (TcpConnection conn : connList) {
      if (conn.getThreadId() == threadId)
        return conn;
    }

    return null;
  }

  /**
   * The port thread is responsible for creating new connections.
   */
  public void runTask()
  {
    if (_lifecycle.isDestroyed())
      return;
    
    try {
      TcpConnection startConn = null;

      if (isStartThreadRequired()
          && _lifecycle.isActive()
          && _activeConnectionCount.get() <= _connectionMax) {
        startConn = _freeConn.allocate();

        if (startConn == null || startConn.isDestroyed()) {
          startConn = new TcpConnection(this, _serverSocket.createSocket());
        }
        else
          startConn._isFree = false; // XXX: validation for 4.0
      }

      if (startConn != null) {
        _startThreadCount.incrementAndGet();
        _activeConnectionCount.incrementAndGet();
        _activeConnectionSet.add(startConn);

        _threadPool.schedule(startConn.getAcceptTask());
      }
    } catch (Throwable e) {
      log.log(Level.SEVERE, e.toString(), e);
    }
  }

  /**
   * Handles the environment config phase
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the environment bind phase
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }


  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    close();
  }

  /**
   * Frees the connection.
   *
   * Called only from TcpConnection
   */
  void free(TcpConnection conn)
  {
    closeConnection(conn);

    // XXX: remove when 4.0 stable
    if (conn._isFree) {
      log.warning(conn + " double free");
      Thread.dumpStack();
      return;
    }
    conn._isFree = true;

    _freeConn.free(conn);
  }

  /**
   * Frees the connection.
   *
   * Called only from TcpConnection
   */
  void kill(TcpConnection conn)
  {
    closeConnection(conn);
  }

  /**
   * Closes the stats for the connection.
   */
  private void closeConnection(TcpConnection conn)
  {
    _activeConnectionSet.remove(conn);
    _activeConnectionCount.decrementAndGet();

    // wake the start thread
    if (isStartThreadRequired()) {
      // if there are not enough idle threads, wake the manager to
      // create a new one
      wake();
    }
  }

  /**
   * Shuts the Port down.  The server gives connections 30
   * seconds to complete.
   */
  public void close()
  {
    Environment.removeEnvironmentListener(this);

    if (! _lifecycle.toDestroy())
      return;

    if (log.isLoggable(Level.FINE))
      log.fine(this + " closing");

    super.destroy();

    Alarm suspendAlarm = _suspendAlarm;
    _suspendAlarm = null;

    if (suspendAlarm != null)
      suspendAlarm.dequeue();

    QServerSocket serverSocket = _serverSocket;
    _serverSocket = null;

    _selectManager = null;
    AbstractSelectManager selectManager = null;

    if (_server != null) {
      selectManager = _server.getSelectManager();
      _server.initSelectManager(null);
    }

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

    if (selectManager != null) {
      try {
        selectManager.close();
      } catch (Throwable e) {
      }
    }

    Set<TcpConnection> activeSet;

    synchronized (_activeConnectionSet) {
      activeSet = new HashSet<TcpConnection>(_activeConnectionSet);
    }

    for (TcpConnection conn : activeSet) {
      conn.destroy();
    }

    // wake the start thread
    wake();

    // Close the socket server socket and send some request to make
    // sure the Port accept thread is woken and dies.
    // The ping is before the server socket closes to avoid
    // confusing the threads

    // ping the accept port to wake the listening threads
    if (localPort > 0) {
      int idleCount = _idleThreadCount.get() + _startThreadCount.get();

      for (int i = 0; i < idleCount + 10; i++) {
        try {
          Socket socket = new Socket();
          InetSocketAddress addr;

          if (localAddress == null ||
              localAddress.getHostAddress().startsWith("0."))
            addr = new InetSocketAddress("127.0.0.1", localPort);
          else
            addr = new InetSocketAddress(localAddress, localPort);

          socket.connect(addr, 100);

          socket.close();
        } catch (ConnectException e) {
        } catch (Throwable e) {
          log.log(Level.FINEST, e.toString(), e);
        }
      }
    }

    TcpConnection conn;
    while ((conn = _freeConn.allocate()) != null) {
      conn.destroy();
    }

    log.finest(this + " closed");
  }

  public String toURL()
  {
    return _url;
  }

  @Override
  protected String getThreadName()
  {
    return "resin-port-" + getAddress() + ":" + getPort();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "]";
  }

  public class SuspendReaper implements AlarmListener {
    public void handleAlarm(Alarm alarm)
    {
      try {
        ArrayList<TcpConnection> oldList = null;

        long now = Alarm.getCurrentTime();

        synchronized (_suspendConnectionSet) {
          Iterator<TcpConnection> iter = _suspendConnectionSet.iterator();

          while (iter.hasNext()) {
            TcpConnection conn = iter.next();

            if (conn.getIdleStartTime() + _suspendTimeMax < now) {
              iter.remove();

              if (oldList == null)
                oldList = new ArrayList<TcpConnection>();

              oldList.add(conn);
            }
          }
        }

        if (oldList != null) {
          for (int i = 0; i < oldList.size(); i++) {
            TcpConnection conn = oldList.get(i);

            if (log.isLoggable(Level.FINE))
              log.fine(this + " suspend idle timeout " + conn);

            ConnectionController async = conn.getController();

            if (async != null)
              async.timeout();

            conn.destroy();
          }
        }
      } finally {
        if (! isClosed())
          alarm.queue(_suspendReaperTimeout);
      }
    }
  }
}
