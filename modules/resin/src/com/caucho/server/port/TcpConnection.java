/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is finishThread software; you can redistribute it and/or modify
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

import com.caucho.server.connection.*;
import com.caucho.loader.Environment;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.TcpConnectionMXBean;
import com.caucho.server.connection.*;
import com.caucho.server.resin.Resin;
import com.caucho.util.Alarm;
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
public class TcpConnection extends Connection
{
  private static final Logger log
    = Logger.getLogger(TcpConnection.class.getName());
  
  private static int _connectionCount;

  private int _connectionId;  // The connection's id
  private final String _id;
  private String _dbgId;
  private String _name;

  private final Port _port;
  private final QSocket _socket;
  
  private final AcceptTask _acceptTask = new AcceptTask();
  private final KeepaliveTask _keepaliveTask = new KeepaliveTask();
  private final ResumeTask _resumeTask = new ResumeTask();
  
  private ServerRequest _request;

  private boolean _isSecure; // port security overrisde
  
  private final Admin _admin = new Admin();
  private final Object _requestLock = new Object();

  private ConnectionState _state = ConnectionState.IDLE;
  
  private boolean _isActive;
  private boolean _isClosed;

  private boolean _isKeepalive;
  private boolean _isWake;
  
  private Runnable _readTask = _acceptTask;
  
  private long _idleTimeMax;
  
  private long _accessTime;   // Time the current request started
  private long _connectionStartTime;
  private long _keepaliveStartTime;
  
  private long _keepaliveExpireTime;
  private long _requestStartTime;
  private long _suspendTime;
  
  private Thread _thread;

  public boolean _isFree;

  private Exception _startThread;

  /**
   * Creates a new TcpConnection.
   *
   * @param server The TCP server controlling the connections
   * @param request The protocol Request
   */
  TcpConnection(Port port, QSocket socket)
  {
    synchronized (TcpConnection.class) {
      _connectionId = _connectionCount++;
    }
    
    _port = port;
    _socket = socket;
    
    _isSecure = port.isSecure();

    int id = getId();

    String protocol = port.getProtocol().getProtocolName();

    if (port.getAddress() == null) {
      Resin resin = Resin.getLocal();
      String serverId = resin != null ? resin.getServerId() : null;
      if (serverId == null)
	serverId = "";
      
      _id = protocol + "-" + serverId + "-" + port.getPort() + "-" + id;
      _name = protocol + "-" + port.getPort() + "-" + id;
    }
    else {
      _id = (protocol + "-" + port.getAddress() + ":" +
             port.getPort() + "-" + id);
      _name = (protocol + "-" + port.getAddress() + "-" +
	       port.getPort() + "-" + id);
    }
  }

  public void setStartThread()
  {
    _startThread = new Exception();
    _startThread.fillInStackTrace();
  }

  public Exception getStartThread()
  {
    return _startThread;
  }

  /**
   * Sets the time of the request start.  ServerRequests can use
   * setAccessTime() to put off connection reaping.  HttpRequest calls
   * setAccessTime() at the beginning of each request.
   *
   * @param now the current time in milliseconds as by Alarm.getCurrentTime().
   */
  public void setAccessTime(long now)
  {
    _accessTime = now;
  }

