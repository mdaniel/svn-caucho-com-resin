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

package com.caucho.jstl.el;

import java.io.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.jsp.PageContextImpl;
import com.caucho.jsp.BodyContentImpl;

import com.caucho.el.*;

public class CoreOutTag extends BodyTagSupport {
  private static final Logger log = Log.open(CoreOutTag.class);
  
  private Expr _value;
  private Expr _escapeXml;
  private Expr _defaultValue;

  /**
   * Sets the JSP-EL expression value.
   */
  public void setValue(Expr value)
  {
    _value = value;
  }

  /**
   * Sets true if XML should be escaped.
   */
  public void setEscapeXml(Expr value)
  {
    _escapeXml = value;
  }

  /**
   * Sets the default value.
   */
  public void setDefault(Expr value)
  {
    _defaultValue = value;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      VariableResolver env = pageContext;
      
      JspWriter out = pageContext.getOut();

      boolean doEscape = (_escapeXml == null || _escapeXml.evalBoolean(env));
      
      if (! _value.print(out, env, doEscape)) {
      }
      else if (_defaultValue != null)
        _defaultValue.print(out, env, doEscape);
      else
        return EVAL_BODY_BUFFERED;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return SKIP_BODY;
  }

  public int doEndTag() throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      VariableResolver env = pageContext;
      
      JspWriter out = pageContext.getOut();
      
      BodyContentImpl body = (BodyContentImpl) getBodyContent();

      if (body != null) {
        boolean doEscape = (_escapeXml == null || _escapeXml.evalBoolean(env));

        String s = body.getString().trim();

        if (doEscape)
          Expr.toStreamEscaped(out, s);
        else
          out.print(s);
      }
    } catch (Exception e) {
    }

    return EVAL_PAGE;
  }
}
