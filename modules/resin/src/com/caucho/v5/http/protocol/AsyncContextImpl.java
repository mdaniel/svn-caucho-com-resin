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

package com.caucho.v5.http.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.network.port.StateConnection;
import com.caucho.v5.util.L10N;

/**
 * Public API to control a comet connection.
 */
public class AsyncContextImpl // extends RequestProtocolBase
  implements AsyncContext { //, CometListenerSocket {
  private static final L10N L = new L10N(AsyncContextImpl.class);
  private static final Logger log = Logger
      .getLogger(AsyncContextImpl.class.getName());

  private ConnectionTcp _connTcp;
  //private AsyncController _cometController;

  private ServletRequest _request;
  private ServletResponse _response;
  
  private boolean _isOriginal;

  private ArrayList<AsyncListenerNode> _listeners;
  private AtomicReference<StateAsync> _stateRef = new AtomicReference<>(StateAsync.IDLE);

  private WebApp _dispatchWebApp;
  private String _dispatchPath;
  private RequestHttpBase _reqHttp;
  
  private long _timeout;
  
  private boolean _isActive;
  
  public AsyncContextImpl(RequestHttpBase httpConn)
  {
    _reqHttp = httpConn;
    _connTcp = httpConn.getConnection();
    // XXX: _cometController = _tcpConnection.toComet(this);
    
    _connTcp.setIdleTimeout(30_000L);
  }
  
  public void restart()
  {
    // XXX: _cometController = _tcpConnection.toComet(this);
  }

  public void start(ServletRequest request,
                   ServletResponse response,
                   boolean isOriginal)
  {
    _request = request;
    _response = response;
    
    switch (_stateRef.get()) {
    case START:
      complete();
      throw new IllegalStateException(L.l("startAsync can't be called on a started context"));
    case COMPLETE:
      throw new IllegalStateException(L.l("startAsync can't be called on a completed context"));
    case IDLE:
    case WAKE:
      _stateRef.set(StateAsync.START);
      break;
      
    default: 
      throw new IllegalStateException(String.valueOf(_stateRef.get()));
    }
    
    _dispatchWebApp = (WebApp) request.getServletContext();

    HttpServletRequest req;
    
    if (request instanceof HttpServletRequest) {
      req = (HttpServletRequest) request;
    }
    else if (request instanceof ServletRequestWrapper) {
      ServletRequestWrapper wrapper = (ServletRequestWrapper) request;
      
      req = (HttpServletRequest) wrapper.getRequest();
    }
    else {
      throw new IllegalStateException(L.l("startAsync requires a HttpServletRequest at {0}", request));
    }
 
    String servletPath = req.getServletPath();
    String pathInfo = req.getPathInfo();
    String queryString = req.getQueryString();

    if (pathInfo == null)
      _dispatchPath = servletPath;
    else if (servletPath == null)
      _dispatchPath = pathInfo;
    else
      _dispatchPath = servletPath + pathInfo;
    
    if (queryString != null) {
      _dispatchPath = _dispatchPath + '?' + queryString;
    }
    
    /* XXX: tck
    if (_cometController == null)
      throw new NullPointerException();
      */
    
    _isOriginal = isOriginal;
    
    ((RequestServlet) req).startAsync(this);
   
    onStart(req, response);
  }

  /**
   * Returns the originating request for the async. 
   */
  @Override
  public ServletRequest getRequest()
  {
    return _request;
  }

  /**
   * Returns the originating request for the async. 
   */
  @Override
  public ServletResponse getResponse()
  {
    return _response;
  }

  @Override
  public boolean hasOriginalRequestAndResponse()
  {
    return _isOriginal;
  }

  /**
   * Sets the suspend/idle timeout for the async request.
   */
  @Override
  public void setTimeout(long idleTimeout)
  {
    if (idleTimeout <= 0) {
      idleTimeout = 3600_000L;
    }
    
    _connTcp.setIdleTimeout(idleTimeout);
  }

  /**
   * Returns the suspend/idle timeout for the async request.
   */
  @Override
  public long getTimeout()
  {
    long timeout = _connTcp.getIdleTimeout();
    
    if (timeout >= 3600_000L) {
      return 0;
    }
    else {
      return timeout;
    }
  }

  /**
   * Tests if the controller is active.
   */
  private boolean isActive()
  {
    return _stateRef.get().isActive();
  }
  
  public boolean isAsyncStarted()
  {
    // return _isActive;
    return _stateRef.get().isStarted();
  }
  
  public boolean isSuspend()
  {
    return _stateRef.get().isSuspend();
  }
  
  public boolean isAsyncComplete()
  {
    return _stateRef.get() == StateAsync.COMPLETE
        || _stateRef.get() == StateAsync.CLOSED;
  }
  
  //
  // dispatch values
  //

  ServletContext getDispatchContext()
  {
    return _dispatchWebApp;
  }

  String getDispatchPath()
  {
    return _dispatchPath;
  }
  
  //
  // AsyncListener
  //

  @Override
  public void addListener(AsyncListener listener)
  {
    addListener(listener, _request, _response);
    
/*
    if (_stateRef.get() == StateAsync.START) {
      try {
        listener.onStartAsync(new AsyncEvent(this, _request, _response));
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
*/
  }

  public void addListener(AsyncListener listener,
                          ServletRequest request,
                          ServletResponse response)
  {
    if (_listeners == null) {
      _listeners = new ArrayList<AsyncListenerNode>();
    }
    
    _listeners.add(new AsyncListenerNode(listener, request, response, null));
  }

  @Override
  public <T extends AsyncListener> T createListener(Class<T> cl)
    throws ServletException
  {
    return _request.getServletContext().createListener(cl);
  }
  
  //
  // Async dispatching
  //

  @Override
  public void dispatch()
  {
    if (_stateRef.get() == StateAsync.COMPLETE) {
      // servlet/1l9a
      return;
    }
    
    /*
    else if (! cometController.isAsyncStarted())
      throw new IllegalStateException(L.l("dispatch is not valid when async cycle has not started, i.e. before startAsync."));
      */
    
    if (! _stateRef.compareAndSet(StateAsync.START, StateAsync.WAKE)) {
      throw new IllegalStateException(L.l("dispatch is not valid when no AsyncContext is available {0}",
                                          this));
    }
     
    _connTcp.proxy().requestWake();
    // cometController.wake();
  }

  @Override
  public void dispatch(String path)
  {
    _dispatchPath = path;

    dispatch();
  }

  @Override
  public void dispatch(ServletContext context, String path)
  {
    _dispatchWebApp = (WebApp) context;
    _dispatchPath = path;

    dispatch();
  }

  @Override
  public void start(Runnable task)
  {
    if (isActive()) {
      ThreadPool.getCurrent().schedule(task);
    }
    else
      throw new IllegalStateException(
          L.l("AsyncContext.start() is not allowed because the AsyncContext has been completed."));
  }

  /**
   * Completes the comet connection
   */
  @Override
  public void complete()
  {
    // AsyncController cometController = _cometController;
    
    //boolean isSuspend = _tcpConnection.isCometSuspend();
    
    StateAsync state = _stateRef.get();
    _stateRef.compareAndSet(state, state.toComplete());
    
    _connTcp.proxy().requestWake();
    /*
    Thread thread = Thread.currentThread();
    
    if (thread == _connTcp.getThread()) {
      onComplete();
    }
    */
    // _reqHttp.completeAsync();

    // TCK: 
    // XXX: should be on completing thread.
    //if (! isSuspend) {
    //onComplete();
    //}
  }
  
  //
  // CometHandler callbacks
  //
  
  /**
   * CometHandler callback when the connection starts.
   */
  public void onStart(ServletRequest request,
                      ServletResponse response)
  {
    if (_listeners == null)
      return;
    
    AsyncEvent event = new AsyncEvent(this, request, response);
    
    for (AsyncListenerNode node : _listeners) {
      try {
        node.onStart(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
  
  /**
   * CometHandler callback when the connection times out.
   */
  //@Override
  public void onTimeout()
  {
    StateAsync state = _stateRef.get();
    _stateRef.compareAndSet(state, state.toComplete());
    
    if (_listeners == null)
      return;
    
    AsyncEvent event = new AsyncEvent(this, _request, _response);
    
    for (AsyncListenerNode node : _listeners) {
      try {
        node.onTimeout(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  //@Override
  public StateConnection service() throws IOException
  {
    _stateRef.compareAndSet(StateAsync.START, StateAsync.WAKE);
    // _stateRef.compareAndSet(StateAsync.WAKE, StateAsync.IDLE);
    
    if (_stateRef.get() == StateAsync.COMPLETE) {
      _reqHttp.completeAsync();
      onComplete();
    }
    
    _isActive = true;
    try {
      return _reqHttp.handleResume();
    } finally {
      _isActive = false;
    }
  }
  
  //@Override
  public void onCloseConnection()
  {
    onComplete();
  }
  
  /**
   * CometHandler callback when the connection times out.
   */
  public void onError()
  {
    if (_listeners == null)
      return;
    
    AsyncEvent event = new AsyncEvent(this, _request, _response);
    
    for (AsyncListenerNode node : _listeners) {
      try {
        node.onError(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
  
  // @Override
  public void onComplete()
  {
    StateAsync state = _stateRef.get();
    
    if (state == StateAsync.CLOSED) {
      return;
    }
    
    _stateRef.compareAndSet(state, StateAsync.CLOSED);
    
    if (_listeners == null)
      return;
    
    AsyncEvent event = new AsyncEvent(this, _request, _response);
    
    for (AsyncListenerNode node : _listeners) {
      try {
        node.onComplete(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  public StateConnection getNextState()
  {
    StateConnection nextState = StateConnection.READ;
    switch (_stateRef.get()) {
    case IDLE:
      return nextState;
    case START:
      return StateConnection.SUSPEND;
    case COMPLETE:
    case WAKE:
      return StateConnection.WAKE;
    case CLOSED:
      return nextState;
    default:
      return nextState;
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _reqHttp.getRequestURI()
            + "," + _stateRef.get()
            + "," + _connTcp.getId()
            + "," + _connTcp.getStateName() + "]");
  }
  
  enum StateAsync {
    IDLE,
    START {
      @Override
      boolean isStarted() { return true; }
      
      @Override
      boolean isSuspend() { return true; }

      @Override
      boolean isActive() { return true; }
    },
    
    WAKE {
      @Override
      boolean isSuspend() { return true; }
    },
    COMPLETE {
      // servlet/1la6
      //@Override
      //boolean isSuspend() { return true; }
      @Override
      boolean isStarted() { return true; }
    },
    CLOSED {
      @Override
      StateAsync toComplete() { return CLOSED; }
    };
    
    boolean isStarted()
    {
      return false;
    }
    
    boolean isSuspend()
    {
      return false;
    }
    
    boolean isActive()
    {
      return false;
    }
    
    StateAsync toComplete()
    {
      return COMPLETE;
    }
  }
}
