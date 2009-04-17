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

package com.caucho.server.connection;

import com.caucho.config.scope.ScopeRemoveListener;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.security.AbstractLogin;
import com.caucho.security.Login;
import com.caucho.security.RoleMapManager;
import com.caucho.security.SecurityContext;
import com.caucho.security.SecurityContextProvider;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.port.TcpConnection;
import com.caucho.server.security.AbstractAuthenticator;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.util.*;
import com.caucho.vfs.BufferedReaderAdapter;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract request implementing methods common to the different
 * request implementations.
 */
public abstract class AbstractHttpRequest
  implements CauchoRequest, SecurityContextProvider
{
  protected static final Logger log
    = Logger.getLogger(AbstractHttpRequest.class.getName());

  static final L10N L = new L10N(AbstractHttpRequest.class);

  protected static final CaseInsensitiveIntMap _headerCodes;
  
  public static final String REQUEST_URI = "javax.servlet.include.request_uri";
  public static final String CONTEXT_PATH = "javax.servlet.include.context_path";
  public static final String SERVLET_PATH = "javax.servlet.include.servlet_path";
  public static final String PATH_INFO = "javax.servlet.include.path_info";
  public static final String QUERY_STRING = "javax.servlet.include.query_string";
  
  public static final String STATUS_CODE = "javax.servlet.error.status_code";
  public static final String EXCEPTION_TYPE = "javax.servlet.error.exception_type";
  public static final String MESSAGE = "javax.servlet.error.message";
  public static final String EXCEPTION = "javax.servlet.error.exception";
  public static final String ERROR_URI = "javax.servlet.error.request_uri";
  public static final String SERVLET_NAME = "javax.servlet.error.servlet_name";
  
  public static final String JSP_EXCEPTION = "javax.servlet.jsp.jspException";
  
  public static final String SHUTDOWN = "com.caucho.shutdown";
  
  private static final String CHAR_ENCODING = "resin.form.character.encoding";
  private static final String FORM_LOCALE = "resin.form.local";
  private static final String CAUCHO_CHAR_ENCODING = "caucho.form.character.encoding";

  private static final char []CONNECTION = "connection".toCharArray();
  private static final char []COOKIE = "cookie".toCharArray();
  private static final char []CONTENT_LENGTH = "content-length".toCharArray();
  private static final char []EXPECT = "expect".toCharArray();
  private static final char []HOST = "host".toCharArray();
  
  private static final char []CONTINUE_100 = "100-continue".toCharArray();
  private static final char []CLOSE = "close".toCharArray();

  private static final boolean []TOKEN;
  private static final boolean []VALUE;

  private static final ServletRequestAttributeListener []NULL_LISTENERS
    = new ServletRequestAttributeListener[0];

  private static final Cookie []NULL_COOKIES
    = new Cookie[0];
  
  protected final DispatchServer _server;
  
  protected final Connection _conn;
  protected final TcpConnection _tcpConn;

  protected AbstractHttpResponse _response;

  protected Invocation _invocation;

  private boolean _keepalive;
  private long _startTime;
  
  private String _runAs;
  private boolean _isLoginRequested;

  protected CharSegment _hostHeader;
  protected boolean _expect100Continue;
    
  private Cookie []_cookiesIn;
  private ArrayList<Cookie> _cookies = new ArrayList<Cookie>();
  
  // True if the page depends on cookies
  private boolean _varyCookies;
  // The cookie the page depends on
  private String _varyCookie;
  private boolean _hasCookie;
  private boolean _isSessionIdFromCookie;

  protected int _sessionGroup;

  private long _contentLength;

  private boolean _sessionIsLoaded;
  private SessionImpl _session;

  // Connection stream
  protected final ReadStream _rawRead;
  // Stream for reading post contents
  protected final ReadStream _readStream;

  // True if the post stream has been initialized
  protected boolean _hasReadStream;

  // Servlet input stream for post contents
  private final ServletInputStreamImpl _is = new ServletInputStreamImpl();

  // character incoding for a Post
  private String _readEncoding;
  // Reader for post contents
  private final BufferedReaderAdapter _bufferedReader;

  private boolean _hasReader;
  private boolean _hasInputStream;
  
  private ErrorPageManager _errorManager = new ErrorPageManager(null);

  // HttpServletRequest stuff
  private final Form _formParser = new Form();
  private final HashMapImpl<String,String[]> _form
    = new HashMapImpl<String,String[]>();
  private HashMapImpl<String,String[]> _filledForm;
  
  private final HashMapImpl<String,Object> _attributes
    = new HashMapImpl<String,Object>();

  private ArrayList<Locale> _locales = new ArrayList<Locale>();

  private ArrayList<Path> _closeOnExit = new ArrayList<Path>();

  // Efficient date class for printing date headers
  protected final QDate _calendar = new QDate();
  private final CharBuffer _cbName = new CharBuffer();
  private final CharBuffer _cbValue = new CharBuffer();
  protected final CharBuffer _cb = new CharBuffer();
  // private final ArrayList<CharSegment> _arrayList = new ArrayList<CharSegment>();

  private HttpBufferStore _httpBuffer;

  private ServletRequestAttributeListener []_attributeListeners;
  
  /**
   * Create a new Request.  Because the actual initialization occurs with
   * the start() method, this just allocates statics.
   *
   * @param server the parent server
   */
  protected AbstractHttpRequest(DispatchServer server, Connection conn)
  {
    _server = server;

    _conn = conn;
    if (conn != null)
      _rawRead = conn.getReadStream();
    else
      _rawRead = null;

    if (conn instanceof TcpConnection)
      _tcpConn = (TcpConnection) conn;
    else
      _tcpConn = null;

    _readStream = new ReadStream();
    _readStream.setReuseBuffer(true);

    _bufferedReader = new BufferedReaderAdapter(_readStream);
  }

  /**
   * Initialization.
   */
  public void init()
  {
  }

  /**
   * Returns the connection.
   */
  public final Connection getConnection()
  {
    return _conn;
  }

  /**
   * returns the dispatch server.
   */
  public final DispatchServer getDispatchServer()
  {
    return _server;
  }

  /**
   * Called when the connection starts
   */
  public void startConnection()
  {
  }
  
  /**
   * Prepare the Request object for a new request.
   *
   * @param s the raw connection stream
   */
  protected void startRequest(HttpBufferStore httpBuffer)
    throws IOException
  {
    _httpBuffer = httpBuffer;
    _invocation = null;

    _varyCookies = false;
    _varyCookie = null;
    _hasCookie = false;
    
    _hostHeader = null;
    _expect100Continue = false;
    
    _cookiesIn = null;
    _cookies.clear();

    _contentLength = -1;

    _sessionGroup = -1;
    _session = null;
    _sessionIsLoaded = false;
    
    _hasReadStream = false;
    _hasReader = false;
    _hasInputStream = false;

    _filledForm = null;
    _locales.clear();

    _readEncoding = null;

    _keepalive = true;
    _isSessionIdFromCookie = false;

    _runAs = null;
    _isLoginRequested = false;

    _attributeListeners = NULL_LISTENERS;

    if (_attributes.size() > 0)
      _attributes.clear();
  }

  /**
   * Returns true if a request has been set
   */
  public boolean hasRequest()
  {
    return false;
  }

  /**
   * Returns the http buffer store
   */
  final HttpBufferStore getHttpBufferStore()
  {
    return _httpBuffer;
  }

  /**
   * Returns true if client disconnects should be ignored.
   */
  public boolean isIgnoreClientDisconnect()
  {
    // server/183c

    Invocation invocation = _invocation;
    
    if (invocation == null)
      return true;
    else {
      WebApp webApp = invocation.getWebApp();

      if (webApp != null)
	return webApp.isIgnoreClientDisconnect();
      else
	return true;
    }
  }

  /**
   * Returns true if the client has disconnected
   */
  public boolean isClientDisconnect()
  {
    return _response.isClientDisconnect();
  }

  /**
   * Sets the client disconnect
   */
  public void clientDisconnect()
  {
    if (_tcpConn != null)
      _tcpConn.close();
  }

  public HttpServletRequest getRequestFacade()
  {
    return this;
  }

  /**
   * Returns the response for this request.
   */
  public CauchoResponse getResponse()
  {
    return _response;
  }

  /**
   * Returns the local server name.
   */
  public String getServerName()
  {
    String host = _conn.getVirtualHost();

    /*
    if (host == null && _invocation != null)
      host = _invocation.getHostName();
    */
    
    CharSequence rawHost;
    if (host == null && (rawHost = getHost()) != null) {
      if (rawHost instanceof CharSegment) {
	CharSegment cb = (CharSegment) rawHost;
	
	char []buffer = cb.getBuffer();
	int offset = cb.getOffset();
	int length = cb.getLength();

	for (int i = length - 1; i >= 0; i--) {
	  char ch = buffer[i + offset];

	  if ('A' <= ch && ch <= 'Z')
	    buffer[i + offset] = (char) (ch + 'a' - 'A');
	}
	
	host = new String(buffer, offset, length);
      }
      else
	return rawHost.toString().toLowerCase();
    }

    if (host == null) {
      InetAddress addr = _conn.getLocalAddress();
      return addr.getHostName();
    }

    int p1 = host.lastIndexOf('/');
    if (p1 < 0)
      p1 = 0;
    
    int p = host.lastIndexOf(':');
    if (p >= 0 && p1 < p)
      return host.substring(p1, p);
    else
      return host;
  }

  protected CharSequence getHost()
  {
    return null;
  }

  /**
   * Returns the server's port.
   */
  public int getServerPort()
  {
    String host = _conn.getVirtualHost();
    
    CharSequence rawHost;
    if (host == null && (rawHost = getHost()) != null) {
      int length = rawHost.length();
      int i;

      for (i = length - 1; i >= 0; i--) {
	if (rawHost.charAt(i) == ':') {
	  int port = 0;

	  for (i++; i < length; i++) {
	    char ch = rawHost.charAt(i);

	    if ('0' <= ch && ch <= '9')
	      port = 10 * port + ch - '0';
	  }

	  return port;
	}
      }
      
      return isSecure() ? 443 : 80;
    }

    if (host == null)
      return _conn.getLocalPort();

    int p1 = host.lastIndexOf(':');
    if (p1 < 0)
      return isSecure() ? 443 : 80;
    else {
      int length = host.length();
      int port = 0;

      for (int i = p1 + 1; i < length; i++) {
	char ch = host.charAt(i);

	if ('0' <= ch && ch <= '9')
	  port = 10 * port + ch - '0';
      }

      return port;
    }
  }

  /**
   * Returns the local port.
   */
  public int getLocalPort()
  {
    return _conn.getLocalPort();
  }

  /**
   * Returns the server's address.
   */
  public String getLocalAddr()
  {
    return _conn.getLocalAddress().getHostAddress();
  }

  /**
   * Returns the server's address.
   */
  public String getLocalName()
  {
    return _conn.getLocalAddress().getHostAddress();
  }

  public String getRemoteAddr()
  {
    return _conn.getRemoteHost();
  }

  public int printRemoteAddr(byte []buffer, int offset)
    throws IOException
  {
    int len = _conn.getRemoteAddress(buffer, offset, buffer.length - offset);

    return offset + len;
  }

  public String getRemoteHost()
  {
    return _conn.getRemoteHost();
  }

  /**
   * Returns the local port.
   */
  public int getRemotePort()
  {
    return _conn.getRemotePort();
  }

  /**
   * Returns the request's scheme.
   */
  public String getScheme()
  {
    return isSecure() ? "https" : "http";
  }

  abstract public String getProtocol();

  abstract public String getMethod();

  /**
   * Returns the URI for the request
   */
  public String getRequestURI() 
  {
    if (_invocation != null)
      return _invocation.getRawURI();
    else
      return "";
  }

  /**
   * Returns the URI for the page.  getPageURI and getRequestURI differ
   * for included files.  getPageURI gets the URI for the included page.
   * getRequestURI returns the original URI.
   */
  public String getPageURI()
  {
    return _invocation.getRawURI();
  }

  public abstract byte []getUriBuffer();

  public abstract int getUriLength();

  /**
   * Returns the context part of the uri.  The context part is the part
   * that maps to an webApp.
   */
  public String getContextPath() 
  {
    if (_invocation != null)
      return _invocation.getContextPath();
    else
      return "";
  }

  /**
   * Returns the context part of the uri.  For included files, this will
   * return the included context-path.
   */
  public String getPageContextPath() 
  {
    return getContextPath();
  }

  /**
   * Returns the portion of the uri mapped to the servlet for the original
   * request.
   */
  public String getServletPath()
  {
    if (_invocation != null)
      return _invocation.getServletPath();
    else
      return "";
  }

  /**
   * Returns the portion of the uri mapped to the servlet for the current
   * page.
   */
  public String getPageServletPath()
  {
    if (_invocation != null)
      return _invocation.getServletPath();
    else
      return "";
  }

  /**
   * Returns the portion of the uri after the servlet path for the original
   * request.
   */
  public String getPathInfo()
  {
    if (_invocation != null)
      return _invocation.getPathInfo();
    else
      return null;
  }

  /**
   * Returns the portion of the uri after the servlet path for the current
   * page.
   */
  public String getPagePathInfo()
  {
    if (_invocation != null)
      return _invocation.getPathInfo();
    else
      return null;
  }

  /**
   * Returns the URL for the request
   */
  public StringBuffer getRequestURL() 
  { 
    StringBuffer sb = new StringBuffer();

    sb.append(getScheme());
    sb.append("://");

    sb.append(getServerName());
    int port = getServerPort();
    
    if (port > 0 &&
	port != 80 &&
	port != 443) {
      sb.append(":");
      sb.append(port);
    }

    sb.append(getRequestURI());

    return sb;
  }

  /**
   * @deprecated As of JSDK 2.1
   */
  public String getRealPath(String path)
  {
    if (path == null)
      return null;
    if (path.length() > 0 && path.charAt(0) == '/')
      return _invocation.getWebApp().getRealPath(path);

    String uri = getPageURI();
    String context = getPageContextPath();
    if (context != null)
      uri = uri.substring(context.length());
    
    int p = uri.lastIndexOf('/');
    if (p >= 0)
      path = uri.substring(0, p + 1) + path;

    return _invocation.getWebApp().getRealPath(path);
  }

  /**
   * Returns the real path of pathInfo.
   */
  public String getPathTranslated()
  {
    String pathInfo = getPathInfo();

    if (pathInfo == null)
      return null;
    else
      return getRealPath(pathInfo);
  }

  /**
   * Returns the current page's query string.
   */
  public String getQueryString()
  {
    if (_invocation != null)
      return _invocation.getQueryString();
    else
      return null;
  }

  /**
   * Returns the current page's query string.
   */
  public String getPageQueryString()
  {
    return getQueryString();
  }

  /**
   * Returns the named header.
   *
   * @param key the header key
   */
  abstract public String getHeader(String key);

  /**
   * Returns the number of headers.
   */
  public int getHeaderSize()
  {
    return -1;
  }

  /**
   * Returns the header key
   */
  public CharSegment getHeaderKey(int index)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the header value
   */
  public CharSegment getHeaderValue(int index)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Fills the result with the header values as
   * CharSegment values.  Most implementations will
   * implement this directly.
   *
   * @param name the header name
   */
  public CharSegment getHeaderBuffer(String name)
  {
    String value = getHeader(name);
    
    if (value != null)
      return new CharBuffer(value);
    else
      return null;
  }

  /**
   * Enumerates the header keys
   */
  abstract public Enumeration getHeaderNames();

  /**
   * Sets the header.  setHeader is used for
   * Resin's caching to simulate If-None-Match.
   */
  public void setHeader(String key, String value)
  {
  }

  /**
   * Adds the header, checking for known values.
   */
  protected boolean addHeaderInt(char []keyBuf, int keyOff, int keyLen,
                                 CharSegment value)
  {
    if (keyLen < 4)
      return true;
    
    int key1 = keyBuf[keyOff];
    switch (key1) {
    case 'c':
    case 'C':
      if (keyLen == CONNECTION.length
	  && match(keyBuf, keyOff, keyLen, CONNECTION)) {
	if (match(value.getBuffer(), value.getOffset(), value.getLength(),
		  CLOSE)) {
	  connectionClose();
	}
      }
      else if (keyLen == COOKIE.length
	       && match(keyBuf, keyOff, keyLen, COOKIE)) {
	fillCookie(_cookies, value);
      }
      else if (keyLen == CONTENT_LENGTH.length
	       && match(keyBuf, keyOff, keyLen, CONTENT_LENGTH)) {
	setContentLength(value);
      }
      
      return true;
      
    case 'e':
    case 'E':
      if (match(keyBuf, keyOff, keyLen, EXPECT)) {
	if (match(value.getBuffer(), value.getOffset(), value.getLength(),
		  CONTINUE_100)) {
	  _expect100Continue = true;
          return false;
	}
      }
      
      return true;
      
    case 'h':
    case 'H':
      if (match(keyBuf, keyOff, keyLen, HOST)) {
	_hostHeader = value;
      }
      return true;
      
    default:
      return true;
    }
  }

  protected void setContentLength(CharSegment value)
  {
    int contentLength = 0;
    int ch;
    int i = 0;
    
    int length = value.length();
    for (;
	 i < length && (ch = value.charAt(i)) >= '0' && ch <= '9';
	 i++) {
      contentLength = 10 * contentLength + ch - '0';
    }

    if (i > 0)
      _contentLength = contentLength;
  }

  /**
   * Called for a connection: close
   */
  protected void connectionClose()
  {
    killKeepalive();
  }

  /**
   * Matches case insensitively, with the second normalized to lower case.
   */
  private boolean match(char []a, int aOff, int aLength, char []b)
  {
    int bLength = b.length;
    
    if (aLength != bLength)
      return false;

    for (int i = aLength - 1; i >= 0; i--) {
      char chA = a[aOff + i];
      char chB = b[i];

      if (chA != chB && chA + 'a' - 'A' != chB) {
	return false;
      }
    }

    return true;
  }

  /**
   * Returns an enumeration of the headers for the named attribute.
   *
   * @param name the header name
   */
  public Enumeration getHeaders(String name)
  {
    String value = getHeader(name);
    if (value == null)
      return NullEnumeration.create();

    ArrayList<String> list = new ArrayList<String>();
    list.add(value);

    return Collections.enumeration(list);
  }

  /**
   * Fills the result with a list of the header values as
   * CharSegment values.  Most implementations will
   * implement this directly.
   *
   * @param name the header name
   * @param resultList the resulting buffer
   */
  public void getHeaderBuffers(String name, ArrayList<CharSegment> resultList)
  {
    String value = getHeader(name);

    if (value != null)
      resultList.add(new CharBuffer(value));
  }

  /**
   * Returns the named header, converted to an integer.
   *
   * @param key the header key.
   *
   * @return the value of the header as an integer.
   */
  public int getIntHeader(String key)
  {
    CharSegment value = getHeaderBuffer(key);

    if (value == null)
      return -1;

    int len = value.length();
    if (len == 0)
      throw new NumberFormatException(value.toString());

    int iValue = 0;
    int i = 0;
    int ch = value.charAt(i);
    int sign = 1;
    if (ch == '+') {
      if (i + 1 < len)
	ch = value.charAt(++i);
      else
	throw new NumberFormatException(value.toString());
    } else if (ch == '-') {
      sign = -1;
      if (i + 1 < len)
	ch = value.charAt(++i);
      else
	throw new NumberFormatException(value.toString());
    }

    for (; i < len && (ch = value.charAt(i)) >= '0' && ch <= '9'; i++)
      iValue = 10 * iValue + ch - '0';

    if (i < len)
      throw new NumberFormatException(value.toString());

    return sign * iValue;
  }

  /**
   * Returns a header interpreted as a date.
   *
   * @param key the header key.
   *
   * @return the value of the header as an integer.
   */
  public long getDateHeader(String key)
  {
    String value = getHeader(key);
    if (value == null)
      return -1;

    long date = -1;
    try {
      date = _calendar.parseDate(value);

      if (date == Long.MAX_VALUE)
	throw new IllegalArgumentException("getDateHeader(" + value + ")");

      return date;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Returns the content length of a post.
   */
  public int getContentLength()
  {
    return (int) _contentLength;
  }

  /**
   * Returns the content length of a post.
   */
  public long getLongContentLength()
  {
    return _contentLength;
  }

  /**
   * Returns the content-length of a post.
   */
  public String getContentType()
  {
    return getHeader("Content-Type");
  }

  /**
   * Returns the content-length of a post.
   */
  public CharSegment getContentTypeBuffer()
  {
    return getHeaderBuffer("Content-Type");
  }

  /**
   * Returns the character encoding of a post.
   */
  public String getCharacterEncoding()
  {
    if (_readEncoding != null)
      return _readEncoding;
    
    CharSegment value = getHeaderBuffer("Content-Type");

    if (value == null)
      return null;

    int i = value.indexOf("charset");
    if (i < 0)
      return null;

    int len = value.length();
    for (i += 7; i < len && Character.isWhitespace(value.charAt(i)); i++) {
    }

    if (i >= len || value.charAt(i) != '=')
      return null;

    for (i++; i < len && Character.isWhitespace(value.charAt(i)); i++) {
    }

    if (i >= len)
      return null;

    char end = value.charAt(i);
    if (end == '"') {
      int tail;
      for (tail = ++i; tail < len; tail++) {
        if (value.charAt(tail) == end)
          break;
      }

      _readEncoding = Encoding.getMimeName(value.substring(i, tail));
      
      return _readEncoding;
    }

    int tail;
    for (tail = i; tail < len; tail++) {
      if (Character.isWhitespace(value.charAt(tail)) ||
	  value.charAt(tail) == ';')
	break;
    }

    _readEncoding = Encoding.getMimeName(value.substring(i, tail));
    
    return _readEncoding;
  }

  /**
   * Sets the character encoding of a post.
   */
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _readEncoding = encoding;
    
    try {
      // server/122d (tck)
      //if (_hasReadStream)
      
      _readStream.setEncoding(_readEncoding);
    } catch (UnsupportedEncodingException e) {
      throw e;
    } catch (java.nio.charset.UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(e.getMessage());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Returns the cookies from the browser
   */
  public Cookie []getCookies()
  {
    // The page varies depending on the presense of any cookies
    setVaryCookie(null);
    
    if (_cookiesIn == null)
      fillCookies();

    // If any cookies actually exist, the page is not anonymous
    if (_cookiesIn != null && _cookiesIn.length > 0)
      setHasCookie();

    if (_cookiesIn == null || _cookiesIn.length == 0)
      return null;
    else
      return _cookiesIn;
  }

  /**
   * Returns the named cookie from the browser
   */
  public Cookie getCookie(String name)
  {
    // The page varies depending on the presense of any cookies
    setVaryCookie(name);

    return findCookie(name);
  }

  private Cookie findCookie(String name)
  {
    if (_cookiesIn == null)
      fillCookies();

    if (_cookiesIn == null)
      return null;

    int length = _cookiesIn.length;
    for (int i = 0; i < length; i++) {
      Cookie cookie = _cookiesIn[i];
      
      if (cookie.getName().equals(name)) {
        setHasCookie();
        return cookie;
      }
    }

    return null;
  }

  /**
   * Parses cookie information from the cookie headers.
   */
  private void fillCookies()
  {
    int size = _cookies.size();

    if (size > 0) {
      _cookiesIn = new Cookie[_cookies.size()];
      _cookies.toArray(_cookiesIn);
    }
    else
      _cookiesIn = NULL_COOKIES;
  }

  /**
   * Parses a single cookie
   *
   * @param cookies the array of cookies read
   * @param rawCook the input for the cookie
   */
  private void fillCookie(ArrayList cookies, CharSegment rawCookie)
  {
    char []buf = rawCookie.getBuffer();
    int j = rawCookie.getOffset();
    int end = j + rawCookie.length();
    int version = 0;
    Cookie cookie = null;

    while (j < end) {
      char ch = 0;

      CharBuffer cbName = _cbName;
      CharBuffer cbValue = _cbValue;
      
      cbName.clear();
      cbValue.clear();

      for (;
	   j < end && ((ch = buf[j]) == ' ' || ch == ';' || ch ==',');
	   j++) {
      }

      if (end <= j)
        break;

      boolean isSpecial = false;
      if (buf[j] == '$') {
        isSpecial = true;
        j++;
      }

      for (; j < end; j++) {
	ch = buf[j];
	if (ch < 128 && TOKEN[ch])
	  cbName.append(ch);
	else
	  break;
      }

      for (; j < end && (ch = buf[j]) == ' '; j++) {
      }

      if (end <= j)
	break;
      else if (ch == ';' || ch == ',') {
        try {
          cookie = new Cookie(cbName.toString(), "");
          cookie.setVersion(version);
          _cookies.add(cookie);
          // some clients can send bogus cookies
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
        continue;
      }
      else if (ch != '=') {
        for (; j < end && (ch = buf[j]) != ';'; j++) {
        }
        continue;
      }

      j++;

      for (; j < end && (ch = buf[j]) == ' '; j++) {
      }

      if (ch == '"') {
        for (j++; j < end; j++) {
          ch = buf[j];
          if (ch == '"')
            break;
          cbValue.append(ch);
        }
        j++;
      }
      else {
        for (; j < end; j++) {
          ch = buf[j];
          if (ch < 128 && VALUE[ch])
	    cbValue.append(ch);
	  else
	    break;
        }
      }

      if (! isSpecial) {
        if (cbName.length() == 0)
          log.warning("bad cookie: " + rawCookie);
        else {
          cookie = new Cookie(cbName.toString(), cbValue.toString());
          cookie.setVersion(version);
          _cookies.add(cookie);
        }
      }
      else if (cookie == null) {
        if (cbName.matchesIgnoreCase("Version"))
          version = cbValue.charAt(0) - '0';
      }
      else if (cbName.matchesIgnoreCase("Version"))
        cookie.setVersion(cbValue.charAt(0) - '0');
      else if (cbName.matchesIgnoreCase("Domain"))
        cookie.setDomain(cbValue.toString());
      else if (cbName.matchesIgnoreCase("Path"))
        cookie.setPath(cbValue.toString());
    }
  }

  /**
   * Called if the page depends on a cookie.  If the cookie is null, then
   * the page depends on all cookies.
   *
   * @param cookie the cookie the page depends on.
   */
  public void setVaryCookie(String cookie)
  {
    if (_varyCookies == false)
      _varyCookie = cookie;
    else if (_varyCookie != null && ! _varyCookie.equals(cookie))
      _varyCookie = null;
    
    _varyCookies = true;

    // XXX: server/1315 vs 2671
    // _response.setPrivateOrResinCache(true);
  }
  
  /**
   * Returns true if the page depends on cookies.
   */
  public boolean getVaryCookies()
  {
    return _varyCookies;
  }
  
  /**
   * Returns the cookie the page depends on, or null if the page
   * depends on several cookies.
   */
  public String getVaryCookie()
  {
    return _varyCookie;
  }

  /**
   * Set when the page actually has a cookie.
   */
  public void setHasCookie()
  {
    _hasCookie = true;
    
    // XXX: 1171 vs 1240
    // _response.setPrivateOrResinCache(true);
  }

  /**
   * True if this page uses cookies.
   */
  public boolean getHasCookie()
  {
    if (_hasCookie)
      return true;
    else if (_invocation != null)
      return _invocation.getSessionId() != null;
    else
      return false;
  }

  /**
   * Returns the memory session.
   */
  public HttpSession getMemorySession()
  {
    if (_session != null && _session.isValid())
      return _session;
    else
      return null;
  }

  /**
   * Returns the current session, creating one if necessary.
   */
  public HttpSession getSession()
  {
    return getSession(true);
  }

  /**
   * Returns the current session.
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  public HttpSession getSession(boolean create)
  {
    if (_session != null) {
      if (_session.isValid())
	return _session;
    }
    else if (! create && _sessionIsLoaded)
      return null;

    _sessionIsLoaded = true;

    _session = createSession(create);
    
    return _session;
  }

  /**
   * Returns the current session.
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  public HttpSession getLoadedSession()
  {
    if (_session != null && _session.isValid())
      return _session;
    else
      return null;
  }

  /**
   * Returns true if the HTTP request's session id refers to a valid
   * session.
   */
  public boolean isRequestedSessionIdValid()
  {
    String id = getRequestedSessionId();

    if (id == null)
      return false;
    
    SessionImpl session = _session;

    if (session == null)
      session = (SessionImpl) getSession(false);

    return session != null && session.isValid() && session.getId().equals(id);
  }

  /**
   * Returns true if the current sessionId came from a cookie.
   */
  public boolean isRequestedSessionIdFromCookie()
  {
    return findSessionIdFromCookie() != null;
  }

  /**
   * Returns true if the current sessionId came from the url.
   */
  public boolean isRequestedSessionIdFromURL()
  {
    return findSessionIdFromUrl() != null;
  }

  /**
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl()
  {
    return isRequestedSessionIdFromURL();
  }

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  public String getRequestedSessionIdNoVary()
  {
    boolean varyCookies = _varyCookies;
    String varyCookie = _varyCookie;
    boolean hasCookie = _hasCookie;
    boolean privateCache = _response.getPrivateCache();
    
    String id = getRequestedSessionId();

    _varyCookies = varyCookies;
    _varyCookie = varyCookie;
    _hasCookie = hasCookie;
    _response.setPrivateOrResinCache(privateCache);

    return id;
  }

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  public String getRequestedSessionId()
  {
    SessionManager manager = getSessionManager();

    String cookieName = null;
    
    if (manager != null && manager.enableSessionCookies()) {
      setVaryCookie(getSessionCookie(manager));

      String id = findSessionIdFromCookie();

      if (id != null) {
	_isSessionIdFromCookie = true;
        setHasCookie();
        return id;
      }
    }

    String id = findSessionIdFromUrl();
    if (id != null) {
      return id;
    }

    if (manager != null && manager.enableSessionCookies())
      return null;
    else
      return findSessionIdFromConnection();
  }

  /**
   * For SSL connections, use the SSL identifier.
   */
  public String findSessionIdFromConnection()
  {
    return null;
  }

  /**
   * Returns the session id in the HTTP request cookies.
   * Because the webApp might use the cookie to change
   * the page contents, the caching sets vary: JSESSIONID.
   */
  private String findSessionIdFromCookie()
  {
    SessionManager manager = getSessionManager();
    
    if (manager == null || ! manager.enableSessionCookies())
      return null;

    Cookie cookie = findCookie(getSessionCookie(manager));

    if (cookie != null) {
      _isSessionIdFromCookie = true;
      return cookie.getValue();
    }
    else
      return null;
  }

  /**
   * Returns the session id in the HTTP request from the url.
   */
  private String findSessionIdFromUrl()
  {
    // server/1319
    // setVaryCookie(getSessionCookie(manager));

    String id = _invocation != null ? _invocation.getSessionId() : null;
    if (id != null)
      setHasCookie();

    return id;
  }

  public int getSessionGroup()
  {
    return _sessionGroup;
  }

  /**
   * Returns the current session.
   *
   * XXX: duplicated in RequestAdapter
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  private SessionImpl createSession(boolean create)
  {
    SessionManager manager = getSessionManager();

    if (manager == null)
      return null;

    String id = getRequestedSessionId();

    long now = Alarm.getCurrentTime();

    SessionImpl session
      = manager.createSession(create, this, id, now, _isSessionIdFromCookie);

    if (session != null
	&& (id == null || ! session.getId().equals(id))
	&& manager.enableSessionCookies()) {
      getResponse().setSessionId(session.getId());
    }
        
    return session;

    /*
    if (id != null && id.length() > 6) {
      // server/01t0
      session = manager.getSession(id, now, false, _isSessionIdFromCookie);

      if (session == null) {
      }
      else if (session.isValid()) {
        if (session != null) {
	  setVaryCookie(getSessionCookie(manager));
          setHasCookie();
	}
	
        if (! session.getId().equals(id) && manager.enableSessionCookies())
          getResponse().setSessionId(session.getId());
        
        return session;
      }
    }
    else
      id = null;

    if (! create)
      return null;

    // Must accept old ids because different webApps in the same
    // server must share the same cookie
    //
    // But, if the session group doesn't match, then create a new
    // session.

    session = manager.createSession(id, now, this, _isSessionIdFromCookie);

    if (session != null)
      setHasCookie();
      
    if (session.getId().equals(id))
      return session;

    if (manager.enableSessionCookies())
      getResponse().setSessionId(session.getId());

    return session;
    */
  }

  /**
   * Returns the session manager.
   */
  protected final SessionManager getSessionManager()
  {
    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.getSessionManager();
    else
      return null;
  }
  
  /**
   * Returns the session cookie.
   */
  protected final String getSessionCookie(SessionManager manager)
  {
    if (isSecure())
      return manager.getSSLCookieName();
    else
      return manager.getCookieName();
  }

  /**
   * Gets the authorization type
   */
  public String getAuthType()
  {
    Object login = getAttribute(AbstractLogin.LOGIN_NAME);

    if (login instanceof X509Certificate)
      return CLIENT_CERT_AUTH;
    
    WebApp app = getWebApp();

    if (app != null && app.getLogin() != null && getUserPrincipal() != null)
      return app.getLogin().getAuthType();
    else
      return null;
  }

  /**
   * Returns the login for the request.
   */
  protected Login getLogin()
  {
    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.getLogin();
    else
      return null;
  }

  /**
   * Internal logging return to get the remote user.  If the request already
   * knows the user, get it, otherwise just return null.
   */
  public String getRemoteUser(boolean create)
  {
    if (_session == null)
      return null;

    Principal user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

    if (user == null && create)
      user = getUserPrincipal();

    if (user != null)
      return user.getName();
    else
      return null;
  }

  /**
   * Returns true if any authentication is requested
   */
  public boolean isLoginRequested()
  {
    return _isLoginRequested;
  }

  /**
   * Authenticate the user.
   */
  public boolean login(boolean isFail)
  {
    try {
      /*
      Principal user = null;
      user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

      if (user != null)
	return true;
      */

      WebApp app = getWebApp();
      if (app == null) {
	if (log.isLoggable(Level.FINE))
	  log.finer("authentication failed, no web-app found");
      
	_response.sendError(HttpServletResponse.SC_FORBIDDEN);
	
	return false;
      }

      // If the authenticator can find the user, return it.
      Login login = app.getLogin();

      if (login != null) {
	Principal user = login.login(getRequestFacade(),
				     getResponse(),
				     isFail);

	return user != null;
	/*
	if (user == null)
	  return false;
	
	setAttribute(AbstractLogin.LOGIN_NAME, user);

	return true;
	*/
      }
      else if (isFail) {
	if (log.isLoggable(Level.FINE))
	  log.finer("authentication failed, no login module found for "
		    + app);

	Thread.dumpStack();
      
	_response.sendError(HttpServletResponse.SC_FORBIDDEN);
	
	return false;
      }
      else {
	// if a non-failure, then missing login is fine
	
	return false;
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  /**
   * Gets the remote user from the authorization type
   */
  public String getRemoteUser()
  {
    Principal principal = getUserPrincipal();

    if (principal != null)
      return principal.getName();
    else
      return null;
  }
  
  /**
   * Returns the Principal representing the logged in user.
   */
  public Principal getUserPrincipal()
  {
    _isLoginRequested = true;
    
    Principal user;
    user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

    if (user != null)
      return user;

    WebApp app = getWebApp();
    if (app == null)
      return null;
    
    // If the authenticator can find the user, return it.
    Login login = app.getLogin();

    if (login != null) {
      user = login.getUserPrincipal(getRequestFacade());

      if (user != null) {
	_response.setPrivateCache(true);
      }
      else {
	// server/123h, server/1920
	// distinguishes between setPrivateCache and setPrivateOrResinCache
	// _response.setPrivateOrResinCache(true);
      }
    }

    return user;
  }

  /**
   * Logs out the principal.
   */
  public void logout()
  {
    Login login = getLogin();

    if (login != null) {
      login.logout(getUserPrincipal(), this, getResponse());
    }
  }

  /**
   * Clear the principal from the request object.
   */
  public void logoutUserPrincipal()
  {
    // XXX:
    /*
    if (_session != null)
      _session.logout();
    */
  }
  
  /**
   * Sets the overriding role.
   */
  public String runAs(String role)
  {
    String oldRunAs = _runAs;

    _runAs = role;

    return oldRunAs;
  }
  
  /**
   * Returns true if the user represented by the current request
   * plays the named role.
   *
   * @param role the named role to test.
   * @return true if the user plays the role.
   */
  public boolean isUserInRole(String role)
  {
    HashMap<String,String> roleMap = _invocation.getSecurityRoleMap();
    
    if (roleMap != null) {
      String linkRole = roleMap.get(role);
      
      if (linkRole != null)
	role = linkRole;
    }

    if (_runAs != null)
      return _runAs.equals(role);
    
    WebApp app = getWebApp();
    
    Principal user = getUserPrincipal();

    if (user == null) {
      if (log.isLoggable(Level.FINE))
        log.fine("no user for isUserInRole");
    
      return false;
    }

    RoleMapManager roleManager = app != null ? app.getRoleMapManager() : null;

    if (roleManager != null) {
      Boolean result = roleManager.isUserInRole(role, user);

      if (result != null) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " userInRole(" + role + ")->" + result);
	
	return result;
      }
    }
    
    Login login = app == null ? null : app.getLogin();

    boolean inRole = login.isUserInRole(user, role);
      
    if (log.isLoggable(Level.FINE)) {
      if (user == null)
        log.fine("no user for isUserInRole");
      else if (inRole)
        log.fine(user + " is in role: " + role);
      else
        log.fine("failed " + user + " in role: " + role);
    }

    return inRole;
  }

  /**
   * Returns true if the transport is secure.
   */
  public boolean isTransportSecure()
  {
    return _conn.isSecure();
  }

  /**
   * Returns the requests underlying read stream, e.g. the post stream.
   */
  public ReadStream getStream()
    throws IOException
  {
    return getStream(true);
  }

  /**
   * Returns the requests underlying read stream, e.g. the post stream.
   */
  public ReadStream getStream(boolean isReader)
    throws IOException
  {
    if (! _hasReadStream) {
      _hasReadStream = true;
      
      initStream(_readStream, _rawRead);

      if (isReader) {
	// Encoding is based on getCharacterEncoding.
	// getReader needs the encoding.
	String charEncoding = getCharacterEncoding();
	String javaEncoding = Encoding.getJavaName(charEncoding);
	_readStream.setEncoding(javaEncoding);
      }

      if (_expect100Continue) {
	_expect100Continue = false;
	_response.writeContinue();
      }
    }

    return _readStream;
  }

  /**
   * Returns the raw read buffer.
   */
  public byte []getRawReadBuffer()
  {
    return _rawRead.getBuffer();
  }

  protected void skip()
    throws IOException
  {
    if (! _hasReadStream) {
      if (! initStream(_readStream, _rawRead))
        return;
      
      _hasReadStream = true;
    }

    while ((_readStream.skip(8192) > 0)) {
    }
  }

  /**
   * Initialize the read stream from the raw stream.
   */
  abstract protected boolean initStream(ReadStream readStream,
                                        ReadStream rawStream)
    throws IOException;

  /**
   * Returns the raw input stream.
   */
  public ReadStream getRawInput()
  {
    throw new UnsupportedOperationException(L.l("raw mode is not supported in this configuration"));
  }

  /**
   * Returns a stream for reading POST data.
   */
  public ServletInputStream getInputStream()
    throws IOException
  {
    if (_hasReader)
      throw new IllegalStateException(L.l("getInputStream() can't be called after getReader()"));

    _hasInputStream = true;

    ReadStream stream = getStream(false);

    _is.init(stream);

    return _is;
  }
  
  /**
   * Returns a Reader for the POST contents
   */
  public BufferedReader getReader()
    throws IOException
  {
    if (_hasInputStream)
      throw new IllegalStateException(L.l("getReader() can't be called after getInputStream()"));

    _hasReader = true;

    try {
      // bufferedReader is just an adapter to get the signature right.
      _bufferedReader.init(getStream(true));

      return _bufferedReader;
    } catch (java.nio.charset.UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(e.getMessage());
    }
  }

  /**
   * Returns an enumeration of the form names.
   */
  public Enumeration<String> getParameterNames()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  public Map<String,String[]> getParameterMap()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.unmodifiableMap(_filledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  public String []getParameterValues(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return (String []) _filledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  public String getParameter(String name)
  {
    String []values = getParameterValues(name);
    
    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  /**
   * Parses the query, either from the GET or the post.
   *
   * <p/>The character encoding is somewhat tricky.  If it's a post, then
   * assume the encoded form uses the same encoding as
   * getCharacterEncoding().
   *
   * <p/>If the request doesn't provide the encoding, use the 
   * character-encoding parameter from the webApp.
   *
   * <p/>Otherwise use the default system encoding.
   */
  private HashMapImpl<String,String[]> parseQuery()
  {
    try {
      _form.clear();
      
      String query = getQueryString();
      CharSegment contentType = getContentTypeBuffer();

      if (query == null && contentType == null)
        return _form;
      
      String charEncoding = getCharacterEncoding();
      if (charEncoding == null)
	charEncoding = (String) getAttribute(CAUCHO_CHAR_ENCODING);
      if (charEncoding == null)
	charEncoding = (String) getAttribute(CHAR_ENCODING);
      if (charEncoding == null) {
        Locale locale = (Locale) getAttribute(FORM_LOCALE);
        if (locale != null)
          charEncoding = Encoding.getMimeName(locale);
      }

      if (query != null) {
	String queryEncoding = charEncoding;
	
	if (queryEncoding == null && _server != null)
	  queryEncoding = _server.getURLCharacterEncoding();
      
	if (queryEncoding == null)
	  queryEncoding = CharacterEncoding.getLocalEncoding();

	String javaEncoding = Encoding.getJavaName(queryEncoding);
	
	_formParser.parseQueryString(_form, query, javaEncoding, true);
      }
      
      if (charEncoding == null)
	charEncoding = CharacterEncoding.getLocalEncoding();
      
      String javaEncoding = Encoding.getJavaName(charEncoding);

      if (contentType == null || ! "POST".equalsIgnoreCase(getMethod())) {
      }
      
      else if (contentType.startsWith("application/x-www-form-urlencoded")) {
 	_formParser.parsePostData(_form, getInputStream(), javaEncoding);
      }

      else if (getWebApp().doMultipartForm()
	       && contentType.startsWith("multipart/form-data")) {
        int length = contentType.length();
        int i = contentType.indexOf("boundary=");

        if (i < 0)
          return _form;

        long formUploadMax = getWebApp().getFormUploadMax();

	Object uploadMax = getAttribute("caucho.multipart.form.upload-max");
	if (uploadMax instanceof Number)
	  formUploadMax = ((Number) uploadMax).longValue();

        // XXX: should this be an error?
        if (formUploadMax >= 0 && formUploadMax < getLongContentLength()) {
          setAttribute("caucho.multipart.form.error",
                       L.l("Multipart form upload of '{0}' bytes was too large.",
                           String.valueOf(getLongContentLength())));
          setAttribute("caucho.multipart.form.error.size",
		       new Long(getLongContentLength()));

          return _form;
        }

        i += "boundary=".length();
        char ch = contentType.charAt(i);
        CharBuffer boundary = new CharBuffer();
        if (ch == '\'') {
          for (i++; i < length && contentType.charAt(i) != '\''; i++)
            boundary.append(contentType.charAt(i));
        }
        else if (ch == '\"') {
          for (i++; i < length && contentType.charAt(i) != '\"'; i++)
            boundary.append(contentType.charAt(i));
        }
        else {
          for (;
               i < length && (ch = contentType.charAt(i)) != ' ' &&
                 ch != ';';
               i++) {
            boundary.append(ch);
          }
        }

        try {
          MultipartForm.parsePostData(_form,
                                      getStream(false), boundary.toString(),
                                      this,
                                      javaEncoding,
                                      formUploadMax);
        } catch (IOException e) {
          log.log(Level.FINE, e.toString(), e);
          setAttribute("caucho.multipart.form.error", e.getMessage());
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return _form;
  }

  // request attributes

  /**
   * Returns an enumeration of the request attribute names.
   */
  public Enumeration<String> getAttributeNames()
  {
    return Collections.enumeration(_attributes.keySet());
  }

  /**
   * Returns the value of the named request attribute.
   *
   * @param name the attribute name.
   *
   * @return the attribute value.
   */
  public Object getAttribute(String name)
  {
    return _attributes.get(name);
  }

  /**
   * Sets the value of the named request attribute.
   *
   * @param name the attribute name.
   * @param value the new attribute value.
   */
  public void setAttribute(String name, Object value)
  {
    setAttribute(this, name, value);
  }

  protected void setAttribute(ServletRequest request,
			      String name,
			      Object value)
  {
    if (value != null) {
      Object oldValue = _attributes.put(name, value);
      
      for (int i = 0; i < _attributeListeners.length; i++) {
	ServletRequestAttributeEvent event;

	if (oldValue != null) {
	  event = new ServletRequestAttributeEvent(getWebApp(), request,
						   name, oldValue);

	  _attributeListeners[i].attributeReplaced(event);
	}
	else {
	  event = new ServletRequestAttributeEvent(getWebApp(), request,
						   name, value);

	  _attributeListeners[i].attributeAdded(event);
	}
      }
    }
    else
      removeAttribute(request, name);
  }

  /**
   * Removes the value of the named request attribute.
   *
   * @param name the attribute name.
   */
  public void removeAttribute(String name)
  {
    removeAttribute(this, name);
  }

  /**
   * Removes the value of the named request attribute.
   *
   * @param name the attribute name.
   */
  protected void removeAttribute(ServletRequest request,
				 String name)
  {
    Object oldValue = _attributes.remove(name);
    
    for (int i = 0; i < _attributeListeners.length; i++) {
      ServletRequestAttributeEvent event;

      event = new ServletRequestAttributeEvent(getWebApp(), request,
					       name, oldValue);

      _attributeListeners[i].attributeRemoved(event);
    }

    if (oldValue instanceof ScopeRemoveListener) {
      ((ScopeRemoveListener) oldValue).removeEvent(this, name);
    }
  }

  /**
   * Returns a request dispatcher relative to the current request.
   *
   * @param path the relative uri to the new servlet.
   */
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path == null || path.length() == 0)
      return null;
    else if (path.charAt(0) == '/')
      return getWebApp().getRequestDispatcher(path);
    else {
      CharBuffer cb = new CharBuffer();
      
      ServletContext app = getWebApp();

      String servletPath = getPageServletPath();
      if (servletPath != null)
        cb.append(servletPath);
      String pathInfo = getPagePathInfo();
      if (pathInfo != null)
        cb.append(pathInfo);
      
      int p = cb.lastIndexOf('/');
      if (p >= 0)
	cb.setLength(p);
      cb.append('/');
      cb.append(path);

      if (app != null)
	return app.getRequestDispatcher(cb.toString());

      return app.getRequestDispatcher(cb.toString());
    }
  }
  
  /*
   * jsdk 2.2
   */

  public Locale getLocale()
  {
    fillLocales();

    return _locales.get(0);
  }

  public Enumeration<Locale> getLocales()
  {
    fillLocales();

    return Collections.enumeration(_locales);
  }

  /**
   * Fill the locale array from the request's headers.
   */
  private void fillLocales()
  {
    if (_locales.size() > 0)
      return;
    
    Enumeration headers = getHeaders("Accept-Language");
    if (headers == null) {
      _locales.add(Locale.getDefault());
      return;
    }

    CharBuffer cb = _cb;
    while (headers.hasMoreElements()) {
      String header = (String) headers.nextElement();
      StringCharCursor cursor = new StringCharCursor(header);

      while (cursor.current() != cursor.DONE) {
	char ch;
	for (; Character.isWhitespace(cursor.current()); cursor.next()) {
	}

	cb.clear();
	for (; (ch = cursor.current()) >= 'a' && ch <= 'z' ||
	       ch >= 'A' && ch <= 'Z' ||
	       ch >= '0' && ch <= '0';
	     cursor.next()) {
	  cb.append(cursor.current());
	}

	String language = cb.toString();
	String country = "";
	String var = "";

	if (cursor.current() == '_' || cursor.current() == '-') {
	  cb.clear();
	  for (cursor.next();
	       (ch = cursor.current()) >= 'a' && ch <= 'z' ||
	       ch >= 'A' && ch <= 'Z' ||
	       ch >= '0' && ch <= '9';
	       cursor.next()) {
	    cb.append(cursor.current());
	  }
	  country = cb.toString();
	}

	if (language.length() > 0) {
	  Locale locale = new Locale(language, country);
	  _locales.add(locale);
	}

	for (;
	     cursor.current() != cursor.DONE && cursor.current() != ',';
	     cursor.next()) {
	}
	cursor.next();
      }
    }

    if (_locales.size() == 0)
      _locales.add(Locale.getDefault());
  }

  /**
   * Returns true if the request is secure.
   */
  public boolean isSecure()
  {
    return _conn.isSecure();
  }

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  public ServletContext getServletContext()
  {
    Invocation invocation = _invocation;

    if (invocation != null)
      return invocation.getWebApp();
    else
      return null;
  }

  /**
   * Returns the servlet response for the request
   *
   * @since Servlet 3.0
   */
  public ServletResponse getServletResponse()
  {
    return getResponse();
  }

  /**
   * Suspend the request
   *
   * @since Servlet 3.0
   */
  public void suspend(long timeout)
  {
  }

  /**
   * Suspend the request
   *
   * @since Servlet 3.0
   */
  public void suspend()
  {
  }

  /**
   * Resume the request
   *
   * @since Servlet 3.0
   */
  public void resume()
  {
  }

  /**
   * Complete the request
   *
   * @since Servlet 3.0
   */
  public void complete()
  {
  }

  /**
   * Returns true if the servlet is suspended
   *
   * @since Servlet 3.0
   */
  public boolean isSuspended()
  {
    return false;
  }

  /**
   * Returns true if the servlet is resumed
   *
   * @since Servlet 3.0
   */
  public boolean isResumed()
  {
    return false;
  }

  /**
   * Returns true if the servlet timed out
   *
   * @since Servlet 3.0
   */
  public boolean isTimeout()
  {
    return false;
  }

  /**
   * Returns true for the initial dispatch
   *
   * @since Servlet 3.0
   */
  public boolean isInitial()
  {
    return true;
  }

  //
  // internal goodies
  //

  /**
   * Returns the request's invocation.
   */
  public final Invocation getInvocation()
  {
    return _invocation;
  }

  /**
   * Sets the request's invocation.
   */
  public final void setInvocation(Invocation invocation)
  {
    _invocation = invocation;

    if (invocation != null) {
      // server/2m05
      
      WebApp app = invocation.getWebApp();
      if (app != null)
	_attributeListeners = app.getRequestAttributeListeners();
    }
  }

  public AbstractHttpRequest getAbstractHttpRequest()
  {
    return this;
  }

  /**
   * Sets the start time to the current time.
   */
  protected final void setStartTime()
  {
    if (_tcpConn != null)
      _tcpConn.beginActive();
    else
      _startTime = Alarm.getCurrentTime();
  }
  
  /**
   * Returns the date for the current request.
   */
  public final long getStartTime()
  {
    if (_tcpConn != null)
      return _tcpConn.getRequestStartTime();
    else
      return _startTime;
  }

  /**
   * Returns the servlet name.
   */
  public String getServletName()
  {
    if (_invocation != null) {
      return _invocation.getServletName();
    }
    else
      return null;
  }

  /**
   * Returns the invocation's webApp.
   */
  public final WebApp getWebApp()
  {
    if (_invocation != null)
      return _invocation.getWebApp();
    else
      return null;
  }

  /**
   * Returns the log buffer.
   */
  public final byte []getLogBuffer()
  {
    return _httpBuffer.getLogBuffer();
  }

  /**
   * Returns true for the top-level request, but false for any include()
   * or forward()
   */
  public boolean isTop()
  {
    return false;
  }

  /**
   * Returns the default error manager
   */
  protected ErrorPageManager getErrorManager()
  {
    Server server = (Server) _server;

    WebApp webApp = server.getWebApp("error.resin", 80, "/");

    if (webApp != null)
      return webApp.getErrorPageManager();
    else
      return _errorManager;
  }

  /**
   * Adds a file to be removed at the end.
   */
  public void addCloseOnExit(Path path)
  {
    _closeOnExit.add(path);
  }

  /**
   * Returns the depth of the request calls.
   */
  public int getRequestDepth(int depth)
  {
    return depth + 1;
  }

  public int getRequestDepth()
  {
    return 0;
  }
  
  /**
   * Handles a comet-style resume.
   *
   * @return true if the connection should stay open (keepalive)
   */
  public boolean handleResume()
    throws IOException
  {
    return false;
  }

  /**
   * Kills the keepalive.
   */
  public void killKeepalive()
  {
    _keepalive = false;

    /*
    ConnectionController controller = _conn.getController();
    if (controller != null)
      controller.close();
    */
  }

  /**
   * Returns true if the keepalive is active.
   */
  protected boolean isKeepalive()
  {
    return _keepalive;
  }

  public boolean isComet()
  {
    return _tcpConn != null && _tcpConn.isComet();
  }

  public boolean isSuspend()
  {
    return _tcpConn != null && (_tcpConn.isSuspend() || _tcpConn.isDuplex());
  }

  public boolean isDuplex()
  {
    return _tcpConn != null && _tcpConn.isDuplex();
  }

  /**
   * Returns true if keepalives are allowed.
   *
   * This method should only be called once, when the response is
   * deciding whether to send the Connection: close (or 'Q' vs 'X'),
   * after that, the calling routines should call isKeepalive() to
   * see what the decision was.
   *
   * Otherwise, the browser might see a keepalive when the final decision
   * is to close the connection.
   */
  public boolean allowKeepalive()
  {
    if (! _keepalive)
      return false;

    TcpConnection tcpConn = _tcpConn;
    
    if (tcpConn == null)
      return true;

    if (! tcpConn.toKeepalive())
      _keepalive = false;

    return _keepalive;
  }

  //
  // servlet 3.0
  //

  /**
   * Adds an async listener for this request
   *
   * @since Servlet 3.0
   */
  public void addAsyncListener(AsyncListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds an async listener for this request
   *
   * @since Servlet 3.0
   */
  public void addAsyncListener(AsyncListener listener,
			       ServletRequest request,
			       ServletResponse response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  public AsyncContext getAsyncContext()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncStarted()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncSupported()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the async timeout
   *
   * @since Servlet 3.0
   */
  public void setAsyncTimeout(long timeout)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync(ServletRequest request,
				 ServletResponse response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Restarts the server.
   */
  protected void restartServer()
    throws IOException, ServletException
  {
    HttpServletResponse res = (HttpServletResponse) getResponse();

    res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

    _server.update();
  }

  void saveSession()
  {
    SessionImpl session = _session;
    if (session != null)
      session.save();
  }

  /**
   * Prepare the Request object for a new request.
   *
   * @param s the raw connection stream
   */
  protected void startInvocation()
    throws IOException
  {
    if (_tcpConn != null)
      _tcpConn.beginActive();
    else
      _startTime = Alarm.getCurrentTime();
  }

  /**
   * Cleans up at the end of the invocation
   */
  public void finishInvocation()
  {
    try {
      _response.finishInvocation();
    } catch (IOException e) {
      log.finer(e.toString());
    }
  }
	   
  /**
   * Cleans up at the end of the request
   */
  public void finishRequest()
    throws IOException
  {
    try {
      // server/0219, but must be freed for GC
      _invocation = null;
      
      _response.finishRequest();

      SessionImpl session = _session;

      if (session != null)
        session.finishRequest();
      
      cleanup();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      for (int i = _closeOnExit.size() - 1; i >= 0; i--) {
        Path path = _closeOnExit.get(i);

        try {
          path.remove();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
      _closeOnExit.clear();

      if (_tcpConn != null) {
	_tcpConn.endActive();

	_tcpConn.finishRequest();
      }
    }
  }

  public void cleanup()
  {
    _session = null;
      
    if (_attributes.size() > 0) {
      for (Map.Entry<String,Object> entry : _attributes.entrySet()) {
	Object value = entry.getValue();

	if (value instanceof ScopeRemoveListener) {
	  ((ScopeRemoveListener) value).removeEvent(this, entry.getKey());
	}
      }
	
      _attributes.clear();
    }

    if (_form != null)
      _form.clear();
    _filledForm = null;
    _cookiesIn = null;
    _cookies.clear();

    HttpBufferStore httpBuffer = _httpBuffer;
    _httpBuffer = null;

    if (httpBuffer != null)
      HttpBufferStore.free(httpBuffer);
  }

  /**
   * Called by server shutdown to kill any active threads
   */
  public void shutdown()
  {
  }

  protected String dbgId()
  {
    return "Tcp[" + _conn.getId() + "] ";
  }

  static {
    _headerCodes = new CaseInsensitiveIntMap();

    TOKEN = new boolean[256];
    VALUE = new boolean[256];

    for (int i = 0; i < 256; i++) {
      TOKEN[i] = true;
    }

    for (int i = 0; i < 32; i++) {
      TOKEN[i] = false;
    }

    for (int i = 127; i < 256; i++) {
      TOKEN[i] = false;
    }

    //TOKEN['('] = false;
    //TOKEN[')'] = false;
    //TOKEN['<'] = false;
    //TOKEN['>'] = false;
    //TOKEN['@'] = false;
    TOKEN[','] = false;
    TOKEN[';'] = false;
    //TOKEN[':'] = false;
    TOKEN['\\'] = false;
    TOKEN['"'] = false;
    //TOKEN['/'] = false;
    //TOKEN['['] = false;
    //TOKEN[']'] = false;
    //TOKEN['?'] = false;
    TOKEN['='] = false;
    //TOKEN['{'] = false;
    //TOKEN['}'] = false;
    TOKEN[' '] = false;

    System.arraycopy(TOKEN, 0, VALUE, 0, TOKEN.length);

    VALUE['='] = true;
  }
}
