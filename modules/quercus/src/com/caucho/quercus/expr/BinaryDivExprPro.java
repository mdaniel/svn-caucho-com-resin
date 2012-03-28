/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.gen.AnalyzeInfo;

import java.io.IOException;

/**
 * Represents a PHP division expression.
 */
public class BinaryDivExprPro extends BinaryDivExpr
  implements ExprPro
{
  BinaryDivExprPro(Expr left, Expr right)
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
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	getLeft().analyze(info);
	getRight().analyze(info);

	return getType();
      }

      /**
       * Returns true for a double.
       */
      @Override
      public ExprType getType()
      {
	if (getLeft().isDouble() && ! getLeft().isLong()
	    || getRight().isDouble() && ! getRight().isLong())
	  return ExprType.DOUBLE;
	else
	  return ExprType.VALUE;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        if (isDouble()) {
	  out.print("new DoubleValue(");
	  getLeft().generateDouble(out);
	  out.print(" / ");
	  getRight().generateDouble(out);
	  out.print(")");
	}
        else if (getRight().isLong()) {
          getLeft().generate(out);
          out.print(".div(");
          getRight().generateLong(out);
          out.print(")");
        }
        else {
          getLeft().generate(out);
          out.print(".div(");
          getRight().generate(out);
          out.print(")");
        }
      }

      /**
       * Generates code to evaluate the expression directly.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
	throws IOException
      {
	out.print("(long) ");
	generateDouble(out);
      }

      /**
       * Generates code to evaluate the expression directly.
       *
       * @param out the writer to the Java source code.
       */
      public void generateDouble(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateDouble(out);
	out.print(" / ");
	getRight().generateDouble(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression directly.
       *
       * @param out the writer to the Java source code.
       */
      public void generatePrint(PhpWriter out)
	throws IOException
      {
	out.print("env.print(");
	getLeft().generateDouble(out);
	out.print(" / ");
	getRight().generateDouble(out);
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
	out.print("new com.caucho.quercus.expr.DivExpr(");
	getLeft().generateExpr(out);
	out.print(", ");
	getRight().generateExpr(out);
	out.print(")");
      }
    };
}

  
