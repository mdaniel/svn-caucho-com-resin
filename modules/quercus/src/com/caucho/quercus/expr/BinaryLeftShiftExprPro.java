/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP left shift expression.
 */
public class BinaryLeftShiftExprPro extends BinaryLeftShiftExpr
  implements ExprPro
{
  public BinaryLeftShiftExprPro(Expr left, Expr right)
  {
    super(left, right);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractBinaryGenerateExpr(getLocation()) {
      @Override
      public ExprGenerator getLeft()
      {
	return ((ExprPro) _left).getGenerator();
      }
    
      @Override
      public ExprGenerator getRight()
      {
	return ((ExprPro) _right).getGenerator();
      }

      /**
       * Returns true for a long.
       */
      @Override
      public boolean isLong()
      {
	return true;
      }

      @Override
      public ExprType getType()
      {
	return ExprType.LONG;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generate(PhpWriter out)
	throws IOException
      {
	getLeft().generate(out);
	out.print(".lshift(");
	getRight().generate(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateLong(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateLong(out);
	out.print(" << ");
	getRight().generateLong(out);
	out.print(")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.LeftShiftExpr(");
	getLeft().generateExpr(out);
	out.print(", ");
	getRight().generateExpr(out);
	out.print(")");
      }
    };
}

  
