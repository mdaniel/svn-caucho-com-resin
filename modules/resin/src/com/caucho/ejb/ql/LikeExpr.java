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

import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

/**
 * A like expression
 */
class LikeExpr extends Expr {
  // value expression
  private Expr _value;
  // match pattern
  private Expr _pattern;
  // escape string
  private String _escape;
  // true if this is a negative between
  private boolean _isNot;

  /**
   * Creates a like expression.
   *
   * @param value the value expression
   * @param pattern the matching pattern
   * @param escape the escape pattern
   * @param isNot if true, this the like is negated
   */
  LikeExpr(Expr value, Expr pattern, String escape, boolean isNot)
    throws ConfigException
  {
    _value = value;
    _pattern = pattern;
    _escape = escape;
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

    if (! _value.isString())
      throw error(L.l("LIKE value `{0}' must be a string expression",
                      _value));

    if (! _pattern.isString())
      throw error(L.l("LIKE pattern `{0}' must be a string expression.",
                      _pattern)); 

    setJavaType(boolean.class);
  }
  

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    _value.generateWhereSubExpr(cb);

    if (_isNot)
      cb.append(" NOT LIKE ");
    else
      cb.append(" LIKE ");

    _pattern.generateWhereSubExpr(cb);

    if (_escape != null) {
      cb.append(" ESCAPE ");
      cb.append(_escape);
    }
  }

  public String toString()
  {
    String not = _isNot ? "NOT " : "";

    if (_escape != null)
      return not + _pattern + " ESCAPE " + _escape;
    else
      return not + _pattern;
  }
}
