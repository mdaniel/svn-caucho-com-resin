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
import com.caucho.quercus.statement.ReturnRefStatement;

import java.io.IOException;

/**
 * Represents a return expression statement in a PHP program.
 */
public class ProReturnRefStatement extends ReturnRefStatement
  implements CompilingStatement
{
  /**
   * Creates the echo statement.
   */
  public ProReturnRefStatement(Location location, Expr expr)
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
        return ProReturnRefStatement.this.getLocation();
      }

      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
        ExprPro expr = (ExprPro) _expr;

        // php/378d
        expr.getGenerator().analyze(info);
        expr.getGenerator().analyzeSetReference(info);

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
        ExprPro expr = (ExprPro) _expr;

        // the "if" test handles Java's issue with trailing statements

        if (expr != null) {
          // php/3c3d
          out.print("return ");
          expr.getGenerator().generateVar(out);
          out.println(";");
        }
        else
          out.print("return new Var();");
      }
    };
}

