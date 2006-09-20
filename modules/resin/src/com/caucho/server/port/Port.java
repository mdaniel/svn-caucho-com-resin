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

package com.caucho.server.port;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.*;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.server.cluster.*;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.log.Log;
import com.caucho.management.server.PortMXBean;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;
import com.caucho.vfs.JsseSSLFactory;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.SSLFactory;

/**
 * Represents a protocol connection.
 */
public class Port
  implements EnvironmentListener, Runnable
{
  private static final L10N L = new L10N(Port.class);

  private static final Logger log = Log.open(Port.class);

  private static final int DEFAULT = -0xcafe;

  // started at 128, but that seems wasteful since the active threads
  // themselves are buffering the free connections
  private FreeList<TcpConnection> _freeConn
    = new FreeList<TcpConnection>(32);

  // The owning server
  private ProtocolDispatchServer _server;

  // The id
  private String _serverId = "";

  // The address
  private String _address;
  // The port
  private int _port;

  // The protocol
  private Protocol _protocol;

  // The SSL factory, if any
  private SSLFactory _sslFactory;

  private InetAddress _socketAddress;

  // default timeout
  private long _socketTimeout = DEFAULT;

  private int _connectionMax = 512;
  private int _minSpareConnection = 16;

  private int _keepaliveMax = DEFAULT;
  
  private long _keepaliveThreadTimeout = DEFAULT;
  private long _keepaliveSocketTimeout = DEFAULT;

  private int _acceptThreadMin = DEFAULT;
  private int _acceptThreadMax = DEFAULT;

  private int _acceptListenBacklog = DEFAULT;

  // The virtual host name
  private String _virtualHost;

  private boolean _tcpNoDelay = true;

  private final PortAdmin _admin = new PortAdmin(this);

  // the server socket
  private QServerSocket _serverSocket;

  // the selection manager
  private AbstractSelectManager _selectManager;

  private volatile int _threadCount;
  private final Object _threadCountLock = new Object();

  private volatile int _idleThreadCount;
  private volatile int _startThreadCount;

  private volatile int _connectionCount;

  private volatile long _lifetimeRequestCount;
  private volatile long _lifetimeKeepaliveCount;
  private volatile long _lifetimeClientDisconnectCount;
  private volatile long _lifetimeRequestTime;
  private volatile long _lifetimeReadBytes;
  private volatile long _lifetimeWriteBytes;

  private volatile int _keepaliveCount;
  private final Object _keepaliveCountLock = new Object();

  // True if the port has been bound
  private volatile boolean _isBound;

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

      if (_acceptThreadMax == DEFAULT)
	_acceptThreadMax = server.getAcceptThreadMax();

      if (_acceptThreadMin == DEFAULT)
	_acceptThreadMin = server.getAcceptThreadMin();

      if (_acceptListenBacklog == DEFAULT)
	_acceptListenBacklog = server.getAcceptListenBacklog();

      if (_keepaliveMax == DEFAULT)
	_keepaliveMax = server.getKeepaliveMax();

      if (_keepaliveThreadTimeout == DEFAULT)
	_keepaliveThreadTimeout = server.getKeepaliveThreadTimeout();

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

  public PortMXBean getAdmin()
  {
    if (_server.getServerId().equals(_serverId))
      return _admin;
    else
      return null;
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
      Class cl = Class.forName("com.caucho.vfs.OpenSSLFactory");

      _sslFactory = (SSLFactory) cl.newInstance();

      return _sslFactory;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      throw new ConfigException(L.l("<openssl> requires Resin Professional.  See http://www.caucho.com for more information."));
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
   * Sets the server socket.
   */
  public void setServerSocket(QServerSocket socket)
  {
    _serverSocket = socket;
  }

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

  /**
   * Sets the minimum spare listen.
   */
  public void setAcceptThreadMin(int minSpare)
    throws ConfigException
  {
    if (minSpare < 1)
      throw new ConfigException(L.l("min-spare-listen must be at least 1."));

    _acceptThreadMin = minSpare;
  }

  /**
   * Sets the maximum spare listen.
   */
  public void setAcceptThreadMax(int maxSpare)
    throws ConfigException
  {
    if (maxSpare < 1)
      throw new ConfigException(L.l("max-spare-listen must be at least 1."));

    _acceptThreadMax = maxSpare;
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
   * Returns true for ignore-client-disconnect.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return _server.isIgnoreClientDisconnect();
  }

  /**
   * Returns the thread count.
   */
  public int getThreadCount()
  {
    return _threadCount;
  }

  /**
   * Sets the default read/write timeout for the accepted sockets.
   */
  public void setSocketTimeout(Period period)
  {
    _socketTimeout = period.getPeriod();
  }

  /**
   * Gets the read timeout for the accepted sockets.
   */
  public long getSocketTimeout()
  {
    return _socketTimeout;
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
   * Sets the write timeout for the accepted sockets.
   *
   * @deprecated
   */
  public void setWriteTimeout(Period period)
  {
  }

  /**
   * Returns the active thread count.
   */
  public int getActiveThreadCount()
  {
    return _threadCount - _idleThreadCount;
  }

  /**
   * Returns the count of idle threads.
   */
  public int getIdleThreadCount()
  {
    return _idleThreadCount;
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
   * Returns the number of connections
   */
  public int getConnectionCount()
  {
    return _connectionCount;
  }

  public long getLifetimeRequestCount()
  {
    return _lifetimeRequestCount;
  }

  public long getLifetimeKeepaliveCount()
  {
    return _lifetimeKeepaliveCount;
  }

  public long getLifetimeClientDisconnectCount()
  {
    return _lifetimeClientDisconnectCount;
  }

  public long getLifetimeRequestTime()
  {
    return _lifetimeRequestTime;
  }

  public long getLifetimeReadBytes()
  {
    return _lifetimeReadBytes;
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

  public void setKeepaliveThreadTimeout(Period period)
  {
    _keepaliveThreadTimeout = period.getPeriod();
  }

  public long getKeepaliveThreadTimeout()
  {
    return _keepaliveThreadTimeout;
  }

  /**
   * Returns the number of keepalive connections
   */
  public int getKeepaliveCount()
  {
    synchronized (_keepaliveCountLock) {
      return _keepaliveCount;
    }
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
   * Returns the accept pool.
   */
  public int getFreeKeepalive()
  {
    int freeKeepalive = _keepaliveMax - _keepaliveCount;
    int freeConnections = _connectionMax - _connectionCount - _minSpareConnection;
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

    if (_server instanceof EnvironmentBean)
      Environment.addEnvironmentListener(this, ((EnvironmentBean) _server).getClassLoader());
  }

  /**
   * Starts the port listening.
   */
  public void bind()
    throws Exception
  {
    synchronized (this) {
      if (_isBound)
        return;
      _isBound = true;
    }

    if (_protocol == null)
      throw new IllegalStateException(L.l("`{0}' must have a configured protocol before starting.", this));

    if (_port == 0)
      return;
    
    if (_serverSocket != null) {
      if (_port == 0) {
      }
      else if (_address != null)
        log.info("listening to " + _address + ":" + _port);
      else
        log.info("listening to " + _port);
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

      log.info(_protocol.getProtocolName() + " listening to " + _socketAddress.getHostName() + ":" + _port);
    }
    else {
      _serverSocket = QJniServerSocket.create(_port, _acceptListenBacklog);

      log.info(_protocol.getProtocolName() + " listening to *:" + _port);
    }

    if (_tcpNoDelay)
      _serverSocket.setTcpNoDelay(_tcpNoDelay);

    _serverSocket.setConnectionSocketTimeout((int) getSocketTimeout());
  }

  /**
   * Starts the port listening.
   */
  public void bind(QServerSocket ss)
    throws Exception
  {
    if (ss == null)
      throw new NullPointerException();

    _isBound = true;

    if (_protocol == null)
      throw new IllegalStateException(L.l("`{0}' must have a configured protocol before starting.", this));

    _admin.register();

    _serverSocket = ss;

    String scheme = _protocol.getProtocolName();
    
    if (_address != null)
      log.info(scheme + " listening to " + _address + ":" + _port);
    else
      log.info(scheme + " listening to *:" + _port);

    if (_sslFactory != null) {
      throw new UnsupportedOperationException();
    }

    if (_tcpNoDelay)
      _serverSocket.setTcpNoDelay(_tcpNoDelay);

    _serverSocket.setConnectionSocketTimeout((int) getSocketTimeout());
  }

  /**
   * binds for the watchdog.
   */
  public QServerSocket bindForWatchdog()
    throws java.io.IOException
  {
    QServerSocket ss;
    
    if (_socketAddress != null) {
      ss = QJniServerSocket.createJNI(_socketAddress, _port,
				      _acceptListenBacklog);

      if (ss == null)
	return null;

      log.fine("watchdog binding to " + _socketAddress.getHostName() + ":" + _port);
    }
    else {
      ss = QJniServerSocket.createJNI(null, _port, _acceptListenBacklog);

      if (ss == null)
	return null;

      log.fine("watchdog binding to *:" + _port);
    }

    if (! ss.isJNI()) {
      ss.close();

      return ss;
    }

    if (_tcpNoDelay)
      ss.setTcpNoDelay(_tcpNoDelay);

    ss.setConnectionSocketTimeout((int) getSocketTimeout());

    _admin.register();

    return ss;
  }  

  /**
   * Starts the port listening.
   */
  public void start()
    throws Throwable
  {
    if (_port == 0)
      return;
    
    if (! _lifecycle.toActive())
      return;

    try {
      bind();

      _admin.register();

      if (_serverSocket.isJNI() && _server.isEnableSelectManager()) {
        _selectManager = _server.getSelectManager();

        if (_selectManager == null) {
          throw new IllegalStateException(L.l("Cannot load select manager"));
        }
      }

      if (_keepaliveMax < 0)
        _keepaliveMax = _server.getKeepaliveMax();

      if (_keepaliveMax < 0)
        _keepaliveMax = 256;

      String name = "resin-port-" + _serverSocket.getLocalPort();
      Thread thread = new Thread(this, name);
      thread.setDaemon(true);

      thread.start();
    } catch (Throwable e) {
      close();

      log.log(Level.WARNING, e.toString(), e);

      throw e;
    }
  }

  /**
   * Returns the active connections.
   */
  public int getActiveConnectionCount()
  {
    return _threadCount - _idleThreadCount;
  }

  /**
   * Returns the keepalive connections.
   */
  public int getKeepaliveConnectionCount()
  {
    return getKeepaliveCount();
  }

  /**
   * returns the select manager.
   */
  public AbstractSelectManager getSelectManager()
  {
    return _selectManager;
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
   * Accepts a new connection.
   */
  public boolean accept(TcpConnection conn, boolean isFirst)
  {
    try {
      synchronized (this) {
        _idleThreadCount++;

        if (isFirst) {
          _startThreadCount--;

          if (_startThreadCount < 0) {
            Thread.dumpStack();
          }
        }

        if (_acceptThreadMax < _idleThreadCount) {
          return false;
        }
      }

      while (_lifecycle.isActive()) {
        QSocket socket = conn.startSocket();

        Thread.interrupted();
        if (_serverSocket.accept(socket)) {
          conn.initSocket();

          return true;
        }
        else {
          if (_acceptThreadMax < _idleThreadCount) {
            return false;
          }
        }
      }
    } catch (Throwable e) {
      if (_lifecycle.isActive() && log.isLoggable(Level.FINER))
        log.log(Level.FINER, e.toString(), e);
    } finally {
      synchronized (this) {
        _idleThreadCount--;

        if (_idleThreadCount + _startThreadCount < _acceptThreadMin) {
          notify();
        }
      }
    }

    return false;
  }

  /**
   * Registers the new connection as started
   */
  void startConnection(TcpConnection conn)
  {
    synchronized (this) {
      _startThreadCount--;
    }
  }

  /**
   * Marks a new thread as running.
   */
  void threadBegin(TcpConnection conn)
  {
    synchronized (_threadCountLock) {
      _threadCount++;
    }
  }

  /**
   * Marks a new thread as stopped.
   */
  void threadEnd(TcpConnection conn)
  {
    synchronized (_threadCountLock) {
      _threadCount--;
    }
  }

  /**
   * Marks a keepalive as starting running.  Called only from TcpConnection.
   */
  boolean keepaliveBegin(TcpConnection conn)
  {
    synchronized (_keepaliveCountLock) {
      if (_keepaliveMax <= _keepaliveCount)
        return false;
      else if (_connectionMax <= _connectionCount + _minSpareConnection)
        return false;

      _keepaliveCount++;

      return true;
    }
  }

  /**
   * Marks the keepalive as ending. Called only from TcpConnection.
   */
  void keepaliveEnd(TcpConnection conn)
  {
    synchronized (_keepaliveCountLock) {
      _keepaliveCount--;

      if (_keepaliveCount < 0) {
        int count = _keepaliveCount;
        _keepaliveCount = 0;

        log.warning("internal error: negative keepalive count " + count);
      }
    }
  }

  /**
   * Returns true if the port is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isAfterActive();
  }

  /**
   * The port thread is responsible for creating new connections.
   */
  public void run()
  {
    while (_lifecycle.isActive()) {
      boolean isStart;

      try {
        // need delay to avoid spawing too many threads over a short time,
        // when the load doesn't justify it
        Thread.yield();
        Thread.sleep(10);

        synchronized (this) {
          isStart = _startThreadCount + _idleThreadCount < _acceptThreadMin;
          if (_connectionMax <= _connectionCount)
            isStart = false;

          if (! isStart) {
            Thread.interrupted();
            wait(60000);
          }

          if (isStart) {
            _connectionCount++;
            _startThreadCount++;
          }
        }

        if (isStart && _lifecycle.isActive()) {
          TcpConnection conn = _freeConn.allocate();
          if (conn == null) {
            conn = new TcpConnection(this, _serverSocket.createSocket());
            conn.setRequest(_protocol.createRequest(conn));
          }

	  conn.start();

          ThreadPool.getThreadPool().schedule(conn);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
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
    synchronized (this) {
      if (_connectionCount-- == _connectionMax) {
        try {
          notify();
        } catch (Throwable e) {
        }
      }
    }
  }

  /**
   * Shuts the TcpServer down.  The server gives connections 30
   * seconds to complete.
   */
  public void close()
  {
    Environment.removeEnvironmentListener(this);

    if (! _lifecycle.toDestroy())
      return;

    if (log.isLoggable(Level.FINE))
      log.fine("closing " + this);

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

    /*
    ArrayList<TcpConnection> connections = new ArrayList<TcpConnection>();
    synchronized (this) {
      connections.addAll(_activeConnections);
    }
    */

    // Close the socket server socket and send some request to make
    // sure the Port accept thread is woken and dies.
    // The ping is before the server socket closes to avoid
    // confusing the threads

    // ping the accept port to wake the listening threads
    if (localPort > 0) {
      int idleCount = _idleThreadCount + _startThreadCount;

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

    log.finest("closed " + this);
  }

  public String toString()
  {
    return "Port[" + getAddress() + ":" + getPort() + "]";
  }
}
