/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a comma expression.
 */
public class BinaryCommaExprPro extends BinaryCommaExpr
  implements ExprPro
{
  public BinaryCommaExprPro(Expr left, Expr right)
  {
    super(left, right);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractBinaryGenerateExpr(getLocation()) {
      public ExprGenerator getLeft()
      {
	return ((ExprPro) _left).getGenerator();
      }
      
      public ExprGenerator getRight()
      {
	return ((ExprPro) _right).getGenerator();
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	out.print("env.comma(");
	getLeft().generateType(out);
	out.print(", ");
	getRight().generateType(out);
	out.print(")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateStatement(PhpWriter out)
	throws IOException
      {
	getLeft().generateTop(out);
	getRight().generateTop(out);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.CommaExpr(");
	getLeft().generateExpr(out);
	out.print(", ");
	getRight().generateExpr(out);
	out.print(")");
      }
    };
}

