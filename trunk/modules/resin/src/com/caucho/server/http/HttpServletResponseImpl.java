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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.caucho.VersionFactory;
import com.caucho.server.httpcache.AbstractCacheFilterChain;
import com.caucho.server.session.CookieImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.HTTPUtil;
import com.caucho.util.L10N;

/**
 * User facade for http responses.
 */
public final class HttpServletResponseImpl extends AbstractCauchoResponse
  implements CauchoResponse
{
  private static final Logger log
    = Logger.getLogger(HttpServletResponseImpl.class.getName());
  private static final L10N L = new L10N(HttpServletResponseImpl.class);

  private static final HashMap<Integer,String> _errors;

  private final HttpServletRequestImpl _request;
  private AbstractHttpResponse _response;

  private int _status = 200;
  private String _statusMessage = "OK";

  private String _sessionId;
  private ArrayList<Cookie> _cookiesOut;

  private AbstractResponseStream _responseStream;
  private ServletOutputStreamImpl _outputStream;
  private ResponseWriter _writer;

  private String _setCharEncoding;
  private String _charEncoding;
  private String _contentType;

  private Locale _locale;

  private boolean _hasError;
  private boolean _forbidForward;

  private AbstractCacheFilterChain _cacheInvocation;

  // send a Cache-Control: no-cache
  private boolean _isNoCache;
  // cache private, e.g. for session cookie
  private boolean _isPrivateCache;
  // application has set cache control
  private boolean _isCacheControl;
  // rewrite: cache disable unless a Vary exists (to handle rewrite issues)
  private boolean _isNoCacheUnlessVary;
  // disable the cache
  private boolean _isDisableCache;

  public HttpServletResponseImpl(HttpServletRequestImpl request,
                                 AbstractHttpResponse response)
  {
    _request = request;
    _response = response;

    _responseStream = response.getResponseStream();
  }

  public HttpServletRequestImpl getRequest()
  {
    return _request;
  }

  //
  // servlet response
  //

  //
  // output stream
  //

  /**
   * Returns an output stream for writing to the client.  You can use
   * the output stream to write binary data.
   */
  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    if (_outputStream != null)
      return _outputStream;

    // server/1b08 (tck)
    if (_writer != null)
      throw new IllegalStateException(L.l("getOutputStream() can't be called after getWriter()."));
    
    try {
      // jsp/0510
      _responseStream.clearBuffer();
    } catch (Exception e) {
      // server/1b32
      log.log(Level.FINER, e.toString(), e);
    }

    _outputStream = _response.getResponseOutputStream();
    _outputStream.init(_responseStream);

    /*
    // server/10a2
    if (! _hasWriter) {
      // jsp/0510 vs server/1b00
      // _responseStream.setOutputStreamOnly(true);
    }
    */
    // jsp/1cie, jsp/1civ
    // _responseStream.setEncoding(null);

    return _outputStream;
  }

  /**
   * Returns a PrintWriter with the proper character encoding for writing
   * text data to the client.
   */
  @Override
  public PrintWriter getWriter()
    throws IOException
  {
    if (_writer != null)
      return _writer;

    if (_outputStream != null) {
      if (_response.isClosed()) {
        // jsp/017o
        _writer = _response.getResponsePrintWriter();
        _writer.init(_responseStream);
        return _writer;
      }
      
      throw new IllegalStateException(L.l("getWriter() can't be called after getOutputStream()."));
    }

    String encoding = getCharacterEncoding();

    _writer = _response.getResponsePrintWriter();
    _writer.init(_responseStream);

    if (encoding != null) {
      _responseStream.setEncoding(encoding);
    }

    return _writer;
  }

  /**
   * Sets the output buffer size to <code>size</code>.  The servlet engine
   * may round the size up.
   *
   * @param size the new output buffer size.
   */
  @Override
  public void setBufferSize(int size)
  {
    _responseStream.setBufferSize(size);
  }

  /**
   * Returns the size of the output buffer.
   */
  @Override
  public int getBufferSize()
  {
    return _responseStream.getBufferSize();
  }

  // needed to support JSP
  public int getRemaining()
  {
    return _responseStream.getRemaining();
  }

  /**
   * Flushes the buffer to the client.
   */
  public void flushBuffer()
    throws IOException
  {
    // server/10sn
    _responseStream.flush();
  }

  /**
   * Returns true if some data has actually been send to the client.  The
   * data will be sent if the buffer overflows or if it's explicitly flushed.
   */
  @Override
  public final boolean isCommitted()
  {
    AbstractHttpResponse response = _response;
    
    if (response != null) {
      return response.isCommitted();
    }
    else {
      return true;
    }
  }

  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void reset()
  {
    reset(false);
  }

  /**
   * Clears the response for a forward()
   *
   * @param force if not true and the response stream has committed,
   *   throw the IllegalStateException.
   */
  void reset(boolean force)
  {
    if (! force && isCommitted()) {
      throw new IllegalStateException(L.l("response cannot be reset() after committed"));
    }

    _responseStream.clearBuffer();

    _status = 200;
    _statusMessage = "OK";

    _setCharEncoding = null;
    _charEncoding = null;
    _locale = null;

    _outputStream = null;
    _writer = null;

    try {
      _responseStream.setLocale(null);
      _responseStream.setEncoding(null);
    } catch (Exception e) {
    }

    if (_cookiesOut != null)
      _cookiesOut.clear();

    _response.reset();
  }

  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  @Override
  public void resetBuffer()
  {
    _responseStream.clearBuffer();

    // jsp/15ma, server/2h7m
    if (_responseStream.isCommitted())
      _responseStream.killCaching();

    /*
    if (_currentWriter instanceof JspPrintWriter)
      ((JspPrintWriter) _currentWriter).clear();
    */
  }

  /**
   * Explicitly sets the length of the result value.  Normally, the servlet
   * engine will handle this.
   */
  @Override
  public void setContentLength(int len)
  {
    if (_outputStream == null && _writer == null) {
      _response.setContentLength(len);
    }
  }

  /**
   * Explicitly sets the length of the result value.  Normally, the servlet
   * engine will handle this.
   */
  @Override
  public void setContentLength(long length)
  {
    if (_outputStream == null && _writer == null) {
      _response.setContentLength(length);
    }
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

  public void setLocale(Locale locale)
  {
    _locale = locale;

    if (_setCharEncoding == null && ! isCommitted()) {
      _charEncoding = getRequest().getWebApp().getLocaleEncoding(locale);
      // server/12n0
      // _setCharEncoding = _charEncoding;

      try {
        if (_charEncoding != null) {
          // _originalStream.setEncoding(_charEncoding);
          _responseStream.setEncoding(_charEncoding);
        }
      } catch (IOException e) {
      }
    }

    StringBuilder cb = new StringBuilder();
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

  //
  // proxy caching
  //

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
    killCache();
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
   * Sets the cache invocation to indicate that the response might be
   * cacheable.
   */
  @Override
  public void setCacheInvocation(AbstractCacheFilterChain cacheInvocation)
  {
    assert(_cacheInvocation == null || cacheInvocation == null);

    _cacheInvocation = cacheInvocation;
  }

  public final AbstractCacheFilterChain getCacheInvocation()
  {
    return _cacheInvocation;
  }
  
  @Override
  public boolean isCaching()
  {
    return _cacheInvocation != null;
  }

  /**
   * Set no cache w/o vary
   */
  public void setNoCacheUnlessVary(boolean isNoCacheUnlessVary)
  {
    _isNoCacheUnlessVary = isNoCacheUnlessVary;
  }

  /**
   * Return true if no-cache without var.
   */
  public boolean isNoCacheUnlessVary()
  {
    return _isNoCacheUnlessVary;
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
  public boolean isPrivateCache()
  {
    return ! _isCacheControl && _isPrivateCache;
  }

  /**
   * True if the application has a set a cache-control directive
   * that Resin doesn't understand.
   */
  public boolean isCacheControl()
  {
    return _isCacheControl;
  }

  /**
   * True if the application has a set a cache-control directive
   * that Resin doesn't understand.
   */
  public void setCacheControl(boolean isCacheControl)
  {
    // server/13d9
    _isCacheControl = isCacheControl;
    // killCache();
  }

  /**
   * Set if the page is non-cacheable.
   */
  public void setNoCache(boolean isNoCache)
  {
    _isNoCache = isNoCache;
    killCache();
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
  @Override
  public void killCache()
  {
    _isDisableCache = true;
    _responseStream.killCaching();
  }

  public boolean isDisableCache()
  {
    return _isDisableCache;
  }

  //
  // HttpServletResponse methods
  //

  /**
   * Sets the HTTP status
   *
   * @param code the HTTP status code
   */
  @Override
  public void setStatus(int code)
  {
    setStatus(code, null);
  }

  /**
   * Sets the HTTP status
   *
   * @param code the HTTP status code
   * @param message the HTTP status message
   */
  @Override
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
      message = _errors.get(code);

      if (message == null)
        message = L.l("Internal Server Error");
    }

    // server/2h0g
    if (code != SC_OK && code != SC_NOT_MODIFIED)
      killCache();
    
    if (code == SC_BAD_REQUEST || code == SC_SWITCHING_PROTOCOLS) {
      _request.killKeepalive("servletResponse: bad request: " + code + " " + message);
    }

    _status = code;
    _statusMessage = message;
  }

  /**
   * Sends an HTTP error page based on the status code
   *
   * @param code the HTTP status code
   */
  @Override
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
  @Override
  public void sendError(int code, String value)
    throws IOException
  {
    if (code == SC_NOT_MODIFIED && isProxyCacheFill()) {
      setStatus(code, value);

      if (handleNotModified()) {
        return;
      }
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

    WebApp webApp = getRequest().getWebApp();

    ErrorPageManager errorManager = null;
    if (webApp != null)
      errorManager = webApp.getErrorPageManager();
    else
      errorManager = getRequest().getServer().getErrorPageManager();

    setStatus(code, value);

    try {
      if (code == SC_NOT_MODIFIED || code == SC_NO_CONTENT) {
        _response.finishInvocation();
        return;
      }
      else if (errorManager != null) {
        // server/10su
        errorManager.sendError(_request, this, code, _statusMessage);

        // _request.killKeepalive();
        // close, but don't force a flush
        // XXX: finish(false);
        _response.finishInvocation();
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

      if (code == SC_NOT_FOUND) {
        s.println(L.l("{0} was not found on this server.",
                      HTTPUtil.encodeString(getRequest().getPageURI())));
      }
      else if (code == SC_SERVICE_UNAVAILABLE) {
        s.println(L.l("The server is temporarily unavailable due to maintenance downtime or excessive load."));
      }

      String version = null;

      if (webApp == null) {
      }
      else if (webApp.getServer() != null
               && webApp.getServer().getServerHeader() != null) {
        version = webApp.getServer().getServerHeader();
      }
      else if (CauchoSystem.isTesting()) {
      }
      else
        version = VersionFactory.getFullVersion();

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

    _request.killKeepalive("servlet error: " + code + " " + value);
    // close, but don't force a flush
    _response.finishInvocation();
  }
  
  private boolean isProxyCacheFill()
  {
    return true;
  }


  /**
   * Handle a SC_NOT_MODIFIED response.  If we've got a cache, fill the
   * results from the cache.
   *
   * @return true if we filled from the cache
   */
  boolean handleNotModified()
    throws IOException
  {
    if (_status != SC_NOT_MODIFIED) {
      return false;
    }
    else if (isProxyCacheFill()) {
      return true;
    }
    /*
    else if (isProxyCacheFill()) {
      if (isCommitted()) {
        return false;
      }

      // need to unclose because the not modified might be detected only
      // when flushing the data
      // _responseStream.clearClosed();

      AbstractCacheFilterChain cacheInvocation = _cacheInvocation;
      _cacheInvocation = null;

      // XXX: complications with filters
      if (cacheInvocation != null
          && cacheInvocation.fillFromCache(getRequest(), this,
                                           matchCacheEntry)) {
        matchCacheEntry.updateExpiresDate();

        _response.finishInvocation(); // Don't force a flush to avoid extra TCP packet
        return true;
      }
    }
  */
    // server/13dh
    else if (_cacheInvocation != null) {
      WebApp webApp = _request.getWebApp();

      long maxAge = webApp.getMaxAge(_request.getRequestURI());

      if (maxAge > 0 && ! containsHeader("Cache-Control")) {
        addHeader("Cache-Control", "max-age=" + (maxAge / 1000L));
      }
    }

    return false;
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
  @Override
  public void setContentType(String value)
  {
    if (isCommitted()) {
      return;
    }
    
    // jsp/0511
    if (_locale == null && _setCharEncoding == null) {
      _charEncoding = null;
    }

    if (value == null) {
      _contentType = null;
      return;
    }

    ContentType item = AbstractHttpResponse.parseContentType(value);

    _contentType = item.getContentType();
    
    String encoding = item.getEncoding();

    // server/172k
    // _setCharEncoding = encoding;

    if (encoding != null) {
      setCharacterEncoding(encoding);
    }
    else if (_charEncoding != null || _setCharEncoding != null) {
      return;
    }

    // XXX: conflict with servlet exception throwing order?
    try {
      encoding = getCharacterEncoding();

      _responseStream.setEncoding(encoding);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Gets the content type.
   */
  public String getContentType()
  {
    if (_contentType == null) {
      return null;
    }

    if (_setCharEncoding != null) {
      return _contentType + "; charset=" + _setCharEncoding;
    }
    
    String charEncoding = getCharacterEncoding();

    if (charEncoding != null
        && (_contentType.startsWith("text/")
            || _contentType.equals("application/json")
            || _contentType.startsWith("multipart/"))) {
      return _contentType + "; charset=" + charEncoding;
    }
    else {
      return _contentType;
    }
  }

  /**
   * Gets the content type.
   */
  public String getContentTypeImpl()
  {
    return _contentType;
  }

  /**
   * Gets the character encoding assigned by the developer. Used for JSP
   * encoding.
   */
  @Override
  public String getCharacterEncodingAssigned()
  {
    return _setCharEncoding;
  }

  /**
   * Gets the character encoding.
   */
  @Override
  public String getCharacterEncoding()
  {
    String charEncoding = _charEncoding;

    if (charEncoding == null) {
      charEncoding = _setCharEncoding;

      if (charEncoding == null) {
        /*
        String contentType = getContentTypeImpl();

        if (contentType == null
            || ! contentType.startsWith("text/")) {
          return null;
        }
        */
        
        WebApp webApp = _request.getWebApp();

        if (webApp != null) {
          if (webApp.getJsp() != null)
            charEncoding = webApp.getJsp().getCharacterEncoding();

          if (charEncoding == null)
            charEncoding = webApp.getCharacterEncoding();
        }

        if (charEncoding == null) {
          // server/085a
          charEncoding = "utf-8";
        }
      }
      
      _charEncoding = charEncoding;
    }

    return charEncoding;
  }

  /**
   * Gets the character encoding.
   */
  public String getCharacterEncodingImpl()
  {
    // server/172d
    // server/2u00
    
    String setCharEncoding = _setCharEncoding;
    if (setCharEncoding != null) {
      return setCharEncoding;
    }
    
    String contentType = getContentTypeImpl();
    
    if (contentType == null
        || contentType.startsWith("text/")
        || contentType.startsWith("multipart/")) {
      // server/1b5a, #4778
      return getCharacterEncoding();
    }
    else {
      return null;
    }
  }

  /**
   * Sets the character encoding.
   */
  @Override
  public void setCharacterEncoding(String encoding)
  {
    if (isCommitted()) {
      return;
    }

    if (_writer != null) {
      // server/172k

      if (encoding != null
          && _charEncoding != null
          && ! encoding.equalsIgnoreCase(_charEncoding)) {
        if (log.isLoggable(Level.FINE))
          log.fine(_request.getRequestURI() + ": setEncoding(" + encoding + ") ignored because writer already initialized with charset=" + _charEncoding);
      }

      return;
    }

    if (encoding == null
        || encoding.equals("ISO-8859-1")
        || encoding.equals("")) {
      _setCharEncoding = "iso-8859-1";
    }
    else
      _setCharEncoding = encoding;

    _charEncoding = _setCharEncoding;

    try {
      _responseStream.setEncoding(encoding);
    } catch (Exception e) {
      log.log(Level.INFO, e.toString(), e);
    }
  }

  /**
   * Sends a redirect to the browser.  If the URL is relative, it gets
   * combined with the current url.
   *
   * @param url the possibly relative url to send to the browser
   */
  @Override
  public void sendRedirect(String url)
    throws IOException
  {
    if (url == null)
      throw new NullPointerException();

    if (isCommitted())
      throw new IllegalStateException(L.l("Can't sendRedirect() after data has committed to the client."));

    _responseStream.clearBuffer();

    // server/10c4
    // reset();
    resetBuffer();

    setStatus(SC_MOVED_TEMPORARILY);

    String encoding = getCharacterEncoding();
    boolean isLatin1 = "iso-8859-1".equals(encoding);
    
    String path = encodeAbsoluteRedirect(url);

    setHeader("Location", path);
    
    if (isLatin1)
      setHeader("Content-Type", "text/html; charset=iso-8859-1");
    else
      setHeader("Content-Type", "text/html; charset=utf-8");

    String msg = "The URL has moved <a href=\"" + path + "\">here</a>";

    // The data is required for some WAP devices that can't handle an
    // empty response.
    if (_writer != null) {
      _writer.println(msg);
    }
    else {
      ServletOutputStream out = getOutputStream();
      out.println(msg);
    }
    // closeConnection();

    _request.saveSession(); // #503

    close();
  }
  
  public String encodeAbsoluteRedirect(String url)
  {
    String path = getAbsolutePath(url);

    // Bug #3051
    String encoding = getCharacterEncoding();

    boolean isLatin1 = "iso-8859-1".equals(encoding);

    return escapeUrl(path, isLatin1);
  }
  
  private String escapeUrl(String path, boolean isLatin1)
  {
    StringBuilder cb = new StringBuilder();

    for (int i = 0; i < path.length(); i++) {
      char ch = path.charAt(i);

      if (ch == '<')
        cb.append("%3c");
      else if (ch == '"') {
        cb.append("%22");
      }
      else if (ch < 0x80)
        cb.append(ch);
      else if (isLatin1) {
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

    return cb.toString();
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

    WebApp webApp = getRequest().getWebApp();

    String hostPrefix = null;
    String host = _request.getHeader("Host");
    String serverName = webApp.getHostName();

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
    String contextPath = _request.getContextPath();
    String queryString = null;

    int p = path.indexOf('?');
    if (p > 0) {
      queryString = path.substring(p + 1);
      path = path.substring(0, p);
    }

    if (uri.equals(contextPath)) {
      path = uri + "/" + path;
    }
    else {
      p = uri.lastIndexOf('/');

      if (p >= 0)
        path = uri.substring(0, p + 1) + path;
    }

    try {
      if (queryString != null)
        return hostPrefix + webApp.getInvocationDecoder().normalizeUri(path) + '?' + queryString;
      else
        return hostPrefix + webApp.getInvocationDecoder().normalizeUri(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addHex(StringBuilder cb, int hex)
  {
    int d1 = (hex >> 4) & 0xf;
    int d2 = (hex) & 0xf;

    cb.append('%');
    cb.append(d1 < 10 ? (char) (d1 + '0') : (char) (d1 - 10 + 'a'));
    cb.append(d2 < 10 ? (char) (d2 + '0') : (char) (d2 - 10 + 'a'));
  }

  /**
   * Sets a header.  This will override a previous header
   * with the same name.
   *
   * @param name the header name
   * @param value the header value
   */
  @Override
  public void setHeader(String name, String value)
  {
    _response.setHeader(name, value);
  }

  /**
   * Adds a header.  If another header with the same name exists, both
   * will be sent to the client.
   *
   * @param name the header name
   * @param value the header value
   */
  public void addHeader(String name, String value)
  {
    _response.addHeader(name, value);
  }

  /**
   * Returns true if the output headers include <code>name</code>
   *
   * @param name the header name to test
   */
  public boolean containsHeader(String name)
  {
    return _response.containsHeader(name);
  }

  /**
   * Sets a header by converting a date to a string.
   *
   * <p>To set the page to expire in 15 seconds use the following:
   * <pre><code>
   * long now = System.currentTime();
   * response.setDateHeader("Expires", now + 15000);
   * </code></pre>
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void setDateHeader(String name, long date)
  {
    _response.setDateHeader(name, date);
  }

  /**
   * Adds a header by converting a date to a string.
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void addDateHeader(String name, long date)
  {
    _response.addDateHeader(name, date);
  }

  /**
   * Sets a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void setIntHeader(String name, int value)
  {
    _response.setIntHeader(name, value);
  }

  /**
   * Adds a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void addIntHeader(String name, int value)
  {
    _response.addIntHeader(name, value);
  }

  /**
   * Adds a cookie to the response.
   *
   * @param cookie the response cookie
   */
  @Override
  public void addCookie(Cookie cookie)
  {
    _request.setHasCookie();

    if (cookie == null)
      return;

    if (_cookiesOut == null)
      _cookiesOut = new ArrayList<Cookie>();

    _cookiesOut.add(cookie);
  }

  public Cookie getCookie(String name)
  {
    if (_cookiesOut == null)
      return null;

    for (int i = _cookiesOut.size() - 1; i >= 0; i--) {
      Cookie cookie = _cookiesOut.get(i);

      if (cookie.getName().equals(name))
        return cookie;
    }

    return null;
  }

  public ArrayList<Cookie> getCookies()
  {
    return _cookiesOut;
  }

  public String getSessionId()
  {
    return _sessionId;
  }

  @Override
  public void setSessionId(String id)
  {
    _sessionId = id;

    // XXX: server/1315 vs server/0506 vs server/170k vs server/01r(e,f)
    // could also set the nocache=JSESSIONID
    WebApp webApp = _request.getWebApp();
    
    if (webApp != null && webApp.getSessionManager().enableSessionCookies()) {
      setPrivateOrResinCache(true);
    }
  }

  protected void addServletCookie(WebApp webApp)
  {
      /* XXX:
      if (_sessionId != null && ! _hasSessionCookie) {
        _hasSessionCookie = true;

        addServletCookie(webApp);
      }
      */

    // server/003a
    if (_sessionId != null
        && webApp != null
        && webApp.getSessionManager().enableSessionCookies()) {
      addCookie(createServletCookie(webApp));
    }
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
    String domain = webApp.generateCookieDomain(_request);
    if (domain != null)
      cookie.setDomain(domain);
    long maxAge = manager.getCookieMaxAge();
    if (maxAge > 0)
      cookie.setMaxAge((int) (maxAge / 1000));
    cookie.setPath(manager.getPath());

    if (manager.getComment() != null)
      cookie.setComment(manager.getComment());

    cookie.setPort(manager.getCookiePort());

    if (manager.isSecure()) {
      // server/12zc (tck) vs server/01io (#4372)
      /*
      if (_request.isSecure())
        cookie.setSecure(true);
        */

      cookie.setSecure(true);
    }
    else if (manager.isCookieSecure()) {
      if (_request.isSecure())
        cookie.setSecure(true);
    }
    
    if (manager.isCookieHttpOnly())
      cookie.setHttpOnly(true);

    return cookie;
  }

  /**
   * Encodes session information in a URL. Calling this will enable
   * sessions for users who have disabled cookies.
   *
   * @param string the url to encode
   * @return a url with session information encoded
   */
  @Override
  public String encodeURL(String string)
  {
    HttpServletRequestImpl request = getRequest();

    WebApp webApp = request.getWebApp();

    if (webApp == null)
      return string;

    if (request.isRequestedSessionIdFromCookie())
      return string;

    HttpSession session = request.getSession(false);
    if (session == null)
      return string;

    SessionManager sessionManager = webApp.getSessionManager();
    if (! sessionManager.enableSessionUrls())
      return string;

    StringBuilder cb = new StringBuilder();

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
        cb.append(string, p, string.length());
      }
      else if ((p = string.indexOf('#')) >= 0) {
        cb.append(string, 0, p);
        cb.append(sessionManager.getSessionPrefix());
        cb.append(session.getId());
        cb.append(string, p, string.length());
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

  @Override
  public String encodeRedirectURL(String string)
  {
    String path = encodeURL(string);

    String encoding = getCharacterEncoding();

    boolean isLatin1 = "iso-8859-1".equals(encoding);

    // server/1u3k, #4699
    return escapeUrl(path, isLatin1);
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
  @Override
  public String encodeUrl(String string)
  {
    return encodeURL(string);
  }

  //
  // CauchoResponse methods
  //

  @Override
  public AbstractResponseStream getResponseStream()
  {
    // jsp/(1cie, 1civ, 1ciw, 1cir), server/053y
    if (_responseStream.getEncoding() == null) {
      String encoding = getCharacterEncoding();
      // server/053y
      try {
        _responseStream.setEncoding(encoding);
      } catch (Exception e) {
      }
    }

    return _responseStream;
  }

  @Override
  public void setResponseStream(AbstractResponseStream responseStream)
  {
    _responseStream = responseStream;

    if (_outputStream != null)
      _outputStream.init(responseStream);

    if (_writer != null)
      _writer.init(responseStream);
  }

  @Override
  public boolean isCauchoResponseStream()
  {
    return _responseStream.isCauchoResponseStream();
  }

  /*
  public void setFlushBuffer(FlushBuffer out)
  {
    _response.setFlushBuffer(out);
  }

  public FlushBuffer getFlushBuffer()
  {
    return _response.getFlushBuffer();
  }
  */

  @Override
  public String getHeader(String key)
  {
    return _response.getHeader(key);
  }

  ArrayList<String> getHeaderKeys()
  {
    return _response.getHeaderKeys();
  }

  ArrayList<String> getHeaderValues()
  {
    return _response.getHeaderValues();
  }

  @Override
  public void setFooter(String key, String value)
  {
    _response.setFooter(key, value);
  }

  @Override
  public void addFooter(String key, String value)
  {
    _response.addFooter(key, value);
  }

  // XXX: really close invocation

  @Override
  public void close()
    throws IOException
  {
    // tck - jsp include
    AbstractHttpResponse response = _response;
    
    if (response != null) {
      response.close();
    }
  }

  /**
   * When set to true, RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  @Override
  public void setForbidForward(boolean forbid)
  {
    _forbidForward = forbid;
  }

  /**
   * Returns true if RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  @Override
  public boolean getForbidForward()
  {
    return _forbidForward;
  }

  @Override
  public boolean hasError()
  {
    return _hasError;
  }

  @Override
  public void setHasError(boolean error)
  {
    _hasError = error;
  }

  //
  // HttpServletRequestImpl methods
  //

  @Override
  public ServletResponse getResponse()
  {
    return null;
  }

  /*
  public ServletResponse getResponse()
  {
    AbstractHttpResponse response = _response;

    if (response == null)
      throw new IllegalStateException(L.l("{0} is not longer valid because it has already been closed.",
                                          this));

    return response;
  }
  */

  @Override
  public AbstractHttpResponse getAbstractHttpResponse()
  {
    return _response;
  }

  @Override
  public int getStatus()
  {
    return _status;
  }

  public String getStatusMessage()
  {
    return _statusMessage;
  }

  public Collection<String> getHeaders(String name)
  {
    // XXX: test
    return _response.getHeaders(name);
  }

  public Collection<String> getHeaderNames()
  {
    // XXX: test
    return _response.getHeaderNames();
  }

  public void setForwardEnclosed(boolean isForwardEnclosed) {
  }

  public boolean isForwardEnclosed()
  {
    return false;
  }

  public void closeImpl()
    throws IOException
  {
    AbstractHttpResponse response = _response;

    _response = null;

    if (response != null)
      response.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }

  static {
    _errors = new HashMap<Integer,String>();
    _errors.put(100, "Continue");
    _errors.put(101, "Switching Protocols");
    _errors.put(200, "OK");
    _errors.put(201, "Created");
    _errors.put(202, "Accepted");
    _errors.put(203, "Non-Authoritative Information");
    _errors.put(204, "No Content");
    _errors.put(205, "Reset Content");
    _errors.put(206, "Partial Content");
    _errors.put(300, "Multiple Choices");
    _errors.put(301, "Moved Permanently");
    _errors.put(302, "Found");
    _errors.put(303, "See Other");
    _errors.put(304, "Not Modified");
    _errors.put(305, "Use Proxy");
    _errors.put(307, "Temporary Redirect");
    _errors.put(400, "Bad Request");
    _errors.put(401, "Unauthorized");
    _errors.put(402, "Payment Required");
    _errors.put(403, "Forbidden");
    _errors.put(404, "Not Found");
    _errors.put(405, "Method Not Allowed");
    _errors.put(406, "Not Acceptable");
    _errors.put(407, "Proxy Authentication Required");
    _errors.put(408, "Request Timeout");
    _errors.put(409, "Conflict");
    _errors.put(410, "Gone");
    _errors.put(411, "Length Required");
    _errors.put(412, "Precondition Failed");
    _errors.put(413, "Request Entity Too Large");
    _errors.put(414, "Request-URI Too Long");
    _errors.put(415, "Unsupported Media Type");
    _errors.put(416, "Requested Range Not Satisfiable");
    _errors.put(417, "Expectation Failed");
    _errors.put(500, "Internal Server Error");
    _errors.put(501, "Not Implemented");
    _errors.put(502, "Bad Gateway");
    _errors.put(503, "Service Temporarily Unavailable");
    _errors.put(504, "Gateway Timeout");
    _errors.put(505, "Http Version Not Supported");
  }
}
