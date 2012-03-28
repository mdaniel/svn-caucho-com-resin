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
import com.caucho.quercus.statement.ThrowStatement;

import java.io.IOException;

/**
 * Represents a throw expression statement in a Quercus program.
 */
public class ProThrowStatement extends ThrowStatement
  implements CompilingStatement
{
  /**
   * Creates the throw statement.
   */
  public ProThrowStatement(Location location, Expr expr)
  {
    super(location, expr);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProThrowStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	ExprPro expr = (ExprPro) _expr;
	
	expr.getGenerator().analyze(info);

	return false;
      }

      /**
       * Returns true if control can go past the statement.
       */
      public int fallThrough()
      {
	return RETURN;
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
	throws IOException
      {
	// php/3g00
	// out.print("if (true) throw ");
    
	out.print("throw ");

	// php/3a5h
	ExprPro expr = (ExprPro) _expr;
	expr.getGenerator().generate(out);

	out.print(".toException(env, \"");
	out.printJavaString(getLocation().getFileName());
	out.println("\", " + getLocation().getLineNumber() + ");");
      }
    };
}
