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

import com.caucho.server.cache.AbstractCacheEntry;
import com.caucho.server.cache.AbstractCacheFilterChain;
import com.caucho.server.dispatch.BadRequestException;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.session.CookieImpl;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.CaseInsensitiveIntMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.HTTPUtil;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.FlushBuffer;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.XmlChar;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the servlet response, controlling response headers and the
 * response stream.
 */
abstract public class AbstractHttpResponse implements CauchoResponse {
  static final protected Logger log
    = Logger.getLogger(AbstractHttpResponse.class.getName());
  static final L10N L = new L10N(AbstractHttpResponse.class);

  static final HashMap<String,String> _errors;

  protected static final CaseInsensitiveIntMap _headerCodes;
  protected static final int HEADER_CACHE_CONTROL = 1;
  protected static final int HEADER_CONTENT_TYPE = HEADER_CACHE_CONTROL + 1;
  protected static final int HEADER_CONTENT_LENGTH = HEADER_CONTENT_TYPE + 1;
  protected static final int HEADER_DATE = HEADER_CONTENT_LENGTH + 1;
  protected static final int HEADER_SERVER = HEADER_DATE + 1;
  protected static final int HEADER_CONNECTION = HEADER_SERVER + 1;

  protected CauchoRequest _originalRequest;
  protected CauchoRequest _request;

  protected int _statusCode;
  protected String _statusMessage;

  protected String _contentType;
  protected String _contentPrefix;
  protected String _charEncoding;
  protected boolean _hasCharEncoding;
  
  protected final ArrayList<String> _headerKeys = new ArrayList<String>();
  protected final ArrayList<String> _headerValues = new ArrayList<String>();
  
  protected final ArrayList<String> _footerKeys = new ArrayList<String>();
  protected final ArrayList<String> _footerValues = new ArrayList<String>();

  protected final ArrayList<Cookie> _cookiesOut = new ArrayList<Cookie>();

  private final AbstractResponseStream _originalResponseStream;
  
  private final ServletOutputStreamImpl _responseOutputStream;
  private final ResponseWriter _responsePrintWriter;

  private AbstractResponseStream _responseStream;

  // the raw output stream.
  protected WriteStream _rawWrite;

  // any stream that needs flusing before getting the writer.
  private FlushBuffer _flushBuffer;

  private boolean _isHeaderWritten;
  private boolean _isChunked;
  private boolean _isClientDisconnect;
  protected final QDate _calendar = new QDate(false);

  protected final CharBuffer _cb = new CharBuffer();
  protected final char [] _headerBuffer = new char[256];

  private String _sessionId;
  private boolean _hasSessionCookie;

  private Locale _locale;
  protected boolean _disableHeaders;
  protected boolean _disableCaching;
  protected long _contentLength;
  protected boolean _isClosed;
  protected boolean _hasSentLog;
  
  protected boolean _hasWriter;
  protected boolean _hasOutputStream;

  private AbstractCacheFilterChain _cacheInvocation;

  // the cache entry for a match/if-modified-since
  private AbstractCacheEntry _matchCacheEntry;

  // the new cache for a request getting filled
  private AbstractCacheEntry _newCacheEntry;
  private OutputStream _cacheStream;
  private Writer _cacheWriter;

  protected boolean _isNoCache;
  private boolean _allowCache;
  private boolean _isPrivateCache;
  private boolean _hasCacheControl;

  protected boolean _isTopCache;

  protected boolean _forbidForward;
  protected boolean _hasError;

  private boolean _isCacheHit;

  private final TempBuffer _tempBuffer = TempBuffer.allocate();

  protected AbstractHttpResponse()
  {
    _originalResponseStream = createResponseStream();
    
    _responseOutputStream = new ServletOutputStreamImpl();
    _responsePrintWriter = new ResponseWriter();

    _responseOutputStream.init(_originalResponseStream);
    _responsePrintWriter.init(_originalResponseStream);
  }

  protected AbstractResponseStream createResponseStream()
  {
    return new ResponseStream(this);
  }

  protected AbstractHttpResponse(CauchoRequest request)
  {
    this();
    
    _request = request;
    _originalRequest = request;
    
    _responseOutputStream.init(_originalResponseStream);
    _responsePrintWriter.init(_originalResponseStream);
  }

  /**
   * If set true, client disconnect exceptions are no propagated to the
   * server code.
   */
  public boolean isIgnoreClientDisconnect()
  {
    if (! (_originalRequest instanceof AbstractHttpRequest))
      return true;
    else {
      return ((AbstractHttpRequest) _originalRequest).isIgnoreClientDisconnect();
    }
  }

  /**
   * Return true if the client has disconnected
   */
  public boolean isClientDisconnect()
  {
    return _isClientDisconnect;
  }

  /**
   * Called when the client has disconnected
   */
  public void clientDisconnect()
  {
    _originalRequest.clientDisconnect();
    
    _isClientDisconnect = true;
  }

  /**
   * Return true for the top request.
   */
  public boolean isTop()
  {
    if (! (_request instanceof AbstractHttpRequest))
      return false;
    else {
      return ((AbstractHttpRequest) _request).isTop();
    }
  }

  /**
   * Returns the next response.
   */
  public ServletResponse getResponse()
  {
    return null;
  }

  /**
   * Initialize the response for a new request.
   *
   * @param stream the underlying output stream.
   */
  public void init(WriteStream stream)
  {
    _rawWrite = stream;
    if (_originalResponseStream instanceof ResponseStream)
      ((ResponseStream) _originalResponseStream).init(_rawWrite);
  }

  /**
   * Initialize the response for a new request.
   *
   * @param request the matching request.
   */
  public void init(CauchoRequest request)
  {
    _request = request;
    _originalRequest = request;
  }

  /**
   * Returns the corresponding request.
   */
  public CauchoRequest getRequest()
  {
    return _request;
  }

  /**
   * Sets the corresponding request.
   */
  public void setRequest(CauchoRequest req)
  {
    _request = req;

    if (_originalRequest == null)
      _originalRequest = req;
  }

  /**
   * Returns the corresponding original
   */
  public CauchoRequest getOriginalRequest()
  {
    return _originalRequest;
  }

  /**
   * Closes the request, called from web-app for early close.
   */
  public void close()
    throws IOException
  {
    // server/125i
    if (! _request.isSuspend()) {
      finishInvocation(true);

      finishRequest(true);
    }
    // getStream().flush();
  }

