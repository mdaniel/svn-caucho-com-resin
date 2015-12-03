/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.db.sql;

import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.logging.Logger;

public class MaxExpr extends FunExpr implements GroupExpr {
  private static final L10N L = new L10N(MaxExpr.class);

  private Expr _expr;
  private int _dataField;

  protected void addArg(Expr expr)
    throws SQLException
  {
    if (_expr != null)
      throw new SQLException(L.l("max requires a single argument"));

    _expr = expr;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _dataField = query.getDataFields();

    query.setDataFields(_dataField + 1);
    query.setGroup(true);

    _expr = _expr.bind(query);
    
    return this;
  }

  /**
   * Returns the expected result type of the expression.
   */
  public Class getType()
  {
    return _expr.getType();
  }

  /**
   * Evaluates aggregate functions during the group phase.
   *
   * @param context the current database tuple
   */
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    if (_expr.isNull(context))
      return;
    
    double value = _expr.evalDouble(context);
    double oldValue = context.getGroupDouble(_dataField);
    
    if (context.isGroupNull(_dataField))
      context.setGroupDouble(_dataField, value);
    else if (oldValue < value)
      context.setGroupDouble(_dataField, value);
  }

  /**
   * Returns true if the value is null.
   *
   * @param queryContext the query context
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return context.isGroupNull(_dataField);
  }

  /**
   * Evaluates the expression as a double.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the double value
   */
  public double evalDouble(QueryContext context)
    throws SQLException
  {
    if (context.isGroupNull(_dataField))
      return 0;
    else
      return context.getGroupDouble(_dataField);
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the long value
   */
  public long evalLong(QueryContext context)
    throws SQLException
  {
    return (long) evalDouble(context);
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the string value
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    if (context.isGroupNull(_dataField))
      return null;
    
    double value = context.getGroupDouble(_dataField);;
    
    return String.valueOf((long) value);
  }

  public String toString()
  {
    return "max(" + _expr + ")";
  }
}
