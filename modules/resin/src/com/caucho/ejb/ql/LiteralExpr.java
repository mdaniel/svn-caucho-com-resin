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
 * Literal expression for EJB-QL.
 */
class LiteralExpr extends Expr {
  // literal value
  private String _value;

  /**
   * Creates a new literal expression.
   *
   * @param value the string value of the literal
   * @param type the java type of the literal
   */
  LiteralExpr(String value, Class javaType)
  {
    _value = value;
    
    setJavaType(javaType);
  }

  /**
   * Returns the literal value
   */
  String getValue()
  {
    return _value;
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    cb.append(_value);
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param c the java code generator
   */
  void printSelect(CharBuffer cb)
    throws ConfigException
  {
    cb.append(_value);
  }

  public String toString()
  {
    return _value;
  }
}
