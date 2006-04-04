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

package com.caucho.jsp;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

import com.caucho.util.FreeList;

import com.caucho.server.webapp.Application;

public class QJspFactory extends JspFactory {
  private static JspEngineInfo _engineInfo = new EngineInfo();
  
  private static FreeList<PageContextImpl> _freePages =
    new FreeList<PageContextImpl>(32);
  
  private static QJspFactory _factory;
  
  static public QJspFactory create()
  {
    if (_factory == null)
      _factory = new QJspFactory();

    return _factory;
  }

  public static PageContextImpl allocatePageContext(Servlet servlet,
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
  public static PageContextImpl allocatePageContext(Servlet servlet,
						    Application app,
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

  public static void freePageContext(PageContext pc)
  {
    if (pc != null) {
      pc.release();

      if (pc instanceof PageContextImpl)
	_freePages.free((PageContextImpl) pc);
    }
  }

  // This doesn't appear in the spec, but apparantly exists in some jsdk.jar
  public String getSpecificationVersion()
  {
    return getEngineInfo().getSpecificationVersion();
  }
  
  public JspEngineInfo getEngineInfo()
  {
    return _engineInfo;
  }

  static class EngineInfo extends JspEngineInfo {
    public String getSpecificationVersion()
    {
      return "2.0";
    }
  }
}
