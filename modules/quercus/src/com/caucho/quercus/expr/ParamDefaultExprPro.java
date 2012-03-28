/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP default expression.
 */
public class ParamDefaultExprPro extends ParamDefaultExpr
  implements ExprPro
{
  public ParamDefaultExprPro()
  {
    super();
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  public final ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
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
        out.print("DefaultValue.DEFAULT");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
        throws IOException
      {
        out.print("0");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateString(PhpWriter out)
        throws IOException
      {
        out.print("\"\"");
      }
    };
}

