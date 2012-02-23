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

package com.caucho.server.httpcache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.CauchoResponseWrapper;
import com.caucho.util.L10N;

public class ProxyCacheResponse extends CauchoResponseWrapper
{
  private static final L10N L = new L10N(ProxyCacheResponse.class);
  private static final Logger log
    = Logger.getLogger(ProxyCacheResponse.class.getName());
  
  private ProxyCacheRequest _request;
  private CauchoResponse _response;
  private ProxyCacheFilterChain _proxyChain;
  
  private boolean _isNotModified;
  private boolean _isResponseSent;
  
  ProxyCacheResponse(ProxyCacheRequest req,
                     CauchoResponse res,
                     ProxyCacheFilterChain proxyChain)
                     
  {
    super(req, res);
    
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
    if (code == HttpServletResponse.SC_NOT_MODIFIED)
      _isNotModified = true;
    
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
    
    if (_proxyChain.fillFromCache(_request, this, 
                                  _request.getProxyCacheEntry())) {
      isFill = true;
    } else {
      IllegalStateException exn
        = new IllegalStateException(L.l("{0} cannot fill a 403 NotModified from the cache for {1}\n  {2}",
                                        this, _request.getRequestURL(),
                                        _request.getProxyCacheEntry()));
      exn.fillInStackTrace();
      
      log.log(Level.WARNING, exn.toString(), exn);
    }
    
    _request.getProxyCacheEntry().updateNotModified();
    _response.setCacheInvocation(null);
    _isResponseSent = true;
    
    return isFill;
  }
}