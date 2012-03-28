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
 * Represents a PHP long literal expression.
 */
public class LiteralLongExprPro extends LiteralLongExpr
  implements ExprPro
{
  public LiteralLongExprPro(long value)
  {
    super(value);
  }

  
  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Returns true for a literal expression.
       */
      public boolean isLiteral()
      {
	return true;
      }

      /**
       * Returns true for a long value.
       */
      public boolean isLong()
      {
	return true;
      }

      /**
       * Returns the static, analyzed type
       */
      public ExprType getType()
      {
	return ExprType.LONG;
      }

      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	return ExprType.LONG;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        out.print(out.addValue(_objValue));
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.LongLiteralExpr(");
	out.print(_value);
	out.print("L)");
      }

      public void generateStatement(PhpWriter out)
	throws IOException
      {
      }
    };
}

