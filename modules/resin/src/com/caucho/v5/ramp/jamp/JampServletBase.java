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

import io.baratine.core.ServiceException;
import io.baratine.core.ServiceExceptionNotFound;
import io.baratine.core.ServiceExceptionUnavailable;
import io.baratine.core.ServiceRef;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerEndpointConfig;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.ServiceRefAmp;
import com.caucho.v5.amp.actor.ServiceRefHandle;
import com.caucho.v5.amp.actor.ServiceRefNull;
import com.caucho.v5.amp.remote.ChannelManagerService;
import com.caucho.v5.amp.remote.ChannelManagerServiceImpl;
import com.caucho.v5.amp.remote.ChannelServer;
import com.caucho.v5.amp.remote.ChannelServerFactoryImpl;
import com.caucho.v5.amp.spi.OutboxAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.json.io.JsonReader;
import com.caucho.v5.json.ser.JsonSerializerFactory;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.ramp.jamp.JampPodManager.PodContext;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.LruCache.Entry;
import com.caucho.v5.websocket.server.WebSocketServletDispatch;
import com.caucho.v5.websocket.server.WebSocketServletDispatch.WebSocketContextDispatch;

/**
 * JampServlet responds to JAMP websocket connections.
 */
//@WebServlet(asyncSupported=true)
abstract public class JampServletBase extends GenericServlet
{
  private static final L10N L = new L10N(JampServletBase.class);
  private static final Logger log 
    = Logger.getLogger(JampServletBase.class.getName());
  
  private static final String APPLICATION_JAMP = "x-application/jamp";
  
  private static final String APPLICATION_JAMP_RPC = "x-application/jamp-rpc";
  
  private static final String APPLICATION_JAMP_PUSH = "x-application/jamp-push";
  private static final String APPLICATION_JAMP_PULL = "x-application/jamp-pull";
  private static final String APPLICATION_JAMP_POLL = "x-application/jamp-poll";
  
  private static final String CHANNEL_COOKIE = "Jamp_Channel";
  
  // private ServiceManagerAmp _ampManagerLocal;
  private SessionContextJamp _sessionContext;
  
  private final JsonSerializerFactory _jsonFactory;
  
  private final LruCache<String,ChannelServer> _sessionMap
    = new LruCache<>(16 * 1024);
    
  private final ConcurrentHashMap<String,JampRestServerSkeleton> _serviceSkelMap
    = new ConcurrentHashMap<>();
  
  private final ConcurrentHashMap<String,PodContext> _podMap
    = new ConcurrentHashMap<>();
  
  private ConcurrentHashMap<String,WebSocketContextPod> _wsMap
    = new ConcurrentHashMap<>();
  
  private final AtomicLong _sequence = new AtomicLong();
  
  private final Lifecycle _lifecycle = new Lifecycle();
  
  private long _alarmTimeout = 5000L;
  
  private long _sessionTimeout = 5 * 60000L;
  
  private long _pullTimeout = 15000L;
  private long _pollTimeout = 100;
  
  private long _rpcTimeout = 15000L;
  
  // private RampFailoverStatus _failover;
  
  private Alarm _alarm = new Alarm(new ChannelTimeout());

  private ChannelServerFactoryImpl _wsBrokerFactory;
  private ChannelManagerServiceImpl _sessionManager;
  private JampPodManager _podManager;
  private WebSocketServletDispatch _webSocket;

  public JampServletBase()
  {
    _podManager = createPodManager();
    _sessionManager = _podManager.getSessionManager();
    
    _sequence.set(CurrentTime.getCurrentTime() << 24);

    _jsonFactory = createJsonFactory();
    
    _webSocket = new WebSocketServletDispatch();
    _webSocket.setContextFactory(x->getContext(x));
    // _webSocket.setConfigFactory(()->createConfig());
  }
  
  protected JampPodManager createPodManager()
  {
    return new JampPodManager();
  }
  
  public void setPullTimeout(Period period)
  {
    _pullTimeout = period.getPeriod();
  }
  
  public void setPollTimeout(Period period)
  {
    _pollTimeout = period.getPeriod();
  }
  
  public void setRpcTimeout(Period period)
  {
    _rpcTimeout = period.getPeriod();
  }
  
  public void setChannelTimeout(Period period)
  {
    _sessionTimeout = period.getPeriod();
  }
  
