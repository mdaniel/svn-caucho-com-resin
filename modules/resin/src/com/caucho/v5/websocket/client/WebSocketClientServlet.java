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

package com.caucho.v5.websocket.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.util.Base64Util;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.QSocket;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.WriteStream;
import com.caucho.v5.vfs.net.SocketSystem;
import com.caucho.v5.websocket.common.ConnectionWebSocketBase;
import com.caucho.v5.websocket.common.EndpointConnection;
import com.caucho.v5.websocket.common.EndpointConnectionQSocket;
import com.caucho.v5.websocket.common.EndpointReaderWebSocket;
import com.caucho.v5.websocket.io.FrameInputStream;
import com.caucho.v5.websocket.io.WebSocketConstants;
import com.caucho.v5.websocket.io.WebSocketProtocolException;
import com.caucho.v5.websocket.plain.ConnectionPlain;

/**
 * WebSocketClient
 */
public class WebSocketClientServlet
  implements WebSocketConstants
{
  private static final Logger log
    = Logger.getLogger(WebSocketClientServlet.class.getName());
  private static final L10N L = new L10N(WebSocketClientServlet.class);

  private String _url;
  private URI _uri;

  private String _scheme;
  private String _host;
  private int _port;
  private String _path;

  private long _connectTimeout;

  private String _virtualHost;
  private boolean _isMasked = true;

  private WebSocketContainerClient _container;
  // private Session _session;
  private Object _endpoint;

  private EndpointConnection _socketConn;
  private ConnectionWebSocketBase _conn;
  private boolean _isClosed;

  // private NioClientTask _context;
  private ThreadClientTaskServlet _threadTask;
  private ConnectionWebSocketJni _jniTask;

  private FrameInputStream _frameIs;

  private HashMap<String,String> _headers = new HashMap<String,String>();
  
  private List<String> _preferredSubprotocols = new ArrayList<>();
  
  private String _origin;

  private EndpointReaderWebSocket _wsEndpointReader;
  private ClientEndpointConfig _config;
  private ClientEndpointConfig.Configurator _configurator;

  /*
  public WebSocketClient(String url, WebSocketListener listener)
  {
    this(new ClientContainerImpl(), createAdapter(listener), createConfigAdapter(url));
  }
  */

  public WebSocketClientServlet(String address,
                                Object endpoint)
  {
    this(address,
         endpoint,
         new WebSocketContainerClient(),
         createConfigAdapter(endpoint));
  }

  public WebSocketClientServlet(String address,
                         Object endpoint,
                         WebSocketContainerClient container,
                         ClientEndpointConfig config)
  {
    Objects.requireNonNull(container);
    Objects.requireNonNull(address);
    Objects.requireNonNull(endpoint);
    
    _container = container;
    _endpoint = endpoint;
    _config = config;

    try {
      _uri = new URI(address);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (config != null) {
      if (config.getPreferredSubprotocols() != null) {
        _preferredSubprotocols.addAll(config.getPreferredSubprotocols());
      }
    }
    
    _configurator = config.getConfigurator();
        
    /*
    if (config != null) {
      _origin = config.getOrigin();
    }
    */

    _scheme = _uri.getScheme();
    _host = _uri.getHost();
    _port = _uri.getPort();
    _path = _uri.getPath();

    if (_path == null) {
      _path = "/";
    }
  }

  private static ClientEndpointConfig createConfigAdapter(Object endpoint)
  {
    ClientEndpointConfig.Builder builder = ClientEndpointConfig.Builder.create();
    
    return builder.build();
  }

  public String getHost()
  {
    return _host;
  }

  public int getPort()
  {
    return _port;
  }

  public String getPath()
  {
    return _path;
  }

  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  public void setConnectTimeout(long timeout)
  {
    _connectTimeout = timeout;
  }

  public void setMasked(boolean isMasked)
  {
    _isMasked = isMasked;
  }

  /**
   * @param preferredSubprotocols
   */
  public void setPreferredSubprotocols(List<String> preferredSubprotocols)
  {
    _preferredSubprotocols = preferredSubprotocols;
  }

  public void setOrigin(String origin)
  {
    _origin = origin;
  }


  public Session connect()
    throws IOException
  {
    return connect(null, null);
  }

  public Session connect(String userName, String password)
    throws IOException
  {
    if (_conn != null) {
      return _conn.getSession();
    }

    connectImpl(userName, password);
    
    return _conn.getSession();
  }

  public Session getSession()
  {
    if (_conn != null) {
      return _conn.getSession();
    }
    else {
      return null;
    }
  }

  public String getProtocolVersion()
  {
    return WebSocketConstants.VERSION;
  }

  public String getHeader(String key)
  {
    return _headers.get(key);
  }

  protected void connectImpl(String userName, String password)
    throws IOException
  {
    if (_endpoint == null) {
      throw new IllegalStateException("missing websocket endpoint");
    }

    int connectTimeout = (int) _connectTimeout;

    InetSocketAddress addr = new InetSocketAddress(_host, _port);

    SocketSystem network = SocketSystem.current();

    QSocket s = network.connect(addr.getAddress(), addr.getPort(), connectTimeout);

    /*
    Socket s = new Socket();

    if (connectTimeout > 0) {
      s.connect(new InetSocketAddress(_host, _port), connectTimeout);
    }
    else {
      s.connect(new InetSocketAddress(_host, _port));
    }
    */

    /*
    SocketChannel chan = SocketChannel.open();

    chan.connect(new InetSocketAddress(_host, _port));

    Socket s = chan.socket();
    */

    s.setTcpNoDelay(true);

    /*
    if ("https".equals(_scheme)) {
      s = openSsl(s);
    }
    */

    _socketConn = new EndpointConnectionQSocket(s);
    _socketConn.setIdleReadTimeout(600000);

    ReadStream is = _socketConn.getInputStream();
    WriteStream os = _socketConn.getOutputStream();

    String path = _path;

    if (_uri.getQuery() != null) {
      path = path + "?" + _uri.getQuery();
    }

    os.print("GET " + path + " HTTP/1.1\r\n");

    if (_virtualHost != null) {
      os.print("Host: " + _virtualHost + "\r\n");
    }
    else if (_host != null) {
      os.print("Host: " + _host + "\r\n");
    }
    else {
      os.print("Host: localhost\r\n");
    }

    byte []clientNonce = new byte[16];

    String key = Base64Util.encode(clientNonce);

    os.print("Sec-WebSocket-Key: " + key + "\r\n");

    String version = WebSocketConstants.VERSION;

    os.print("Sec-WebSocket-Version: " + version + "\r\n");

    if (_origin != null) {
      os.print("Origin: " + _origin + "\r\n");
    }

    StringBuilder ext = new StringBuilder();

    if (! _isMasked) {
      if (ext.length() > 0)
        ext.append(", ");

      ext.append("x-unmasked");
    }
    
    if (_config != null) {
      List<Extension> extList = _config.getExtensions();

      if (extList != null) {
        for (Extension extension : extList) {
          String extName = extension.getName();
          
          if (ext.length() > 0) {
            ext.append(", ");
          }
        
          ext.append(extName);
        }
      }
    }

    if (ext.length() > 0) {
      os.print("Sec-WebSocket-Extensions: " + ext + "\r\n");
    }
    
    if (_preferredSubprotocols != null && _preferredSubprotocols.size() > 0) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < _preferredSubprotocols.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }

        sb.append(_preferredSubprotocols.get(i));
      }

      os.print("Sec-WebSocket-Protocol: " + sb + "\r\n");
    }
    
    HashMap<String,List<String>> headers = new HashMap<>();

    _configurator.beforeRequest(headers);
    
    for (Map.Entry<String,List<String>> entry : headers.entrySet()) {
      List<String> values = entry.getValue();

      os.print(entry.getKey());
      os.print(": ");
      
      for (int i = 0; i < values.size(); i++) {
        if (i != 0) {
          os.print(", ");
        }
        
        os.print(values.get(i));
      }
      
      os.print("\r\n");
    }

    if (_origin != null) {
      os.print("Origin: " + _origin + "\r\n");
    }

    os.print("Upgrade: websocket\r\n");
    os.print("Connection: Upgrade\r\n");

    os.print("\r\n");
    os.flush();

    parseHeaders(is);

    _frameIs = new FrameInputStream();
    //_frameIs.init(this, _is);

    // _wsOut = new WebSocketOutputStream(_out);
    // _wsIn = new WebSocketInputStream(_is);

    os.flush();

    // _context = new NioClientTask(this);

    String []names = new String[0];
    //Endpoint endpoint = WebSocketEndpointSkeleton.wrap(_endpoint, _config, names);
    Endpoint endpoint = null; // WebSocketEndpointSkeleton.wrap(_endpoint, _config, names);
    
    long connId = _container.nextSessionId();

    _conn = new ConnectionPlain(connId, 
                                _container,
                                _uri.toString(),
                                endpoint,
                                _config,
                                _frameIs);

    _conn.initConnection(_socketConn);

    String subprotocol = _headers.get("Sec-WebSocket-Protocol");

    _conn.setSubprotocol(subprotocol);

    _wsEndpointReader = _conn.getEndpointReader();

    // session.start(_is, _os);

    // _listener.onStart(this);

    // _context.start();

    /*
    if (s instanceof JniSocketImpl) {
      PollTcpManager selectManager;

      selectManager = SelectManagerFactoryJni.createJni();
      if (selectManager != null) {
        _jniTask = new ConnectionWebSocketJni(this, s, selectManager);
      }
    }
    */

    if (_jniTask == null) {
      _threadTask = new ThreadClientTaskServlet(this);
    }

    if (_jniTask != null) {
      _jniTask.start();
    }
    
    // open must be before thread reader is spawned so any handlers will
    // be ready to process messages
    _conn.open();

    if (_threadTask != null) {
      // _threadTask.start();

      ThreadPool.getCurrent().execute(_threadTask);
    }

  }

  public Session addChannel(Endpoint endpoint, String path)
  {
    return null;
  }

  public EndpointReaderWebSocket getEndpointReader()
  {
    return _wsEndpointReader;
  }

  public EndpointConnection getSocketConnection()
  {
    return _socketConn;
  }

  protected void parseHeaders(ReadStream in)
    throws IOException
  {
    String status = in.readln();

    if (status == null) {
      throw new WebSocketProtocolException(L.l("Unexpected connection close"));
    }
    else if (status == null || ! status.startsWith("HTTP")) {
      throw new WebSocketProtocolException(L.l("Unexpected response {0}", status));
    }

    String line;
    while ((line = in.readln()) != null && line.length() != 0) {
      int p = line.indexOf(':');

      if (p > 0) {
        String header = line.substring(0, p).trim();
        String value = line.substring(p + 1).trim();

        _headers.put(header, value);
      }
    }

    if (! status.startsWith("HTTP/1.1 101")) {
      StringBuilder sb = new StringBuilder();

      int ch;

      while (in.available() > 0 && (ch = in.read()) >= 0) {
        sb.append((char) ch);
      }

      throw new WebSocketProtocolException(L.l("Unexpected response {0}\n\n{1}",
                                               status, sb));

    }
  }

  public void disconnect()
  {
    EndpointConnection socketConn = _socketConn;
    _socketConn = null;

    ConnectionWebSocketBase conn = _conn;
    _conn = null;

    if (conn != null) {
      conn.onDisconnectRead();
    }

    if (socketConn != null) {
      socketConn.disconnect();
    }
  }

  public boolean isClosed()
  {
    return _isClosed;
  }

  public void close()
  {
    ConnectionWebSocketBase conn = _conn;

    if (conn != null) {
      conn.close();
    }
    
    // _endpoint.onClose(_session, new CloseReason(CloseCodes.NORMAL_CLOSURE, "ok"));
    
    disconnect();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _uri + "]";
  }
}
