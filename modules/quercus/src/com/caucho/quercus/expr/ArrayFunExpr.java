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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.ConstArrayValue;
import com.caucho.quercus.env.LongValue;

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents the array function
 */
public class ArrayFunExpr extends Expr {
  private final Expr []_keys;
  private final Expr []_values;

  public ArrayFunExpr(ArrayList<Expr> keyList, ArrayList<Expr> valueList)
  {
    _keys = new Expr[keyList.size()];
    keyList.toArray(_keys);
    
    _values = new Expr[valueList.size()];
    valueList.toArray(_values);
  }

  public ArrayFunExpr(Expr []keys, Expr []values)
  {
    _keys = keys;
    _values = values;
  }

  /**
   * Returns true for a constant array.
   */
  public boolean isConstant()
  {
    for (int i = 0; i < _keys.length; i++) {
      if (_keys[i] != null && ! _keys[i].isLiteral())
	return false;
    }
    
    for (int i = 0; i < _values.length; i++) {
      if (_values[i] != null && ! _values[i].isLiteral())
	return false;
    }

    return true;
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
    ArrayValue array = new ArrayValueImpl();

    for (int i = 0; i < _values.length; i++) {
      Expr keyExpr = _keys[i];
      Value value = _values[i].evalArg(env);

      value = value.toArgValue();

      if (keyExpr != null) {
	Value key = keyExpr.evalArg(env).toArgValue();
	
	array.put(key, value);
      }
      else
	array.put(value);
    }

    return array;
  }

  //
  // Java code generation
  //

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
    for (int i = 0; i < _values.length; i++) {
      if (_keys[i] != null)
        _keys[i].analyze(info);
      
      _values[i].analyze(info);
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
    if (isConstant() && _keys.length > 3) {
      ConstArrayValue array = new ConstArrayValue();

      try {
	for (int i = 0; i < _keys.length; i++) {
	  if (_keys[i] != null)
	    array.put(_keys[i].eval(null), _values[i].eval(null));
	  else
	    array.put(_values[i].eval(null));
	}
      } catch (RuntimeException e) {
	throw e;
      } catch (Throwable e) {
	throw new RuntimeException(e);
      }

      out.print(array);
      out.print(".copy()");
    }
    
    else if (_keys.length < 16) {
      out.print("new ArrayValueImpl()");

      for (int i = 0; i < _keys.length; i++) {
	out.print(".");
	out.print("append(");
	if (_keys[i] != null) {
	  _keys[i].generateCopy(out);
	  out.print(", ");
	}
	_values[i].generateCopy(out);
	out.print(")");
      }
    }
    else {
      out.print("new ArrayValueImpl(");
      out.print("new Value[] {");
      
      for (int i = 0; i < _keys.length; i++) {
	if (i != 0)
	  out.print(", ");
	    
	if (_keys[i] != null)
	  _keys[i].generateCopy(out);
	else
	  out.print("null");
      }
      
      out.print("}, new Value[] {");
	
      for (int i = 0; i < _values.length; i++) {
	if (i != 0)
	  out.print(", ");
	    
	_values[i].generateCopy(out);
      }

      out.print("})");
    }
  }
  
  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    // quercus/3724
    out.print("new com.caucho.quercus.expr.ArrayFunExpr(");
    
    out.print("new Expr[] {");
      
    for (int i = 0; i < _keys.length; i++) {
      if (i != 0)
	out.print(", ");
	    
      if (_keys[i] != null)
	_keys[i].generateExpr(out);
      else
	out.print("null");
    }
      
    out.print("}, new Expr[] {");
	
    for (int i = 0; i < _values.length; i++) {
      if (i != 0)
	out.print(", ");
	    
      _values[i].generateExpr(out);
    }

    out.print("})");
  }
  
  public String toString()
  {
    return "array()";
  }
}