  @Override
  public void init()
    throws ServletException
  {
    super.init();
    
    // WebApp webApp = (WebApp) getServletContext();
    
    // _ampManager = webApp.getRampManager();
    
    _lifecycle.toActive();
    
    // _failover = RampFailoverStatus.getCurrent();
    
    _alarm.queue(_alarmTimeout);
  }
  
  @Override
  public void destroy()
  {
    _lifecycle.toDestroy();
    
    _alarm.dequeue();
    
    super.destroy();
    
  }
  
  protected String getAuthority(String podName)
  {
    return "public://";
  }
  
  private WebSocketContextDispatch getContext(String pathInfo)
  {
    String podName = getPodName(pathInfo);

    Objects.requireNonNull(podName);
    
    WebSocketContextPod wsCxtPod = _wsMap.get(podName);
    
    if (wsCxtPod == null || wsCxtPod.isClosed()) {
      PodContext podContext = _podManager.getPodContextByName(podName);
      
      if (podContext.getAmpManager() == null) {
        throw new ServiceException(L.l("'{0}' is an inactive pod", podName));
      }
      
      EndpointJampConfigServer config;
      
      config = new EndpointJampConfigServer(podName,
                                            podContext.getAmpManager(),
                                            getChannelContext(),
                                            podContext.getWsRegistryFactory(),
                                            podContext.getJsonFactory());
      
      WebSocketContextDispatch wsCxt = new WebSocketContextDispatch(config);
      
      WebSocketContextPod wsCxtPodNew
        = new WebSocketContextPod(wsCxt, podContext.getAmpManager());

      if (wsCxtPod != null) {
        _wsMap.replace(podName, wsCxtPod, wsCxtPodNew);
      }
      else {
        _wsMap.putIfAbsent(podName, wsCxtPodNew);
      }
      
      wsCxtPod = _wsMap.get(podName);
    }

    return wsCxtPod.getContext();
  }

  /*
  private PodContext getPodContext(String pathInfo)
  {
    String podName = getPodName(pathInfo);
    
    return _podManager.getPodContextByName(podName);
  }
  */
    
  /*
  private PodContext getPodContextByName(String podName)
  {
    PodContext podContext = _podMap.get(podName);
    
    // if (podContext == null || podContext.isModified()) {
    if (podContext == null) {
      PodContext newPodContext = createPodContext(podName);
      
      _podMap.putIfAbsent(podName, newPodContext);
      
      podContext = _podMap.get(podName);
    }
    
    return podContext;
  }
  */
  
  protected String getPodName(String pathInfo)
  {
    return "";
  }
  
  protected JsonSerializerFactory createJsonFactory()
  {
    JsonSerializerFactory jsonFactory = new JsonSerializerFactory();
    
    jsonFactory.addSerializer(ServiceRefHandle.class,
                              new JsonSerializerServiceRef());
    
    jsonFactory.addDeserializer(ServiceRef.class,
                                new JsonDeserializerServiceRef());
    
    return jsonFactory;
  }
  
  protected ChannelServerFactoryImpl
  createChannelFactory(Supplier<ServiceManagerAmp> manager, 
                        String podName,
                        ChannelManagerService sessionManager)
  {
    return new ChannelServerFactoryImpl(manager, sessionManager, podName);
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    try {
      if ("websocket".equals(req.getHeader("Upgrade"))
          && request instanceof RequestServlet) {
        _webSocket.service((RequestServlet) request, (ResponseServlet) response);
        return;
      }

      serviceImpl(req, res);
    } finally {
      getChannelContext().start(null);
    }
  }
  
