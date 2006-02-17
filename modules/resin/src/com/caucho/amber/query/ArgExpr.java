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

package com.caucho.amber.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.caucho.util.CharBuffer;

import com.caucho.amber.type.Type;

/**
 * Parameter argument expression.
 */
class ArgExpr extends AbstractAmberExpr {
  private QueryParser _parser;
  
  // argument index
  private int _index;
  
  private int _sqlIndex;


  /**
   * Creates a new argument expression.
   *
   * @param index the argument index
   */
  ArgExpr(QueryParser parser, int index)
  {
    _parser = parser;
    
    _index = index;
  }

  /**
   * Returns the index value
   */
  int getIndex()
  {
    return _index;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    parser.addArg(this);
		  
    return this;
  }

  /**
   * Generates the literal.
   */
  public void generateWhere(CharBuffer cb)
  {
    _sqlIndex = _parser.generateSQLArg();
    
    cb.append("?");
  }

  /**
   * Sets the parameter.
   */
  public void setParameter(PreparedStatement pstmt, int i,
			   Type []argTypes, Object []argValues)
    throws SQLException
  {
    if (argTypes[_index - 1] != null)
      argTypes[_index - 1].setParameter(pstmt, _sqlIndex + 1,
					argValues[_index - 1]);
    else
      pstmt.setString(_sqlIndex + 1, null);
  }

  public String toString()
  {
    return "?" + _index;
  }
}
