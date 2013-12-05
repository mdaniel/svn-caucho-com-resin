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

package com.caucho.server.http;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.meter.ActiveTimeMeter;
import com.caucho.env.meter.AverageMeter;
import com.caucho.env.meter.MeterService;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.BadRequestException;
import com.caucho.server.dispatch.Invocation;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SocketTimeoutException;

/**
 * Handles a new request from an HTTP connection.
 */
public class HttpRequest extends AbstractHttpRequest
  implements ProtocolConnection
{
  private static final L10N L = new L10N(HttpRequest.class);
  
  private static final Logger log
    = Logger.getLogger(HttpRequest.class.getName());

  public static final int HTTP_0_9 = 0x0009;
  public static final int HTTP_1_0 = 0x0100;
  public static final int HTTP_1_1 = 0x0101;

  private static final CharBuffer _getCb = new CharBuffer("GET");
  private static final CharBuffer _headCb = new CharBuffer("HEAD");
  private static final CharBuffer _postCb = new CharBuffer("POST");
  
  private static final char []_toLowerAscii;
  private static final char []_toUpperAscii;
  private static final boolean []_isHttpWhitespace;

  private static final String REQUEST_TIME_PROBE
    = "Resin|Http|Request";
  private static final String REQUEST_READ_BYTES_PROBE
  = "Resin|Http|Request Read Bytes";
  private static final String REQUEST_WRITE_BYTES_PROBE
  = "Resin|Http|Request Write Bytes";

  private final CharBuffer _method     // "GET"
    = new CharBuffer();
  private String _methodString;

  private final CharBuffer _uriHost    // www.caucho.com:8080
    = new CharBuffer();
  private CharSequence _host;
  
  private byte []_uri;                 // "/path/test.jsp/Junk?query=7"
  private int _uriLength;

  private final CharBuffer _protocol   // "HTTP/1.0"
    = new CharBuffer();
  private int _version;
  
  private char []_headerBuffer;
  private int _headerLength;

  private CharSegment []_headerKeys;
  private CharSegment []_headerValues;
  private int _headerSize;

  private ChunkedInputStream _chunkedInputStream = new ChunkedInputStream();
  private ContentLengthStream _contentLengthStream = new ContentLengthStream();
  private RawInputStream _rawInputStream = new RawInputStream();

  private ActiveTimeMeter _requestTimeProbe;
  private AverageMeter _requestReadBytesProbe;
  private AverageMeter _requestWriteBytesProbe;

  /**
   * Creates a new HttpRequest.  New connections reuse the request.
   *
   * @param server the owning server.
   */
  public HttpRequest(ServletService server, SocketLink conn)
  {
    super(server, conn);

    _requestTimeProbe
      = MeterService.createActiveTimeMeter(REQUEST_TIME_PROBE);

    _requestReadBytesProbe
      = MeterService.createAverageMeter(REQUEST_READ_BYTES_PROBE, "");

    _requestWriteBytesProbe
      = MeterService.createAverageMeter(REQUEST_WRITE_BYTES_PROBE, "");
  }

  @Override
  public HttpResponse createResponse()
  {
    return new HttpResponse(this, getConnection().getWriteStream());
  }

  /**
   * Return true if the request waits for a read before beginning.
   */
  @Override
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
  @Override
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
   * Returns the virtual host of the request
   */
  @Override
  protected CharSequence getHost()
  {
    if (_host != null)
      return _host;

    String virtualHost = getConnection().getVirtualHost();
    
    if (virtualHost != null) {
      _host = virtualHost;
    }
    else if (_uriHost.length() > 0) {
      _host = _uriHost;
    }
    else if ((_host = getForwardedHostHeader()) != null) {
    }
    else {
      _host = getHostHeader();
    }

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

    String virtualHost = getConnection().getVirtualHost();
    if (virtualHost != null)
      return virtualHost;
    else if (_host != null) {
    }
    else if (_uriHost.length() > 0) {
      _host = _uriHost;
    }
    else if ((_host = getForwardedHostHeader()) != null) {
    }
    else if ((_host = getHostHeader()) != null) {
    }
    else if (HTTP_1_1 <= getVersion())
      throw new BadRequestException("HTTP/1.1 requires a Host header (Remote IP=" + getRemoteHost() + ")");

    return _host;
  }

  /**
   * Returns the byte buffer containing the request URI
   */
  @Override
  public byte []getUriBuffer()
  {
    return _uri;
  }

  /**
   * Returns the length of the request URI
   */
  @Override
  public int getUriLength()
  {
    return _uriLength;
  }

  /**
   * Returns the protocol.
   */
  @Override
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
    else if (protocol.equals("HTTP/1.0")) {
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
  @Override
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
    
    char []toLowerAscii = _toLowerAscii;

    for (int i = _headerSize - 1; i >= 0; i--) {
      CharSegment key = headerKeys[i];

      if (key.length() != length)
        continue;

      int offset = key.getOffset();
      int j;
      
      for (j = length - 1; j >= 0; j--) {
        char a = testBuf[j];
        char b = keyBuf[offset + j];
        
        if (a == b) {
          continue;
        }
        else if (toLowerAscii[a] != toLowerAscii[b]) {
          break;
        }
      }

      if (j < 0) {
        return _headerValues[i];
      }
    }

    return null;
  }

  /**
   * Returns the header value for the key, returned as a CharSegment.
   */
  @Override
  public CharSegment getHeaderBuffer(String key)
  {
    int i = matchNextHeader(0, key);

    if (i >= 0) {
      return _headerValues[i];
    }
    else {
      return null;
    }
  }

  /**
   * Fills an ArrayList with the header values matching the key.
   *
   * @param values ArrayList which will contain the maching values.
   * @param key the header key to select.
   */
  @Override
  public void getHeaderBuffers(String key, ArrayList<CharSegment> values)
  {
    int i = -1;
    
    while ((i = matchNextHeader(i + 1, key)) >= 0) {
      values.add(_headerValues[i]);
    }
  }

  /**
   * Return an enumeration of headers matching a key.
   *
   * @param key the header key to match.
   * @return the enumeration of the headers.
   */
  @Override
  public Enumeration<String> getHeaders(String key)
  {
    ArrayList<String> values = new ArrayList<String>();
    
    int i = -1;
    while ((i = matchNextHeader(i + 1, key)) >= 0) {
      values.add(_headerValues[i].toString());
    }

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
    char []toLowerAscii = _toLowerAscii;

    for (; i < size; i++) {
      CharSegment header = _headerKeys[i];

      if (header.length() != length)
        continue;

      int offset = header.getOffset();

      int j;
      for (j = 0; j < length; j++) {
        char a = key.charAt(j);
        char b = keyBuf[offset + j];
        
        if (a == b) {
          continue;
        }
        else if (toLowerAscii[a] != toLowerAscii[b]) {
          break;
        }
      }

      if (j == length) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Returns an enumeration of all the header keys.
   */
  @Override
  public Enumeration<String> getHeaderNames()
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
  @Override
  public void setHeader(String key, String value)
  {
    int tail;

    if (_headerSize > 0) {
      tail = (_headerValues[_headerSize - 1].getOffset()
              + _headerValues[_headerSize - 1].getLength());
    }
    else {
      tail = 0;
    }

    int keyLength = key.length();
    int valueLength = value.length();
    char []headerBuffer = _headerBuffer;
    
    for (int i = keyLength - 1; i >= 0; i--) {
      headerBuffer[tail + i] = key.charAt(i);
    }

    _headerKeys[_headerSize].init(headerBuffer, tail, keyLength);

    tail += keyLength;

    for (int i = valueLength - 1; i >= 0; i--) {
      headerBuffer[tail + i] = value.charAt(i);
    }

    _headerValues[_headerSize].init(headerBuffer, tail, valueLength);
    _headerSize++;
    // XXX: size
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
    SocketLink conn = getConnection();

    if (! (conn instanceof TcpSocketLink))
      return;
    
    TcpSocketLink tcpConn = (TcpSocketLink) conn;
    
    if (! conn.isSecure())
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
                             certs); //spec mandates array
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
  @Override
  public String findSessionIdFromConnection()
  {
    SocketLink link = getConnection();
    
    TcpSocketLink tcpConn = null;
    
    if (link instanceof TcpSocketLink)
      tcpConn = (TcpSocketLink) getConnection();

    if (tcpConn == null || ! tcpConn.isSecure())
      return null;

    /*
    QSocket socket = tcpConn.getSocket(); // XXX:
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
  @Override
  public boolean initStream(ReadStream readStream, ReadStream rawRead)
    throws IOException
  {
    // ReadStream readStream = getReadStream();

    // needed to avoid auto-flush on read conflicting with partially
    // generated response
    rawRead.setSibling(null);

    if (getConnection().isDuplex()) {
      _rawInputStream.init(rawRead);
      readStream.init(_rawInputStream, null);
      return true;
    }

    long contentLength = getLongContentLength();

    if (contentLength < 0 && HTTP_1_1 <= getVersion()
        && getHeader("Transfer-Encoding") != null) {
      _chunkedInputStream.init(rawRead);
      readStream.init(_chunkedInputStream, null);
      return true;
    }
    // Otherwise use content-length
    else if (contentLength >= 0) {
      _contentLengthStream.init(rawRead, contentLength);
      readStream.init(_contentLengthStream, null);

      return true;
    }
    else if (getMethod().equals("POST")) {
      _contentLengthStream.init(rawRead, 0);
      readStream.init(_contentLengthStream, null);

      throw new com.caucho.server.dispatch.BadRequestException("POST requires content-length");
    }

    else {
      _contentLengthStream.init(rawRead, 0);
      readStream.init(_contentLengthStream, null);

      return false;
    }
  }

  @Override
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
  @Override
  public ReadStream getRawInput()
  {
    return getRawRead();
  }

  /**
   * Handles a new HTTP request.
   *
   * <p>Note: ClientDisconnectException must be rethrown to
   * the caller.
   *
   * @return true if the connection should stay open (keepalive)
   */
  @Override
  public boolean handleRequest()
    throws IOException
  {
    boolean isInvocation = false;

    ServletService server = getServer();
    // Thread thread = getTcpSocketLink().getThread();
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    long startTime = 0;
    
    long startReadBytes = getRawRead().getPosition();
    long startWriteBytes = getRawWrite().getPosition();

    try {
      thread.setContextClassLoader(server.getClassLoader());
      
      startRequest();

      if (! parseRequest()) {
        if (log.isLoggable(Level.FINER)) {
          log.finer(dbgId() + " empty request");
        }
        
        return false;
      }

      CharSequence host = getInvocationHost();

      Invocation invocation = getInvocation(host, _uri, _uriLength);

      if (invocation == null) {
        if (log.isLoggable(Level.FINER)) {
          log.finer(dbgId() + " empty invocation");
        }
        
        return false;
      }

      HttpServletRequestImpl requestFacade = getRequestFacade();

      requestFacade.setInvocation(invocation);

      isInvocation = true;
      startTime = _requestTimeProbe.start();
      startInvocation();

      invocation.service(requestFacade, getResponseFacade());
    } catch (ClientDisconnectException e) {
      clientDisconnect();
      /*
      CauchoResponse response = getResponseFacade();
      
      if (response != null)
        response.killCache();

      killKeepalive();
      */

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      CauchoResponse response = getResponseFacade();
      
      if (response != null)
        response.killCache();
      
      killKeepalive("http handleRequest exception: " + e);

      sendRequestError(e);

      return false;
    } finally {
      if (isInvocation) {
        finishInvocation();
      }

      if (! isSuspend()) {
        finishRequest();
      }

      if (startTime > 0) {
        _requestTimeProbe.end(startTime);
        
        long endReadBytes = getRawRead().getPosition();
        long endWriteBytes = getRawWrite().getPosition();
        
        _requestReadBytesProbe.add(endReadBytes - startReadBytes);
        _requestWriteBytesProbe.add(endWriteBytes - startWriteBytes);
      }

      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  private boolean parseRequest()
    throws IOException
  {
    try {
      ReadStream is = getRawRead();

      if (! readRequest(is)) {
        clearRequest();

        return false;
      }

      if (log.isLoggable(Level.FINE)) {
        log.fine(dbgId() + _method + " "
                 + new String(_uri, 0, _uriLength) + " " + _protocol);
        log.fine(dbgId() + "Remote-IP: " + getRemoteHost()
                 + ":" + getRemotePort());
      }

      parseHeaders(is);

      return true;
    } catch (ClientDisconnectException e) {
      throw e;
    } catch (SocketTimeoutException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return false;
    } catch (ArrayIndexOutOfBoundsException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      throw new BadRequestException(L.l("Invalid request: URL or headers are too long"), e);
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
      
      throw new BadRequestException(String.valueOf(e), e);
    }
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
    
    // HttpBufferStore httpBuffer = getHttpBufferStore();
    HttpBufferStore bufferStore = getHttpBufferStore();

    _method.clear();
    _methodString = null;
    _protocol.clear();

    _uriLength = 0;
    
    if (bufferStore != null) {
      _uri = bufferStore.getUriBuffer();
      _headerBuffer = bufferStore.getHeaderBuffer();
      _headerKeys = bufferStore.getHeaderKeys();
      _headerValues = bufferStore.getHeaderValues();
    }
    else {
      // server/05h8
      _uri = getSmallUriBuffer(); // httpBuffer.getUriBuffer();
      
      _headerBuffer = getSmallHeaderBuffer(); // httpBuffer.getHeaderBuffer();
      _headerKeys = getSmallHeaderKeys();     // httpBuffer.getHeaderKeys();
      _headerValues = getSmallHeaderValues(); // httpBuffer.getHeaderValues();
    }

    _uriHost.clear();
    _host = null;

    _headerSize = 0;
    _headerLength = 0;
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
    // server/12o3 - default to 1.0 for error messages in request
    _version = HTTP_1_0;
    
    boolean []isHttpWhitespace = _isHttpWhitespace;
    char []toUpperAscii = _toUpperAscii;
    
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

      ch = readBuffer[readOffset++] & 0xff;
    } while (isHttpWhitespace[ch]);

    char []buffer = _method.getBuffer();
    int length = buffer.length;
    int offset = 0;

    // scan method
    while (true) {
      if (length <= offset) {
      }
      else if (ch > ' ') {
        buffer[offset++] = toUpperAscii[ch];
      }
      else {
        break;
      }
        

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
        if (readLength <= readOffset) {
          if ((readLength = s.fillBuffer()) < 0)
            return false;
          readOffset = 0;
        }
        ch = readBuffer[readOffset++];
      }
      
      if (ch != '/') {
        log.warning("Invalid Request (method='" + _method + "' url ch=0x" + Integer.toHexString(ch & 0xff) + ") (IP=" + getRemoteHost() + ")");
        
        throw new BadRequestException("Invalid Request(Remote IP=" + getRemoteHost() + ")");
      }

      if (readLength <= readOffset) {
        if ((readLength = s.fillBuffer()) < 0) {
          if (ch == '/') {
            uriBuffer[uriLength++] = (byte) ch;
            _uriLength = uriLength;
          }
          
          _version = 0;

          return true;
        }
        readOffset = 0;
      }

      int ch1 = readBuffer[readOffset++] & 0xff;

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
              _version = 0;
              
              return true;
            }
            readOffset = 0;
          }
          ch = readBuffer[readOffset++] & 0xff;

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
    
    int readTail = uriBuffer.length - uriLength - 1;
    
    if (readLength < readTail) {
      readTail = readLength;
    }

    // read URI
    while (! isHttpWhitespace[ch]) {
      // There's no check for over-running the length because
      // allowing resizing would allow a DOS memory attack and
      // also lets us save a bit of efficiency.
      uriBuffer[uriLength++] = (byte) ch;

      if (readTail <= readOffset) {
        if ((readTail = fillUrlTail(s, readOffset, uriLength)) <= 0) {
          _uriLength = uriLength;
          _version = 0;
          return true;
        }

        readOffset = s.getOffset();
        uriBuffer = _uri;
        uriLength = _uriLength;
      }
      
      ch = readBuffer[readOffset++] & 0xff;
    }
    
    _uriLength = uriLength;
    _version = 0;

    // skip whitespace
    while (ch == ' ' || ch == '\t') {
      if (readLength <= readOffset) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0)
          return true;
      }
      ch = readBuffer[readOffset++] & 0xff;
    }

    buffer = _protocol.getBuffer();
    length = buffer.length;
    offset = 0;
    
    // scan protocol
    while (! isHttpWhitespace[ch]) {
      if (offset < length) {
        buffer[offset++] = toUpperAscii[ch];
      }

      if (readLength <= readOffset) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0) {
          _protocol.setLength(offset);
          return true;
        }
      }
      ch = readBuffer[readOffset++] & 0xff;
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
    int version = getVersion();

    if (version < HTTP_1_0) {
      return;
    }

    if (version < HTTP_1_1) {
      killKeepalive("http client version less than 1.1: " + version);
    }

    byte []readBuffer = s.getBuffer();
    int readOffset = s.getOffset();
    int readLength = s.getLength();

    char []headerBuffer = _headerBuffer;
    headerBuffer[0] = 'z';
    int headerOffset = 1;
    _headerSize = 0;
    
    int readTail = readLength;
    if (headerBuffer.length - 1 < readTail - readOffset) {
      readTail = headerBuffer.length - 1;
    }

    boolean isLogFine = log.isLoggable(Level.FINE);

    while (true) {
      int ch;

      int keyOffset = headerOffset;

      // scan the key
      while (true) {
        if (readTail <= readOffset) {
          if ((readTail = fillHeaderTail(s, readOffset, headerOffset)) <= 0) {
            return;
          }

          readOffset = s.getOffset();
          headerOffset = _headerLength;
          headerBuffer = _headerBuffer;
        }

        ch = readBuffer[readOffset++];

        if (ch == '\n') {
          s.setOffset(readOffset);
          return;
        }
        else if (ch == ':') {
          break;
        }

        headerBuffer[headerOffset++] = (char) ch;
      }

      // strip trailing whitespace from key
      while (headerBuffer[headerOffset - 1] == ' ') {
        headerOffset--;
      }
      
      int keyLength = headerOffset - keyOffset;

      // skip whitespace
      do {
        if (readTail <= readOffset) {
          if ((readTail = fillHeaderTail(s, readOffset, headerOffset)) <= 0) {
            return;
          }

          readOffset = s.getOffset();
          headerOffset = _headerLength;
          headerBuffer = _headerBuffer;
        }
        
        ch = readBuffer[readOffset++];
      } while (ch == ' ' || ch == '\t');

      int valueOffset = headerOffset;

      // scan the value
      while (true) {
        if (readTail <= readOffset) {
          if ((readTail = fillHeaderTail(s, readOffset, headerOffset)) <= 0) {
            break;
          }

          readOffset = s.getOffset();
          headerOffset = _headerLength;
          headerBuffer = _headerBuffer;
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

      while (headerBuffer[headerOffset - 1] <= ' ') {
        headerOffset--;
      }
      
      int headerSize = _headerSize;
      
      CharSegment []headerKeys = _headerKeys;
      CharSegment []headerValues = _headerValues;

      if (headerKeys.length <= headerSize) {
        _headerLength = headerOffset;
        extendHeaderBuffers();
        
        headerBuffer = _headerBuffer;
        headerKeys = _headerKeys;
        headerValues = _headerValues;
      }

      headerKeys[headerSize].init(headerBuffer, keyOffset, keyLength);

      int valueLength = headerOffset - valueOffset;
      headerValues[headerSize].init(headerBuffer, valueOffset, valueLength);

      if (isLogFine) {
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
  
  private int fillUrlTail(ReadStream s, int readOffset,
                          int uriOffset)
    throws IOException
  {
    _uriLength = uriOffset;
    
    if (_uri.length <= uriOffset) {
      extendHeaderBuffers();
    }
    
    if (s.getLength() <= readOffset) {
      if (s.fillBuffer() < 0) {
        return -1;
      }
    }
    else {
      s.setOffset(readOffset);
    }
    
    int tail = s.getLength() - s.getOffset();
    
    if (_uri.length - uriOffset < tail) {
      tail = _uri.length - uriOffset;
    }
    
    return tail;
  }
  
  private int fillHeaderTail(ReadStream s, int readOffset,
                             int headerOffset)
    throws IOException
  {
    _headerLength = headerOffset;
    
    if (_headerBuffer.length <= headerOffset) {
      extendHeaderBuffers();
    }
    
    if (s.getLength() <= readOffset) {
      if (s.fillBuffer() < 0) {
        return -1;
      }
    }
    else {
      s.setOffset(readOffset);
    }
    
    int tail = s.getLength() - s.getOffset();
    
    if (_headerBuffer.length - headerOffset < tail) {
      tail = _headerBuffer.length - headerOffset;
    }
    
    return tail;
  }

  protected void extendHeaderBuffers()
    throws IOException
  {
    HttpBufferStore bufferStore = getHttpBufferStore();
    
    if (bufferStore != null) {
      throw new BadRequestException(L.l("URL or HTTP headers are too long (IP={0})",
                                        getRemoteAddr()));
    }
    
    bufferStore = allocateHttpBufferStore();
    
    byte []uri = bufferStore.getUriBuffer();
    System.arraycopy(_uri,  0, uri, 0, _uriLength);
    
    char []headerBuffer = bufferStore.getHeaderBuffer();
    CharSegment []headerKeys = bufferStore.getHeaderKeys();
    CharSegment []headerValues = bufferStore.getHeaderValues();
    
    if (headerBuffer == _headerBuffer || _uri == uri) {
      throw new IllegalStateException();
    }
    
    System.arraycopy(_headerBuffer, 0, headerBuffer, 0, _headerLength);
    
    for (int i = 0; i < _headerSize; i++) {
      headerKeys[i].init(headerBuffer,  
                         _headerKeys[i].getOffset(),
                         _headerKeys[i].getLength());
      
      headerValues[i].init(headerBuffer,  
                           _headerValues[i].getOffset(),
                           _headerValues[i].getLength());
    }
    
    _uri = uri;
    _headerBuffer = headerBuffer;
    _headerKeys = headerKeys;
    _headerValues = headerValues;
  }
  
  @Override
  public void onCloseConnection()
  {
    super.onCloseConnection();
    
    _uri = null;
    _headerBuffer = null;
    _headerKeys = null;
    _headerValues = null;
  }

  //
  // upgrade to duplex
  //


  /**
   * Upgrade to duplex
   */
  @Override
  public SocketLinkDuplexController startDuplex(SocketLinkDuplexListener handler)
  {
    TcpSocketLink conn = (TcpSocketLink) getConnection();

    SocketLinkDuplexController context = conn.startDuplex(handler);

    _rawInputStream.init(conn.getReadStream());
    getReadStream().setSource(_rawInputStream);
    
    return context;
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

  @Override
  protected String dbgId()
  {
    String serverId = getServer().getServerId();
    int connId = getConnectionId();

    if ("".equals(serverId))
      return "Http[" + connId + "] ";
    else
      return "Http[" + serverId + ", " + connId + "] ";
  }

  @Override
  public String toString()
  {
    String serverId = getServer().getServerId();
    int connId = getConnectionId();

    if ("".equals(serverId))
      return "HttpRequest[" + connId + "]";
    else {
      return ("HttpRequest[" + serverId + ", " + connId + "]");
    }
  }
  
  static {
    _toLowerAscii = new char[256];
    
    for (int i = 0; i < 256; i++) {
      if ('A' <= i && i <= 'Z') {
        _toLowerAscii[i] = (char) (i + 'a' - 'A');
      }
      else {
        _toLowerAscii[i] = (char) i;
      }
    }
    
    _toUpperAscii = new char[256];
    
    for (int i = 0; i < 256; i++) {
      if ('a' <= i && i <= 'z') {
        _toUpperAscii[i] = (char) (i + 'A' - 'a');
      }
      else {
        _toUpperAscii[i] = (char) i;
      }
    }
    
    _isHttpWhitespace = new boolean[256];
    _isHttpWhitespace[' '] = true;
    _isHttpWhitespace['\t'] = true;
    _isHttpWhitespace['\r'] = true;
    _isHttpWhitespace['\n'] = true;
  }
}
