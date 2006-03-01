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

import java.util.ArrayList;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Represents a PHP function expression.
 */
public class VarMethodCallExpr extends Expr {
  private static final L10N L = new L10N(VarMethodCallExpr.class);

  private final Expr _objExpr;
  
  protected final Expr _name;
  protected final Expr []_args;

  private Expr []_fullArgs;

  private AbstractFunction _fun;

  public VarMethodCallExpr(Expr objExpr, Expr name, ArrayList<Expr> args)
  {
    _objExpr = objExpr;
    
    _name = name;

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public VarMethodCallExpr(Expr objExpr, Expr name, Expr []args)
  {
    _objExpr = objExpr;
    
    _name = name;

    _args = args;
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
    Value []values = new Value[_args.length];

    for (int i = 0; i < values.length; i++) {
      values[i] = _args[i].evalArg(env);
    }
    
    return _objExpr.eval(env).evalMethod(env, _name.evalString(env), values);
  }

  //
  // java code generation
  //
  
  /**
   * Analyzes the function.
   */
  public void analyze(AnalyzeInfo info)
  {
    _objExpr.analyze(info);
    _name.analyze(info);
    
    for (int i = 0; i < _args.length; i++) {
      _args[i].analyze(info);
    }
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _objExpr.generate(out);
    out.print(".evalMethod(env, ");
    _name.generateString(out);

    if (_args.length <= 5) {
      for (int i = 0; i < _args.length; i++) {
	out.print(", ");
      
	_args[i].generateArg(out);
      }
    }
    else {
      out.print(", new Value[] {");

      for (int i = 0; i < _args.length; i++) {
	if (i != 0)
	  out.print(", ");
      
	_args[i].generateArg(out);
      }
      
      out.print("}");
    }
      
    out.print(")");
  }
  
  public String toString()
  {
    return _objExpr + "->" + _name + "()";
  }
}

