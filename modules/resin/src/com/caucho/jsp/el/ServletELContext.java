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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.el;

import com.caucho.el.ELParser;
import com.caucho.el.Expr;

import javax.el.ELContext;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Parses the expression.
 */
abstract public class ServletELContext extends ELContext
{
  abstract public ServletContext getApplication();
  
  abstract public Object getApplicationScope();
  
  public Object getCookie()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    Cookie []cookies = getRequest().getCookies();

    for (int i = 0; cookies != null && i < cookies.length; i++) {
      if (map.get(cookies[i].getName()) == null)
	map.put(cookies[i].getName(), cookies[i]);
    }
      
    return map;
  }
  
  public Object getHeaderValues()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;

    HashMap<String,String[]> map = new HashMap<String,String[]>();
    Enumeration e = request.getHeaderNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Enumeration values = request.getHeaders(name);
      
      ArrayList<String> list = new ArrayList<String>();

      while (values.hasMoreElements())
	list.add((String) values.nextElement());

      map.put(name, list.toArray(new String[list.size()]));
    }
      
    return map;
  }
  
  abstract public HttpServletRequest getRequest();
  
  abstract public Object getRequestScope();
}
