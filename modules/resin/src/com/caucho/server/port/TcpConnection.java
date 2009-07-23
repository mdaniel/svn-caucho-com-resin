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
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A protocol-independent TcpConnection.  TcpConnection controls the
 * TCP Socket and provides buffered streams.
 *
 * <p>Each TcpConnection has its own thread.
 */
public class TcpConnection extends Connection
{
  private static final L10N L = new L10N(TcpConnection.class);
  private static final Logger log
    = Logger.getLogger(TcpConnection.class.getName());

  private static final ThreadLocal<ServerRequest> _currentRequest
    = new ThreadLocal<ServerRequest>();

  private static final AtomicInteger _connectionCount = new AtomicInteger();

  private static ClassLoader _systemClassLoader;

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

  private ConnectionState _state = ConnectionState.IDLE;
  private ConnectionController _controller;

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
    _connectionId = _connectionCount.incrementAndGet();

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

  /**
   * Returns the ServerRequest for the current thread.
   */
  public static ServerRequest getCurrentRequest()
  {
    return _currentRequest.get();
  }

  /**
   * For QA only, set the current request.
   */
  public static void qaSetCurrentRequest(ServerRequest request)
  {
    _currentRequest.set(request);
  }

  /**
   * Sets an exception to mark the location of the thread start.
   */
  public void setStartThread()
  {
    _startThread = new Exception();
    _startThread.fillInStackTrace();
  }

  /**
   * Returns an exception marking the location of the thread start
   */
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
   * Returns true for active.
   */
  public boolean isActive()
  {
    return _state.isActive();
  }

  /**
   * Returns true for active.
   */
  public boolean isRequestActive()
  {
    return _state.isRequestActive();
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
    /* XXX: debugging
    setStartThread();
    */

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

  /**
   * Returns true if the connection is secure, i.e. a SSL connection
   */
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
   * Returns the admin
   */
  public TcpConnectionMXBean getAdmin()
  {
    return _admin;
  }

  //
  // port/socket predicates
  //

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
   * Returns the virtual host.
   */
  @Override
  public String getVirtualHost()
  {
    return getPort().getVirtualHost();
  }

  //
  // state predicates
  //

  /**
   * Returns the state string.
   */
  public final String getState()
  {
    return _state.toString();
  }

  public final boolean isDestroyed()
  {
    return _state.isDestroyed();
  }

  @Override
  public boolean isComet()
  {
    return _state.isComet();
  }

  public boolean isSuspend()
  {
    return _controller != null && _controller.isSuspended();
  }

  @Override
  public boolean isDuplex()
  {
    return _state.isDuplex();
  }

  public boolean isWake()
  {
    return _isWake;
  }

  //
  // comet predicates
  //

  public ConnectionController getController()
  {
    return _controller;
  }

  public String getCometPath()
  {
    if (_controller != null)
      return _controller.getForwardPath();
    else
      return null;
  }

  //
  // statistics
  //

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

  public final long getRequestStartTime()
  {
    return _requestStartTime;
  }

  //
  // request processing
  //

  /**
   * Starts the connection.
   */
  public void start()
  {
  }

  /**
   * Handles a new connection/socket from the client.
   */
  RequestState handleRequests()
  {
    Thread thread = Thread.currentThread();
    ClassLoader systemLoader = _systemClassLoader;

    boolean isValid = false;
    RequestState result = RequestState.EXIT;

   try {
     // clear the interrupted flag
     Thread.interrupted();

     _currentRequest.set(_request);

     boolean isStatKeepalive = _state.isKeepalive();
     boolean isKeepalive;
     do {
       thread.setContextClassLoader(systemLoader);

       if (_controller != null)
         throw new IllegalStateException(L.l("can't shift to active from duplex {0} controller {1}",
                                             _state, _controller));

       _state = _state.toActive();

       if (_port.isClosed()) {
         return RequestState.EXIT;
       }

       result = RequestState.EXIT;

       _isWake = false;

       if ((result = keepaliveRead()) != RequestState.REQUEST) {
         break;
       }

       boolean isRequestValid = getRequest().handleRequest();

       // statistics
       gatherStatistics(isStatKeepalive);

       if (! isRequestValid) {
         _isKeepalive = false;
       }

       if (_state == ConnectionState.DUPLEX) {
         // duplex (xmpp/hmtp) handling
         isValid = true;
         _readTask.run();

         return RequestState.THREAD_DETACHED;
       }
       else if (_state == ConnectionState.COMET) {
         if (_port.suspend(this)) {
           isValid = true;

           return RequestState.THREAD_DETACHED;
         }
         else {
           return RequestState.EXIT;
         }
       }
     } while (_isKeepalive
              && (result = keepaliveRead()) == RequestState.REQUEST);

     isValid = true;

     return result;
   } catch (ClientDisconnectException e) {
     _port.addLifetimeClientDisconnectCount();

      if (log.isLoggable(Level.FINER)) {
        log.finer(dbgId() + e);
      }
    } catch (InterruptedIOException e) {
      if (log.isLoggable(Level.FINEST)) {
        log.log(Level.FINEST, dbgId() + e, e);
      }
    } catch (IOException e) {
      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, dbgId() + e, e);
      }
    } finally {
      thread.setContextClassLoader(systemLoader);

      _currentRequest.set(null);

      if (! isValid)
        destroy();
    }

