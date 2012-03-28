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
 * Represents a PHP each expression.
 */
public class FunEachExprPro extends FunEachExpr
  implements ExprPro
{
  public FunEachExprPro(Expr expr)
  {
    super(expr);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractUnaryExprGenerator(getLocation()) {
    @Override
    public ExprGenerator getExpr()
    {
      return ((ExprPro) _expr).getGenerator();
    }

    @Override
    public ExprType analyze(AnalyzeInfo info)
    {
      getExpr().analyze(info);
      getExpr().analyzeSetReference(info);

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
      out.print("ArrayModule.each(env, ");
      getExpr().generateRef(out);
      out.print(")");
    }

  };
}

