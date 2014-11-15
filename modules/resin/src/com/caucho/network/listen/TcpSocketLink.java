/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is software; you can redistribute it and/or modify
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
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.inject.Module;
import com.caucho.inject.RequestContext;
import com.caucho.loader.Environment;
import com.caucho.management.server.*;
import com.caucho.util.CurrentTime;
import com.caucho.util.Friend;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SocketTimeoutException;
import com.caucho.vfs.StreamImpl;

/**
 * A protocol-independent TcpConnection.  TcpConnection controls the
 * TCP Socket and provides buffered streams.
 *
 * <p>Each TcpConnection has its own thread.
 */
@Module
public class TcpSocketLink extends AbstractSocketLink
{
  private static final L10N L = new L10N(TcpSocketLink.class);
  private static final Logger log
    = Logger.getLogger(TcpSocketLink.class.getName());

  private static final ThreadLocal<ProtocolConnection> _currentRequest
    = new ThreadLocal<ProtocolConnection>();

  private final int _connectionId;  // The connection's id
  private final String _id;
  private final String _name;
  private String _dbgId;

  private final TcpPort _port;
  private final QSocket _socket;
  private final ProtocolConnection _request;
  private final ClassLoader _loader;

  private final AcceptTask _acceptTask;
  // HTTP keepalive task
  private final KeepaliveRequestTask _keepaliveTask;
  // HTTP keepalive timeout
  private final KeepaliveTimeoutTask _keepaliveTimeoutTask;
  // Comet resume task
  private final CometResumeTask _resumeTask;
  // duplex (websocket) task
  private DuplexReadTask _duplexReadTask;

  private SocketLinkState _state = SocketLinkState.INIT;
  
  private final AtomicReference<SocketLinkRequestState> _requestStateRef
    = new AtomicReference<SocketLinkRequestState>(SocketLinkRequestState.INIT)
    ;
  private TcpAsyncController _async;

  private long _idleTimeout;
  private long _suspendTimeout;

  private long _connectionStartTime;
  private long _requestStartTime;

  private long _idleStartTime;
  private long _idleExpireTime;

  // statistics state
  private String _displayState;

  private volatile Thread _thread;

  /**
   * Creates a new TcpConnection.
   *
   * @param server The TCP server controlling the connections
   * @param request The protocol Request
   */
  TcpSocketLink(int connId,
                TcpPort listener,
                QSocket socket)
  {
    _connectionId = connId;

    _port = listener;
    _socket = socket;

    int id = getId();

    _loader = listener.getClassLoader();

    Protocol protocol = listener.getProtocol();

    _request = protocol.createConnection(this);

    _id = listener.getDebugId() + "-" + id;
    _name = _id;

    _acceptTask = new AcceptTask(this);
    _keepaliveTask = new KeepaliveRequestTask(this);
    _keepaliveTimeoutTask = new KeepaliveTimeoutTask(this);
    _resumeTask = new CometResumeTask(this);
  }

  /**
   * Returns the ServerRequest for the current thread.
   */
  public static ProtocolConnection getCurrentRequest()
  {
    return _currentRequest.get();
  }

  /**
   * For QA only, set the current request.
   */
  public static void setCurrentRequest(ProtocolConnection request)
  {
    _currentRequest.set(request);
  }

  /**
   * Returns the connection id.  Primarily for debugging.
   */
  @Override
  public int getId()
  {
    return _connectionId;
  }
  
