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
import com.caucho.quercus.statement.DoStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;

/**
 * Represents a do ... while statement.
 */
public class ProDoStatement extends DoStatement
  implements CompilingStatement
{
  public ProDoStatement(Location location, Expr test, Statement block,
                        String label)
  {
    super(location, test, block, label);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProDoStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	ExprPro test = (ExprPro) _test;
	StatementGenerator blockGen
	  = ((CompilingStatement) _block).getGenerator();
	
	AnalyzeInfo contInfo = info.copy();

	info.clear();
	AnalyzeInfo breakInfo = info;

	AnalyzeInfo loopInfo = contInfo.createLoop(contInfo, breakInfo);

	blockGen.analyze(loopInfo);

	loopInfo.merge(contInfo);

	if (test != null)
	  test.getGenerator().analyze(loopInfo);

	info.merge(loopInfo);

	// handle loop values

	blockGen.analyze(loopInfo);

	loopInfo.merge(contInfo);

	if (test != null)
	  test.getGenerator().analyze(loopInfo);

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
	ExprPro test = (ExprPro) _test;
	StatementGenerator blockGen
	  = ((CompilingStatement) _block).getGenerator();
	
	//String loopLabel = out.createDoLabel();
	
	//out.pushLoopLabel(loopLabel);
	
	//out.println(loopLabel + ":");
    
    out.println(_label + ":");
	out.println("do {");
	out.pushDepth();
	out.println("env.checkTimeout();");

	blockGen.generate(out);
	out.popDepth();
	out.print("} while (");

	if (_test.isTrue())
	  out.print("BooleanValue.TRUE.toBoolean()");
	else if (_test.isFalse())
	  out.print("BooleanValue.FALSE.toBoolean()");
	else
	  test.getGenerator().generateBoolean(out);

	out.println(");");
	
	//out.popLoopLabel();
      }
    };
}

