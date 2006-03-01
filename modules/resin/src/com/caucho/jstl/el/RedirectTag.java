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

package com.caucho.jstl.el;

import java.io.*;
import java.net.*;

import javax.servlet.jsp.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.el.ELException;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.jsp.*;
import com.caucho.el.*;
import com.caucho.jstl.NameValueTag;
import com.caucho.jstl.rt.CoreUrlTag;

public class RedirectTag extends TagSupport implements NameValueTag {
  private static L10N L = new L10N(RedirectTag.class);
  
  private Expr _valueExpr;
  private Expr _contextExpr;

  private CharBuffer _url;

  /**
   * Sets the URL to be imported.
   */
  public void setURL(Expr value)
  {
    _valueExpr = value;
  }
  
  /**
   * Sets the external context for the import.
   */
  public void setContext(Expr context)
  {
    _contextExpr = context;
  }

  /**
   * Adds a parameter.
   */
  public void addParam(String name, String value)
  {
    String encoding = this.pageContext.getResponse().getCharacterEncoding();
    
    CoreUrlTag.addParam(_url, name, value, encoding);
  }

  public int doStartTag() throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
    
      String value = _valueExpr.evalString(pageContext);
      String context = null;

      if (_contextExpr != null)
	context = _contextExpr.evalString(pageContext);
    
      _url = UrlTag.normalizeURL(pageContext, value, context);

      return EVAL_BODY_INCLUDE;
    } catch (ELException e) {
      throw new JspException(e);
    }
  }
      
  public int doEndTag() throws JspException
  {
    String value = UrlTag.encodeURL(pageContext, _url);

    try {
      HttpServletResponse response;
      ((HttpServletResponse) pageContext.getResponse()).sendRedirect(value);
    } catch (IOException e) {
      throw new JspException(e);
    }

    return SKIP_PAGE;
  }
}
