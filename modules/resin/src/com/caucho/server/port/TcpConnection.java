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

import com.caucho.loader.Environment;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.TcpConnectionMXBean;
import com.caucho.server.connection.*;
import com.caucho.util.Alarm;
import com.caucho.util.ThreadPool;
import com.caucho.util.ThreadTask;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A protocol-independent TcpConnection.  TcpConnection controls the
 * TCP Socket and provides buffered streams.
 *
 * <p>Each TcpConnection has its own thread.
 */
public class TcpConnection extends PortConnection implements ThreadTask
{
  private static final Logger log
    = Logger.getLogger(TcpConnection.class.getName());

  private final QSocket _socket;

  private boolean _isInUse;
  private boolean _isActive;
  private boolean _isClosed;

  private boolean _isKeepalive;
  private boolean _isResume;
  private boolean _isDead;

  private final Object _requestLock = new Object();

  private final String _id;
  private String _name;

  private boolean _isSecure; // port security overrisde

  private final Admin _admin = new Admin();
  
  private String _state = "unknown";
  private long _startTime;
  private Thread _thread;

  /**
   * Creates a new TcpConnection.
   *
   * @param server The TCP server controlling the connections
   * @param request The protocol Request
   */
  TcpConnection(Port port, QSocket socket)
  {
    setPort(port);

    _isSecure = port.isSecure();

    int id = getId();

    if (port.getAddress() == null) {
      _id = "resin-tcp-connection-*:" + port.getPort() + "-" + id;
      _name = "INADDR_ANY-" + port.getPort() + "-" + id;
    }
    else {
      _id = ("resin-tcp-connection-" + port.getAddress() + ":" +
             port.getPort() + "-" + id);
      _name = port.getAddress() + "-" + port.getPort() + "-" + id;
    }

    _socket = socket;
  }

  /**
   * Returns the object name for jmx.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Initialize the socket.
   */
  public void initSocket()
    throws IOException
  {
    _isClosed = false;
    _isInUse = true;
    _isKeepalive = false;

    getWriteStream().init(_socket.getStream());
    getReadStream().init(_socket.getStream(), getWriteStream());

    if (log.isLoggable(Level.FINE)) {
      Port port = getPort();

      if (port != null)
	log.fine("starting connection " + this + ", total=" + port.getConnectionCount());
      else
	log.fine("starting connection " + this);
    }
  }

  /**
   * Returns the connection's socket
   */
  public QSocket getSocket()
  {
    return _socket;
  }

  /**
   * Returns the connection's socket
   */
  public QSocket startSocket()
  {
    _isClosed = false;
    
    return _socket;
  }

  /**
   * Try to read nonblock
   */
  private boolean waitForKeepalive()
    throws IOException
  {
    Port port = getPort();
    QSocket socket = _socket;
    
    if (port.isClosed())
      return false;

    ReadStream is = getReadStream();
    
    if (getReadStream().getBufferAvailable() > 0)
      return true;
    
    long timeout = port.getKeepaliveTimeout();

    boolean isSelectManager = port.getServer().isEnableSelectManager();
    
    if (isSelectManager) {
      timeout = port.getKeepaliveSelectThreadTimeout();
    }

    if (timeout > 0 && timeout < port.getSocketTimeout())
      return is.fillWithTimeout(timeout);
    else if (isSelectManager)
      return false;
    else
      return true;
  }

  public boolean isSecure()
  {
    if (_isClosed)
      return false;
    else
      return _socket.isSecure() || _isSecure;
  }

  /**
   * Returns true for closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  /**
   * Set true for active.
   */
  public void setActive(boolean isActive)
  {
    _isActive = isActive;
  }

  /**
   * Returns true for active.
   */
  public boolean isActive()
  {
    return _isActive;
  }

  /**
   * Sets the keepalive state.  Called only by the SelectManager and Port.
   */
  public void setKeepalive()
  {
    if (_isKeepalive)
      log.warning("illegal state: setting keepalive with active keepalive: " + this);
    
    _isKeepalive = true;
  }

  /**
   * Sets the keepalive state.  Called only by the SelectManager and Port.
   */
  public void clearKeepalive()
  {
    if (! _isKeepalive)
      log.warning("illegal state: clearing keepalive with inactive keepalive: " + this);
    
    _isKeepalive = false;
  }

  /**
   * Returns the local address of the socket.
   */
  public InetAddress getLocalAddress()
  {
    // The extra cases handle Kaffe problems.
    try {
      return _socket.getLocalAddress();
    } catch (Exception e) {
      try {
	return InetAddress.getLocalHost();
      } catch (Exception e1) {
	try {
	  return InetAddress.getByName("127.0.0.1");
	} catch (Exception e2) {
	  return null;
	}
      }
    }
  }

