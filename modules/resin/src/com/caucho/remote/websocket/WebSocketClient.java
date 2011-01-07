/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketListener;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.*;

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
  
  private String _virtualHost;
  private boolean _isMasked;

  private WebSocketListener _listener;

  private Socket _s;
  private ReadStream _is;
  private WriteStream _os;
  private boolean _isClosed;

  private ClientContext _context;
  
  private WebSocketInputStream _wsIs;
  private WebSocketOutputStream _wsOs;
  
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
    
    _s = new Socket(_host, _port);

    _is = Vfs.openRead(_s.getInputStream());
    _os = Vfs.openWrite(_s.getOutputStream());

    _os.print("WEBSOCKET " + _path + " HTTP/1.1\r\n");
    
    if (_virtualHost != null)
      _os.print("Host: " + _virtualHost + "\r\n");
    else if (_host != null)
      _os.print("Host: " + _host + "\r\n");
    else
      _os.print("Host: localhost\r\n");
    
    byte []clientNonce = new byte[8];
    
    if (_isMasked)
      _os.print("Sec-WebSocket-Nonce: 0000000000000000\r\n");
    else
      _os.print("Sec-WebSocket-Protocol: unmasked\r\n");
    
    _os.print("Upgrade: WebSocket\r\n");
    _os.print("Connection: Upgrade\r\n");
    _os.print("Origin: Foo\r\n");
    
    if (userName != null) {
      _os.print("Sec-WebSocket-Login: true\r\n");
    }
    
    _os.print("\r\n");
    _os.flush();

    parseHeaders(_is);

    // _wsOut = new WebSocketOutputStream(_out);
    // _wsIn = new WebSocketInputStream(_is);

    if (_isMasked) {
      byte []serverNonce = null;
    
      serverNonce = readHello(_is);
    
      writeHello(_os, clientNonce, serverNonce);
    }
    
    if (userName != null) {
      byte []authNonce = readAuthNonce(_is);
      
      try {
        writeAuth(_os, authNonce, userName, password);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
    
    _os.flush();
    
    _context = new ClientContext();

    _listener.onStart(this);
    _listener.onHandshakeComplete(this, true);
    
    // XXX: ThreadPool?
    Thread thread = new Thread(_context);
    thread.setDaemon(true);
    thread.start();
  }

  protected void parseHeaders(ReadStream in)
    throws IOException
  {
    String line = in.readln();

    if (! line.startsWith("HTTP/1.1 101"))
      throw new IOException(L.l("Unexpected response {0}", line));

    while ((line = in.readln()) != null && line.length() != 0) {
      int p = line.indexOf(':');

      if (p > 0) {
        String header = line.substring(0, p).trim();
        String value = line.substring(p + 1).trim();
      }
    }
  }
  
  private byte []readHello(ReadStream is)
    throws IOException
  {
    int frame1 = is.read();
    int frame2 = is.read();
    
    if (frame2 < 0)
      throw new EOFException(L.l("unexpected EOF waiting for HELLO"));
    
    int op = frame1 & 0xf;
    int len = frame2 & 0x7f;
    
    if (op != OP_HELLO) {
      throw new EOFException(L.l("expected WebSocket HELLO at {0}", op));
    }
    
    if (len != 0x18) {
      throw new EOFException(L.l("unexpected HELLO length {0}", len));
    }
    
    byte []serverNonce = new byte[8];
    
    is.readAll(serverNonce, 0, serverNonce.length);
    
    byte []serverHash = new byte[16];
    is.readAll(serverHash, 0, serverHash.length);
    
    return serverNonce;
  }
  
  private byte []readAuthNonce(ReadStream is)
    throws IOException
  {
    int frame1 = is.read();
    int frame2 = is.read();
    
    if (frame2 < 0)
      throw new EOFException(L.l("unexpected EOF waiting for auth"));
    
    int op = frame1 & 0xf;
    int len = frame2 & 0x7f;
    
    if (op != OP_EXT) {
      throw new EOFException(L.l("expected WebSocket auth at {0}", op));
    }
    
    byte []data = new byte[len];
    
    is.readAll(data, 0, data.length);
    
    int p = scanToLf(data);
    
    if (p < 0)
      throw new IllegalStateException("Cannot find authentication in '"
                                      + new String(data));
    
    String key = new String(data, 0, p);
    
    if (! "x-authentication/challenge".equals(key))
      throw new IllegalStateException(key);
    
    byte []nonce = new byte[data.length - p - 1];
    System.arraycopy(data, p + 1, nonce, 0, nonce.length);
    
    return nonce;
  }
  
  private int scanToLf(byte []data)
  {
    for (int i = 0; i < data.length; i++) {
      if (data[i] == '\n')
        return i;
    }
    
    return -1;
  }
  
  private void writeHello(WriteStream os, byte []clientNonce, byte []serverNonce)
    throws IOException
  {
    byte []hash = calculateHash(serverNonce, clientNonce);
   
    os.write(FLAG_FIN|OP_HELLO);
    os.write(hash.length);
    
    os.write(hash);
  }
  
  private void writeAuth(WriteStream os, 
                         byte []authNonce, 
                         String userName,
                         String password)
    throws IOException, NoSuchAlgorithmException
  {
    MessageDigest md = MessageDigest.getInstance("MD5");
    
    md.update(userName.getBytes());
    md.update((byte) ':');
    md.update(password.getBytes());
    
    byte []passwordDigest = md.digest();
    
    md.reset();
    
    md.update(passwordDigest);
    md.update(authNonce);
    
    byte []digest = md.digest();
    
    String code = "x-authentication/response";
   
    int length = code.length() + 1 + userName.length() + 1 + digest.length;
   
    os.write(FLAG_FIN|OP_EXT);
    os.write(length);
    
    os.print(code);
    os.print("\n");
    os.print(userName);
    os.print("\n");
    os.write(digest);
  }

  private byte []calculateHash(byte []nonce1, byte []nonce2)
  {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      
      md.update(nonce1);
      
      String webSocket = "WebSocket";
      for (int i = 0; i < webSocket.length(); i++) {
        md.update((byte) webSocket.charAt(i));
      }
      md.update(nonce2);
      
      byte []digest = md.digest();
      
      return digest;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  
  @Override
  public void complete()
  {
    OutputStream os = _os;

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }

  public void close()
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

  public PrintWriter startTextMessage()
  {
    throw new UnsupportedOperationException();
  }

  public long getTimeout()
  {
    return 0;
  }

  public void setTimeout(long timeout)
  {
  }


  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "]";
  }

  class ClientContext implements Runnable
  {
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
          _listener.onComplete(WebSocketClient.this);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
        
        close();
        
        thread.setName(name);
      }
    }

    public void handleRequests()
      throws IOException
    {
      // server/2h20
      // _listener.onStart(this);
      
      InputStream is = _is;

      while (! isClosed()) {
        int frame1 = is.read();
        int frame2 = is.read();
        
        if (frame2 < 0) {
          return;
        }
        
        int op = frame1 & 0x0f;
        boolean isFinal = (frame1 & FLAG_FIN) == FLAG_FIN;
        long len = frame2 & 0x7f;
        
        if (len == 0x7e) {
          len = ((is.read() << 8) + is.read());
        }
        else if (len == 0x7f) {
          len = (((long) is.read() << 56)
              + ((long) is.read() << 48)
              + ((long) is.read() << 40)
              + ((long) is.read() << 32)
              + ((long) is.read() << 24)
              + ((long) is.read() << 16)
              + ((long) is.read() << 8)
              + ((long) is.read()));
          
        }
        
        switch (op) {
        case OP_BINARY:
          if (_wsIs == null)
            _wsIs = new WebSocketInputStream(_is);
          
          _wsIs.init(isFinal, len);
          
          _listener.onReadBinary(WebSocketClient.this, _wsIs);
          break;
          
        default:
          throw new IllegalStateException("Unknown WebSocket opcode 0x" +
                                          Integer.toHexString(op));
        }
      }
    }
  }
}
