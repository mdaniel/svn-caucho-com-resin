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
import com.caucho.quercus.statement.EchoStatement;

import java.io.IOException;

/**
 * Represents an echo statement in a PHP program.
 */
public class ProEchoStatement extends EchoStatement
  implements CompilingStatement
{
  
  /**
   * Creates the echo statement.
   */
  public ProEchoStatement(Location location, Expr expr)
  {
    super(location, expr);

    if (! (expr instanceof ExprPro))
      throw new IllegalArgumentException(expr + " (" + expr.getClass().getName() + ") needs to be a CompilingExpr");
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  
  //
  // java generation code
  //

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProEchoStatement.this.getLocation();
      }
      
    /**
     * Analyze the statement
     */
    public boolean analyze(AnalyzeInfo info)
    {
      info.getFunction().setOutUsed();

      ExprPro compilingExpr = (ExprPro) _expr;

      compilingExpr.getGenerator().analyze(info);

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
      ExprPro compilerExpr = (ExprPro) _expr;

      compilerExpr.getGenerator().generatePrint(out);
    
      out.println(";");
    }

    /**
     * Generates static/initialization code code for the statement.
     *
     * @param out the writer to the generated Java source.
     */
    /*
      public void generateCoda(PhpWriter out)
      throws IOException
      {
      try {
      out.print("static com.caucho.quercus.expr.Expr " + _genId + " = ");
      _expr.generateExpr(out);
      out.println(";");
      }
      catch (Throwable t) {
      rethrow(t, IOException.class);
      }
      }
    */
    };
}