  public String getDebugId()
  {
    return _id;
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
  public TcpPort getPort()
  {
    return _port;
  }
  
  public SocketLinkThreadLauncher getLauncher()
  {
    return getPort().getLauncher();
  }

  /**
   * Returns the request for the connection.
   */
  public final ProtocolConnection getRequest()
  {
    return _request;
  }

  /**
   * Returns the admin
   */
  public TcpConnectionInfo getConnectionInfo()
  {
    TcpConnectionInfo connectionInfo = null;
    
    if (isActive()) {
      connectionInfo = new TcpConnectionInfo(getId(), 
                                             getThreadId(),
                                             _port.getAddress(),
                                             _port.getPort(),
                                             getDisplayState(),
                                             getRequestStartTime());
      if (connectionInfo.hasRequest()) {
        connectionInfo.setRemoteAddress(getRemoteHost());
        connectionInfo.setUrl(getRequestUrl());
      }
      
    }
    
    return connectionInfo;
  }
  
  public String getRequestUrl()
  {
    ProtocolConnection request = getRequest();
  
    String url = request.getProtocolRequestURL();
    if (url != null && ! "".equals(url))
      return url;
    
    TcpPort port = getPort();
    
    String protocolName = port.getProtocolName();
    if (protocolName == null)
      protocolName = "request";
    
    if (port.getAddress() == null)
      return protocolName + "://*:" + port.getPort();
    else
      return protocolName + "://" + port.getAddress() + ":" + port.getPort();
  }
  

  //
  // timeout properties
  //

  /**
   * Sets the idle time for a keepalive connection.
   */
  public void setIdleTimeout(long idleTimeout)
  {
    _idleTimeout = idleTimeout;
    _suspendTimeout = idleTimeout;
  }

  /**
   * The idle time for a keepalive connection
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  //
  // port information
  //
  
  @Override
  public boolean isPortActive()
  {
    return _port.isActive();
  }
  
  //
  // state information
  //

  /**
   * Returns the state.
   */
  @Override
  public SocketLinkState getState()
  {
    return _state;
  }

  public final boolean isIdle()
  {
    return _state.isIdle();
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

  @Override
  public boolean isKeepaliveAllocated()
  {
    return _state.isKeepaliveAllocated();
  }

  /**
   * Returns true for closed.
   */
  public boolean isClosed()
  {
    return _state.isClosed();
  }

  public final boolean isDestroyed()
  {
    return _state.isDestroyed() || _requestStateRef.get().isDestroyed();
  }

  @Override
  public boolean isCometActive()
  {
    TcpAsyncController async = _async;

    return (_state.isCometActive()
            && async != null 
            && ! async.isCompleteRequested());
  }

  public boolean isAsyncStarted()
  {
    return _requestStateRef.get().isAsyncStarted();
  }

  public boolean isAsyncComplete()
  {
    TcpAsyncController async = _async;
    
    return async != null && async.isCompleteRequested();
  }

  @Override
  public boolean isCometSuspend()
  {
    return _state.isCometSuspend();
  }

  boolean isCometComplete()
  {
    TcpAsyncController async = _async;
    
    return async != null && async.isCompleteRequested();
  }

  @Override
  public boolean isDuplex()
  {
    return _state.isDuplex();
  }

  boolean isWakeRequested()
  {
    return _requestStateRef.get().isAsyncWake();
  }
  
  SocketLinkRequestState getRequestState()
  {
    return _requestStateRef.get();
  }

  //
  // port/socket information
  //

  /**
   * Returns the connection's socket
   */
  public QSocket getSocket()
  {
    return _socket;
  }

  /**
   * Returns the local address of the socket.
   */
  @Override
  public InetAddress getLocalAddress()
  {
    return _socket.getLocalAddress();
  }

  /**
   * Returns the local host name.
   */
  @Override
  public String getLocalHost()
  {
    return _socket.getLocalHost();
  }

  /**
   * Returns the socket's local TCP port.
   */
  @Override
  public int getLocalPort()
  {
    return _socket.getLocalPort();
  }

  /**
   * Returns the socket's remote address.
   */
  @Override
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
  @Override
  public int getRemotePort()
  {
    return _socket.getRemotePort();
  }

  /**
   * Returns true if the connection is secure, i.e. a SSL connection
   */
  @Override
  public boolean isSecure()
  {
    return _socket.isSecure() || _port.isSecure();
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
  // SSL api
  //
  
  /**
   * Returns the cipher suite
   */
  @Override
  public String getCipherSuite()
  {
    return _socket.getCipherSuite();
  }
  
  /***
   * Returns the key size.
   */
  @Override
  public int getKeySize()
  {
    return _socket.getCipherBits();
  }
  
  /**
   * Returns any client certificates.
   * @throws CertificateException 
   */
  @Override
  public X509Certificate []getClientCertificates()
    throws CertificateException
  {
    return _socket.getClientCertificates();
  }
  

  //
  // thread information
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
  
  public final Thread getThread()
  {
    return _thread;
  }
  
  //
  // connection information

  /**
   * Returns the time the connection started
   */
  public final long getConnectionStartTime()
  {
    return _connectionStartTime;
  }

  //
  // request information
  //

  /**
   * Returns the time the request started
   */
  public final long getRequestStartTime()
  {
    return _requestStartTime;
  }

  /**
   * Returns the idle expire time (keepalive or suspend).
   */
  public long getIdleExpireTime()
  {
    return _idleExpireTime;
  }

  /**
   * Returns the idle start time (keepalive or suspend)
   */
  public long getIdleStartTime()
  {
    return _idleStartTime;
  }

  /**
   * Returns the accept task. Only called from the launcher.
   */
  private Runnable getAcceptTask()
  {
    return _acceptTask;
  }

  /**
   * Returns the current keepalive task (request or duplex)
   */
  private ConnectionTask getKeepaliveTask()
  {
    if (_state.isDuplex()) {
      return _duplexReadTask;
    }
    else {
      return _keepaliveTask;
    }
  }

  /**
   * Returns the current keepalive task (request or duplex)
   */
  private ConnectionTask getKeepaliveTimeoutTask()
  {
    return _keepaliveTimeoutTask;
  }

  /**
   * Returns the comet resume task
   */
  private ConnectionTask getResumeTask()
  {
    return _resumeTask;
  }

  //
  // statistics state
  //

  /**
   * Returns the user statistics state
   */
  public String getDisplayState()
  {
    return _displayState;
  }

  /**
   * Sets the user statistics state
   */
  private void setStatState(String state)
  {
    _displayState = state;
  }

  //
  // async/comet predicates
  //

  /**
   * Poll the socket to test for an end-of-file for a comet socket.
   */
  @Friend(TcpPort.class)
  boolean isReadEof()
  {
    QSocket socket = _socket;

    if (socket == null) {
      return true;
    }

    try {
      StreamImpl s = socket.getStream();

      return s.isEof();
      /*
      int len = s.getAvailable();

      if (len > 0 || len == ReadStream.READ_TIMEOUT)
        return false;
      else
        return true;
        */
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return true;
    }
  }
  
  //
  // transition requests from external threads (thread-safe)
  
  /**
   * Start a request connection from the idle state.
   */
  @Friend(SocketLinkThreadLauncher.class)
  AcceptTask requestAccept()
  {
    if (_requestStateRef.get().toAccept(_requestStateRef)) {
      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " request-accept " + getName()
                  + " (count=" + _port.getThreadCount()
                  + ", idle=" + _port.getIdleThreadCount() + ")");
      }

      return _acceptTask;
    }
    else {
      return null;
    }
  }
  
  /**
   * Wake a connection from a select/poll keepalive.
   */
  void requestWakeKeepalive()
  {
    if (_requestStateRef.get().toWakeKeepalive(_requestStateRef)) {
      if (! getLauncher().offerResumeTask(getKeepaliveTask())) {
        log.severe(L.l("Schedule failed for {0}", this));
      }
    }
  }
  
  /**
   * Wake a connection from a select/poll keepalive.
   */
  void requestTimeoutKeepalive()
  {
    if (_requestStateRef.get().toWakeKeepalive(_requestStateRef)) {
      if (! getLauncher().offerResumeTask(getKeepaliveTimeoutTask())) {
        log.severe(L.l("Schedule failed for {0}", this));
      }
    }
  }
  
  /**
   * Wake a connection from a comet suspend.
   */
  void requestWakeComet()
  {
    if (_requestStateRef.get().toAsyncWake(_requestStateRef)) {
      if (! getLauncher().offerResumeTask(getResumeTask())) {
        log.severe(L.l("Schedule failed for {0}", this));
      }
    }
  }

  /**
   * Closes the controller.
   */
  void requestCometComplete()
  {
    TcpAsyncController async = _async;
    
    if (async != null) {
      async.setCompleteRequested();
    }

    try {
      requestWakeComet();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Closes the controller.
   */
  void requestCometTimeout()
  {
    TcpAsyncController async = _async;
    
    if (async != null) {
      async.setTimeout();
    }

    try {
      requestWakeComet();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Closes the connection()
   */
  /*
  public final void requestClose()
  {
    if (_requestStateRef.get().toClose(_requestStateRef)) {
      if (getLauncher().offerResumeTask(new CloseTask(this))) {
        return;
      }
    }

    requestDestroy();
  }
  */

  /**
   * Destroys the connection()
   */
  public final void requestDestroy()
  {
    if (_requestStateRef.get().toDestroy(_requestStateRef)) {
      if (! getLauncher().offerResumeTask(new DestroyTask(this))) {
        destroy();
      }
    }
    else {
      closeConnection();
    }
  }
  
  private void destroy()
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " destroying connection");
    }
    
    try {
      _socket.forceShutdown();
    } catch (Throwable e) {
      
    }
    
    SocketLinkState state = _state;
    _state = _state.toDestroy(this);

    closeConnection(state);
  }

  @Override
  public void requestShutdownBegin()
  {
    _port.requestShutdownBegin();
  }

  @Override
  public void requestShutdownEnd()
  {
    _port.requestShutdownEnd();
  }

  //
  // Callbacks from the request processing tasks
  //
  
  @Friend(ConnectionTask.class)
  final void startThread(final Thread thread)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " start thread " + thread.getName()
                 + " (count=" + _port.getThreadCount()
                 + ", idle=" + _port.getIdleThreadCount() + ")");
    }
    
