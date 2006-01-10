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

import java.util.ArrayList;
import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.parser.PhpParser;

import com.caucho.quercus.program.AnalyzeInfo;

/**
 * Represents a PHP list assignment expression.
 */
public class ListExpr extends Expr {
  private final Expr []_varList;
  private final Value []_keyList;
  private final Expr _value;

  private ListExpr(Expr []varList, Expr value)
    throws IOException
  {
    _varList = varList;

    _keyList = new Value[varList.length];

    for (int i = 0; i < varList.length; i++)
      _keyList[i] = new LongValue(i);

    _value = value;
  }

  public static Expr create(PhpParser parser,
			    ArrayList<Expr> varList, Expr value)
    throws IOException
  {
    Expr []vars = new Expr[varList.size()];

    varList.toArray(vars);

    for (int i = 0; i < vars.length; i++) {
      if (vars[i] != null)
	vars[i].assign(parser);
    }

    boolean isSuppress = value instanceof SuppressErrorExpr;

    if (isSuppress) {
      SuppressErrorExpr suppressExpr = (SuppressErrorExpr) value;

      value = suppressExpr.getExpr();
    }

    Expr expr;
    
    if (value instanceof EachExpr)
      expr = new ListEachExpr(vars, (EachExpr) value);
    else
      expr = new ListExpr(vars, value);

    if (isSuppress)
      return new SuppressErrorExpr(expr);
    else
      return expr;
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
    Value value = _value.eval(env);

    int len = _varList.length;
    
    for (int i = 0; i < len; i++) {
      if (_varList[i] != null)
	_varList[i].evalAssign(env, value.get(_keyList[i]).copy());
    }
    
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
    return eval(env).copy();
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    // XXX: should be unique (?)
    info.getFunction().addTempVar("_quercus_list");
    
    _value.analyze(info);

    for (int i = 0; i < _varList.length; i++) {
      if (_varList[i] != null)
	_varList[i].analyzeAssign(info);
    }
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    String var = "_quercus_list";

    int count = 1;
    
    out.print("env.first(" + var + " = ");
    _value.generate(out);

    VarInfo varInfo = new VarInfo(var, null);
    VarExpr varExpr = new PhpVarExpr(varInfo);

    varExpr.setVarState(VarState.VALID);

    for (int i = 0; i < _varList.length; i++) {
      if (_varList[i] == null)
	continue;
      
      out.print(", ");

      if (i > 0 && i % 4 == 0) {
	out.print("env.first(");
	count++;
      }

      Expr refExpr = new ArrayGetExpr(varExpr, new LongLiteralExpr(i));
      Expr assign = _varList[i].createAssign(null, refExpr);

      assign.generate(out);
    }

    for (; count > 0; count--)
      out.print(")");
  }
}

