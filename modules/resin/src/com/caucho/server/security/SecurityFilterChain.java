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

package com.caucho.server.security;

import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SecurityFilterChain implements FilterChain {
  private FilterChain _next;
  
  private ServletContext _webApp;

  private AbstractConstraint []_constraints;
  private HashMap<String,AbstractConstraint[]> _methodMap;

  SecurityFilterChain(FilterChain next)
  {
    _next = next;
  }

  public void setWebApp(ServletContext app)
  {
    _webApp = app;
  }

  public void setConstraints(ArrayList<AbstractConstraint> constraints)
  {
    _constraints = new AbstractConstraint[constraints.size()];

    constraints.toArray(_constraints);
  }

  public void setMethodMap(HashMap<String,AbstractConstraint[]> methodMap)
  {
    _methodMap = methodMap;
  }

  public void destroy()
  {
  }

  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    // This filter is always called before user filters so we know that
    // the request and response are AbstractRequest and Response.
    CauchoRequest req = (CauchoRequest) request;
    CauchoResponse res = (CauchoResponse) response;

    AbstractConstraint []constraints = null;
    if (_methodMap != null)
      constraints = _methodMap.get(req.getMethod());

    if (constraints == null)
      constraints = _constraints;

    boolean isPrivateCache = false;
    if (constraints != null) {
      // non-authentication constraints are first
      for (int i = 0; i < constraints.length; i++) {
	AbstractConstraint constraint = constraints[i];
	
	if (constraint.needsAuthentication())
	  continue;

        if (! constraint.isAuthorized(req, res, _webApp))
          return;
	
	if (constraint.isPrivateCache())
	  isPrivateCache = true;
      }

      boolean hasAuth = false;
      for (int i = 0; i < constraints.length; i++) {
	AbstractConstraint constraint = constraints[i];
	
        if (! constraint.needsAuthentication())
	  continue;

        if (! hasAuth) {
          hasAuth = true;
          if (! req.authenticate())
            return;
        }
        
        if (! constraint.isAuthorized(req, res, _webApp))
          return;
	
	if (constraint.isPrivateCache())
	  isPrivateCache = true;
      }
    }

    if (isPrivateCache)
      res.setPrivateCache(true);
    
    _next.doFilter(request, response);
  }
}
