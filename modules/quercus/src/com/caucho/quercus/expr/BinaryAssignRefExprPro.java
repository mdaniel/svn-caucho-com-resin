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
 * Represents a PHP assignment expression.
 */
public class BinaryAssignRefExprPro extends BinaryAssignRefExpr
  implements ExprPro
{
  public BinaryAssignRefExprPro(AbstractVarExpr var, Expr value)
  {
    super(var, value);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      private ExprGenerator getVar()
      {
        return ((ExprPro) _var).getGenerator();
      }
      
      private ExprGenerator getValue()
      { 
        return ((ExprPro) _value).getGenerator();
      }
      
      /**
       * Analyze the expression
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        getVar().analyzeAssign(info, getValue());
        getVar().analyzeSetReference(info);
        
        getValue().analyzeSetReference(info);

        return ExprType.VALUE;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generate(PhpWriter out)
        throws IOException
      {
        getVar().generateAssignRef(out, _value, false);
      }
      
      /**
       * Generates code for a function arg.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateValue(PhpWriter out)
        throws IOException
      {
        generate(out);
        out.print(".toValue()");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateTop(PhpWriter out)
        throws IOException
      {
        getVar().generateAssignRef(out, _value, true);
      }

      /**
       * Generates code to evaluate the expression, copying the result
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateCopy(PhpWriter out)
      throws IOException
      {
        generate(out);
        out.print(".copy()");  // php/3a5r
      }
    };
}

