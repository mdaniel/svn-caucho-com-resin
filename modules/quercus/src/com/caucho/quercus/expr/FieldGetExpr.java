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

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class FieldGetExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(FieldGetExpr.class);

  private final Expr _objExpr;
  private final String _name;

  public FieldGetExpr(Location location, Expr objExpr, String name)
  {
    super(location);
    _objExpr = objExpr;

    _name = name.intern();
    Thread.dumpStack();
  }

  public FieldGetExpr(Expr objExpr, String name)
  {
    _objExpr = objExpr;

    _name = name.intern();

    Thread.dumpStack();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    Value obj = _objExpr.eval(env);

    return obj.getField(env, _name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
  {
    Value value = _objExpr.evalArg(env);

    return value.getFieldArg(env, _name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    // quercus/0d1k
    Value value = _objExpr.evalObject(env);

    return value.getFieldRef(env, _name);
  }

  /**
   * Evaluates the expression as a copyable  value.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    Value obj = _objExpr.eval(env);

    return obj.getField(env, _name).copy();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
  {
    Value obj = _objExpr.evalObject(env);

    obj.putField(env, _name, value);
  }

  /**
   * Evaluates the expression, creating an array if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    Value obj = _objExpr.evalObject(env);

    return obj.getFieldArray(env, _name);
  }

  /**
   * Evaluates the expression, creating an object if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    Value obj = _objExpr.evalObject(env);

    // php/0a6f
    return obj.getFieldObject(env, _name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
  {
    Value obj = _objExpr.eval(env);

    obj.removeField(_name);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
    _objExpr.analyze(info);
  }

  /**
   * Analyze the statement as an assignement
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    _objExpr.analyze(info);

    // php/3a6e
    _objExpr.analyzeSetReference(info);
    _objExpr.analyzeSetModified(info);
  }

  /**
   * Analyze the statement as modified
   */
  public void analyzeSetModified(AnalyzeInfo info)
  {
    _objExpr.analyzeSetModified(info);
  }

  /**
   * Analyze the statement as a reference
   */
  public void analyzeSetReference(AnalyzeInfo info)
  {
    // php/3a6f
    _objExpr.analyzeSetReference(info);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _objExpr.generate(out);
    out.print(".getField(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArg(PhpWriter out)
    throws IOException
  {
    _objExpr.generateArg(out);
    out.print(".getFieldArg(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    _objExpr.generateObject(out);
    out.print(".getFieldRef(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to evaluate the expression, where the result is copied.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".copy()");
  }

  /**
   * Generates code to assign the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    _objExpr.generateObject(out);
    out.print(".putField(env, \"");
    out.printJavaString(_name);
    out.print("\", ");
    value.generateCopy(out);
    out.print(")");
  }

  /**
   * Generates code to assign the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    _objExpr.generateObject(out);
    out.print(".putField(env, \"");
    out.printJavaString(_name);
    out.print("\", ");
    value.generateRef(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateObject(PhpWriter out)
    throws IOException
  {
    _objExpr.generateObject(out);
    out.print(".getFieldObject(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    _objExpr.generateObject(out);
    out.print(".getFieldArray(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to assign the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    _objExpr.generate(out);
    out.print(".removeField(\"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new FieldGetExpr(");

    _objExpr.generateExpr(out);

    out.print(", \"");

    out.printJavaString(_name);

    out.print("\")");
  }

  public String toString()
  {
    return _objExpr + "->" + _name;
  }
}

