/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a conditional expression.
 */
public class ConditionalExprPro extends ConditionalExpr
  implements ExprPro
{
  public ConditionalExprPro(Expr test, Expr trueExpr, Expr falseExpr)
  {
    super(test, trueExpr, falseExpr);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprGenerator getTest()
      {
	return ((ExprPro) _test).getGenerator();
      }
      
      public ExprGenerator getTrue()
      {
	return ((ExprPro) _trueExpr).getGenerator();
      }
      
      public ExprGenerator getFalse()
      {
	return ((ExprPro) _falseExpr).getGenerator();
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	getTest().analyze(info);

	AnalyzeInfo falseExprInfo = info.copy();

	getTrue().analyze(info);

	getFalse().analyze(falseExprInfo);

	info.merge(falseExprInfo);

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
	out.print("(");
	getTest().generateBoolean(out);
	out.print(" ? ");
	getTrue().generate(out);
	out.print(" : ");
	getFalse().generate(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression as a boolean.
       *
       * @param out the writer to the Java source code.
       */
      public void generateBoolean(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getTest().generateBoolean(out);
	out.print(" ? ");
	getTrue().generateBoolean(out);
	out.print(" : ");
	getFalse().generateBoolean(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getTest().generateBoolean(out);
	out.print(" ? ");
	getTrue().generateLong(out);
	out.print(" : ");
	getFalse().generateLong(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression as a double.
       *
       * @param out the writer to the Java source code.
       */
      public void generateDouble(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getTest().generateBoolean(out);
	out.print(" ? ");
	getTrue().generateDouble(out);
	out.print(" : ");
	getFalse().generateDouble(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression as a string.
       *
       * @param out the writer to the Java source code.
       */
      public void generateString(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getTest().generateBoolean(out);
	out.print(" ? ");
	getTrue().generateString(out);
	out.print(" : ");
	getFalse().generateString(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression, copying the result
       *
       * @param out the writer to the Java source code.
       */
      public void generateCopy(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getTest().generateBoolean(out);
	out.print(" ? ");
	getTrue().generateCopy(out); // php/3a5o
	out.print(" : ");
	getFalse().generateCopy(out); // php/3a5o
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
	out.print("if (");
	getTest().generateBoolean(out);
	out.println(") {");
	out.pushDepth();
	getTrue().generateStatement(out);
	out.popDepth();
	out.println("} else {");
	out.pushDepth();
	getFalse().generateStatement(out);
	out.popDepth();
	out.println("}");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.ConditionalExpr(");
	getTest().generateExpr(out);
	out.print(", ");
	getTrue().generateExpr(out);
	out.print(", ");
	getFalse().generateExpr(out);
	out.print(")");
      }
    };
}

