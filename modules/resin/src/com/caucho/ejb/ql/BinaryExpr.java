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

package com.caucho.ejb.ql;

import com.caucho.config.ConfigException;
import com.caucho.util.CharBuffer;

/**
 * A binary expression
 */
class BinaryExpr extends Expr {
  // binary operation
  private int _op;
  // left expression
  private Expr _left;
  // right expression
  private Expr _right;

  /**
   * Creates a binary expression.
   *
   * @param op the operation
   * @param left the left expression
   * @param right the right expression
   */
  BinaryExpr(Query query, int op, Expr left, Expr right)
    throws ConfigException
  {
    _query = query;
    _op = op;
    _left = left;
    _right = right;

    evalTypes();
  }

  Expr getLeft()
  {
    return _left;
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    /*
    if (_left.getComponentCount() != _right.getComponentCount()) {
      throw error(L.l("`{0}' has mismatched types `{1}' != `{2}'",
                      this, _left.getJavaType().getName(),
                      _right.getJavaType().getName()));
    }
    */
    
    switch (_op) {
    case Query.EQ:
    case Query.NE:
      if (! _left.getJavaType().equals(_right.getJavaType()) &&
          _left.isNumeric() != _right.isNumeric() &&
          ! (_left.isDate() && _right.isDate()))
        throw error(L.l("`{0}' has mismatched types `{1}' != `{2}'",
                        this, _left.getJavaType().getName(),
                        _right.getJavaType().getName()));
      setJavaType(boolean.class);
      break;
      
    case Query.LT:
    case Query.GT:
    case Query.LE:
    case Query.GE:
      if (_left.isDate() && _right.isDate()) {
      }
      else if (! _left.isNumeric())
        throw error(L.l("`{0}' expects a numeric value at {1}.  Less-than and greater-than comparisons only make sense with numbers and dates.",
                        this, _left.getJavaType().getName()));
      else if (! _right.isNumeric())
        throw error(L.l("`{0}' expects a numeric value at {1}.  Less-than and greater-than comparisons only make sense with numbers and dates.",
                        this, _right.getJavaType().getName()));
      
      setJavaType(boolean.class);
      break;
      
    case Query.AND:
    case Query.OR:
      if (! _left.isBoolean())
        throw error(L.l("`{0}' expects a boolean value at {1}",
                        _left, _left.getJavaType().getName()));
      if (! _right.isBoolean())
        throw error(L.l("`{0}' expects a boolean value at {1}",
                        _right, _right.getJavaType().getName()));
      
      setJavaType(boolean.class);
      break;

    case '+':
    case '-':
    case '*':
    case '/':
      if (! _left.isNumeric())
        throw error(L.l("`{0}' expects a numeric value at {1}",
                        _left, _left.getJavaType().getName()));
      if (! _right.isNumeric())
        throw error(L.l("`{0}' expects a numeric value at {1}",
                        _right, _right.getJavaType().getName()));

      /*
      if (int.class.isAssignableFrom(_left.getJavaType()) &&
          int.class.isAssignableFrom(_right.getJavaType()))
        setJavaType(int.class);
      else if (long.class.isAssignableFrom(_left.getJavaType()) &&
               long.class.isAssignableFrom(_right.getJavaType()))
        setJavaType(long.class);
      else
        setJavaType(double.class);
      */
      if (isInteger(_left.getJavaType()) &&
          isInteger(_right.getJavaType()))
        setJavaType(long.class);
      else
        setJavaType(double.class);
      break;

    default:
      throw new RuntimeException("unknown binary op:" + _op + " " + (char) _op);
    }
  }
  
  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    if (_left.isKey()) {
      int componentCount = _left.getComponentCount();
      
      generateWhereComponents(cb, componentCount);

      return;
    }
    else if (_right.isKey()) {
      int componentCount = _right.getComponentCount();
      
      generateWhereComponents(cb, componentCount);
      
      return;
    }
    
    //_left.generateWhereSubExpr(cb);
    _left.generateComponent(cb, 0);

    switch (_op) {
    case Query.EQ:
      cb.append(" = ");
      break;
    case Query.NE:
      cb.append(" <> ");
      break;
    case Query.LT:
      cb.append(" < ");
      break;
    case Query.GT:
      cb.append(" > ");
      break;
    case Query.LE:
      cb.append(" <= ");
      break;
    case Query.GE:
      cb.append(" >= ");
      break;
      
    case Query.AND:
      cb.append(" AND ");
      break;
    case Query.OR:
      cb.append(" OR ");
      break;

    default:
      cb.append(" " + ((char) _op) + " ");
      break;
    }

    //_right.generateWhereSubExpr(cb);
    _right.generateComponent(cb, 0);
  }

  private void generateWhereComponents(CharBuffer cb, int componentCount)
  {
    for (int i = 0; i < componentCount; i++) {
      if (i != 0)
        cb.append(" AND ");

      _left.generateComponent(cb, i);

      if (_op == Query.EQ)
        cb.append(" = ");
      else if (_op == Query.NE)
        cb.append(" <> ");

      _right.generateComponent(cb, i);
    }
  }

  /**
   * Generates the where SQL for this expression
   */
  void generateWhereSubExpr(CharBuffer cb)
  {
    cb.append("(");
    generateWhere(cb);
    cb.append(")");
  }
  
  void generateSelect(CharBuffer cb)
  {
    cb.append("(");
    generateWhere(cb);
    cb.append(")");
  }

  public String toString()
  {
    CharBuffer value = CharBuffer.allocate();

    if (_left instanceof BinaryExpr) {
      value.append("(");
      value.append(_left.toString());
      value.append(")");
    }
    else
      value.append(_left.toString());

    switch (_op) {
    case Query.EQ:
      value.append(" = ");
      break;
    case Query.NE:
      value.append(" <> ");
      break;
    case Query.LT:
      value.append(" < ");
      break;
    case Query.GT:
      value.append(" > ");
      break;
    case Query.LE:
      value.append(" <= ");
      break;
    case Query.GE:
      value.append(" >= ");
      break;
      
    case Query.AND:
      value.append(" AND ");
      break;
    case Query.OR:
      value.append(" OR ");
      break;

    default:
      value.append(" " + ((char) _op) + " ");
      break;
    }

    if (_right instanceof BinaryExpr) {
      value.append("(");
      value.append(_right.toString());
      value.append(")");
    }
    else
      value.append(_right.toString());

    return value.close();
  }
}
