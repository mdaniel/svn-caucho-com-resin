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

package com.caucho.v5.jsp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.jsp.JspFactory;

import com.caucho.v5.http.webapp.WebAppResin;

/**
 * Handles JSP pages.  Most of the work is done in the JspManager and QServlet.
 *
 * @see JspManager
 */
public class JspServlet extends QServlet {
  static final String COPYRIGHT =
    "Copyright (c) 1998-2015 Caucho Technology.  All rights reserved.";

  private boolean _isXml = false;
  private boolean _loadTldOnInit = false;
  private int _pageCacheMax = 256;

  /**
   * Set true when JSP pages should default to xml.
   */
  public void setXml(boolean isXml)
  {
    _isXml = isXml;
  }

  /**
   * Set true when JSP pages should load tld files.
   */
  public void setLoadTldOnInit(boolean isPreload)
  {
    _loadTldOnInit = isPreload;
  }

  /**
   * Set true when JSP pages should default to xml.
   */
  public void setPageCacheMax(int max)
  {
    _pageCacheMax = max;
  }
  
  /**
   * Initializes the servlet.  Primarily, this sets the PageManager to the
   * correct JspManager.
   */
  @Override
  public void init(ServletConfig conf)
    throws ServletException
  {
    super.init(conf);

    JspManager manager = new JspManager();

    WebAppResin webApp = (WebAppResin) getServletContext();
    
    if (webApp == null) {
      return;
    }

    manager.setXml(_isXml);
    manager.setLoadTldOnInit(_loadTldOnInit
                             || webApp.getJsp().isLoadTldOnInit());
    manager.setPageCacheMax(_pageCacheMax);

    manager.initWebApp(webApp);
      
    setManager(manager);

    manager.init();

    if (JspFactory.getDefaultFactory() == null)
      JspFactory.setDefaultFactory(new QJspFactory());
  }

  /**
   * Static initialization
   */
  public static void initStatic()
  {
    if (JspFactory.getDefaultFactory() == null)
      JspFactory.setDefaultFactory(new QJspFactory());
  }

  @Override
  public String getServletInfo()
  {
    return "JSP";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getServletContext() + "]";
  }
}

