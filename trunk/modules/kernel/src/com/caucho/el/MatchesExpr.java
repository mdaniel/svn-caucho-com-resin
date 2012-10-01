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

import java.io.IOException;
import java.util.regex.*;

import javax.el.*;

import com.caucho.vfs.WriteStream;

/**
 * Represents the regular expression operation: matches
 */
public class MatchesExpr extends AbstractBooleanExpr {
  private final Expr _left;
  private final Expr _right;

  /**
   * Creates a comparison expression
   *
   * @param op the lexical code for the operation
   * @param left the left subexpression
   * @param right the right subexpression
   */
  public MatchesExpr(Expr left, Expr right)
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
   * Evaluate the expression as a boolean.
   *
   * @param env the variable environment
   */
  @Override
  public boolean evalBoolean(ELContext env)
    throws ELException
  {
    Object aObj = _left.getValue(env);
    Object bObj = _right.getValue(env);
    
    if (aObj == null || bObj == null)
      return false;
    
    String a = toString(aObj, env);
    String b = toString(bObj, env);
    
    try {
      return Pattern.compile(b).matcher(a).find();
    } catch (PatternSyntaxException e) {
      throw new ELException(e);
    }
  }

  /**
   * Prints the code to create an LongLiteral.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.MatchesExpr(");
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
    if (! (o instanceof MatchesExpr))
      return false;

    MatchesExpr expr = (MatchesExpr) o;

    return (_left.equals(expr._left) &&
            _right.equals(expr._right));
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return "(" + _left + " matches " + _right + ")";
  }
}
