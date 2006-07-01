/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.connection.RequestAdapter;
import com.caucho.server.connection.ResponseAdapter;
import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.IncludeResponseStream;

class NamedDispatcherImpl implements RequestDispatcher {
  private Application _application;
  private FilterChain _filterChain;
  private String _queryString;

  NamedDispatcherImpl(FilterChain filterChain, String queryString,
                      Application application)
  {
    _filterChain = filterChain;
    _queryString = queryString;
    _application = application;
  }

  public void include(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletResponse res = (HttpServletResponse) response;

    RequestAdapter reqAdapt = null;
    if (! (request instanceof CauchoRequest)) {
      reqAdapt = RequestAdapter.create();
      reqAdapt.init((HttpServletRequest) request, res, _application);
      request = reqAdapt;
    }
    CauchoRequest req = (CauchoRequest) request;
    
    //AbstractResponseStream s = res.getResponseStream();
    // s.setDisableClose(true);

    DispatchResponse subResponse = DispatchResponse.createDispatch();
    subResponse.setRequest(req);
    subResponse.setNextResponse(res);
    // subResponse.init(req, s);
    subResponse.start();
    
    try {
      _filterChain.doFilter(req, subResponse);
    } finally {
      subResponse.finish();
    }

    if (reqAdapt != null)
      RequestAdapter.free(reqAdapt);

    DispatchResponse.free(subResponse);
  }

  /**
   * Forward the request to the named servlet.
   */
  public void forward(ServletRequest req, ServletResponse res)
    throws ServletException, IOException
  {
    res.resetBuffer();
    
    res.setContentLength(-1);

    _filterChain.doFilter(req, res);

    // this is not in a finally block so we can return a real error message
    // if it's not handled.
    // server/1328
    if (res instanceof AbstractHttpResponse) {
      ((AbstractHttpResponse) res).finish();
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
}