  /**
   * Returns the socket's local TCP port.
   */
  public int getLocalPort()
  {
    return _socket.getLocalPort();
  }

  /**
   * Returns the socket's remote address.
   */
  public InetAddress getRemoteAddress()
  {
    return _socket.getRemoteAddress();
  }

  /**
   * Returns the socket's remote host name.
   */
  public String getRemoteHost()
  {
    return _socket.getRemoteHost();
  }

  /**
   * Adds from the socket's remote address.
   */
  public int getRemoteAddress(byte []buffer, int offset, int length)
  {
    return _socket.getRemoteAddress(buffer, offset, length);
  }

  /**
   * Returns the socket's remote port
   */
  public int getRemotePort()
  {
    return _socket.getRemotePort();
  }

  /**
   * Returns the virtual host.
   */
  public String getVirtualHost()
  {
    return getPort().getVirtualHost();
  }

  /**
   * Returns the state string.
   */
  public final String getState()
  {
    return _state;
  }

  /**
   * Sets the state string.
   */
  public final void setState(String state)
  {
    _state = state;
  }

  /**
   * Begins an active connection.
   */
  public final void beginActive()
  {
    _state = "active";
    _startTime = Alarm.getCurrentTime();
  }

  /**
   * Ends an active connection.
   */
  public final void endActive()
  {
    _state = "idle";
    _startTime = 0;
  }

  /**
   * Returns the thread id.
   */
  public final long getThreadId()
  {
    Thread thread = _thread;

    if (thread != null)
      return thread.getId();
    else
      return -1;
  }

  /**
   * Returns the time the current request has taken.
   */
  public final long getRequestActiveTime()
  {
    if (_startTime > 0)
      return Alarm.getCurrentTime() - _startTime;
    else
      return -1;
  }

  /**
   * Tries to mark the connection as a keepalive connection
   *
   * At exit, the connection is either:
   *   1) freed (no keepalive)
   *   2) rescheduled (keepalive with new thread)
   *   3) in select pool (keepalive with poll)
   */
  private void keepalive()
  {
    Port port = getPort();
    
    ConnectionController controller = getController();
    
    if (controller != null) {
      if (port.suspend(controller)) {
        log.fine("[" + getId() + "] suspend");
      }
      else {
        log.fine("[" + getId() + "] suspend fail");
	
	free();
      }
    }
    else if (! port.keepaliveBegin(this)) {
      if (log.isLoggable(Level.FINE))
        log.fine("[" + getId() + "] failed keepalive");

      free();
    }
    else if (port.getSelectManager() != null) {
      if (! port.getSelectManager().keepalive(this)) {
        // XXX: s/b
        // setKeepalive();
        // ThreadPool.schedule(this);
        if (log.isLoggable(Level.FINE))
          log.fine("[" + getId() + "] failed keepalive (select)");

        port.keepaliveEnd(this);
	free();
      }
      else {
        if (log.isLoggable(Level.FINE))
          log.fine("[" + getId() + "] keepalive (select)");
      }
    }
    else {
      if (log.isLoggable(Level.FINE))
        log.fine("[" + getId() + "] keepalive (thread)");

      setKeepalive();
      ThreadPool.getThreadPool().schedule(this);
    }
  }

  void setResume()
  {
    _isResume = true;
  }

  /**
   * Wakes the connection (comet-style).
   */
  protected boolean wake()
  {
    ConnectionController controller = getController();

    if (controller != null) {
      _isResume = true;
      return getPort().resume(controller);
    }
    else
      return false;
  }

