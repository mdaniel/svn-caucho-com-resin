/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import com.caucho.el.Expr;
import com.caucho.jsp.PageContextImpl;
import com.caucho.jstl.ChooseTag;
import com.caucho.util.L10N;

import javax.el.ELException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Tag representing an "when" condition.
 */
public class WhenTag extends TagSupport {
  private static L10N L = new L10N(WhenTag.class);
  
  private Expr _testExpr;

  /**
   * Sets the test expression.
   *
   * @param test the test expression.
   */
  public void setTest(Expr test)
  {
    _testExpr = test;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      Tag parent = getParent();

      if (! (parent instanceof ChooseTag))
        throw new JspException(L.l("c:when tag must be contained in a c:choose tag."));

      ChooseTag chooseTag = (ChooseTag) parent;

      if (chooseTag.isMatch())
        return SKIP_BODY;

      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      boolean test = _testExpr.evalBoolean(pageContext.getELContext());

      if (test) {
        chooseTag.setMatch();
      
        return EVAL_BODY_INCLUDE;
      }
      else
        return SKIP_BODY;
    } catch (ELException e) {
      throw new JspException(e);
    }
  }
}
