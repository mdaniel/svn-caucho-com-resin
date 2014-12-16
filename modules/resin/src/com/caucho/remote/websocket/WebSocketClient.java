/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.remote.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.caucho.util.Base64;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketEncoder;
import com.caucho.websocket.WebSocketListener;

/**
 * WebSocketClient
 */
public class WebSocketClient implements WebSocketContext, WebSocketConstants {
  private static final Logger log
    = Logger.getLogger(WebSocketClient.class.getName());
  private static final L10N L = new L10N(WebSocketClient.class);
  
  private String _url;

  private String _scheme;
  private String _host;
  private int _port;
  private String _path;
  
  private long _connectTimeout;
  
  private String _virtualHost;
  private boolean _isMasked;

  private WebSocketListener _listener;

  private Socket _s;
  private ReadStream _is;
  private WriteStream _os;
  private boolean _isClosed;

  private ClientContext _context;
  
  private FrameInputStream _frameIs;
  private WebSocketInputStream _wsIs;
  
  private WebSocketOutputStream _wsOs;
  private WebSocketWriter _wsWriter;
  
  private WebSocketWriter _wsWriter;
  
  public WebSocketClient(String url,
                         WebSocketListener listener)
  {
    setUrl(url);
    
    _listener = listener;
    
    if (url == null)
      throw new IllegalArgumentException();
    
    if (_listener == null)
      throw new IllegalArgumentException();
  }
  
