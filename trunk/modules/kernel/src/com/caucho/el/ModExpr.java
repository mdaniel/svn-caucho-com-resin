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
 *   Free Software Foundation, Inc.
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
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a binary mod numeric operation
 */
public class ModExpr extends Expr {
  private final Expr _left;
  private final Expr _right;
  
  /**
   * Creates the multiplication expression.
   *
   * @param left the left sub-expression
   * @param right the right sub-expression
   */
  public ModExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return _left.isConstant() && _right.isConstant();
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
    Object aObj = _left.getValue(env);
    Object bObj = _right.getValue(env);

    if (aObj instanceof BigDecimal
        || isDouble(aObj)
        || bObj instanceof BigDecimal
        || isDouble(bObj)) {
      double a = toDouble(aObj, env);
      double b = toDouble(bObj, env);
      
      return new Double(a % b);
    }
    else if (aObj instanceof BigInteger
             || bObj instanceof BigInteger) {
      BigInteger a = toBigInteger(aObj, env);
      BigInteger b = toBigInteger(bObj, env);
      
      return a.remainder(b);
    }
    else if (aObj == null && bObj == null)
      return new Long(0);
    else {
      long a = toLong(aObj, env);
      long b = toLong(bObj, env);

      return new Long(a % b);
    }
  }
  
  /**
   * Evaluate the expression as a long
   *
   * @param env the variable environment
   *
   * @return the result as an long
   */
  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    long a = _left.evalLong(env);
    long b = _right.evalLong(env);

    return a % b;
  }
  
  /**
   * Evaluate the expression as a double
   *
   * @param env the variable environment
   *
   * @return the result as an double
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    double a = _left.evalDouble(env);
    double b = _right.evalDouble(env);

    return a % b;
  }

  /**
   * Prints the Java code to recreate an LongLiteral.
   *
   * @param os the output stream to the *.java file
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.ModExpr(");
    _left.printCreate(os);
    os.print(", ");
    _right.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ModExpr))
      return false;

    ModExpr expr = (ModExpr) o;

    return (_left.equals(expr._left) &&
            _right.equals(expr._right));
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return "(" + _left + " + " + _right + ")";
  }
}
