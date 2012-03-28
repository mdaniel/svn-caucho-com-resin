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
 * Represents a PHP subtract expression.
 */
public class BinarySubExprPro extends BinarySubExpr
  implements ExprPro
{
  BinarySubExprPro(Expr left, Expr right)
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
	ExprType type = ExprType.LONG;

	type = type.withType(getLeft().analyze(info));
	type = type.withType(getRight().analyze(info));

	return type;
      }
      
      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	if (getLeft().isLong() && getRight().isLong()) {
	  out.print("LongValue.create(");
	  getLeft().generateLong(out);
	  out.print(" - ");
	  getRight().generateLong(out);
	  out.print(")");
	}
	else if (getLeft().isDouble() && getRight().isDouble()) {
	  out.print("new DoubleValue(");
	  getLeft().generateDouble(out);
	  out.print(" - ");
	  getRight().generateDouble(out);
	  out.print(")");
	}
	else if (getLeft().isLong()) {
	  getRight().generate(out);
	  out.print(".sub_rev(");
	  getLeft().generateLong(out);
	  out.print(")");
	}
	else if (getRight().isLong()) {
	  getLeft().generate(out);
	  out.print(".sub(");
	  getRight().generateLong(out);
	  out.print(")");
	}
	else {
	  getLeft().generate(out);
	  out.print(".sub(");
	  getRight().generate(out);
	  out.print(")");
	}
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateLong(out);
	out.print(" - ");
	getRight().generateLong(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateDouble(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateDouble(out);
	out.print(" - ");
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
	out.print("new com.caucho.quercus.expr.SubExpr(");
	getLeft().generateExpr(out);
	out.print(", ");
	getRight().generateExpr(out);
	out.print(")");
      }
    };
}

