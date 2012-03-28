/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.Location;

import java.io.IOException;

/**
 * Represents a PHP required expression.
 */
public class ParamRequiredExprPro extends ParamRequiredExpr
  implements ExprPro
{
  public static final ParamRequiredExprPro REQUIRED = new ParamRequiredExprPro();

  protected ParamRequiredExprPro()
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

      public void generate(PhpWriter out)
        throws IOException
      {
        out.print("ParamRequiredExprPro.REQUIRED.eval(env)");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
        throws IOException
      {
        out.print("ParamRequiredExprPro.REQUIRED");
      }
    };
}

