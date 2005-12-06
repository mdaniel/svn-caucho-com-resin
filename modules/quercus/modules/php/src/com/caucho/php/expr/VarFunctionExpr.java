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

package com.caucho.php.expr;

import java.io.IOException;

import java.util.ArrayList;

import java.lang.reflect.Method;

import com.caucho.php.Php;

import com.caucho.php.gen.PhpWriter;

import com.caucho.php.env.Env;
import com.caucho.php.env.StringValue;
import com.caucho.php.env.Value;

import com.caucho.php.program.AbstractFunction;
import com.caucho.php.program.PhpProgram;
import com.caucho.php.program.AnalyzeInfo;

import com.caucho.util.L10N;

/**
 * Represents a PHP function expression.
 */
public class VarFunctionExpr extends Expr {
  private static final L10N L = new L10N(FunctionExpr.class);
  private final Expr _name;
  private final Expr []_args;

  public VarFunctionExpr(Expr name, ArrayList<Expr> args)
  {
    _name = name;

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public VarFunctionExpr(Expr name, Expr []args)
  {
    _name = name;

    _args = args;
  }

  /**
   * Returns the reference of the value.
   */
  public Expr createRef()
  {
    return new RefExpr(this);
  }

  /**
   * Returns the copy of the value.
   */
  public Expr createCopy()
  {
    return this;
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
    return env.getFunction(_name.eval(env)).eval(env, _args);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
    throws Throwable
  {
    return env.getFunction(_name.eval(env)).evalRef(env, _args);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
    _name.analyze(info);

    for (int i = 0; i < _args.length; i++)
      _args[i].analyze(info);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    generateImpl(out, true);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  private void generateImpl(PhpWriter out, boolean isRef)
    throws IOException
  {
    out.print("env.getFunction(");
    _name.generate(out);
    out.print(")");

    if (isRef)
      out.print(".evalRef(env");
    else
      out.print(".eval(env");
    
    if (_args.length <= 5) {
      for (int i = 0; i < _args.length; i++) {
	out.print(", ");

	_args[i].generateArg(out);
      }

      out.print(")");
    }
    else {
      out.print(", new Value[] {");

      for (int i = 0; i < _args.length; i++) {
	if (i != 0)
	  out.print(", ");

	_args[i].generateArg(out);
      }
      out.print("})");
    }
  }
  
  public String toString()
  {
    return _name + "()";
  }
}

