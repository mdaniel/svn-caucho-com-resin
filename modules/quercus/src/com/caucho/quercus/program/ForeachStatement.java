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

package com.caucho.quercus.program;

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ContinueValue;
import com.caucho.quercus.env.BreakValue;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a foreach statement.
 */
public class ForeachStatement extends Statement {
  private final Expr _objExpr;
  
  private final VarExpr _key;
  private final String _keyName;
  
  private final VarExpr _value;
  private final String _valueName;
  private final boolean _isRef;

  private final Statement _block;

  public ForeachStatement(Expr objExpr,
			  VarExpr key, VarExpr value, boolean isRef,
			  Statement block)
  {
    _objExpr = objExpr;
    
    _key = key;
    if (key != null)
      _keyName = key.getName();
    else
      _keyName = null;

    _value = value;
    _valueName = value.getName();
    _isRef = isRef;
    
    _block = block;
  }
  
  public Value execute(Env env)
    throws Throwable
  {
    Value origObj = _objExpr.eval(env);
    Value obj = origObj.copy();

    if (_key == null) {
      for (Value value : obj.getValueArray(env)) {
	env.setValue(_valueName, value);

	Value result = _block.execute(env);

	if (result == null || result instanceof ContinueValue) {
	}
	else if (result instanceof BreakValue)
	  return null;
	else
	  return result;
      }

      return null;
    }
    else {
      for (Value key : obj.getKeyArray()) {
	if (_keyName != null)
	  env.setValue(_keyName, key);

	if (_isRef) {
	  Value value = origObj.getRef(key);

	  env.setValue(_valueName, value);
	}
	else {
	  Value value = obj.get(key).toValue();
      
	  env.setValue(_valueName, value);
	}

	Value result = _block.execute(env);

	if (result == null || result instanceof ContinueValue) {
	}
	else if (result instanceof BreakValue)
	  return null;
	else
	  return result;
      }
    }

    return null;
  }

  //
  // Java code generation
  //

  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    _objExpr.analyze(info);

    AnalyzeInfo contInfo = info.copy();
    AnalyzeInfo breakInfo = info;

    AnalyzeInfo loopInfo = info.createLoop(contInfo, breakInfo);

    if (_key != null) {
      _key.analyzeAssign(loopInfo);

      // XXX: not necessarily true
      _key.analyzeSetReference(loopInfo);
    }

    if (_value != null) {
      _value.analyzeAssign(loopInfo);

      // XXX: not necessarily true
      _value.analyzeSetReference(loopInfo);
    }

    _block.analyze(loopInfo);

    loopInfo.merge(contInfo);

    info.merge(loopInfo);

    if (_key != null)
      _key.analyzeAssign(loopInfo);

    if (_value != null)
      _value.analyzeAssign(loopInfo);

    _block.analyze(loopInfo);

    info.merge(loopInfo);

    return true;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  protected void generateImpl(PhpWriter out)
    throws IOException
  {
    int id = out.generateId();
    
    String objVar = "quercus_obj_" + id;
    String keysVar = "quercus_keys_" + id;
    String valuesVar = "quercus_values_" + id;
    String indexVar = "quercus_index_" + id;

    out.print("Value " + objVar + " = ");
    _objExpr.generate(out);
    out.println(";");

    if (_key != null || _isRef)
      out.println("Value []" + keysVar + " = " + objVar + ".getKeyArray();");

    if (! _isRef)
      out.println("Value []" + valuesVar + " = " + objVar + ".getValueArray(env);");

    if (_key != null || _isRef) {
      out.println("for (int " + indexVar + " = 0; " +
		  indexVar + " < " + keysVar + ".length; " +
		  indexVar + "++) {");
    }
    else {
      out.println("for (int " + indexVar + " = 0; " +
		  indexVar + " < " + valuesVar + ".length; " +
		  indexVar + "++) {");
    }

    out.pushDepth();

    String keyVar = keysVar + "[" + indexVar + "]";

    if (_key != null) {
      _key.generate(out);
      out.println(".set(" + keyVar + ");");
    }
    
    if (_isRef) {
      String valueVar = objVar + ".getRef(" + keyVar + ")";
      
      _value.generateAssignRef(out, valueVar);
      out.println(";");
    }
    else {
      String valueVar = valuesVar + "[" + indexVar + "]";
      
      _value.generate(out);
      out.println(".set(" + valueVar + ");");
    }

    _block.generate(out);
    
    out.popDepth();
    out.println("}");
  }
  
  public String toString()
  {
    return "ForeachStatement[]";
  }
}