  /**
   * Returns the time the last Request began in milliseconds.
   */
  public long getAccessTime()
  {
    return _accessTime;
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
   * Returns the connection id.  Primarily for debugging.
   */
  public int getId()
  {
    return _connectionId;
  }

  /**
   * Returns true for closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  /**
   * Sets the idle time for a keepalive connection.
   */
  public void setIdleTimeMax(long idleTime)
  {
    _idleTimeMax = idleTime;
  }
  
  /**
   * The idle time for a keepalive connection
   */
  public long getIdleTimeMax()
  {
    return _idleTimeMax;
  }

  /**
   * Sets the keepalive state.  Called only by the SelectManager and Port.
   */
  public void setKeepalive()
  {
    if (_isKeepalive)
      log.warning(this + " illegal state: setting keepalive with active keepalive: ");
    
    _isKeepalive = true;
  }

  /**
   * Sets the keepalive state.  Called only by the SelectManager and Port.
   */
  public void clearKeepalive()
  {
    if (! _isKeepalive)
      log.warning(this + " illegal state: clearing keepalive with inactive keepalive");
    
    _isKeepalive = false;
  }

  /**
   * Returns the keepalive expire time.
   */
  public long getKeepaliveExpireTime()
  {
    return _keepaliveExpireTime;
  }
  
  /**
   * Returns the keepalive start time
   */
  public long getKeepaliveStartTime()
  {
    return _keepaliveStartTime;
  }

  /**
   * Returns the object name for jmx.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the port which generated the connection.
   */
  public Port getPort()
  {
    return _port;
  }
 
  /**
   * Returns the current read task
   */
  public Runnable getReadTask()
  {
    if (_state.isClosed())
      Thread.dumpStack();
    setStartThread();
    return _readTask;
  }
 
  /**
   * Returns the current write task
   */
  public Runnable getResumeTask()
  {
    return _resumeTask;
  }

  /**
   * Returns the request for the connection.
   */
  public final ServerRequest getRequest()
  {
    return _request;
  }

  /**
   * Sets the connection's request.
   */
  public final void setRequest(ServerRequest request)
  {
    _request = request;
  }

  @Override
  public boolean isSecure()
  {
    if (_isClosed)
      return false;
    else
      return _socket.isSecure() || _isSecure;
  }
  
  /**
   * Returns the connection's socket
   */
  public QSocket getSocket()
  {
    return _socket;
  }

  /**
   * Returns the time the comet suspend started
   */
  public long getSuspendTime()
  {
    return _suspendTime;
  }
 
  /**
   * Initialize the socket for a new connection
   */
  public void initSocket()
    throws IOException
  {
    _idleTimeMax = _port.getKeepaliveTimeout();

    getWriteStream().init(_socket.getStream());
    getReadStream().init(_socket.getStream(), getWriteStream());

    if (log.isLoggable(Level.FINE)) {
      log.fine(dbgId() + "starting connection " + this + ", total=" + _port.getConnectionCount());
    }
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
  @Override
  public String getRemoteHost()
  {
    return _socket.getRemoteHost();
  }

  /**
   * Adds from the socket's remote address.
   */
  @Override
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
   * Returns the state string.
   */
  public final String getState()
  {
    return _state.toString();
  }

  /**
   * Returns the virtual host.
   */
  @Override
  public String getVirtualHost()
  {
    return getPort().getVirtualHost();
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
    if (_requestStartTime > 0)
      return Alarm.getCurrentTime() - _requestStartTime;
    else
      return -1;
  }

  void setResume()
  {
    _isWake = false;
    _suspendTime = 0;
  }

  void setWake()
  {
    _isWake = true;
  }

  boolean isWake()
  {
    return _isWake;
  }

  boolean isComet()
  {
    return _state.isComet();
  }

  /**
   * Begins an active connection.
   */
  public final void beginActive()
  {
    _state = _state.toActive();
    _requestStartTime = Alarm.getCurrentTime();
  }

  /**
   * Ends an active connection.
   */
  public final void endActive()
  {
    _requestStartTime = 0;
  }

  public boolean toKeepalive()
  {
    if (! _isKeepalive) {
      _isKeepalive = _port.allowKeepalive(_connectionStartTime);
    }
    
    return _isKeepalive;
  }

  /**
   * Starts the connection.
   */
  public void start()
  {
  }
  
  RequestState handleConnection()
  {
    Thread thread = Thread.currentThread();
    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
 
    boolean isValid = false;
    RequestState result = RequestState.EXIT;

   try {
     // clear the interrupted flag
     Thread.interrupted();
     
     boolean isKeepalive;
     do {
       thread.setContextClassLoader(systemLoader);

       _state = _state.toActive();

       if (_port.isClosed()) {
         return RequestState.EXIT;
       }

       result = RequestState.EXIT;
       synchronized (_requestLock) {
         isKeepalive = getRequest().handleRequest();
       }
       
       if (_state == ConnectionState.DUPLEX) {
	 isValid = true;
	 _readTask.run();

	 return RequestState.THREAD_DETACHED;
       }
     } while (isKeepalive
	      && (result = keepaliveRead()) == RequestState.REQUEST);

     isValid = true;

     return result;
   } catch (ClientDisconnectException e) {
      if (log.isLoggable(Level.FINER)) {
        log.finer(dbgId() + e);
      }
    } catch (IOException e) {
      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, dbgId() + e, e);
      }
    } finally {
      thread.setContextClassLoader(systemLoader);

      if (! isValid)
        destroy();
    }
    
    return RequestState.EXIT;
  }
 
