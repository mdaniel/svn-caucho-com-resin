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

import java.lang.reflect.Method;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.StaticFunction;
import com.caucho.quercus.module.PhpModule;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.Statement;
import com.caucho.quercus.program.ExprStatement;
import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.program.InterpretedClassDef;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(ThisFieldExpr.class);

  private final InterpretedClassDef _quercusClass;
  
  private final StringValue _name;

  public ThisFieldExpr(InterpretedClassDef quercusClass, String name)
  {
    _quercusClass = phpClass;
    
    _name = new StringValue(name);
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
    Value obj = env.getThis();

    return obj.get(_name);
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
    Value obj = env.getThis();

    return obj.getRef(_name);
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
    Value obj = env.getThis();

    return obj.getArg(_name);
  }

  /**
   * Evaluates the expression, creating an object if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArgObject(Env env)
    throws Throwable
  {
    Value obj = env.getThis();

    return obj.getArgObject(env, _name);
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
    Value obj = env.getThis();

    obj.put(_name, value);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
    throws Throwable
  {
    Value obj = env.getThis();

    return obj.getArray(_name);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
    throws Throwable
  {
    Value obj = env.getThis();

    // quercus/0d8f

    return obj.getObject(env, _name);
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
    Value obj = env.getThis();

    obj.remove(_name);
  }

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    /*
    if (_quercusClass.get(_name) != null) {
      // quercus/3d80
      
      String var = "quercus_this._php_" + _name;
    
      out.print("(" + var + " != null ? " + var + " : NullValue.NULL)");
    }
    else {
      // quercus/3d8o
      */
      
    out.print("quercus_this.get(");
    out.print(_name);
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
    /*
    if (_quercusClass.get(_name) != null) {
      // quercus/3d85
      
      String var = "quercus_this._php_" + _name;
    
      out.print("(" + var + " = (" + var + " != null ? " + var + ".toRefVar() : new Var()))");
    }
    else {
      // quercus/3d8g
      */
      
    out.print("quercus_this.getRef(");
    out.print(_name);
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
    /*
    if (_quercusClass.get(_name) != null) {
      // quercus/3d8p

      generateRef(out);
    }
    else {
      // quercus/3d8r
      */
      
    out.print("quercus_this.getArg(");
    out.print(_name);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArgObject(PhpWriter out)
    throws IOException
  {
    out.print("quercus_this.getArgObject(env, ");
    out.print(_name);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression, creating an array for the
   * field value if unset.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    /*
    if (_quercusClass.get(_name) != null) {
      // quercus/3d8c
      
      String var = "quercus_this._php_" + _name;
    
      out.print("(" + var + " = (" + var + " != null ? " + var + ".getArray() : new ArrayValue()))");
    }
    else {
      // quercus/3d8i
      */
      
    out.print("quercus_this.getArray(");
    out.print(_name);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression, creating an object for the
   * field value if unset.
   *
   * @param out the writer to the Java source code.
   */
  public void generateObject(PhpWriter out)
    throws IOException
  {
    /*
    if (_quercusClass.get(_name) != null) {
      // quercus/3d8f
      
      String var = "quercus_this._php_" + _name;
    
      out.print("(" + var + " = (" + var + " != null ? " + var + ".getObject(env) : env.createObject()))");
    }
    else {
      // quercus/3d8h
      */
      
    out.print("quercus_this.getObject(env, ");
    out.print(_name);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    /*
    if (_quercusClass.get(_name) != null) {
      // quercus/3d81
      
      String var = "_quercus_" + _name;

      if (! isTop)
	out.print("(");
    
      out.print("quercus_this." + var + " = ");
      value.generate(out);

      if (! isTop)
	out.print(")");
    }
    else {
      // quercus/3d90
      */
      
    out.print("quercus_this.put(");
    out.print(_name);
    out.print(", ");
    value.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    out.print("quercus_this.put(");
    out.print(_name);
    out.print(", ");
    value.generateRef(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    /*
    // quercus/3d91
    
    if (_quercusClass.get(_name) != null) {
      String var = "quercus_this._php_" + _name;
    
      out.print(var + " = null");
    }
    else {
    */
    
    out.print("quercus_this.remove(");
    out.print(_name);
    out.print(")");
  }
  
  public String toString()
  {
    return "$this->" + _name;
  }
}

