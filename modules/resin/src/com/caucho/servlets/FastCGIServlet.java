/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.server.cluster.Server;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SocketStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.Vfs;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  static final protected Logger log
    = Logger.getLogger(FastCGIServlet.class.getName());
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
  
  private static final int FCGI_REQUEST_COMPLETE = 0;
  private static final int FCGI_CANT_MPX_CONN = 1;
  private static final int FCGI_OVERLOADED = 2;
  private static final int FCGI_UNKNOWN_ROLE = 3;

  private static final ArrayList<Integer> _fcgiServlets
    = new ArrayList<Integer>();

  private int _servletId;

  private FreeList<FastCGISocket> _freeSockets
    = new FreeList<FastCGISocket>(8);

  private Path _pwd;
  private String _hostAddress;
  private InetAddress _hostAddr;
  private int _hostPort;
  protected QDate _calendar = new QDate();
  private long _readTimeout = 120000;

  private int _maxKeepaliveCount = 250;
  private long _keepaliveTimeout = 15000;

  private int _idCount = 0;

  /**
   * Sets the host address.
   */
  public void setServerAddress(String hostAddress)
  {
    _hostAddress = hostAddress;

    try {
      int p = hostAddress.indexOf(':');
      if (p > 0) {
        _hostPort = new Integer(_hostAddress.substring(p + 1)).intValue();
        _hostAddr = InetAddress.getByName(_hostAddress.substring(0, p));
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Sets the keepalive max.
   */
  public void setMaxKeepalive(int max)
  {
    _maxKeepaliveCount = max;
  }

  /**
   * Sets the keepalive timeout.
   */
  public void setKeepaliveTimeout(Period period)
  {
    _keepaliveTimeout = period.getPeriod();
  }

  /**
   * Sets the socket timeout.
   */
  public void setReadTimeout(Period timeout)
  {
    _readTimeout = timeout.getPeriod();
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  public void init(WebApp webApp)
    throws ServletException
  {
    init();
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  public void init()
    throws ServletException
  {
    int id = -1;

    _pwd = Vfs.lookup();
    
    for (int i = 0; i < 0x10000; i += 1024) {
      if (! _fcgiServlets.contains(new Integer(i))) {
	id = i;
	break;
      }
    }

    Server server = Server.getCurrent();

    if (server == null)
      throw new ConfigException(L.l("Server context is required for '{0}'",
				    this));

    if (id < 0)
      throw new ServletException("Can't open FastCGI servlet");

    _fcgiServlets.add(new Integer(id));

    _servletId = id;

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
      if (fcgiSocket != null)
	fcgiSocket.close();
      
      fcgiSocket = _freeSockets.allocate();
    } while (fcgiSocket != null && ! fcgiSocket.isValid());

    if (fcgiSocket != null && fcgiSocket.isValid()) {
      log.finer(fcgiSocket + ": reuse()");
    }
    else {
      if (fcgiSocket != null)
	fcgiSocket.close();
      
      try {
	Socket socket = new Socket(_hostAddr, _hostPort);

	if (_readTimeout > 0)
	  socket.setSoTimeout((int) _readTimeout);

	fcgiSocket = new FastCGISocket(nextId(), socket, _maxKeepaliveCount);
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);

	throw new ServletException(L.l("Can't connect to FastCGI {0}:{1}.  Check that the FastCGI service has started.", _hostAddr, _hostPort));
      }
    }

    boolean isOkay = false;

    try {
      fcgiSocket.setExpire(Alarm.getCurrentTime() + _keepaliveTimeout);
	
      if (handleRequest(req, res, fcgiSocket, out,
			fcgiSocket.allocateKeepalive())) {
	if (_freeSockets.free(fcgiSocket)) {
	  log.finer(fcgiSocket + ": keepalive()");
	  
	  fcgiSocket = null;
	}
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      if (fcgiSocket != null)
        fcgiSocket.close();
    }
  }

  private int nextId()
  {
    synchronized (this) {
      int id = _idCount++;

      if (id <= 0 || 1024 < id) {
	_idCount = 2;
	id = 1;
      }

      return id + _servletId;
    }
  }

  private boolean handleRequest(HttpServletRequest req,
				HttpServletResponse res,
				FastCGISocket fcgiSocket,
				OutputStream out,
				boolean keepalive)
    throws ServletException, IOException
  {
    ReadStream rs = fcgiSocket.getReadStream();
    WriteStream ws = fcgiSocket.getWriteStream();
    
    writeHeader(fcgiSocket, ws, FCGI_BEGIN_REQUEST, 8);

    int role = FCGI_RESPONDER;
    
    ws.write(role >> 8);
    ws.write(role);
    ws.write(keepalive ? FCGI_KEEP_CONN : 0); // flags
    for (int i = 0; i < 5; i++)
      ws.write(0);

    setEnvironment(fcgiSocket, ws, req);

    InputStream in = req.getInputStream();
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buf = tempBuf.getBuffer();
    int len = buf.length;
    int sublen;

    writeHeader(fcgiSocket, ws, FCGI_PARAMS, 0);
    
    boolean hasStdin = false;
    while ((sublen = in.read(buf, 0, len)) > 0) {
      hasStdin = true;
      writeHeader(fcgiSocket, ws, FCGI_STDIN, sublen);
      ws.write(buf, 0, sublen);
    }

    TempBuffer.free(tempBuf);
    tempBuf = null;

    /*
    if (hasStdin)
      writeHeader(fcgiSocket, ws, FCGI_STDIN, 0);
    */
    writeHeader(fcgiSocket, ws, FCGI_STDIN, 0);

    FastCGIInputStream is = new FastCGIInputStream(fcgiSocket);

    int ch = parseHeaders(res, is);

    if (ch >= 0)
      out.write(ch);

    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();
    
    while ((sublen = is.read(buffer, 0, buffer.length)) > 0) {
      out.write(buffer, 0, sublen);
    }

    TempBuffer.free(tb);

    return ! is.isDead() && keepalive;
  }

  private void setEnvironment(FastCGISocket fcgi,
			      WriteStream ws, HttpServletRequest req)
    throws IOException
  {
    addHeader(fcgi, ws, "REQUEST_URI", req.getRequestURI());
    addHeader(fcgi, ws, "REQUEST_METHOD", req.getMethod());
    
    addHeader(fcgi, ws, "SERVER_SOFTWARE", "Resin/" + com.caucho.Version.VERSION);
    
    addHeader(fcgi, ws, "SERVER_NAME", req.getServerName());
    //addHeader(fcgi, ws, "SERVER_ADDR=" + req.getServerAddr());
    addHeader(fcgi, ws, "SERVER_PORT", String.valueOf(req.getServerPort()));
    
    addHeader(fcgi, ws, "REMOTE_ADDR", req.getRemoteAddr());
    addHeader(fcgi, ws, "REMOTE_HOST", req.getRemoteAddr());
    // addHeader(fcgi, ws, "REMOTE_PORT=" + req.getRemotePort());

    if (req.getRemoteUser() != null)
      addHeader(fcgi, ws, "REMOTE_USER", req.getRemoteUser());
    else
      addHeader(fcgi, ws, "REMOTE_USER", "");
    if (req.getAuthType() != null)
      addHeader(fcgi, ws, "AUTH_TYPE", req.getAuthType());
    
    addHeader(fcgi, ws, "GATEWAY_INTERFACE", "CGI/1.1");
    addHeader(fcgi, ws, "SERVER_PROTOCOL", req.getProtocol());
    if (req.getQueryString() != null)
      addHeader(fcgi, ws, "QUERY_STRING", req.getQueryString());
    else
      addHeader(fcgi, ws, "QUERY_STRING", "");

    String scriptPath = req.getServletPath();
    String pathInfo = req.getPathInfo();

    WebApp webApp = (WebApp) req.getServletContext();
    
    Path appDir = webApp.getAppDir();
    String realPath = webApp.getRealPath(scriptPath);
    
    if (! appDir.lookup(realPath).isFile() && pathInfo != null)
      scriptPath = scriptPath + pathInfo;

    /*
     * FastCGI (specifically quercus) uses the PATH_INFO and PATH_TRANSLATED
     * for the script path.
     */
    log.finer("FCGI file: " + webApp.getRealPath(scriptPath));

    addHeader(fcgi, ws, "PATH_INFO", req.getContextPath() + scriptPath);
    addHeader(fcgi, ws, "PATH_TRANSLATED", webApp.getRealPath(scriptPath));
    
    /* These are the values which would be sent to CGI.
    addHeader(fcgi, ws, "SCRIPT_NAME", req.getContextPath() + scriptPath);
    addHeader(fcgi, ws, "SCRIPT_FILENAME", app.getRealPath(scriptPath));
    
    if (pathInfo != null) {
      addHeader(fcgi, ws, "PATH_INFO", pathInfo);
      addHeader(fcgi, ws, "PATH_TRANSLATED", req.getRealPath(pathInfo));
    }
    else {
      addHeader(fcgi, ws, "PATH_INFO", "");
      addHeader(fcgi, ws, "PATH_TRANSLATED", "");
    }
    */

    int contentLength = req.getContentLength();
    if (contentLength < 0)
      addHeader(fcgi, ws, "CONTENT_LENGTH", "0");
    else
      addHeader(fcgi, ws, "CONTENT_LENGTH", String.valueOf(contentLength));

    ServletContext rootContext = webApp.getContext("/");

    if (rootContext != null)
      addHeader(fcgi, ws, "DOCUMENT_ROOT", rootContext.getRealPath("/"));

    CharBuffer cb = new CharBuffer();
    
    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String value = req.getHeader(key);

      if (key.equalsIgnoreCase("content-length"))
        addHeader(fcgi, ws, "CONTENT_LENGTH", value);
      else if (key.equalsIgnoreCase("content-type"))
        addHeader(fcgi, ws, "CONTENT_TYPE", value);
      else if (key.equalsIgnoreCase("if-modified-since")) {
      }
      else if (key.equalsIgnoreCase("if-none-match")) {
      }
      else if (key.equalsIgnoreCase("authorization")) {
      }
      else if (key.equalsIgnoreCase("proxy-authorization")) {
      }
      else
        addHeader(fcgi, ws, convertHeader(cb, key), value);
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
    CharBuffer key = new CharBuffer();
    CharBuffer value = new CharBuffer();

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
	int status = 0;
	int len = value.length();

	for (int i = 0; i < len; i++) {
	  char digit = value.charAt(i);

	  if ('0' <= digit && digit <= '9')
	    status = 10 * status + digit - '0';
	  else
	    break;
	}
	
	res.setStatus(status);
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

  private void addHeader(FastCGISocket fcgiSocket, WriteStream ws,
			 String key, String value)
    throws IOException
  {
    if (value == null)
      return;
    
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

    writeHeader(fcgiSocket, ws, FCGI_PARAMS, len);
    
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

  private void addHeader(FastCGISocket fcgiSocket, WriteStream ws,
			 CharBuffer key, String value)
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

    writeHeader(fcgiSocket, ws, FCGI_PARAMS, len);
    
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

  private void writeHeader(FastCGISocket fcgiSocket,
			   WriteStream ws, int type, int length)
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

  public void destroy()
  {
    FastCGISocket socket;
    
    while ((socket = _freeSockets.allocate()) != null) {
      try {
	socket.close();
      } catch (Throwable e) {
      }
    }
	   
    _fcgiServlets.remove(new Integer(_servletId));
  }

  static class FastCGIInputStream extends InputStream {
    private FastCGISocket _fcgiSocket;
    
    private InputStream _is;
    private int _chunkLength;
    private int _padLength;
    private boolean _isDead;

    public FastCGIInputStream()
    {
    }

    public FastCGIInputStream(FastCGISocket fcgiSocket)
    {
      init(fcgiSocket);
    }

    public void init(FastCGISocket fcgiSocket)
    {
      _fcgiSocket = fcgiSocket;
      
      _is = fcgiSocket.getReadStream();
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
      do {
	if (_chunkLength > 0) {
	  _chunkLength--;
	  return _is.read();
	}
      } while (readNext());

      return -1;
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
        _is.read();
	
        switch (type) {
        case FCGI_END_REQUEST:
        {
          int appStatus = ((_is.read() << 24) +
                           (_is.read() << 16) +
                           (_is.read() << 8) +
                           (_is.read()));
          int pStatus = _is.read();

	  if (log.isLoggable(Level.FINER)) {
	    log.finer(_fcgiSocket + ": FCGI_END_REQUEST(appStatus:" + appStatus + ", pStatus:" + pStatus + ")");
	  }
	  
          if (appStatus != 0)
            _isDead = true;

	  if (pStatus != FCGI_REQUEST_COMPLETE)
	    _isDead = true;
          
          _is.skip(3);
          _is = null;
          return false;
        }

        case FCGI_STDOUT:
	  if (log.isLoggable(Level.FINER)) {
	    log.finer(_fcgiSocket + ": FCGI_STDOUT(length:" + length + ", padding:" + padding + ")");
	  }
	  
          if (length == 0) {
	    if (padding > 0)
	      _is.skip(padding);
	    
            break;
	  }
          else {
            _chunkLength = length;
            _padLength = padding;
            return true;
          }

        case FCGI_STDERR:
	  if (log.isLoggable(Level.FINER)) {
	    log.finer(_fcgiSocket + ": FCGI_STDERR(length:" + length + ", padding:" + padding + ")");
	  }
	  
          byte []buf = new byte[length];
          _is.read(buf, 0, length);
          log.warning(new String(buf, 0, length));

	  if (padding > 0)
	    _is.skip(padding);
          break;

        default:
	  log.warning(_fcgiSocket + ": Unknown Protocol(" + type + ")");

	  _isDead = true;
          _is.skip(length + padding);
          break;
        }
      }

      _isDead = true;
        
      return false;
    }
  }

  static class FastCGISocket {
    private int _id;
    private int _keepaliveCount;
    private long _expireTime;
    private Socket _socket;
    private SocketStream _socketStream;
    private ReadStream _rs;
    private WriteStream _ws;

    FastCGISocket(int id, Socket socket, int maxKeepaliveCount)
    {
      _id = id;
      _socket = socket;
      _keepaliveCount = maxKeepaliveCount;

      _socketStream = new SocketStream(_socket);

      _ws = new WriteStream(_socketStream);
      _ws.setDisableClose(true);
      
      _rs = new ReadStream(_socketStream, _ws);
      _rs.setDisableClose(true);
      
      log.fine(this + ": open()");
    }

    int getId()
    {
      return _id;
    }

    void setExpire(long expireTime)
    {
      _expireTime = expireTime;
    }

    ReadStream getReadStream()
    {
      return _rs;
    }

    WriteStream getWriteStream()
    {
      return _ws;
    }

    boolean isValid()
    {
      return _socket != null && Alarm.getCurrentTime() < _expireTime;
    }

    boolean allocateKeepalive()
    {
      if (! isValid())
	return false;
      else
	return --_keepaliveCount > 0;
    }

    boolean isKeepalive()
    {
      return _keepaliveCount > 0;
    }

    void close()
    {
      try {
	log.fine(this + ": close()");
	
	Socket socket = _socket;
	_socket = null;

	_socketStream = null;

	if (socket != null)
	  socket.close();
      
	_ws.close();
	_rs.close();
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }

    public String toString()
    {
      return "FastCGISocket[" + _id + "," + _socket + "]";
    }
  }
}
