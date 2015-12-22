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

package com.caucho.v5.ramp.jamp;

import io.baratine.core.Result;
import io.baratine.core.ResultFuture;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.ServiceRefAmp;
import com.caucho.v5.amp.actor.ServiceRefUnauthorized;
import com.caucho.v5.amp.inbox.OutboxAmpBase;
import com.caucho.v5.amp.remote.ChannelServer;
import com.caucho.v5.amp.remote.GatewayReply;
import com.caucho.v5.amp.spi.HeadersAmp;
import com.caucho.v5.amp.spi.InboxAmp;
import com.caucho.v5.amp.spi.LookupAmp;
import com.caucho.v5.amp.spi.MethodRefAmp;
import com.caucho.v5.amp.spi.OutboxAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.util.L10N;

/**
 * Channel for jamp-rpc.
 */
public class ChannelServerJampRpcServlet implements ChannelServer
{
  private static final Logger log
    = Logger.getLogger(ChannelServerJampRpcServlet.class.getName());
  
  private static final L10N L = new L10N(ChannelServerJampRpcServlet.class);
  
  private final JampServletBase _servlet;
  
  private ConcurrentHashMap<String,ServiceRefAmp> _linkServiceMap;
  private ArrayList<ServiceRefAmp> _serviceCloseList;
  
  private final ServiceManagerAmp _manager;
  private final LookupAmp _registry;
  
  private HttpServletRequest _req;
  private HttpServletResponse _res;
  
  //private ResultJampRpc<JampRestMessage> _future;
  private ResultFuture<JampRestMessage> _future;

  private String _sessionId;

  private OutboxAmp _outbox;

  public ChannelServerJampRpcServlet(JampServletBase servlet,
                              ServiceManagerAmp manager,
                              LookupAmp registry,
                              QueueJampUnpark unparkQueue)
  {
    _servlet = servlet;
    _manager = manager;
    _registry = registry;
    
    // _unparkQueue = unparkQueue;
    
    _outbox = new OutboxAmpBase();
    _outbox.setInbox(manager.getSystemInbox());
    _outbox.setMessage(manager.getSystemMessage());
  }
  
  void init(HttpServletRequest req, HttpServletResponse res)
  {
    _req = req;
    _res = res;
    
    _future = null;
  }
  
  public OutboxAmp getOutbox()
  {
    return _outbox;
  }

  public void initSession(ChannelServerJampRpcServlet sessionRpc)
  {
    if (sessionRpc == null) {
      return;
    }
    
    _linkServiceMap = sessionRpc._linkServiceMap;
    _serviceCloseList = sessionRpc._serviceCloseList;
    _sessionId = sessionRpc._sessionId;
  }
  
  void finish()
  {
    _req = null;
    _res = null;
    _future = null;
  }

  public JampRestMessage pollMessage(long timeout, TimeUnit unit)
  {
    //ResultJampRpc<JampRestMessage> future = _future;
    ResultFuture<JampRestMessage> future = _future;
    
    if (future != null) {
      try {
        return future.get(timeout, unit);
      } catch (Throwable exn) {
        log.log(Level.FINE, exn.toString(), exn);
        
        return null;
      }
    }
    else {
      return null;
    }
  }


  @Override
  public ServiceRefAmp getServiceRefOut()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /*
  @Override
  public final OutboxAmp createOutbox()
  {
    // OutboxAmpDirect outbox = new OutboxAmpDirect();
    OutboxAmpBase outbox = new OutboxAmpBase();
    outbox.setInbox(_manager.getSystemInbox());
    outbox.setMessage(_manager.getSystemMessage());
    
    return outbox;
  }
  */
  
  @Override
  public final InboxAmp getInbox()
  {
    // OutboxAmpDirect outbox = new OutboxAmpDirect();
    return _manager.getSystemInbox();
  }
  
  @Override
  public ServiceManagerAmp getManager()
  {
    return _manager;
  }
  
  protected String getSessionId()
  {
    if (_sessionId == null) {
      _sessionId = _servlet.createSession(this, _req, _res);
    }
    
    return _sessionId;
  }
  
  /**
   * Mark the link as authenticated. When isLogin is true, the client
   * can access published services.
   * 
   * @uid the user id that logged in.
   */
  @Override
  public void onLogin(String uid)
  {
  }
  
  ServiceRefAmp getLink(String address)
  {
    ConcurrentHashMap<String, ServiceRefAmp> linkMap = _linkServiceMap;
    
    if (linkMap != null) {
      return linkMap.get(address);
    }
    else {
      return null;
    }
  }
  
