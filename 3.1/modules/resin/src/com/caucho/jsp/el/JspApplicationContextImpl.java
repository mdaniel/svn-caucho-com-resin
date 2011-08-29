/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.jsp.TaglibManager;
import com.caucho.jsp.PageContextImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.util.FreeList;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public class JspApplicationContextImpl implements JspApplicationContext
{
  private static final L10N L = new L10N(JspApplicationContextImpl.class);
  
  private final WebApp _webApp;
  
  private final ExpressionFactory _expressionFactory;

  private TaglibManager _taglibManager;

  private ELResolver []_resolverArray = new ELResolver[0];
  private ELContextListener []_listenerArray = new ELContextListener[0];

  private boolean _hasRequest;

  private FreeList<PageContextImpl> _freePages
    = new FreeList<PageContextImpl>(256);

  public JspApplicationContextImpl(WebApp webApp)
  {
    _webApp = webApp;

    _expressionFactory = new JspExpressionFactoryImpl(this);
  }

  //
  // properties
  //
  
  WebApp getWebApp()
  {
    return _webApp;
  }

  public TaglibManager getTaglibManager()
  {
    return _taglibManager;
  }

  public void setTaglibManager(TaglibManager taglibManager)
  {
    _taglibManager = taglibManager;
  }

  //
  // JspApplicationContext API methods
  //
  
  /**
   * Adds an ELContextListener.
   */
  public void addELContextListener(ELContextListener listener)
  {
    if (_hasRequest)
      throw new IllegalStateException(L.l("Cannot add ELContextListener after requests have started."));
    
    ELContextListener []listenerArray
      = new ELContextListener[_listenerArray.length + 1];
    System.arraycopy(_listenerArray, 0,
		     listenerArray, 0,
		     _listenerArray.length);

    listenerArray[_listenerArray.length] = listener;
    
    _listenerArray = listenerArray;
  }

  public ELContextListener []getELListenerArray()
  {
    _hasRequest = true;
    
    return _listenerArray;
  }
  
  /**
   * Adds an ELResolver
   */
  public void addELResolver(ELResolver resolver)
  {
    if (_hasRequest)
      throw new IllegalStateException(L.l("Can't add ELResolver after starting request."));
    
    ELResolver []resolverArray = new ELResolver[_resolverArray.length + 1];
    System.arraycopy(_resolverArray, 0,
		     resolverArray, 0,
		     _resolverArray.length);

    resolverArray[_resolverArray.length] = resolver;
    
    _resolverArray = resolverArray;
  }

  public ELResolver []getELResolverArray()
  {
    return _resolverArray;
  }
  
  /**
   * Gets the expression factory
   */
  public ExpressionFactory getExpressionFactory()
  {
    return _expressionFactory;
  }

  //
  // page context stuff
  //
  
  public PageContextImpl allocatePageContext(Servlet servlet,
						    ServletRequest request,
						    ServletResponse response,
						    String errorPageURL,
						    boolean needsSession,
						    int buffer,
						    boolean autoFlush)
  {
    PageContextImpl pc = _freePages.allocate();
    if (pc == null)
      pc = new PageContextImpl();

    try {
      pc.initialize(servlet, request, response, errorPageURL,
                    needsSession, buffer, autoFlush);
    } catch (Exception e) {
    }

    return pc;
  }

  /**
   * The jsp page context initialization.
   */
  public PageContextImpl allocatePageContext(Servlet servlet,
						    WebApp app,
						    ServletRequest request,
						    ServletResponse response,
						    String errorPageURL,
						    HttpSession session,
						    int buffer,
						    boolean autoFlush,
						    boolean isPrintNullAsBlank)
  {
    PageContextImpl pc = _freePages.allocate();
    if (pc == null)
      pc = new PageContextImpl();

    pc.initialize(servlet, app, request, response, errorPageURL,
		  session, buffer, autoFlush, isPrintNullAsBlank);

    return pc;
  }

  public PageContext getPageContext(Servlet servlet,
				    ServletRequest request,
				    ServletResponse response,
				    String errorPageURL,
				    boolean needsSession,
				    int buffer,
				    boolean autoFlush)
  {
    return allocatePageContext(servlet, request, response,
			       errorPageURL, needsSession,
			       buffer, autoFlush);
  }

  /**
   * Frees a page context after the JSP page is done with it.
   *
   * @param pc the PageContext to free
   */
  public void releasePageContext(PageContext pc)
  {
    if (pc != null) {
      pc.release();

      if (pc instanceof PageContextImpl)
	_freePages.free((PageContextImpl) pc);
    }
  }

  public void freePageContext(PageContextImpl pc)
  {
    if (pc != null) {
      pc.release();

      if (pc instanceof PageContextImpl)
	_freePages.free((PageContextImpl) pc);
    }
  }

  public String toString()
  {
    return "JspApplicationContextImpl[" + _webApp + "]";
  }
}
