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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.expr;

import com.caucho.amber.query.QueryParser;
import com.caucho.util.CharBuffer;

/**
 * String concatenation expression.
 */
public class ConcatExpr extends AbstractAmberExpr {
  private AmberExpr _left;
  private AmberExpr _right;

  /**
   * Creates a new cmp expression
   */
  public ConcatExpr(AmberExpr left, AmberExpr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Returns the java type.
   */
  public Class getJavaType()
  {
    return String.class;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _left = _left.bindSelect(parser);
    _right = _right.bindSelect(parser);

    return this;
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, true);
  }

  /**
   * Generates the (update) where expression.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, false);
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  public String toString()
  {
    return "(" + _left + " || " + _right + ")";
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    cb.append('(');

    if (select)
      _left.generateWhere(cb);
    else
      _left.generateUpdateWhere(cb);

    cb.append(" || ");

    if (select)
      _right.generateWhere(cb);
    else
      _right.generateUpdateWhere(cb);

    cb.append(')');
  }
}
