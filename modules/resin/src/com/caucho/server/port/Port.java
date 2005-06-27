/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.*;
import java.net.*;

import java.util.ArrayList;

import java.util.logging.*;

import java.nio.channels.Selector;

import javax.management.ObjectName;

import com.caucho.util.L10N;
import com.caucho.util.FreeList;
import com.caucho.util.ThreadPool;
import com.caucho.util.Alarm;

import com.caucho.log.Log;

import com.caucho.vfs.*;
import com.caucho.vfs.SSLFactory;
import com.caucho.vfs.JsseSSLFactory;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.WeakCloseListener;
import com.caucho.loader.EnvironmentBean;

import com.caucho.config.ConfigException;

import com.caucho.config.types.Period;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.jmx.Jmx;

import com.caucho.server.port.mbean.PortMBean;

/**
 * Represents a protocol connection.
 */
public class Port implements EnvironmentListener, PortMBean, Runnable {
  private static final L10N L = new L10N(Port.class);
  
  private static final Logger log = Log.open(Port.class);

  private static final long CLOSE_INTERVAL = 1000L;

  // started at 128, but that seems wasteful since the active threads
  // themselves are buffering the free connections
  private FreeList<TcpConnection> _freeConn
    = new FreeList<TcpConnection>(32);
  
  // The owning server
  private ProtocolDispatchServer _server;

  // The id
  private String _serverId = "";

  // The host
  private String _host;
  // The port
  private int _port;

  // The protocol
  private Protocol _protocol;

  // The SSL factory, if any
  private SSLFactory _sslFactory;

  private InetAddress _socketAddress;

  // time in milliseconds to wait on a socket read/write before giving up
  private long _readTimeout;
  private long _writeTimeout;

  // default timeout
  private long _timeout = 65000L;

  private int _connectionMax = 512;
  private int _minSpareConnection = 16;

  private int _keepaliveMax = -1;
  private long _keepaliveTimeout = 120000L;
    
  private int _minSpareListen = 5;
  private int _maxSpareListen = 10;

  private int _listenBacklog = 100;

  // The virtual host name
  private String _virtualHost;

  private boolean _tcpNoDelay = true;

  private boolean _isIgnoreClientDisconnect;

  // The JMX name
  private ObjectName _objectName;

  // the server socket
  private QServerSocket _serverSocket;

  // the selection manager
  private AbstractSelectManager _selectManager;

  private volatile int _threadCount;
  private final Object _threadCountLock = new Object();
  
  private volatile int _idleThreadCount;
  private volatile int _startThreadCount;

  private volatile long _lastThreadTime;
  
  private volatile int _connectionCount;
  
  private volatile int _keepaliveCount;
  private final Object _keepaliveCountLock = new Object();
  
  // The TcpServer thread.
  private Thread _thread;

  // True if the port has been bound
  private volatile boolean _isBound;
  
  // The port lifecycle
  private final Lifecycle _lifecycle = new Lifecycle();

