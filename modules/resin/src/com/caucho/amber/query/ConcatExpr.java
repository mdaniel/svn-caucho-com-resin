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

package com.caucho.amber.query;

import com.caucho.util.CharBuffer;

/**
 * String concatenation expression.
 */
class ConcatExpr extends AbstractAmberExpr {
  private AmberExpr _left;
  private AmberExpr _right;

  /**
   * Creates a new cmp expression
   */
  ConcatExpr(AmberExpr left, AmberExpr right)
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
    cb.append('(');
    _left.generateWhere(cb);
    cb.append(" || ");
    _right.generateWhere(cb);
    cb.append(')');
  }

  public String toString()
  {
    return "(" + _left + " || " + _right + ")";
  }
}
