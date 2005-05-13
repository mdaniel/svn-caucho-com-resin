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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.query;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.StringType;

import com.caucho.amber.entity.EntityItem;

import com.caucho.amber.connection.AmberConnectionImpl;

/**
 * Represents an Amber query expression
 */
abstract public class AbstractAmberExpr implements AmberExpr {
  private static final L10N L = new L10N(AbstractAmberExpr.class);
  
  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns the expr type.
   */
  public Type getType()
  {
    return StringType.create();
  }

  /**
   * Converts to a boolean expression.
   */
  public AmberExpr createBoolean()
    throws QueryParseException
  {
    if (isBoolean())
      return this;
    else
      throw new QueryParseException(L.l("'{0}' can't be used as a boolean",
				   this));
  }

  /**
   * Binds the expression as a select item.
   */
  abstract public AmberExpr bindSelect(QueryParser parser);

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type)
  {
    return usesFrom(from, type, false);
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return false;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    return this;
  }

  /**
   * Returns the number of columns.
   */
  public int getColumnCount()
  {
    return 1;
  }
  
  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Generates the where in a join expression.
   */
  public void generateJoin(CharBuffer cb)
  {
    generateWhere(cb);
  }
  
  /**
   * Generates the select expression.
   */
  public void generateSelect(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnectionImpl aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getType().getObject(aConn, rs, index);
  }

  /**
   * Returns the object for the expr.
   */
  public Object getCacheObject(AmberConnectionImpl aConn,
			       ResultSet rs, int index)
    throws SQLException
  {
    return getObject(aConn, rs, index);
  }

  /**
   * Returns the object for the expr.
   */
  public EntityItem findItem(AmberConnectionImpl aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getType().findItem(aConn, rs, index);
  }
}