  /**
   * Returns true for closed requests.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Initializes the Response at the beginning of the request.
   */
  public void start()
    throws IOException
  {
    _statusCode = 200;
    _statusMessage = "OK";

    _headerKeys.clear();
    _headerValues.clear();
    
    _footerKeys.clear();
    _footerValues.clear();

    _hasSessionCookie = false;
    _cookiesOut.clear();

    _isHeaderWritten = false;
    _isChunked = false;
    _isClientDisconnect = false;
    _charEncoding = null;
    _hasCharEncoding = false;
    _contentType = null;
    _contentPrefix = null;
    _locale = null;
    if (_originalResponseStream instanceof ResponseStream)
      ((ResponseStream) _originalResponseStream).init(_rawWrite);
    _flushBuffer = null;

    _contentLength = -1;
    _disableHeaders = false;
    _disableCaching = false;
    _isClosed = false;
    _hasSentLog = false;

    _hasWriter = false;
    _hasOutputStream = false;

    _cacheInvocation = null;
    _matchCacheEntry = null;
    _newCacheEntry = null;
    _cacheStream = null;
    _cacheWriter = null;
    _isPrivateCache = false;
    _hasCacheControl = false;
    _allowCache = true;
    _isNoCache = false;
    _isTopCache = false;
    _isCacheHit = false;

    _sessionId = null;

    _forbidForward = false;

    _originalResponseStream.start();
    
    _responseStream = _originalResponseStream;

    _responseOutputStream.init(_responseStream);
    _responsePrintWriter.init(_responseStream);
  }

  /**
   * For a HEAD request, the response stream should write no data.
   */
  protected void setHead()
  {
    _originalResponseStream.setHead();
  }

  /**
   * For a HEAD request, the response stream should write no data.
   */
  protected final boolean isHead()
  {
    return _originalResponseStream.isHead();
  }

  /**
   * Set true for a cache hit.
   */
  public void setCacheHit(boolean isHit)
  {
    _isCacheHit = isHit;
  }

  /**
   * Set true for a cache hit.
   */
  public boolean isCacheHit()
  {
    return _isCacheHit;
  }

  /**
   * When set to true, RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  public void setForbidForward(boolean forbid)
  {
    _forbidForward = forbid;
  }

  /**
   * Returns true if RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  public boolean getForbidForward()
  {
    return _forbidForward;
  }

  /**
   * Set to true while processing an error.
   */
  public void setHasError(boolean hasError)
  {
    _hasError = hasError;
  }

  /**
   * Returns true if we're processing an error.
   */
  public boolean hasError()
  {
    return _hasError;
  }

  /**
   * Sets the cache entry so we can use it if the servlet returns
   * not_modified response.
   *
   * @param entry the saved cache entry
   */
  public void setMatchCacheEntry(AbstractCacheEntry entry)
  {
    _matchCacheEntry = entry;
  }

  /**
   * Sets the cache invocation to indicate that the response might be
   * cacheable.
   */
  public void setCacheInvocation(AbstractCacheFilterChain cacheInvocation)
  {
    AbstractCacheFilterChain oldCache = _cacheInvocation;
    _cacheInvocation = null;
    
    AbstractCacheEntry oldEntry = _newCacheEntry;
    _newCacheEntry = null;

    if (oldEntry != null)
      oldCache.killCaching(oldEntry);
      
    _cacheInvocation = cacheInvocation;
  }

  public void setTopCache(boolean isTopCache)
  {
    _isTopCache = isTopCache;
  }

  public void setStatus(int code)
  {
    setStatus(code, null);
  }

  public void setStatus(int code, String message)
  {
    if (code < 0)
      code = 500;

    if (message != null) {
    }
    else if (code == SC_OK)  
      message = "OK";
    
    else if (code == SC_NOT_MODIFIED)  
      message = "Not Modified";
    
    else if (message == null) {
      message = (String) _errors.get(String.valueOf(code));

      if (message == null)
        message = L.l("Internal Server Error");
    }

    // server/2h0g
    if (code != SC_OK && code != SC_NOT_MODIFIED)
      killCache();

    _statusCode = code;
    _statusMessage = message;
  }

  public int getStatusCode()
  {
    return _statusCode;
  }

  public String getStatusMessage()
  {
    return _statusMessage;
  }

  public void sendError(int code)
    throws IOException
  {
    sendError(code, null);
  }

  /**
   * Sends an HTTP error to the browser.
   *
   * @param code the HTTP error code
   * @param value a string message
   */
  public void sendError(int code, String value)
    throws IOException
  {
    if (code == SC_NOT_MODIFIED && _matchCacheEntry != null) {
      setStatus(code, value);
      if (handleNotModified(_isTopCache))
        return;
    }
    
    if (isCommitted())
      throw new IllegalStateException(L.l("sendError() forbidden after buffer has been committed."));

    //_currentWriter = null;
    //setStream(_originalStream);
    resetBuffer();

    if (code != SC_NOT_MODIFIED)
      killCache();

    /* XXX: if we've already got an error, won't this just mask it?
    if (responseStream.isCommitted())
      throw new IllegalStateException("response can't sendError() after commit");
    */

    WebApp app = getRequest().getWebApp();

    ErrorPageManager errorManager = null;
    if (app != null)
      errorManager = app.getErrorPageManager();

    setStatus(code, value);
    try {
      if (code == SC_NOT_MODIFIED || code == SC_NO_CONTENT) {
        finishInvocation();
        return;
      }
      else if (errorManager != null) {
        errorManager.sendError(getOriginalRequest(), this,
			       code, _statusMessage);
        // _request.killKeepalive();
        // close, but don't force a flush
        // XXX: finish(false);
        finishInvocation();
        return;
      }

      setContentType("text/html");
      ServletOutputStream s = getOutputStream();

      s.println("<html>");
      if (! isCommitted()) {
        s.print("<head><title>");
        s.print(code);
        s.print(" ");
        s.print(_statusMessage);
        s.println("</title></head>");
      }
      s.println("<body>");

      s.print("<h1>");
      s.print(code);
      s.print(" ");
      s.print(_statusMessage);
      s.println("</h1>");

      if (code == HttpServletResponse.SC_NOT_FOUND) {
        s.println(L.l("{0} was not found on this server.",
                      HTTPUtil.encodeString(getRequest().getPageURI())));
      }
      else if (code == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
        s.println(L.l("The server is temporarily unavailable due to maintenance downtime or excessive load."));
      }

      String version = null;

      if (app == null) {
      }
      else if (app.getServer() != null
	       && app.getServer().getServerHeader() != null) {
	version = app.getServer().getServerHeader();
      }
      else if (CauchoSystem.isTesting()) {
      }
      else
	version = com.caucho.Version.FULL_VERSION;
    
      if (version != null) {
	s.println("<p /><hr />");
	s.println("<small>");
	
	s.println(version);
	  
	s.println("</small>");
      }
      
      s.println("</body></html>");
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    _request.killKeepalive();
    // close, but don't force a flush
    finishInvocation();
  }

  /**
   * Sends a redirect to the browser.  If the URL is relative, it gets
   * combined with the current url.
   *
   * @param url the possibly relative url to send to the browser
   */
  public void sendRedirect(String url)
    throws IOException
  {
    if (url == null)
      throw new NullPointerException();
    
    if (_originalResponseStream.isCommitted())
      throw new IllegalStateException(L.l("Can't sendRedirect() after data has committed to the client."));

    _responseStream.clearBuffer();
    _originalResponseStream.clearBuffer();
    
    _responseStream = _originalResponseStream;
    resetBuffer();
    
    setStatus(SC_MOVED_TEMPORARILY);
    String path = getAbsolutePath(url);

    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < path.length(); i++) {
      char ch = path.charAt(i);

      if (ch == '<')
	cb.append("%3c");
      else if (ch < 0x80)
	cb.append(ch);
      else if (_charEncoding == null) {
	addHex(cb, ch);
      }
      else if (ch < 0x800) {
	int d1 = 0xc0 + ((ch >> 6) & 0x1f);
	int d2 = 0x80 + (ch & 0x3f);

	addHex(cb, d1);
	addHex(cb, d2);
      }
      else if (ch < 0x8000) {
	int d1 = 0xe0 + ((ch >> 12) & 0xf);
	int d2 = 0x80 + ((ch >> 6) & 0x3f);
	int d3 = 0x80 + (ch & 0x3f);

	addHex(cb, d1);
	addHex(cb, d2);
	addHex(cb, d3);
      }
    }

    path = cb.toString();
    
    setHeader("Location", path);
    setHeader("Content-Type", "text/html; charset=utf-8");

    // The data is required for some WAP devices that can't handle an
    // empty response.
    ServletOutputStream out = getOutputStream();
    out.println("The URL has moved <a href=\"" + path + "\">here</a>");
    // closeConnection();
    
    if (_request instanceof AbstractHttpRequest) {
      AbstractHttpRequest request = (AbstractHttpRequest) _request;

      request.saveSession(); // #503
    }

    close();
  }

