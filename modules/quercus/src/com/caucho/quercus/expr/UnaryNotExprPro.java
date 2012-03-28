/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP boolean negation
 */
public class UnaryNotExprPro extends UnaryNotExpr
  implements ExprPro
{
  public UnaryNotExprPro(Expr expr)
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
       * Return true as a boolean.
       */
      public boolean isBoolean()
      {
	return true;
      }

      @Override
      public ExprType getType()
      {
	return ExprType.BOOLEAN;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	out.print("BooleanValue.create(");
	generateBoolean(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateBoolean(PhpWriter out)
        throws IOException
      {
        out.print("! (");
        getExpr().generateBoolean(out);
        out.print(")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.NotExpr(");
	getExpr().generateExpr(out);
	out.print(")");
      }
    };
}