  private void serviceImpl(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PodContext podContext = _podManager.getPodContext(req.getPathInfo());
    
    String method = req.getMethod();

    String origin = req.getHeader("Origin");

    if (origin != null && "OPTIONS".equals(method)) {
      allowAll(req, res, origin, true);

      return;
    }
    else if (origin != null) {
      allowAll(req, res, origin, false);
    }
    
    if (! podContext.isLocal()) {
      redirect(req, res);
      return;
    }

    String contentType = req.getContentType();
    
    if (contentType != null) {
      int p = contentType.indexOf(';');
      
      if (p > 0) {
        contentType = contentType.substring(0, p).trim();
      }
    }
      
    if ((APPLICATION_JAMP.equals(contentType)
        || APPLICATION_JAMP_RPC.equals(contentType))
        && "POST".equals(method)) {
      ChannelServer channel = null;
      
      String connId = getConnectionId(req);
      
      if (connId != null) {
        channel = _sessionMap.get(connId);
      }
      
      ChannelServerJampRpcServlet channelRpc;
      
      channelRpc = new ChannelServerJampRpcServlet(this,
                                            podContext.getAmpManager(),
                                            podContext.getLookup(),
                                            podContext.getUnparkQueue());
      
      if (channel instanceof ChannelServerJampRpcServlet) {
        ChannelServerJampRpcServlet sessionRpc = (ChannelServerJampRpcServlet) channel;
        
        channelRpc.initSession(sessionRpc);
      }
      
      // Outbox<Object> outboxPrev = OutboxThreadLocal.getCurrent();
      
      // OutboxAmp outbox = OutboxAmp.current();
                   
      try {
        // OutboxThreadLocal.setCurrent(channelRpc.getOutbox());
        
        channelRpc.init(req, res);
        
        doServiceJampRpc(req, res, podContext, channelRpc);
      } finally{
        channelRpc.finish();

        ServiceRef.flushOutbox();
        /*
        if (outbox != null) {
          outbox.flush();
        }
        */
        // OutboxThreadLocal.setCurrent(outboxPrev);
      }
    }
    else if (APPLICATION_JAMP_PUSH.equals(contentType) && "POST".equals(method)) {
      doServiceJampPush(req, res, podContext, 
                        getChannelRegistry(req, res, podContext));
    }
    else if (APPLICATION_JAMP_PULL.equals(contentType)
        && ("POST".equals(method) || "GET".equals(method))) {
      doServiceJampPull(req, res, podContext, 
                        getChannelRegistry(req, res, podContext));
    }
    else if (APPLICATION_JAMP_POLL.equals(contentType)) {
      doServiceJampPoll(req, res, podContext, 
                        getChannelRegistry(req, res, podContext));
    }
    else {
      doRestJamp(req, res, podContext);
    }
  }
  
