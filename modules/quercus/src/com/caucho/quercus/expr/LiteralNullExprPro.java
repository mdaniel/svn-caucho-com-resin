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
 * Represents a PHP null expression.
 */
public class LiteralNullExprPro extends LiteralNullExpr
  implements ExprPro
{
  public static final LiteralNullExprPro NULL
    = new LiteralNullExprPro();
  
  private LiteralNullExprPro()
  {
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      //
      // Java code generation
      //

      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
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
	out.print("NullValue.NULL");
      }

      /**
       * Recreate the expression for default args.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("LiteralNullExpr.NULL");
      }

      public void generateStatement(PhpWriter out)
	throws IOException
      {
      }
    };
}

