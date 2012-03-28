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
 * Represents a logical or expression.
 */
public class BinaryOrExprPro extends BinaryOrExpr
  implements ExprPro
{
  public BinaryOrExprPro(Expr left, Expr right)
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

      @Override
      public ExprType getType()
      {
	return ExprType.BOOLEAN;
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	// quercus/3a0z
	getLeft().analyze(info);

	AnalyzeInfo copy = info.copy();

	getRight().analyze(copy);

	info.merge(copy);

	return ExprType.BOOLEAN;
      }

      /**
       * Generates code to recreate the expression.
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
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      public void generateBoolean(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateBoolean(out);
	out.print(" || ");
	getRight().generateBoolean(out);
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
	out.print("new com.caucho.quercus.expr.OrExpr(");
	getLeft().generateExpr(out);
	out.print(", ");
	getRight().generateExpr(out);
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
	generateBoolean(out);
	out.println(") {}");
      }
    };
}

