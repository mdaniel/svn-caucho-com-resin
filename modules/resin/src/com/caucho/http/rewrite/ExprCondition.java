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
 * @author Sam
 */

package com.caucho.http.rewrite;

import com.caucho.v5.el.ELParser;
import com.caucho.v5.el.Expr;
import com.caucho.v5.util.L10N;

import javax.el.ELContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A {@link RewriteDispatch} condition that is an el expression.
 */
public class ExprCondition
  extends AbstractCondition
{
  private Expr _expr;

  public ExprCondition(String expr)
  {
    ELContext elContext = new RewriteELContext();
    
    _expr = new ELParser(elContext, expr).parse();
  }

  @Override
  public String getTagName()
  {
    return "expr";
  }

  @Override
  public boolean isMatch(HttpServletRequest request,
                         HttpServletResponse response)
  {
    return _expr.evalBoolean(new RewriteELContext());
  }
}