  private void redirect(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    String contextPath = req.getContextPath();
    String servletPath = req.getServletPath();
    String pathInfo = req.getPathInfo();
  
    String podName = getPodName(pathInfo);
    
    String address = getPodForward(podName);
    
    String queryString = req.getQueryString();
    
    if (queryString != null) {
      queryString = "?" + queryString;
    }
    else {
      queryString = "";
    }
    
    if (address != null) {
      if (req.isSecure()) {
        res.sendRedirect("https://" + address + contextPath + servletPath + pathInfo + queryString);
      }
      else {
        res.sendRedirect("http://" + address + contextPath + servletPath + pathInfo + queryString);
      }
    }
    else {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
  
  protected String getPodForward(String pathName)
  {
    return null;
  }
  
  protected ChannelServerJampPull getChannelRegistry(HttpServletRequest req,
                                                     HttpServletResponse res,
                                                     PodContext podContext)
  {
    String connId = getConnectionId(req);
    
    ChannelServer channel = null;
    ChannelServerJampPull channelJamp = null;

    if (connId == null || (channel = _sessionMap.get(connId)) == null) {
      OutJampPull conn = new OutJampPull();
      
      channelJamp = podContext.getSessionRegistryFactory().create(conn);
      
      channelJamp.setSessionTimeout(_sessionTimeout);
      
      channelJamp.initRequest(req);
      
      connId = getConnectionId(channelJamp);
      
      conn.setId(connId);
      
      res.addCookie(new Cookie(CHANNEL_COOKIE, connId));
      
      _sessionMap.put(connId, channelJamp);
    }
    else {
      channelJamp = (ChannelServerJampPull) channel;
    }
    
    channelJamp.updateSessionExpires();
    
    getChannelContext().start(channelJamp);
    
    return channelJamp;
  }
  
  private String getConnectionId(ChannelServerJampPull registryConn)
  {
    String address = registryConn.getAddress();
    
    int p = address.lastIndexOf("/");

    return address.substring(p + 1);
    
  }

  private String getConnectionId(HttpServletRequest req)
  {
    Cookie []cookies = req.getCookies();
    
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (CHANNEL_COOKIE.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    
    return null;
  }
  
  /*
  protected ServiceManagerAmp getAmpManager()
  {
    return _ampManager;
  }
  */
  
  protected ServiceManagerAmp createAmpManager()
  {
    return Amp.getContextManager();
  }
  

  //@Override
  protected ServerEndpointConfig createConfig()
  {
    String path = "/";
    
    // XXX:
    return new EndpointJampConfigServer(path,
                                        _podManager.getPodContext(path).getAmpManager(),
                                        getChannelContext(),
                                        _wsBrokerFactory,
                                        _jsonFactory);
  }
  
  private SessionContextJamp getChannelContext()
  {
    if (_sessionContext == null) {
      _sessionContext = new SessionContextJamp();
    }
    
    return _sessionContext;
  }
  
  public void doServiceJampRpc(HttpServletRequest req,
                               HttpServletResponse res,
                               PodContext podContext,
                               ChannelServerJampRpcServlet channel)
    throws IOException, ServletException
  {
    InJamp in = new InJamp(channel, _jsonFactory); // , outbox);
    
    try (OutboxAmp outbox = OutboxAmp.currentOrCreate(channel.getManager())) {
      int queryCount = in.readMessages(req.getReader(), outbox);
      
      PrintWriter pw = res.getWriter();
      OutJamp out = new OutJamp(_jsonFactory);
      out.init(pw);
      
      JampRestMessage msg;
      
      if (queryCount > 0) {
        if ((msg = channel.pollMessage(_rpcTimeout, TimeUnit.MILLISECONDS)) != null) {
          pw.print("[");
          msg.write(out);
          pw.print("]");
          
          return;
        }
      }
      else {
        outbox.flush();
      }
      
      pw.print("[]");
      
      //outbox.flush();
      
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
        throw e;
      }
      
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().println(e.toString());
    } finally {
      // OutboxThreadLocal.setCurrent(oldOutbox);
      //outbox.flush();
    }
  }

  String createSession(ChannelServerJampRpcServlet channel,
                       HttpServletRequest req,
                       HttpServletResponse res)
  {
    String sessionId = _sessionManager.generateSessionId();
    
    _sessionMap.put(sessionId, channel);
    
    res.addCookie(new Cookie("Jamp_Channel", sessionId));
    
    return sessionId;
  }
  
  public void doServiceJampPush(HttpServletRequest req,
                                HttpServletResponse res,
                                PodContext podContext,
                                ChannelServerJampPull channel)
    throws IOException, ServletException
  {
    InJamp in = new InJamp(channel);

    // OutboxAmp outbox = in.getOutbox();
    // OutboxThreadLocal.setCurrent(outbox);
    
    try (OutboxAmp outbox = OutboxAmp.currentOrCreate(channel.getManager())) {
      JsonReader jIn = in.startSequence(req.getReader());
      
      while (in.readMessage(jIn) != null) {
      }
      
      // outbox.flush();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
        throw e;
      }
      
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().println(e.toString());
    }
  }
  
  public void doServiceJampPull(HttpServletRequest req,
                                HttpServletResponse res,
                                PodContext podContext,
                                ChannelServerJampPull readBroker)
    throws IOException, ServletException
  {
    AsyncContext async = null;
    
    try {
      JampRestMessage msg = readBroker.pollMessage(0, TimeUnit.MILLISECONDS);
      
      if (req.getAttribute(AsyncContext.ASYNC_REQUEST_URI) != null) {
        async = req.getAsyncContext();
      }
      else if (msg == null) {
        async = req.startAsync();
        async.setTimeout(_pullTimeout + _alarmTimeout + 5000);
        
        msg = readBroker.pollAsync(async, _pullTimeout);
        
        if (msg == null) {
          async = null;
          return;
        }
      }
      
      PrintWriter pw = res.getWriter();
      OutJamp out = new OutJamp(_jsonFactory);
      out.init(pw);
      
      pw.print("[");

      if (msg != null) {
        msg.write(out);
      
        while ((msg = readBroker.pollMessage(0, TimeUnit.MILLISECONDS)) != null) {
          pw.println(",");
        
          msg.write(out);
        }
      }
      
      pw.println("]");
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
        throw e;
      }
      
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().println(e.toString());
    } finally {
      if (async != null) {
        async.complete();
      }
    }
  }
  
