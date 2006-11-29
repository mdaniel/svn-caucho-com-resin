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

import java.util.ArrayList;

/**
 * A binary expression
 */
class AndExpr extends Expr {
  // components to the and
  private ArrayList<Expr> _components = new ArrayList<Expr>();

  /**
   * Creates an and expression.
   */
  AndExpr(Query query)
    throws ConfigException
  {
    setJavaType(boolean.class);
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    setJavaType(boolean.class);
  }

  /**
   * Adds a subexpression.
   */
  void add(Expr expr)
  {
    _components.add(expr);
  }

  /**
   * Return the expression if single.
   */
  Expr getSingleExpr()
  {
    if (_components.size() == 1)
      return _components.get(0);
    else
      return this;
  }
  
  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    if (_components.size() == 1) {
      Expr comp = _components.get(0);

      comp.generateWhere(cb);
      return;
    }

    for (int i = 0; i < _components.size(); i++) {
      Expr comp = _components.get(i);

      if (i != 0)
	cb.append(" AND ");

      comp.generateWhereSubExpr(cb);
    }
  }

  /**
   * Prints the where SQL for this expression
   */
  void generateWhereSubExpr(CharBuffer cb)
  {
    cb.append("(");
    generateWhere(cb);
    cb.append(")");
  }
  
  void printSelect(CharBuffer cb)
    throws ConfigException
  {
    cb.append("(");
    generateWhere(cb);
    cb.append(")");
  }

  public String toString()
  {
    CharBuffer value = CharBuffer.allocate();

    value.append("(");

    for (int i = 0; i < _components.size(); i++) {
      Expr comp = _components.get(i);

      if (i != 0)
	value.append(" AND ");

      value.append(comp.toString());
    }

    value.append(")");

    return value.close();
  }
}
