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
 * Represents a PHP array unset expression.
 */
public class ArrayUnsetExprPro extends ArrayUnsetExpr
  implements ExprPro
{
  public ArrayUnsetExprPro(Expr expr, Expr index)
  {
    super(expr, index);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyze the expression
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        ExprPro expr = (ExprPro) _expr;
        ExprPro index = (ExprPro) _index;
        
        expr.getGenerator().analyze(info);
        index.getGenerator().analyze(info);

        return ExprType.VALUE;
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        ExprPro expr = (ExprPro) _expr;
        ExprPro index = (ExprPro) _index;

        expr.getGenerator().generateDirty(out);
        out.print(".remove(");
        index.getGenerator().generate(out);
        out.print(")");
      }
    };
}

