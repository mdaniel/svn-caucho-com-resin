/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.servlets;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.log.Log;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.webapp.Application;

/**
 * Load balancing.
 *
 * <pre>
 * &lt;servlet-mapping url-pattern='/remote/*'>
 *   &lt;servlet-name>com.caucho.server.http.FastCGIServlet&lt;/servlet-name>
 *   &lt;init-param server-address='localhost:8086'/>
 * &lt;/servlet-mapping>
 * </pre>
 */
public class FastCGIServlet extends GenericServlet {
  static final protected Logger log = Log.open(FastCGIServlet.class);
  static final L10N L = new L10N(FastCGIServlet.class);
  
  private static final int FCGI_BEGIN_REQUEST = 1;
  private static final int FCGI_ABORT_REQUEST = 2;
  private static final int FCGI_END_REQUEST = 3;
  private static final int FCGI_PARAMS = 4;
  private static final int FCGI_STDIN = 5;
  private static final int FCGI_STDOUT = 6;
  private static final int FCGI_STDERR = 7;
  private static final int FCGI_DATA = 8;
  private static final int FCGI_GET_VALUES = 9;
  private static final int FCGI_GET_VALUES_RESULT = 10;
  private static final int FCGI_UNKNOWNE_TYPE = 11;

  private static final int FCGI_RESPONDER = 1;
  private static final int FCGI_AUTHORIZER = 2;
  private static final int FCGI_FILTER = 3;

  private static final int FCGI_VERSION = 1;
  
  private static final int FCGI_KEEP_CONN = 1;

  private FreeList<FastCGISocket> _freeSockets =
    new FreeList<FastCGISocket>(8);

  private String _hostAddress;
  private InetAddress _hostAddr;
  private int _hostPort;
  protected QDate _calendar = new QDate();
  private Application _app;

  private int _maxKeepaliveCount = 250;

