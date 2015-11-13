/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.el;

import java.io.IOException;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;

/*
 * The semicolon operators behaves like the comma operator in C.
 * To evaluate A;B, A is first evaluated, and its value is discarded. B is 
 * then evaluated and its value is returned.
 */
public class SemicolonExpr extends Expr
{
  protected static final Logger log = 
    Logger.getLogger(SemicolonExpr.class.getName());
  protected static final L10N L = new L10N(SemicolonExpr.class);

  private final Expr _left;
  private final Expr _right;

  public SemicolonExpr(Expr left, Expr right)
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
    return (_right.isConstant() && _right.isConstant());
  }

  @Override
  public Object getValue(ELContext env) throws ELException
  {
    _left.getValue(env);
    return _right.getValue(env);
  }
  
  public void setValue(ELContext env, Object value)
  {
    _left.getValue(env);
    _right.setValue(env, value);
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
    os.print("new com.caucho.v5.el.SemicolonExpr(");
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
    if (! (o instanceof SemicolonExpr))
      return false;

    SemicolonExpr expr = (SemicolonExpr) o;

    return (_left.equals(expr._left) &&
            _right.equals(expr._right));
  }

  @Override
  public String toString()
  {
    return String.format("(%s ; %s)", _left, _right);
  }
}
