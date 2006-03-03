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

package com.caucho.amber.query;

import com.caucho.util.CharBuffer;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.BooleanType;

/**
 * Bound identifier expression.
 */
class LikeExpr extends AbstractAmberExpr {
  private AmberExpr _expr;
  
  private AmberExpr _value;
  private String _escape;
  private boolean _isNot;

  /**
   * Creates a new cmp expression
   */
  LikeExpr(AmberExpr expr, AmberExpr value, String escape, boolean isNot)
  {
    _expr = expr;
    _value = value;
    _isNot = isNot;
    _escape = escape;
  }
  
  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Returns the expr type.
   */
  public Type getType()
  {
    return BooleanType.create();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _expr = _expr.bindSelect(parser);
    _value = _value.bindSelect(parser);

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_expr.usesFrom(from, type));
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    _expr = _expr.replaceJoin(join);
    _value = _value.replaceJoin(join);
    
    return this;
  }
  
  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append('(');
    _expr.generateWhere(cb);

    if (_isNot)
      cb.append(" NOT");

    cb.append(" LIKE ");

    _value.generateWhere(cb);

    if (_escape != null) {
      cb.append(" ESCAPE ");
      cb.append(_escape);
    }
    
    cb.append(')');
  }

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();
    
    cb.append('(');
    cb.append(_expr);

    if (_isNot)
      cb.append(" NOT");

    cb.append(" LIKE ");

    cb.append(_value);
    
    cb.append(')');

    return cb.close();
  }
}
