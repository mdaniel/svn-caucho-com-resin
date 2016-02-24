/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.cache.EntryHttpCacheBase;
import com.caucho.v5.http.cache.FilterChainHttpCacheBase;
import com.caucho.v5.http.webapp.ErrorPageManager;
import com.caucho.v5.http.webapp.WebAppResinBase;

public class ResponseCauchoWrapper implements ResponseCaucho {
  private static final Logger log
    = Logger.getLogger(ResponseCauchoWrapper.class.getName());

  private final RequestCaucho _request;

  // the wrapped response
  private HttpServletResponse _response;

  public ResponseCauchoWrapper()
  {
    _request = null;
  }

  public ResponseCauchoWrapper(RequestCaucho request)
  {
    _request = request;
  }

  public ResponseCauchoWrapper(RequestCaucho request,
                               HttpServletResponse response)
  {
    _request = request;

    if (response == null)
      throw new IllegalArgumentException();

    _response = response;
  }

  public void setResponse(HttpServletResponse response)
  {
    _response = response;
  }

  protected RequestCaucho getRequest()
  {
    return _request;
  }

  //
  // ServletResponse
  //

  @Override
  public void setContentType(String type)
  {
    _response.setContentType(type);
  }

  public String getContentType()
  {
    return _response.getContentType();
  }

  public String getCharacterEncoding()
  {
    return _response.getCharacterEncoding();
  }

  public void setCharacterEncoding(String charset)
  {
    _response.setCharacterEncoding(charset);
  }

  public void setLocale(Locale locale)
  {
    _response.setLocale(locale);
  }

  public Locale getLocale()
  {
    return _response.getLocale();
  }

  public ServletOutputStream getOutputStream()
    throws IOException
  {
    return _response.getOutputStream();
  }

  public PrintWriter getWriter()
    throws IOException
  {
    return _response.getWriter();
  }

  public void setBufferSize(int size)
  {
    _response.setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _response.getBufferSize();
  }

  public void flushBuffer()
    throws IOException
  {
    _response.flushBuffer();
  }

  public boolean isCommitted()
  {
    return _response.isCommitted();
  }

  public void reset()
  {
    _response.reset();
  }

  @Override
  public void resetBuffer()
  {
    _response.resetBuffer();
  }

  @Override
  public void setContentLength(int len)
  {
    _response.setContentLength(len);
  }

  @Override
  public void setContentLengthLong(long length)
  {
    _response.setContentLengthLong(length);
  }

  //
  // HttpServletResponse
  //

  @Override
  public void setStatus(int sc)
  {
    _response.setStatus(sc);
  }

  @Override
  public void sendError(int sc, String msg)
    throws IOException
  {
    if (! sendInternalError(sc, msg)) {
      _response.sendError(sc, msg);
    }
  }

  @Override
  public void sendError(int sc)
    throws IOException
  {
    sendError(sc, null);
  }

  protected boolean sendInternalError(int sc, String msg)
  {
    if (sc == HttpServletResponse.SC_NOT_MODIFIED)
      return false;
    
    // server/10su
    RequestCaucho req = _request;

    if (req == null)
      return false;

    WebAppResinBase webApp = req.getWebApp();

    if (webApp == null)
      return false;

    ErrorPageManager errorManager = webApp.getErrorPageManager();

    if (errorManager == null)
      return false;

    if (msg != null)
      setStatus(sc, msg);
    else
      setStatus(sc);

    try {
      errorManager.sendError(_request, this, sc, getStatusMessage());

      killCache();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }

    return true;
  }

  @Override
  public void sendRedirect(String location)
    throws IOException
  {
    _response.sendRedirect(location);
  }

  public void setHeader(String name, String value)
  {
    _response.setHeader(name, value);
  }

  public void addHeader(String name, String value)
  {
    _response.addHeader(name, value);
  }

  public boolean containsHeader(String name)
  {
    return _response.containsHeader(name);
  }

  public void setDateHeader(String name, long date)
  {
    _response.setDateHeader(name, date);
  }

  public void addDateHeader(String name, long date)
  {
    _response.addDateHeader(name, date);
  }

  public void setIntHeader(String name, int value)
  {
    _response.setIntHeader(name, value);
  }

  public void addIntHeader(String name, int value)
  {
    _response.addIntHeader(name, value);
  }