  /**
   * Starts a keepalive, either returning available data or
   * returning false to close the loop
   *
   * If keepaliveRead() returns true, data is available.
   * If it returns false, either the connection is closed,
   * or the connection has been registered with the select.
   */
  private RequestState keepaliveRead()
    throws IOException
  {
    if (waitForKeepalive())
      return RequestState.REQUEST;
    
    Port port = _port;
    
    _suspendTime = Alarm.getCurrentTime();
    _keepaliveExpireTime = _suspendTime + _idleTimeMax;
    
    if (! _port.keepaliveBegin(this, _connectionStartTime)) {
      close();
      
      return RequestState.EXIT;
    }
    
    _state = _state.toKeepaliveSelect();

    if (_port.getSelectManager() != null) {
      if (_port.getSelectManager().keepalive(this)) {
        if (log.isLoggable(Level.FINE))
          log.fine(dbgId() + "keepalive (select)");

	return RequestState.THREAD_DETACHED;
      }
      else {
 	log.warning(dbgId() + "failed keepalive (select)");

	_state = _state.toActive();
        port.keepaliveEnd(this);
	close();
	
	return RequestState.EXIT;
      }
    }
    else {
      if (log.isLoggable(Level.FINE))
        log.fine(dbgId() + "keepalive (thread)");
      
      if (getReadStream().waitForRead()) {
        port.keepaliveEnd(this);

	_state = _state.toActive();
      
        return RequestState.REQUEST;
      }
      else {
	_state = _state.toActive();
        port.keepaliveEnd(this);
        
        close();
          
        return RequestState.EXIT;
      }
    }
  }

  /**
   * Try to read nonblock
   */
  private boolean waitForKeepalive()
    throws IOException
  {
    if (_state.isClosed())
      return false;
    
    Port port = getPort();
    
    if (port.isClosed())
      return false;

    ReadStream is = getReadStream();
    
    if (getReadStream().getBufferAvailable() > 0)
      return true;
    
    long timeout = port.getKeepaliveTimeout();

    boolean isSelectManager = port.getServer().isSelectManagerEnabled();
    
    if (isSelectManager) {
      timeout = port.getKeepaliveSelectThreadTimeout();
    }

    if (timeout > 0 && timeout < port.getSocketTimeout()) {
      port.keepaliveThreadBegin();

      try {
	return is.fillWithTimeout(timeout);
      } finally {
	port.keepaliveThreadEnd();
      }
    }
    else if (isSelectManager)
      return false;
    else
      return true;
  }

  /**
   * Sets controller
   */
  @Override
  public void setController(ConnectionController controller)
  {
    super.setController(controller);

    if (controller.isDuplex()) {
      TcpDuplexController duplex = (TcpDuplexController) controller;
      
      _state = ConnectionState.DUPLEX;
      _readTask = new DuplexReadTask(duplex);
    }
    else {
      _state = ConnectionState.COMET;
    }
  }

