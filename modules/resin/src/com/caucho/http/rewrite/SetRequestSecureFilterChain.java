/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.http.rewrite;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.http.protocol.RequestCaucho;
import com.caucho.http.protocol.RequestServlet;
import com.caucho.http.protocol.RequestAdapter;

public class SetRequestSecureFilterChain implements FilterChain
{
  private final FilterChain _next;
  private Boolean _isSecure;

  public SetRequestSecureFilterChain(FilterChain next,
                                     Boolean isSecure)
  {
    _next = next;
    _isSecure = isSecure;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    /*
    if (res instanceof CauchoResponse) {
      // server/125i - XXX: needs refactor
      CauchoResponse cRes = (CauchoResponse) res;
      CauchoRequest oldReq = cRes.getAbstractHttpResponse().getRequest();

      cRes.getAbstractHttpResponse().setRequest((CauchoRequest) req);
      try {
        next.doFilter(req, res);
      } finally {
        cRes.getAbstractHttpResponse().setRequest(oldReq);
      }
    }
    */
    
    if (req instanceof RequestServlet) {
      RequestServlet requestFacade = (RequestServlet) req;

      requestFacade.setSecure(_isSecure);

      // XXX: finally

      _next.doFilter(req, res);
    }
    else {
      req = new SecureServletRequestWrapper(req, _isSecure);

      _next.doFilter(req, res);
    }
  }

  public static class SecureServletRequestWrapper extends RequestAdapter
  {
    private boolean _isSecure;

    public SecureServletRequestWrapper(HttpServletRequest request,
                                       boolean isSecure)
    {
      setRequest(request);

      if (request instanceof RequestCaucho)
        setWebApp(((RequestCaucho) request).getWebApp());

      _isSecure = isSecure;
    }

    @Override
    public boolean isSecure()
    {
      return _isSecure;
    }

    /**
     * Returns the request's scheme.
     */
    @Override
    public String getScheme()
    {
      return isSecure() ? "https" : "http";
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _next + "]";
  }
}
