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

import com.caucho.admin.AverageTimeProbe;
import com.caucho.admin.SampleCountProbe;
import com.caucho.admin.ProbeManager;
import com.caucho.server.cluster.Server;
import com.caucho.server.connection.*;
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
import com.caucho.util.Alarm;
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
import javax.servlet.http.HttpServletResponse;

/**
 * Handles a new request from an HTTP connection.
 */
public class HttpRequest extends AbstractHttpRequest
  implements TcpServerRequest
{
  private static final Logger log
    = Logger.getLogger(HttpRequest.class.getName());

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

  private static final String REQUEST_TIME_PROBE
    = "Resin|Server|Http Request Time";
  private static final String REQUEST_COUNT_PROBE
    = "Resin|Server|Http Request Count";

  private final CharBuffer _method     // "GET"
    = new CharBuffer();
  private String _methodString;

  private final CharBuffer _uriHost    // www.caucho.com:8080
    = new CharBuffer();
  private final CharBuffer _hostBuffer
    = new CharBuffer();
  private CharSequence _host;

  private byte []_uri;                 // "/path/test.jsp/Junk?query=7"
  private int _uriLength;

  private final CharBuffer _protocol   // "HTTP/1.0"
    = new CharBuffer();
  private int _version;

  private final InvocationKey _invocationKey = new InvocationKey();

  private char []_headerBuffer;

  private CharSegment []_headerKeys;
  private CharSegment []_headerValues;
  private int _headerSize;

  private ChunkedInputStream _chunkedInputStream = new ChunkedInputStream();
  private ContentLengthStream _contentLengthStream = new ContentLengthStream();

  private AverageTimeProbe _requestTimeProbe;
  private SampleCountProbe _requestCountProbe;

  /**
   * Creates a new HttpRequest.  New connections reuse the request.
   *
   * @param server the owning server.
   */
  public HttpRequest(Server server, Connection conn)
  {
    super(server, conn);

    _requestTimeProbe
      = ProbeManager.createAverageTimeProbe(REQUEST_TIME_PROBE);

    _requestCountProbe
      = ProbeManager.createSampleCountProbe(REQUEST_COUNT_PROBE);
  }

  @Override
  protected HttpResponse createResponse()
  {
    return new HttpResponse(this, _conn.getWriteStream());
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
    return getRequestFacade() != null;
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

  //
  // HTTP request properties
  //

  /**
   * Returns a buffer containing the request method.
   */
  public CharSegment getMethodBuffer()
  {
    return _method;
  }

  /**
   * Returns the HTTP method (GET, POST, HEAD, etc.)
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
   * Returns true for a secure connection.
   */
  public boolean isSecure()
  {
    return _conn.isSecure();
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
   * Returns the virtual host from the invocation
   */
  private CharSequence getInvocationHost()
    throws IOException
  {
    if (_host != null)
      return _host;

    String virtualHost = _conn.getVirtualHost();
    if (virtualHost != null)
      return virtualHost;
    else if (_host != null) {
    }
    else if (_uriHost.length() > 0) {
      _host = _uriHost;
    }
    else if (_hostHeader != null) {
      _host = _hostHeader;
    }
    else if (HTTP_1_1 <= getVersion())
      throw new BadRequestException("HTTP/1.1 requires a Host header");

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
   * Returns the HTTP version of the request based on getProtocol().
   */
  int getVersion()
  {
    if (_version > 0)
      return _version;

    CharSegment protocol = getProtocolBuffer();
    if (protocol.equals("HTTP/1.1")) {
      _version = HTTP_1_1;
      return HTTP_1_1;
    }
    if (protocol.equals("HTTP/1.0")) {
      _version = HTTP_1_0;
      return _version;
    }
    else if (protocol.equals("HTTP/0.9")) {
      _version = HTTP_0_9;
      return HTTP_0_9;
    }
    else if (protocol.length() < 8) {
      _version = HTTP_0_9;
      return _version;
    }


    int i = protocol.indexOf('/');
    int len = protocol.length();
    int major = 0;
    for (i++; i < len; i++) {
      char ch = protocol.charAt(i);

      if ('0' <= ch && ch <= '9')
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

      if ('0' <= ch && ch <= '9')
        minor = 10 * minor + ch - '0';
      else
        break;
    }

    _version = 256 * major + minor;

    return _version;
  }

  //
  // HTTP request headers
  //

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
      tail = (_headerValues[_headerSize - 1].getOffset()
              + _headerValues[_headerSize - 1].getLength());
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

  //
  // attribute management
  //

  /**
   * Initialize any special attributes.
   */
  @Override
  protected void initAttributes(HttpServletRequestImpl request)
  {
    TcpConnection tcpConn = _tcpConn;

    if (tcpConn == null || ! tcpConn.isSecure())
      return;

    QSocket socket = tcpConn.getSocket();

    String cipherSuite = socket.getCipherSuite();
    request.setAttribute("javax.servlet.request.cipher_suite", cipherSuite);

    int keySize = socket.getCipherBits();
    if (keySize != 0)
      request.setAttribute("javax.servlet.request.key_size",
                           new Integer(keySize));

    try {
      X509Certificate []certs = socket.getClientCertificates();
      if (certs != null && certs.length > 0) {
        request.setAttribute("javax.servlet.request.X509Certificate",
                             certs[0]);
        request.setAttribute(com.caucho.security.AbstractLogin.LOGIN_NAME,
                             certs[0].getSubjectDN());
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  //
  // session management
  //

  /**
   * For SSL connections, use the SSL identifier.
   */
  public String findSessionIdFromConnection()
  {
    TcpConnection tcpConn = _tcpConn;

    if (tcpConn == null || ! tcpConn.isSecure())
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

  //
  // stream management
  //

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
   * Returns the raw input stream.
   */
  public ReadStream getRawInput()
  {
    return _rawRead;
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
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_server.getClassLoader());
      
      startRequest(HttpBufferStore.allocate((Server) _server));

      if (! parseRequest()) {
        return false;
      }

      CharSequence host = getInvocationHost();

      Invocation invocation = getInvocation(host, _uri, _uriLength);

      if (invocation == null)
        return false;

      HttpServletRequestImpl requestFacade = getRequestFacade();

      requestFacade.setInvocation(invocation);

      startInvocation();

      invocation.service(requestFacade, getResponseFacade());
    } catch (ClientDisconnectException e) {
      getResponseFacade().killCache();
      killKeepalive();

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      getResponseFacade().killCache();
      killKeepalive();

      sendRequestError(e);

      return false;
    } finally {
      finishInvocation();

      if (getRequestFacade() != null) {
        _requestTimeProbe.addData(Alarm.getCurrentTime() - getStartTime());
        _requestCountProbe.addData();
      }

      if (! isSuspend()) {
        finishRequest();
      }

      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  private boolean parseRequest()
    throws IOException
  {
    try {
      if (! readRequest(_rawRead)) {
        if (log.isLoggable(Level.FINE))
          log.fine(dbgId() + "read timeout");

        clearRequest();

        return false;
      }

      if (log.isLoggable(Level.FINE)) {
        log.fine(dbgId() + _method + " "
                 + new String(_uri, 0, _uriLength) + " " + _protocol);
        log.fine(dbgId() + "Remote-IP: " + _conn.getRemoteHost()
                 + ":" + _conn.getRemotePort());
      }

      parseHeaders(_rawRead);

      return true;
    } catch (ClientDisconnectException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      throw new BadRequestException(String.valueOf(e), e);
    }
  }

  /**
   * Clear the request variables in preparation for a new request.
   *
   * @param s the read stream for the request
   */
  @Override
  protected void startRequest(HttpBufferStore httpBuffer)
    throws IOException
  {
    super.startRequest(httpBuffer);

    _method.clear();
    _methodString = null;
    _protocol.clear();

    _uriLength = 0;
    _uri = httpBuffer.getUriBuffer();

    _uriHost.clear();
    _host = null;

    _headerSize = 0;
    _headerBuffer = httpBuffer.getHeaderBuffer();
    _headerKeys = httpBuffer.getHeaderKeys();
    _headerValues = httpBuffer.getHeaderValues();
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

    // skip leading whitespace
    do {
      if (readLength <= readOffset) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }
      
      ch = readBuffer[readOffset++];
    } while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');

    char []buffer = _method.getBuffer();
    int length = buffer.length;
    int offset = 0;

    // scan method
    while (true) {
      if (length <= offset) {
      }
      else if ('a' <= ch && ch <= 'z')
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
      if (readLength <= readOffset) {
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

      if (readLength <= readOffset) {
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
          if (readLength <= readOffset) {
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
      if (readLength <= readOffset) {
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
      if (length <= offset) {
      }
      else if ('a' <= ch && ch <= 'z')
        buffer[offset++] = ((char) (ch + 'A' - 'a'));
      else
        buffer[offset++] = (char) ch;

      if (readLength <= readOffset) {
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
      if (readLength <= readOffset) {
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
