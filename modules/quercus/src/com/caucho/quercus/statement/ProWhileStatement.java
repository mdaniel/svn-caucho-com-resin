/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.Statement;
import com.caucho.quercus.statement.WhileStatement;

import java.io.IOException;

/**
 * Represents a while statement.
 */
public class ProWhileStatement extends WhileStatement
  implements CompilingStatement
{
  public ProWhileStatement(Location location,
                           Expr test,
                           Statement block,
                           String label)
  {
    super(location, test, block, label);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private final StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProWhileStatement.this.getLocation();
      }
      
      private ExprGenerator getTest()
      {
	return ((ExprPro) _test).getGenerator();
      }
      
      private StatementGenerator getBlock()
      {
	return ((CompilingStatement) _block).getGenerator();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	if (getTest() != null)
	  getTest().analyze(info);

	AnalyzeInfo contInfo = info.copy();
	AnalyzeInfo breakInfo = info;

	AnalyzeInfo loopInfo = info.createLoop(contInfo, breakInfo);

	getBlock().analyze(loopInfo);

	if (getTest() != null)
	  getTest().analyze(loopInfo);

	loopInfo.merge(contInfo);

	// handle loop values

	getBlock().analyze(loopInfo);

	loopInfo.merge(contInfo);

	if (getTest() != null)
	  getTest().analyze(loopInfo);

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
        out.println(_label + ":");
	out.print("while (");
	
	if (_test.isTrue())
	  out.print("BooleanValue.TRUE.toBoolean()");
	else if (_test.isFalse())
	  out.print("BooleanValue.FALSE.toBoolean()");
	else
	  getTest().generateBoolean(out);
	
	out.println(") {");
	
	out.pushDepth();
	out.println("env.checkTimeout();");

	getBlock().generate(out);
	out.popDepth();
	out.println("}");
      }
    };

}

