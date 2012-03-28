/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Converts to a long
 */
public class ToLongExprPro extends ToLongExpr
  implements ExprPro
{
  public ToLongExprPro(Expr expr)
  {
    super(expr);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractUnaryExprGenerator(getLocation()) {
      public ExprGenerator getExpr()
      {
	return ((ExprPro) _expr).getGenerator();
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        getExpr().analyze(info);
        
        return ExprType.LONG;
      }
      
      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	getExpr().generate(out);
	out.print(".toLongValue()");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
	throws IOException
      {
	getExpr().generateLong(out);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.ToLongExpr(");
	getExpr().generateExpr(out);
	out.print(")");
      }
    };
}

