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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents a PHP comparison expression.
 */
public class LeqExpr extends BinaryExpr {
  public LeqExpr(Location location, Expr left, Expr right)
  {
    super(location, left, right);
  }

  public LeqExpr(Expr left, Expr right)
  {
    super(left, right);
  }

  /**
   * Returns true for a boolean.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Evaluates the equality as a boolean.
   */
  public Value eval(Env env)
  {
    return evalBoolean(env) ? BooleanValue.TRUE : BooleanValue.FALSE;
  }

  /**
   * Evaluates the equality as a boolean.
   */
  public boolean evalBoolean(Env env)
  {
    Value lValue = _left.eval(env);
    Value rValue = _right.eval(env);

    return lValue.leq(rValue);
  }

  //
  // java generation code
  //

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.toValue(");
    generateBoolean(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    if (_left.isLong() && _right.isLong()) {
      out.print("(");
      _left.generateLong(out);
      out.print(" <= ");
      _right.generateLong(out);
      out.print(")");
    }
    else if (_left.isNumber() || _right.isNumber()) {
      out.print("(");
      _left.generateDouble(out);
      out.print(" <= ");
      _right.generateDouble(out);
      out.print(")");
    }
    else {
      _left.generate(out);
      out.print(".leq(");
      _right.generate(out);
      out.print(")");
    }
  }

  public String toString()
  {
    return "(" + _left + " <= " + _right + ")";
  }
}

