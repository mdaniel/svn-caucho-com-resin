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
import com.caucho.util.*;

/**
 * Representing a string interpolation expression.
 */
public class InterpolateExpr extends Expr {
  private Expr _left;
  private Expr _right;

  /**
   * Create a new interpolation expression.
   *
   * @param left the left subexpression
   * @param right the right subexpression
   */
  public InterpolateExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  /**
   * Returns true if this is a constant expression.
   */
  public boolean isConstant()
  {
    return _left.isConstant() && _right.isConstant();
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the string value
   */
  public Object evalObject(VariableResolver env)
    throws ELException
  {
    return evalString(env);
  }
  
  /**
   * Evaluate the expression as an object.
   */
  public String evalString(VariableResolver env)
    throws ELException
  {
    CharBuffer cb = CharBuffer.allocate();

    Expr expr = this;

    for (;
         expr instanceof InterpolateExpr;
         expr = ((InterpolateExpr) expr)._right) {
      InterpolateExpr subExpr = (InterpolateExpr) expr;

      String value = subExpr._left.evalString(env);

      if (value != null)
        cb.append(value);
    }

    String value = expr.evalString(env);
    if (value != null)
      cb.append(value);
    
    return cb.close();
  }

  /**
   * Prints the interpolated value directly to the output.
   *
   * @param out the output stream
   * @param env the variable environment
   * @param escapeXml if true, then escape the output
   *
   * @return false, since the interpolated value is never null
   */
  public boolean print(WriteStream out,
                       VariableResolver env,
                       boolean escapeXml)
    throws IOException, ELException
  {
    _left.print(out, env, escapeXml);
    _right.print(out, env, escapeXml);

    return false;
  }

  /**
   * Prints the code to create an LongLiteral.
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.InterpolateExpr(");
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
    if (! (o instanceof InterpolateExpr))
      return false;

    InterpolateExpr expr = (InterpolateExpr) o;

    return _left.equals(expr._left) && _right.equals(expr._right);
  }
}