  private void setUrl(String url)
  {
    _url = url;
    parseUrl(url);
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

  public void connect()
    throws IOException
  {
    connect(null, null);
  }

  public void connect(String userName, String password)
    throws IOException
  {
    if (_s != null)
      return;
    
    connectImpl(userName, password);
  }

  private void parseUrl(String url)
  {
    int p = url.indexOf("://");
    if (p < 0)
      throw new IllegalArgumentException(L.l("'{0}' is an illegal URL because it is missing a scheme",
                                             url));

    _scheme = url.substring(0, p);

    int q = url.indexOf('/', p + 3);

    String server;
    if (q < 0) {
      server = url.substring(p + 3);
      _path = "/";
    }
    else {
      server = url.substring(p + 3, q);
      _path = url.substring(q);
    }

    p = server.indexOf(':');

    if (p < 0) {
      _host = server;
      
      if ("https".equals(_scheme))
        _port = 443;
      else
        _port = 80;
    }
    else {
      _host = server.substring(0, p);
      _port = Integer.parseInt(server.substring(p + 1));
    }
  }

  protected void connectImpl(String userName, String password)
    throws IOException
  {
    if (_listener == null)
      throw new IllegalStateException("missing websocket listener");

    int connectTimeout = (int) _connectTimeout;
    _s = new Socket();
    
    if (connectTimeout > 0)
      _s.connect(new InetSocketAddress(_host, _port), connectTimeout);
    else
      _s.connect(new InetSocketAddress(_host, _port));
    
    if ("https".equals(_scheme)) {
      _s = openSsl(_s);
    }

    _is = VfsStream.openRead(_s.getInputStream());
    _os = VfsStream.openWrite(_s.getOutputStream());

    _os.print("GET " + _path + " HTTP/1.1\r\n");
    
    if (_virtualHost != null)
      _os.print("Host: " + _virtualHost + "\r\n");
    else if (_host != null)
      _os.print("Host: " + _host + "\r\n");
    else
      _os.print("Host: localhost\r\n");
    
    byte []clientNonce = new byte[16];
    
    String key = Base64.encode(clientNonce);
    
    _os.print("Sec-WebSocket-Key: " + key + "\r\n");
    
    String version = WebSocketConstants.VERSION;
    
    _os.print("Sec-WebSocket-Version: " + version + "\r\n");
      
    if (! _isMasked)
      _os.print("Sec-WebSocket-Extensions: x-unmasked\r\n");
    
    _os.print("Upgrade: WebSocket\r\n");
    _os.print("Connection: Upgrade\r\n");
    
    _os.print("\r\n");
    _os.flush();

    parseHeaders(_is);

    _frameIs = new UnmaskedFrameInputStream();
    _frameIs.init(this, _is);
    
    // _wsOut = new WebSocketOutputStream(_out);
    // _wsIn = new WebSocketInputStream(_is);
    
    _os.flush();
    
    _context = new ClientContext();

    _listener.onStart(this);
    
    // XXX: ThreadPool?
    Thread thread = new Thread(_context);
    thread.setDaemon(true);
    thread.start();
  }

  private Socket openSsl(Socket s)
      throws ConnectException
  {
    try {
      SSLContext context = SSLContext.getInstance("TLS");

      javax.net.ssl.TrustManager tm =
        new javax.net.ssl.X509TrustManager() {
          public java.security.cert.X509Certificate[]
            getAcceptedIssuers() {
            return null;
          }
          public void checkClientTrusted(
                                         java.security.cert.X509Certificate[] cert, String foo) {
          }
          public void checkServerTrusted(
                                         java.security.cert.X509Certificate[] cert, String foo) {
          }
        };

      context.init(null, new javax.net.ssl.TrustManager[] { tm }, null);
      SSLSocketFactory factory = context.getSocketFactory();

      return factory.createSocket(s, _host, _port, true);
    } catch (ConnectException e) {
      throw new ConnectException("SSL " + _host + ":" + _port + ": " + e.getMessage());
    } catch (Exception e) {
      throw new ConnectException("SSL " + _host + ":" + _port + ": " + e.toString());
    }
  }

  protected void parseHeaders(ReadStream in)
    throws IOException
  {
    String status = in.readln();
    
    if (status == null) {
      throw new WebSocketProtocolException(L.l("Unexpected connection close", status));
    }

    if (! status.startsWith("HTTP")) {
      throw new WebSocketProtocolException(L.l("Unexpected response {0}", status));
    }
    
    String line;
    while ((line = in.readln()) != null && line.length() != 0) {
      int p = line.indexOf(':');

      if (p > 0) {
        String header = line.substring(0, p).trim();
        String value = line.substring(p + 1).trim();
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
  
  @Override
  public void disconnect()
  {
    OutputStream os = _os;

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public <T> BlockingQueue<T> createOutputQueue(WebSocketEncoder<T> encoder)
  {
    return new WebSocketBlockingQueue<T>(this, encoder, 256);
  }

  @Override
  public void setAutoFlush(boolean isAutoFlush)
  {
  }

  @Override
  public boolean isAutoFlush()
  {
    return false;
  }

  @Override
  public void flush() throws IOException
  {
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  @Override
  public void close()
  {
    close(1000, "ok");
  }

  @Override
  public void close(int code, String msg)
  {
    _isClosed = true;
    
    OutputStream os = _os;
    _os = null;

    InputStream is = _is;
    _is = null;

    Socket s = _s;
    _s = null;

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (is != null)
        is.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (s != null)
        s.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  public InputStream getInputStream()
  {
    return _is;
  }

  @Override
  public OutputStream startBinaryMessage()
    throws IOException
  {
    if (_wsOs == null) {
      OutputStream os = _os;
      if (os == null)
        throw new IllegalStateException(L.l("startBinaryMessage cannot be called with a closed context"));
      
      _wsOs = new WebSocketOutputStream(os, new byte[4096]);
    }
    
    _wsOs.init();
    
    return _wsOs;
  }

  @Override
  public PrintWriter startTextMessage()
    throws IOException
  {
    if (_wsWriter == null) {
      OutputStream os = _os;
      if (os == null)
        throw new IllegalStateException(L.l("startTextMessage cannot be called with a closed context"));
      
      _wsWriter = new WebSocketWriter(os, new byte[4096]);
    }
    
    _wsWriter.init();
    
    return new WebSocketPrintWriter(_wsWriter);
  }

  @Override
  public long getTimeout()
  {
    return 0;
  }

  @Override
  public void setTimeout(long timeout)
  {
  }
  
  @Override
  public void pong(byte []message)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "]";
  }

  class ClientContext implements Runnable
  {
    @Override
    public void run()
    {
      Thread thread = Thread.currentThread();
      String name = thread.getName();
      
      try {
        thread.setName("web-socket-client");
        
        handleRequests();
      } catch (Exception e) {
        if (_isClosed)
          log.log(Level.FINEST, e.toString(), e);
        else
          log.log(Level.WARNING, e.toString(), e);
      } finally {
        try {
          _listener.onDisconnect(WebSocketClient.this);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
        
        close(1000, "");
        
        thread.setName(name);
      }
    }

    public void handleRequests()
      throws IOException
    {
      // server/2h20
      // _listener.onStart(this);
      
      FrameInputStream is = _frameIs;

      while (is.readFrameHeader()) {
        
        int op = is.getOpcode();
        
        switch (op) {
        case OP_BINARY:
          if (_wsIs == null)
            _wsIs = new WebSocketInputStream(is);
          
          _wsIs.init();

          try {
            _listener.onReadBinary(WebSocketClient.this, _wsIs);
          } finally {
            _wsIs.close();
          }
          break;
          
        default:
          throw new IllegalStateException("Unknown WebSocket opcode 0x" +
                                          Integer.toHexString(op));
        }
      }
    }
  }

  @Override
  public void onClose(int closeCode, String closeMessage)
  {

  }

}