  public Port()
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
  public void setServer(ProtocolDispatchServer server)
  {
    _server = server;

    if (_protocol != null)
      _protocol.setServer(server);
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

  /**
   * Returns the JMX object name.
   */
  public ObjectName getObjectName()
  {
    return _objectName;
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
   * Sets the host.
   */
  public void setHost(String host)
    throws UnknownHostException
  {
    if ("*".equals(host))
      host = null;
    
    _host = host;
    if (host != null)
      _socketAddress = InetAddress.getByName(host);
  }

  /**
   * Gets the host.
   */
  public String getHost()
  {
    return _host;
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
    if (minSpare < 1)
      throw new ConfigException(L.l("min-spare-listen must be at least 1."));
    
    _minSpareListen = minSpare;
  }

  /**
   * Sets the maximum spare listen.
   */
  public void setMaxSpareListen(int maxSpare)
    throws ConfigException
  {
    if (maxSpare < 1)
      throw new ConfigException(L.l("max-spare-listen must be at least 1."));
    
    _maxSpareListen = maxSpare;
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
   * Sets the socket's listen property
   */
  public void setSocketListenBacklog(int backlog)
  {
    _listenBacklog = backlog;
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
  public void setTimeout(Period period)
  {
    _timeout = period.getPeriod();
  }

  /**
   * Sets the read timeout for the accepted sockets.
   */
  public void setReadTimeout(Period period)
  {
    _readTimeout = period.getPeriod();
  }

  /**
   * Gets the read timeout for the accepted sockets.
   */
  public long getReadTimeout()
  {
    return _readTimeout;
  }

  /**
   * Sets the write timeout for the accepted sockets.
   */
  public void setWriteTimeout(Period period)
  {
    _writeTimeout = period.getPeriod();
  }

  /**
   * Gets the write timeout for the accepted sockets.
   */
  public long getWriteTimeout()
  {
    return _writeTimeout;
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

  /**
   * Sets the keepalive max.
   */
  public void setKeepaliveMax(int max)
  {
    if (_server != null && _server.getKeepaliveMax() < max) {
      max = _server.getKeepaliveMax();
      log.config(L.l("keepalive limited to {0}", max));
    }

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
   * Returns the number of keepalive connections
   */
  public int getKeepaliveCount()
  {
    return _keepaliveCount;
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
   * Initializes the port.
   */
  public void init()
    throws ConfigException
  {
    if (! _lifecycle.toInit())
      return;

    if (_server instanceof EnvironmentBean)
      Environment.addEnvironmentListener(this, ((EnvironmentBean) _server).getClassLoader());

    if (_readTimeout <= 0)
      _readTimeout = _timeout;
    
    if (_writeTimeout <= 0)
      _writeTimeout = _timeout;
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
    
    try {
      if (_host == null)
	_objectName = Jmx.getObjectName("type=Port,port=" + _port);
      else
	_objectName = Jmx.getObjectName("type=Port,port=" + _port +",host=" + _host);

      Jmx.register(this, _objectName);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (_serverSocket != null) {
      if (_port == 0) {
      }
      else if (_host != null)
	log.info("listening to " + _host + ":" + _port);
      else
	log.info("listening to " + _port);
    }
    else if (_sslFactory != null && _socketAddress != null) {
      _serverSocket = _sslFactory.create(_socketAddress, _port);
      
      log.info(_protocol.getProtocolName() + "s listening to " + _socketAddress.getHostName() + ":" + _port);
    }
    else if (_sslFactory != null) {
      if (_host == null) {
	_serverSocket = _sslFactory.create(null, _port);
	log.info(_protocol.getProtocolName() + "s listening to *:" + _port);
      }
      else {
	InetAddress addr = InetAddress.getByName(_host);
	
	_serverSocket = _sslFactory.create(addr, _port);

	log.info(_protocol.getProtocolName() + "s listening to " + _host + ":" + _port);
      }
    }
    else if (_socketAddress != null) {
      _serverSocket = QJniServerSocket.create(_socketAddress, _port,
					      _listenBacklog);

      log.info(_protocol.getProtocolName() + " listening to " + _socketAddress.getHostName() + ":" + _port);
    }
    else {
      _serverSocket = QJniServerSocket.create(_port, _listenBacklog);

      log.info(_protocol.getProtocolName() + " listening to *:" + _port);
    }

    if (_tcpNoDelay)
      _serverSocket.setTcpNoDelay(_tcpNoDelay);

    _serverSocket.setConnectionReadTimeout((int) _readTimeout);
    _serverSocket.setConnectionWriteTimeout((int) _writeTimeout);
  }

  /**
   * Starts the port listening.
   */
  public void start()
    throws Throwable
  {
    if (! _lifecycle.toActive())
      return;

    try {
      bind();

      if (_serverSocket.isJNI()) {
	_selectManager = _server.getSelectManager();

	if (_selectManager == null) {
	  throw new IllegalStateException();
	}
      }

      if (_keepaliveMax < 0)
	_keepaliveMax = _server.getKeepaliveMax();

      if (_keepaliveMax < 0)
	_keepaliveMax = 256;

      String name = "resin-port-" + _serverSocket.getLocalPort();
      _thread = new Thread(this, name);
      _thread.setDaemon(true);

      _thread.start();
    } catch (Throwable e) {
      close();
      
      log.log(Level.WARNING, e.toString(), e);

      throw e;
    }
  }

  /**
   * Returns the total connections.
   */
  public int getTotalConnectionCount()
  {
    return _connectionCount;
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
    return _keepaliveCount;
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
	    System.err.println("StartCount: " + _startThreadCount + " " + conn);
	    Thread.dumpStack();
	  }
	}
	
	if (_maxSpareListen < _idleThreadCount) {
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
	  if (_maxSpareListen < _idleThreadCount) {
	    return false;
	  }
	}
      }
    } catch (Throwable e) {
      if (_lifecycle.isActive() && log.isLoggable(Level.FINER))
	log.log(Level.FINER, e.toString(), e);
    } finally {
      boolean newConn = false;
      
      synchronized (this) {
	_idleThreadCount--;
	
	if (_idleThreadCount + _startThreadCount < _minSpareListen) {
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
   * Tries to mark the connection as a keepalive connection
   */
  void keepalive(TcpConnection conn)
  {
    if (! keepaliveBegin(conn)) {
      conn.free();
    }
    else if (_selectManager != null) {
      if (! _selectManager.keepalive(conn)) {
	keepaliveEnd(conn);
      }
    }
    else {
      conn.setKeepalive();
      ThreadPool.schedule(conn);
    }
  }

  /**
   * Marks a keepalive as starting running.
   */
  private boolean keepaliveBegin(TcpConnection conn)
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
   * Marks the keepalive as ending
   */
  void keepaliveEnd(TcpConnection conn)
  {
    synchronized (_keepaliveCountLock) {
      _keepaliveCount--;

      if (_keepaliveCount < 0) {
	log.warning("internal error: negative keepalive count " + _keepaliveCount + " in Port.keepaliveEnd().");
	_keepaliveCount = 0;
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
      boolean isStart = false;

      try {
	// need delay to avoid spawing too many threads over a short time,
	// when the load doesn't justify it
	Thread.yield();
	Thread.sleep(10);
	  
	synchronized (this) {
	  isStart = _startThreadCount + _idleThreadCount < _minSpareListen;
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

	  ThreadPool.schedule(conn);
	}
      } catch (Throwable e) {
	e.printStackTrace();
      }
    }
    
    _thread = null;
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
   */
  void free(TcpConnection conn)
  {
    closeConnection(conn);
    
    _freeConn.free(conn);
  }

  /**
   * Frees the connection.
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
      if (_connectionCount == _connectionMax) {
	try {
	  notify();
	} catch (Throwable e) {
	}
      }
      
      _connectionCount--;
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
      _server.setSelectManager(null);
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
    
    try {
      if (_objectName != null)
	Jmx.unregister(_objectName);
    } catch (Throwable e) {
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
	  Socket socket;

	  if (localAddress == null ||
	      localAddress.getHostAddress().startsWith("0."))
	    socket = new Socket("127.0.0.1", localPort);
	  else
	    socket = new Socket(localAddress, localPort);

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
    return "Port[" + getHost() + ":" + getPort() + "]";
  }
}
