/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.RequestAdapter;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

class NamedDispatcherImpl implements RequestDispatcher {
  private WebApp _webApp;
  
  private FilterChain _includeFilterChain;  
  private FilterChain _forwardFilterChain;  
  
  private String _queryString;

  NamedDispatcherImpl(FilterChain includeFilterChain,
		      FilterChain forwardFilterChain,
		      String queryString, WebApp webApp)
  {
    _includeFilterChain = includeFilterChain;
    _forwardFilterChain = forwardFilterChain;
    _queryString = queryString;
    _webApp = webApp;
  }

  public void include(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletResponse res = (HttpServletResponse) response;

    RequestAdapter reqAdapt = null;
    
    if (! (request instanceof CauchoRequest)) {
      reqAdapt = RequestAdapter.create();
      reqAdapt.init((HttpServletRequest) request, res, _webApp);
      request = reqAdapt;
    }

    CauchoRequest req = (CauchoRequest) request;

    DispatchResponse dispatchResponse = new DispatchResponse(res);
    dispatchResponse.init(res);

    try {
      _includeFilterChain.doFilter(req, dispatchResponse);
    } finally {
      dispatchResponse.finish();
    }

    //_includeFilterChain.doFilter(req, new DispatchResponse(res));

    //AbstractResponseStream s = res.getResponseStream();
    // s.setDisableClose(true);

    /* XXX:
    DispatchResponse subResponse = DispatchResponse.createDispatch();
    // XXX: subResponse.init(req);
    subResponse.setNextResponse(res);
    // subResponse.init(req, s);
    subResponse.startRequest(null);
    
    try {
      _includeFilterChain.doFilter(req, subResponse);
    } finally {
      subResponse.finishInvocation();
      subResponse.finishRequest();
    }

    if (reqAdapt != null)
      RequestAdapter.free(reqAdapt);

    DispatchResponse.free(subResponse);
    */
  }

  /**
   * Forward the request to the named servlet.
   */
  public void forward(ServletRequest req, ServletResponse res)
    throws ServletException, IOException
  {
    res.resetBuffer();
    
    res.setContentLength(-1);

    _forwardFilterChain.doFilter(req, res);

    // this is not in a finally block so we can return a real error message
    // if it's not handled.
    // server/1328, server/125i
    if (res instanceof CauchoResponse) {
      CauchoResponse cRes = (CauchoResponse) res;
      
      cRes.close();
    }
    else {
        try {
          OutputStream os = res.getOutputStream();
	  if (os != null)
	    os.close();
        } catch (IllegalStateException e) {
        }
	
	try {
	  PrintWriter out = res.getWriter();
	  if (out != null)
	    out.close();
	} catch (IllegalStateException e1) {
	}

    }

    /*
    ServletResponse ptr = res;
    while (ptr instanceof HttpServletResponseWrapper) {
      ptr = ((HttpServletResponseWrapper) ptr).getResponse();

      if (ptr instanceof AbstractHttpResponse)
	((AbstractHttpResponse) ptr).finish();
    }
    */

    /*
    if (res instanceof AbstractHttpResponse)
      ((AbstractHttpResponse) res).finish(true);

    if (res instanceof Response)
      ((Response) res).finish(true);
    */
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _includeFilterChain + "]";
  }
}

