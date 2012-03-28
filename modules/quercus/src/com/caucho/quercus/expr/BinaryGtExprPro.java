/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP comparison expression.
 */
public class BinaryGtExprPro extends BinaryGtExpr
  implements ExprPro
{
  public BinaryGtExprPro(Expr left, Expr right)
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
       * Returns true for a boolean.
       */
      public boolean isBoolean()
      {
	return true;
      }

      //
      // Generates the java code
      //

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	out.print("env.toValue(");
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
        if (getLeft().isLong() && getRight().isLong()) {
          out.print("(");
          getLeft().generateLong(out);
          out.print(" > ");
          getRight().generateLong(out);
          out.print(")");
        }
        else if ((getLeft().isNumber() || getRight().isNumber())
		 && ! getLeft().isBoolean() && ! getRight().isBoolean()) {
          out.print("(");
          getLeft().generateDouble(out);
          out.print(" > ");
          getRight().generateDouble(out);
          out.print(")");
        }
        else {
          getLeft().generate(out);
          out.print(".gt(");
          getRight().generate(out);
          out.print(")");
        }
      }
    };
}

