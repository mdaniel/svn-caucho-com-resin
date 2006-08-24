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

import com.caucho.amber.query.*;


import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

/**
 * Bound identifier expression.
 */
public class BinaryExpr extends AbstractAmberExpr {
  private static final L10N L = new L10N(BinaryExpr.class);

  private AmberExpr _left;
  private AmberExpr _right;
  private int _token;

  /**
   * Creates a new cmp expression
   */
  public BinaryExpr(int token, AmberExpr left, AmberExpr right)
  {
    _token = token;
    _left = left;
    _right = right;
  }

  /**
   * Returns true for a boolean expr.
   */
  public boolean isBoolean()
  {
    switch (_token) {
    case QueryParser.EQ:
    case QueryParser.NE:
    case QueryParser.LT:
    case QueryParser.LE:
    case QueryParser.GT:
    case QueryParser.GE:
      return true;
    default:
      return false;
    }
  }

  /**
   * Returns the java type.
   */
  public Class getJavaType()
  {
    switch (_token) {
    case QueryParser.EQ:
    case QueryParser.NE:
    case QueryParser.LT:
    case QueryParser.LE:
    case QueryParser.GT:
    case QueryParser.GE:
      return boolean.class;
    default:
      return double.class;
    }
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
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_left.usesFrom(from, type) || _right.usesFrom(from, type));
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    _left = _left.replaceJoin(join);
    _right = _right.replaceJoin(join);

    return this;
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append('(');

    _left.generateWhere(cb);

    switch (_token) {
    case QueryParser.EQ:
      cb.append(" = ");
      break;
    case QueryParser.NE:
      cb.append(" <> ");
      break;
    case QueryParser.LT:
      cb.append(" < ");
      break;
    case QueryParser.LE:
      cb.append(" <= ");
      break;
    case QueryParser.GT:
      cb.append(" > ");
      break;
    case QueryParser.GE:
      cb.append(" >= ");
      break;
    case '+':
      cb.append(" + ");
      break;
    case '-':
      cb.append(" - ");
      break;
    case '*':
      cb.append(" * ");
      break;
    case '/':
      cb.append(" / ");
      break;
    case '%':
      cb.append(" % ");
      break;
    default:
      throw new IllegalStateException();
    }

    _right.generateWhere(cb);

    cb.append(')');
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
    String str = "(" + _left;

    switch (_token) {
    case QueryParser.EQ:
      str += " = ";
      break;
    case QueryParser.NE:
      str += " <> ";
      break;
    case QueryParser.LT:
      str += " < ";
      break;
    case QueryParser.LE:
      str += " <= ";
      break;
    case QueryParser.GT:
      str += " > ";
      break;
    case QueryParser.GE:
      str += " >= ";
      break;
    case '+':
      str += " + ";
      break;
    case '-':
      str += " - ";
      break;
    case '*':
      str += " * ";
      break;
    case '/':
      str += " / ";
      break;
    case '%':
      str += " % ";
      break;
    }

    return str + _right + ")";
  }
}