  public void addCookie(Cookie cookie)
  {
    _response.addCookie(cookie);
  }

  public String encodeURL(String url)
  {
    return _response.encodeURL(url);
  }

  public String encodeRedirectURL(String name)
  {
    return _response.encodeRedirectURL(name);
  }

  @SuppressWarnings("deprecation")
  public void setStatus(int sc, String msg)
  {
    _response.setStatus(sc, msg);
  }

  @SuppressWarnings("deprecation")
  public String encodeUrl(String url)
  {
    return _response.encodeUrl(url);
  }

  @SuppressWarnings("deprecation")
  public String encodeRedirectUrl(String url)
  {
    return _response.encodeRedirectUrl(url);
  }

  public int getStatus()
  {
    return _response.getStatus();
  }

  public String getHeader(String name)
  {
      try {
          return _response.getHeader(name);
      } catch (AbstractMethodError ame) { // #4990 http://bugs.caucho.com/view.php?id=4990
          return null;
      }
  }

  public Collection<String> getHeaders(String name)
  {
    return _response.getHeaders(name);
  }

  public Collection<String> getHeaderNames()
  {
    return _response.getHeaderNames();
  }

  //
  // CauchoResponse
  //

  public OutResponseBase2 getResponseStream()
  {
    if (_response instanceof ResponseCaucho) {
      ResponseCaucho cResponse = (ResponseCaucho) _response;

      return cResponse.getResponseStream();
    }
    else
      return null;
  }

  public void setResponseStream(OutResponseBase2 os)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setResponseStream(os);
  }

  public boolean isCauchoResponseStream()
  {
    if (_response instanceof ResponseCaucho)
      return ((ResponseCaucho) _response).isCauchoResponseStream();

    return false;
  }

  public void setFooter(String key, String value)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setFooter(key, value);
  }

  public void addFooter(String key, String value)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).addFooter(key, value);
  }

  public void close() throws IOException
  {
    //support for spring.MockHttpServletResponse
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).close();
  }

  @Override
  public boolean getForbidForward()
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).getForbidForward();

    return false;
  }

  @Override
  public void setForbidForward(boolean forbid)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setForbidForward(forbid);
  }

  @Override
  public String getStatusMessage()
  {
    if (_response instanceof ResponseCaucho)
      return ((ResponseCaucho) _response).getStatusMessage();
    else
      return null;
  }

  @Override
  public boolean hasError()
  {
    if (_response instanceof ResponseCaucho)
      return ((ResponseCaucho) _response).hasError();
    else
      return false;
  }

  @Override
  public void setHasError(boolean error)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setHasError(error);
  }

  @Override
  public void setSessionId(String id)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setSessionId(id);
  }

  @Override
  public void killCache()
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).killCache();
  }

  @Override
  public void completeCache()
  {
    getResponseStream().completeCache();
  }

  @Override
  public void setNoCache(boolean killCache)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setNoCache(killCache);
  }

  public void setPrivateCache(boolean isPrivate)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setPrivateCache(isPrivate);
  }

  public boolean isNoCacheUnlessVary()
  {
    ResponseCaucho cRes = getCauchoResponse();

    if (cRes != null)
      return cRes.isNoCacheUnlessVary();
    else
      return false;
  }

  public ResponseCaucho getCauchoResponse()
  {
    if (_response instanceof ResponseCaucho)
      return (ResponseCaucho) _response;
    else
      return null;
  }

  public RequestHttpBase getAbstractHttpResponse()
  {
    /*
    if (_response instanceof ResponseCaucho)
      return ((ResponseCaucho) _response).getAbstractHttpResponse();
      */

    return null;
  }

  @Override
  public void setCacheInvocation(FilterChainHttpCacheBase cacheFilterChain)
  {
  }

  @Override
  public boolean isCaching()
  {
    return false;
  }

  public void setMatchCacheEntry(EntryHttpCacheBase cacheEntry)
  {
  }

  @Override
  public ServletResponse getResponse()
  {
    return _response;
  }

  @Override
  public void setForwardEnclosed(boolean isForwardEnclosed)
  {
  }

  @Override
  public boolean isForwardEnclosed()
  {
    return false;
  }

  @Override
  public void writeHeaders(int length) throws IOException
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }
}
