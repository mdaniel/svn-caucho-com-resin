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

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.AnalyzeInfo;

/**
 * Represents a PHP assignment expression.
 */
public class AssignRefExpr extends Expr {
  private final AbstractVarExpr _var;
  private final Expr _value;

  public AssignRefExpr(AbstractVarExpr var, Expr value)
  {
    _var = var;
    _value = value;
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
    Value value = _value.evalRef(env);

    _var.evalAssign(env, value);
    
    return value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
    throws Throwable
  {
    Value value = _value.evalRef(env);

    _var.evalAssign(env, value);
    
    return value.copy();
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    _var.analyzeAssign(info);
    _var.analyzeSetReference(info);

    _value.analyze(info);
    _value.analyzeSetReference(info);
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _var.generateAssignRef(out, _value, false);
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out)
    throws IOException
  {
    _var.generateAssignRef(out, _value, true);
  }

  /**
   * Generates code to evaluate the expression, copying the result
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".copy()");  // php/3a5r
  }
  
  public String toString()
  {
    return "(" + _var + " =& " + _value + ")";
  }
}

