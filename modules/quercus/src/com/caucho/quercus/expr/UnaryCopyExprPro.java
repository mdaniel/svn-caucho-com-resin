/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP variable assignment
 */
public class UnaryCopyExprPro extends UnaryCopyExpr
  implements ExprPro
{
  public UnaryCopyExprPro(Expr expr)
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
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	getExpr().generate(out);
	out.print(".copy()");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateReturn(PhpWriter out)
	throws IOException
      {
	getExpr().generate(out);
	out.print(".copyReturn()");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.CopyExpr(");
	getExpr().generateExpr(out);
	out.print(")");
      }
    };
}

