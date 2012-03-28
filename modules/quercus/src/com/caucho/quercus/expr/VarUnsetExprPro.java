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
 * Represents unsetting a PHP variable
 */
public class VarUnsetExprPro extends VarUnsetExpr
  implements ExprPro
{
  public VarUnsetExprPro(AbstractVarExpr var)
  {
    super(var);
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
        ExprGenerator varGen = ((ExprPro) _var).getGenerator();
        
        varGen.analyzeUnset(info);

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
        ExprGenerator varGen = ((ExprPro) _var).getGenerator();

        varGen.generateUnset(out);
      }
    };
}