  private void addHex(CharBuffer cb, int hex)
  {
    int d1 = (hex >> 4) & 0xf;
    int d2 = (hex) & 0xf;
    
    cb.append('%');
    cb.append(d1 < 10 ? (char) (d1 + '0') : (char) (d1 - 10 + 'a'));
    cb.append(d2 < 10 ? (char) (d2 + '0') : (char) (d2 - 10 + 'a'));
  }

  /**
   * Switch to raw socket mode.
   */
  public void switchToRaw()
    throws IOException
  {
    throw new UnsupportedOperationException(L.l("raw mode is not supported in this configuration"));
  }

  /**
   * Switch to raw socket mode.
   */
  public WriteStream getRawOutput()
    throws IOException
  {
    throw new UnsupportedOperationException(L.l("raw mode is not supported in this configuration"));
  }

  /**
   * Returns the absolute path for a given relative path.
   *
   * @param path the possibly relative url to send to the browser
   */
  private String getAbsolutePath(String path)
  {
    int slash = path.indexOf('/');
    
    int len = path.length();

    for (int i = 0; i < len; i++) {
      char ch = path.charAt(i);

      if (ch == ':')
        return path;
      else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z')
        continue;
      else
        break;
    }

    WebApp app = getRequest().getWebApp();

    String hostPrefix = null;
    String host = _request.getHeader("Host");
    String serverName = app.getHostName();

    if (serverName == null
	|| serverName.equals("")
	|| serverName.equals("default")) {
      serverName = _request.getServerName();
    }

    int port = _request.getServerPort();

    if (hostPrefix != null && ! hostPrefix.equals("")) {
    }
    else if (serverName.startsWith("http:")
	     || serverName.startsWith("https:"))
      hostPrefix = serverName;
    else if (host != null) {
      hostPrefix = _request.getScheme() + "://" + host;
    }
    else {
      hostPrefix = _request.getScheme() + "://" + serverName;
      
      if (serverName.indexOf(':') < 0
	  && port != 0 && port != 80 && port != 443)
        hostPrefix += ":" + port;
    }

    if (slash == 0)
      return hostPrefix + path;

    String uri = _request.getRequestURI();
    String queryString = null;

    int p = path.indexOf('?');
    if (p > 0) {
      queryString = path.substring(p + 1);
      path = path.substring(0, p);
    }
    
    p = uri.lastIndexOf('/');

    if (p >= 0)
      path = uri.substring(0, p + 1) + path;

    try {
      if (queryString != null)
        return hostPrefix + InvocationDecoder.normalizeUri(path) + '?' + queryString;
      else
        return hostPrefix + InvocationDecoder.normalizeUri(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the response already contains the named header.
   *
   * @param name name of the header to test.
   */
  public boolean containsHeader(String name)
  {
    for (int i = 0; i < _headerKeys.size(); i++) {
      String oldKey = _headerKeys.get(i);
 
      if (oldKey.equalsIgnoreCase(name))
 	return true;
    }

    if (name.equalsIgnoreCase("content-type"))
      return _contentType != null;
    
    if (name.equalsIgnoreCase("content-length"))
      return _contentLength >= 0;
 
    return false;
  }

  /**
   * Returns the value of an already set output header.
   *
   * @param name name of the header to get.
   */
  public String getHeader(String name)
  {
    ArrayList<String> keys = _headerKeys;
    
    int headerSize = keys.size();
    for (int i = 0; i < headerSize; i++) {
      String oldKey = (String) keys.get(i);
 
      if (oldKey.equalsIgnoreCase(name))
 	return (String) _headerValues.get(i);
    }

    if (name.equalsIgnoreCase("content-type"))
      return _contentType;
 
    if (name.equalsIgnoreCase("content-length"))
      return _contentLength >= 0 ? String.valueOf(_contentLength) : null;
 
    return null;
  }

  /**
   * Sets a header, replacing an already-existing header.
   *
   * @param key the header key to set.
   * @param value the header value to set.
   */
  public void setHeader(String key, String value)
  {
    if (_disableHeaders)
      return;
    else if (value == null)
      throw new NullPointerException();

    if (setSpecial(key, value))
      return;

    // server/05e8 (tck)
    if (_hasWriter) {
      return;
    }

    setHeaderImpl(key, value);
  }


  /**
   * Sets a header, replacing an already-existing header.
   *
   * @param key the header key to set.
   * @param value the header value to set.
   */
  protected void setHeaderImpl(String key, String value)
  {
    int i = 0;
    boolean hasHeader = false;

    ArrayList<String> keys = _headerKeys;
    ArrayList<String> values = _headerValues;
    
    for (i = keys.size() - 1; i >= 0; i--) {
      String oldKey = keys.get(i);

      if (oldKey.equalsIgnoreCase(key)) {
	if (hasHeader) {
	  keys.remove(i);
	  values.remove(i);
	}
	else {
	  hasHeader = true;

	  values.set(i, value);
	}
      }
    }

    if (! hasHeader) {
      keys.add(key);
      values.add(value);
    }
  }

  /**
   * Adds a new header.  If an old header with that name exists,
   * both headers are output.
   *
   * @param key the header key.
   * @param value the header value.
   */
  public void addHeader(String key, String value)
  {
    if (_disableHeaders)
      return;

    if (setSpecial(key, value))
      return;

    // server/05e8 (tck)
    if (_hasWriter) {
      return;
    }

    _headerKeys.add(key);
    _headerValues.add(value);
  }

  /**
   * Special processing for a special value.
   */
  protected boolean setSpecial(String key, String value)
  {
    int length = key.length();
    if (256 <= length)
      return false;
    
    key.getChars(0, length, _headerBuffer, 0);

    switch (_headerCodes.get(_headerBuffer, length)) {
    case HEADER_CACHE_CONTROL:
      if (value.startsWith("max-age")) {
      }
      else if (value.equals("x-anonymous")) {
      }
      else
	_hasCacheControl = true;
      return false;
	
    case HEADER_CONNECTION:
      if ("close".equalsIgnoreCase(value))
	_request.killKeepalive();
      return true;
	
    case HEADER_CONTENT_TYPE:
      setContentType(value);
      return true;
	
    case HEADER_CONTENT_LENGTH:
      _contentLength = Long.parseLong(value);
      return true;
	
    case HEADER_DATE:
      return true;
	
    case HEADER_SERVER:
      return false;

    default:
      return false;
    }
  }
  
  public void removeHeader(String key)
  {
    if (_disableHeaders)
      return;
    
    ArrayList<String> keys = _headerKeys;
    ArrayList<String> values = _headerValues;
    
    for (int i = keys.size() - 1; i >= 0; i--) {
      String oldKey = keys.get(i);

      if (oldKey.equalsIgnoreCase(key)) {
        keys.remove(i);
        values.remove(i);
        return;
      }
    }
  }

  /**
   * Convenience for setting an integer header.  An old header with the
   * same name will be replaced.
   *
   * @param name the header name.
   * @param value an integer to be converted to a string for the header.
   */
  public void setIntHeader(String name, int value)
  {
    _cb.clear();
    _cb.append(value);
    setHeader(name, _cb.toString());
  }

  /**
   * Convenience for adding an integer header.  If an old header already
   * exists, both will be sent to the browser.
   *
   * @param key the header name.
   * @param value an integer to be converted to a string for the header.
   */
  public void addIntHeader(String key, int value)
  {
    _cb.clear();
    _cb.append(value);
    addHeader(key, _cb.toString());
  }

  /**
   * Convenience for setting a date header.  An old header with the
   * same name will be replaced.
   *
   * @param name the header name.
   * @param value an time in milliseconds to be converted to a date string.
   */
  public void setDateHeader(String name, long value)
  {
    _calendar.setGMTTime(value);

    setHeader(name, _calendar.printDate());
  }


  /**
   * Convenience for adding a date header.  If an old header with the
   * same name exists, both will be displayed.
   *
   * @param key the header name.
   * @param value an time in milliseconds to be converted to a date string.
   */
  public void addDateHeader(String key, long value)
  {
    _calendar.setGMTTime(value);

    addHeader(key, _calendar.printDate());
  }

  /**
   * Sets the content length of the result.  In general, Resin will handle
   * the content length, but for things like long downloads adding the
   * length will give a valuable hint to the browser.
   *
   * @param length the length of the content.
   */
  public void setContentLength(int length)
  {
    _contentLength = length;
  }

  /**
   * Returns the value of the content-length header.
   */
  public long getContentLengthHeader()
  {
    return _contentLength;
  }

  /**
   * Sets the browser content type.  If the value contains a charset,
   * the output encoding will be changed to match.
   *
   * <p>For example, to set the output encoding to use UTF-8 instead of
   * the default ISO-8859-1 (Latin-1), use the following:
   * <code><pre>
   * setContentType("text/html; charset=UTF-8");
   * </pre></code>
   */
  public void setContentType(String value)
  {
    if (isCommitted())
      return;

    if (_disableHeaders || value == null) {
      _contentType = null;
      return;
    }
    else if (value == "text/html" || value.equals("text/html")) {
      _contentType = "text/html";
      return;
    }
    
    _contentType = value;
    
    int length = value.length();
    int i;
    int ch;

    for (i = 0;
	 i < length && value.charAt(i) != ';'
	   && ! Character.isWhitespace(value.charAt(i));
	 i++) {
    }

    if (i < length)
      _contentPrefix = _contentType.substring(0, i);
    else
      _contentPrefix = _contentType;

    while ((i = value.indexOf(';', i)) > 0) {
      int semicolon = i;
      for (i++; i < length && XmlChar.isWhitespace(value.charAt(i)); i++) {
      }

      int j;
      for (j = i + 1;
           j < length && ! XmlChar.isWhitespace((ch = value.charAt(j))) &&
             ch != '=';
           j++) {
      }
      
      if (length <= j)
	break;
      else if ((ch = value.charAt(i)) != 'c' && ch != 'C') {
      }
      else if (value.substring(i, j).equalsIgnoreCase("charset")) {
	for (; j < length && XmlChar.isWhitespace(value.charAt(j)); j++) {
	}

        if (length <= j || value.charAt(j) != '=')
          continue;
        
	for (j++; j < length && XmlChar.isWhitespace(value.charAt(j)); j++) {
	}

        String encoding;

        if (j < length && value.charAt(j) == '"') {
          int k = ++j;
          
          for (; j < length && value.charAt(j) != '"'; j++) {
          }

          encoding = value.substring(k, j);
        }
        else {
          int k = j;
          for (k = j;
               j < length && ! XmlChar.isWhitespace(ch = value.charAt(j)) && ch != ';';
               j++) {
          }

          encoding = value.substring(k, j);
        }

	int tail = value.indexOf(';', semicolon + 1);

	StringBuilder sb = new StringBuilder();
	sb.append(value, 0, semicolon);
	if (tail > 0)
	  sb.append(value, tail, value.length());
	
	_contentType = sb.toString();

	if (! _hasWriter) {
	  _hasCharEncoding = true;
	  _charEncoding = encoding;
	}
	break;
      }
      else
	i = j;
    }

    // XXX: conflict with servlet exception throwing order?
    try {
      _responseStream.setEncoding(_charEncoding);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Gets the content type.
   */
  public String getContentType()
  {
    if (_contentType == null)
      return null;

    String charEncoding = getCharacterEncoding();

    if (charEncoding != null)
      return _contentType + "; charset=" + charEncoding;
    else
      return _contentType;
  }

  /**
   * Gets the character encoding.
   */
  public String getCharacterEncoding()
  {
    if (_charEncoding != null)
      return _charEncoding;

    WebApp app = _request.getWebApp();

    String encoding = null;
    
    if (app != null)
      encoding = app.getCharacterEncoding();

    if (encoding != null)
      return encoding;
    else
      return "iso-8859-1";
  }

  /**
   * Sets the character encoding.
   */
  public void setCharacterEncoding(String encoding)
  {
    if (isCommitted())
      return;
    if (_hasWriter)
      return;

    _hasCharEncoding = true;
    if (encoding == null
        || encoding.equals("ISO-8859-1")
        || encoding.equals("")) {
      encoding = null;
      _charEncoding = "iso-8859-1";
    }
    else
      _charEncoding = encoding;

    try {
      _responseStream.setEncoding(encoding);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  String getRealCharacterEncoding()
  {
    return _charEncoding;
  }

  /**
   * Adds a cookie to the response.
   *
   * @param cookie the response cookie
   */
  public void addCookie(Cookie cookie)
  {
    _request.setHasCookie();

    if (_disableHeaders)
      return;

    if (cookie == null)
      return;

    _cookiesOut.add(cookie);
  }

  public Cookie getCookie(String name)
  {
    if (_cookiesOut == null)
      return null;

    for (int i = _cookiesOut.size() - 1; i >= 0; i--) {
      Cookie cookie = (Cookie) _cookiesOut.get(i);

      if (cookie.getName().equals(name))
        return cookie;
    }

    return null;
  }

  public ArrayList getCookies()
  {
    return _cookiesOut;
  }

  public void setSessionId(String id)
  {
    _sessionId = id;

    // XXX: server/1315 vs server/0506 vs server/170k
    // could also set the nocache=JSESSIONID
    setPrivateOrResinCache(true);
  }

  /**
   * Sets a footer, replacing an already-existing footer
   *
   * @param key the header key to set.
   * @param value the header value to set.
   */
  public void setFooter(String key, String value)
  {
    if (_disableHeaders)
      return;
    else if (value == null)
      throw new NullPointerException();

    int i = 0;
    boolean hasFooter = false;

    for (i = _footerKeys.size() - 1; i >= 0; i--) {
      String oldKey = _footerKeys.get(i);

      if (oldKey.equalsIgnoreCase(key)) {
	if (hasFooter) {
	  _footerKeys.remove(i);
	  _footerValues.remove(i);
	}
	else {
	  hasFooter = true;

	  _footerValues.set(i, value);
	}
      }
    }

    if (! hasFooter) {
      _footerKeys.add(key);
      _footerValues.add(value);
    }
  }

  /**
   * Adds a new footer.  If an old footer with that name exists,
   * both footers are output.
   *
   * @param key the footer key.
   * @param value the footer value.
   */
  public void addFooter(String key, String value)
  {
    if (_disableHeaders)
      return;

    if (setSpecial(key, value))
      return;

    _footerKeys.add(key);
    _footerValues.add(value);
  }

  /**
   * Sets the ResponseStream
   */
  public void setResponseStream(AbstractResponseStream responseStream)
  {
    _responseStream = responseStream;

    _responseOutputStream.init(responseStream);
    _responsePrintWriter.init(responseStream);
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getResponseStream()
  {
    return _responseStream;
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getOriginalStream()
  {
    return _originalResponseStream;
  }

  /**
   * Returns true for a Caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return _responseStream.isCauchoResponseStream();
  }
  
  /**
   * Returns the ServletOutputStream for the response.
   */
  public ServletOutputStream getOutputStream() throws IOException
  {
    /*
    if (_hasWriter)
      throw new IllegalStateException(L.l("getOutputStream() can't be called after getWriter()."));
    */

    _hasOutputStream = true;

    /*
    // server/10a2
    if (! _hasWriter) {
      // jsp/0510 vs jsp/1b00
      // _responseStream.setOutputStreamOnly(true);
    }
    */
    
    return _responseOutputStream;
  }

  /**
   * Sets the flush buffer
   */
  public void setFlushBuffer(FlushBuffer flushBuffer)
  {
    _flushBuffer = flushBuffer;
  }

  /**
   * Gets the flush buffer
   */
  public FlushBuffer getFlushBuffer()
  {
    return _flushBuffer;
  }

  /**
   * Returns a PrintWriter for the response.
   */
  public PrintWriter getWriter() throws IOException
  {
    /*
    if (_hasOutputStream)
      throw new IllegalStateException(L.l("getWriter() can't be called after getOutputStream()."));
    */

    if (! _hasWriter) {
      _hasWriter = true;

      if (_charEncoding != null)
	_responseStream.setEncoding(_charEncoding);
    }
    
    return _responsePrintWriter;
  }

  /**
   * Returns the parent writer.
   */
  public PrintWriter getNextWriter()
  {
    return null;
  }

  /**
   * Encode the URL with the session jd.
   *
   * @param string the url to be encoded
   *
   * @return the encoded url
   */
  public String encodeURL(String string)
  {
    CauchoRequest request = getRequest();
    
    WebApp app = request.getWebApp();

    if (app == null)
      return string;
    
    if (request.isRequestedSessionIdFromCookie())
      return string;

    HttpSession session = request.getSession(false);
    if (session == null)
      return string;

    SessionManager sessionManager = app.getSessionManager();
    if (! sessionManager.enableSessionUrls())
      return string;

    CharBuffer cb = _cb;
    cb.clear();

    String altPrefix = sessionManager.getAlternateSessionPrefix();

    if (altPrefix == null) {
      // standard url rewriting
      int p = string.indexOf('?');

      if (p == 0) {
	cb.append(string);
      }
      else if (p > 0) {
        cb.append(string, 0, p);
        cb.append(sessionManager.getSessionPrefix());
        cb.append(session.getId());
        cb.append(string, p, string.length() - p);
      }
      else if ((p = string.indexOf('#')) >= 0) {
        cb.append(string, 0, p);
        cb.append(sessionManager.getSessionPrefix());
        cb.append(session.getId());
        cb.append(string, p, string.length() - p);
      }
      else {
        cb.append(string);
        cb.append(sessionManager.getSessionPrefix());
        cb.append(session.getId());
      }
    }
    else {
      int p = string.indexOf("://");
      
      if (p < 0) {
	cb.append(altPrefix);
	cb.append(session.getId());
	
	if (! string.startsWith("/")) {
	  cb.append(_request.getContextPath());
	  cb.append('/');
	}
        cb.append(string);
      }
      else {
	int q = string.indexOf('/', p + 3);

	if (q < 0) {
	  cb.append(string);
	  cb.append(altPrefix);
	  cb.append(session.getId());
	}
	else {
	  cb.append(string.substring(0, q));
	  cb.append(altPrefix);
	  cb.append(session.getId());
	  cb.append(string.substring(q));
	}
      }
    }

    return cb.toString();
  }

  public String encodeRedirectURL(String string)
  {
    return encodeURL(string);
  }

    /**
     * @deprecated
     */
  public String encodeRedirectUrl(String string)
  {
    return encodeRedirectURL(string);
  }

    /**
     * @deprecated
     */
  public String encodeUrl(String string)
  {
    return encodeURL(string);
  }

  /*
   * jsdk 2.2
   */

  public void setBufferSize(int size)
  {
    _responseStream.setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _responseStream.getBufferSize();
  }

  public void flushBuffer()
    throws IOException
  {
    // server/10sn
    _responseStream.flush();
  }

  public void flushHeader()
    throws IOException
  {
    _responseStream.flushBuffer();
  }

  public void setDisableAutoFlush(boolean disable)
  {
    // XXX: _responseStream.setDisableAutoFlush(disable);
  }

  /**
   * Returns true if some data has been sent to the browser.
   */
  public boolean isCommitted()
  {
    return _originalResponseStream.isCommitted();
  }

  public void reset()
  {
    reset(false);
  }

  public void resetBuffer()
  {
    _responseStream.clearBuffer();
    /*
    if (_currentWriter instanceof JspPrintWriter)
      ((JspPrintWriter) _currentWriter).clear();
    */
  }

  /**
   * Clears the response for a forward()
   *
   * @param force if not true and the response stream has committed,
   *   throw the IllegalStateException.
   */
  void reset(boolean force)
  {
    if (! force && _originalResponseStream.isCommitted())
      throw new IllegalStateException(L.l("response cannot be reset() after committed"));
    
    _responseStream.clearBuffer();
    /*
    if (_currentWriter instanceof JspPrintWriter)
      ((JspPrintWriter) _currentWriter).clear();
    */
    _statusCode = 200;
    _statusMessage = "OK";

    _headerKeys.clear();
    _headerValues.clear();

    // cookiesOut.clear();

    _contentLength = -1;
    //_isNoCache = false;
    //_isPrivateCache = false;
    
    _charEncoding = null;
    _locale = null;

    _hasOutputStream = false;
    _hasWriter = false;
    try {
      _responseStream.setLocale(null);
      _responseStream.setEncoding(null);
    } catch (Exception e) {
    }
  }

  // XXX: hack to deal with forwarding
  public void clearBuffer()
  {
    _responseStream.clearBuffer();
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;

    if (! _hasCharEncoding && ! isCommitted()) {
      _charEncoding = getRequest().getWebApp().getLocaleEncoding(locale);

      try {
        if (_charEncoding != null) {
          // _originalStream.setEncoding(_charEncoding);
          _responseStream.setEncoding(_charEncoding);
	}
      } catch (IOException e) {
      }
    }

    CharBuffer cb = _cb;
    cb.clear();
    cb.append(locale.getLanguage());
    if (locale.getCountry() != null &&  ! "".equals(locale.getCountry())) {
      cb.append("-");
      cb.append(locale.getCountry());
      if (locale.getVariant() != null && ! "".equals(locale.getVariant())) {
        cb.append("-");
        cb.append(locale.getVariant());
      }
    }
    
    setHeader("Content-Language", cb.toString());
  }

  public Locale getLocale()
  {
    if (_locale != null)
      return _locale;
    else
      return Locale.getDefault();
  }

  // needed to support JSP
  
  public int getRemaining()
  {
    return _responseStream.getRemaining();
  }

  /**
   * Returns the number of bytes sent to the output.
   */
  public int getContentLength()
  {
    return _originalResponseStream.getContentLength();
  }

  /**
   * Disables the response
   *
   * @since Servlet 3.0
   */
  public void disable()
  {
  }

  /**
   * Enables the response
   *
   * @since Servlet 3.0
   */
  public void enable()
  {
  }

  /**
   * Returns true if the response is disabled
   *
   * @since Servlet 3.0
   */
  public boolean isDisabled()
  {
    return false;
  }

  public boolean disableHeaders(boolean disable)
  {
    boolean old = _disableHeaders;
    _disableHeaders = disable;
    return old;
  }

  public boolean disableCaching(boolean disable)
  {
    boolean old = _disableCaching;
    _disableCaching = disable;
    return old;
  }

  /**
   * Returns true if the headers have been written.
   */
  public boolean isHeaderWritten()
  {
    return _isHeaderWritten;
  }

  /**
   * Returns true if the headers have been written.
   */
  public void setHeaderWritten(boolean isWritten)
  {
    _isHeaderWritten = isWritten;
  }
  
  /**
   * Writes the continue
   */
  final void writeContinue()
    throws IOException
  {
    if (! isHeaderWritten()) {
      writeContinueInt(_rawWrite);
      _rawWrite.flush();
    }
  }
  
  /**
   * Writes the continue
   */
  protected void writeContinueInt(WriteStream os)
    throws IOException
  {
  }

  /**
   * Writes the headers to the stream.  Called prior to the first flush
   * of data.
   *
   * @param os browser stream.
   * @param length length of the response if known, or -1 is unknown.
   * @return true if the content is chunked.
   */
  protected boolean writeHeaders(WriteStream os, int length)
    throws IOException
  {
    if (isHeaderWritten())
      return _isChunked;

    // server/1373 for getBufferSize()
    boolean canCache = startCaching(true);
    _isHeaderWritten = true;
    boolean isHead = false;
    
    if (_request.getMethod().equals("HEAD")) {
      isHead = true;
      _originalResponseStream.setHead();
    }

    WebApp webApp = _request.getWebApp();

    int majorCode = _statusCode / 100;

    if (webApp != null) {
      if (majorCode == 5)
	webApp.addStatus500();
    }

    HttpSession session = _originalRequest.getMemorySession();
    if (session instanceof SessionImpl)
      ((SessionImpl) session).saveBeforeHeaders();

    if (_sessionId != null && ! _hasSessionCookie) {
      _hasSessionCookie = true;

      addServletCookie(webApp);
    }

    _isChunked = writeHeadersInt(os, length, isHead);

    return _isChunked;
  }

  protected void addServletCookie(WebApp webApp)
  {
    addCookie(createServletCookie(webApp));
  }

  protected Cookie createServletCookie(WebApp webApp)
  {
    SessionManager manager = webApp.getSessionManager();

    String cookieName;

    if (_request.isSecure())
      cookieName = manager.getSSLCookieName();
    else
      cookieName = manager.getCookieName();
      
    CookieImpl cookie = new CookieImpl(cookieName, _sessionId);
    cookie.setVersion(manager.getCookieVersion());
    String domain = manager.getCookieDomain();
    if (domain != null)
      cookie.setDomain(domain);
    long maxAge = manager.getCookieMaxAge();
    if (maxAge > 0)
      cookie.setMaxAge((int) (maxAge / 1000));
    cookie.setPath("/");
      
    cookie.setPort(manager.getCookiePort());
    if (manager.getCookieSecure()) {
      cookie.setSecure(_request.isSecure());
      /*
	else if (manager.getCookiePort() == null)
	cookie.setPort(String.valueOf(_request.getServerPort()));
      */
    }

    return cookie;
  }

  /**
   * Called to start caching.
   */
  protected boolean startCaching(boolean isByte)
  {
    if (_isHeaderWritten)
      return false;
    _isHeaderWritten = true;

    if (_statusCode == SC_OK && ! _disableCaching) // && getBufferSize() > 0)
      return startCaching(_headerKeys, _headerValues,
			  _contentType, _charEncoding, isByte);
    else
      return false;
  }

  /**
   * Tests to see if the response is cacheable.
   *
   * @param keys the header keys of the response
   * @param values the header values of the response
   * @param contentType the contentType of the response
   * @param charEncoding the character encoding of the response
   *
   * @return true if caching has started
   */
  boolean startCaching(ArrayList<String> keys,
                       ArrayList<String> values,
                       String contentType, String charEncoding,
		       boolean isByte)
  {
    if (_cacheInvocation == null)
      return false;
    /*
      // jsp/17ah
    else if (_responseStream != _originalResponseStream) {
      return false;
    }
    */
    else if (! isCauchoResponseStream()) {
      return false;
    }
    else if (! (_originalRequest instanceof CauchoRequest)) {
      return false;
    }
    else if (! _allowCache) {
      return false;
    }
    else {
      CauchoRequest request = (CauchoRequest) _originalRequest;
      
      _newCacheEntry = _cacheInvocation.startCaching(request,
						     this, keys, values,
						     contentType,
						     charEncoding,
						     _contentLength);

      if (_newCacheEntry == null) {
	return false;
      }
      else if (isByte) {
	_cacheStream = _newCacheEntry.openOutputStream();

	if (_cacheStream != null)
	  _originalResponseStream.setByteCacheStream(_cacheStream);
      
	return _cacheStream != null;
      }
      else {
	_cacheWriter = _newCacheEntry.openWriter();

	if (_cacheWriter != null)
	  _originalResponseStream.setCharCacheStream(_cacheWriter);
      
	return _cacheWriter != null;
      }
    }
  }
  

  /**
   * Handle a SC_NOT_MODIFIED response.  If we've got a cache, fill the
   * results from the cache.
   *
   * @param isTop if true, this is the top-level request.
   *
   * @return true if we filled from the cache
   */
  private boolean handleNotModified(boolean isTop)
    throws IOException
  {
    if (_statusCode != SC_NOT_MODIFIED) {
      return false;
    }
    else if (_matchCacheEntry != null) {
      if (_originalResponseStream.isCommitted())
        return false;

      // need to unclose because the not modified might be detected only
      // when flushing the data
      _originalResponseStream.clearClosed();
      _isClosed = false;

      /* XXX: complications with filters */
      if (_cacheInvocation != null
	  && _cacheInvocation.fillFromCache((CauchoRequest) _originalRequest,
					    this, _matchCacheEntry, isTop)) {
        _matchCacheEntry.updateExpiresDate();
        _cacheInvocation = null;
        _matchCacheEntry = null;

        finishInvocation(); // Don't force a flush to avoid extra TCP packet
      
        return true;
      }
    }
    // server/13dh
    else if (_cacheInvocation != null) {
      CauchoRequest req = (CauchoRequest) _originalRequest;
      WebApp app = req.getWebApp();
      
      long maxAge = app.getMaxAge(req.getRequestURI());

      if (maxAge > 0 && ! containsHeader("Expires")) {
	setDateHeader("Expires", maxAge + Alarm.getCurrentTime());
      }
    }

    return false;
  }

  abstract protected boolean writeHeadersInt(WriteStream os,
					     int length,
					     boolean isHead)
    throws IOException;

  /**
   * Sets true if the cache is only for the browser, but not
   * Resin's cache or proxies.
   *
   * <p>Since proxy caching also caches headers, cached pages with
   * session ids can't be cached in the browser.
   *
   * XXX: but doesn't this just mean that Resin shouldn't
   * send the session information back if the page is cached?
   * Because a second request where everything is identical
   * would see the same response except for the cookies.
   */
  public void setPrivateCache(boolean isPrivate)
  {
    // XXX: let the webApp override this?
    _isPrivateCache = isPrivate;

    // server/12dm
    _allowCache = false;
  }

  /**
   * Sets true if the cache is only for the browser and 
   * Resin's cache but not proxies.
   */
  public void setPrivateOrResinCache(boolean isPrivate)
  {
    // XXX: let the webApp override this?

    _isPrivateCache = isPrivate;
  }
  
  /**
   * Returns the value of the private cache.
   */
  public boolean getPrivateCache()
  {
    return _isPrivateCache;
  }

  /**
   * Returns true if the response should contain a Cache-Control: private
   */
  protected boolean isPrivateCache()
  {
    return ! _hasCacheControl && _isPrivateCache;
  }

  /**
   * Set if the page is non-cacheable.
   */
  public void setNoCache(boolean isNoCache)
  {
    _isNoCache = isNoCache;
  }

  /**
   * Returns true if the page is non-cacheable
   */
  public boolean isNoCache()
  {
    return _isNoCache;
  }

  /**
   * Set if the page is non-cacheable.
   */
  public void killCache()
  {
    _allowCache = false;

    // server/1b15
    // setNoCache(true);
  }

  /**
   * Fills the response for a cookie
   *
   * @param cb result buffer to contain the generated string
   * @param cookie the cookie
   * @param date the current date
   * @param version the cookies version
   */
  public boolean fillCookie(CharBuffer cb, Cookie cookie,
                            long date, int version,
			    boolean isCookie2)
  {
    // How to deal with quoted values?  Old browsers can't deal with
    // the quotes.
    
    cb.clear();
    cb.append(cookie.getName());
    if (isCookie2) {
      cb.append("=\"");
      cb.append(cookie.getValue());
      cb.append("\"");
    }
    else {
      cb.append("=");
      cb.append(cookie.getValue());
    }

    String domain = cookie.getDomain();
    if (domain != null && ! domain.equals("")) {
      if (isCookie2) {
        cb.append("; Domain=");
      
        cb.append('"');
        cb.append(domain);
        cb.append('"');
      }
      else {
        cb.append("; domain=");
        cb.append(domain);
      }
    }

    String path = cookie.getPath();
    if (path != null && ! path.equals("")) {
      if (isCookie2) {
        cb.append("; Path=");
      
        cb.append('"');
        cb.append(path);
        cb.append('"');
      }
      else {
	// Caps from TCK test
	if (version > 0)
	  cb.append("; Path=");
	else
	  cb.append("; path=");
        cb.append(path);
      }
    }
    
    if (cookie.getSecure()) {
      if (version > 0)
        cb.append("; Secure");
      else
        cb.append("; secure");
    }

    int maxAge = cookie.getMaxAge();
    if (version > 0) {
      if (maxAge >= 0) {
        cb.append("; Max-Age=");
        cb.append(maxAge);
      }
      
      cb.append("; Version=");
      cb.append(version);
      
      if (cookie.getComment() != null) {
        cb.append("; Comment=\"");
        cb.append(cookie.getComment());
        cb.append("\"");
      }

      if (cookie instanceof CookieImpl) {
	CookieImpl extCookie = (CookieImpl) cookie;
	String port = extCookie.getPort();

	if (port != null && isCookie2) {
	  cb.append("; Port=\"");
	  cb.append(port);
	  cb.append("\"");
	}
      }
    }

    if (isCookie2) {
    }
    else if (maxAge == 0) {
      cb.append("; expires=Thu, 01-Dec-1994 16:00:00 GMT");
    }
    else if (maxAge >= 0) {
      _calendar.setGMTTime(date + 1000L * (long) maxAge);
      cb.append("; expires=");
      cb.append(_calendar.format("%a, %d-%b-%Y %H:%M:%S GMT"));
    }

    WebApp app = _request.getWebApp();
    if (app.getCookieHttpOnly()) {
      cb.append("; HttpOnly");
    }

    return true;
  }

  /*
  protected ConnectionController getController()
  {
    if (_originalRequest instanceof AbstractHttpRequest) {
      AbstractHttpRequest request = (AbstractHttpRequest) _originalRequest;
      Connection conn = request.getConnection();
      return conn.getController();
    }
    else
      return null;
  }
  */

  public AbstractHttpResponse getAbstractHttpResponse()
  {
    return this;
  }

  public TcpDuplexController upgradeProtocol(TcpDuplexHandler handler)
  {
    throw new IllegalStateException(L.l("'{0}' does not support upgrading",
					this));
  }
  
  /**
   * Complete the invocation.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   */
  public void finishInvocation() throws IOException
  {
    // server/1960, server/1l11
    // finishInvocation(false);
    boolean isClose = ! _request.isSuspend();
    
    finishInvocation(isClose);
  }
  
  /**
   * Complete the invocation.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   */
  public void finishRequest() throws IOException
  {
    finishRequest(false);
  }

  /**
   * Complete the request.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   *
   * @param isClose true if the response should be flushed.
   */
  private void finishInvocation(boolean isClose)
    throws IOException
  {
    if (_isClosed)
      return;

    boolean isSuspend = false;
    Connection conn = null;

    try {
      if (_originalRequest instanceof AbstractHttpRequest) {
	AbstractHttpRequest request = (AbstractHttpRequest) _originalRequest;
	
	conn = request.getConnection();
      }

      /* XXX:
      if (_statusCode == SC_NOT_MODIFIED && _request.isInitial()) {
	handleNotModified(_isTopCache);
      }
      */
      if (_statusCode == SC_NOT_MODIFIED) {
	handleNotModified(_isTopCache);
      }

      if (isClose)
	_responseStream.close();
      else if (_responseStream != _originalResponseStream)
	_responseStream.finish();
      else
	_responseStream.flush();

      if (_rawWrite != null) {
	_rawWrite.flushBuffer();
      }
    } catch (ClientDisconnectException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;

      if (isIgnoreClientDisconnect())
	log.fine(e.toString());
      else
	throw e;
    } catch (IOException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;
      
      throw e;
    }
  }

  /**
   * Complete the request.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   *
   * @param isClose true if the response should be flushed.
   */
  private void finishRequest(boolean isClose) throws IOException
  {
    if (_isClosed)
      return;

    Connection conn = null;

    try {
      if (_originalRequest instanceof AbstractHttpRequest) {
	AbstractHttpRequest request = (AbstractHttpRequest) _originalRequest;
	
	conn = request.getConnection();

	try {
	  request.skip();
	} catch (BadRequestException e) {
	  log.warning(e.toString());
	  log.log(Level.FINE, e.toString(), e);
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      // include() files finish too, but shouldn't force a flush, hence
      // flush is false
      // Never send flush?
      _responseStream.close();

      _isClosed = true;

      if (_rawWrite != null)
	_rawWrite.flushBuffer();

      if (_newCacheEntry != null && _cacheInvocation != null) {
	OutputStream cacheStream = _cacheStream;
	_cacheStream = null;
	
	Writer cacheWriter = _cacheWriter;
	_cacheWriter = null;

	if (cacheStream != null)
	  cacheStream.close();

	if (cacheWriter != null)
	  cacheWriter.close();
	
        WebApp webApp = _request.getWebApp();
	if (_statusCode == 200 && _allowCache
            && webApp != null && webApp.isActive()) {
	  AbstractCacheFilterChain cache = _cacheInvocation;
	  _cacheInvocation = null;

	  AbstractCacheEntry cacheEntry = _newCacheEntry;
	  _newCacheEntry = null;
	  
	  cache.finishCaching(cacheEntry);
	}
      }
    } catch (ClientDisconnectException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;

      if (isIgnoreClientDisconnect())
	log.fine(e.toString());
      else
	throw e;
    } catch (IOException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;
      
      throw e;
    } finally {
      _isClosed = true;

      AbstractCacheFilterChain cache = _cacheInvocation;
      _cacheInvocation = null;
      
      AbstractCacheEntry cacheEntry = _newCacheEntry;
      _newCacheEntry = null;
      
      _cacheStream = null;
      _cacheWriter = null;

      if (cacheEntry != null)
	cache.killCaching(cacheEntry);
    }
  }

  public void killCaching()
  {
    AbstractCacheFilterChain cache = _cacheInvocation;
    _cacheInvocation = null;
    
    AbstractCacheEntry cacheEntry = _newCacheEntry;
    _newCacheEntry = null;

    if (cacheEntry != null) {
      cache.killCaching(cacheEntry);
      _cacheStream = null;
      _cacheWriter = null;
      _responseStream.killCaching();
    }
  }

  TempBuffer getBuffer()
  {
    return _tempBuffer;
  }

  protected final QDate getCalendar()
  {
    return _calendar;
  }

  protected void free()
  {
    _request = null;
    _originalRequest = null;
    _cacheInvocation = null;
    _newCacheEntry = null;
    _matchCacheEntry = null;
    _cacheStream = null;
    _cacheWriter = null;
  }

  static {
    _errors = new HashMap<String,String>();
    _errors.put("100", "Continue");
    _errors.put("101", "Switching Protocols");
    _errors.put("200", "OK");
    _errors.put("201", "Created");
    _errors.put("202", "Accepted");
    _errors.put("203", "Non-Authoritative Information");
    _errors.put("204", "No Content");
    _errors.put("205", "Reset Content");
    _errors.put("206", "Partial Content");
    _errors.put("300", "Multiple Choices");
    _errors.put("301", "Moved Permanently");
    _errors.put("302", "Found");
    _errors.put("303", "See Other");
    _errors.put("304", "Not Modified");
    _errors.put("305", "Use Proxy");
    _errors.put("307", "Temporary Redirect");
    _errors.put("400", "Bad Request");
    _errors.put("401", "Unauthorized");
    _errors.put("402", "Payment Required");
    _errors.put("403", "Forbidden");
    _errors.put("404", "Not Found");
    _errors.put("405", "Method Not Allowed");
    _errors.put("406", "Not Acceptable");
    _errors.put("407", "Proxy Authentication Required");
    _errors.put("408", "Request Timeout");
    _errors.put("409", "Conflict");
    _errors.put("410", "Gone");
    _errors.put("411", "Length Required");
    _errors.put("412", "Precondition Failed");
    _errors.put("413", "Request Entity Too Large");
    _errors.put("414", "Request-URI Too Long");
    _errors.put("415", "Unsupported Media Type");
    _errors.put("416", "Requested Range Not Satisfiable");
    _errors.put("417", "Expectation Failed");
    _errors.put("500", "Internal Server Error");
    _errors.put("501", "Not Implemented");
    _errors.put("502", "Bad Gateway");
    _errors.put("503", "Service Temporarily Unavailable");
    _errors.put("504", "Gateway Timeout");
    _errors.put("505", "Http Version Not Supported");

    _headerCodes = new CaseInsensitiveIntMap();
    _headerCodes.put("cache-control", HEADER_CACHE_CONTROL);
    _headerCodes.put("connection", HEADER_CONNECTION);
    _headerCodes.put("content-type", HEADER_CONTENT_TYPE);
    _headerCodes.put("content-length", HEADER_CONTENT_LENGTH);
    _headerCodes.put("date", HEADER_DATE);
    _headerCodes.put("server", HEADER_SERVER);
  }
}
