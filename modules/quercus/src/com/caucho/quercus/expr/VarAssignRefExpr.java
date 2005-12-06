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

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.AnalyzeInfo;

/**
 * Represents a PHP assignment expression.
 */
public class VarAssignRefExpr extends Expr {
  private final VarExpr _var;
  private final String _varName;
  private final Expr _value;

  public VarAssignRefExpr(VarExpr var, Expr value)
  {
    _var = var;
    _varName = var.getName();
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

    // XXX: s/b direct
    env.setValue(_varName, value);
    
    return value;
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

    _value.analyze(info);
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   * @param owner the owning expression
   */
  public VarState getVarState(VarExpr var, VarExpr owner)
  {
    if (_var == owner)
      return VarState.UNKNOWN;
    else if (_var.equals(var))
      return VarState.VALID;
    else
      return _value.getVarState(var, owner);
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExpr var)
  {
    return _var.equals(var) || _value.isVarAssigned(var);
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
    out.println(";");
  }
  
  public String toString()
  {
    return "$" + _varName + "=&" + _value;
  }
}

