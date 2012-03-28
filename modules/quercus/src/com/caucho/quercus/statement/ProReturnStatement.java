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
import com.caucho.quercus.statement.ReturnStatement;

import java.io.IOException;

/**
 * Represents a return expression statement in a PHP program.
 */
public class ProReturnStatement extends ReturnStatement
  implements CompilingStatement
{
  /**
   * Creates the return statement.
   */
  public ProReturnStatement(Location loc, Expr expr)
  {
    super(loc, expr);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProReturnStatement.this.getLocation();
      }
      
      private ExprGenerator getExpr()
      {
	return ((ExprPro) _expr).getGenerator();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
 	getExpr().analyze(info);

	// getExpr().analyzeSetReference(info); // php/3783, php/3a5d

	return false;
      }

      /**
       * Returns true if control can go past the statement.
       */
      @Override
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
        // the "if" test handles Java's issue with trailing statements
        
        if (getExpr() != null) {
          out.print("return ");
          
          // php/3a5h
          getExpr().generateValue(out);
          
          /**
             Expr copy = getExpr().createCopy();

             if (getExpr() instanceof VarExpr)
             copy.generateReturn(out);
             else
             copy.generate(out);
          */
          
          out.println(";");
        }
        else {
          out.print("return NullValue.NULL;");
        }
      }
    };
  
}

