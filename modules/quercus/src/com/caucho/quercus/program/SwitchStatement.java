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

import java.util.ArrayList;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BreakValue;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a switch statement.
 */
public class SwitchStatement extends Statement {
  private final Expr _value;
  
  private final Expr[][] _cases;
  private final BlockStatement[] _blocks;

  private final Statement _defaultBlock;

  public SwitchStatement(Expr value,
			 ArrayList<Expr[]> caseList,
			 ArrayList<BlockStatement> blockList,
			 Statement defaultBlock)
  {
    _value = value;

    _cases = new Expr[caseList.size()][];
    caseList.toArray(_cases);
    
    _blocks = new BlockStatement[blockList.size()];
    blockList.toArray(_blocks);

    _defaultBlock = defaultBlock;
  }

  /**
   * Executes the 'switch' statement, returning any value.
   */
  public Value execute(Env env)
    throws Throwable
  {
    Value testValue = _value.eval(env);
    
    int len = _cases.length;

    for (int i = 0; i < len; i++) {
      Expr []values = _cases[i];

      for (int j = 0; j < values.length; j++) {
	Value caseValue = values[j].eval(env);

	if (testValue.eq(caseValue)) {
	  Value retValue = _blocks[i].execute(env);

	  if (retValue instanceof BreakValue)
	    return null;
	  else
	    return retValue;
	}
      }
    }

    if (_defaultBlock != null) {
      Value retValue = _defaultBlock.execute(env);
      
      if (retValue instanceof BreakValue)
	return null;
      else
	return retValue;
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
    _value.analyze(info);

    AnalyzeInfo breakInfo = info;
    
    AnalyzeInfo switchInfo = info.createLoop(null, breakInfo);
    
    info.clear();

    boolean isFallThrough = false;

    for (int i = 0; i < _cases.length; i++) {
      Expr []cases = _cases[i];

      if (cases.length > 0)
	cases[0].analyze(switchInfo);

      AnalyzeInfo topCase = switchInfo.copy();
      
      for (int j = 1; j < cases.length; j++) {
	cases[j].analyze(switchInfo);

	topCase.merge(switchInfo);
      }

      // XXX: need to handle internal breaks like the loops

      if (_blocks[i].analyze(topCase))
	isFallThrough = true;

      info.merge(topCase);
    }

    if (_defaultBlock == null)
      isFallThrough = true;
    else if (_defaultBlock.analyze(switchInfo))
      isFallThrough = true;

    info.merge(switchInfo);


    return isFallThrough;
  }

  /**
   * Returns true if control can go past the statement.
   */
  public int fallThrough()
  {
    if (_defaultBlock == null)
      return FALL_THROUGH;

    int fallThrough = _defaultBlock.fallThrough();

    for (int i = 0; i < _blocks.length; i++) {
      fallThrough &= _blocks[i].fallThrough();
    }

    if (fallThrough == BREAK_FALL_THROUGH)
      return 0;
    else
      return fallThrough;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    String breakVar = "switch_" + out.generateId();
    String oldBreakVar = out.setBreakVar(breakVar);

    boolean oldSwitch = out.setSwitch(true);
    
    String testVar = "quercus_test_" + out.generateId();

    out.print("Value " + testVar + " = ");
    _value.generate(out);
    // XXX: should be a createValue like createCopy
    out.println(".toValue();");

    out.println(breakVar + ": {");
    out.pushDepth();

    boolean hasTest = false;

    for (int i = 0; i < _cases.length; i++) {
      Expr []values = _cases[i];

      if (values.length == 0)
	continue;

      if (hasTest)
	out.print("else ");
      hasTest = true;

      out.print("if (");

      for (int j = 0; j < values.length; j++) {
	if (j != 0)
	  out.print(" || ");

	out.print(testVar + ".eq(");
	values[j].generate(out);
	out.print(")");
      }

      out.println(") {");
      out.pushDepth();

      Statement []stmts = _blocks[i].getStatements();

      for (int j = 0; j < stmts.length; j++) {
	stmts[j].generate(out);

	if (stmts[j].fallThrough() != FALL_THROUGH)
	  break;
      }

      out.popDepth();
      out.println("}");
    }

    if (_defaultBlock != null) {
      if (hasTest)
	out.print("else ");
      
      out.println("{");
      out.pushDepth();

      _defaultBlock.generate(out);

      out.popDepth();
      out.println("}");
    }
    
    out.popDepth();
    out.println("}");
    
    out.setSwitch(oldSwitch);
    out.setBreakVar(oldBreakVar);
  }
  
  public String toString()
  {
    return "SwitchStatement[]";
  }
}

