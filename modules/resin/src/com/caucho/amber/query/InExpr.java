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

import java.util.ArrayList;

import com.caucho.util.CharBuffer;

/**
 * "in" expression
 */
class InExpr extends AbstractAmberExpr {
  private AmberExpr _expr;
  
  private ArrayList<AmberExpr> _values;
  private boolean _isNot;

  /**
   * Creates a new cmp expression
   */
  InExpr(AmberExpr expr, ArrayList<AmberExpr> values, boolean isNot)
  {
    _expr = expr;
    _values = values;
    _isNot = isNot;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _expr = _expr.bindSelect(parser);

    for (int i = 0; i < _values.size(); i++) {
      _values.set(i, _values.get(i).bindSelect(parser));
    }

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (_expr.usesFrom(from, type))
      return true;

    for (int i = 0; i < _values.size(); i++) {
      if (_values.get(i).usesFrom(from, type))
	return true;
    }

    return false;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    _expr = _expr.replaceJoin(join);

    for (int i = 0; i < _values.size(); i++) {
      _values.set(i, _values.get(i).replaceJoin(join));
    }
    
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

    cb.append(" IN (");

    for (int i = 0; i < _values.size(); i++) {
      if (i != 0)
	cb.append(',');

      _values.get(i).generateWhere(cb);
    }

    cb.append("))");
  }

  public String toString()
  {
    CharBuffer cb = new CharBuffer();
    
    cb.append('(');
    cb.append(_expr);

    if (_isNot)
      cb.append(" NOT");

    cb.append(" IN (");

    for (int i = 0; i < _values.size(); i++) {
      if (i != 0)
	cb.append(',');

      cb.append(_values.get(i));
    }

    cb.append("))");

    return cb.toString();
  }
}
