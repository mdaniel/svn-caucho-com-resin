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

package com.caucho.v5.websocket.server;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.util.Base64Util;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.websocket.common.ConnectionWebSocketBase;
import com.caucho.v5.websocket.io.FrameInputStream;
import com.caucho.v5.websocket.io.WebSocketConstants;
import com.caucho.v5.websocket.plain.ConnectionPlain;

/**
 * websocket server container
 */
@ModulePrivate
public class WebSocketServletDispatch
{
  private static final L10N L = new L10N(WebSocketServletDispatch.class);
  private static final Logger log
    = Logger.getLogger(WebSocketServletDispatch.class.getName());

  private final ServerContainerDelegate _container;
  //private WebSocketEndpointFactory _factory;
  private PathTemplateWebSocket _pathTemplate;
  
  private ServerEndpointConfig _configDefault;

  private final AtomicLong _connId = new AtomicLong();
  
  private Function<String,WebSocketContextDispatch> _contextFactory
    = x->getContext(x);

  public WebSocketServletDispatch(ServerEndpointConfig config)
  {
    this(new ServerContainerDelegate(), config);
  }

  public WebSocketServletDispatch(ServerContainerDelegate container,
                              ServerEndpointConfig config)
  {
    _container = container;

    //initWebSocket();

    _configDefault = config;

    //_factory = createFactory();

    //_pathTemplate = new PathTemplateWebSocket(_config.getPath());
    _pathTemplate = new PathTemplateWebSocket(config.getPath());
  }

  public WebSocketServletDispatch(ServerContainerDelegate container,
                              WebSocketEndpointFactory factory)
  {
    _container = container;

    _configDefault = factory.getConfig();

    //_factory = factory;

    _pathTemplate = new PathTemplateWebSocket("/");
  }

  public WebSocketServletDispatch()
  {
    ServerContainerDelegate container = null;
    
    try {
      container = createContainer();
    } catch (Exception e) {
      log.finer(e.toString());
      log.log(Level.FINEST, e.toString(), e);
    }
    
    _container = container;

    _pathTemplate = new PathTemplateWebSocket("/");
  }

  protected ServerContainerDelegate createContainer()
  {
    return new ServerContainerDelegate();
  }

  private ServerEndpointConfig createConfig()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void init()
  {
    if (_container == null) {
      return;
    }
    
    initWebSocket();
  }

  protected void initWebSocket()
  {
  }
  
  public void setContextFactory(Function<String,WebSocketContextDispatch> factory)
  {
    Objects.requireNonNull(factory);
    
    _contextFactory = factory;
  }
  
  private WebSocketContextDispatch getContext(String pathInfo)
  {
    return _contextFactory.apply(pathInfo);
  }
  
  protected WebSocketContextDispatch getContextImpl(String pathInfo)
  {
    return new WebSocketContextDispatch(_configDefault);
  }

  public void service(RequestServlet req, ResponseServlet res)
    throws IOException, ServletException
  {
    String origin = req.getHeader("Origin");
    
    WebSocketContextDispatch wsCxt = getContext(req.getPathInfo());
    
    ServerEndpointConfig config = wsCxt.getConfig();
    
    if (! config.getConfigurator().checkOrigin(origin)) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    String subprotocol = negotiateSubprotocol(req, config);

    if (subprotocol != null) {
      res.setHeader("Sec-WebSocket-Protocol", subprotocol);
    }

    Enumeration<String> e = req.getHeaders("Sec-WebSocket-Extensions");
    ArrayList<String> extensionList = new ArrayList<String>();

    if (e != null) {
      while (e.hasMoreElements()) {
        String value = e.nextElement();

        for (String extension: value.split("[\\s,]+")) {
          if (extension.length() > 0) {
            extensionList.add(extension);
          }
        }
      }
    }

    startWebSocket(req, res, wsCxt, subprotocol, extensionList);
  }
  
