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
import com.caucho.quercus.program.InterpretedClassDef;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(ThisFieldExpr.class);

  private final InterpretedClassDef _quercusClass;
  
  private final String _name;

  public ThisFieldExpr(Location location, InterpretedClassDef quercusClass, String name)
  {
    super(location);
    _quercusClass = quercusClass;
    
    _name = name.intern();
  }

  public ThisFieldExpr(InterpretedClassDef quercusClass, String name)
  {
    _quercusClass = quercusClass;
    
    _name = name.intern();
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

    return obj.getField(_name);
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
    Value obj = env.getThis();

    return obj.getField(_name).copy();
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

    return obj.getFieldRef(env, _name);
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

    return obj.getFieldArg(env, _name);
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

    obj.putField(env, _name, value);
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

    return obj.getFieldArray(env, _name);
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
    throws Throwable
  {
    Value obj = env.getThis();

    obj.removeField(_name);
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
    if (false && _quercusClass.isDeclaredField(_name)) {
      out.print("q_this._fields[_f_" + _name + "].toValue()");
    }
    else {
      out.print("q_this.getField(\"");
      out.printJavaString(_name);
      out.print("\")");
    }
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    out.print("q_this.getFieldRef(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to evaluate the expression, as a copy.
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
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArg(PhpWriter out)
    throws IOException
  {
    out.print("q_this.getFieldArg(env, \"");
    out.printJavaString(_name);
    out.print("\")");
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
    out.print("q_this.getFieldArray(env, \"");
    out.printJavaString(_name);
    out.print("\")");
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
    out.print("q_this.getFieldObject(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    if (false && _quercusClass.isDeclaredField(_name)) {
      if (! isTop)
	out.print("(");
      
      out.print("q_this._fields[_f_" + _name + "] = ");
      out.print("q_this._fields[_f_" + _name + "].set(");

      value.generateCopy(out);

      out.print(")");
      
      if (! isTop)
	out.print(")");
    }
    else {
      out.print("q_this.putField(env, \"");
      out.printJavaString(_name);
      out.print("\", ");
      value.generateCopy(out);
      out.print(")");
    }
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    if (false && _quercusClass.isDeclaredField(_name)) {
      // php/39f5
      if (! isTop)
	out.print("(");
      
      out.print("q_this._fields[_f_" + _name + "] = ");

      value.generateRef(out);
      
      if (! isTop)
	out.print(")");
    }
    else {
      out.print("q_this.putField(env, \"");
      out.printJavaString(_name);
      out.print("\", ");
      value.generateRef(out);
      out.print(")");
    }
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    out.print("q_this.removeField(\"");
    out.printJavaString(_name);
    out.print("\")");
  }
  
  public String toString()
  {
    return "$this->" + _name;
  }
}