  public void doServiceJampPoll(HttpServletRequest req,
                                HttpServletResponse res,
                                PodContext podContext,
                                ChannelServerJampPull channelIn)
    throws IOException, ServletException
  {
    InJamp in = new InJamp(channelIn);

    try {
      if ("POST".equals(req.getMethod())) {
        JsonReader jIn = in.startSequence(req.getReader());
      
        // OutboxAmp outbox = in.getOutbox();
        try (OutboxAmp outbox = OutboxAmp.currentOrCreate(channelIn.getManager())) {
          while (in.readMessage(jIn) != null) {
          }
          outbox.flush();
        }
      }
        
      PrintWriter pw = res.getWriter();
      OutJamp out = new OutJamp(_jsonFactory);
      out.init(pw);
      
      pw.print("[");
        
      JampRestMessage msg = channelIn.pollMessage(_pollTimeout,
                                                   TimeUnit.MILLISECONDS);

      if (msg != null) {
        msg.write(out);
      
        while ((msg = channelIn.pollMessage(0, TimeUnit.MILLISECONDS)) != null) {
          pw.print(",\n");
        
          msg.write(out);
        }
      }
      
      pw.println("]");
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
        throw e;
      }
      
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().println(e.toString());
    }
  }
  
  public void doRestJamp(HttpServletRequest req,
                         HttpServletResponse res,
                         PodContext podContext)
    throws IOException, ServletException
  {
    // ServiceManagerAmp ampManager = podContext.getAmpManager();
    // String podName = podContext.getPodName();
    
    String servicePath = getServicePath(req, podContext);
    
    String methodName = getMethodName(req, podContext);
    
    try {
      JampMethodRest jampMethod = getMethod(podContext, servicePath, methodName, req, res);

      if (jampMethod == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      switch (req.getMethod()) {
      case "POST":
        jampMethod.doPost(req, res, servicePath);
        break;

      case "GET":
        jampMethod.doGet(req, res, servicePath);
        break;
        
      case "PUT":
        jampMethod.doPut(req, res, servicePath);
        break;
        
      case "DELETE":
        jampMethod.doDelete(req, res, servicePath);
        break;

      default:
        res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        break;
      }
    } catch (ServiceExceptionNotFound e) {
      log.log(Level.FINE, e.toString(), e);
      
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (ServiceExceptionUnavailable e) {
      log.log(Level.FINE, e.toString(), e);
      
      // res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (ServiceException e) {
      if (log.isLoggable(Level.FINER)) {
        throw e;
      }
      
      log.log(Level.FINE, e.toString(), e);
      
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.setContentType("text/plain");
      
      PrintWriter out = res.getWriter();
      
      out.println(e.toString());
    }
  }
  
  protected String getServicePath(HttpServletRequest req,
                                  PodContext podContext)
  {
    return podContext.getServicePath(req.getPathInfo());
  }
  
  protected String getMethodName(HttpServletRequest req,
                                  PodContext podContext)
  {
    String methodName = req.getParameter("m");
    
    if ("PUT".equals(req.getMethod())) {
      return "put";
    }
    else if ("DELETE".equals(req.getMethod())) {
      return "delete";
    }
    else if (methodName != null) {
      return methodName;
    }
    else if ("GET".equals(req.getMethod())) {
      return "get";
    }
    else {
      return null;
    }
  }
  
  protected JampMethodRest getMethod(PodContext podContext,
                                     String serviceName, 
                                     String methodName,
                                     HttpServletRequest req,
                                     HttpServletResponse res)
  {
    if (serviceName == null) {
      return null;
    }
    
    if (methodName == null) {
      return null;
    }
    
    ServiceRefAmp serviceRef = lookupService(podContext,
                                          serviceName, req, res); 

    if (serviceRef == null) {
      if (log.isLoggable(Level.FINE)) {
        log.fine(L.l("Service {0} does not exist.", serviceName));
      }
      
      return null;
    }

    // return null;
    
    /*
    if (isLoginRequired()) {
      return null;
    }
    */

    JampRestServerSkeleton skel = _serviceSkelMap.get(serviceName);
    
    if (skel == null) {
      skel = createSkeleton(serviceRef);
    }
    
    JampMethodRest method = getMethod(skel, methodName);
    
    if (method == null) {
      if (log.isLoggable(Level.FINE)) {
        log.fine(L.l("Method '{0}' in service '{1}' does not exist", 
                     methodName, serviceRef));
      }
    }
    
    return method;
  }
  
  protected JampRestServerSkeleton createSkeleton(ServiceRefAmp serviceRef)
  {
    return new JampRestServerSkeleton(serviceRef, _jsonFactory);
  }

  protected ServiceRefAmp lookupService(PodContext podContext,
                                     String serviceName,
                                     HttpServletRequest req,
                                     HttpServletResponse res)
  {
    // String podName = podContext.getPodName();
    ServiceManagerAmp ampManager = podContext.getAmpManager();
    
    ServiceRefAmp sessionRef = ampManager.lookup("session://" + serviceName);
    
    if (! sessionRef.isClosed()) {
      /*
      RegistryAmpInServerJamp registryChannel
        = getChannelBroker(req, res, podContext);
        */
      ChannelServerJampPull registry = getChannelRegistry(req, res, podContext);
      
      String path = "session://" + serviceName + "/" + registry.getSessionId();
      
      ServiceRefAmp serviceRef = ampManager.lookup(path);

      if (serviceRef != null && ! serviceRef.isClosed()) {
        return serviceRef;
      }
    }
    
    // baratine/807d
    //String address = "pod://" + podName + serviceName;
    String address = "public://" + serviceName;

    ServiceRefAmp serviceRef = ampManager.lookup(address);

    if (serviceRef.isPublic()) {
      return serviceRef;
    }
    else {
      return new ServiceRefNull(ampManager, address);
    }
  }

  /*
  protected ServiceRef lookupService(PodContext podContext,
                                     String serviceName,
                                     HttpServletRequest req,
                                     HttpServletResponse res)
  {
    ServiceManagerAmp ampManager = podContext.getAmpManager();
    
    ServiceRefAmp serviceRef = ampManager.lookup(serviceName); // "public://" + serviceName);
      
    if (serviceRef.isPublic()) {
      return serviceRef;
    }
    else if (serviceRef.isValid()) {
      return null;
    }
    else {
      ChannelServerJamp broker = getChannelRegistry(req, res, podContext);
        
      String connId = getConnectionId(broker);
        
      ServiceRefAmp sessionRef = ampManager.lookup("session://" + serviceName + "/" + connId);
        
      if (sessionRef.isValid()) {
        serviceRef = sessionRef;
      }

      return serviceRef;
    }
  }
  */
  
  protected JampMethodRest getMethod(JampRestServerSkeleton skel,
                                     String methodName)
  {
    return skel.getMethod(methodName);
  }

  private void allowAll(HttpServletRequest request,
                        HttpServletResponse response,
                        String origin,
                        boolean isOptions)
  {
    String headers = request.getHeader("Access-Control-Request-Headers");

    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Access-Control-Allow-Credentials", "true");

    if (isOptions) {
      response.setHeader("Access-Control-Allow-Methods",
                         "POST, GET, OPTIONS, PUT");
      response.setHeader("Access-Control-Allow-Headers", headers);
      response.setIntHeader("Access-Control-Max-Age", 30 * 60 * 1000);
    }
  }
  
  private class ChannelTimeout implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        ArrayList<String> brokerList = null;
        
        long now = CurrentTime.getCurrentTime();
        
        Iterator<Entry<String, ChannelServer>> iter = _sessionMap.iterator();
        
        while (iter.hasNext()) {
          Entry<String,ChannelServer> entry = iter.next();

          ChannelServerJampPull broker = (ChannelServerJampPull) entry.getValue();
          
          broker.timeoutConnection(now);
          
          if (broker.timeoutSession(now)) {
            if (brokerList == null) {
              brokerList = new ArrayList<>();
            }
            
            brokerList.add(entry.getKey());
          }
        }
        
        if (brokerList != null) {
          for (String key : brokerList) {
            ChannelServer channel = _sessionMap.remove(key);
            
            try {
              channel.shutdown(ShutdownModeAmp.GRACEFUL);
            } catch (Exception e) {
              log.log(Level.FINER, e.toString(), e);
            }
          }
        }
      } finally {
        if (_lifecycle.isActive()) {
          alarm.queue(_alarmTimeout);
        }
      }
    }
  }
  
  static class WebSocketContextPod {
    private WebSocketContextDispatch _wsContext;
    private ServiceManagerAmp _ampManager;
    
    WebSocketContextPod(WebSocketContextDispatch wsContext, 
                        ServiceManagerAmp ampManager)
    {
      _wsContext = wsContext;
      _ampManager = ampManager;
    }
    
    public WebSocketContextDispatch getContext()
    {
      return _wsContext;
    }
    
    public boolean isClosed()
    {
      return _ampManager.isClosed();
    }
  }
}
