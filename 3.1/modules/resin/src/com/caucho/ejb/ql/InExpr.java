/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;

/**
 * An 'in' expression
 */
class InExpr extends Expr {
  // value expression
  private Expr _expr;
  // args
  private ArrayList<Expr> _args;
  // true if this is a negative between
  private boolean _isNot;

  /**
   * Creates an 'in' expression.
   *
   * @param expr the value expression
   * @param args the args
   * @param isNot if true, this the like is negated
   */
  InExpr(Query query, Expr expr, ArrayList<Expr> args, boolean isNot)
    throws ConfigException
  {
    _query = query;
    
    _expr = expr;
    _args = args;
    _isNot = isNot;

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

    for (int i = 0; i < _args.size(); i++) {
      Expr expr = _args.get(i);

      if (! expr.isString())
        throw error(L.l("'IN' literal requires strings at `{0}'",
                        expr));
    }
    
    setJavaType(boolean.class);
  }
  

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    _expr.generateWhereSubExpr(cb);

    if (_isNot)
      cb.append(" NOT ");

    cb.append(" IN (");
    for (int i = 0; i < _args.size(); i++) {
      Expr arg = _args.get(i);

      if (i != 0)
        cb.append(", ");
      
      arg.generateWhereSubExpr(cb);
    }
    cb.append(")");
  }

  
  public String toString()
  {
    String str = _expr.toString();

    if (_isNot)
      str += " NOT ";

    str += " IN (";
    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        str += ", ";
      
      Expr arg = _args.get(i);

      str += arg;
    }
    str += ")";

    return str;
  }
}
