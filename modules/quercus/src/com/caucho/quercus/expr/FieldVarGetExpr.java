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

import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class FieldVarGetExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(FieldVarGetExpr.class);

  private final Expr _objExpr;
  private final Expr _nameExpr;

  public FieldVarGetExpr(Expr objExpr, Expr nameExpr)
  {
    _objExpr = objExpr;
    
    _nameExpr = nameExpr;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
    throws Throwable
  {
    Value value = _objExpr.evalArg(env);

    return value.getFieldArg(env, _nameExpr.evalString(env));
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
    // quercus/0d1k
    Value value = _objExpr.evalObject(env);

    return value.getFieldRef(env, _nameExpr.evalString(env));
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
    Value obj = _objExpr.eval(env);

    return obj.getField(_nameExpr.evalString(env));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
    throws Throwable
  {
    Value obj = _objExpr.evalObject(env);

    obj.putField(env, _nameExpr.evalString(env), value);
  }

  /**
   * Evaluates the expression, creating an array if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
    throws Throwable
  {
    Value obj = _objExpr.evalObject(env);

    return obj.getFieldArray(env, _nameExpr.evalString(env));
  }

  /**
   * Evaluates the expression, creating an object if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
    throws Throwable
  {
    Value obj = _objExpr.evalObject(env);

    return obj.getFieldObject(env, _nameExpr.evalString(env));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
    throws Throwable
  {
    Value obj = _objExpr.eval(env);

    obj.remove(_nameExpr.eval(env));
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
    _nameExpr.analyze(info);
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
    out.print(".getField(");
    _nameExpr.generateString(out);
    out.print(")");
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
    out.print(".getFieldArg(");
    _nameExpr.generateString(out);
    out.print(")");
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
    out.print(".getFieldRef(");
    _nameExpr.generateString(out);
    out.print(")");
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
    out.print(".putField(env, ");
    _nameExpr.generateString(out);
    out.print(", ");
    value.generateCopy(out); // php/3a85
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
    out.print(".putField(env, ");
    _nameExpr.generateString(out);
    out.print(", ");
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
    out.print(".getFieldObject(env, ");
    _nameExpr.generateString(out);
    out.print(")");
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
    out.print(".getFieldArray(");
    _nameExpr.generateString(out);
    out.print(")");
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
    out.print(".removeField(");
    _nameExpr.generateString(out);
    out.print(")");
  }
  
  public String toString()
  {
    return _objExpr + "->{" + _nameExpr + "}";
  }
}