    final Thread oldThread = _thread;
    
    if (oldThread != null) {
      throw new IllegalStateException("old: " + oldThread.getId() + "@" + oldThread
                                      + " current: " + thread.getId() + "@" + thread);
    }
    
    _thread = thread;
    
    _request.onAttachThread();
  }

  /**
   * Completion processing at the end of the thread
   */
  @Friend(ConnectionTask.class)
  final void finishThread(RequestState resultState)
  {
    Thread thread = _thread;
    _thread = null;
    
    _request.onDetachThread();
    
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " finish thread: " + Thread.currentThread().getName());
    }
    
    Thread currentThread = Thread.currentThread();
    
    if (thread != currentThread) {
      Thread.dumpStack();
      throw new IllegalStateException("old: " + thread
                                      + " current: " + currentThread);
    }
    
    if (_requestStateRef.get().isDestroyed()) {
      destroy();
    }
    
    SocketLinkRequestState reqState = _requestStateRef.get();
    
    SocketLinkState state = _state;
    
    if (reqState.isAsyncStarted() || reqState.isAsyncWake()) {
      if (state.isClosed()) {
        destroy();
        return;
      }
      
      if (! reqState.toAsyncSuspendThread(_requestStateRef)) {
        if (! getLauncher().offerResumeTask(getResumeTask())) {
          log.severe(L.l("Schedule resume failed for {0}", this));
        }
      }
      
      return;
    }

    if (! (state.isComet() || state.isDuplex())
        && ! resultState.isAsyncOrDuplex()) {
      try {
        closeAsyncIfNotAsync();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    if (resultState.isKeepaliveSelect() && ! state.isClosed()) {
      // keepalive wake before the thread exits
      if (! reqState.toSuspendKeepalive(_requestStateRef)) {
        if (! getLauncher().offerResumeTask(getKeepaliveTask())) {
          log.severe(L.l("Schedule keepalive failed for {0}", this));
        }
      }
      
      return;
    }
    
    if (resultState.isDetach() && ! state.isClosed()) {
      return;
    }
    
    getPort().closeConnection(this);

    if (state.isAllowIdle() && _requestStateRef.get().isAllowIdle()) {
      _state = state.toIdle();
      
      _requestStateRef.get().toIdle(_requestStateRef);
        
      _port.free(this);
    }
    else if (isDestroyed()) {
    }
    else if (resultState.isClosed()) {
      // network/0272
      close();
    }
    else if (_port.isActive()) {
      String msg = (this + " Internal error unable to free: "
                    + " result=" + resultState
                    + " requestState=" + reqState + " " + _requestStateRef.get());
      log.warning(msg);
    }
  }
  
  @Friend(ConnectionTask.class)
  RequestState handleAcceptTask()
    throws IOException
  {
    getLauncher().onChildIdleBegin();
    try {
      return handleAcceptTaskImpl();
    } finally {
      getLauncher().onChildIdleEnd();
    }
  }

  /**
   * Called in the idle state. Only called from the acceptTask and from
   * handleAcceptTask
   */
  @Friend(AcceptTask.class)
  RequestState handleAcceptTaskImpl()
    throws IOException
  {
    TcpPort listener = getPort();
    SocketLinkThreadLauncher launcher = listener.getLauncher();
    
    RequestState result = RequestState.REQUEST_COMPLETE;

    while (result.isAcceptAllowed()
           && listener.isActive()
           && ! getState().isDestroyed()) {
      
      setStatState("accept");
      _state = _state.toAccept();

      if (! accept() || ! listener.isActive()) {
        setStatState("close");
        close();

        return RequestState.EXIT;
      }

      launcher.onChildIdleEnd();
      try {
        toStartConnection();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " accept from "
                    + getRemoteHost() + ":" + getRemotePort());
        }

        if (_port.isAsyncThrottle()) {
          _state = _state.toActiveWithKeepalive(this);
          
          result = handleRequests(false);
        }
        else {
          result = handleRequests(true);
        }
      } catch (IOException e) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, this + " handleAccept: " + e, e);
        else if (log.isLoggable(Level.FINE))
          log.fine(this + " handleAccept: " + e);
        
        setStatState("close");
        close();

        return RequestState.EXIT;
      } finally {
        launcher.onChildIdleBegin();
      }
    }

    return result;
  }

  private boolean accept()
  {
    SocketLinkThreadLauncher launcher = _port.getLauncher();

    if (launcher.isIdleOverflow()) {
      return false;
    }

    return getPort().accept(getSocket());
  }

  @Friend(KeepaliveRequestTask.class)
  RequestState handleKeepaliveTask()
    throws IOException
  {
    return handleRequests(true);
    
    /*
    RequestState state = handleRequests(true);
    
    if (state.isAcceptAllowed()) {
      return handleAcceptTask();
    }
    else {
      return state;
    }
    */
  }

  @Friend(KeepaliveTimeoutTask.class)
  RequestState handleKeepaliveTimeoutTask()
    throws IOException
  {
    _state = _state.toActiveNoKeepalive(this);
    
    close();
    
    // return handleAcceptTask();
    
    return RequestState.CLOSED;
  }

  @Friend(DestroyTask.class)
  RequestState handleDestroyTask()
    throws IOException
  {
    destroy();
    
    return RequestState.EXIT;
  }

  /**
   * Handles the resume from ResumeTask. 
   * 
   * Called by the request thread only.
   */
  @Friend(CometResumeTask.class)
  RequestState handleResumeTask()
  {
    try {
      while (true) {
        _state = _state.toCometResume(this);
      
        TcpAsyncController async = getAsyncController();

        if (async == null) {
          // network/0290
          close();
          return RequestState.EXIT;
        }
        // _state = _state.toCometWake();
        // _state = _state.toCometDispatch();
      
        if (async.isTimeout()) {
          if (async.timeout()) {
            _request.handleResume();
            close();
            return RequestState.EXIT;
          }
        }

        async.toResume();
        
        long requestTimeout = getPort().getRequestTimeout();
        
        if (requestTimeout > 0)
          _socket.setRequestExpireTime(CurrentTime.getCurrentTime() + requestTimeout);

        // server/1lb5, #4697
        // if (! async.isCompleteRequested()) {
        getRequest().handleResume();

        // }

        if (_state.isComet()) {
          if (toSuspend())
            return RequestState.ASYNC;
          else
            continue;
        }
        else if (isKeepaliveAllocated()) {
          // server/1l81, network/0291
          //_state = _state.toKeepalive(this);
          
          closeAsync();

          return handleRequests(false);
        }
        else {
          closeAsync();
          
          close();
        
          return RequestState.REQUEST_COMPLETE;
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    } catch (OutOfMemoryError e) {
      String msg = "TcpSocketLink OutOfMemory";

      ShutdownSystem.shutdownOutOfMemory(msg);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _socket.setRequestExpireTime(0);
    }
    
    return RequestState.EXIT;
  }

  @Friend(DuplexReadTask.class)
  RequestState handleDuplexRead(SocketLinkDuplexController duplex)
    throws IOException
  {
    toDuplexActive();

    RequestState result = RequestState.EXIT;

    ReadStream readStream = getReadStream();

    while ((result = processKeepalive()) == RequestState.REQUEST_COMPLETE) {
      toDuplexActive();
      
      long position = readStream.getPosition();

      duplex.serviceRead();

      if (position == readStream.getPosition()) {
        log.warning(duplex + " was not processing any data. Shutting down.");
        
        close();

        return RequestState.EXIT;
      }
      
      if (duplex.isCompleteRequested()) {
        close();
        
        return RequestState.EXIT;
      }
    }
    
    if (result.isClosed()) {
      close();
    }

    return result;
  }

  /**
   * Handles a new connection/socket from the client.
   */
  private RequestState handleRequests(boolean isDataAvailable)
    throws IOException
  {
    Thread thread = getThread();

    RequestState result = RequestState.EXIT;

    try {
      // boolean isKeepalive = false; // taskType == Task.KEEPALIVE;
      // boolean isDataAvailable = taskType == Task.KEEPALIVE && ! _state.isKeepalive();

      result = handleRequestsImpl(isDataAvailable);
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
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, dbgId() + e, e);
      }
    } finally {
      thread.setContextClassLoader(_loader);

      if (result == null) {
        result = RequestState.EXIT;
      }
    }

    switch (result) {
    case KEEPALIVE_SELECT:
    case ASYNC:
      return result;
      
    case DUPLEX:
      return _duplexReadTask.doTask();
      
    case EXIT:
    case CLOSED:
      close();
      return result;
      
    case REQUEST_COMPLETE:
      // acceptTask significantly faster than finishing
      close();
      return result;
      /*
      if (taskType == Task.ACCEPT) {
        return result;
      }
      else {
        SocketLinkThreadLauncher launcher = getLauncher();
        
        try {
          launcher.onChildIdleBegin();
          
          return _acceptTask.doTask();
        } finally {
          launcher.onChildIdleEnd();
        }
      }
      */
      
    default:
      throw new IllegalStateException(String.valueOf(result));
    }
  }

  /**
   * Handles a new connection/socket from the client.
   */
  private RequestState handleRequestsImpl(boolean isDataAvailable)
    throws IOException
  {
    RequestState result;
    
    do {
      result = RequestState.EXIT;
      
      if (_port.isClosed()) {
        return RequestState.EXIT;
      }
      
      // clear the interrupted flag
      Thread.interrupted();

      if (! isDataAvailable
          && (result = processKeepalive()) != RequestState.REQUEST_COMPLETE) {
        return result;
      }

      getPort().addLifetimeRequestCount();
      
      try {
        result = handleRequest();
      } finally {
        if (! result.isAsyncOrDuplex()) {
          closeAsyncIfNotAsync();
        }
      }
      
      isDataAvailable = false;
    } while (result.isRequestKeepalive() && _state.isKeepaliveAllocated());

    return result;
  }
  
  private RequestState handleRequest()
    throws IOException
  {
    dispatchRequest();

    if (_state == SocketLinkState.DUPLEX) {
      if (_duplexReadTask == null)
        throw new NullPointerException();
      // duplex (xmpp/hmtp) handling
      return RequestState.DUPLEX;
    }

    getWriteStream().flush();

    if (_state.isCometActive()) {
      if (toSuspend())
        return RequestState.ASYNC;
      else {
        return handleResumeTask();
      }
    }
   
    return RequestState.REQUEST_COMPLETE;
  }

  private void dispatchRequest()
    throws IOException
  {
    Thread thread = getThread();

    try {
      thread.setContextClassLoader(_loader);

      _currentRequest.set(_request);
      RequestContext.begin();
      _requestStartTime = CurrentTime.getCurrentTime();
      
      long requestTimeout = getPort().getRequestTimeout();
      
      if (requestTimeout > 0)
        _socket.setRequestExpireTime(_requestStartTime + requestTimeout);
      
      long readBytes = _socket.getTotalReadBytes();
      long writeBytes = _socket.getTotalWriteBytes();

      _state = _state.toActive(this, _connectionStartTime);

      if (! getRequest().handleRequest()) {
        killKeepalive("dispatch handleRequest failed");
        
        if (log.isLoggable(Level.FINE)) {
          log.fine(this + " disabled keepalive because request failed "
                   + getRequest());
        }
      }
      
      _requestStartTime = 0;
      
      long readBytesEnd = _socket.getTotalReadBytes();
      long writeBytesEnd = _socket.getTotalWriteBytes();
      
      _port.addLifetimeReadBytes(readBytesEnd - readBytes);
      _port.addLifetimeWriteBytes(writeBytesEnd - writeBytes);
    }
    finally {
      thread.setContextClassLoader(_loader);

      _currentRequest.set(null);
      RequestContext.end();

      _socket.setRequestExpireTime(0);
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
  private RequestState processKeepalive()
    throws IOException
  {
    TcpPort port = _port;

    _idleStartTime = CurrentTime.getCurrentTimeActual();
    _idleExpireTime = _idleStartTime + _idleTimeout;
    
    // quick timed read to see if data is already available
    int available;
    
    if (_port.getSelectManager() != null) {
      available = port.keepaliveThreadRead(getReadStream());
    }
    else {
      available = 0;
    }

    if (available > 0) {
      return RequestState.REQUEST_COMPLETE;
    }
    else if (available < 0) {
      if (log.isLoggable(Level.FINER))
        log.finer(this + " keepalive read failed: " + available);
      
      killKeepalive("process-keepalive eof");
      
      setStatState(null);
      // close();
      
      return RequestState.CLOSED;
    }
    
    getPort().addLifetimeKeepaliveCount();

    _state = _state.toKeepalive(this);

    // use select manager if available
    if (_port.getSelectManager() != null) {
      _requestStateRef.get().toStartKeepalive(_requestStateRef);
      _state = _state.toKeepaliveSelect();
      
      // keepalive to select manager succeeds
      if (_port.getSelectManager().keepalive(this)) {
        if (log.isLoggable(Level.FINE))
          log.fine(dbgId() + " keepalive (select)");
        
        getPort().addLifetimeKeepaliveSelectCount();

        return RequestState.KEEPALIVE_SELECT;
      }
      else {
        log.warning(dbgId() + " failed keepalive (select)");
        System.out.println("FAILED_KA:");
        _requestStateRef.get().toWakeKeepalive(_requestStateRef);
      }
    }

    return threadKeepalive();
  }
  
  private RequestState threadKeepalive()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + " keepalive (thread)");

    long timeout = getPort().getKeepaliveTimeout();
    long expires = timeout + CurrentTime.getCurrentTimeActual();

    do {
      try {
        long delta = expires - CurrentTime.getCurrentTimeActual();
        
        if (delta < 0) {
          delta = 0;
        }
        
        long result = getReadStream().fillWithTimeout(delta);
        
        if (result > 0) {
          return RequestState.REQUEST_COMPLETE;
        }
        else if (result < 0) {
          return RequestState.CLOSED;
        }
      } catch (SocketTimeoutException e) {
        log.log(Level.FINEST, e.toString(), e);
      } catch (IOException e) {
        log.log(Level.FINEST, e.toString(), e);
        break;
      }
    } while (CurrentTime.getCurrentTimeActual() < expires);

    // close();
    killKeepalive("thread-keepalive timeout (" + timeout + "ms)");
    
    return RequestState.CLOSED;
  }

  //
  // state transitions
  //

  /**
   * Start a connection
   */
  private void toStartConnection()
    throws IOException
  {
    _connectionStartTime = CurrentTime.getCurrentTime();

    setStatState("read");
    initSocket();

    _request.onStartConnection();
  }
  
  /**  
   * Initialize the socket for a new connection
   */
  private void initSocket()
    throws IOException
  {
    _idleTimeout = _port.getKeepaliveTimeout();
    _suspendTimeout = _port.getSuspendTimeMax();

    getWriteStream().init(_socket.getStream());

    // ReadStream cannot use getWriteStream or auto-flush
    // because of duplex mode
    ReadStream is = getReadStream();
    is.init(_socket.getStream(), null);
    
    byte []buffer = is.getBuffer();
    int readLength = buffer.length;
    
    /*
    if (_listener.isAsyncThrottle()) {
      readLength = 0;
    }
    */
    // faster performance by waiting for the read
    readLength = 0;
    
    int len = _socket.acceptInitialRead(buffer, 0, readLength);
    
    if (len > 0) {
      is.setLength(len);
    }

    if (log.isLoggable(Level.FINEST)) {
      log.finest(dbgId() + "starting connection " + this
                 + ", total=" + _port.getConnectionCount());
    }
  }
  
  @Override
  public void clientDisconnect()
  {
    killKeepalive("client disconnect");
    
    TcpAsyncController async = _async;
    
    if (async != null) {
      async.complete();
    }
  }
  /**
   * Kills the keepalive, so the end of the request is the end of the
   * connection.
   */
  @Override
  public void killKeepalive(String reason)
  {
    Thread thread = Thread.currentThread();
    
    SocketLinkState state = _state;
    
    if (thread != _thread && ! state.isAsyncStarted()) {
      throw new IllegalStateException(L.l("{0} killKeepalive called from invalid thread.\n  expected: {1}\n  actual: {2}",
                                          this,
                                          _thread,
                                          thread));
    }
    
    _state = state.toKillKeepalive(this);
    
    if (log.isLoggable(Level.FINE) && state.isKeepaliveAllocated()) {
      log.fine(this + " keepalive disabled from "
               + state +", reason=" + reason);
    }
  }
  
  //
  // idle management (for logging)
  //
  /*
  public void beginThreadIdle()
  {
    _listener.getLauncher().onChildIdleBegin();
  }
  
  public void endThreadIdle()
  {
    _listener.getLauncher().onChildIdleEnd();
  }
  */

  //
  // async/comet state transitions
  //
  
  TcpAsyncController getAsyncController()
  {
    return _async;
  }
  
  /**
   * Starts a comet connection.
   * 
   * Called by the socketLink thread only.
   */
  @Override
  public AsyncController toComet(SocketLinkCometListener cometHandler)
  {
    Thread thread = Thread.currentThread();
    
    if (thread != _thread)
      throw new IllegalStateException(L.l("{0} toComet called from invalid thread.\n  expected: {1}\n  actual: {2}",
                                          this,
                                          _thread,
                                          thread));
    
    TcpAsyncController async = _async;
    
    // TCK
    if (async != null && async.isCompleteRequested()) {
      throw new IllegalStateException(L.l("Comet cannot be requested after complete()."));
    }
    
    _requestStateRef.get().toAsyncStart(_requestStateRef);
    
    _state = _state.toComet();
    
    if (async == null)
      _async = async = new TcpAsyncController(this);
    
    async.initHandler(cometHandler);
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " starting comet " + cometHandler);
    
    return async;
  }
  
  /**
   * Starts a comet connection.
   * 
   * Called by the socketLink thread only.
   */
  @Override
  public AsyncController toCometRestart(SocketLinkCometListener cometHandler)
  {
    
    TcpAsyncController async = _async;
    
    if (async != null && async.isCompleteRequested()) {
      // #5684
      return async;
    }
    
    return toComet(cometHandler);
  }

  private boolean toSuspend()
  {
    _idleStartTime = CurrentTime.getCurrentTime();
    _idleExpireTime = _idleStartTime + _suspendTimeout;

    if (log.isLoggable(Level.FINER))
      log.finer(this + " suspending comet");
    
    _state = _state.toCometSuspend(this);
    
    return _requestStateRef.get().toAsyncSuspendRequest(_requestStateRef);
  }
  
  //
  // duplex/websocket
  //

  /**
   * Starts a full duplex (tcp style) request for hmtp/xmpp
   */
  @Override
  public SocketLinkDuplexController startDuplex(SocketLinkDuplexListener handler)
  {
    Thread thread = Thread.currentThread();
    
    if (thread != _thread)
      throw new IllegalStateException(L.l("{0} toComet called from invalid thread",
                                          this));
    
    _state = _state.toDuplex(this);

    SocketLinkDuplexController duplex = new SocketLinkDuplexController(this, handler);
    
    _duplexReadTask = new DuplexReadTask(this, duplex);
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " starting duplex " + handler);

    try {
      handler.onStart(duplex);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return duplex;
  }
  
  private void toDuplexActive()
  {
    _state = _state.toDuplexActive(this);
  }
  
  //
  // close operations
  //

  private void close()
  {
    setStatState(null);
  
    closeAsync();
      
    closeConnection();
  }
  
  /**
   * Closes the connection.
   */
  private void closeConnection()
  {
    SocketLinkState state = _state;
    _state = state.toClosed(this);
    
    closeConnection(state);
  }

  private void closeConnection(SocketLinkState state)
  {
    AbstractSelectManager selectManager = _port.getSelectManager();
    
    if (selectManager != null && state.isKeepalive()) {
      selectManager.closeKeepalive(this);
    }
    
    if (state.isClosed() || state.isIdle()) {
      return;
    }

    // detach any comet
    /*
    if (state.isComet() || state.isCometSuspend())
      port.cometDetach(this);
      */

    try {
      getWriteStream().close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      getReadStream().close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    TcpPort port = getPort();
    
    QSocket socket = _socket;
    
    if (port != null) {
      port.closeSocket(socket);
    }

    try {
      socket.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      getRequest().onCloseConnection();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (log.isLoggable(Level.FINER)) {
      if (port != null)
        log.finer(dbgId() + "closing connection " + this
                  + ", total=" + port.getConnectionCount());
      else
        log.finer(dbgId() + "closing connection " + this);
    }
  }

  private void closeAsyncIfNotAsync()
  {
    SocketLinkState state = _state;
    
    if (state.isComet() || state.isDuplex())
      return;
    
    closeAsync();
  }
  
  private void closeAsync()
  {
    DuplexReadTask duplexTask = _duplexReadTask;
    _duplexReadTask = null;

    AsyncController async = _async;
    _async = null;

    if (async != null) {
      async.onClose();
      async.close();
    }
    
    if (duplexTask != null)
      duplexTask.onClose();
  }

  private String dbgId()
  {
    if (_dbgId == null) {
      Object serverId = Environment.getAttribute("caucho.server-id");

      if (serverId != null)
        _dbgId = (getClass().getSimpleName() + "[id=" + getId()
                  + "," + serverId + "] ");
      else
        _dbgId = (getClass().getSimpleName() + "[id=" + getId() + "] ");
    }

    return _dbgId;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _id + "," + _port.toURL() + "," + _state + "]";
  }
  
  enum Task {
    ACCEPT,
    KEEPALIVE;
  }
}
