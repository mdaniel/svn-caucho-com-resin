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
abstract public class AbstractHttpResponse {
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

  protected final AbstractHttpRequest _request;

  protected String _contentType;
  protected String _contentPrefix;
  protected String _charEncoding;
  protected boolean _hasCharEncoding;
  
  protected final ArrayList<String> _headerKeys = new ArrayList<String>();
  protected final ArrayList<String> _headerValues = new ArrayList<String>();
  
  protected final ArrayList<String> _footerKeys = new ArrayList<String>();
  protected final ArrayList<String> _footerValues = new ArrayList<String>();

  // the raw output stream.
  private final WriteStream _rawWrite;
  
  private final AbstractResponseStream _responseStream;
  
  private final ServletOutputStreamImpl _responseOutputStream
    = new ServletOutputStreamImpl();
  private final ResponseWriter _responsePrintWriter
    = new ResponseWriter();

  private HttpBufferStore _bufferStore;

  // any stream that needs flusing before getting the writer.
  private FlushBuffer _flushBuffer;

  private boolean _isHeaderWritten;
  private boolean _isChunked;
  private boolean _isClientDisconnect;
  protected final QDate _calendar = new QDate(false);

  protected final CharBuffer _cb = new CharBuffer();
  protected final char [] _headerBuffer = new char[256];

  private boolean _hasSessionCookie;

  protected boolean _disableHeaders;
  protected boolean _disableCaching;
  protected long _contentLength;
  protected boolean _isClosed;
  protected boolean _hasSentLog;

  protected boolean _forbidForward;
  protected boolean _hasError;

  protected AbstractHttpResponse()
  {
    _rawWrite = null;

    _request = null;
    _responseStream = createResponseStream();
  }

  protected AbstractHttpResponse(AbstractHttpRequest request,
                                 WriteStream rawWrite)
  {
    if (rawWrite == null)
      throw new NullPointerException();
    
    _rawWrite = rawWrite;

    _request = request;
    
    _responseStream = createResponseStream();
  }
  
  protected AbstractResponseStream
    createResponseStream(HttpBufferStore bufferStore)
  {
    ResponseStream responseStream = new ResponseStream();

    responseStream.setResponse(this);
    responseStream.init(_rawWrite);

    return responseStream;
  }
  