  /**
   * Starts the connection.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
  }
  
  /**
   * Runs as a task.
   */
  public void run()
  {
    Port port = getPort();

    boolean isKeepalive = _isKeepalive;
    _isKeepalive = false;

    boolean isResume = _isResume;
    _isResume = false;

    boolean isFirst = ! isKeepalive;
    
    ServerRequest request = getRequest();
    boolean isWaitForRead = request.isWaitForRead();
	
    Thread thread = Thread.currentThread();
    String oldThreadName = thread.getName();
		   
    thread.setName(_id);
    
    if (isKeepalive)
      port.keepaliveEnd(this);

    port.threadBegin(this);

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

    long startTime = Alarm.getExactTime();

    thread.setContextClassLoader(systemLoader);
    
    //_admin.register();

    try {
      _thread = thread;

      if (isResume) {
	ConnectionController controller = getController();

	if (request.handleResume()) {
	  isKeepalive = true;
	}
	else {
	  if (controller != null)
	    controller.close();

	  isKeepalive = false;
	}

	return;
      }
      
      while (! _isDead) {
	if (isKeepalive) {
	}
	else if (! port.accept(this, isFirst)) {
	  return;
	}

        isFirst = false;
	
	try {
	  thread.interrupted();
	  // clear the interrupted flag
	  
	  do {
	    thread.setContextClassLoader(systemLoader);

            isKeepalive = false;

	    if (! port.isClosed()
		&& (! isWaitForRead || getReadStream().waitForRead())) {

              synchronized (_requestLock) {
		isKeepalive = request.handleRequest();
	      }

	      ConnectionController controller = getController();
	      if (controller == null) {
	      }
	      else if (isKeepalive)
		return;
	      else
		controller.close();
	    }
	  } while (isKeepalive && waitForKeepalive() && ! port.isClosed());

          if (isKeepalive) {
	    return;
	  }
	  else {
	    getRequest().protocolCloseEvent();
	  }
	}
	catch (ClientDisconnectException e) {
	  isKeepalive = false;

          if (log.isLoggable(Level.FINER))
	    log.finer("[" + getId() + "] " + e);
	}
	catch (IOException e) {
	  isKeepalive = false;
	  
	  if (log.isLoggable(Level.FINE))
	    log.log(Level.FINE, "[" + getId() + "] " + e, e);
	  
        }
	finally {
	  thread.setContextClassLoader(systemLoader);

          if (! isKeepalive)
	    closeImpl();
	}
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      isKeepalive = false;
    } finally {
      thread.setContextClassLoader(systemLoader);
    
      //_admin.unregister();
      
      port.threadEnd(this);

      if (isKeepalive)
	keepalive();
      else
	free();

      _thread = null;
      thread.setName(oldThreadName);
    }
  }

  /**
   * Sends a broadcast request.
   */
  public void sendBroadcast(BroadcastTask task)
  {
    synchronized (_requestLock) {
      task.execute(this);
    }
  }

  /**
   * Closes on shutdown.
   */
  public void closeOnShutdown()
  {
    QSocket socket = _socket;

    if (socket != null) {
      try {
        socket.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }

      Thread.currentThread().yield();
    }
  }

  /**
   * Closes the connection.
   */
  private void closeImpl()
  {
    QSocket socket = _socket;

    boolean isClosed;

    synchronized (this) {
      isClosed = _isClosed;
      _isClosed = true;
    }

    ConnectionController controller = getController();
    if (controller != null)
      controller.close();

    if (! isClosed) {
      _isActive = false;
      boolean isKeepalive = _isKeepalive;
      _isKeepalive = false;

      Port port = getPort();

      if (isKeepalive)
	port.keepaliveEnd(this);
      
      if (log.isLoggable(Level.FINE) && _isInUse) {
	Object serverId = Environment.getAttribute("caucho.server-id");
	String prefix = "";

	if (serverId != null)
	  prefix = "[" + serverId + "] ";
    
	if (port != null)
	  log.fine(prefix + "closing connection " + this + ", total=" + port.getConnectionCount());
	else
	  log.fine(prefix + "closing connection " + this);
      }

      _isInUse = false;

      try {
        getWriteStream().close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }

      try {
        getReadStream().close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }

      if (socket != null) {
	try {
	  socket.close();
	} catch (Throwable e) {
	  log.log(Level.FINE, e.toString(), e);
	}

	getPort().closeSocket(socket);
      }
    }
  }
  
  /**
   * Destroys the connection()
   */
  public final void destroy()
  {
    _isDead = true;
    
    closeImpl();
  }
  
  /**
   * Frees the connection()
   */
  final void free()
  {
    closeImpl();

    setState("free");
    
    if (! _isDead)
      getPort().free(this);
    else
      getPort().kill(this);
  }

  public String toString()
  {
    if (_isActive)
      return "TcpConnection[id=" + _id + ",socket=" + _socket + ",active]";
    else
      return "TcpConnection[id=" + _id + ",socket=" + _socket + ",port=" + getPort() + "]";
  }

  class Admin extends AbstractManagedObject implements TcpConnectionMXBean {
    Admin()
    {
      super(ClassLoader.getSystemClassLoader());
    }
    
    public String getName()
    {
      return _name;
    }

    public long getThreadId()
    {
      return TcpConnection.this.getThreadId();
    }

    public long getRequestActiveTime()
    {
      return TcpConnection.this.getRequestActiveTime();
    }

    public String getState()
    {
      return TcpConnection.this.getState();
    }

    void register()
    {
      registerSelf();
    }

    void unregister()
    {
      unregisterSelf();
    }
  }
}
