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

package com.caucho.ejb.ql;

import java.util.*;

import com.caucho.util.*;

import com.caucho.config.ConfigException;

/**
 * A between expression
 */
class BetweenExpr extends Expr {
  // value expression
  private Expr _value;
  // min expression
  private Expr _min;
  // max expression
  private Expr _max;
  // true if this is a negative between
  private boolean _isNot;

  /**
   * Creates a binary expression.
   *
   * @param value the value expression
   * @param min the minimum expression
   * @param max the maximum expression
   * @param isNot if true, this the between is negated
   */
  BetweenExpr(Expr value, Expr min, Expr max, boolean isNot)
    throws ConfigException
  {
    _value = value;
    _min = min;
    _max = max;
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

    setJavaType(boolean.class);
    
    if (_value.isDate() && _min.isDate() && _max.isDate())
      return;
    
    else if (_value.isDate())
      throw error(L.l("BETWEEN expects date expression at `{0}'.  All values must either be date values or all must be numeric values.", _value));

    else if (_min.isDate())
      throw error(L.l("BETWEEN expects date expression at `{0}'.  All values must either be date values or all must be numeric values.", _min));
    
    else if (_max.isDate())
      throw error(L.l("BETWEEN expects date expression at `{0}'.  All values must either be date values or all must be numeric values.", _max));

    if (! _value.isNumeric())
      throw error(L.l("BETWEEN expects numeric expression at `{0}'", _value));
    
    if (! _min.isNumeric())
      throw error(L.l("BETWEEN expects numeric expression at `{0}'", _min));
    
    if (! _max.isNumeric())
      throw error(L.l("BETWEEN expects numeric expression at `{0}'", _max));
  }
  

  /**
   * Prints the where SQL for this expression
   *
   * @param cb the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    _value.generateWhereSubExpr(cb);

    if (_isNot)
      cb.append(" NOT BETWEEN ");
    else
      cb.append(" BETWEEN ");

    _min.generateWhereSubExpr(cb);
              
    cb.append(" AND ");

    _max.generateWhereSubExpr(cb);
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

  public String toString()
  {
    if (_isNot)
      return "NOT BETWEEN " + _min + " AND " + _max;
    else
      return "BETWEEN " + _min + " AND " + _max;
  }
}
