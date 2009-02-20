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

package com.caucho.server.http;

import com.caucho.server.cluster.Server;
import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.Connection;
import com.caucho.server.connection.HttpServletRequestImpl;
import com.caucho.server.connection.HttpServletResponseImpl;
import com.caucho.server.dispatch.BadRequestException;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.port.ServerRequest;
import com.caucho.server.port.TcpServerRequest;
import com.caucho.server.port.TcpConnection;
import com.caucho.server.port.TcpCometController;
import com.caucho.server.cluster.*;
import com.caucho.server.webapp.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Handles a new request from an HTTP connection.
 */
public class HttpRequest extends AbstractHttpRequest
  implements TcpServerRequest
{
  static final Logger log = Logger.getLogger(HttpRequest.class.getName());

  static final int HTTP_0_9 = 0x0009;
  static final int HTTP_1_0 = 0x0100;
  static final int HTTP_1_1 = 0x0101;

  static final CharBuffer _getCb = new CharBuffer("GET");
  static final CharBuffer _headCb = new CharBuffer("HEAD");
  static final CharBuffer _postCb = new CharBuffer("POST");

  static final char []_hostCb = "Host".toCharArray();
  static final char []_userAgentCb = "User-Agent".toCharArray();

  static final CharBuffer _http11Cb = new CharBuffer("HTTP/1.1");
  static final CharBuffer _http10Cb = new CharBuffer("HTTP/1.0");

  private String _scheme;           // "http:" or "https:"
  private boolean _isSecure;

  private CharBuffer _method;       // "GET"
  private String _methodString;

  private CharBuffer _uriHost;      // www.caucho.com:8080
  private CharSequence _host;
  private CharBuffer _hostBuffer = new CharBuffer();

  private final byte []_uri;              // "/path/test.jsp/Junk?query=7"
  private int _uriLength;

  private int _urlLengthMax = 8192;

  private CharBuffer _protocol;     // "HTTP/1.0"
  private int _version;

  private final InvocationKey _invocationKey = new InvocationKey();

  private final char []_headerBuffer;

  private CharSegment []_headerKeys;
  private CharSegment []_headerValues;
  private int _headerCapacity = 256;
  private int _headerSize;

  private boolean _hasRequest;

  private ChunkedInputStream _chunkedInputStream = new ChunkedInputStream();
  private ContentLengthStream _contentLengthStream = new ContentLengthStream();

  private ErrorPageManager _errorManager = new ErrorPageManager(null);

  private HttpServletRequestImpl _requestFacade;
  private HttpServletResponseImpl _responseFacade;

  private boolean _initAttributes;

  /**
   * Creates a new HttpRequest.  New connections reuse the request.
   *
   * @param server the owning server.
   */
  public HttpRequest(DispatchServer server, Connection conn)
  {
    super(server, conn);

    _response = new HttpResponse(this);
    _response.init(conn.getWriteStream());

    if (server instanceof Server)
      _urlLengthMax = ((Server) server).getUrlLengthMax();

    // XXX: response.setIgnoreClientDisconnect(server.getIgnoreClientDisconnect());

    _uri = new byte[_urlLengthMax];

    _method = new CharBuffer();
    _uriHost = new CharBuffer();
    _protocol = new CharBuffer();

    if (TempBuffer.isSmallmem()) {
      _headerBuffer = new char[4 * 1024];
      _headerCapacity = 64;
    }
    else {
      _headerBuffer = new char[16 * 1024];
      _headerCapacity = 256;
    }
    
    _headerSize = 0;
    _headerKeys = new CharSegment[_headerCapacity];
    _headerValues = new CharSegment[_headerCapacity];
    for (int i = 0; i < _headerCapacity; i++) {
      _headerKeys[i] = new CharSegment();
      _headerValues[i] = new CharSegment();
    }
  }

  /**
   * Return true if the request waits for a read before beginning.
   */
  public final boolean isWaitForRead()
  {
    return true;
  }

  /**
   * Returns true if the request exists
   */
  @Override
  public boolean hasRequest()
  {
    return _hasRequest;
  }
  
  /**
   * Handles a new HTTP request.
   *
   * <p>Note: ClientDisconnectException must be rethrown to
   * the caller.
   *
   * @return true if the connection should stay open (keepalive)
   */
  public boolean handleRequest()
    throws IOException
  {
    _hasRequest = false;
    
    try {
      startRequest();
      startInvocation();
      
      _response.start();

      // XXX: use same one for keepalive?
      _requestFacade = new HttpServletRequestImpl(this);
      _responseFacade = new HttpServletResponseImpl(_response);
      _requestFacade.setResponse(_responseFacade);

      try {
	if (! readRequest(_rawRead)) {
	  if (log.isLoggable(Level.FINE))
	    log.fine(dbgId() + "read timeout");

	  return false;
	}

	setStartTime();

	_hasRequest = true;

	_isSecure = _conn.isSecure() || _conn.getLocalPort() == 443;

	if (_protocol.length() == 0)
	  _protocol.append("HTTP/0.9");

	if (log.isLoggable(Level.FINE)) {
	  log.fine(dbgId() + _method + " " +
		   new String(_uri, 0, _uriLength) + " " + _protocol);
	  log.fine(dbgId() + "Remote-IP: " + _conn.getRemoteHost() + ":" + _conn.getRemotePort());
	}

	parseHeaders(_rawRead);

	if (getVersion() >= HTTP_1_1 && isForce10()) {
	  _protocol.clear();
	  _protocol.append("HTTP/1.0");
	  _version = HTTP_1_0;
	}
      } catch (ClientDisconnectException e) {
	throw e;
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);

	throw new BadRequestException(String.valueOf(e));
      }

      CharSequence host = getHost();
      if (host == null && getVersion() >= HTTP_1_1)
	throw new BadRequestException("HTTP/1.1 requires host");

      String ipHost = _conn.getVirtualHost();
      if (ipHost != null)
	host = ipHost;

      _invocationKey.init(_isSecure,
			  host, _conn.getLocalPort(),
			  _uri, _uriLength);

      Invocation invocation = getInvocation(host);

      if (invocation == null)
	return false;

      setInvocation(invocation);

      startInvocation();

      invocation.service(_requestFacade, _responseFacade);
    } catch (ClientDisconnectException e) {
      _response.killCache();

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      _response.killCache();
      killKeepalive();

      try {
        _errorManager.sendServletError(e, this, _response);
      } catch (ClientDisconnectException e1) {
        throw e1;
      } catch (Throwable e1) {
        log.log(Level.FINE, e1.toString(), e1);
      }

      if (_server instanceof Server) {
	WebApp webApp = ((Server) _server).getDefaultWebApp();
	if (webApp != null)
	  webApp.accessLog(_requestFacade, _responseFacade);
      }

      return false;
    } finally {
      finishInvocation();

      if (! isSuspend()) {
	finishRequest();
      }
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(dbgId() +
               (isKeepalive() ? "keepalive" : "no-keepalive"));
    }

    return isKeepalive();
  }

  private Invocation getInvocation(CharSequence host)
    throws Throwable
  {
    Invocation invocation = _server.getInvocation(_invocationKey);

    if (invocation == null) {
      invocation = _server.createInvocation();
      invocation.setSecure(_isSecure);

      if (host != null) {
	String hostName = host.toString().toLowerCase();

	invocation.setHost(hostName);
	invocation.setPort(_conn.getLocalPort());

	// Default host name if the host doesn't have a canonical
	// name
	int p = hostName.indexOf(':');
	if (p > 0)
	  invocation.setHostName(hostName.substring(0, p));
	else
	  invocation.setHostName(hostName);
      }

      InvocationDecoder decoder = _server.getInvocationDecoder();

      decoder.splitQueryAndUnescape(invocation, _uri, _uriLength);

      if (_server.isModified()) {
	_server.logModified(log);

	_invocation = invocation;
	if (_server instanceof Server)
	  _invocation.setWebApp(((Server) _server).getDefaultWebApp());

	restartServer();
	
	return null;
      }

      invocation = _server.buildInvocation(_invocationKey.clone(),
					   invocation);
    }

    invocation = invocation.getRequestInvocation(this);

    return invocation;
  }
  
  /**
   * Handles a comet-style resume.
   *
   * @return true if the connection should stay open (keepalive)
   */
  @Override
  public boolean handleResume()
    throws IOException
  {
    try {
      startInvocation();

      if (! isComet())
	return false;

      String url = _tcpConn.getCometPath();

      // servlet 3.0 spec defaults to suspend
      _tcpConn.suspend();
	  
      if (url != null) {
	WebApp webApp = getWebApp();

	RequestDispatcherImpl disp
	  = (RequestDispatcherImpl) webApp.getRequestDispatcher(url);

	if (disp != null) {
	  disp.forwardResume(_requestFacade, _responseFacade);
	  
	  return isSuspend();
	}
      }
	
      _invocation.doResume(_requestFacade, _responseFacade);
    } catch (ClientDisconnectException e) {
      _response.killCache();

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      // isResume = false;
      _response.killCache();
      killKeepalive();

      return false;
    } finally {
      finishInvocation();
	
      if (! isSuspend())
	finishRequest();
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(dbgId() +
               (isKeepalive() ? "keepalive" : "no-keepalive"));
    }

    return isSuspend();
  }

  /**
   * There are some bogus clients that can't deal with HTTP/1.1 even
   * though they advertise it.
   */
  private boolean isForce10()
  {
    return false;
  }

  /**
   * Returns true for the top-level request, but false for any include()
   * or forward()
   */
  public boolean isTop()
  {
    return true;
  }

  protected boolean checkLogin()
  {
    return true;
  }

  /**
   * Clear the request variables in preparation for a new request.
   *
   * @param s the read stream for the request
   */
  @Override
  protected void startRequest()
    throws IOException
  {
    super.startRequest();

    _method.clear();
    _methodString = null;
    _protocol.clear();
    _uriLength = 0;
    _uriHost.clear();
    _host = null;

    _headerSize = 0;
    _initAttributes = false;
  }

  /**
   * Returns true for a secure connection.
   */
  public boolean isSecure()
  {
    return _isSecure;
  }

  /**
   * Read the first line of a request:
   *
   * GET [http://www.caucho.com[:80]]/path [HTTP/1.x]
   *
   * @return true if the request is valid
   */
  private boolean readRequest(ReadStream s)
    throws IOException
  {
    int i = 0;

    byte []readBuffer = s.getBuffer();
    int readOffset = s.getOffset();
    int readLength = s.getLength();
    int ch;

    if (readOffset >= readLength) {
      try {
        if ((readLength = s.fillBuffer()) < 0)
          return false;
      } catch (InterruptedIOException e) {
        log.fine(dbgId() + "keepalive timeout");
        return false;
      }
      readOffset = 0;
    }
    ch = readBuffer[readOffset++];

    // conn.setAccessTime(getDate());

    // skip leading whitespace
    while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }
      ch = readBuffer[readOffset++];
    }

    char []buffer = _method.getBuffer();
    int length = buffer.length;
    int offset = 0;

    // scan method
    while (true) {
      if (length <= offset) {
      }
      else if (ch >= 'a' && ch <= 'z')
	buffer[offset++] = ((char) (ch + 'A' - 'a'));
      else if (ch > ' ')
	buffer[offset++] = (char) ch;
      else
	break;

      if (readLength <= readOffset) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }
      ch = readBuffer[readOffset++];
    }

    _method.setLength(offset);

    // skip whitespace
    while (ch == ' ' || ch == '\t') {
      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }

      ch = readBuffer[readOffset++];
    }

    byte []uriBuffer = _uri;
    int uriLength = 0;

    // skip 'http:'
    if (ch != '/') {
      while (ch > ' ' && ch != '/') {
        if (readOffset >= readLength) {
          if ((readLength = s.fillBuffer()) < 0)
            return false;
          readOffset = 0;
        }
        ch = readBuffer[readOffset++];
      }

      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0) {
          if (ch == '/') {
            uriBuffer[uriLength++] = (byte) ch;
            _uriLength = uriLength;
          }

          return true;
        }
        readOffset = 0;
      }

      int ch1 = readBuffer[readOffset++];

      if (ch1 != '/') {
        uriBuffer[uriLength++] = (byte) ch;
        ch = ch1;
      }
      else {
        // read host
        host:
        while (true) {
          if (readOffset >= readLength) {
            if ((readLength = s.fillBuffer()) < 0) {
              return true;
            }
            readOffset = 0;
          }
          ch = readBuffer[readOffset++];

          switch (ch) {
          case ' ': case '\t': case '\n': case '\r':
            break host;

          case '?':
            break host;

          case '/':
            break host;

          default:
            _uriHost.append((char) ch);
            break;
          }
        }
      }
    }

    // read URI
    uri:
    while (true) {
      switch (ch) {
      case ' ': case '\t': case '\n': case '\r':
	break uri;

      default:
        // There's no check for overrunning the length because
        // allowing resizing would allow a DOS memory attack and
        // also lets us save a bit of efficiency.
        uriBuffer[uriLength++] = (byte) ch;
        break;
      }

      if (readOffset >= readLength) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0) {
          _uriLength = uriLength;
          return true;
        }
      }
      ch = readBuffer[readOffset++];
    }

    _uriLength = uriLength;

    // skip whitespace
    while (ch == ' ' || ch == '\t') {
      if (readOffset >= readLength) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0)
          return true;
      }
      ch = readBuffer[readOffset++];
    }

    buffer = _protocol.getBuffer();
    length = buffer.length;
    offset = 0;
    // scan protocol
    while (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') {
      if (offset >= length) {
      }
      else if (ch >= 'a' && ch <= 'z')
	buffer[offset++] = ((char) (ch + 'A' - 'a'));
      else
	buffer[offset++] = (char) ch;

      if (readOffset >= readLength) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0) {
          _protocol.setLength(offset);
          return true;
        }
      }
      ch = readBuffer[readOffset++];
    }
    _protocol.setLength(offset);

    if (offset != 8) {
      _protocol.append("HTTP/0.9");
      _version = HTTP_0_9;
    }
    else if (buffer[7] == '1') // && _protocol.equals(_http11Cb))
      _version = HTTP_1_1;
    else if (buffer[7] == '0') // && _protocol.equals(_http10Cb))
      _version = HTTP_1_0;
    else
      _version = HTTP_0_9;

    // skip to end of line
    while (ch != '\n') {
      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0)
          return true;
        readOffset = 0;
      }
      ch = readBuffer[readOffset++];
    }

    s.setOffset(readOffset);

    return true;
  }

  /**
   * Parses headers from the read stream.
   *
   * @param s the input read stream
   */
  private void parseHeaders(ReadStream s) throws IOException
  {
    // This is still slowest part of the web server.  I don't see how
    // to improve it much more, but there must be a way.
    int version = getVersion();

    if (version < HTTP_1_0) {
      return;
    }

    if (version < HTTP_1_1)
      killKeepalive();

    byte []readBuffer = s.getBuffer();
    int readOffset = s.getOffset();
    int readLength = s.getLength();

    char []headerBuffer = _headerBuffer;
    int headerOffset = 1;
    int headerBufferSize = headerBuffer.length;
    headerBuffer[0] = 'z';
    int headerSize = 0;
    _headerSize = 0;

    CharSegment []headerKeys = _headerKeys;
    CharSegment []headerValues = _headerValues;

    boolean debug = log.isLoggable(Level.FINE);

    while (true) {
      int ch;

      int keyOffset = headerOffset;

      // scan the key
      while (true) {
        if (readLength <= readOffset) {
          readOffset = 0;
          if ((readLength = s.fillBuffer()) <= 0)
            return;
        }
        ch = readBuffer[readOffset++];

        if (ch == '\n') {
          s.setOffset(readOffset);
          return;
        }
        else if (ch == ':')
          break;

        headerBuffer[headerOffset++] = (char) ch;
      }

      while (headerBuffer[headerOffset - 1] == ' ')
        headerOffset--;

      int keyLength = headerOffset - keyOffset;
      headerKeys[headerSize].init(headerBuffer, keyOffset, keyLength);

      do {
        if (readLength <= readOffset) {
          readOffset = 0;
          if ((readLength = s.fillBuffer()) <= 0)
            return;
        }
        ch = readBuffer[readOffset++];
      } while (ch == ' ' || ch == '\t');

      int valueOffset = headerOffset;

      // scan the value
      while (true) {
        if (readLength <= readOffset) {
          readOffset = 0;
          if ((readLength = s.fillBuffer()) <= 0)
            break;
        }

        if (ch == '\n') {
          int ch1 = readBuffer[readOffset];

          if (ch1 == ' ' || ch1 == '\t') {
            ch = ' ';
            readOffset++;

            if (headerBuffer[headerOffset - 1] == '\r')
              headerOffset--;
          }
          else
            break;
        }

        headerBuffer[headerOffset++] = (char) ch;

        ch = readBuffer[readOffset++];
      }

      while (headerBuffer[headerOffset - 1] <= ' ')
        headerOffset--;

      int valueLength = headerOffset - valueOffset;
      headerValues[headerSize].init(headerBuffer, valueOffset, valueLength);

      if (debug) {
        log.fine(dbgId() +
                 headerKeys[headerSize] + ": " + headerValues[headerSize]);
      }

      if (addHeaderInt(headerBuffer, keyOffset, keyLength,
                       headerValues[headerSize])) {
        headerSize++;
      }

      _headerSize = headerSize;
    }
  }

  /**
   * Returns the HTTP version of the request based on getProtocol().
   */
  int getVersion()
  {
    if (_version > 0)
      return _version;

    CharSegment protocol = getProtocolBuffer();
    if (protocol.length() < 8) {
      _version = HTTP_0_9;
      return _version;
    }

    if (protocol.equals("HTTP/1.0")) {
      _version = HTTP_1_0;
      return _version;
    }
    else if (protocol.equals("HTTP/1.1")) {
      _version = HTTP_1_1;
      return HTTP_1_1;
    }
    else if (protocol.equals("HTTP/0.9")) {
      _version = HTTP_0_9;
      return HTTP_0_9;
    }

    int i = protocol.indexOf('/');
    int len = protocol.length();
    int major = 0;
    for (i++; i < len; i++) {
      char ch = protocol.charAt(i);

      if (ch >= '0' && ch <= '9')
	major = 10 * major + ch - '0';
      else if (ch == '.')
	break;
      else {
	_version = HTTP_1_0;
	return _version;
      }
    }

    int minor = 0;
    for (i++; i < len; i++) {
      char ch = protocol.charAt(i);

      if (ch >= '0' && ch <= '9')
	minor = 10 * minor + ch - '0';
      else
	break;
    }

    _version = 256 * major + minor;

    return _version;
  }

  /**
   * Returns the header.
   */
  public String getMethod()
  {
    if (_methodString == null) {
      CharSegment cb = getMethodBuffer();
      if (cb.length() == 0) {
        _methodString = "GET";
        return _methodString;
      }

      switch (cb.charAt(0)) {
      case 'G':
        _methodString = cb.equals(_getCb) ? "GET" : cb.toString();
        break;

      case 'H':
        _methodString = cb.equals(_headCb) ? "HEAD" : cb.toString();
        break;

      case 'P':
        _methodString = cb.equals(_postCb) ? "POST" : cb.toString();
        break;

      default:
        _methodString = cb.toString();
      }
    }

    return _methodString;

  }

  /**
   * Returns a buffer containing the request method.
   */
  public CharSegment getMethodBuffer()
  {
    return _method;
  }

  /**
   * Returns the virtual host of the request
   */
  protected CharSequence getHost()
  {
    if (_host != null)
      return _host;

    String virtualHost = _conn.getVirtualHost();
    if (virtualHost != null)
      _host = virtualHost;
    else if (_uriHost.length() > 0)
      _host = _uriHost;
    else
      _host = _hostHeader;

    return _host;
  }

  /**
   * Returns the byte buffer containing the request URI
   */
  public byte []getUriBuffer()
  {
    return _uri;
  }

  /**
   * Returns the length of the request URI
   */
  public int getUriLength()
  {
    return _uriLength;
  }

  /**
   * Returns the protocol.
   */
  public String getProtocol()
  {
    switch (_version) {
    case HTTP_1_1:
      return "HTTP/1.1";
    case HTTP_1_0:
      return "HTTP/1.0";
    case HTTP_0_9:
    default:
      return "HTTP/0.9";
    }
  }

  /**
   * Returns a char segment containing the protocol.
   */
  public CharSegment getProtocolBuffer()
  {
    return _protocol;
  }

  /**
   * Adds a new header.  Used only by the caching to simulate
   * If-Modified-Since.
   *
   * @param key the key of the new header
   * @param value the value for the new header
   */
  public void setHeader(String key, String value)
  {
    int tail;

    if (_headerSize > 0) {
      tail = (_headerValues[_headerSize - 1].getOffset() +
              _headerValues[_headerSize - 1].getLength());
    }
    else
      tail = 0;

    char []headerBuffer = _headerBuffer;
    for (int i = key.length() - 1; i >= 0; i--)
      headerBuffer[tail + i] = key.charAt(i);

    _headerKeys[_headerSize].init(headerBuffer, tail, key.length());

    tail += key.length();

    for (int i = value.length() - 1; i >= 0; i--)
      headerBuffer[tail + i] = value.charAt(i);

    _headerValues[_headerSize].init(headerBuffer, tail, value.length());
    _headerSize++;
  }

  /**
   * Returns the number of headers.
   */
  @Override
  public int getHeaderSize()
  {
    return _headerSize;
  }

  /**
   * Returns the header key
   */
  @Override
  public CharSegment getHeaderKey(int index)
  {
    return _headerKeys[index];
  }

  /**
   * Returns the header value
   */
  @Override
  public CharSegment getHeaderValue(int index)
  {
    return _headerValues[index];
  }

  /**
   * Returns the header.
   */
  public String getHeader(String key)
  {
    CharSegment buf = getHeaderBuffer(key);
    if (buf != null)
      return buf.toString();
    else
      return null;
  }

  /**
   * Returns the matching header.
   *
   * @param testBuf header key
   * @param length length of the key.
   */
  public CharSegment getHeaderBuffer(char []testBuf, int length)
  {
    char []keyBuf = _headerBuffer;
    CharSegment []headerKeys = _headerKeys;

    for (int i = _headerSize - 1; i >= 0; i--) {
      CharSegment key = headerKeys[i];

      if (key.length() != length)
        continue;

      int offset = key.getOffset();
      int j;
      for (j = length - 1; j >= 0; j--) {
        char a = testBuf[j];
        char b = keyBuf[offset + j];
        if (a == b)
          continue;

        if (a >= 'A' && a <= 'Z')
          a += 'a' - 'A';
        if (b >= 'A' && b <= 'Z')
          b += 'a' - 'A';
        if (a != b)
          break;
      }

      if (j < 0)
        return _headerValues[i];
    }

    return null;
  }

  /**
   * Returns the header value for the key, returned as a CharSegment.
   */
  public CharSegment getHeaderBuffer(String key)
  {
    int i = matchNextHeader(0, key);

    if (i >= 0)
      return _headerValues[i];
    else
      return null;
  }

  /**
   * Fills an ArrayList with the header values matching the key.
   *
   * @param values ArrayList which will contain the maching values.
   * @param key the header key to select.
   */
  public void getHeaderBuffers(String key, ArrayList<CharSegment> values)
  {
    int i = -1;
    while ((i = matchNextHeader(i + 1, key)) >= 0)
      values.add(_headerValues[i]);
  }

  /**
   * Return an enumeration of headers matching a key.
   *
   * @param key the header key to match.
   * @return the enumeration of the headers.
   */
  public Enumeration getHeaders(String key)
  {
    ArrayList<String> values = new ArrayList<String>();
    int i = -1;
    while ((i = matchNextHeader(i + 1, key)) >= 0)
      values.add(_headerValues[i].toString());

    return Collections.enumeration(values);
  }

  /**
   * Returns the index of the next header matching the key.
   *
   * @param i header index to start search
   * @param key header key to match
   *
   * @return the index of the next header matching, or -1.
   */
  private int matchNextHeader(int i, String key)
  {
    int size = _headerSize;
    int length = key.length();

    char []keyBuf = _headerBuffer;

    for (; i < size; i++) {
      CharSegment header = _headerKeys[i];

      if (header.length() != length)
        continue;

      int offset = header.getOffset();

      int j;
      for (j = 0; j < length; j++) {
        char a = key.charAt(j);
        char b = keyBuf[offset + j];
        if (a == b)
          continue;

        if (a >= 'A' && a <= 'Z')
          a += 'a' - 'A';
        if (b >= 'A' && b <= 'Z')
          b += 'a' - 'A';
        if (a != b)
          break;
      }

      if (j == length)
        return i;
    }

    return -1;
  }

  /**
   * Returns an enumeration of all the header keys.
   */
  public Enumeration getHeaderNames()
  {
    ArrayList<String> names = new ArrayList<String>();

    for (int i = 0; i < _headerSize; i++) {
      CharSegment name = _headerKeys[i];

      int j;
      for (j = 0; j < names.size(); j++) {
	String oldName = names.get(j);
	if (name.matches(oldName))
	  break;
      }
      if (j == names.size())
	names.add(j, name.toString());
    }

    return Collections.enumeration(names);
  }

  /**
   * Returns a stream for reading POST data.
   */
  public boolean initStream(ReadStream readStream, ReadStream rawRead)
    throws IOException
  {
    long contentLength = getLongContentLength();

    // needed to avoid auto-flush on read conflicting with partially
    // generated response
    rawRead.setSibling(null);

    String te;
    if (contentLength < 0 && HTTP_1_1 <= getVersion()
	&& (te = getHeader("Transfer-Encoding")) != null) {
      _chunkedInputStream.init(rawRead);
      readStream.init(_chunkedInputStream, null);
      return true;
    }
    // Otherwise use content-length
    else if (contentLength >= 0) {
      _contentLengthStream.init(rawRead, contentLength);
      _readStream.init(_contentLengthStream, null);

      return true;
    }
    else if (getMethod().equals("POST")) {
      _contentLengthStream.init(rawRead, 0);
      _readStream.init(_contentLengthStream, null);

      throw new com.caucho.server.dispatch.BadRequestException("POST requires content-length");
    }

    else {
      _contentLengthStream.init(rawRead, 0);
      _readStream.init(_contentLengthStream, null);

      return false;
    }
  }

  protected void skip()
    throws IOException
  {
    if (getMethod() == "GET")
      return;

    super.skip();
  }

  /**
   * Prints the remote address into a buffer.
   *
   * @param buffer the buffer which will contain the address.
   * @param offset the initial offset into the buffer.
   *
   * @return the final offset into the buffer.
   */
  /*
  public int printRemoteAddr(byte []buffer, int offset)
    throws IOException
  {
    Connection conn = getConnection();

    if (! (conn instanceof TcpConnection))
      return super.printRemoteAddr(buffer, offset);

    QSocket socket = ((TcpConnection) conn).getSocket();

    if (socket instanceof QJniSocket) {
      QJniSocket jniSocket = (QJniSocket) socket;
      long ip = jniSocket.getRemoteIP();

      for (int i = 24; i >= 0; i -= 8) {
        int value = (int) (ip >> i) & 0xff;

        if (value < 10)
          buffer[offset++] = (byte) (value + '0');
        else if (value < 100) {
          buffer[offset++] = (byte) (value / 10 + '0');
          buffer[offset++] = (byte) (value % 10 + '0');
        }
        else {
          buffer[offset++] = (byte) (value / 100 + '0');
          buffer[offset++] = (byte) (value / 10 % 10 + '0');
          buffer[offset++] = (byte) (value % 10 + '0');
        }

        if (i != 0)
          buffer[offset++] = (byte) '.';
      }
    }
    else {
      InetAddress addr = conn.getRemoteAddress();

      if (addr == null) {
        buffer[offset++] = (byte) '0';
        buffer[offset++] = (byte) '.';
        buffer[offset++] = (byte) '0';
        buffer[offset++] = (byte) '.';
        buffer[offset++] = (byte) '0';
        buffer[offset++] = (byte) '.';
        buffer[offset++] = (byte) '0';

        return offset;
      }

      byte []bytes = addr.getAddress();
      for (int i = 0; i < bytes.length; i++) {
        if (i != 0)
          buffer[offset++] = (byte) '.';

        int value = bytes[i] & 0xff;

        if (value < 10)
          buffer[offset++] = (byte) (value + '0');
        else if (value < 100) {
          buffer[offset++] = (byte) (value / 10 + '0');
          buffer[offset++] = (byte) (value % 10 + '0');
        }
        else {
          buffer[offset++] = (byte) (value / 100 + '0');
          buffer[offset++] = (byte) (value / 10 % 10 + '0');
          buffer[offset++] = (byte) (value % 10 + '0');
        }
      }
    }

    return offset;
  }
  */

  /**
   * Returns the client's remote host name.
   */
  /*
  public String getRemoteHost()
  {
    Connection conn = getConnection();

    if (conn instanceof TcpConnection) {
      QSocket socket = ((TcpConnection) conn).getSocket();

      if (socket instanceof QJniSocket) {
        QJniSocket jniSocket = (QJniSocket) socket;
        long ip = jniSocket.getRemoteIP();

        CharBuffer cb = _cb;
        cb.clear();

        for (int i = 24; i >= 0; i -= 8) {
          int value = (int) (ip >> i) & 0xff;

          if (value < 10)
            cb.append((char) (value + '0'));
          else if (value < 100) {
            cb.append((char) (value / 10 + '0'));
            cb.append((char) (value % 10 + '0'));
          }
          else {
            cb.append((char) (value / 100 + '0'));
            cb.append((char) (value / 10 % 10 + '0'));
            cb.append((char) (value % 10 + '0'));
          }

          if (i != 0)
            cb.append('.');
        }

        return cb.toString();
      }
    }

    InetAddress addr = conn.getRemoteAddress();

    byte []bytes = addr.getAddress();
    CharBuffer cb = _cb;
    cb.clear();
    for (int i = 0; i < bytes.length; i++) {
      int value = bytes[i] & 0xff;

      if (i != 0)
        cb.append('.');

      if (value < 10)
        cb.append((char) (value + '0'));
      else if (value < 100) {
        cb.append((char) (value / 10 + '0'));
        cb.append((char) (value % 10 + '0'));
      }
      else {
        cb.append((char) (value / 100 + '0'));
        cb.append((char) (value / 10 % 10 + '0'));
        cb.append((char) (value % 10 + '0'));
      }
    }

    return cb.toString();
  }
  */

  /**
   * Returns the named attribute.
   */
  public Object getAttribute(String name)
  {
    if (! _initAttributes)
      initAttributes();

    return super.getAttribute(name);
  }

  /**
   * Returns an enumeration of the attribute names.
   */
  public Enumeration<String> getAttributeNames()
  {
    if (! _initAttributes)
      initAttributes();

    return super.getAttributeNames();
  }

  /**
   * For SSL connections, use the SSL identifier.
   */
  public String findSessionIdFromConnection()
  {
    TcpConnection tcpConn = _tcpConn;
    
    if (! _isSecure || tcpConn == null)
      return null;

    QSocket socket = tcpConn.getSocket(); // XXX:
    /*
    if (! (socket instanceof SSLSocket))
      return null;

    SSLSession sslSession = ((SSLSocket) socket).getSession();
    if (sslSession == null)
      return null;

    byte []sessionId = sslSession.getId();
    if (sessionId == null)
      return null;

    CharBuffer cb = CharBuffer.allocate();
    Base64.encode(cb, sessionId, 0, sessionId.length);
    for (int i = cb.length() - 1; i >= 0; i--) {
      char ch = cb.charAt(i);
      if (ch == '/')
        cb.setCharAt(i, '-');
    }

    return cb.close();
    */
    return null;
  }

  /**
   * Returns the raw input stream.
   */
  public ReadStream getRawInput()
  {
    return _rawRead;
  }

  @Override
  public HttpServletRequest getRequestFacade()
  {
    return _requestFacade;
  }

  /**
   * Initialize any special attributes.
   */
  private void initAttributes()
  {
    _initAttributes = true;

    TcpConnection tcpConn = _tcpConn;
    
    if (! _isSecure || tcpConn == null)
      return;

    QSocket socket = tcpConn.getSocket();

    String cipherSuite = socket.getCipherSuite();
    super.setAttribute("javax.servlet.request.cipher_suite", cipherSuite);

    int keySize = socket.getCipherBits();
    if (keySize != 0)
      super.setAttribute("javax.servlet.request.key_size",
                         new Integer(keySize));

    try {
      X509Certificate []certs = socket.getClientCertificates();
      if (certs != null && certs.length > 0) {
        super.setAttribute("javax.servlet.request.X509Certificate", certs[0]);
        super.setAttribute(com.caucho.security.AbstractLogin.LOGIN_NAME,
                           certs[0].getSubjectDN());
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  public final void protocolCloseEvent()
  {
  }

  /**
   * Cleans up at the end of the invocation
   */
  @Override
  public void finishRequest()
    throws IOException
  {
    super.finishRequest();
    
    skip();
  }

  protected String dbgId()
  {
    if ("".equals(_server.getServerId()))
      return "Http[" + _conn.getId() + "] ";
    else
      return "Http[" + _server.getServerId() + ", " + _conn.getId() + "] ";
  }

  public String toString()
  {
    if ("".equals(_server.getServerId()))
      return "HttpRequest[" + _conn.getId() + "]";
    else {
      return ("HttpRequest[" + _server.getServerId()
	      + ", " + _conn.getId() + "]");
    }
  }
}
