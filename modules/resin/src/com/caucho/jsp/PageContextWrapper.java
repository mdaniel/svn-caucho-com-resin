/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jsp;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.jstl.core.*;
import javax.servlet.jsp.jstl.fmt.*;
import javax.servlet.jsp.el.ExpressionEvaluator;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.el.*;

import com.caucho.log.Log;

import com.caucho.xpath.VarEnv;

import com.caucho.server.webapp.Application;

import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.connection.ResponseAdapter;
import com.caucho.server.connection.RequestAdapter;

import com.caucho.server.webapp.RequestDispatcherImpl;

public class PageContextWrapper extends PageContextImpl {
  private static final Logger log = Log.open(PageContextWrapper.class);
  static final L10N L = new L10N(PageContextWrapper.class);

  private static final FreeList<PageContextWrapper> _freeList =
    new FreeList<PageContextWrapper>(32);

  private PageContextImpl _parent;

  public void init(PageContextImpl parent)
  {
    _parent = parent;
    clearAttributes();
    setOut(parent.getOut());
  }

  public static PageContextWrapper create(JspContext parent)
  {
    PageContextWrapper wrapper = _freeList.allocate();
    if (wrapper == null)
      wrapper = new PageContextWrapper();

    wrapper.init((PageContextImpl) parent);

    return wrapper;
  }

  /**
   * Returns the underlying servlet for the page.
   */
  public Object getPage()
  {
    return _parent.getPage();
  }

  /**
   * Returns the servlet request for the page.
   */
  public ServletRequest getRequest()
  {
    return _parent.getRequest();
  }
  
  /**
   * Returns the servlet response for the page.
   */
  public ServletResponse getResponse()
  {
    return _parent.getResponse();
  }
  
  /**
   * Returns the servlet response for the page.
   */
  public HttpServletRequest getCauchoRequest()
  {
    return _parent.getCauchoRequest();
  }
  
  /**
   * Returns the servlet response for the page.
   */
  public CauchoResponse getCauchoResponse()
  {
    return _parent.getCauchoResponse();
  }

  public HttpSession getSession()
  {
    return _parent.getSession();
  }

  public ServletConfig getServletConfig()
  {
    return _parent.getServletConfig();
  }

  /**
   * Returns the page's servlet context.
   */
  public ServletContext getServletContext()
  {
    return _parent.getServletContext();
  }

  /**
   * Returns the page's application.
   */
  public Application getApplication()
  {
    return _parent.getApplication();
  }

  /**
   * Returns the page's error page.
   */
  public String getErrorPage()
  {
    return _parent.getErrorPage();
  }

  /**
   * Sets the page's error page.
   */
  public void setErrorPage(String errorPage)
  {
    _parent.setErrorPage(errorPage);
  }

  /**
   * Returns the Throwable stored by the error page.
   */
  public Throwable getThrowable()
  {
    return _parent.getThrowable();
  }

  /**
   * Returns the error data
   */
  public ErrorData getErrorData()
  {
    return _parent.getErrorData();
  }

  /**
   * Returns the variable resolver
   */
  public javax.servlet.jsp.el.VariableResolver getVariableResolver()
  {
    return _parent.getVariableResolver();
  }

  /**
   * Returns the expression evaluator
   */
  public ExpressionEvaluator getExpressionEvaluator()
  {
    return _parent.getExpressionEvaluator();
  }

  public static void free(PageContextWrapper wrapper)
  {
    _freeList.free(wrapper);
  }
}
