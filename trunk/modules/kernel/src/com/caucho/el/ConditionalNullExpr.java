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
 *
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;

/**
 * Represents a conditional expression
 */
public class ConditionalNullExpr extends Expr {
  private Expr _value;
  private Expr _defaultExpr;

  /**
   * Creates the conditional expression.
   *
   * @param test the conditional expressions test.
   * @param trueExpr the true subexpression
   * @param falseExpr the false subexpression
   */
  public ConditionalNullExpr(Expr value, Expr defaultExpr)
  {
    _value = value;
    _defaultExpr = defaultExpr;
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return (_value.isConstant() &&
            _defaultExpr.isConstant());
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the result as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object value = _value.getValue(env);
    
    if (value != null && ! "".equals(value))
      return value;
    else
      return _defaultExpr.getValue(env);
  }

  /**
   * Prints the Java code to recreate the expr
   *
   * @param os the output stream to the *.java file
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.ConditionalNullExpr(");
    _value.printCreate(os);
    os.print(", ");
    _defaultExpr.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ConditionalNullExpr))
      return false;

    ConditionalNullExpr expr = (ConditionalNullExpr) o;

    return (_value.equals(expr._value) &&
            _defaultExpr.equals(expr._defaultExpr));
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return "(" + _value + " ?: " + _defaultExpr + ")";
  }
}
