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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.Location;

/**
 * Represents a PHP list() = each() assignment expression.
 */
public class ListEachExpr extends Expr {
  private final AbstractVarExpr _keyVar;
  private final AbstractVarExpr _valueVar;
  private final Expr _value;

  public ListEachExpr(Location location, Expr []varList, EachExpr each)
    throws IOException
  {
    super(location);
    if (varList.length > 0) {
      // XXX: need test
      _keyVar = (AbstractVarExpr) varList[0];
    }
    else
      _keyVar = null;

    if (varList.length > 1) {
      // XXX: need test
      _valueVar = (AbstractVarExpr) varList[1];
    }
    else
      _valueVar = null;

    _value = each.getExpr();
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
    Value value = _value.eval(env);

    if (! (value instanceof ArrayValue))
      return BooleanValue.FALSE;

    ArrayValue array = (ArrayValue) value;

    if (! array.hasCurrent())
      return BooleanValue.FALSE;

    if (_keyVar != null)
      _keyVar.evalAssign(env, array.key());

    if (_valueVar != null)
      _valueVar.evalAssign(env, array.current());

    return array.each();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    Value value = _value.eval(env);

    if (! (value instanceof ArrayValue))
      return false;

    ArrayValue array = (ArrayValue) value;

    if (! array.hasCurrent())
      return false;

    if (_keyVar != null)
      _keyVar.evalAssign(env, array.key());

    if (_valueVar != null)
      _valueVar.evalAssign(env, array.current());

    array.next();

    return true;
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

    if (_keyVar != null)
      _keyVar.analyzeAssign(info);

    if (_valueVar != null)
      _valueVar.analyzeAssign(info);
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

    out.print("((" + var + " = ");
    _value.generate(out);
    out.print(").hasCurrent() ? ");

    out.print("env.first(BooleanValue.TRUE");

    if (_keyVar != null) {
      out.print(", ");
      _keyVar.generateAssign(out, new EachKeyExpr(getLocation(), var), false);
    }

    if (_valueVar != null) {
      out.print(", ");
      _valueVar.generateAssign(out, new EachValueExpr(getLocation(), var), false);
    }

    out.print(", " + var + ".next()");

    out.print(") : BooleanValue.FALSE)");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateStatement(PhpWriter out)
    throws IOException
  {
    String var = "_quercus_list";

    out.print("if ((" + var + " = ");
    _value.generate(out);
    out.println(").hasCurrent()) {");
    out.pushDepth();

    if (_keyVar != null) {
      _keyVar.generateAssign(out, new EachKeyExpr(getLocation(), var), true);
      out.println(";");
    }

    if (_valueVar != null) {
      _valueVar.generateAssign(out, new EachValueExpr(getLocation(), var), true);
      out.println(";");
    }

    out.println(var + ".next();");

    out.popDepth();
    out.println("}");
  }

  static class EachKeyExpr extends Expr {
    private String _var;

    EachKeyExpr(Location location, String var)
    {
      super(location);
      _var = var;
    }

    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }

    public void generate(PhpWriter out)
      throws IOException
    {
      out.print(_var + ".key()");
    }
  }

  static class EachValueExpr extends Expr {
    private String _var;

    EachValueExpr(Location location, String var)
    {
      super(location);
      _var = var;
    }

    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }

    public void generate(PhpWriter out)
      throws IOException
    {
      out.print(_var + ".current()");
    }
  }
}