  void putLink(String address, ServiceRefAmp serviceRef)
  {
    synchronized (this) {
      ConcurrentHashMap<String, ServiceRefAmp> linkMap = _linkServiceMap;
    
      if (linkMap == null) {
        linkMap = _linkServiceMap = new ConcurrentHashMap<>();
      }
      
      linkMap.put(address, serviceRef);
    }
  }
  
  @Override
  public MethodRefAmp lookupMethod(String address, String methodName)
  {
    ServiceRefAmp linkService = getLink(address);

    if (linkService != null) {
      return linkService.getMethod(methodName);
    }
    
    MethodRefAmp methodRef = _registry.lookupMethod(address, methodName);
    ServiceRefAmp serviceRef = methodRef.getService();
    String addressService = serviceRef.address();

    if (addressService.startsWith("session:")) {
      ServiceRefAmp sessionRef = lookupSession(serviceRef);
      
      putLink(address, sessionRef);

      return sessionRef.getMethod(methodName);
    }
    else if (serviceRef.isPublic()) {
      return methodRef;
    }
    /*
    else if (address.startsWith("/")
             || addressService.startsWith("public:")) {
      return methodRef;
    }
    */
    
    if (log.isLoggable(Level.FINE)) {
      log.fine("unauthorized service " + address + " from " + this);
    }
      
    return new ServiceRefUnauthorized(getManager(), address).getMethod(methodName);
  }
  
  @Override
  public ServiceRefAmp lookup(String address)
  {
    ServiceRefAmp linkActor = getLink(address);

    if (linkActor != null) {
      return linkActor;
    }
    
    ServiceRefAmp serviceRef = _registry.lookup(address);
    String addressService = serviceRef.address();
    
    if (addressService.startsWith("session:")) {
      ServiceRefAmp sessionRef = lookupSession(serviceRef);
      
      putLink(address, sessionRef);

      return sessionRef;
    }
    else if (address.startsWith("/")) {
      return serviceRef;
    }
    else if (addressService.startsWith("public:")) {
      return serviceRef;
    }
    
    if (log.isLoggable(Level.FINE)) {
      log.fine("unauthorized service " + address + " from " + this);
    }
      
    return new ServiceRefUnauthorized(getManager(), address);
  }
  
  protected ServiceRefAmp lookupSession(ServiceRefAmp serviceRef)
  {
    return (ServiceRefAmp) serviceRef.lookup("/" + getSessionId());
  }
  
  @Override
  public GatewayReply createGatewayReply(String remoteName)
  {
    //ResultJampRpc<JampRestMessage> future
    //  = new ResultJampRpc<>(_unparkQueue);
    
    ResultFuture<JampRestMessage> future
      = new ResultFuture<>();
    
    _future = future;
      
    return new GatewayReplyJampRpc(remoteName, future);
  }
  
  @Override
  public ServiceRefAmp createGatewayRef(String remoteName)
  {
    throw new IllegalArgumentException(L.l("jamp-rpc cannot support ServiceRef arguments"));
  }

  /**
   * Called when the link is closing.
   */
  @Override
  public void shutdown(ShutdownModeAmp mode)
  {
    // jamp/3210
    //getReadMailbox().close();

    for (int i = _serviceCloseList.size() - 1; i >= 0; i--) {
      ServiceRefAmp service = _serviceCloseList.get(i);

      service.shutdown(mode);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _sessionId + "]";
  }
  
  private class GatewayReplyJampRpc implements GatewayReply
  {
    private final String _remoteName;
    //private final ResultJampRpc<JampRestMessage> _future;
    private final ResultFuture<JampRestMessage> _future;
    
    public GatewayReplyJampRpc(String remoteName,
                               ResultFuture<JampRestMessage> future)
    {
      Objects.requireNonNull(future);
      
      _remoteName = remoteName;
      _future = future;
    }
    
    @Override
    public boolean isAsync()
    {
      return true;
    }
    
    @Override
    public <V> void completeAsync(Result<V> result, V value)
    {
      _future.completeFuture(result, value);
    }

    @Override
    public void queryReply(HeadersAmp headers, 
                           long qid,
                           Object value)
    {
      JampRestMessage msg;
      
      msg = new JampRestMessage.Reply(headers, _remoteName, qid, value);
      
      _future.ok(msg);
    }

    @Override
    public void queryError(HeadersAmp headers, 
                           long qid, 
                           Throwable exn)
    {
      JampRestMessage msg;
      
      msg = new JampRestMessage.Error(headers, _remoteName, qid, exn);
      
      _future.ok(msg);
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _remoteName + "]";
    }
  }
}