    return RequestState.EXIT;
  }

  private void gatherStatistics(boolean isStatKeepalive)
  {
    if (_requestStartTime > 0) {
      long startTime = _requestStartTime;
      _requestStartTime = 0;

      if (isStatKeepalive)
        _port.addLifetimeKeepaliveCount();

      _port.addLifetimeRequestCount();

      long now = Alarm.getCurrentTime();
      _port.addLifetimeRequestTime(now - startTime);

      ReadStream rs = getReadStream();
      long readCount = rs.getPosition();
      rs.clearPosition();
      _port.addLifetimeReadBytes(readCount);

      WriteStream ws = getWriteStream();
      long writeCount = ws.getPosition();
      ws.clearPosition();
      _port.addLifetimeWriteBytes(writeCount);
    }
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
    if (waitForKeepalive()) {
      return RequestState.REQUEST;
    }

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
          log.fine(dbgId() + " keepalive (select)");

        return RequestState.THREAD_DETACHED;
      }
      else {
        log.warning(dbgId() + " failed keepalive (select)");

        _state = _state.toActive();
        port.keepaliveEnd(this);

        close();

        return RequestState.EXIT;
      }
    }
    else {
      if (log.isLoggable(Level.FINE))
        log.fine(dbgId() + " keepalive (thread)");

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
      timeout = port.getBlockingTimeoutForSelect();
    }

    if (port.getSocketTimeout() < timeout)
      timeout = port.getSocketTimeout();

    if (timeout < 0)
      timeout = 0;

    // server/2l02

    port.keepaliveThreadBegin();

    try {
      boolean result = is.fillWithTimeout(timeout);

      return result;
    } finally {
      port.keepaliveThreadEnd();
    }

    /*
    if (isSelectManager)
      return false;
    else
      return true;
    */
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

      // Thread.yield();
    }
  }

  //
  // state transitions
  //

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
   * Sets the keepalive state.  Called only by the SelectManager and Port.
   */
  public void clearKeepalive()
  {
    if (! _isKeepalive)
      log.warning(this + " illegal state: clearing keepalive with inactive keepalive");

    _isKeepalive = false;
  }

  void setResume()
  {
    _isWake = false;
    _suspendTime = 0;
  }

  /**
   * Begins an active connection.
   */
  public final long beginActive()
  {
    _state = _state.toActive();
    _requestStartTime = Alarm.getCurrentTime();
    return _requestStartTime;
  }

  /**
   * Ends an active connection.
   */
  public final void endActive()
  {
    // state change?
    // _requestStartTime = 0;
  }

  /**
   * Finish a request.
   */
  public void finishRequest()
  {
    ConnectionController controller = _controller;
    _controller = null;

    if (controller != null)
      controller.closeImpl();

    // XXX: to finishRequest
    _state = _state.toCompleteComet();
  }

  public boolean toKeepalive()
  {
    if (! _isKeepalive)
      return false;

    _isKeepalive = _port.allowKeepalive(_connectionStartTime);

    return _isKeepalive;
  }

  /**
   * Starts a comet request
   */
  @Override
  public ConnectionCometController toComet(boolean isTop,
                                           ServletRequest request,
                                           ServletResponse response)
  {
    if (_controller != null)
      throw new IllegalStateException(L.l("comet mode can't start in state '{0}'",
                                          _state));

    ConnectionCometController controller
      = super.toComet(isTop, request, response);

    _controller = controller;

    if (log.isLoggable(Level.FINER))
      log.finer(this + " starting comet");

    _state = ConnectionState.COMET;

    return controller;
  }

  void setWake()
  {
    _isWake = true;
  }

  public void suspend()
  {
    if (_controller != null)
      _controller.suspend();
  }

  void toSuspend()
  {
    _suspendTime = Alarm.getCurrentTime();
    _keepaliveExpireTime = _suspendTime + _idleTimeMax;
  }

  /**
   * Wakes the connection (comet-style).
   */
  protected boolean wake()
  {
    if (! _state.isComet())
      return false;

    _isWake = true;

    // comet
    if (getPort().resume(this)) {
      log.fine(dbgId() + " wake");
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Starts a full duplex (tcp style) request for hmtp/xmpp
   */
  public TcpDuplexController toDuplex(TcpDuplexHandler handler)
  {
    if (_controller != null) {
      ConnectionState state = _state;

      log.warning(this + " toDuplex call failed for state " + state);

      destroy();

      throw new IllegalStateException(L.l("duplex mode can't start in state '{0}'",
                                          state));
    }

    TcpDuplexController duplex = new TcpDuplexController(this, handler);

    _controller = duplex;

    if (log.isLoggable(Level.FINER))
      log.finer(this + " starting duplex");

    _state = ConnectionState.DUPLEX;
    _isKeepalive = false;
    _readTask = new DuplexReadTask(duplex);

    return duplex;
  }

  public void closeController(ConnectionCometController controller)
  {
    if (controller == _controller) {
      _controller = null;

      closeControllerImpl();
    }
  }

  /**
   * Closes the controller.
   */
  protected void closeControllerImpl()
  {
    _state = _state.toCompleteComet();

    getPort().resume(this);
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
    ConnectionState state;

    synchronized (this) {
      state = _state;

      if (state == ConnectionState.IDLE || state.isClosed())
        return;

      _state = _state.toClosed();
    }

    QSocket socket = _socket;

    // detach any comet
    getPort().detach(this);

    getRequest().protocolCloseEvent();

    ConnectionController controller = _controller;
    _controller = null;

    if (controller != null)
      controller.closeImpl();

    _isKeepalive = false;

    Port port = getPort();

    if (state.isKeepalive()) {
      port.keepaliveEnd(this);
    }

    if (log.isLoggable(Level.FINER)) {
      if (port != null)
        log.finer(dbgId() + " closing connection " + this + ", total=" + port.getConnectionCount());
      else
        log.finer(dbgId() + " closing connection " + this);
    }

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
      getPort().closeSocket(socket);

      try {
        socket.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Destroys the connection()
   */
  public final void destroy()
  {
    _socket.forceShutdown();

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
  void finishThread()
  {
    closeImpl();

    if (_state != ConnectionState.IDLE
        && _state != ConnectionState.DESTROYED) {
      if (_controller != null) {
        throw new IllegalStateException(L.l("{0} can't switch to idle from {1} with an active controller {2}",
                                            this, _state, _controller));
      }

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

    boolean isDuplex()
    {
      return this == DUPLEX || this == DUPLEX_KEEPALIVE;
    }

    /**
     * True if the state is one of the keepalive states, either
     * a true keepalive-select, or comet or duplex.
     */
    boolean isKeepalive()
    {
      // || this == COMET
      return (this == REQUEST_KEEPALIVE
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

    boolean isRequestActive()
    {
      switch (this) {
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

    boolean isDestroyed()
    {
      return this == DESTROYED;
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

      case COMET:
        return COMET;

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

    ConnectionState toComplete()
    {
      switch (this) {
      case CLOSED:
      case DESTROYED:
        return this;

      default:
        return COMPLETE;
      }
    }

    ConnectionState toCompleteComet()
    {
      if (this == COMET)
        return REQUEST;
      else
        return this;
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

    /**
     * Loop to accept new connections.
     *
     * @param isThreadStart true if the thread has just started to
     * properly account for thread housekeeping
     */
    void doAccept(boolean isThreadStart)
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

      ClassLoader systemLoader = _systemClassLoader;
      thread.setContextClassLoader(systemLoader);

      boolean isValid = false;
      RequestState result = RequestState.EXIT;

      try {
        _thread = thread;

        // _state = _state.toAccept();

        while (! _port.isClosed() && _state != ConnectionState.DESTROYED) {
          if (_controller != null) {
            throw new IllegalStateException(L.l("{0} cannot change from {1} to accept with an active controller {2}", this, _state, _controller));
          }

          _state = _state.toAccept();

          if (! _port.accept(TcpConnection.this, isThreadStart)) {
            close();
            break;
          }

          if (_readTask != _keepaliveTask)
            Thread.dumpStack();

          isThreadStart = false;

          _connectionStartTime = Alarm.getCurrentTime();

          _request.startConnection();
          _isKeepalive = true;

          result = handleRequests();

          if (result == RequestState.THREAD_DETACHED) {
            break;
          }

          close();
        }

        isValid = true;
      } catch (OutOfMemoryError e) {
        CauchoSystem.exitOom(getClass(), e);
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
          finishThread();
      }
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + TcpConnection.this + "]";
    }
  }

  class KeepaliveTask implements Runnable {
    public void run()
    {
      Thread thread = Thread.currentThread();
      String oldThreadName = thread.getName();

      thread.setName(_id);

      if (_state.isKeepalive()) {
        _port.keepaliveEnd(TcpConnection.this);
      }

      _port.threadBegin(TcpConnection.this);

      RequestState result = RequestState.EXIT;

      boolean isValid = false;
      try {
        _state = _state.toActive();

        result = handleRequests();

        isValid = true;
      } catch (OutOfMemoryError e) {
        CauchoSystem.exitOom(getClass(), e);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
        return;
      } finally {
        thread.setName(oldThreadName);
        _port.threadEnd(TcpConnection.this);

        if (! isValid)
          destroy();

        else if (result != RequestState.THREAD_DETACHED)
          close();
      }

      if (isValid && result != RequestState.THREAD_DETACHED) {
        // acceptTask significantly faster than finishing
        _acceptTask.doAccept(false);
      }
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + TcpConnection.this + "]";
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

      if (_state.isKeepalive()) {
        _port.keepaliveEnd(TcpConnection.this);
      }

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
      } catch (OutOfMemoryError e) {
        CauchoSystem.exitOom(getClass(), e);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        thread.setName(oldThreadName);
        _port.threadEnd(TcpConnection.this);

        if (! isValid)
          destroy();

        if (result != RequestState.THREAD_DETACHED)
          finishThread();
      }
    }
  }

  class ResumeTask implements Runnable {
    public void run()
    {
      boolean isValid = false;

      try {
        ConnectionCometController comet
          = (ConnectionCometController) _controller;

        if (comet != null)
          comet.startResume();

        _isWake = false;

        if (getRequest().handleResume()
            && _port.suspend(TcpConnection.this)) {
          isValid = true;
        }
        else if (_isKeepalive) {
          isValid = true;
          _keepaliveTask.run();
        }
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } catch (OutOfMemoryError e) {
        CauchoSystem.exitOom(getClass(), e);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        if (! isValid) {
          finishThread();
        }
      }
    }
  }

  class Admin extends AbstractManagedObject implements TcpConnectionMXBean {
    Admin()
    {
      super(_systemClassLoader);
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

    public String getUrl()
    {
      ServerRequest request = TcpConnection.this.getRequest();

      if (request instanceof AbstractHttpRequest) {
        AbstractHttpRequest req = (AbstractHttpRequest) request;

        if (! "".equals(req.getRequestURI())) {
          String url = String.valueOf(req.getRequestURL());

          return url;
        }
        else {
          Port port = TcpConnection.this.getPort();

          if (port.getAddress() == null)
            return "accept://*:" + port.getPort();
          else
            return "accept://" + port.getAddress() + ":" + port.getPort();
        }
      }


      return null;
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

  static {
    _systemClassLoader = ClassLoader.getSystemClassLoader();
  }
}