  /**
   * If set true, client disconnect exceptions are no propagated to the
   * server code.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return _request.isIgnoreClientDisconnect();
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
    _request.clientDisconnect();
    
    _isClientDisconnect = true;
  }

  /**
   * Return true for the top request.
   */
  public boolean isTop()
  {
    if (_request instanceof AbstractHttpRequest)
      return ((AbstractHttpRequest) _request).isTop();
    else
      return false;
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
  /*
  public void init(WriteStream stream)
  {
    _rawWrite = stream;
    
    if (_originalResponseStream instanceof ResponseStream)
      ((ResponseStream) _originalResponseStream).init(_rawWrite);

  }
  */

  /**
   * Returns the corresponding request.
   */
  public AbstractHttpRequest getRequest()
  {
    return _request;
  }

  protected WriteStream getRawWrite()
  {
    return _rawWrite;
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
  public void startRequest(HttpBufferStore bufferStore)
    throws IOException
  {
    _bufferStore = bufferStore;
    
    _headerKeys.clear();
    _headerValues.clear();
    
    _footerKeys.clear();
    _footerValues.clear();

    _hasSessionCookie = false;

    _responseStream.start();

    _isHeaderWritten = false;
    _isChunked = false;
    _isClientDisconnect = false;
    _charEncoding = null;
    _hasCharEncoding = false;
    _contentType = null;
    _contentPrefix = null;
    
    _flushBuffer = null;

    _contentLength = -1;
    _disableHeaders = false;
    _isClosed = false;
    _hasSentLog = false;

    _forbidForward = false;
  }

  protected AbstractResponseStream createResponseStream()
  {
    ResponseStream responseStream = new ResponseStream(this);

    responseStream.init(_rawWrite);

    return responseStream;
  }

  /**
   * For a HEAD request, the response stream should write no data.
   */
  protected void setHead()
  {
    _responseStream.setHead();
  }

  /**
   * For a HEAD request, the response stream should write no data.
   */
  protected final boolean isHead()
  {
    return _responseStream.isHead();
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
    // XXX: server/13w0 for _isHeaderWritten because the Expires in caching
    // occurs after the output fills (committed), which contradicts the tck
    // requirements
    if (isCommitted() && ! _isHeaderWritten) {
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
    // server/05e8 (tck)
    if (isCommitted()) {
      return;
    }

    addHeaderImpl(key, value);
  }

  /**
   * Adds a new header.  If an old header with that name exists,
   * both headers are output.
   *
   * @param key the header key.
   * @param value the header value.
   */
  public void addHeaderImpl(String key, String value)
  {
    if (_disableHeaders)
      return;

    if (setSpecial(key, value))
      return;

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
      /*
      if (value.startsWith("max-age")) {
      }
      else if (value.equals("x-anonymous")) {
      }
      else
	_hasCacheControl = true;
      */
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

  public ArrayList<String> getHeaderKeys()
  {
    return _headerKeys;
  }

  public ArrayList<String> getHeaderValues()
  {
    return _headerValues;
  }

  public Iterable<String> getHeaders(String name)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public Iterable<String> getHeaderNames()
  {
    throw new UnsupportedOperationException("unimplemented");
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

  void setContentType(String contentType)
  {
    _contentType = contentType;
  }

  void setCharEncoding(String charEncoding)
  {
    _charEncoding = charEncoding;
  }

  String getRealCharacterEncoding()
  {
    return _charEncoding;
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

  protected boolean hasFooter()
  {
    return _footerKeys.size() > 0;
  }

  /**
   * Gets the response stream.
   */
  protected AbstractResponseStream getResponseStream()
  {
    return _responseStream;
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

  protected ServletOutputStreamImpl getResponseOutputStream()
  {
    return _responseOutputStream;
  }

  protected ResponseWriter getResponsePrintWriter()
  {
    return _responsePrintWriter;
  }

  /**
   * Returns the parent writer.
   */
  public PrintWriter getNextWriter()
  {
    return null;
  }

  /*
   * jsdk 2.2
   */

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
    if (_responseStream.isCommitted())
      return true;

    if (_contentLength >= 0
	&& _contentLength <= _responseStream.getContentLength()) {
      return true;
    }

    return false;
  }

  protected void reset()
  {
    _headerKeys.clear();
    _headerValues.clear();

    _contentLength = -1;
  }

  // XXX: hack to deal with forwarding
  /*
  public void clearBuffer()
  {
    _responseStream.clearBuffer();
  }
  */

  /**
   * Returns the number of bytes sent to the output.
   */
  public int getContentLength()
  {
    return _responseStream.getContentLength();
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

    HttpServletRequestImpl req = _request.getRequestFacade();
    HttpServletResponseImpl res = _request.getResponseFacade();

    if (res == null)
      return false;

    // server/1373 for getBufferSize()
    boolean canCache = res.startCaching(true);

    _isHeaderWritten = true;
    boolean isHead = false;
    
    if (_request.getMethod().equals("HEAD")) {
      isHead = true;
      _responseStream.setHead();
    }

    WebApp webApp = _request.getWebApp();

    int statusCode = res.getStatus();
    
    int majorCode = statusCode / 100;

    if (webApp != null) {
      if (majorCode == 5)
	webApp.addStatus500();
    }

    if (req != null) {
      HttpSession session = req.getMemorySession();
      if (session instanceof SessionImpl)
        ((SessionImpl) session).saveBeforeHeaders();

      res.addServletCookie(webApp);
      /* XXX:
      if (_sessionId != null && ! _hasSessionCookie) {
        _hasSessionCookie = true;

        addServletCookie(webApp);
      }
      */
    }

    _isChunked = writeHeadersInt(os, length, isHead);

    return _isChunked;
  }

  abstract protected boolean writeHeadersInt(WriteStream os,
					     int length,
					     boolean isHead)
    throws IOException;

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
      conn = _request.getConnection();

      /* XXX:
      if (_statusCode == SC_NOT_MODIFIED && _request.isInitial()) {
	handleNotModified(_isTopCache);
      }
      if (_statusCode == SC_NOT_MODIFIED) {
	handleNotModified(_isTopCache);
      }
      */
      
      if (_responseStream == null) {
      }
      else if (isClose) {
	_responseStream.close();
	finishResponseStream(isClose);
      }
      /*
      else if (_responseStream != _originalResponseStream) {
	_responseStream.finish();
      }
      */
      else {
	_responseStream.flush();
	finishResponseStream(isClose);
      }

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

  protected void finishResponseStream(boolean isClose)
    throws IOException
  {
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
      AbstractHttpRequest request = _request;
	
      conn = request.getConnection();

      try {
        request.skip();
      } catch (BadRequestException e) {
        log.warning(e.toString());
        log.log(Level.FINE, e.toString(), e);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      // XXX: finishCache();
      
      // include() files finish too, but shouldn't force a flush, hence
      // flush is false
      // Never send flush?
      _responseStream.close();

      _isClosed = true;

      if (_rawWrite != null)
	_rawWrite.flushBuffer();
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
    }
  }

  TempBuffer getBuffer()
  {
    return _bufferStore.getTempBuffer();
  }

  protected final QDate getCalendar()
  {
    return _calendar;
  }

  protected void free()
  {
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
