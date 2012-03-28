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
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.ExprStatement;

import java.io.IOException;

/**
 * Represents an expression statement in a PHP program.
 */
public class ProExprStatement extends ExprStatement
  implements CompilingStatement
{
  /**
   * Creates the expression statement.
   */
  public ProExprStatement(Location location, Expr expr)
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
        return ProExprStatement.this.getLocation();
      }
      
      private ExprGenerator getExpr()
      {
	return ((ExprPro) ProExprStatement.this.getExpr()).getGenerator();
      }

      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
        getExpr().analyzeTop(info);

        return true;
      }

      /**
       * Returns the variables state.
       *
       * @param var the variables to test
       */
      /*
      public VarState getVarState(VarExpr var,
				  Statement stmtOwner,
				  VarExpr exprOwner)
      {
	    return getExpr().getVarState(var, exprOwner);
      }
      */

      /**
       * Returns true if the variable is ever assigned.
       *
       * @param var the variable to test
       */
      public boolean isVarAssigned(VarExpr var)
      {
        return getExpr().isVarAssigned(var);
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
        throws IOException
      {
        getExpr().generateStatement(out);
      }
    };
}

