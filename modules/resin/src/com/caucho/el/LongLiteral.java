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

package com.caucho.el;

import java.io.*;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.vfs.*;

/**
 * Represents a long literal expression.
 */
public class LongLiteral extends Expr {
  private long _value;
  private Long _objValue;

  /**
   * Creates the literal with its value.
   */
  public LongLiteral(long value)
  {
    _value = value;
    _objValue = new Long(value);
  }

  /**
   * Returns true if the expression is constant.
   */
  public boolean isConstant()
  {
    return true;
  }

  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the long value as a Long
   */
  public Object evalObject(VariableResolver env)
    throws ELException
  {
    return _objValue;
  }

  /**
   * Evaluate the expression as an object as a long.
   *
   * @param env the variable environment
   *
   * @return the long value
   */
  public long evalLong(VariableResolver env)
    throws ELException
  {
    return _value;
  }

  /**
   * Evaluate the expression as a double.
   *
   * @param env the variable environment
   *
   * @return the double value
   */
  public double evalDouble(VariableResolver env)
    throws ELException
  {
    return (double) _value;
  }

  /**
   * Evalutes directly to the output.
   *
   * @param out the output stream
   * @param env the variable environment
   */
  public void print(WriteStream out, VariableResolver env)
    throws IOException, ELException
  {
    out.print(_value);
  }

  /**
   * Evaluates directly to the output.
   *
   * @param out the output stream
   * @param env the variable environment
   */
  public void printEscaped(WriteStream out, VariableResolver env)
    throws IOException, ELException
  {
    out.print(_value);
  }

  /**
   * Prints the code to create an LongLiteral.
   *
   * @param os the stream to the generated *.java file
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.LongLiteral(");
    os.print(_value);
    os.print("L)");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof LongLiteral))
      return false;

    LongLiteral literal = (LongLiteral) o;

    return _value == literal._value;
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return String.valueOf(_value);
  }
}
