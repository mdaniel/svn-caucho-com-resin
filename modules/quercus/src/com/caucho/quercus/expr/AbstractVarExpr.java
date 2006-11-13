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

import com.caucho.quercus.parser.QuercusParser;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.Statement;
import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.program.ExprStatement;
import com.caucho.quercus.Location;

/**
 * Represents an expression that is assignable
 */
abstract public class AbstractVarExpr extends Expr {
  public AbstractVarExpr(Location location)
  {
    super(location);
  }
  
  public AbstractVarExpr()
  {
  }
  /**
   * Marks the value as assigned
   */
  public void assign(QuercusParser parser)
  {
    // XXX: used by list, e.g. quercus/03l8.  need further tests
  }

  /**
   * Creates the assignment.
   */
  @Override
  public Expr createAssign(QuercusParser parser, Expr value)
  {
    return parser.getExprFactory().createAssign(this, value);
  }

  /**
   * Creates the assignment.
   */
  @Override
  public Expr createAssignRef(QuercusParser parser,
                              Expr value)
  {
    return parser.getExprFactory().createAssignRef(this, value);
  }

  /**
   * Creates the reference
   * @param location
   */
  @Override
  public Expr createRef(QuercusParser parser)
  {
    return parser.getExprFactory().createRef(this);
  }

  /**
   * Creates the copy.
   * @param location
   */
  public Expr createCopy(Location location)
  {
    return new CopyExpr(location, this);
  }

  /**
   * Creates the assignment.
   */
  public Statement createUnset(Location location)
  {
    return new ExprStatement(location, new UnsetVarExpr(location, this));
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public Value eval(Env env);

  /**
   * Evaluates the expression as a reference (by RefExpr).
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public Value evalRef(Env env);

  /**
   * Evaluates the expression as an argument.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public Value evalArg(Env env);

  /**
   * Evaluates the expression as an argument.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public void evalUnset(Env env);

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public void evalAssign(Env env, Value value);

  //
  // Java code generation
  //

  /**
   * Analyze a variable assignment
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Analyze the unset assignment
   */
  public void analyzeUnset(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  abstract public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException;

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  abstract public void generateAssignRef(PhpWriter out,
                                         Expr value,
                                         boolean isTop)
    throws IOException;

  /**
   * Generates code to evaluate the unset expression
   *
   * @param out the writer to the Java source code.
   */
  abstract public void generateUnset(PhpWriter out)
    throws IOException;
}

