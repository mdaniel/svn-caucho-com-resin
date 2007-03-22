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
 * A unary expression
 */
class UnaryExpr extends Expr {
  // unary operation
  private int _op;
  // main expression
  private Expr _expr;

  /**
   * Creates a unary expression.
   *
   * @param op the operation
   * @param expr the expression
   */
  UnaryExpr(int op, Expr expr)
    throws ConfigException
  {
    _op = op;
    _expr = expr;

    evalTypes();
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    if (getJavaType() != null)
      return;

    switch (_op) {
    case '+':
      if (! _expr.isNumeric())
        throw error(L.l("'+' expects numeric expression at '{0}'", _expr));
      setJavaType(_expr.getJavaType());
      break;
    case '-':
      if (! _expr.isNumeric())
        throw error(L.l("'-' expects numeric expression at '{0}'", _expr));
      setJavaType(_expr.getJavaType());
      break;
    case Query.NOT:
      if (! _expr.isBoolean())
        throw error(L.l("NOT expects boolean expression at '{0}'", _expr));
      setJavaType(boolean.class);
      break;

    default:
      throw new RuntimeException();
    }
  }
  

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    switch (_op) {
    case '+':
      cb.append("+");
      break;
    case '-':
      cb.append("-");
      break;
    case Query.NOT:
      cb.append("NOT ");
      break;
    }
    
    _expr.generateWhereSubExpr(cb);
  }
}
