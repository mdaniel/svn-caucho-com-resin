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

package com.caucho.php.program;

import java.io.IOException;

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;

import com.caucho.php.expr.Expr;
import com.caucho.php.expr.VarExpr;
import com.caucho.php.expr.VarState;

import com.caucho.php.gen.PhpWriter;

import com.caucho.vfs.WriteStream;

/**
 * Represents an expression statement in a PHP program.
 */
public class ExprStatement extends Statement {
  private Expr _expr;
  
  /**
   * Creates the expression statement.
   */
  public ExprStatement(Expr expr)
  {
    _expr = expr;
  }

  /**
   * Returns the expression.
   */
  public Expr getExpr()
  {
    return _expr;
  }
  
  public Value execute(Env env)
    throws Throwable
  {
    _expr.evalTop(env);

    return null;
  }

  //
  // java generation code
  //

  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);

    return true;
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   */
  public VarState getVarState(VarExpr var,
			      Statement stmtOwner,
			      VarExpr exprOwner)
  {
    return _expr.getVarState(var, exprOwner);
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExpr var)
  {
    return _expr.isVarAssigned(var);
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _expr.generateStatement(out);
  }

  /**
   * Generates static/initialization code code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateCoda(PhpWriter out)
    throws IOException
  {
  }

  /**
   * Disassembly.
   */
  public void debug(JavaWriter out)
    throws IOException
  {
    out.println(_expr + ";");
  }
  
  public String toString()
  {
    return "ExprStatement[]";
  }
}