  /**
   * Sets the host address.
   */
  public void setServerAddress(String hostAddress)
    throws ServletException
  {
    _hostAddress = hostAddress;

    try {
      int p = hostAddress.indexOf(':');
      if (p > 0) {
        _hostPort = new Integer(_hostAddress.substring(p + 1)).intValue();
        _hostAddr = InetAddress.getByName(_hostAddress.substring(0, p));
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  public void init()
    throws ServletException
  {
    _app = (Application) getServletContext();

    String serverAddress = getInitParameter("server-address");
    if (serverAddress != null)
      setServerAddress(serverAddress);
    
    if (_hostAddress == null)
      throw new ServletException("FastCGIServlet needs valid server-address");
  }

  /**
   * Handle the request.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    OutputStream out = res.getOutputStream();

    FastCGISocket fcgiSocket = null;

    do {
      fcgiSocket = _freeSockets.allocate();
    } while (fcgiSocket != null && ! fcgiSocket.isValid());

    if (fcgiSocket == null) {
      Socket socket = new Socket(_hostAddr, _hostPort);

      fcgiSocket = new FastCGISocket(socket, _maxKeepaliveCount);
    }

    WriteStream ws = new WriteStream(fcgiSocket.getSocketStream());
    ws.setDisableClose(true);
    ReadStream rs = new ReadStream(fcgiSocket.getSocketStream(), ws);
    rs.setDisableClose(true);
    boolean isOkay = false;

    try {
      if (handleRequest(req, res, rs, ws, out,
			fcgiSocket.allocateKeepalive()) &&
	  _freeSockets.free(fcgiSocket)) {
	fcgiSocket = null;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      if (fcgiSocket != null)
        fcgiSocket.close();
    } finally {
      ws.close();
      rs.close();
    }
  }

  private boolean handleRequest(HttpServletRequest req,
				HttpServletResponse res,
				ReadStream rs, WriteStream ws,
				OutputStream out,
				boolean keepalive)
    throws ServletException, IOException
  {
    writeHeader(ws, FCGI_BEGIN_REQUEST, 8);

    int role = FCGI_RESPONDER;
    
    ws.write(role >> 8);
    ws.write(role);
    ws.write(keepalive ? FCGI_KEEP_CONN : 0); // flags
    for (int i = 0; i < 5; i++)
      ws.write(0);

    setEnvironment(ws, req);

    InputStream in = req.getInputStream();
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buf = tempBuf.getBuffer();
    int len = buf.length;
    int sublen;

    writeHeader(ws, FCGI_PARAMS, 0);
    
    boolean hasStdin = false;
    while ((sublen = in.read(buf, 0, len)) > 0) {
      hasStdin = true;
      writeHeader(ws, FCGI_STDIN, sublen);
      ws.write(buf, 0, sublen);
    }

    TempBuffer.free(tempBuf);

    if (hasStdin)
      writeHeader(ws, FCGI_STDIN, 0);

    FastCGIInputStream is = new FastCGIInputStream(rs);

    int ch = parseHeaders(res, is);

    if (ch >= 0)
      out.write(ch);

    while ((ch = is.read()) >= 0)
      out.write(ch);

    return ! is.isDead() && keepalive;
  }

  private void setEnvironment(WriteStream ws, HttpServletRequest req)
    throws IOException
  {
    addHeader(ws, "REQUEST_URI", req.getRequestURI());
    addHeader(ws, "REQUEST_METHOD", req.getMethod());
    
    addHeader(ws, "SERVER_SOFTWARE", "Resin/" + com.caucho.Version.VERSION);
    
    addHeader(ws, "SERVER_NAME", req.getServerName());
    //addHeader(ws, "SERVER_ADDR=" + req.getServerAddr());
    addHeader(ws, "SERVER_PORT", String.valueOf(req.getServerPort()));
    
    addHeader(ws, "REMOTE_ADDR", req.getRemoteAddr());
    addHeader(ws, "REMOTE_HOST", req.getRemoteAddr());
    // addHeader(ws, "REMOTE_PORT=" + req.getRemotePort());

    if (req.getRemoteUser() != null)
      addHeader(ws, "REMOTE_USER", req.getRemoteUser());
    else
      addHeader(ws, "REMOTE_USER", "");
    if (req.getAuthType() != null)
      addHeader(ws, "AUTH_TYPE", req.getAuthType());
    
    addHeader(ws, "GATEWAY_INTERFACE", "CGI/1.1");
    addHeader(ws, "SERVER_PROTOCOL", req.getProtocol());
    if (req.getQueryString() != null)
      addHeader(ws, "QUERY_STRING", req.getQueryString());
    else
      addHeader(ws, "QUERY_STRING", "");

    String scriptPath = req.getServletPath();
    String pathInfo = req.getPathInfo();
    
    Path appDir = _app.getAppDir();
    String realPath = _app.getRealPath(scriptPath);
    
    if (! appDir.lookup(realPath).isFile() && pathInfo != null)
      scriptPath = scriptPath + pathInfo;

    /*
     * FastCGI (specifically php) uses the PATH_INFO and PATH_TRANSLATED
     * for the script path.
     */
    log.finer("FCGI file: " + _app.getRealPath(scriptPath));

    addHeader(ws, "PATH_INFO", req.getContextPath() + scriptPath);
    addHeader(ws, "PATH_TRANSLATED", _app.getRealPath(scriptPath));
    
    /* These are the values which would be sent to CGI.
    addHeader(ws, "SCRIPT_NAME", req.getContextPath() + scriptPath);
    addHeader(ws, "SCRIPT_FILENAME", app.getRealPath(scriptPath));
    
    if (pathInfo != null) {
      addHeader(ws, "PATH_INFO", pathInfo);
      addHeader(ws, "PATH_TRANSLATED", req.getRealPath(pathInfo));
    }
    else {
      addHeader(ws, "PATH_INFO", "");
      addHeader(ws, "PATH_TRANSLATED", "");
    }
    */

    int contentLength = req.getContentLength();
    if (contentLength < 0)
      addHeader(ws, "CONTENT_LENGTH", "0");
    else
      addHeader(ws, "CONTENT_LENGTH", String.valueOf(contentLength));

    addHeader(ws, "DOCUMENT_ROOT", _app.getContext("/").getRealPath("/"));

    CharBuffer cb = new CharBuffer();
    
    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String value = req.getHeader(key);

      if (key.equalsIgnoreCase("content-length"))
        addHeader(ws, "CONTENT_LENGTH", value);
      else if (key.equalsIgnoreCase("content-type"))
        addHeader(ws, "CONTENT_TYPE", value);
      else if (key.equalsIgnoreCase("if-modified-since")) {
      }
      else if (key.equalsIgnoreCase("if-none-match")) {
      }
      else if (key.equalsIgnoreCase("authorization")) {
      }
      else if (key.equalsIgnoreCase("proxy-authorization")) {
      }
      else
        addHeader(ws, convertHeader(cb, key), value);
    }
  }

  private CharBuffer convertHeader(CharBuffer cb, String key)
  {
    cb.clear();

    cb.append("HTTP_");
    
    for (int i = 0; i < key.length(); i++) {
      char ch = key.charAt(i);
      if (ch == '-')
        cb.append('_');
      else if (ch >= 'a' && ch <= 'z')
        cb.append((char) (ch + 'A' - 'a'));
      else
        cb.append(ch);
    }

    return cb;
  }
  
  private int parseHeaders(HttpServletResponse res, InputStream is)
    throws IOException
  {
    CharBuffer key = CharBuffer.allocate();
    CharBuffer value = CharBuffer.allocate();

    int ch = is.read();

    if (ch < 0) {
      log.fine("Can't contact FastCGI");
      res.sendError(404);
      return -1;
    }
    
    while (ch >= 0) {
      key.clear();
      value.clear();

      for (;
           ch >= 0 && ch != ' ' && ch != '\r' && ch != '\n' && ch != ':';
           ch = is.read()) {
        key.append((char) ch);
      }
      
      for (;
           ch >= 0 && ch == ' ' || ch == ':';
           ch = is.read()) {
      }

      for (;
           ch >= 0 && ch != '\r' && ch != '\n';
           ch = is.read()) {
        value.append((char) ch);
      }

      if (ch == '\r') {
        ch = is.read();
        if (ch == '\n')
          ch = is.read();
      }

      if (key.length() == 0)
        return ch;

	if (log.isLoggable(Level.FINE))
	  log.fine("fastcgi:" + key + ": " + value);
	
      if (key.equalsIgnoreCase("status")) {
	res.setStatus(Integer.parseInt(value.toString()));
      }
      else if (key.startsWith("http") || key.startsWith("HTTP")) {
      }
      else if (key.equalsIgnoreCase("location")) {
	res.sendRedirect(value.toString());
      }
      else
	res.addHeader(key.toString(), value.toString());
    }

    return ch;
  }

  private void addHeader(WriteStream ws, String key, String value)
    throws IOException
  {
    int keyLen = key.length();
    int valLen = value.length();

    int len = keyLen + valLen;

    if (keyLen < 0x80)
      len += 1;
    else
      len += 4;

    if (valLen < 0x80)
      len += 1;
    else
      len += 4;

    writeHeader(ws, FCGI_PARAMS, len);
    
    if (keyLen < 0x80)
      ws.write(keyLen);
    else {
      ws.write(0x80 | (keyLen >> 24));
      ws.write(keyLen >> 16);
      ws.write(keyLen >> 8);
      ws.write(keyLen);
    }
    
    if (valLen < 0x80)
      ws.write(valLen);
    else {
      ws.write(0x80 | (valLen >> 24));
      ws.write(valLen >> 16);
      ws.write(valLen >> 8);
      ws.write(valLen);
    }

    ws.print(key);
    ws.print(value);
  }

  private void addHeader(WriteStream ws, CharBuffer key, String value)
    throws IOException
  {
    int keyLen = key.getLength();
    int valLen = value.length();

    int len = keyLen + valLen;

    if (keyLen < 0x80)
      len += 1;
    else
      len += 4;

    if (valLen < 0x80)
      len += 1;
    else
      len += 4;

    writeHeader(ws, FCGI_PARAMS, len);
    
    if (keyLen < 0x80)
      ws.write(keyLen);
    else {
      ws.write(0x80 | (keyLen >> 24));
      ws.write(keyLen >> 16);
      ws.write(keyLen >> 8);
      ws.write(keyLen);
    }
    
    if (valLen < 0x80)
      ws.write(valLen);
    else {
      ws.write(0x80 | (valLen >> 24));
      ws.write(valLen >> 16);
      ws.write(valLen >> 8);
      ws.write(valLen);
    }

    ws.print(key.getBuffer(), 0, keyLen);
    ws.print(value);
  }

  private void writeHeader(WriteStream ws, int type, int length)
    throws IOException
  {
    int id = 1;
    int pad = 0;

    ws.write(FCGI_VERSION);
    ws.write(type);
    ws.write(id >> 8);
    ws.write(id);
    ws.write(length >> 8);
    ws.write(length);
    ws.write(pad);
    ws.write(0);
  }

  static class FastCGIInputStream extends InputStream {
    private InputStream _is;
    private int _chunkLength;
    private int _padLength;
    private boolean _isDead;

    public FastCGIInputStream()
    {
    }

    public FastCGIInputStream(InputStream is)
    {
      init(is);
    }

    public void init(InputStream is)
    {
      _is = is;
      _chunkLength = 0;
      _isDead = false;
    }

    public boolean isDead()
    {
      return _isDead;
    }

    public int read()
      throws IOException
    {
      if (_chunkLength > 0) {
        _chunkLength--;
        return _is.read();
      }

      if (! readNext())
        return -1;

      _chunkLength--;
      return _is.read();
    }

    private boolean readNext()
      throws IOException
    {
      if (_is == null)
        return false;

      if (_padLength > 0) {
        _is.skip(_padLength);
        _padLength = 0;
      }
      
      int version;

      while ((version = _is.read()) >= 0) {
        int type = _is.read();
        int id = (_is.read() << 8) + _is.read();
        int length = (_is.read() << 8) + _is.read();
        int padding = _is.read();
        _is.skip(1);

        switch (type) {
        case FCGI_END_REQUEST:
        {
          int appStatus = ((_is.read() << 24) +
                           (_is.read() << 16) +
                           (_is.read() << 8) +
                           (_is.read()));
          int pStatus = _is.read();

          if (appStatus != 0)
            _isDead = true;
          
          _is.skip(3);
          _is = null;
          return false;
        }

        case FCGI_STDOUT:
          if (length == 0)
            break;
          else {
            _chunkLength = length;
            _padLength = padding;
            return true;
          }

        case FCGI_STDERR:
          byte []buf = new byte[length];
          _is.read(buf, 0, length);
          log.warning(new String(buf, 0, length));
          break;

        default:
          _is.skip(length + padding);
          break;
        }
      }

      _isDead = true;
        
      return false;
    }
  }

  static class FastCGISocket {
    private int _keepaliveCount;
    private Socket _socket;
    private SocketStream _socketStream;

    FastCGISocket(Socket socket, int maxKeepaliveCount)
    {
      _socket = socket;
      _keepaliveCount = maxKeepaliveCount;

      _socketStream = new SocketStream(_socket);
    }

    SocketStream getSocketStream()
    {
      return _socketStream;
    }

    boolean isValid()
    {
      return _socket != null; // XXX: check for timeout
    }

    boolean allocateKeepalive()
    {
      return --_keepaliveCount > 0;
    }

    boolean isKeepalive()
    {
      return _keepaliveCount > 0;
    }

    void close()
    {
      try {
	Socket socket = _socket;
	_socket = null;

	_socketStream = null;

	if (socket != null)
	  socket.close();
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }
  }
}
