/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Converts to an string
 */
public class ToStringExprPro extends ToStringExpr
  implements ExprPro
{
  public ToStringExprPro(Expr expr)
  {
    super(expr);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractUnaryExprGenerator(getLocation()) {
      public ExprGenerator getExpr()
      {
        return ((ExprPro) _expr).getGenerator();
      }

      //
      // Java code generation
      //

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        getExpr().generate(out);
        out.print(".toString(env)");
      }
    };
}

