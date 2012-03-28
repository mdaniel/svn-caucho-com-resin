/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP constant expression.
 */
public class ConstDirExprPro extends ConstDirExpr
  implements ExprPro
{
  public ConstDirExprPro(String dirName)
  {
    super(dirName);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        return ExprType.VALUE;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        out.print("env.createString(env.getRealPath(getUserDirStatic()))");
      }
    };
}

