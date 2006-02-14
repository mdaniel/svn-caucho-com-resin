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
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.DoubleValue;

import com.caucho.quercus.parser.PhpParser;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP error suppression
 */
public class SuppressErrorExpr extends UnaryExpr {
  public SuppressErrorExpr(Expr expr)
  {
    super(expr);
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssign(PhpParser parser, Expr value)
    throws IOException
  {
    // php/03j2
    
    return new SuppressErrorExpr(getExpr().createAssign(parser, value));
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssignRef(PhpParser parser, Expr value)
    throws IOException
  {
    // php/03j2
    
    return new SuppressErrorExpr(getExpr().createAssignRef(parser, value));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
    throws Throwable
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.eval(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }
  
  /**
   * Evaluates the expression as a boolean.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
    throws Throwable
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.evalBoolean(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }
  
  /**
   * Evaluates the expression as a string.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public String evalString(Env env)
    throws Throwable
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.evalString(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }
  
  /**
   * Evaluates the expression, copying the results as necessary
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
    throws Throwable
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.evalCopy(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }

  //
  // Java code generation
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.suppress(env.setErrorMask(0), ");
    _expr.generate(out);
    out.println(")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    // php/3a5s
    out.print("env.suppress(env.setErrorMask(0), ");
    _expr.generateCopy(out);
    out.println(")");
  }
  
  public String toString()
  {
    return "@" + _expr;
  }
}

