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

package com.caucho.v5.http.cache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.protocol.RequestCache;
import com.caucho.v5.http.protocol.ResponseCache;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.http.protocol.ResponseCauchoWrapper;
import com.caucho.v5.http.protocol.ResponseFacade;
import com.caucho.v5.util.L10N;

public class ResponseHttpCache extends ResponseCauchoWrapper
{
  private static final L10N L = new L10N(ResponseHttpCache.class);
  private static final Logger log
    = Logger.getLogger(ResponseHttpCache.class.getName());
  
  private RequestHttpCache _request;
  private ResponseCache _response;
  private FilterChainHttpCache _proxyChain;
  
  private boolean _isNotModified;
  private boolean _isResponseSent;
  
  ResponseHttpCache(RequestHttpCache req,
                    ResponseCache res,
                    FilterChainHttpCache proxyChain)
                     
  {
    super(req, (ResponseCaucho) res);
    
    _request = req;
    _response = res;
    _proxyChain = proxyChain;
  }
  
  @Override
  public void setStatus(int code)
  {
    if (code == HttpServletResponse.SC_NOT_MODIFIED)
      _isNotModified = true;
    
    super.setStatus(code);
  }
  
  @Override
  public void setStatus(int code, String message)
  {
    if (code == HttpServletResponse.SC_NOT_MODIFIED) {
      _isNotModified = true;
    }
    
    super.setStatus(code, message);
  }
  
  @Override
  public void sendError(int code)
    throws IOException
  {
    sendError(code, null);
  }
  
  @Override
  public void sendError(int code, String message)
    throws IOException
  {
    if (code == HttpServletResponse.SC_NOT_MODIFIED) {
      _isNotModified = true;
      
      if (! fillFromCache()) {
        super.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        
        return;
      }
    }
    else {
      super.sendError(code, message);
    }
  }
  
  @Override
  public PrintWriter getWriter()
    throws IOException
  {
    if (_isNotModified) {
      fillFromCache();
    }

    return super.getWriter();
  }
  
  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    if (_isNotModified) {
      fillFromCache();
    }

    return super.getOutputStream();
  }
  
  @Override
  public void flushBuffer()
    throws IOException
  {
    if (_isNotModified) {
      fillFromCache();
    }
    
    super.flushBuffer();
  }
  
  @Override
  public void writeHeaders(int length)
    throws IOException
  {
    if (_isNotModified) {
      fillFromCache();
    }
  }
  
  @Override
  public void close()
    throws IOException
  {
    if (_isNotModified) {
      fillFromCache();
    }
  }
  
  private boolean fillFromCache()
    throws IOException
  {
    if (_isResponseSent)
      return true;
    
    _isResponseSent = true;
    
    boolean isFill = false;

    if (_proxyChain.fillFromCache((RequestCache) _request, 
                                  (ResponseCache) this, 
                                  _request.getProxyCacheEntry())) {
      isFill = true;
    }
    else {
      _proxyChain.clearCache();
      
      IllegalStateException exn
        = new IllegalStateException(L.l("{0} cannot fill a 403 NotModified from the cache for {1}\n  {2}",
                                        this, _request.getRequestURL(),
                                        _request.getProxyCacheEntry()));
      exn.fillInStackTrace();
      
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, exn.toString(), exn);
      else
        log.warning(exn.getMessage());
    }
    
    _request.getProxyCacheEntry().updateNotModified();
    _response.setCacheInvocation(null);
    _isResponseSent = true;
    
    return isFill;
  }
}