  private String negotiateSubprotocol(HttpServletRequest req,
                                      ServerEndpointConfig config)
  {

    Enumeration<String> e = req.getHeaders("Sec-WebSocket-Protocol");
    ArrayList<String> protocolList = new ArrayList<String>();

    if (e != null) {
      while (e.hasMoreElements()) {
        String value = e.nextElement();

        for (String protocol : value.split("[\\s,]+")) {
          if (protocol.length() > 0) {
            protocolList.add(protocol);
          }
        }
      }
    }

    Configurator cfg = config.getConfigurator();

    if (cfg != null) {
      return cfg.getNegotiatedSubprotocol(config.getSubprotocols(),
                                          protocolList);
    }
    else {
      return null;
    }
  }

  private void startWebSocket(RequestServlet req,
                              ResponseServlet res,
                              WebSocketContextDispatch wsCxt,
                              String subprotocol,
                              ArrayList<String> extensionList)
    throws IOException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " upgrade HTTP to WebSocket");

    String method = req.getMethod();

    if (! "GET".equals(method)) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

      throw new IllegalStateException(L.l("HTTP Method must be 'GET', because the WebSocket protocol requires 'GET'.\n  remote-IP: {0}",
                                          req.getRemoteAddr()));
    }

    String connection = req.getHeader("Connection");
    String upgrade = req.getHeader("Upgrade");

    if (! "websocket".equalsIgnoreCase(upgrade)) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);

      throw new IllegalStateException(L.l("HTTP Upgrade header '{0}' must be 'WebSocket', because the WebSocket protocol requires an Upgrade: WebSocket header.\n  remote-IP: {1}",
                                          upgrade,
                                          req.getRemoteAddr()));
    }

    if (connection == null
        || connection.toLowerCase().indexOf("upgrade") < 0) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);

      throw new IllegalStateException(L.l("HTTP Connection header '{0}' must be 'Upgrade', because the WebSocket protocol requires a Connection: Upgrade header.\n  remote-IP: {1}",
                                          connection,
                                          req.getRemoteAddr()));
    }

    String key = req.getHeader("Sec-WebSocket-Key");

    if (key == null) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);

      throw new IllegalStateException(L.l("HTTP Sec-WebSocket-Key header is required, because the WebSocket protocol requires an Origin header.\n  remote-IP: {0}",
                                          req.getRemoteAddr()));
    }
    else if (key.length() != 24) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);

      throw new IllegalStateException(L.l("HTTP Sec-WebSocket-Key header is invalid '{0}' because it's not a 16-byte value.\n  remote-IP: {1}",
                                          key,
                                          req.getRemoteAddr()));
    }

    String version = req.getHeader("Sec-WebSocket-Version");

    String requiredVersion = WebSocketConstants.VERSION;
    if (! requiredVersion.equals(version)) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);

      throw new IllegalStateException(L.l("HTTP Sec-WebSocket-Version header with value '{0}' is required, because the WebSocket protocol requires an Sec-WebSocket-Version header.\n  remote-IP: {1}",
                                          requiredVersion,
                                          req.getRemoteAddr()));
    }

    boolean isMasked = true;
    boolean isMux = false;

    if (extensionList.indexOf("mux") >= 0) {
      isMux = true;
    }

    if (extensionList.indexOf("x-unmasked") >= 0) {
      isMasked = false;
    }

    ArrayList<String> serverExtensionList = new ArrayList<String>();

    if (! isMasked) {
      serverExtensionList.add("x-unmasked");
    }

    if (isMux) {
      serverExtensionList.add("mux");
    }

    res.setStatus(101);//, "Switching Protocols");
    res.setHeader("Upgrade", "websocket");
    res.setHeader("Connection", "Upgrade");

    String accept = calculateWebSocketAccept(key);

    res.setHeader("Sec-WebSocket-Accept", accept);

    if (serverExtensionList.size() > 0) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < serverExtensionList.size(); i++) {
        if (i > 0)
          sb.append(", ");

        sb.append(serverExtensionList.get(i));
      }

      res.setHeader("Sec-WebSocket-Extensions", sb.toString());
    }

    res.setContentLength(0);

    FrameInputStream fIs;

    fIs = new FrameInputStream();

    /*
    String uri = "ws://" + req.getServerName();

    if (req.getServerPort() != 80) {
      uri += ":" + req.getLocalPort();
    }
    */
    
    String uri = "";

    uri += req.getContextPath();

    String path = req.getServletPath();

    if (path == null) {
      path = "";
    }

    if (req.getPathInfo() != null) {
      path = path + req.getPathInfo();
    }

    if (req.getQueryString() != null) {
      path = path + "?" + req.getQueryString();
    }

    uri = uri + path;

    String []paths = _pathTemplate.match(path);
    
    ServerEndpointConfig config = wsCxt.getConfig();

    config.getConfigurator().modifyHandshake(config,
                                             new HandshakeRequestImpl(req),
                                             new HandshakeResponseImpl(res));

    // Endpoint endpoint = _endpointSkeleton.newEndpoint(_factory, paths);
    Endpoint endpoint = wsCxt.getFactory().get();

    ConnectionWebSocketBase wsConn;

    long connId = _connId.incrementAndGet();

    wsConn = new ConnectionPlain(connId, _container, uri, endpoint, config, fIs);

    wsConn.setPathParameters(_pathTemplate.getNames(), paths);

    wsConn.setSubprotocol(subprotocol);

    RequestWebSocketServer webSocket = null;//new RequestWebSocketServer(wsConn, req.getConnection());

    // order for duplex
    req.startDuplex(webSocket);
    
    webSocket.onStart();
    /*
    try {
      res.getOutputStream().flush();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    */
    
    _container.addSession(wsConn.getSession());

    wsConn.open();
  }

  private String calculateWebSocketAccept(String key)
  {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");

      int length = key.length();
      for (int i = 0; i < length; i++) {
        md.update((byte) key.charAt(i));
      }

      String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
      length = guid.length();
      for (int i = 0; i < length; i++) {
        md.update((byte) guid.charAt(i));
      }

      byte []digest = md.digest();

      return Base64Util.encode(digest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class HandshakeRequestImpl implements HandshakeRequest {
    private HttpServletRequest _req;

    HandshakeRequestImpl(HttpServletRequest req)
    {
      _req = req;
    }

    @Override
    public Map<String, List<String>> getHeaders()
    {
      HashMap<String,List<String>> headerMap = new HashMap<>();

      Enumeration<String> e = _req.getHeaderNames();
      while (e.hasMoreElements()) {
        String key = e.nextElement();

        ArrayList<String> valueList = new ArrayList<>();

        Enumeration<String> valueEnum = _req.getHeaders(key);

        while (valueEnum.hasMoreElements()) {
          valueList.add(valueEnum.nextElement());
        }

        headerMap.put(key, valueList);
      }

      return headerMap;
    }

    @Override
    public Object getHttpSession()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Map<String, List<String>> getParameterMap()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getQueryString()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public URI getRequestURI()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Principal getUserPrincipal()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isUserInRole(String role)
    {
      // TODO Auto-generated method stub
      return false;
    }
  }

  class HandshakeResponseImpl implements HandshakeResponse {
    private HttpServletResponse _res;

    HandshakeResponseImpl(HttpServletResponse res)
    {
      _res = res;
    }

    @Override
    public Map<String, List<String>> getHeaders()
    {
      return null;
    }
  }
  
  public static class WebSocketContextDispatch
  {
    private ServerEndpointConfig _config;
    private WebSocketEndpointFactory _factory;
    
    public WebSocketContextDispatch(ServerEndpointConfig config)
    {
      _config = config;
      _factory = WebSocketEndpointFactory.create(config);
    }
    
    public ServerEndpointConfig getConfig()
    {
      return _config;
    }
    
    public WebSocketEndpointFactory getFactory()
    {
      return _factory;
    }
  }
}
