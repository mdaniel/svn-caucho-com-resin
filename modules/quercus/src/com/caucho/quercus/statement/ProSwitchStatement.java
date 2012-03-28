/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.BlockStatement;
import com.caucho.quercus.statement.Statement;
import com.caucho.quercus.statement.SwitchStatement;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a switch statement.
 */
public class ProSwitchStatement extends SwitchStatement
  implements CompilingStatement
{
  public ProSwitchStatement(Location location,
                            Expr value,
                            ArrayList<Expr[]> caseList,
                            ArrayList<BlockStatement> blockList,
                            Statement defaultBlock,
                            String label)
  {
    super(location, value, caseList, blockList, defaultBlock, label);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProSwitchStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	ExprPro value = (ExprPro) _value;
	
	value.getGenerator().analyze(info);

	// XXX: right for continue?
	// (continues inside switches are equivalent to breaks)
	AnalyzeInfo contInfo = info.copy();
	AnalyzeInfo breakInfo = info;

	AnalyzeInfo switchInfo = info.createLoop(contInfo, breakInfo);

	info.clear();

	boolean isFallThrough = false;

	for (int i = 0; i < _cases.length; i++) {
	  Expr []cases = _cases[i];

	  if (cases.length > 0) {
	    ExprPro caseExpr = (ExprPro) cases[0];
	    
	    caseExpr.getGenerator().analyze(switchInfo);
	  }

	  AnalyzeInfo topCase = switchInfo.copy();

	  for (int j = 1; j < cases.length; j++) {
	    ExprPro caseExpr = (ExprPro) cases[j];
	    
	    caseExpr.getGenerator().analyze(switchInfo);

	    topCase.merge(switchInfo);
	  }

	  // XXX: need to handle internal breaks like the loops

	  CompilingStatement block = (CompilingStatement) _blocks[i];

	  if (block.getGenerator().analyze(topCase))
	    isFallThrough = true;

	  info.merge(topCase);
	}

	CompilingStatement defaultBlock = (CompilingStatement) _defaultBlock;
	if (defaultBlock == null)
	  isFallThrough = true;
	else if (defaultBlock.getGenerator().analyze(switchInfo))
	  isFallThrough = true;

	info.merge(switchInfo);

	return true;
      }

      /**
       * Returns true if control can go past the statement.
       */
      @Override
      public int fallThrough()
      {
	return FALL_THROUGH;
	/*
	// php/367t, php/367u

	if (_defaultBlock == null)
	  return FALL_THROUGH;

	CompilingStatement defaultBlock = (CompilingStatement) _defaultBlock;
	int fallThrough = defaultBlock.getGenerator().fallThrough();

	for (int i = 0; i < _blocks.length; i++) {
	  CompilingStatement block = (CompilingStatement) _blocks[i];
	  
	  fallThrough &= block.getGenerator().fallThrough();
	}

	if (fallThrough == BREAK_FALL_THROUGH)
	  return FALL_THROUGH;
	else
	  return fallThrough;
	*/
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
	throws IOException
      {
	String testVar = "quercus_test_" + out.generateId();
	
	ExprPro value = (ExprPro) _value;

	out.print("Value " + testVar + " = ");
	value.getGenerator().generate(out);
	// XXX: should be a createValue like createCopy
	out.println(".toValue();");

	out.println(_label + ": {");
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
	    ExprPro caseExpr = (ExprPro) values[j];
	    
	    if (j != 0)
	      out.print(" || ");

	    out.print(testVar + ".eq(");
	    caseExpr.getGenerator().generate(out);
	    out.print(")");
	  }

	  out.println(") {");
	  out.pushDepth();

	  Statement []stmts = _blocks[i].getStatements();

	  for (int j = 0; j < stmts.length; j++) {
	    CompilingStatement stmt = (CompilingStatement) stmts[j];
	    
	    stmt.getGenerator().generate(out);

	    if (stmt.getGenerator().fallThrough() != FALL_THROUGH)
	      break;
	  }

	  out.popDepth();
	  out.println("}");
	}

	if (_defaultBlock != null) {
	  if (hasTest)
	    out.print("else if (true) ");
	  else
	    out.print("if (true) "); // php/367y
	  
	  out.println("{");
	  out.pushDepth();

	  CompilingStatement block = (CompilingStatement) _defaultBlock;
	  block.getGenerator().generate(out);

	  out.popDepth();
	  out.println("}");
	}

	out.popDepth();
	out.println("}");
      }
    };
}

