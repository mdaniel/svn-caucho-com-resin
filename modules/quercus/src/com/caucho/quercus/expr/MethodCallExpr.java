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

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.util.L10N;

/**
 * Represents a PHP function expression.
 */
public class MethodCallExpr extends Expr {
  private static final L10N L = new L10N(MethodCallExpr.class);

  private final Expr _objExpr;
  
  protected final String _name;
  protected final Expr []_args;

  public MethodCallExpr(Location location, Expr objExpr, String name, ArrayList<Expr> args)
  {
    super(location);
    _objExpr = objExpr;
    
    _name = name;

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public MethodCallExpr(Expr objExpr, String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, objExpr, name, args);
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
    env.checkTimeout();

    return _objExpr.eval(env).evalMethod(env, _name, _args);
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
    env.checkTimeout();
    
    return _objExpr.eval(env).evalMethodRef(env, _name, _args);
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
    
    for (int i = 0; i < _args.length; i++) {
      _args[i].analyze(info);
      
      _args[i].analyzeSetReference(info);
      _args[i].analyzeSetModified(info);
    }
  }

  public void generate(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
  }

  public void generateRef(PhpWriter out)
    throws IOException
  {
    generateImpl(out, true);
  }

  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
    out.print(".copyReturn()"); // php/3a5x
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateValue(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
    out.print(".toValue()");  // php/3a5z
  }
  
  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  private void generateImpl(PhpWriter out, boolean isRef)
    throws IOException
  {
    String ref = isRef ? "Ref" : "";
    
    _objExpr.generate(out);
    out.print(".evalMethod" + ref + "(env, \"");
    out.printJavaString(_name);
    out.print("\"");

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

