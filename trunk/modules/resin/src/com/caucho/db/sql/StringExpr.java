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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

class StringExpr extends Expr {
  private String _value;

  StringExpr(String value)
  {
    _value = value;
  }

  /**
   * Returns the value.
   */
  public String getValue()
  {
    return _value;
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return String.class;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return 0;
  }

  /**
   * Returns true if the literal is null.
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return _value == null;
  }

  /**
   * Evaluates the literal as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    return _value;
  }

  /**
   * Evaluates the expression to a buffer
   *
   * @param result the result buffer
   *
   * @return the length of the result
   */
  @Override
  public int evalToBuffer(QueryContext context,
                          byte []buffer,
                          int offset)
    throws SQLException
  {
    String v = evalString(context);

    int len = v.length();
    for (int i = 0; i < len; i++) {
      buffer[offset + i] = (byte) v.charAt(i);
    }

    return len;
  }

  /**
   * Evaluates the expression to a buffer
   *
   * @param result the result buffer
   *
   * @return the length of the result
   */
  @Override
  public byte []evalBytes(QueryContext context)
    throws SQLException
  {
    String v = evalString(context);
    
    if (v == null)
      return null;
    
    int len = v.length();
    byte []value = new byte[v.length()];
    
    for (int i = 0; i < len; i++) {
      value[i] = (byte) v.charAt(i);
    }

    return value;
  }

  public String toString()
  {
    return "'" + _value + "'";
  }
}
