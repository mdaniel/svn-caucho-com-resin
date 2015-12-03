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

import com.caucho.server.httpcache.AbstractCacheEntry;
import com.caucho.server.httpcache.AbstractCacheFilterChain;
import com.caucho.server.webapp.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class CauchoDispatchResponse extends AbstractCauchoResponse {
  private static final Logger log
    = Logger.getLogger(CauchoDispatchResponse.class.getName());

  private final CauchoRequest _request;

  // the wrapped response
  private HttpServletResponse _response;

  public CauchoDispatchResponse()
  {
    _request = null;
  }

  public CauchoDispatchResponse(CauchoRequest request)
  {
    _request = request;
  }

  public CauchoDispatchResponse(CauchoRequest request,
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

  protected CauchoRequest getRequest()
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

  @Override
  public String getContentType()
  {
    return _response.getContentType();
  }

  @Override
  public void setContentLength(long length)
  {
    if (_response instanceof CauchoResponse) {
      CauchoResponse cRes = (CauchoResponse) _response;
      
      cRes.setContentLength(length);
    }
    else if (length <= Integer.MAX_VALUE) { 
      _response.setContentLength((int) length);
    }
  }

  @Override
  public String getCharacterEncoding()
  {
    return _response.getCharacterEncoding();
  }

  @Override
  public void setCharacterEncoding(String charset)
  {
    _response.setCharacterEncoding(charset);
  }

  @Override
  public void setLocale(Locale locale)
  {
    _response.setLocale(locale);
  }

  @Override
  public Locale getLocale()
  {
    return _response.getLocale();
  }

  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    return _response.getOutputStream();
  }

  @Override
  public PrintWriter getWriter()
    throws IOException
  {
    return _response.getWriter();
  }

  @Override
  public void setBufferSize(int size)
  {
    _response.setBufferSize(size);
  }

  @Override
  public int getBufferSize()
  {
    return _response.getBufferSize();
  }

  @Override
  public void flushBuffer()
    throws IOException
  {
    _response.flushBuffer();
  }

  @Override
  public boolean isCommitted()
  {
    return _response.isCommitted();
  }

  @Override
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
    CauchoRequest req = _request;

    if (req == null)
      return false;

    WebApp webApp = req.getWebApp();

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

  @Override
  public void setHeader(String name, String value)
  {
    _response.setHeader(name, value);
  }

  @Override
  public void addHeader(String name, String value)
  {
    _response.addHeader(name, value);
  }

  @Override
  public boolean containsHeader(String name)
  {
    return _response.containsHeader(name);
  }

  @Override
  public void setDateHeader(String name, long date)
  {
    _response.setDateHeader(name, date);
  }

  @Override
  public void addDateHeader(String name, long date)
  {
    _response.addDateHeader(name, date);
  }

  @Override
  public void setIntHeader(String name, int value)
  {
    _response.setIntHeader(name, value);
  }

  @Override
  public void addIntHeader(String name, int value)
  {
    _response.addIntHeader(name, value);
  }

  @Override
  public void addCookie(Cookie cookie)
  {
    _response.addCookie(cookie);
  }

  @Override
  public String encodeURL(String url)
  {
    return _response.encodeURL(url);
  }

  @Override
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

  @Override
  public int getStatus()
  {
    return _response.getStatus();
  }

  @Override
  public String getHeader(String name)
  {
      try {
          return _response.getHeader(name);
      } catch (AbstractMethodError ame) { // #4990 http://bugs.caucho.com/view.php?id=4990
          return null;
      }
  }

  @Override
  public Collection<String> getHeaders(String name)
  {
    return _response.getHeaders(name);
  }

  @Override
  public Collection<String> getHeaderNames()
  {
    return _response.getHeaderNames();
  }

  //
  // CauchoResponse
  //

  @Override
  public AbstractResponseStream getResponseStream()
  {
    if (_response instanceof CauchoResponse) {
      CauchoResponse cResponse = (CauchoResponse) _response;

      return cResponse.getResponseStream();
    }
    else
      return null;
  }

  @Override
  public void setResponseStream(AbstractResponseStream os)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setResponseStream(os);
  }

  @Override
  public boolean isCauchoResponseStream()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).isCauchoResponseStream();

    return false;
  }

  @Override
  public void setFooter(String key, String value)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setFooter(key, value);
  }

  @Override
  public void addFooter(String key, String value)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).addFooter(key, value);
  }

  @Override
  public void close() throws IOException
  {
    //support for spring.MockHttpServletResponse
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).close();
  }

  @Override
  public boolean getForbidForward()
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).getForbidForward();

    return false;
  }

  @Override
  public void setForbidForward(boolean forbid)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setForbidForward(forbid);
  }

  @Override
  public String getStatusMessage()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getStatusMessage();
    else
      return null;
  }

  @Override
  public boolean hasError()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).hasError();
    else
      return false;
  }

  @Override
  public void setHasError(boolean error)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setHasError(error);
  }

  @Override
  public void setSessionId(String id)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setSessionId(id);
  }

  @Override
  public void killCache()
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).killCache();
  }

  @Override
  public void completeCache()
  {
    getResponseStream().completeCache();
  }

  @Override
  public void setNoCache(boolean killCache)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setNoCache(killCache);
  }

  @Override
  public void setPrivateCache(boolean isPrivate)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setPrivateCache(isPrivate);
  }

  @Override
  public boolean isNoCacheUnlessVary()
  {
    CauchoResponse cRes = getCauchoResponse();

    if (cRes != null)
      return cRes.isNoCacheUnlessVary();
    else
      return false;
  }

  public CauchoResponse getCauchoResponse()
  {
    if (_response instanceof CauchoResponse)
      return (CauchoResponse) _response;
    else
      return null;
  }

  @Override
  public AbstractHttpResponse getAbstractHttpResponse()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getAbstractHttpResponse();

    return null;
  }

  @Override
  public void setCacheInvocation(AbstractCacheFilterChain cacheFilterChain)
  {
  }

  @Override
  public boolean isCaching()
  {
    return false;
  }

  public void setMatchCacheEntry(AbstractCacheEntry cacheEntry)
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
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }

  /* (non-Javadoc)
   * @see com.caucho.server.http.CauchoResponse#getCharacterEncodingAssigned()
   */
  @Override
  public String getCharacterEncodingAssigned()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