  /**
   * Wakes the connection (comet-style).
   */
  @Override
  protected boolean wake()
  {
    ConnectionController controller = getController();

    if (controller == null)
      return false;
    
    _isWake = true;

    // comet
    if (getPort().resume(this)) {
      log.fine(dbgId() + "wake");
      return true;
    }

    log.fine(dbgId() + "wake failed");
    return false;
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

      Thread.yield();
    }
  }
  
  public void close()
  {
    closeImpl();
  }

  /**
   * Closes the connection.
   */
  private void closeImpl()
  {
    QSocket socket = _socket;
    ConnectionState state;

    synchronized (this) {
      state = _state;
      
      if (state == ConnectionState.IDLE || state.isClosed())
        return;
      
      _state = _state.toClosed();
    }

    // detach any comet
    getPort().detach(this);

    getRequest().protocolCloseEvent();
     
    ConnectionController controller = getController();
    if (controller != null)
      controller.close();
    
    if (! state.isClosed()) {
      _isActive = false;
      _isKeepalive = false;

      Port port = getPort();

      if (state.isKeepalive()) {
	port.keepaliveEnd(this);
      }

      if (log.isLoggable(Level.FINER)) {
	if (port != null)
	  log.finer(dbgId() + "closing connection " + this + ", total=" + port.getConnectionCount());
	else
	  log.finer(dbgId() + "closing connection " + this);
      }
      _isWake = false;

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
   * Closes the controller.
   */
  @Override
  protected void closeControllerImpl()
  {
    getPort().resume(this);
  }
  
  /**
   * Destroys the connection()
   */
  public final void destroy()
  {
    closeImpl();
    
    ConnectionState state = _state;
    _state = ConnectionState.DESTROYED;

    if (state != ConnectionState.DESTROYED) {
      getPort().kill(this);
    }
  }
  
  /**
   * Completion processing at the end of the thread
   */
  void finish()
  {
    closeImpl();

    if (_state != ConnectionState.IDLE
	&& _state != ConnectionState.DESTROYED) {
      _state = _state.toIdle();
      _readTask = _acceptTask;

      getPort().free(this);
    }
  }

  protected String dbgId()
  {
    if (_dbgId == null) {
      Object serverId = Environment.getAttribute("caucho.server-id");

      if (serverId != null)
	_dbgId = (getClass().getSimpleName() + "[id=" + getId()
		  + "," + serverId + "] ");
      else
	_dbgId = (getClass().getSimpleName() + "[id=" + getId() + "]");
    }

    return _dbgId;
  }

  @Override
  public String toString()
  {
    return "TcpConnection[id=" + _id + "," + _port.toURL() + "," + _state + "]";
  }
  
  enum RequestState {
    REQUEST,
    THREAD_DETACHED,
    EXIT
  };
  
  enum ConnectionState {
    IDLE,
    ACCEPT,
    REQUEST,
    REQUEST_ACTIVE,
    REQUEST_KEEPALIVE,
    COMET,
    DUPLEX,
    DUPLEX_KEEPALIVE,
    COMPLETE,
    CLOSED,
    DESTROYED;
    
    boolean isComet()
    {
      return this == COMET;
    }
    
    /**
     * True if the state is one of the keepalive states, either
     * a true keepalive-select, or comet or duplex.
     */
    boolean isKeepalive()
    {
      return (this == REQUEST_KEEPALIVE
              || this == COMET
              || this == DUPLEX
              || this == DUPLEX_KEEPALIVE);
    }
    
    boolean isActive()
    {
      switch (this) {
      case IDLE:
      case ACCEPT:
      case REQUEST:
      case REQUEST_ACTIVE:
	return true;

      default:
	return false;
      }
    }
    
    boolean isClosed()
    {
      return this == CLOSED || this == DESTROYED;
    }
    
    ConnectionState toKeepaliveSelect()
    {
      switch (this) {
      case REQUEST:
      case REQUEST_ACTIVE:
      case REQUEST_KEEPALIVE:
        return REQUEST_KEEPALIVE;
        
      case DUPLEX:
      case DUPLEX_KEEPALIVE:
        return DUPLEX_KEEPALIVE;
        
      default:
	throw new IllegalStateException(this + " is an illegal keepalive state");
      }
    }
    
    ConnectionState toActive()
    {
      switch (this) {
      case ACCEPT:
      case REQUEST:
      case REQUEST_ACTIVE:
      case REQUEST_KEEPALIVE:
	return REQUEST_ACTIVE;
        
      case DUPLEX_KEEPALIVE:
      case DUPLEX:
        return DUPLEX;
	
      default:
	throw new IllegalStateException(this + " is an illegal active state");
      }
    }
    
    ConnectionState toAccept()
    {
      if (this != DESTROYED)
	return ACCEPT;
      else
	throw new IllegalStateException(this + " is an illegal accept state");
    }
    
    ConnectionState toIdle()
    {
      if (this != DESTROYED)
	return IDLE;
      else
	throw new IllegalStateException(this + " is an illegal idle state");
    }
    
    ConnectionState toClosed()
    {
      if (this != DESTROYED)
	return CLOSED;
      else
	throw new IllegalStateException(this + " is an illegal closed state");
    }
  }

  class AcceptTask implements Runnable {
    public void run()
    {
      doAccept(true);
    }
      
    void doAccept(boolean isStart)
    {
      Port port = _port;

      // next state is keepalive
      _readTask = _keepaliveTask;
    
      ServerRequest request = getRequest();
      boolean isWaitForRead = request.isWaitForRead();
	
      Thread thread = Thread.currentThread();
      String oldThreadName = thread.getName();
		   
      thread.setName(_id);
 
      port.threadBegin(TcpConnection.this);

      ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
      thread.setContextClassLoader(systemLoader);

      boolean isValid = false;
      RequestState result = RequestState.EXIT;
      
      try {
        _thread = thread;
	_state = _state.toAccept();

        while (! _state.isClosed()) {
	  if (_readTask != _keepaliveTask) {
	    System.out.println(TcpConnection.this + " bad task");
	    Thread.dumpStack();
	  }
	  
          _state = _state.toAccept();
          
          if (! _port.accept(TcpConnection.this, isStart)) {
            close();
            break;
          }
          
	  if (_readTask != _keepaliveTask)
	    Thread.dumpStack();
	  
          isStart = false;
        
          _connectionStartTime = Alarm.getCurrentTime();
          
          if (isWaitForRead && ! getReadStream().waitForRead()) {
            close();
            continue;
          }

	  result = handleConnection();
	  
          if (result == RequestState.THREAD_DETACHED) {
            break;
          }
        }
        
        isValid = true;
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        thread.setContextClassLoader(systemLoader);

        port.threadEnd(TcpConnection.this);
 
        _thread = null;
        thread.setName(oldThreadName);
      
        if (! isValid)
          destroy();

	if (result != RequestState.THREAD_DETACHED)
	  finish();
      }
    }
  }
 
  class KeepaliveTask implements Runnable {
    public void run()
    {
      Thread thread = Thread.currentThread();
      String oldThreadName = thread.getName();
		   
      thread.setName(_id);
    
      _port.keepaliveEnd(TcpConnection.this);
      _port.threadBegin(TcpConnection.this);

      RequestState result = RequestState.EXIT;
      
      boolean isValid = false;
      try {
	_state = _state.toActive();
	
        result = handleConnection();
         
        isValid = true;
      } finally {
        thread.setName(oldThreadName);
        _port.threadEnd(TcpConnection.this);
        
        if (! isValid)
          destroy();

	if (result != RequestState.THREAD_DETACHED)
	  close();
      }

      if (isValid && result != RequestState.THREAD_DETACHED)
	_acceptTask.doAccept(false);
    }
  }

  class DuplexReadTask implements Runnable {
    private TcpDuplexController _duplex;
    
    DuplexReadTask(TcpDuplexController duplex)
    {
      _duplex = duplex;
    }
    
    public void run()
    {
      Thread thread = Thread.currentThread();
      String oldThreadName = thread.getName();
		   
      thread.setName(_id);
    
      _port.keepaliveEnd(TcpConnection.this);
      _port.threadBegin(TcpConnection.this);
      
      boolean isValid = false;
      RequestState result = RequestState.EXIT;
      
      try {
	_state = _state.toActive();
	
        do {
	  result = RequestState.EXIT;
	  
          if (! _duplex.serviceRead())
	    break;
        } while ((result = keepaliveRead()) == RequestState.REQUEST);

        isValid = true;
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        thread.setName(oldThreadName);
        _port.threadEnd(TcpConnection.this);
        
        if (! isValid)
          destroy();

	if (result != RequestState.THREAD_DETACHED)
	  finish();
      }    
    }
  }
  
  class ResumeTask implements Runnable {
    public void run()
    {
      boolean isValid = false;
      
      try {
        if (! getRequest().handleResume()) {
          close();
        }
        
        isValid = true;
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        if (! isValid)
          close();
        
        finish();
      }
    }
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
