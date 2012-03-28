/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.VarVarExpr;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.VarGlobalStatement;

import java.io.IOException;

/**
 * Represents a global statement in a PHP program.
 */
public class ProVarGlobalStatement extends VarGlobalStatement
  implements CompilingStatement
{
  /**
   * Creates the echo statement.
   */
  public ProVarGlobalStatement(Location location, VarVarExpr var)
  {
    super(location, var);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      public Location getLocation()
      {
	return ProVarGlobalStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	ExprPro varExpr = (ExprPro) _varExpr;
	
	varExpr.getGenerator().analyze(info);

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
	ExprPro varExpr = (ExprPro) _varExpr;
	
	out.print("env.setRef(");
	varExpr.getGenerator().generateStringValue(out);
	out.print(", ");
	out.print("env.getGlobalVar(");
	varExpr.getGenerator().generateStringValue(out);
	out.println("));");
      }
    };
}

