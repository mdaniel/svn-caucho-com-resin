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

package com.caucho.server.dispatch;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.logging.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.jsp.*;
import com.caucho.util.*;
import com.caucho.log.Log;

/**
 * Represents the final servlet in a filter chain.
 */
public class PageFilterChain implements FilterChain {
  private static final Logger log = Log.open(PageFilterChain.class);
  private static final L10N L = new L10N(PageFilterChain.class);

  private ServletContext _application;
  private QServlet _servlet;
  private String _jspFile;
  private ServletConfigImpl _config;
  private ServletContext _servletContext;
  private SoftReference<Page> _pageRef;
  private boolean _isSingleThread;

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the JSP servlet
   */
  PageFilterChain(ServletContext application, QServlet servlet)
  {
    _application = application;
    _servlet = servlet;
  }

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the JSP servlet
   */
  PageFilterChain(ServletContext application, QServlet servlet,
                  String jspFile, ServletConfigImpl config)
  {
    _application = application;
    _servlet = servlet;
    _jspFile = jspFile;
    _config = config;
  }

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext servletContext)
  {
    _servletContext = servletContext;
  }

  /**
   * Gets the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  /**
   * Returns the servlet.
   */
  public QServlet getServlet()
  {
    return _servlet;
  }
  
  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param req the servlet request
   * @param res the servlet response
   */
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    FileNotFoundException notFound = null;

    SoftReference<Page> pageRef = _pageRef;

    Page page;

    if (pageRef != null)
      page = pageRef.get();
    else
      page = null;

    if (page == null || page.cauchoIsModified()) {
      try {
	_pageRef = null;
	
	page = compilePage(page, req, res);

	if (page != null) {
	  _pageRef = new SoftReference<Page>(page);
      
	  _isSingleThread = page instanceof SingleThreadModel;
	}
      } catch (FileNotFoundException e) {
	page = null;

	notFound = e;
      }
    }

    if (page == null) {
      // jsp/01cg
      if (notFound == null)
	return;
      
      String errorUri = (String) req.getAttribute("javax.servlet.error.request_uri");
      String uri = (String) req.getAttribute("javax.servlet.include.request_uri");
      String forward = (String) req.getAttribute("caucho.forward");

      if (errorUri != null) {
	throw new FileNotFoundException(errorUri);
      }
      else if (uri != null) {
	throw new FileNotFoundException(uri);
      }
      else if (forward != null) {
	throw new FileNotFoundException(req.getRequestURI());
      }
	
      ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    else if (req instanceof HttpServletRequest) {
      if (_isSingleThread) {
        synchronized (page) {
          page.pageservice(req, res);
        }
      }
      else
        page.pageservice(req, res);
    }
  }

  /**
   * Compiles the page, returning the new page.
   */
  private Page compilePage(Page oldPage,
			   HttpServletRequest req,
			   HttpServletResponse res)
    throws ServletException, FileNotFoundException
  {
    Page newPage = null;
	
    if (oldPage != null && ! oldPage.startRecompiling()) {
      return oldPage;
    }

    try {
      if (_jspFile != null) {
	req.setAttribute("caucho.jsp.jsp-file", _jspFile);
	req.setAttribute("caucho.jsp.servlet-config", _config);
      }

      if (_config != null)
	newPage = (Page) _config.createServlet();
      else {
	newPage = _servlet.getPage(req, res);

	if (newPage != null && ! newPage.isInit()) {
	  ServletConfigImpl config = new ServletConfigImpl();
	  config.setServletContext(_application);
	  newPage.init(config);
	}
      }

      // XXX: In theory, should let the requests drain.  In practice,
      // the JSP destroy() method doesn't do anything.
      if (oldPage != null && ! oldPage.isDead())
	oldPage.destroy();

      if (newPage != null)
	newPage._caucho_use();

      return newPage;
    } catch (ServletException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (FileNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    } finally {
      if (_jspFile != null) {
	req.removeAttribute("caucho.jsp.jsp-file");
	req.removeAttribute("caucho.jsp.servlet-config");
      }
    }
  }
}
